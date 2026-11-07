package com.scaler.ecommorce.capston.controller;

import java.net.URI;

import com.scaler.ecommorce.capston.dtos.CreateProductDto;
import com.scaler.ecommorce.capston.dtos.ProductPageResponse;
import com.scaler.ecommorce.capston.dtos.ProductResponse;
import com.scaler.ecommorce.capston.dtos.UpdateProductPriceDto;
import com.scaler.ecommorce.capston.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
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
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{id}")
    public ProductResponse getProductById(@PathVariable @Positive long id) {
        return ProductResponse.from(productService.getProductById(id));
    }

    @GetMapping
    public ProductPageResponse getProducts(@RequestParam(defaultValue = "0") @Min(0) int page,
                                           @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        validateOffset(page, size);
        return ProductPageResponse.from(productService.getAllProducts(page, size));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductDto request) {
        var product = productService.createProduct(request.title(), request.image(), request.description(),
                request.price().doubleValue(), request.categoryName());
        return ResponseEntity.created(URI.create("/products/" + product.getId())).body(ProductResponse.from(product));
    }

    @PutMapping
    public ProductResponse updatePrice(@Valid @RequestBody UpdateProductPriceDto request) {
        return ProductResponse.from(productService.updateProductPrice(request.id(), request.price().doubleValue()));
    }

    @GetMapping("/title")
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
