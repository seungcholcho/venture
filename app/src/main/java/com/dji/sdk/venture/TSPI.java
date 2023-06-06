package com.dji.sdk.venture;

import android.util.Log;

import java.util.Date;

import dji.common.flightcontroller.FlightMode;

public class TSPI {
    CircularQueue latQueue;
    CircularQueue lonQueue;
    private Date timestamp;
    private String gpsSignalStrength;
    private double Altitude;
    private double Latitude;
    private double Longitude;

    private double pitch;
    private double yaw;
    private double roll;

    private double vX;
    private double vY;
    private double vZ;
    private double xXYZ;

    protected FlightMode flightState = null;

    public TSPI(){
        this.latQueue = new CircularQueue(15);
        this.lonQueue = new CircularQueue(15);
    }
    public void updateTSPIdji(Date time, String GPSSignal, double altitude, double latitude, double longitude, double pitch, double yaw, double roll,
                              double vX, double vY, double vZ, double xXYZ, FlightMode flightState){

        this.timestamp = time;
        this.gpsSignalStrength = GPSSignal;
        this.Altitude = altitude;
        this.Latitude = latitude;
        this.Longitude = longitude;

        this.pitch = pitch;
        this.yaw = yaw;
        this.roll = roll;

        this.vX = vX;
        this.vY = vY;
        this.vZ = vZ;
        this.xXYZ = xXYZ;

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

    public double getLongitude(){
        return Longitude;
    }

    public void appendLatLonToQueue(double lat, double lon){
        Log.d("appending", "appending");
        latQueue.insert(lat);
        lonQueue.insert(lon);
    }

    public double getPitch() {
        return pitch;
    }

    public double getYaw() {
        return yaw;
    }
    public double getRoll() {
        return roll;
    }

    public double getvX(){return vX;}
    public double getvY(){return vY;}
    public double getvZ(){return vZ;}
    public double getxXYZ(){return xXYZ;}
    public FlightMode getFlightState() {
        return flightState;
    }


}
