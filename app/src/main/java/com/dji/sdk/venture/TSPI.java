package com.dji.sdk.venture;

import java.util.Date;

import dji.common.flightcontroller.FlightMode;

public class TSPI {
    private Date timestamp;
    private String gpsSignalStrength;
    private double currentAltitude;
    private double currentLatitude;
    private double currentLongitude;
    protected FlightMode flightState = null;

    public void updateTSPI(Date time, String GPSSignal, double altitude, double latitude, double longitude, FlightMode flightState){
        this.timestamp = time;
        this.gpsSignalStrength = GPSSignal;
        this.currentAltitude = altitude;
        this.currentLatitude = latitude;
        this.currentLongitude = longitude;
        this.flightState = flightState;
    }

    public double getLatitude() {
        return currentLatitude;
    }

    public double getLongitude() {
        return currentLongitude;
    }
    public FlightMode getFlightState() {
        return flightState;
    }


}
