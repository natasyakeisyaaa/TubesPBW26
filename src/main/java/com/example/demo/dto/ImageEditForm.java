package com.example.demo.dto;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data 
public class ImageEditForm {
    @NotBlank(message = "Deskripsi tidak boleh kosong")
    @Size(max = 120, message = "Deskripsi maksimal berukuran 120 karakter")
    private String description;

    private MultipartFile file;

    @NotEmpty(message = "Minimal 1 tag perlu dipilih")
    @Size(max = 3, message = "1 Gambar maksimal memiliki 3 tag")
    private List<Long> tagIds = new ArrayList<>();
}
