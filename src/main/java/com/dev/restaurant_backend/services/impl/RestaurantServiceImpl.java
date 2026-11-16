package com.dev.restaurant_backend.services.impl;

import com.dev.restaurant_backend.domain.GeoLocation;
import com.dev.restaurant_backend.domain.RestaurantCreateUpdateRequest;
import com.dev.restaurant_backend.domain.entities.Address;
import com.dev.restaurant_backend.domain.entities.Photo;
import com.dev.restaurant_backend.domain.entities.Restaurant;
import com.dev.restaurant_backend.repositories.RestaurantRepository;
import com.dev.restaurant_backend.services.GeoLocationService;
import com.dev.restaurant_backend.services.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final GeoLocationService geoLocationService;

    // Builds a new restaurant from the request, resolves its coordinates and saves it
    @Override
    public Restaurant createRestaurant(RestaurantCreateUpdateRequest request) {
        // Convert the address into coordinates that Elasticsearch can store
        Address address = request.getAddress();
        GeoLocation geoLocation = geoLocationService.getGeoLocation(address);
        GeoPoint geoPoint = new GeoPoint(geoLocation.getLatitude(), geoLocation.getLongitude());

        // Turn uploaded photo IDs into Photo objects
        List<String> photoIds = request.getPhotoIds();
        List<Photo> photos = photoIds.stream()
                .map(photoUrl -> Photo.builder()
                        .url(photoUrl)
                        .uploadDate(LocalDateTime.now())
                        .build()).toList();

        // New restaurants start with no reviews, so the rating is 0
        Restaurant restaurant = Restaurant.builder()
                .name(request.getName())
                .cuisineType(request.getCuisineType())
                .contactInformation(request.getContactInformation())
                .address(address)
                .geoLocation(geoPoint)
                .operatingHours(request.getOperatingHours())
                .averageRating(0f)
                .photos(photos)
                .build();

        return restaurantRepository.save(restaurant);
    }

    // Picks one search strategy based on which optional filters are given; the first match wins
    @Override
    public Page<Restaurant> searchRestaurants(
            String query, Float minRating, Float latitude, Float longitude,
            Float radius, Pageable pageable) {

        // Only a minimum rating was given: filter by rating
        if (minRating != null && (query == null || query.isEmpty())) {
            return restaurantRepository.findByAverageRatingGreaterThanEqual(minRating, pageable);
        }

        // No rating chosen: use 0 so every rating matches
        Float searchMinRating = minRating == null ? 0f : minRating;

        // Text was given: fuzzy search by name or cuisine type
        if (query != null && !query.trim().isEmpty()) {
            return restaurantRepository.findByQueryAndMinRating(query, searchMinRating, pageable);
        }

        // Location and radius were given: find restaurants nearby
        if (latitude != null && longitude != null && radius != null) {
            return restaurantRepository.findByLocationNear(latitude, longitude, radius, pageable);
        }

        // No filters: return all restaurants
        return restaurantRepository.findAll(pageable);
    }

    @Override
    public Optional<Restaurant> getRestaurantById(String id) {
        return restaurantRepository.findById(id);
    }


}
