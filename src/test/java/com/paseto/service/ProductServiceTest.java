package com.paseto.service;

import com.paseto.dto.ProductRequest;
import com.paseto.dto.ProductResponse;
import com.paseto.entity.Product;
import com.paseto.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Unit Tests")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product testProduct1;
    private Product testProduct2;
    private ProductRequest testRequest;

    @BeforeEach
    void setUp() {
        // Setup test products
        testProduct1 = new Product();
        testProduct1.setId(1L);
        testProduct1.setName("Laptop");
        testProduct1.setDescription("High-end laptop");
        testProduct1.setPrice(new BigDecimal("999.99"));
        testProduct1.setStock(10);
        testProduct1.setImageUrl("http://example.com/laptop.jpg");
        testProduct1.setSku("LAPTOP-001");
        testProduct1.setActive(true);
        testProduct1.setCreatedAt(LocalDateTime.now());
        testProduct1.setUpdatedAt(LocalDateTime.now());

        testProduct2 = new Product();
        testProduct2.setId(2L);
        testProduct2.setName("Mouse");
        testProduct2.setDescription("Wireless mouse");
        testProduct2.setPrice(new BigDecimal("29.99"));
        testProduct2.setStock(50);
        testProduct2.setImageUrl("http://example.com/mouse.jpg");
        testProduct2.setSku("MOUSE-001");
        testProduct2.setActive(true);
        testProduct2.setCreatedAt(LocalDateTime.now());
        testProduct2.setUpdatedAt(LocalDateTime.now());

        // Setup test request
        testRequest = new ProductRequest();
        testRequest.setName("Keyboard");
        testRequest.setDescription("Mechanical keyboard");
        testRequest.setPrice(new BigDecimal("79.99"));
        testRequest.setStock(25);
        testRequest.setImageUrl("http://example.com/keyboard.jpg");
        testRequest.setSku("KEYBOARD-001");
        testRequest.setActive(true);
    }

    // ==================== FIND ALL TESTS ====================

    @Nested
    @DisplayName("Find All Tests")
    class FindAllTests {

        @Test
        @DisplayName("Should return all products")
        void shouldReturnAllProducts() {
            // Given
            List<Product> products = Arrays.asList(testProduct1, testProduct2);
            when(productRepository.findAll()).thenReturn(products);

            // When
            List<ProductResponse> result = productService.findAll();

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals("Laptop", result.get(0).getName());
            assertEquals("Mouse", result.get(1).getName());

            verify(productRepository).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no products exist")
        void shouldReturnEmptyList() {
            // Given
            when(productRepository.findAll()).thenReturn(List.of());

            // When
            List<ProductResponse> result = productService.findAll();

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(productRepository).findAll();
        }
    }

    // ==================== FIND BY ID TESTS ====================

    @Nested
    @DisplayName("Find By ID Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Should find product by valid ID")
        void shouldFindProductById() {
            // Given
            Long productId = 1L;
            when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct1));

            // When
            ProductResponse result = productService.findById(productId);

            // Then
            assertNotNull(result);
            assertEquals("Laptop", result.getName());
            assertEquals("High-end laptop", result.getDescription());
            assertEquals(new BigDecimal("999.99"), result.getPrice());
            assertEquals(10, result.getStock());

            verify(productRepository).findById(productId);
        }

        @Test
        @DisplayName("Should throw exception when product not found")
        void shouldThrowExceptionWhenNotFound() {
            // Given
            Long productId = 999L;
            when(productRepository.findById(productId)).thenReturn(Optional.empty());

            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> productService.findById(productId)
            );

            assertEquals("Product not found", exception.getMessage());
            verify(productRepository).findById(productId);
        }
    }

    // ==================== CREATE TESTS ====================

    @Nested
    @DisplayName("Create Tests")
    class CreateTests {

        @Test
        @DisplayName("Should create new product successfully")
        void shouldCreateProduct() {
            // Given
            Product savedProduct = new Product();
            savedProduct.setId(3L);
            savedProduct.setName(testRequest.getName());
            savedProduct.setDescription(testRequest.getDescription());
            savedProduct.setPrice(testRequest.getPrice());
            savedProduct.setStock(testRequest.getStock());
            savedProduct.setImageUrl(testRequest.getImageUrl());
            savedProduct.setSku(testRequest.getSku());
            savedProduct.setActive(testRequest.getActive());
            savedProduct.setCreatedAt(LocalDateTime.now());
            savedProduct.setUpdatedAt(LocalDateTime.now());

            when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

            // When
            ProductResponse result = productService.create(testRequest);

            // Then
            assertNotNull(result);
            assertEquals("Keyboard", result.getName());
            assertEquals("Mechanical keyboard", result.getDescription());
            assertEquals(new BigDecimal("79.99"), result.getPrice());
            assertEquals(25, result.getStock());
            assertEquals("KEYBOARD-001", result.getSku());
            assertTrue(result.getActive());

            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("Should create product with null optional fields")
        void shouldCreateProductWithNullOptionals() {
            // Given
            ProductRequest request = new ProductRequest();
            request.setName("Simple Product");
            request.setPrice(new BigDecimal("10.00"));
            request.setStock(5);
            request.setDescription(null);
            request.setImageUrl(null);
            request.setSku(null);
            request.setActive(null);

            Product savedProduct = new Product();
            savedProduct.setId(4L);
            savedProduct.setName("Simple Product");
            savedProduct.setPrice(new BigDecimal("10.00"));
            savedProduct.setStock(5);
            savedProduct.setDescription(null);
            savedProduct.setImageUrl(null);
            savedProduct.setSku(null);
            savedProduct.setActive(null);
            savedProduct.setCreatedAt(LocalDateTime.now());
            savedProduct.setUpdatedAt(LocalDateTime.now());

            when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

            // When
            ProductResponse result = productService.create(request);

            // Then
            assertNotNull(result);
            assertEquals("Simple Product", result.getName());
            assertEquals(new BigDecimal("10.00"), result.getPrice());
            assertEquals(5, result.getStock());

            verify(productRepository).save(any(Product.class));
        }
    }

    // ==================== UPDATE TESTS ====================

    @Nested
    @DisplayName("Update Tests")
    class UpdateTests {

        @Test
        @DisplayName("Should update existing product successfully")
        void shouldUpdateProduct() {
            // Given
            Long productId = 1L;
            ProductRequest request = new ProductRequest();
            request.setName("Updated Laptop");
            request.setDescription("Updated description");
            request.setPrice(new BigDecimal("1099.99"));
            request.setStock(15);
            request.setImageUrl("http://example.com/updated.jpg");
            request.setSku("LAPTOP-UPD");
            request.setActive(false);

            Product updatedProduct = new Product();
            updatedProduct.setId(productId);
            updatedProduct.setName(request.getName());
            updatedProduct.setDescription(request.getDescription());
            updatedProduct.setPrice(request.getPrice());
            updatedProduct.setStock(request.getStock());
            updatedProduct.setImageUrl(request.getImageUrl());
            updatedProduct.setSku(request.getSku());
            updatedProduct.setActive(request.getActive());
            updatedProduct.setCreatedAt(LocalDateTime.now());
            updatedProduct.setUpdatedAt(LocalDateTime.now());

            when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct1));
            when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);

            // When
            ProductResponse result = productService.update(productId, request);

            // Then
            assertNotNull(result);
            assertEquals("Updated Laptop", result.getName());
            assertEquals("Updated description", result.getDescription());
            assertEquals(new BigDecimal("1099.99"), result.getPrice());
            assertEquals(15, result.getStock());
            assertEquals("LAPTOP-UPD", result.getSku());
            assertFalse(result.getActive());

            verify(productRepository).findById(productId);
            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("Should throw exception when updating non-existent product")
        void shouldThrowExceptionWhenUpdatingNonExistent() {
            // Given
            Long productId = 999L;
            when(productRepository.findById(productId)).thenReturn(Optional.empty());

            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> productService.update(productId, testRequest)
            );

            assertEquals("Product not found", exception.getMessage());
            verify(productRepository).findById(productId);
            verify(productRepository, never()).save(any(Product.class));
        }
    }

    // ==================== DELETE TESTS ====================

    @Nested
    @DisplayName("Delete Tests")
    class DeleteTests {

        @Test
        @DisplayName("Should delete product successfully")
        void shouldDeleteProduct() {
            // Given
            Long productId = 1L;
            when(productRepository.existsById(productId)).thenReturn(true);
            doNothing().when(productRepository).deleteById(productId);

            // When
            productService.delete(productId);

            // Then
            verify(productRepository).existsById(productId);
            verify(productRepository).deleteById(productId);
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent product")
        void shouldThrowExceptionWhenDeletingNonExistent() {
            // Given
            Long productId = 999L;
            when(productRepository.existsById(productId)).thenReturn(false);

            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> productService.delete(productId)
            );

            assertEquals("Product not found", exception.getMessage());
            verify(productRepository).existsById(productId);
            verify(productRepository, never()).deleteById(anyLong());
        }
    }

    // ==================== SEARCH BY NAME TESTS ====================

    @Nested
    @DisplayName("Search By Name Tests")
    class SearchByNameTests {

        @Test
        @DisplayName("Should find products containing search term (case insensitive)")
        void shouldSearchProductsByName() {
            // Given
            String searchTerm = "lap";
            List<Product> products = Arrays.asList(testProduct1);

            when(productRepository.findByNameContainingIgnoreCase(searchTerm)).thenReturn(products);

            // When
            List<ProductResponse> result = productService.searchByName(searchTerm);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("Laptop", result.get(0).getName());

            verify(productRepository).findByNameContainingIgnoreCase(searchTerm);
        }

        @Test
        @DisplayName("Should find products with uppercase search term")
        void shouldSearchWithUppercaseTerm() {
            // Given
            String searchTerm = "MOUSE";
            List<Product> products = Arrays.asList(testProduct2);

            when(productRepository.findByNameContainingIgnoreCase(searchTerm)).thenReturn(products);

            // When
            List<ProductResponse> result = productService.searchByName(searchTerm);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("Mouse", result.get(0).getName());

            verify(productRepository).findByNameContainingIgnoreCase(searchTerm);
        }

        @Test
        @DisplayName("Should return empty list when no products match search")
        void shouldReturnEmptyListWhenNoMatch() {
            // Given
            String searchTerm = "nonexistent";
            when(productRepository.findByNameContainingIgnoreCase(searchTerm)).thenReturn(List.of());

            // When
            List<ProductResponse> result = productService.searchByName(searchTerm);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(productRepository).findByNameContainingIgnoreCase(searchTerm);
        }

        @Test
        @DisplayName("Should find multiple products with similar names")
        void shouldFindMultipleProducts() {
            // Given
            Product product3 = new Product();
            product3.setId(3L);
            product3.setName("Laptop Stand");
            product3.setDescription("For laptops");
            product3.setPrice(new BigDecimal("49.99"));
            product3.setStock(20);
            product3.setActive(true);
            product3.setCreatedAt(LocalDateTime.now());
            product3.setUpdatedAt(LocalDateTime.now());

            String searchTerm = "lap";
            List<Product> products = Arrays.asList(testProduct1, product3);

            when(productRepository.findByNameContainingIgnoreCase(searchTerm)).thenReturn(products);

            // When
            List<ProductResponse> result = productService.searchByName(searchTerm);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals("Laptop", result.get(0).getName());
            assertEquals("Laptop Stand", result.get(1).getName());

            verify(productRepository).findByNameContainingIgnoreCase(searchTerm);
        }
    }

    // ==================== EDGE CASES TESTS ====================

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle product with zero stock")
        void shouldHandleZeroStock() {
            // Given
            Product productWithZeroStock = new Product();
            productWithZeroStock.setId(5L);
            productWithZeroStock.setName("Out of Stock Product");
            productWithZeroStock.setPrice(new BigDecimal("99.99"));
            productWithZeroStock.setStock(0);
            productWithZeroStock.setActive(true);
            productWithZeroStock.setCreatedAt(LocalDateTime.now());
            productWithZeroStock.setUpdatedAt(LocalDateTime.now());

            when(productRepository.findById(5L)).thenReturn(Optional.of(productWithZeroStock));

            // When
            ProductResponse result = productService.findById(5L);

            // Then
            assertNotNull(result);
            assertEquals(0, result.getStock());
        }

        @Test
        @DisplayName("Should handle product with very high price")
        void shouldHandleHighPrice() {
            // Given
            ProductRequest request = new ProductRequest();
            request.setName("Premium Product");
            request.setPrice(new BigDecimal("999999.99"));
            request.setStock(1);

            Product savedProduct = new Product();
            savedProduct.setId(6L);
            savedProduct.setName("Premium Product");
            savedProduct.setPrice(new BigDecimal("999999.99"));
            savedProduct.setStock(1);
            savedProduct.setCreatedAt(LocalDateTime.now());
            savedProduct.setUpdatedAt(LocalDateTime.now());

            when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

            // When
            ProductResponse result = productService.create(request);

            // Then
            assertNotNull(result);
            assertEquals(new BigDecimal("999999.99"), result.getPrice());
        }

        @Test
        @DisplayName("Should handle inactive product")
        void shouldHandleInactiveProduct() {
            // Given
            testProduct1.setActive(false);
            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct1));

            // When
            ProductResponse result = productService.findById(1L);

            // Then
            assertNotNull(result);
            assertFalse(result.getActive());
        }
    }
}
