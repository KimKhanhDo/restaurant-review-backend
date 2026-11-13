package com.dev.restaurant_backend.controllers;

import com.dev.restaurant_backend.domain.RestaurantCreateUpdateRequest;
import com.dev.restaurant_backend.domain.dtos.RestaurantCreateUpdateRequestDto;
import com.dev.restaurant_backend.domain.dtos.RestaurantDto;
import com.dev.restaurant_backend.domain.entities.Restaurant;
import com.dev.restaurant_backend.mappers.RestaurantMapper;
import com.dev.restaurant_backend.services.RestaurantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/v1/restaurants")
public class RestaurantController {

    private final RestaurantService restaurantService;
    private final RestaurantMapper restaurantMapper;

    // Creates a new restaurant; the request body is validated before this method runs
    @PostMapping
    public ResponseEntity<RestaurantDto> createRestaurant(
            @Valid @RequestBody RestaurantCreateUpdateRequestDto requestDto
    ) {
        // Convert the API DTO into the request object the service works with
        RestaurantCreateUpdateRequest restaurantCreateUpdateRequest = restaurantMapper
                .toRestaurantCreateUpdateRequest(requestDto);

        // Save the restaurant, then convert it into a DTO for the response
        Restaurant restaurant = restaurantService.createRestaurant(restaurantCreateUpdateRequest);
        RestaurantDto createdRestaurantDto = restaurantMapper.toRestaurantDto(restaurant);
        return ResponseEntity.ok(createdRestaurantDto);
    }
}
