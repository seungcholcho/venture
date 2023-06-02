package com.dji.sdk.venture;

public class InputData {

    private float pitch;
    private float roll;
    private float yaw;
    private float throttle;

    InputData(float pitch, float roll, float yaw, float throttle){
        this.pitch = pitch;
        this.roll = roll;
        this.yaw = yaw;
        this.throttle = throttle;
    }

    public void UpdateInputData(float pitch, float roll, float yaw, float throttle){
        this.pitch = pitch;
        this.roll = roll;
        this.yaw = yaw;
        this.throttle = throttle;
    }

    public void addPitch(float tmp){
        this.pitch = pitch + tmp;
    }

    public void addRoll(float tmp){
        this.roll = roll + tmp;
    }

    public void addYaw(float tmp){
        this.yaw = yaw + tmp;
    }

    public void addThrottle(float tmp){
        this.throttle = throttle + tmp;
    }


    public void setPitch(float pitch){
        this.pitch = pitch;
    }

    public void setRoll(float roll){
        this.roll = roll;
    }

    public void setYaw(float yaw){
        this.yaw = yaw;
    }

    public void setThrottle(float throttle){
        this.throttle = throttle;
    }

    public float getPitch(){
        return this.pitch;
    }

    public float getRoll(){
        return this.roll;
    }

    public float getYaw(){
        return this.yaw;
    }

    public float getThrottle(){
        return this.throttle;
    }


}
