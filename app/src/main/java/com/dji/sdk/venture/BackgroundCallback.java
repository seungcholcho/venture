package com.dji.sdk.venture;

import android.util.Log;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;

import dji.common.flightcontroller.Attitude;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.FlightMode;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.sdk.flightcontroller.FlightController;

public class BackgroundCallback extends Thread {
    TSPI mTSPI;
    FlightController flightController;

   private String currentTime;
   private String gpsSignalStrength;
   private double Altitude_seaTohome;
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

                    long time = System.currentTimeMillis();
                    SimpleDateFormat simpl = new SimpleDateFormat("yyyyMMddaahhmmss");
                    String currentTimeString = simpl.format(time);

                    currentTime = currentTimeString;
                    gpsSignalStrength = String.valueOf(djiFlightControllerCurrentState.getGPSSignalLevel());

                    Altitude_seaTohome = djiFlightControllerCurrentState.getTakeoffLocationAltitude();
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

                    mTSPI.updateTSPIdji(currentTime,gpsSignalStrength,Altitude_seaTohome,Altitude,Latitude,Longitude,pitch,yaw,roll,vX,vY,vZ,xXYZ,flightState);
                }
            });
        }
    }
}
