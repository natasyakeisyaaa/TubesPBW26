package com.example.demo.repository;

import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Tag;

import lombok.RequiredArgsConstructor;

@Repository 
@RequiredArgsConstructor 
public class TagRepository {
    private final JdbcClient db;

    public List<Tag> findAll() {
        return db.sql("SELECT id, name FROM Tag ORDER BY name ASC")
            .query(Tag.class)
            .list();
            
    }

    public List<Tag> findByImageId(Long imageId) {
        return db.sql("SELECT t.id, t.name FROM Tag t JOIN Image_Tag it ON it.tag_id = t.id WHERE it.image_id = :imageId ORDER BY t.name ASC")
            .param("imageId", imageId)
            .query(Tag.class)
            .list();
    }

    public long countExistingByIds(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) return 0L;

        return db.sql("SELECT COUNT(*) FROM Tag WHERE id IN (:tagIds)")
            .param("tagIds", tagIds)
            .query(Long.class)
            .single();
    }
}
