package com.example.demo.dto;

import java.util.List;

public record PageResult<T>(List<T> items, int page, int size, long totalItems, int totalPages) {
    public boolean hasPrevious() {
        return page > 1;
    }

    public boolean hasNext() {
        return page < totalPages;
    }
} 
