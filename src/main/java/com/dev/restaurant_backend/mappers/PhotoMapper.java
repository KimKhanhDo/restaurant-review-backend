package com.dev.restaurant_backend.mappers;

import com.dev.restaurant_backend.domain.dtos.PhotoDto;
import com.dev.restaurant_backend.domain.entities.Photo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PhotoMapper {

    PhotoDto toDto(Photo photo);
}
