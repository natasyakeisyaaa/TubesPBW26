package com.example.demo.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service 
public class FileStorageService {
    // Konstanta untuk file management
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10 MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/png", "image/jpeg", "image/webp");

    private final Path uploadRoot;

    public FileStorageService(@Value("${app.upload.dir}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadRoot);
        } catch (IOException e) {
            throw new IllegalStateException("Gagal membuat folder upload", e);
        }
    }

    public void save(MultipartFile file, String filename) {
        Path target = resolveSafePath(filename);
        try(InputStream in = file.getInputStream()) {
            Files.createDirectories(uploadRoot);
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Gagal menyimpan gambar", e);
        }
    }

    public void delete(String filename) {
        if (filename == null || filename.isBlank()) return;
        Path target = resolveSafePath(filename);

        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new IllegalStateException("Gagal menghapus gambar", e);
        }
    }

    public String prepareFileName(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("File gambar tidak ditemukan");
        if (file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("Ukuran gambar maksimal 10 MB");

        String originalName = file.getOriginalFilename();
        int dotIndex = (originalName != null) ? originalName.lastIndexOf('.') : -1;
        if (originalName == null || dotIndex < 0 || dotIndex == originalName.length() - 1) throw new IllegalArgumentException("File extension tidak valid");    

        String extension = originalName.substring(dotIndex + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) throw new IllegalArgumentException("Tipe file harus berupa gambar (PNG, JPG, JPEG, WEBP)");

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.trim().toLowerCase())) throw new IllegalArgumentException("Content type harus berupa gambar (PNG, JPG, JPEG, WEBP)");

        // Cek header, untuk memastikan bukan file yang diubah extensionnya
        byte[] header;
        try(InputStream in = file.getInputStream()) {
            header = in.readNBytes(12);
        } catch (IOException e) {
            throw new IllegalStateException("Gagal membaca header file", e);
        }

        String detectedExt = detectImageExtension(header);
        if (detectedExt == null) throw new IllegalArgumentException("Isi file bukan gambar PNG, JPG, JPEG, atau WEBP yang valid");

        return UUID.randomUUID() + "." + detectedExt;
    }

    private Path resolveSafePath(String fileName) {
        if (fileName == null || fileName.isBlank()) throw new IllegalArgumentException("Filename tidak boleh kosong");

        Path target = uploadRoot.resolve(fileName).normalize();
        if (!target.startsWith(uploadRoot) || target.equals(uploadRoot)) throw new IllegalArgumentException("Target tidak valid");

        return target;
    }

    private String detectImageExtension(byte[] bytes) {
        /*
        PNG: 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        JPG: 0xFF, 0xD8, 0xFF
        WEBP: 0x52, 0x49, 0x46, 0x46, [], [], [], [], 0x57, 0x45, 0x42, 0x50
        */
        if (bytes == null) return null;
        if (matches(bytes, 0, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) return "png";
        if (matches(bytes, 0, 0xFF, 0xD8, 0xFF)) return "jpg";
        if (matches(bytes, 0, 'R', 'I', 'F', 'F') && matches(bytes, 8, 'W', 'E', 'B', 'P')) return "webp";
        return null;
    }

    private boolean matches(byte[] bytes, int offset, int... expected) {
        if (bytes.length < offset + expected.length)
            return false;
        for (int i = 0; i < expected.length; i++) {
            if (bytes[offset + i] != (byte) expected[i])
                return false;
        }
        return true;
    }
}
