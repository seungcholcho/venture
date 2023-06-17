package com.dji.sdk.venture;

import android.util.Log;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;

import dji.common.flightcontroller.Attitude;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.FlightMode;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.sdk.flightcontroller.FlightController;

//When using the API provided by DJI, a callback function must be employed.
//However, if this callback is used directly on the main page, it can lead to priority conflicts.
//To avoid this issue, a separate background thread is created.
public class BackgroundCallback extends Thread {
    TSPI mTSPI;
    FlightController flightController;
    protected FlightMode flightState = null;

    private String currentTime;
    private String gpsSignalStrength;
    private double Altitude_seaTohome;
    private double Altitude_homeTodrone;
    private double Latitude;
    private double Longitude;

    private double pitch;
    private double yaw;
    private double roll;

    private double vX;
    private double vY;
    private double vZ;
    private double xXYZ;


    public BackgroundCallback(TSPI mTSPI, FlightController mflightController) {
        this.mTSPI = mTSPI;
        this.flightController = mflightController;
    }

    //Function that runs when the class is run.
    public void run() {
        getFlightControllerState();
    }

    //In order to use the API provided by DJI, the request is sent once every 0.1 seconds using the Callback function.
    //Return values include the connection status of the controller and TSPI data of the drone.
    private void getFlightControllerState() {

        if (flightController == null) {
            Log.d("FlightControllerState", "not Connected");
        } else {
            flightController.setStateCallback(new FlightControllerState.Callback() {
                @Override
                public void onUpdate(@NonNull FlightControllerState djiFlightControllerCurrentState) {
                    //getAircraftLocation 객체
                    LocationCoordinate3D locationCoordinate3D = djiFlightControllerCurrentState.getAircraftLocation();

                    long time = System.currentTimeMillis();
                    SimpleDateFormat simpl = new SimpleDateFormat("yyMMddHHmmss");
                    String currentTimeString = simpl.format(time);

                    currentTime = currentTimeString;
                    gpsSignalStrength = String.valueOf(djiFlightControllerCurrentState.getGPSSignalLevel());

                    Altitude_seaTohome = djiFlightControllerCurrentState.getTakeoffLocationAltitude();
                    Altitude_homeTodrone = locationCoordinate3D.getAltitude();

                    Latitude = locationCoordinate3D.getLatitude();
                    Longitude = locationCoordinate3D.getLongitude();
                    flightState = djiFlightControllerCurrentState.getFlightMode();

                    vX = djiFlightControllerCurrentState.getVelocityX();
                    vY = djiFlightControllerCurrentState.getVelocityY();
                    vZ = djiFlightControllerCurrentState.getVelocityZ();
                    xXYZ = (float) Math.sqrt((vX * vX) + (vY * vY) + (vZ * vZ));

                    Attitude attitude = djiFlightControllerCurrentState.getAttitude();

                    pitch = attitude.pitch;
                    yaw = attitude.yaw;
                    roll = attitude.roll;

                    mTSPI.updateTSPIdji(currentTime, gpsSignalStrength, Altitude_seaTohome, Altitude_homeTodrone, Latitude, Longitude, pitch, yaw, roll, vX, vY, vZ, xXYZ, flightState);
                }
            });
        }
    }
}
