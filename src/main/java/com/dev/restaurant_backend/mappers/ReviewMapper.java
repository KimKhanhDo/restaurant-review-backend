package com.dev.restaurant_backend.mappers;

import com.dev.restaurant_backend.domain.ReviewCreateUpdateRequest;
import com.dev.restaurant_backend.domain.dtos.ReviewCreateUpdateRequestDto;
import com.dev.restaurant_backend.domain.dtos.ReviewDto;
import com.dev.restaurant_backend.domain.entities.Review;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ReviewMapper {

    ReviewCreateUpdateRequest toReviewCreateUpdateRequest(ReviewCreateUpdateRequestDto dto);

    ReviewDto toReviewDto(Review review);
}
