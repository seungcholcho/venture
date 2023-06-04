package com.dji.sdk.venture;

import android.util.Log;

import androidx.annotation.NonNull;

import org.bouncycastle.asn1.crmf.SinglePubInfo;

import java.util.Calendar;
import java.util.Date;

import dji.common.error.DJIError;
import dji.common.flightcontroller.Attitude;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.FlightMode;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.util.CommonCallbacks;
import dji.sdk.flightcontroller.FlightController;

public class SendInputData extends Thread {
    InputData inputData;
    FlightController flightController;

    private double pitch;
    private double yaw;
    private double roll;
    private double throttle;
   protected FlightMode flightState = null;

    public SendInputData(InputData inputData, FlightController mflightController) {
        this.inputData = inputData;
        this.flightController = mflightController;
    }

    public void send() {
        Log.d("SendInputData", "Start send");

        flightController.sendVirtualStickFlightControlData(
                new FlightControlData(inputData.getPitch(), inputData.getRoll(), inputData.getYaw(), inputData.getThrottle()),
                new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        Log.d("onResult","Succeed input data into virtual stick");

                    }
                });
    }
}
