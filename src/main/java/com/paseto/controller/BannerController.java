package com.paseto.controller;

import com.paseto.dto.BannerListResponse;
import com.paseto.dto.BannerRequest;
import com.paseto.dto.BannerResponse;
import com.paseto.dto.DeleteResponse;
import com.paseto.dto.EntityResponse;
import com.paseto.service.BannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/banners")
@RequiredArgsConstructor
@Tag(name = "Banners", description = "Banner management APIs (Public - No authentication required)")
public class BannerController {

    private final BannerService bannerService;

    @Operation(
            summary = "Get all banners",
            description = "Retrieve a list of all banners ordered by display order"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Banners retrieved successfully"
            )
    })
    @GetMapping
    public ResponseEntity<BannerListResponse> getAllBanners() {
        List<BannerResponse> banners = bannerService.findAll();
        BannerListResponse response = new BannerListResponse(200, "OK", banners);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get active banners",
            description = "Retrieve only active banners ordered by display order"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Active banners retrieved successfully"
            )
    })
    @GetMapping("/active")
    public ResponseEntity<BannerListResponse> getActiveBanners() {
        List<BannerResponse> banners = bannerService.findActive();
        BannerListResponse response = new BannerListResponse(200, "OK", banners);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get banner by ID",
            description = "Retrieve a specific banner by its ID"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Banner found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Banner not found"
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<EntityResponse<BannerResponse>> getBannerById(
            @Parameter(description = "Banner ID", required = true)
            @PathVariable Long id) {
        BannerResponse data = bannerService.findById(id);
        EntityResponse<BannerResponse> response = EntityResponse.of(
                200,
                "Banner found",
                data
        );
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Create new banner",
            description = "Create a new banner (public endpoint - no authentication required)"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Banner created successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation error"
            )
    })
    @PostMapping
    public ResponseEntity<EntityResponse<BannerResponse>> createBanner(
            @Valid @RequestBody BannerRequest request) {
        BannerResponse data = bannerService.create(request);
        EntityResponse<BannerResponse> response = EntityResponse.of(
                201,
                "Banner created successfully",
                data
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Update banner",
            description = "Update an existing banner by ID"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Banner updated successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Banner not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation error"
            )
    })
    @PutMapping("/{id}")
    public ResponseEntity<EntityResponse<BannerResponse>> updateBanner(
            @Parameter(description = "Banner ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody BannerRequest request) {
        BannerResponse data = bannerService.update(id, request);
        EntityResponse<BannerResponse> response = EntityResponse.of(
                200,
                "Banner updated successfully",
                data
        );
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Delete banner",
            description = "Delete a banner by ID"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Banner deleted successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Banner not found"
            )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<EntityResponse<DeleteResponse>> deleteBanner(
            @Parameter(description = "Banner ID", required = true)
            @PathVariable Long id) {
        bannerService.delete(id);
        DeleteResponse data = DeleteResponse.of(id);
        EntityResponse<DeleteResponse> response = EntityResponse.of(
                200,
                "Banner Delete success.",
                data
        );
        return ResponseEntity.ok(response);
    }
}
