package com.example.demo.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.ImageEditForm;
import com.example.demo.dto.ImageUploadForm;
import com.example.demo.dto.ImageView;
import com.example.demo.dto.PageResult;
import com.example.demo.dto.SessionUser;
import com.example.demo.model.Image;
import com.example.demo.model.Role;
import com.example.demo.model.Tag;
import com.example.demo.repository.ImageRepository;
import com.example.demo.repository.TagRepository;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class ImageService {
    private static final int PAGE_SIZE = 6; // Jumlah gambar per halaman

    private final ImageRepository imageRepository;
    private final TagRepository tagRepository;
    private final FileStorageService fileService;

    public List<Tag> getAllTags() {
        return tagRepository.findAll();
    }

    @Transactional 
    public long uploadImage(SessionUser sessionUser, ImageUploadForm form) {
        if (sessionUser == null) throw new IllegalArgumentException("User belum login");

        String description = form.getDescription().trim();
        List<Long> ids = validateAndNormalizeTags(form.getTagIds());
        String fileName = fileService.prepareFileName(form.getFile());

        // Build image baru
        Image newImage = Image.builder()
            .userId(sessionUser.id())
            .description(description)
            .imagePath(fileName)
            .build();
        
        // insert ke database
        Long imageId = imageRepository.save(newImage);
        imageRepository.replaceImageTags(imageId, ids);

        // simpan di disk
        fileService.save(form.getFile(), fileName);
        return imageId;
    }

    public PageResult<ImageView> getGallery(String keyword, List<Long> tagIds, Long ownerUserId, int page) {
        long totalItems = imageRepository.countGallery(keyword, tagIds, ownerUserId);
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / PAGE_SIZE));
        int safePage = Math.max(1, Math.min(totalPages, page)); // Mencegah user manual mengetik page 0, atau page melebihi total page yang ada (clamping)

        if (totalItems == 0) return new PageResult<>(List.of(), safePage, PAGE_SIZE, 0L, totalPages); // Kalau tidak ada hasil, langsung kembalikan view kosong

        // Ambil bagian gambar di halaman tertentu dengan offset
        int offset = (safePage - 1) * PAGE_SIZE;
        List<ImageView> images = imageRepository.findGallery(keyword, tagIds, ownerUserId, PAGE_SIZE, offset);

        List<Long> imageIds = images.stream()
            .map(ImageView::getId)
            .toList();
        
        Map<Long, List<Tag>> tagMap = imageRepository.findTagsByImageId(imageIds);
        for (ImageView image : images) {
            image.setTags(tagMap.getOrDefault(image.getId(), List.of()));
        }

        return new PageResult<>(images, safePage, PAGE_SIZE, totalItems, totalPages);
    }

    public ImageView getImgForEdit(Long imageId, SessionUser sessionUser) {
        Image image = findOwnedImage(imageId, sessionUser);
        List<Tag> tags = tagRepository.findByImageId(imageId);

        ImageView imageView = ImageView.builder()
            .id(imageId)
            .userId(sessionUser.id())
            .description(image.getDescription())
            .imagePath(image.getImagePath())
            .uploadedAt(image.getUploadedAt())
            .tags(tags)
            .build();
        
        return imageView;
    }

    @Transactional 
    public void updateImage(Long imageId, SessionUser sessionUser, ImageEditForm form) {
        // Ambil gambar yang ingin di update
        Image existing = findOwnedImage(imageId, sessionUser);

        // Ambil hasil edit
        String description = form.getDescription().trim();
        List<Long> tagIds = validateAndNormalizeTags(form.getTagIds());

        // Optionally, buat gambar baru jika user mengganti gambar
        MultipartFile newFile = form.getFile();
        boolean hasNewFile = (newFile != null) && !newFile.isEmpty();

        String oldImagePath = existing.getImagePath();
        String targetFileName = hasNewFile ? fileService.prepareFileName(newFile) : oldImagePath;

        Image image = Image.builder()
            .id(imageId)
            .userId(sessionUser.id())
            .description(description)
            .imagePath(targetFileName)
            .build();
        
        // Masukkan perubahan ke db, lalu cek perubahan rows
        int rows = imageRepository.update(image);
        if (rows == 0) throw new IllegalStateException("Gagal menyimpan perubahan gambar");
        imageRepository.replaceImageTags(imageId, tagIds);

        // Jika gambar diganti, ganti juga di disk
        if (hasNewFile) {
            fileService.save(newFile, targetFileName);
            fileService.delete(oldImagePath);
        }
    }

    @Transactional 
    public void deleteImage(Long imageId, SessionUser sessionUser) {
        if (sessionUser == null) throw new IllegalArgumentException("User belum login");
        Image image = imageRepository.findByImageId(imageId).orElseThrow(() -> new IllegalArgumentException("Gambar tidak ditemukan"));
        
        // Hanya pemilik foto atau admin yang berhak menghapus
        if (!image.getUserId().equals(sessionUser.id()) && sessionUser.role() != Role.ADMIN) throw new IllegalArgumentException("Anda tidak diizinkan menghapus gambar ini");

        // Hapus dari db, lalu dari disk
        String imagePath = imageRepository.deleteByImageId(imageId).orElseThrow(() -> new IllegalArgumentException("Gagal menghapus gambar dari database"));
        fileService.delete(imagePath);
    }

    private List<Long> validateAndNormalizeTags(List<Long> tagIds) {
        List<Long> uniqueIds = tagIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (uniqueIds.isEmpty() || uniqueIds.size() > 3) {
            throw new IllegalArgumentException("Gambar harus memiliki 1-3 tag");
        }

        if (tagRepository.countExistingByIds(uniqueIds) != uniqueIds.size()) {
            throw new IllegalArgumentException("Tag yang dipilih tidak valid");
        }

        return uniqueIds;
    }

    private Image findOwnedImage(Long imageId, SessionUser sessionUser) {
        if (sessionUser == null) throw new IllegalArgumentException("User belum login");
        Image image = imageRepository.findByImageId(imageId).orElseThrow(() -> new IllegalArgumentException("Gambar tidak ditemukan"));

        if (!image.getUserId().equals(sessionUser.id())) throw new IllegalArgumentException("Anda tidak memiliki akses ke gambar ini");
        return image;
    }
}
