package com.dji.sdk.venture;

import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Calendar;
import java.util.Date;

import dji.common.flightcontroller.Attitude;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.model.LocationCoordinate2D;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class TSPIlogger extends Thread{
   TSPI mTSPI;
   FlightController flightController;
   public TSPIlogger (TSPI mTSPI){
      this.mTSPI = mTSPI;
   }

   public void run(){
      initFlightControllerState();
   }
   private void initFlightControllerState(){

      Log.d("FlightControllerState", "connecting FlightController");

      try {
         flightController = ((Aircraft) DJISDKManager.getInstance().getProduct()).getFlightController();
      } catch (Exception e) {
         Log.d("FlightControllerState","not Connected");
      }

      if(flightController == null){
         Log.d("FlightControllerState","not Connected");
      } else {
         flightController.setStateCallback(new FlightControllerState.Callback() {
            @Override
            public void onUpdate(@NonNull FlightControllerState djiFlightControllerCurrentState) {
               //getAircraftLocation 객체
               LocationCoordinate3D locationCoordinate3D = djiFlightControllerCurrentState.getAircraftLocation();

               //getAttitiude 객체
               Attitude attitude = djiFlightControllerCurrentState.getAttitude();

               //현재 시간
               mTSPI.setTimestamp(Calendar.getInstance().getTime());
               //GPS 신호 레벨
               mTSPI.setGpsSignalStrength(String.valueOf(djiFlightControllerCurrentState.getGPSSignalLevel()));

               //getAircraftLocation에서 얻어오는 데이터
               mTSPI.setCurrentLatitude(locationCoordinate3D.getLatitude());
               mTSPI.setCurrentLongitude(locationCoordinate3D.getLongitude());

               //getAttitude에서 얻어오는 데이데
               mTSPI.setCurrentAltitude(locationCoordinate3D.getAltitude());

               mTSPI.setPitch(attitude.pitch);
               mTSPI.setYaw(attitude.yaw);

               //Log.d("(Thread)TSPILogger", "hello from logger");
            }
         });
      }
   }
}
