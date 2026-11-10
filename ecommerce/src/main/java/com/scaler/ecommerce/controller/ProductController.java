package com.scaler.ecommerce.controller;

import java.net.URI;
import java.util.List;

import com.scaler.ecommerce.dao.CategoryRepository;
import com.scaler.ecommerce.dao.CategoryRepository.CatalogCategory;
import com.scaler.ecommerce.config.OpenApiConfig;
import com.scaler.ecommerce.dtos.CreateProductDto;
import com.scaler.ecommerce.dtos.ProductPageResponse;
import com.scaler.ecommerce.dtos.ProductResponse;
import com.scaler.ecommerce.dtos.UpdateProductPriceDto;
import com.scaler.ecommerce.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/products")
@Tag(name = "Products")
public class ProductController {
    private final ProductService productService;
    private final CategoryRepository categories;

    public ProductController(ProductService productService, CategoryRepository categories) {
        this.productService = productService;
        this.categories = categories;
    }

    @GetMapping("/categories")
    public List<CatalogCategory> categories() {
        return categories.catalog();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a product by ID")
    public ProductResponse getProductById(@PathVariable @Positive long id) {
        return ProductResponse.from(productService.getProductById(id));
    }

    @GetMapping
    @Operation(summary = "List products", description = "Pages are zero-based and sorted by price, then ID.")
    public ProductPageResponse getProducts(@RequestParam(defaultValue = "0") @Min(0) int page,
                                           @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                                           @RequestParam(required = false) @Positive Long category,
                                           @RequestParam(defaultValue = "") @Size(max = 100) String q,
                                           @RequestParam(defaultValue = "price-asc")
                                           @Pattern(regexp = "price-asc|price-desc|newest") String sort) {
        validateOffset(page, size);
        if (category == null && q.isBlank() && "price-asc".equals(sort)) {
            return ProductPageResponse.from(productService.getAllProducts(page, size));
        }
        return ProductPageResponse.from(productService.browse(category, q, sort, page, size));
    }

    @PostMapping
    @Operation(summary = "Create a product", security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
    @ApiResponse(responseCode = "201", description = "Product created", useReturnTypeSchema = true)
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductDto request) {
        var product = productService.createProduct(request.title(), request.image(), request.description(),
                request.price().doubleValue(), request.categoryName());
        return ResponseEntity.created(URI.create("/products/" + product.getId())).body(ProductResponse.from(product));
    }

    @PutMapping
    @Operation(summary = "Update a product price", security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
    public ProductResponse updatePrice(@Valid @RequestBody UpdateProductPriceDto request) {
        return ProductResponse.from(productService.updateProductPrice(request.id(), request.price().doubleValue()));
    }

    @GetMapping("/title")
    @Operation(summary = "Find products by title", description = "Returns a zero-based page of exact title matches.")
    public ProductPageResponse getByTitle(@RequestParam @NotBlank @Size(max = 255) String title,
                                          @RequestParam(defaultValue = "0") @Min(0) int page,
                                          @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        validateOffset(page, size);
        return ProductPageResponse.from(productService.findByProductTitle(title, page, size));
    }

    private static void validateOffset(int page, int size) {
        if ((long) page * size > Integer.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page offset is too large");
        }
    }
}
