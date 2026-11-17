package com.dev.restaurant_backend.services.impl;

import com.dev.restaurant_backend.domain.ReviewCreateUpdateRequest;
import com.dev.restaurant_backend.domain.entities.Photo;
import com.dev.restaurant_backend.domain.entities.Restaurant;
import com.dev.restaurant_backend.domain.entities.Review;
import com.dev.restaurant_backend.domain.entities.User;
import com.dev.restaurant_backend.exceptions.RestaurantNotFoundException;
import com.dev.restaurant_backend.exceptions.ReviewNotAllowedException;
import com.dev.restaurant_backend.repositories.RestaurantRepository;
import com.dev.restaurant_backend.services.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final RestaurantRepository restaurantRepository;

    @Override
    public Review createReview(User author, String restaurantId, ReviewCreateUpdateRequest review) {
        Restaurant restaurant = getRestaurantOrThrow(restaurantId);

        boolean hasExistingReview = restaurant.getReviews().stream()
                .anyMatch(r -> r.getWrittenBy().getId().equals(author.getId()));

        if (hasExistingReview) {
            throw new ReviewNotAllowedException("User has already reviewed this restaurant");
        }

        LocalDateTime now = LocalDateTime.now();

        List<Photo> photos = review.getPhotoIds().stream().map(url -> {
            return Photo.builder()
                    .url(url)
                    .uploadDate(now)
                    .build();
        }).toList();

        String reviewId = UUID.randomUUID().toString();

        Review reviewToCreate = Review.builder()
                .id(reviewId)
                .content(review.getContent())
                .rating(review.getRating())
                .photos(photos)
                .datePosted(now)
                .lastEdited(now)
                .writtenBy(author)
                .build();

        restaurant.getReviews().add(reviewToCreate);

        updateRestaurantAverageRating(restaurant);

        Restaurant savedRestaurant = restaurantRepository.save(restaurant);

        return savedRestaurant.getReviews().stream()
                .filter(r -> reviewId.equals(r.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(("Error retrieving created review")));

    }

    // Reviews are nested inside the restaurant document, so sorting and paging happen in memory
    @Override
    public Page<Review> listReviews(String restaurantId, Pageable pageable) {
        Restaurant restaurant = getRestaurantOrThrow(restaurantId);
        List<Review> reviews = restaurant.getReviews();

        Sort sort = pageable.getSort();

        if (sort.isSorted()) {
            // Only the first sort rule is supported to keep it simple
            Sort.Order order = sort.iterator().next();
            String property = order.getProperty();
            boolean isAscending = order.getDirection().isAscending();

            Comparator<Review> comparator = switch (property) {
                case "rating" -> Comparator.comparingInt(Review::getRating);
                // datePosted and unknown fields both sort by date, so a bad sort param never fails
                default -> Comparator.comparing(Review::getDatePosted);
            };

            reviews.sort(isAscending ? comparator : comparator.reversed());
        } else {
            // Newest first when the client asks for no order
            reviews.sort(Comparator.comparing(Review::getDatePosted).reversed());
        }

        int start = (int) pageable.getOffset();

        // Page is past the last review: return an empty page instead of letting subList throw
        if (start >= reviews.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, reviews.size());
        }

        // The last page may hold fewer reviews than the page size
        int end = Math.min(start + pageable.getPageSize(), reviews.size());

        return new PageImpl<>(reviews.subList(start, end), pageable, reviews.size());
    }

    // Recalculates the average from all reviews; 0 when there are none
    private void updateRestaurantAverageRating(Restaurant restaurant) {
        List<Review> reviews = restaurant.getReviews();

        double averageRating = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);

        restaurant.setAverageRating((float) averageRating);
    }

    // Shared by all review operations that need an existing restaurant
    private Restaurant getRestaurantOrThrow(String restaurantId) {
        return restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RestaurantNotFoundException(
                        "Restaurant not found with id " + restaurantId
                ));
    }
}
