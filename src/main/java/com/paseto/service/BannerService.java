package com.paseto.service;

import com.paseto.dto.BannerRequest;
import com.paseto.dto.BannerResponse;
import com.paseto.entity.Banner;
import com.paseto.repository.BannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BannerService {

    private final BannerRepository bannerRepository;

    public List<BannerResponse> findAll() {
        return bannerRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(BannerResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<BannerResponse> findActive() {
        return bannerRepository.findByActiveOrderByDisplayOrderAsc(true).stream()
                .map(BannerResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public BannerResponse findById(Long id) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Banner not found"));
        return BannerResponse.fromEntity(banner);
    }

    public BannerResponse create(BannerRequest request) {
        Banner banner = new Banner();
        banner.setTitle(request.getTitle());
        banner.setDescription(request.getDescription());
        banner.setImageUrl(request.getImageUrl());
        banner.setLinkUrl(request.getLinkUrl());
        banner.setDisplayOrder(request.getDisplayOrder());
        banner.setActive(request.getActive());

        banner = bannerRepository.save(banner);
        return BannerResponse.fromEntity(banner);
    }

    public BannerResponse update(Long id, BannerRequest request) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Banner not found"));

        banner.setTitle(request.getTitle());
        banner.setDescription(request.getDescription());
        banner.setImageUrl(request.getImageUrl());
        banner.setLinkUrl(request.getLinkUrl());
        banner.setDisplayOrder(request.getDisplayOrder());
        banner.setActive(request.getActive());

        banner = bannerRepository.save(banner);
        return BannerResponse.fromEntity(banner);
    }

    public void delete(Long id) {
        if (!bannerRepository.existsById(id)) {
            throw new IllegalArgumentException("Banner not found");
        }
        bannerRepository.deleteById(id);
    }
}
