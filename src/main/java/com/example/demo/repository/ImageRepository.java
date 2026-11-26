package com.example.demo.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.example.demo.dto.ImageView;
import com.example.demo.model.Image;
import com.example.demo.model.Tag;

import lombok.RequiredArgsConstructor;

@Repository 
@RequiredArgsConstructor 
public class ImageRepository {
    private final JdbcClient db;

    public Long save(Image image) {
        Long imageId = db.sql("INSERT INTO Images (user_id, description, image_path) VALUES (:userId, :description, :imagePath) RETURNING id")
            .param("userId", image.getUserId())
            .param("description", image.getDescription())
            .param("imagePath", image.getImagePath())
            .query(Long.class)
            .single();

        return imageId;
    }

    public void replaceImageTags(Long imageId, List<Long> tagIds) {
        // delete yang existing sekarang agar gak duplikat
        db.sql("DELETE FROM Image_Tag WHERE image_id = :imageId")
            .param("imageId", imageId)
            .update();
        
        // Cek tagIds kosong atau tidak
        if (tagIds == null || tagIds.isEmpty()) return;

        // Masukkan set of tags yang baru
        for (Long id : tagIds) {
            db.sql("INSERT INTO Image_Tag (image_id, tag_id) VALUES (:imageId, :tagId)")
                .param("imageId", imageId)
                .param("tagId", id)
                .update();
        }
    }

    public Optional<Image> findByImageId(Long id) {
        return db.sql("SELECT id, user_id, description, image_path, uploaded_at FROM Images WHERE id = :id")
            .param("id", id)
            .query(Image.class)
            .optional();
    }

    public int update(Image image) {
        return db.sql("UPDATE Images SET description = :description, image_path = :image_path WHERE id = :id AND user_id = :user_id")
            .param("description", image.getDescription())
            .param("image_path", image.getImagePath())
            .param("id", image.getId())
            .param("user_id", image.getUserId())
            .update();
    }

    /**
     * Method untuk menghapus image dari database. Image yang tersimpan dalam server belum dihapus disini, dan belum ada pengecekan role untuk delete.
     * Hanya admin dan pemilik foto yang berhak melakukan delete
     * @param id
     * @return {@code Optional<String>} berisi image path untuk di delete dari server
     */
    public Optional<String> deleteByImageId(Long id) {
        return db.sql("DELETE FROM Images WHERE id = :id RETURNING image_path")
            .param("id", id)
            .query(String.class)
            .optional();
    }

    public List<ImageView> findGallery(String keyword, List<Long> tagIds, Long ownerUserId, int limit, int offset) {
        Filter filter = buildFilter(keyword, tagIds, ownerUserId);
        String sql = "SELECT i.id, i.user_id, u.username, u.full_name, i.description, i.image_path, i.uploaded_at FROM Images i JOIN Users u ON i.user_id = u.id " + filter.where() + " ORDER BY i.uploaded_at DESC, i.id DESC LIMIT :limit OFFSET :offset";
        
        return db.sql(sql)
            .params(filter.params)
            .param("limit", limit)
            .param("offset", offset)
            .query(ImageView.class)
            .list();
    }

    public Long countGallery(String keyword, List<Long> tagIds, Long ownerUserId) {
        Filter filter = buildFilter(keyword, tagIds, ownerUserId);
        String sql = "SELECT COUNT(*) FROM Images i " + filter.where();

        return db.sql(sql)
            .params(filter.params())
            .query(Long.class)
            .single();
    }

    public Map<Long, List<Tag>> findTagsByImageId(List<Long> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) return Map.of();

        List<ImageTagRow> rows = db.sql("SELECT it.image_id, t.id, t.name FROM Image_Tag it JOIN Tag t ON it.tag_id = t.id WHERE it.image_id IN (:imageIds) ORDER BY t.name ASC")
            .param("imageIds", imageIds)
            .query(ImageTagRow.class)
            .list();

        Map<Long, List<Tag>> tags = new LinkedHashMap<>();
        for (ImageTagRow row : rows) {
            if (!tags.containsKey(row.imageId())) {
                tags.put(row.imageId(), new ArrayList<>());
            }
            tags.get(row.imageId()).add(new Tag(row.id(), row.name()));
        }

        return tags;
    }
    
    private Filter buildFilter(String keyword, List<Long> tagIds, Long ownerUserId) {
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();

        // Filter by keyword
        if (keyword != null && !keyword.isBlank()) {
            keyword = keyword.trim();
            conditions.add("i.description ILIKE :keyword");
            params.put("keyword", "%" + keyword + "%");
        }

        // Filter by tags, groupby agar pencarian tidak menemukan duplikat entry
        if (tagIds != null && !tagIds.isEmpty()) {
            List<Long> uniqueIds = tagIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
            
            if (!uniqueIds.isEmpty()) {
                conditions.add("i.id IN (SELECT image_id FROM Image_Tag WHERE tag_id IN (:tagIds) GROUP BY image_id HAVING COUNT(DISTINCT tag_id) = :tagCount)");
                params.put("tagIds", uniqueIds);
                params.put("tagCount", uniqueIds.size());
            }
        }

        // Filter by kepemilikan foto
        if (ownerUserId != null) {
            conditions.add("i.user_id = :ownerUserId");
            params.put("ownerUserId", ownerUserId);
        }

        String where = conditions.isEmpty() ? "" : "WHERE " + String.join(" AND ", conditions);
        return new Filter(where, params);
    }

    private record Filter(String where, Map<String, Object> params) {}
    private record ImageTagRow(Long imageId, Long id, String name) {}
}
