package com.scaler.ecommerce.dtos;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateProductDto(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 255) String image,
        @Size(max = 255) String description,
        @NotBlank @Size(max = 255) String categoryName,
        @NotNull @PositiveOrZero @Digits(integer = 12, fraction = 2) BigDecimal price) {}
