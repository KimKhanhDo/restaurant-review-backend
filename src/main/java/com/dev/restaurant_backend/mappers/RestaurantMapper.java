package com.dev.restaurant_backend.mappers;

import com.dev.restaurant_backend.domain.RestaurantCreateUpdateRequest;
import com.dev.restaurant_backend.domain.dtos.GeoPointDto;
import com.dev.restaurant_backend.domain.dtos.RestaurantCreateUpdateRequestDto;
import com.dev.restaurant_backend.domain.dtos.RestaurantDto;
import com.dev.restaurant_backend.domain.entities.Restaurant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RestaurantMapper {

    RestaurantCreateUpdateRequest toRestaurantCreateUpdateRequest(RestaurantCreateUpdateRequestDto dto);

    RestaurantDto toRestaurantDto(Restaurant restaurant);

    @Mapping(target = "latitude", expression = "java(geoPoint.getLat())")
    @Mapping(target = "longitude", expression = "java(geoPoint.getLon())")
    GeoPointDto toGeoPointDto(GeoPoint geoPoint);
}

