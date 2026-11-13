package com.dev.restaurant_backend.domain.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantCreateUpdateRequestDto {

    @NotBlank(message = "Restaurant name is required")
    private String name;

    @NotBlank(message = "Cuisine type is required")
    private String cuisineType;

    @NotBlank(message = "Contact information is required")
    private String contactInformation;

    @NotNull(message = "Address is required")
    @Valid
    private AddressDto address;

    @NotNull(message = "Operating hours are required")
    @Valid
    private OperatingHoursDto operatingHours;

    @NotNull(message = "At least one photo ID is required")
    @Size(min = 1, message = "At least one photo ID is required")
    private List<String> photoIds;
}
