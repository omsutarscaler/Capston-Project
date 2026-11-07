package com.scaler.ecommorce.capston.dtos;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateProductPriceDto(
        @NotNull @Positive Long id,
        @NotNull @PositiveOrZero @Digits(integer = 12, fraction = 2) BigDecimal price) {}
