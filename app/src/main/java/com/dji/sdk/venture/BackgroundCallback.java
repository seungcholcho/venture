package com.dji.sdk.venture;

import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Calendar;
import java.util.Date;

import dji.common.flightcontroller.Attitude;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.FlightMode;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.sdk.flightcontroller.FlightController;

public class BackgroundCallback extends Thread {
    TSPI mTSPI;
    FlightController flightController;

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

    public BackgroundCallback(TSPI mTSPI, FlightController mflightController) {
        this.mTSPI = mTSPI;
        this.flightController = mflightController;
    }

    public void run() {
        initFlightControllerState();
    }

    private void initFlightControllerState() {

        if (flightController == null) {
            Log.d("FlightControllerState", "not Connected");
        } else {
            flightController.setStateCallback(new FlightControllerState.Callback() {
                @Override
                public void onUpdate(@NonNull FlightControllerState djiFlightControllerCurrentState) {
                    //getAircraftLocation 객체
                    LocationCoordinate3D locationCoordinate3D = djiFlightControllerCurrentState.getAircraftLocation();

                    timestamp = Calendar.getInstance().getTime();
                    gpsSignalStrength = String.valueOf(djiFlightControllerCurrentState.getGPSSignalLevel());
                    Altitude = locationCoordinate3D.getAltitude();
                    Latitude = locationCoordinate3D.getLatitude();
                    Longitude = locationCoordinate3D.getLongitude();
                    flightState = djiFlightControllerCurrentState.getFlightMode();

                    vX = djiFlightControllerCurrentState.getVelocityX();
                    vY = djiFlightControllerCurrentState.getVelocityY();
                    vZ = djiFlightControllerCurrentState.getVelocityZ();
                    xXYZ = (float) Math.sqrt((vX*vX) + (vY*vY) + (vZ*vZ));

                    Attitude attitude = djiFlightControllerCurrentState.getAttitude();

                    pitch = attitude.pitch;
                    yaw = attitude.yaw;
                    roll = attitude.roll;

                    mTSPI.updateTSPIdji(timestamp,gpsSignalStrength,Altitude,Latitude,Longitude,pitch,yaw,roll,vX,vY,vZ,xXYZ,flightState);
                }
            });
        }
    }
}
