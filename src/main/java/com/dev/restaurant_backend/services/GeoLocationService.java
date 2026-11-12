package com.dev.restaurant_backend.services;

import com.dev.restaurant_backend.domain.GeoLocation;
import com.dev.restaurant_backend.domain.entities.Address;

public interface GeoLocationService {
    GeoLocation getGeoLocation(Address address);
}
