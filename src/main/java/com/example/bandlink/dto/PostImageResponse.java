package com.example.bandlink.dto;

import com.example.bandlink.entity.PostImage;

public record PostImageResponse(Long id, String imageUrl, int sortOrder) {
    public static PostImageResponse from(PostImage image) {
        return new PostImageResponse(image.getId(), image.getImageUrl(), image.getSortOrder());
    }
}
