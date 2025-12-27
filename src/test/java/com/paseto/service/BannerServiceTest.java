package com.paseto.service;

import com.paseto.dto.BannerRequest;
import com.paseto.dto.BannerResponse;
import com.paseto.entity.Banner;
import com.paseto.repository.BannerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BannerService Unit Tests")
class BannerServiceTest {

    @Mock
    private BannerRepository bannerRepository;

    @InjectMocks
    private BannerService bannerService;

    private Banner testBanner1;
    private Banner testBanner2;
    private Banner testBanner3;
    private BannerRequest testRequest;

    @BeforeEach
    void setUp() {
        // Setup test banners with different displayOrder
        testBanner1 = new Banner();
        testBanner1.setId(1L);
        testBanner1.setTitle("Summer Sale");
        testBanner1.setDescription("Big summer sale");
        testBanner1.setImageUrl("http://example.com/summer.jpg");
        testBanner1.setLinkUrl("http://example.com/summer");
        testBanner1.setDisplayOrder(2);
        testBanner1.setActive(true);
        testBanner1.setCreatedAt(LocalDateTime.now());
        testBanner1.setUpdatedAt(LocalDateTime.now());

        testBanner2 = new Banner();
        testBanner2.setId(2L);
        testBanner2.setTitle("Winter Sale");
        testBanner2.setDescription("Winter discounts");
        testBanner2.setImageUrl("http://example.com/winter.jpg");
        testBanner2.setLinkUrl("http://example.com/winter");
        testBanner2.setDisplayOrder(1);
        testBanner2.setActive(true);
        testBanner2.setCreatedAt(LocalDateTime.now());
        testBanner2.setUpdatedAt(LocalDateTime.now());

        testBanner3 = new Banner();
        testBanner3.setId(3L);
        testBanner3.setTitle("Inactive Banner");
        testBanner3.setDescription("This banner is inactive");
        testBanner3.setImageUrl("http://example.com/inactive.jpg");
        testBanner3.setLinkUrl(null);
        testBanner3.setDisplayOrder(3);
        testBanner3.setActive(false);
        testBanner3.setCreatedAt(LocalDateTime.now());
        testBanner3.setUpdatedAt(LocalDateTime.now());

        // Setup test request
        testRequest = new BannerRequest();
        testRequest.setTitle("New Banner");
        testRequest.setDescription("New banner description");
        testRequest.setImageUrl("http://example.com/new.jpg");
        testRequest.setLinkUrl("http://example.com/new");
        testRequest.setDisplayOrder(10);
        testRequest.setActive(true);
    }

    // ==================== FIND ALL TESTS ====================

    @Nested
    @DisplayName("Find All Tests")
    class FindAllTests {

        @Test
        @DisplayName("Should return all banners ordered by display order")
        void shouldReturnAllBannersOrdered() {
            // Given - Return banners in wrong order
            List<Banner> banners = Arrays.asList(testBanner1, testBanner2, testBanner3);
            when(bannerRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(banners);

            // When
            List<BannerResponse> result = bannerService.findAll();

            // Then
            assertNotNull(result);
            assertEquals(3, result.size());
            assertEquals("Summer Sale", result.get(0).getTitle());
            assertEquals("Winter Sale", result.get(1).getTitle());
            assertEquals("Inactive Banner", result.get(2).getTitle());

            verify(bannerRepository).findAllByOrderByDisplayOrderAsc();
        }

        @Test
        @DisplayName("Should return empty list when no banners exist")
        void shouldReturnEmptyList() {
            // Given
            when(bannerRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of());

            // When
            List<BannerResponse> result = bannerService.findAll();

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(bannerRepository).findAllByOrderByDisplayOrderAsc();
        }
    }

    // ==================== FIND ACTIVE TESTS ====================

    @Nested
    @DisplayName("Find Active Tests")
    class FindActiveTests {

