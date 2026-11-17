package com.dev.restaurant_backend.services.impl;

import com.dev.restaurant_backend.domain.ReviewCreateUpdateRequest;
import com.dev.restaurant_backend.domain.entities.Photo;
import com.dev.restaurant_backend.domain.entities.Restaurant;
import com.dev.restaurant_backend.domain.entities.Review;
import com.dev.restaurant_backend.domain.entities.User;
import com.dev.restaurant_backend.exceptions.RestaurantNotFoundException;
import com.dev.restaurant_backend.exceptions.ReviewNotAllowedException;
import com.dev.restaurant_backend.exceptions.ReviewNotFoundException;
import com.dev.restaurant_backend.repositories.RestaurantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
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

    private final User binh = User.builder()
            .id("user-binh")
            .username("binh")
            .givenName("Binh")
            .familyName("Tran")
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

    @Test
    void listReviews_whenNoSort_returnsNewestFirst() {
        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurantWithFiveReviews()));

        Page<Review> page = reviewService.listReviews(RESTAURANT_ID, PageRequest.of(0, 20));

        assertThat(page.getContent()).extracting(Review::getId)
                .containsExactly("review-e", "review-d", "review-c", "review-b", "review-a");
    }

    @Test
    void listReviews_whenSortedByRatingDesc_returnsHighestRatedFirst() {
        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurantWithFiveReviews()));

        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "rating"));
        Page<Review> page = reviewService.listReviews(RESTAURANT_ID, pageable);

        assertThat(page.getContent()).extracting(Review::getId)
                .containsExactly("review-a", "review-c", "review-b", "review-d", "review-e");
    }

    @Test
    void listReviews_whenUnknownSortField_sortsByDatePosted() {
        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurantWithFiveReviews()));

        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "content"));
        Page<Review> page = reviewService.listReviews(RESTAURANT_ID, pageable);

        assertThat(page.getContent()).extracting(Review::getId)
                .containsExactly("review-a", "review-b", "review-c", "review-d", "review-e");
    }

    @Test
    void listReviews_whenLastPage_returnsRemainingReviews() {
        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurantWithFiveReviews()));

        // Newest first: [E, D] [C, B] [A], so page 2 only holds A
        Page<Review> page = reviewService.listReviews(RESTAURANT_ID, PageRequest.of(2, 2));

        assertThat(page.getContent()).extracting(Review::getId).containsExactly("review-a");
        assertThat(page.getTotalElements()).isEqualTo(5);
    }

    @Test
    void listReviews_whenPageBeyondReviews_returnsEmptyPage() {
        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurantWithFiveReviews()));

        Page<Review> page = reviewService.listReviews(RESTAURANT_ID, PageRequest.of(10, 2));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(5);
    }

    @Test
    void getReviewById_whenReviewExists_returnsReview() {
        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurantWithFiveReviews()));

        Optional<Review> found = reviewService.getReviewById(RESTAURANT_ID, "review-c");

        assertThat(found).map(Review::getId).contains("review-c");
    }

    @Test
    void getReviewById_whenReviewMissing_returnsEmpty() {
        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurantWithFiveReviews()));

        Optional<Review> found = reviewService.getReviewById(RESTAURANT_ID, "review-unknown");

        assertThat(found).isEmpty();
    }

    @Test
    void updateReview_whenValid_updatesAndReturnsReview() {
        LocalDateTime postedAt = LocalDateTime.now().minusHours(1);
        Restaurant restaurant = restaurantWithReviews(reviewPostedAt(kim, 3, postedAt));
        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));

        ReviewCreateUpdateRequest request = request("Better than I remembered", 5, List.of("pho-v2.jpg"));

        Review updated = reviewService.updateReview(kim, RESTAURANT_ID, "review-user-kim", request);

        assertThat(updated.getContent()).isEqualTo("Better than I remembered");
        assertThat(updated.getRating()).isEqualTo(5);
        assertThat(updated.getPhotos()).extracting(Photo::getUrl).containsExactly("pho-v2.jpg");
        // datePosted must not move, otherwise editing could extend the 48-hour window
        assertThat(updated.getDatePosted()).isEqualTo(postedAt);
        assertThat(updated.getLastEdited()).isAfter(postedAt);
        verify(restaurantRepository).save(restaurant);
    }

    @Test
    void updateReview_whenValid_recalculatesAverageRating() {
        Restaurant restaurant = restaurantWithReviews(
                reviewPostedAt(an, 4, LocalDateTime.now().minusDays(5)),
                reviewPostedAt(kim, 3, LocalDateTime.now().minusHours(1)));
        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));

        reviewService.updateReview(kim, RESTAURANT_ID, "review-user-kim", request("Better than I remembered", 5, List.of()));

        // (4 + 5) / 2
        assertThat(restaurant.getAverageRating()).isEqualTo(4.5f);
    }

    @Test
    void updateReview_whenValid_keepsReviewPosition() {
        Restaurant restaurant = restaurantWithReviews(
                reviewPostedAt(an, 4, LocalDateTime.now().minusDays(5)),
                reviewPostedAt(kim, 3, LocalDateTime.now().minusHours(1)),
                reviewPostedAt(binh, 5, LocalDateTime.now().minusDays(2)));
        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));

        reviewService.updateReview(kim, RESTAURANT_ID, "review-user-kim", request("Better than I remembered", 5, List.of()));

        assertThat(restaurant.getReviews()).extracting(Review::getId)
                .containsExactly("review-user-an", "review-user-kim", "review-user-binh");
    }

    @Test
    void updateReview_whenReviewMissing_throwsReviewNotFoundException() {
        Restaurant restaurant = restaurantWithReviews(reviewPostedAt(kim, 3, LocalDateTime.now().minusHours(1)));
        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));

        ReviewCreateUpdateRequest request = request("Better than I remembered", 5, List.of());

        assertThatThrownBy(() -> reviewService.updateReview(kim, RESTAURANT_ID, "review-unknown", request))
                .isInstanceOf(ReviewNotFoundException.class);
        verify(restaurantRepository, never()).save(any());
    }

    @Test
    void updateReview_whenNotAuthor_throwsReviewNotAllowedException() {
        Restaurant restaurant = restaurantWithReviews(reviewPostedAt(kim, 3, LocalDateTime.now().minusHours(1)));
        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));

        ReviewCreateUpdateRequest request = request("Not my review", 1, List.of());

        // An tries to edit Kim's review
        assertThatThrownBy(() -> reviewService.updateReview(an, RESTAURANT_ID, "review-user-kim", request))
                .isInstanceOf(ReviewNotAllowedException.class);
        verify(restaurantRepository, never()).save(any());
    }

    @Test
    void updateReview_whenOlderThan48Hours_throwsReviewNotAllowedException() {
        Restaurant restaurant = restaurantWithReviews(reviewPostedAt(kim, 3, LocalDateTime.now().minusHours(49)));
        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));

        ReviewCreateUpdateRequest request = request("Too late to change", 5, List.of());

        assertThatThrownBy(() -> reviewService.updateReview(kim, RESTAURANT_ID, "review-user-kim", request))
                .isInstanceOf(ReviewNotAllowedException.class);
        verify(restaurantRepository, never()).save(any());
    }

    @Test
    void deleteReview_whenAuthor_removesReview() {
        Restaurant restaurant = restaurantWithReviews(
                reviewPostedAt(an, 4, LocalDateTime.now().minusDays(5)),
                reviewPostedAt(kim, 3, LocalDateTime.now().minusHours(1)),
                reviewPostedAt(binh, 5, LocalDateTime.now().minusDays(2)));
        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));

        reviewService.deleteReview(kim, RESTAURANT_ID, "review-user-kim");

        assertThat(restaurant.getReviews()).extracting(Review::getId)
                .containsExactly("review-user-an", "review-user-binh");
        verify(restaurantRepository).save(restaurant);
    }

    @Test
    void deleteReview_whenAuthor_recalculatesAverageRating() {
        Restaurant restaurant = restaurantWithReviews(
                reviewPostedAt(an, 4, LocalDateTime.now().minusDays(5)),
                reviewPostedAt(kim, 3, LocalDateTime.now().minusHours(1)),
                reviewPostedAt(binh, 5, LocalDateTime.now().minusDays(2)));
        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));

        reviewService.deleteReview(kim, RESTAURANT_ID, "review-user-kim");

        // (4 + 5) / 2
        assertThat(restaurant.getAverageRating()).isEqualTo(4.5f);
    }

    @Test
    void deleteReview_whenLastReview_resetsAverageRatingToZero() {
        Restaurant restaurant = restaurantWithReviews(reviewPostedAt(kim, 3, LocalDateTime.now().minusHours(1)));
        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));

        reviewService.deleteReview(kim, RESTAURANT_ID, "review-user-kim");

        assertThat(restaurant.getReviews()).isEmpty();
        assertThat(restaurant.getAverageRating()).isEqualTo(0.0f);
    }

    @Test
    void deleteReview_whenOlderThan48Hours_stillDeletes() {
        // Unlike editing, deleting your own review has no time limit
        Restaurant restaurant = restaurantWithReviews(reviewPostedAt(kim, 3, LocalDateTime.now().minusDays(30)));
        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));

        reviewService.deleteReview(kim, RESTAURANT_ID, "review-user-kim");

        assertThat(restaurant.getReviews()).isEmpty();
        verify(restaurantRepository).save(restaurant);
    }

    @Test
    void deleteReview_whenReviewMissing_throwsReviewNotFoundException() {
        Restaurant restaurant = restaurantWithReviews(reviewPostedAt(kim, 3, LocalDateTime.now().minusHours(1)));
        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));

        assertThatThrownBy(() -> reviewService.deleteReview(kim, RESTAURANT_ID, "review-unknown"))
                .isInstanceOf(ReviewNotFoundException.class);
        verify(restaurantRepository, never()).save(any());
    }

    @Test
    void deleteReview_whenNotAuthor_throwsReviewNotAllowedException() {
        Restaurant restaurant = restaurantWithReviews(reviewPostedAt(kim, 3, LocalDateTime.now().minusHours(1)));
        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));

        // An tries to delete Kim's review
        assertThatThrownBy(() -> reviewService.deleteReview(an, RESTAURANT_ID, "review-user-kim"))
                .isInstanceOf(ReviewNotAllowedException.class);
        assertThat(restaurant.getReviews()).hasSize(1);
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

    // Stored out of order on purpose, so the tests prove the service really sorts
    private Restaurant restaurantWithFiveReviews() {
        return restaurantWithReviews(
                review("review-c", 4, LocalDateTime.of(2026, 11, 10, 12, 0)),
                review("review-a", 5, LocalDateTime.of(2026, 11, 1, 12, 0)),
                review("review-e", 1, LocalDateTime.of(2026, 11, 15, 12, 0)),
                review("review-b", 3, LocalDateTime.of(2026, 11, 5, 12, 0)),
                review("review-d", 2, LocalDateTime.of(2026, 11, 12, 12, 0))
        );
    }

    private Review review(String id, int rating, LocalDateTime datePosted) {
        return Review.builder()
                .id(id)
                .content("Review " + id)
                .rating(rating)
                .datePosted(datePosted)
                .lastEdited(datePosted)
                .writtenBy(an)
                .build();
    }

    // Id follows the author ("review-user-kim") so tests can refer to a review by who wrote it
    private Review reviewPostedAt(User author, int rating, LocalDateTime datePosted) {
        return Review.builder()
                .id("review-" + author.getId())
                .content("Review by " + author.getUsername())
                .rating(rating)
                .datePosted(datePosted)
                .lastEdited(datePosted)
                .writtenBy(author)
                .build();
    }
}
