package com.dev.restaurant_backend.services.impl;

import com.dev.restaurant_backend.domain.ReviewCreateUpdateRequest;
import com.dev.restaurant_backend.domain.entities.Photo;
import com.dev.restaurant_backend.domain.entities.Restaurant;
import com.dev.restaurant_backend.domain.entities.Review;
import com.dev.restaurant_backend.domain.entities.User;
import com.dev.restaurant_backend.exceptions.RestaurantNotFoundException;
import com.dev.restaurant_backend.exceptions.ReviewNotAllowedException;
import com.dev.restaurant_backend.repositories.RestaurantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    private static final String RESTAURANT_ID = "pho-saigon";

    @Mock
    private RestaurantRepository restaurantRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private final User kim = User.builder()
            .id("user-kim")
            .username("kim")
            .givenName("Kim")
            .familyName("Do")
            .build();

    private final User an = User.builder()
            .id("user-an")
            .username("an")
            .givenName("An")
            .familyName("Nguyen")
            .build();

    @Test
    void createReview_whenValid_returnsCreatedReview() {
        Restaurant restaurant = restaurantWithReviews();
        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        stubSaveToReturnInput();

        ReviewCreateUpdateRequest request = request("Amazing pho, rich broth", 5, List.of("pho.jpg"));

        Review created = reviewService.createReview(kim, RESTAURANT_ID, request);

        assertThat(created.getId()).isNotBlank();
        assertThat(created.getContent()).isEqualTo("Amazing pho, rich broth");
        assertThat(created.getRating()).isEqualTo(5);
        assertThat(created.getWrittenBy()).isEqualTo(kim);
        assertThat(created.getPhotos()).extracting(Photo::getUrl).containsExactly("pho.jpg");
        assertThat(created.getDatePosted()).isNotNull().isEqualTo(created.getLastEdited());
        verify(restaurantRepository).save(restaurant);
    }

    @Test
    void createReview_whenOtherReviewsExist_recalculatesAverageRating() {
        Restaurant restaurant = restaurantWithReviews(review(an, 3));
        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        stubSaveToReturnInput();

        reviewService.createReview(kim, RESTAURANT_ID, request("Great pho", 5, List.of()));

        // Capture what the service handed to save() to check the stored average
        ArgumentCaptor<Restaurant> savedCaptor = ArgumentCaptor.forClass(Restaurant.class);
        verify(restaurantRepository).save(savedCaptor.capture());
        Restaurant saved = savedCaptor.getValue();

        assertThat(saved.getReviews()).hasSize(2);
        assertThat(saved.getAverageRating()).isEqualTo(4.0f);
    }

    @Test
    void createReview_whenUserAlreadyReviewed_throwsReviewNotAllowedException() {
        Restaurant restaurant = restaurantWithReviews(review(kim, 4));
        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));

        ReviewCreateUpdateRequest request = request("Coming back for more", 5, List.of());

        assertThatThrownBy(() -> reviewService.createReview(kim, RESTAURANT_ID, request))
                .isInstanceOf(ReviewNotAllowedException.class);
        verify(restaurantRepository, never()).save(any());
    }

    @Test
    void createReview_whenRestaurantNotFound_throwsRestaurantNotFoundException() {
        when(restaurantRepository.findById("unknown-restaurant")).thenReturn(Optional.empty());

        ReviewCreateUpdateRequest request = request("Great pho", 5, List.of());

        assertThatThrownBy(() -> reviewService.createReview(kim, "unknown-restaurant", request))
                .isInstanceOf(RestaurantNotFoundException.class);
        verify(restaurantRepository, never()).save(any());
    }

    // Behaves like Elasticsearch: returns the restaurant exactly as it was saved
    private void stubSaveToReturnInput() {
        when(restaurantRepository.save(any(Restaurant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Restaurant restaurantWithReviews(Review... reviews) {
        return Restaurant.builder()
                .id(RESTAURANT_ID)
                .name("Pho Saigon")
                // Mutable list because the service adds the new review to it
                .reviews(new ArrayList<>(List.of(reviews)))
                .build();
    }

    private Review review(User author, int rating) {
        return Review.builder()
                .id("review-" + author.getId())
                .content("Existing review")
                .rating(rating)
                .writtenBy(author)
                .build();
    }

    private ReviewCreateUpdateRequest request(String content, int rating, List<String> photoIds) {
        return ReviewCreateUpdateRequest.builder()
                .content(content)
                .rating(rating)
                .photoIds(photoIds)
                .build();
    }
}