        @Test
        @DisplayName("Should return only active banners ordered by display order")
        void shouldReturnActiveBannersOnly() {
            // Given
            List<Banner> activeBanners = Arrays.asList(testBanner2, testBanner1);
            when(bannerRepository.findByActiveOrderByDisplayOrderAsc(true)).thenReturn(activeBanners);

            // When
            List<BannerResponse> result = bannerService.findActive();

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            assertTrue(result.get(0).getActive());
            assertTrue(result.get(1).getActive());
            assertEquals("Winter Sale", result.get(0).getTitle());
            assertEquals("Summer Sale", result.get(1).getTitle());

            verify(bannerRepository).findByActiveOrderByDisplayOrderAsc(true);
        }

        @Test
        @DisplayName("Should return empty list when no active banners exist")
        void shouldReturnEmptyListWhenNoActiveBanners() {
            // Given
            when(bannerRepository.findByActiveOrderByDisplayOrderAsc(true)).thenReturn(List.of());

            // When
            List<BannerResponse> result = bannerService.findActive();

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(bannerRepository).findByActiveOrderByDisplayOrderAsc(true);
        }

        @Test
        @DisplayName("Should exclude inactive banners")
        void shouldExcludeInactiveBanners() {
            // Given - Only return active banners
            List<Banner> activeBanners = Arrays.asList(testBanner1);
            when(bannerRepository.findByActiveOrderByDisplayOrderAsc(true)).thenReturn(activeBanners);

            // When
            List<BannerResponse> result = bannerService.findActive();

            // Then
            assertEquals(1, result.size());
            assertTrue(result.get(0).getActive());
            assertEquals("Summer Sale", result.get(0).getTitle());

            // Verify inactive banner is not in results
            boolean hasInactive = result.stream().anyMatch(b -> !b.getActive());
            assertFalse(hasInactive);
        }
    }

    // ==================== FIND BY ID TESTS ====================

    @Nested
    @DisplayName("Find By ID Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Should find banner by valid ID")
        void shouldFindBannerById() {
            // Given
            Long bannerId = 1L;
            when(bannerRepository.findById(bannerId)).thenReturn(Optional.of(testBanner1));

            // When
            BannerResponse result = bannerService.findById(bannerId);

            // Then
            assertNotNull(result);
            assertEquals("Summer Sale", result.getTitle());
            assertEquals("Big summer sale", result.getDescription());
            assertEquals("http://example.com/summer.jpg", result.getImageUrl());
            assertEquals("http://example.com/summer", result.getLinkUrl());
            assertEquals(2, result.getDisplayOrder());
            assertTrue(result.getActive());

            verify(bannerRepository).findById(bannerId);
        }

        @Test
        @DisplayName("Should throw exception when banner not found")
        void shouldThrowExceptionWhenNotFound() {
            // Given
            Long bannerId = 999L;
            when(bannerRepository.findById(bannerId)).thenReturn(Optional.empty());

            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> bannerService.findById(bannerId)
            );

            assertEquals("Banner not found", exception.getMessage());
            verify(bannerRepository).findById(bannerId);
        }

