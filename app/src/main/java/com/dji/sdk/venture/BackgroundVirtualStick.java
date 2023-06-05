package com.dji.sdk.venture;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.TimerTask;

import dji.common.error.DJIError;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.util.CommonCallbacks;
import dji.sdk.flightcontroller.FlightController;

public class BackgroundVirtualStick extends TimerTask {

    private float pitch;
    private float roll;
    private float yaw;
    private float throttle;
    private float temp;

    FlightController flightController;

    Handler mainHandler;

    public BackgroundVirtualStick(FlightController mflightController) {
        this.pitch = (float) 2;
        this.roll = 0;
        this.yaw = -179;
        this.throttle = 0;
        this.temp = (float) 0;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.flightController = mflightController;
    }

    public void UpdateInputData(float pitch, float roll, float yaw, float throttle) {
        this.pitch = pitch;
        this.roll = roll;
        this.yaw = yaw;
        this.throttle = throttle;
    }

    public void mulPitch(float tmp) {
        this.pitch = pitch * tmp;
    }

    public void mulYaw(float tmp) {
        this.yaw = yaw * tmp;
    }

    public void mulRoll(float tmp) {
        this.roll = roll * tmp;
    }

    public void mulThrottle(float tmp) {
        this.throttle = throttle * tmp;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public void setRoll(float roll) {
        this.roll = roll;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public void setThrottle(float throttle) {
        this.throttle = throttle;
    }

    public float getPitch() {
        return this.pitch;
    }

    public float getRoll() {
        return this.roll;
    }

    public float getYaw() {
        return this.yaw;
    }

    public float getThrottle() {
        return this.throttle;
    }

    @Override
    public void run() {
        long time = System.currentTimeMillis();
        SimpleDateFormat simpl = new SimpleDateFormat("yyyy년 MM월 dd일 aa hh시 mm분 ss초");
        String s = simpl.format(time);
        Log.d("TaskLog", s);

        calculateTSPI();
        Log.d("TaskCalculate", String.valueOf(getPitch()));

        send();
        Log.d("TaskSend", "Succeed updated data");

    }

    public void calculateTSPI() {
        Log.d("calculateTSPI", "Run");

        if (getYaw()<180){
            setYaw(this.yaw + temp);
            temp = temp + (float) 1;
        } else if (getYaw() >= 180) {
            setYaw(180);
            setPitch(0);
        }

        //예상 움직임
        //하강 -> 승강 반복
//        setThrottle(this.throttle + 1);

//        if(getThrottle()<15){
//            setThrottle(getThrottle() + 5);
//        }else if (getThrottle()==15){
//            setThrottle(3);
//        }

//        if (getThrottle() > 4 || getThrottle() < -4){
//            Log.d("calculateTSPI","IF");
//            setThrottle(-4);
//        }

        //      예상 움직임
//      0.2초마다 회전
//        setYaw(this.yaw + (float) temp);
//        if (getYaw() > 180 || getYaw() < -180)
//            temp = temp* -1;

        //예상 움직임
        //후진 하강 -> 전진 상승 반복
//        setPitch(this.pitch + temp);
//        if (getPitch() > 4 || getPitch() < -4)
//            temp = temp* -1;


    }

    public void send() {
        Log.d("SendInputData", "Start send");

        flightController.sendVirtualStickFlightControlData(
                new FlightControlData(getRoll(), getPitch(), getYaw(), getThrottle()),
                new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        Log.d("onResult", "Succeed input data into virtual stick");
                    }
                });
    }


}
