package com.dev.restaurant_backend.services.impl;


import com.dev.restaurant_backend.domain.GeoLocation;
import com.dev.restaurant_backend.domain.entities.Address;
import com.dev.restaurant_backend.services.GeoLocationService;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class RandomMelbourneLocationService implements GeoLocationService {

    // Bounding box around inner and middle Melbourne (southern hemisphere, so latitude is negative)
    private static final double MIN_LATITUDE = -37.95;
    private static final double MAX_LATITUDE = -37.70;
    private static final double MIN_LONGITUDE = 144.85;
    private static final double MAX_LONGITUDE = 145.15;

    @Override
    public GeoLocation getGeoLocation(Address address) {
        Random random = new Random();
        double latitude = MIN_LATITUDE + random.nextDouble() * (MAX_LATITUDE - MIN_LATITUDE);
        double longitude = MIN_LONGITUDE + random.nextDouble() * (MAX_LONGITUDE - MIN_LONGITUDE);

        return GeoLocation.builder()
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }
}
