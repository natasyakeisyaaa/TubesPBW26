package com.example.demo.dto;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import com.example.demo.model.Tag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder 
@Data 
@NoArgsConstructor 
@AllArgsConstructor 
public class ImageView {
    private Long id;
    private Long userId;
    private String username;
    private String fullName;
    private String description;
    private String imagePath;
    private OffsetDateTime uploadedAt;
    
    @Builder.Default
    private List<Tag> tags = new ArrayList<>();
}
