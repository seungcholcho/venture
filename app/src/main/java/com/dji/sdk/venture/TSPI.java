package com.dji.sdk.venture;

import java.util.Date;

import dji.common.flightcontroller.FlightMode;

public class TSPI {
    private Date timestamp;
    private String gpsSignalStrength;
    private double Altitude;
    private double Latitude;
    private double Longitude;
    protected FlightMode flightState = null;


    public void updateTSPIdji(Date time, String GPSSignal, double altitude, double latitude, double longitude, FlightMode flightState){
        this.timestamp = time;
        this.gpsSignalStrength = GPSSignal;
        this.Altitude = altitude;
        this.Latitude = latitude;
        this.Longitude = longitude;
        this.flightState = flightState;
    }

    public void updateTSPIserver(String GPSSignal, double altitude, double latitude, double longitude){
        this.gpsSignalStrength = GPSSignal;
        this.Altitude = altitude;
        this.Latitude = latitude;
        this.Longitude = longitude;
    }

    public double getLatitude() {
        return Latitude;
    }

    public double getLongitude() {
        return Longitude;
    }
    public FlightMode getFlightState() {
        return flightState;
    }


}
