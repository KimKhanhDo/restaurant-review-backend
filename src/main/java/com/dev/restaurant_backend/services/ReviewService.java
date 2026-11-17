package com.dev.restaurant_backend.services;

import com.dev.restaurant_backend.domain.ReviewCreateUpdateRequest;
import com.dev.restaurant_backend.domain.entities.Review;
import com.dev.restaurant_backend.domain.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewService {
    Review createReview(User author, String restaurantId, ReviewCreateUpdateRequest review);

    Page<Review> listReviews(String restaurantId, Pageable pageable);
}
