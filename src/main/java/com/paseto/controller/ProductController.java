package com.paseto.controller;

import com.paseto.dto.*;
import com.paseto.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product management APIs (PASETO Authentication required)")
@SecurityRequirement(name = "PASETO Authentication")
public class ProductController {

    private final ProductService productService;

    @Operation(
            summary = "Get all products",
            description = "Retrieve a list of all products"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Products retrieved successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - PASETO token required"
            )
    })
    @GetMapping
    public ResponseEntity<ProductListResponse> getAllProducts() {
        List<ProductResponse> products = productService.findAll();
        ProductListResponse response = new ProductListResponse(200, "OK", products);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get product by ID",
            description = "Retrieve a specific product by its ID"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Product found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Product not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - PASETO token required"
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<EntityResponse<ProductResponse>> getProductById(
            @Parameter(description = "Product ID", required = true)
            @PathVariable Long id) {
        ProductResponse data = productService.findById(id);
        EntityResponse<ProductResponse> response = EntityResponse.of(
                200,
                "Product found",
                data
        );
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Create new product",
            description = "Create a new product (requires PASETO authentication)"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Product created successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation error"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - PASETO token required"
            )
    })
    @PostMapping
    public ResponseEntity<EntityResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest request) {
        ProductResponse data = productService.create(request);
        EntityResponse<ProductResponse> response = EntityResponse.of(
                201,
                "Product created successfully",
                data
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Update product",
            description = "Update an existing product by ID"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Product updated successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Product not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation error"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - PASETO token required"
            )
    })
    @PutMapping("/{id}")
    public ResponseEntity<EntityResponse<ProductResponse>> updateProduct(
            @Parameter(description = "Product ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        ProductResponse data = productService.update(id, request);
        EntityResponse<ProductResponse> response = EntityResponse.of(
                200,
                "Product updated successfully",
                data
        );
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Delete product",
            description = "Delete a product by ID"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Product deleted successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Product not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - PASETO token required"
            )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<EntityResponse<DeleteResponse>> deleteProduct(
            @Parameter(description = "Product ID", required = true)
            @PathVariable Long id) {
        productService.delete(id);
        DeleteResponse data = DeleteResponse.of(id);
        EntityResponse<DeleteResponse> response = EntityResponse.of(
                200,
                "Product Delete success.",
                data
        );
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Search products by name",
            description = "Search for products containing the specified name (case-insensitive)"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Search completed successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - PASETO token required"
            )
    })
    @GetMapping("/search")
    public ResponseEntity<ProductListResponse> searchProducts(
            @Parameter(description = "Product name to search for", required = true)
            @RequestParam String name) {
        List<ProductResponse> products = productService.searchByName(name);
        ProductListResponse response = new ProductListResponse(200, "OK", products);
        return ResponseEntity.ok(response);
    }
}
