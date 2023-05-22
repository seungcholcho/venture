package com.dji.sdk.venture;

import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Calendar;
import java.util.Date;

import dji.common.flightcontroller.FlightControllerState;
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

                    mTSPI.updateTSPI(timestamp,gpsSignalStrength,Altitude,Latitude,Longitude);
                }
            });
        }
    }
}
