package com.paseto.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class BannerResponse {

    private Long id;
    private String title;
    private String description;
    private String imageUrl;
    private String linkUrl;
    private Integer displayOrder;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BannerResponse fromEntity(com.paseto.entity.Banner banner) {
        return new BannerResponse(
            banner.getId(),
            banner.getTitle(),
            banner.getDescription(),
            banner.getImageUrl(),
            banner.getLinkUrl(),
            banner.getDisplayOrder(),
            banner.getActive(),
            banner.getCreatedAt(),
            banner.getUpdatedAt()
        );
    }
}
