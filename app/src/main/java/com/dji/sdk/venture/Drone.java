package com.dji.sdk.venture;

import com.google.android.gms.maps.model.LatLng;

public class Drone {
    private String id;
    private LatLng location;

    public Drone(String id, LatLng location) {
        this.id = id;
        this.location = location;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LatLng getLocation() {
        return location;
    }

    public void setLocation(LatLng location) {
        this.location = location;
    }
}