        @Test
        @DisplayName("Should find inactive banner by ID")
        void shouldFindInactiveBanner() {
            // Given
            Long bannerId = 3L;
            when(bannerRepository.findById(bannerId)).thenReturn(Optional.of(testBanner3));

            // When
            BannerResponse result = bannerService.findById(bannerId);

            // Then
            assertNotNull(result);
            assertEquals("Inactive Banner", result.getTitle());
            assertFalse(result.getActive());

            verify(bannerRepository).findById(bannerId);
        }
    }

    // ==================== CREATE TESTS ====================

    @Nested
    @DisplayName("Create Tests")
    class CreateTests {

        @Test
        @DisplayName("Should create new banner successfully")
        void shouldCreateBanner() {
            // Given
            Banner savedBanner = new Banner();
            savedBanner.setId(4L);
            savedBanner.setTitle(testRequest.getTitle());
            savedBanner.setDescription(testRequest.getDescription());
            savedBanner.setImageUrl(testRequest.getImageUrl());
            savedBanner.setLinkUrl(testRequest.getLinkUrl());
            savedBanner.setDisplayOrder(testRequest.getDisplayOrder());
            savedBanner.setActive(testRequest.getActive());
            savedBanner.setCreatedAt(LocalDateTime.now());
            savedBanner.setUpdatedAt(LocalDateTime.now());

            when(bannerRepository.save(any(Banner.class))).thenReturn(savedBanner);

            // When
            BannerResponse result = bannerService.create(testRequest);

            // Then
            assertNotNull(result);
            assertEquals("New Banner", result.getTitle());
            assertEquals("New banner description", result.getDescription());
            assertEquals("http://example.com/new.jpg", result.getImageUrl());
            assertEquals("http://example.com/new", result.getLinkUrl());
            assertEquals(10, result.getDisplayOrder());
            assertTrue(result.getActive());

            verify(bannerRepository).save(any(Banner.class));
        }

        @Test
        @DisplayName("Should create banner with null link URL")
        void shouldCreateBannerWithNullLink() {
            // Given
            BannerRequest request = new BannerRequest();
            request.setTitle("Banner Without Link");
            request.setDescription("No link URL");
            request.setImageUrl("http://example.com/no-link.jpg");
            request.setLinkUrl(null);
            request.setDisplayOrder(5);
            request.setActive(true);

            Banner savedBanner = new Banner();
            savedBanner.setId(5L);
            savedBanner.setTitle("Banner Without Link");
            savedBanner.setDescription("No link URL");
            savedBanner.setImageUrl("http://example.com/no-link.jpg");
            savedBanner.setLinkUrl(null);
            savedBanner.setDisplayOrder(5);
            savedBanner.setActive(true);
            savedBanner.setCreatedAt(LocalDateTime.now());
            savedBanner.setUpdatedAt(LocalDateTime.now());

            when(bannerRepository.save(any(Banner.class))).thenReturn(savedBanner);

            // When
            BannerResponse result = bannerService.create(request);

            // Then
            assertNotNull(result);
            assertEquals("Banner Without Link", result.getTitle());
            assertNull(result.getLinkUrl());

            verify(bannerRepository).save(any(Banner.class));
        }

        @Test
        @DisplayName("Should create banner with default display order")
        void shouldCreateBannerWithDefaultDisplayOrder() {
            // Given
            BannerRequest request = new BannerRequest();
            request.setTitle("Default Order Banner");
            request.setImageUrl("http://example.com/default.jpg");
            request.setDisplayOrder(0); // Default value
            request.setActive(true);

            Banner savedBanner = new Banner();
            savedBanner.setId(6L);
            savedBanner.setTitle("Default Order Banner");
            savedBanner.setImageUrl("http://example.com/default.jpg");
            savedBanner.setDisplayOrder(0);
            savedBanner.setActive(true);
            savedBanner.setCreatedAt(LocalDateTime.now());
            savedBanner.setUpdatedAt(LocalDateTime.now());

            when(bannerRepository.save(any(Banner.class))).thenReturn(savedBanner);

            // When
            BannerResponse result = bannerService.create(request);

            // Then
            assertNotNull(result);
            assertEquals(0, result.getDisplayOrder());

            verify(bannerRepository).save(any(Banner.class));
        }
    }

    // ==================== UPDATE TESTS ====================

    @Nested
    @DisplayName("Update Tests")
    class UpdateTests {

        @Test
        @DisplayName("Should update existing banner successfully")
        void shouldUpdateBanner() {
            // Given
            Long bannerId = 1L;
            BannerRequest request = new BannerRequest();
            request.setTitle("Updated Summer Sale");
            request.setDescription("Updated description");
            request.setImageUrl("http://example.com/updated-summer.jpg");
            request.setLinkUrl("http://example.com/updated-summer");
            request.setDisplayOrder(20);
            request.setActive(false);

            Banner updatedBanner = new Banner();
            updatedBanner.setId(bannerId);
            updatedBanner.setTitle(request.getTitle());
            updatedBanner.setDescription(request.getDescription());
            updatedBanner.setImageUrl(request.getImageUrl());
            updatedBanner.setLinkUrl(request.getLinkUrl());
            updatedBanner.setDisplayOrder(request.getDisplayOrder());
            updatedBanner.setActive(request.getActive());
            updatedBanner.setCreatedAt(LocalDateTime.now());
            updatedBanner.setUpdatedAt(LocalDateTime.now());

            when(bannerRepository.findById(bannerId)).thenReturn(Optional.of(testBanner1));
            when(bannerRepository.save(any(Banner.class))).thenReturn(updatedBanner);

            // When
            BannerResponse result = bannerService.update(bannerId, request);

            // Then
            assertNotNull(result);
            assertEquals("Updated Summer Sale", result.getTitle());
            assertEquals("Updated description", result.getDescription());
            assertEquals("http://example.com/updated-summer.jpg", result.getImageUrl());
            assertEquals("http://example.com/updated-summer", result.getLinkUrl());
            assertEquals(20, result.getDisplayOrder());
            assertFalse(result.getActive());

            verify(bannerRepository).findById(bannerId);
            verify(bannerRepository).save(any(Banner.class));
        }

        @Test
        @DisplayName("Should throw exception when updating non-existent banner")
        void shouldThrowExceptionWhenUpdatingNonExistent() {
            // Given
            Long bannerId = 999L;
            when(bannerRepository.findById(bannerId)).thenReturn(Optional.empty());

            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> bannerService.update(bannerId, testRequest)
            );

            assertEquals("Banner not found", exception.getMessage());
            verify(bannerRepository).findById(bannerId);
            verify(bannerRepository, never()).save(any(Banner.class));
        }

        @Test
        @DisplayName("Should update banner to inactive")
        void shouldUpdateBannerToInactive() {
            // Given
            Long bannerId = 1L;
            BannerRequest request = new BannerRequest();
            request.setTitle("Summer Sale");
            request.setImageUrl("http://example.com/summer.jpg");
            request.setActive(false);

            Banner updatedBanner = new Banner();
            updatedBanner.setId(bannerId);
            updatedBanner.setTitle("Summer Sale");
            updatedBanner.setImageUrl("http://example.com/summer.jpg");
            updatedBanner.setActive(false);
            updatedBanner.setCreatedAt(LocalDateTime.now());
            updatedBanner.setUpdatedAt(LocalDateTime.now());

            when(bannerRepository.findById(bannerId)).thenReturn(Optional.of(testBanner1));
            when(bannerRepository.save(any(Banner.class))).thenReturn(updatedBanner);

            // When
            BannerResponse result = bannerService.update(bannerId, request);

            // Then
            assertNotNull(result);
            assertFalse(result.getActive());

            verify(bannerRepository).findById(bannerId);
            verify(bannerRepository).save(any(Banner.class));
        }
    }

    // ==================== DELETE TESTS ====================

    @Nested
    @DisplayName("Delete Tests")
    class DeleteTests {

        @Test
        @DisplayName("Should delete banner successfully")
        void shouldDeleteBanner() {
            // Given
            Long bannerId = 1L;
            when(bannerRepository.existsById(bannerId)).thenReturn(true);
            doNothing().when(bannerRepository).deleteById(bannerId);

            // When
            bannerService.delete(bannerId);

            // Then
            verify(bannerRepository).existsById(bannerId);
            verify(bannerRepository).deleteById(bannerId);
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent banner")
        void shouldThrowExceptionWhenDeletingNonExistent() {
            // Given
            Long bannerId = 999L;
            when(bannerRepository.existsById(bannerId)).thenReturn(false);

            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> bannerService.delete(bannerId)
            );

            assertEquals("Banner not found", exception.getMessage());
            verify(bannerRepository).existsById(bannerId);
            verify(bannerRepository, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("Should delete inactive banner")
        void shouldDeleteInactiveBanner() {
            // Given
            Long bannerId = 3L;
            when(bannerRepository.existsById(bannerId)).thenReturn(true);
            doNothing().when(bannerRepository).deleteById(bannerId);

            // When
            bannerService.delete(bannerId);

            // Then
            verify(bannerRepository).existsById(bannerId);
            verify(bannerRepository).deleteById(bannerId);
        }
    }

    // ==================== EDGE CASES TESTS ====================

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle banner with very long title")
        void shouldHandleLongTitle() {
            // Given
            String longTitle = "A".repeat(200);
            BannerRequest request = new BannerRequest();
            request.setTitle(longTitle);
            request.setImageUrl("http://example.com/long.jpg");
            request.setActive(true);

            Banner savedBanner = new Banner();
            savedBanner.setId(7L);
            savedBanner.setTitle(longTitle);
            savedBanner.setImageUrl("http://example.com/long.jpg");
            savedBanner.setActive(true);
            savedBanner.setCreatedAt(LocalDateTime.now());
            savedBanner.setUpdatedAt(LocalDateTime.now());

            when(bannerRepository.save(any(Banner.class))).thenReturn(savedBanner);

            // When
            BannerResponse result = bannerService.create(request);

            // Then
            assertNotNull(result);
            assertEquals(200, result.getTitle().length());
        }

        @Test
        @DisplayName("Should handle banner with null description")
        void shouldHandleNullDescription() {
            // Given
            BannerRequest request = new BannerRequest();
            request.setTitle("No Description Banner");
            request.setDescription(null);
            request.setImageUrl("http://example.com/no-desc.jpg");
            request.setActive(true);

            Banner savedBanner = new Banner();
            savedBanner.setId(8L);
            savedBanner.setTitle("No Description Banner");
            savedBanner.setDescription(null);
            savedBanner.setImageUrl("http://example.com/no-desc.jpg");
            savedBanner.setActive(true);
            savedBanner.setCreatedAt(LocalDateTime.now());
            savedBanner.setUpdatedAt(LocalDateTime.now());

            when(bannerRepository.save(any(Banner.class))).thenReturn(savedBanner);

            // When
            BannerResponse result = bannerService.create(request);

            // Then
            assertNotNull(result);
            assertNull(result.getDescription());

            verify(bannerRepository).save(any(Banner.class));
        }

        @Test
        @DisplayName("Should handle banner with negative display order")
        void shouldHandleNegativeDisplayOrder() {
            // Given
            BannerRequest request = new BannerRequest();
            request.setTitle("Negative Order Banner");
            request.setImageUrl("http://example.com/neg.jpg");
            request.setDisplayOrder(-5);
            request.setActive(true);

            Banner savedBanner = new Banner();
            savedBanner.setId(9L);
            savedBanner.setTitle("Negative Order Banner");
            savedBanner.setImageUrl("http://example.com/neg.jpg");
            savedBanner.setDisplayOrder(-5);
            savedBanner.setActive(true);
            savedBanner.setCreatedAt(LocalDateTime.now());
            savedBanner.setUpdatedAt(LocalDateTime.now());

            when(bannerRepository.save(any(Banner.class))).thenReturn(savedBanner);

            // When
            BannerResponse result = bannerService.create(request);

            // Then
            assertNotNull(result);
            assertEquals(-5, result.getDisplayOrder());

            verify(bannerRepository).save(any(Banner.class));
        }

        @Test
        @DisplayName("Should handle banner with very high display order")
        void shouldHandleHighDisplayOrder() {
            // Given
            BannerRequest request = new BannerRequest();
            request.setTitle("High Order Banner");
            request.setImageUrl("http://example.com/high.jpg");
            request.setDisplayOrder(Integer.MAX_VALUE);
            request.setActive(true);

            Banner savedBanner = new Banner();
            savedBanner.setId(10L);
            savedBanner.setTitle("High Order Banner");
            savedBanner.setImageUrl("http://example.com/high.jpg");
            savedBanner.setDisplayOrder(Integer.MAX_VALUE);
            savedBanner.setActive(true);
            savedBanner.setCreatedAt(LocalDateTime.now());
            savedBanner.setUpdatedAt(LocalDateTime.now());

            when(bannerRepository.save(any(Banner.class))).thenReturn(savedBanner);

            // When
            BannerResponse result = bannerService.create(request);

            // Then
            assertNotNull(result);
            assertEquals(Integer.MAX_VALUE, result.getDisplayOrder());

            verify(bannerRepository).save(any(Banner.class));
        }
    }
}
