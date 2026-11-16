package com.dev.restaurant_backend.services.impl;

import com.dev.restaurant_backend.domain.GeoLocation;
import com.dev.restaurant_backend.domain.RestaurantCreateUpdateRequest;
import com.dev.restaurant_backend.domain.entities.Address;
import com.dev.restaurant_backend.domain.entities.Photo;
import com.dev.restaurant_backend.domain.entities.Restaurant;
import com.dev.restaurant_backend.exceptions.RestaurantNotFoundException;
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
        Address address = request.getAddress();
        GeoPoint geoPoint = toGeoPoint(address);
        List<Photo> photos = toPhotos(request.getPhotoIds());

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

    // Updates an existing restaurant; throws RestaurantNotFoundException if the id doesn't exist
    @Override
    public Restaurant updateRestaurant(String id, RestaurantCreateUpdateRequest request) {
        // Stop early: nothing to update if the restaurant doesn't exist
        Restaurant restaurant = getRestaurantById(id)
                .orElseThrow(() ->
                        new RestaurantNotFoundException("Restaurant with id " + id + " not found"));

        // Address may have changed, so recalculate the coordinates
        GeoPoint newGeoPoint = toGeoPoint(request.getAddress());
        List<Photo> photos = mergePhotos(restaurant.getPhotos(), request.getPhotoIds());

        // Rating, reviews and createdBy are kept as they are
        restaurant.setName(request.getName());
        restaurant.setCuisineType(request.getCuisineType());
        restaurant.setContactInformation(request.getContactInformation());
        restaurant.setAddress(request.getAddress());
        restaurant.setGeoLocation(newGeoPoint);
        restaurant.setOperatingHours(request.getOperatingHours());
        restaurant.setPhotos(photos);

        return restaurantRepository.save(restaurant);
    }

    @Override
    public void deleteRestaurant(String id) {
        restaurantRepository.deleteById(id);
    }

    // Converts an address into coordinates that Elasticsearch can store
    private GeoPoint toGeoPoint(Address address) {
        GeoLocation geoLocation = geoLocationService.getGeoLocation(address);
        return new GeoPoint(geoLocation.getLatitude(), geoLocation.getLongitude());
    }

    // Turns uploaded photo IDs into Photo objects
    private List<Photo> toPhotos(List<String> photoIds) {
        return photoIds.stream()
                .map(this::newPhoto)
                .toList();
    }

    // Keeps existing photos (and their upload dates); only new photo IDs get today's date
    private List<Photo> mergePhotos(List<Photo> existingPhotos, List<String> photoIds) {
        List<Photo> oldPhotos = existingPhotos == null ? List.of() : existingPhotos;

        return photoIds.stream()
                .map(photoUrl -> {
                    // Look for an old photo with the same URL
                    Optional<Photo> oldPhoto = oldPhotos.stream()
                            .filter(photo -> photoUrl.equals(photo.getUrl()))
                            .findFirst();

                    // Reuse it if found, otherwise create a new one
                    return oldPhoto.orElseGet(() -> newPhoto(photoUrl));
                })
                .toList();
    }

    // Creates a photo uploaded now
    private Photo newPhoto(String photoUrl) {
        return Photo.builder()
                .url(photoUrl)
                .uploadDate(LocalDateTime.now())
                .build();
    }
}
