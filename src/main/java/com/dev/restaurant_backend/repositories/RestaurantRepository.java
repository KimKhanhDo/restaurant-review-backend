package com.dev.restaurant_backend.repositories;

import com.dev.restaurant_backend.domain.entities.Restaurant;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RestaurantRepository extends ElasticsearchRepository<Restaurant, String> {
// TODO: Custom queries
}
