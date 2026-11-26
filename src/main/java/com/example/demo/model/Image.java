package com.example.demo.model;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder 
@Data 
@NoArgsConstructor 
@AllArgsConstructor 
public class Image {
    private Long id;
    private Long userId;
    private String description;
    private String imagePath;
    private OffsetDateTime uploadedAt;
}
