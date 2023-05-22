package com.dji.sdk.venture;

import java.util.Date;

public class TSPI {
    private Date timestamp;
    private String gpsSignalStrength;
    private double currentAltitude;
    private double currentLatitude;
    private double currentLongitude;


    public void updateTSPI(Date time, String GPSSignal, double altitude, double latitude, double longitude){
        this.timestamp = time;
        this.gpsSignalStrength = GPSSignal;
        this.currentAltitude = altitude;
        this.currentLatitude = latitude;
        this.currentLongitude = longitude;
    }

    public double getLatitude() {
        return currentLatitude;
    }

    public double getLongitude() {
        return currentLongitude;
    }

}
