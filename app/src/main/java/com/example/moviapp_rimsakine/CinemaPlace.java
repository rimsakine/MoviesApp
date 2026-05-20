package com.example.moviapp_rimsakine;

import com.google.android.gms.maps.model.LatLng;

public class CinemaPlace {
    private final String name;
    private final String address;
    private final LatLng position;
    private final float distanceMeters;

    public CinemaPlace(String name, String address, LatLng position, float distanceMeters) {
        this.name = name;
        this.address = address;
        this.position = position;
        this.distanceMeters = distanceMeters;
    }

    public String getName() { return name; }
    public String getAddress() { return address; }
    public LatLng getPosition() { return position; }
    public float getDistanceMeters() { return distanceMeters; }
}
