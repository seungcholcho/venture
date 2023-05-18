package com.dji.sdk.venture;

import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Calendar;
import java.util.Date;

import dji.common.flightcontroller.Attitude;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class TSPIlogger extends Thread{
   TSPI mTSPI;

   public TSPIlogger (TSPI mTSPI){
      this.mTSPI = mTSPI;
   }

   public void run(){
      initFlightControllerState();
   }
   private void initFlightControllerState(){

      Log.d("FlightControllerState", "connecting FlightController");

      try{
      FlightController flightController = ((Aircraft) DJISDKManager.getInstance().getProduct()).getFlightController();
      flightController.setStateCallback(new FlightControllerState.Callback() {
         @Override
         public void onUpdate(@NonNull FlightControllerState djiFlightControllerCurrentState) {
            LocationCoordinate3D locationCoordinate3D = djiFlightControllerCurrentState.getAircraftLocation();
            Attitude attitude = djiFlightControllerCurrentState.getAttitude();
            mTSPI.setTimestamp(Calendar.getInstance().getTime());
            mTSPI.setGpsSignalStrength(String.valueOf(djiFlightControllerCurrentState.getGPSSignalLevel()));

            mTSPI.setCurrentLatitude(locationCoordinate3D.getLatitude());
            mTSPI.setCurrentLongitude(locationCoordinate3D.getLongitude());
            mTSPI.setPitch(attitude.pitch);
            mTSPI.setYaw(attitude.yaw);

            Log.d("(Thread)TSPILogger", "hello from logger");
         }
      });
      } catch (Exception e) {
         Log.d("FlightControllerState","not Connected");
         throw new RuntimeException(e);
      }
   }
}
