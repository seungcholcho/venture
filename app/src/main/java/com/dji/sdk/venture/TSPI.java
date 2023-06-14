package com.dji.sdk.venture;

import android.content.Context;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import dji.common.flightcontroller.FlightMode;

public class TSPI {
    CircularQueue latQueue;
    CircularQueue lonQueue;
    private int queSize = 2;
    private int taskInterval;
    private String timestamp;
    private String gpsSignalStrength;
    private double Altitude_seaTohome;
    private double Altitude;
    private double Latitude;
    private double Longitude;

    //trajectory
    private double targetLat;
    private double targetLon;

    private double pitch;
    private double yaw;
    private double roll;

    private double vX;
    private double vY;
    private double vZ;
    private double xXYZ;

    private boolean mission;
    private String databaseTime;

    protected FlightMode flightState = null;

    //Write Log
    StringBuffer loggedTSPI;
    private String header;

    public TSPI(){
        this.latQueue = new CircularQueue(queSize);
        this.lonQueue = new CircularQueue(queSize);

        this.mission = false;
        this.databaseTime = "NaN";

        //Write Log
        this.loggedTSPI = new StringBuffer();

        this.header = "CurrentTime,DatabaseTime,curLat,curLon,targetLat,targetLon,mission,queSize,taskInterval\n";
        this.loggedTSPI.append(header);
    }

    public void updateTSPIdji(String time, String GPSSignal, double altitude_seaTohome, double altitude, double latitude, double longitude, double pitch, double yaw, double roll,
                              double vX, double vY, double vZ, double xXYZ, FlightMode flightState){
        this.timestamp = time;
        this.gpsSignalStrength = GPSSignal;
        this.Altitude_seaTohome = altitude_seaTohome;
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

    public void updateTSPIserver(String timestamp,String GPSSignal, double altitude_seaTohome,double altitude, double latitude, double longitude){
        this.timestamp = timestamp;
        this.gpsSignalStrength = GPSSignal;
        this.Altitude_seaTohome = altitude_seaTohome;
        this.Altitude = altitude;
        this.Latitude = latitude;
        this.Longitude = longitude;
    }

    public void setTargetLat(double targetLat){
        this.targetLat = targetLat;
    }

    public void setTargetLon(double targetLon){
        this.targetLon = targetLon;
    }
    public void setTaskInterval(int taskInterval){
        this.taskInterval = taskInterval;
    }
    public void setMission(boolean mission){this.mission = mission;}
    public void setDatabaseTime(String databaseTime){this.databaseTime = databaseTime;}

    public String getTimestamp(){return timestamp;}
    public String getGpsSignalStrength(){return gpsSignalStrength;}
    public double getAltitude_seaTohome(){return Altitude_seaTohome;}
    public double getAltitude(){return Altitude;}
    public double getLatitude() {
        return Latitude;
    }

    public double getLongitude(){
        return Longitude;
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
    public int getQueSize(){return queSize;}
    public double getTargetLat(){
        return targetLat;
    }

    public double getTargetLon(){
        return targetLon;
    }

    public FlightMode getFlightState() {
        return flightState;
    }
    public boolean getMission(){return this.mission;}
    public String getDatabaseTime(){return this.databaseTime;}

    public void appendLatLonToQueue(double lat, double lon){
        latQueue.insert(lat);
        lonQueue.insert(lon);
    }

    public String logResults(){
        if(loggedTSPI.length() == header.length()){
            loggedTSPI.append(timestamp).append(",");
            loggedTSPI.append(databaseTime).append(",");
            loggedTSPI.append(Latitude).append(",");
            loggedTSPI.append(Longitude).append(",");
            loggedTSPI.append(targetLat).append(",");
            loggedTSPI.append(targetLon).append(",");
            loggedTSPI.append(mission).append(",") ;
            loggedTSPI.append(queSize).append(",");
            loggedTSPI.append(taskInterval).append("\n");
        } else {
            loggedTSPI.delete(0, loggedTSPI.length());
            loggedTSPI.append(timestamp).append(",");
            loggedTSPI.append(databaseTime).append(",");
            loggedTSPI.append(Latitude).append(",");
            loggedTSPI.append(Longitude).append(",");
            loggedTSPI.append(targetLat).append(",");
            loggedTSPI.append(targetLon).append(",");
            loggedTSPI.append(mission).append(",") ;
            loggedTSPI.append(queSize).append(",");
            loggedTSPI.append(taskInterval).append("\n");
        }

        return String.valueOf(loggedTSPI);
    }

    public void writeLogfile(Context context, String filename, String content){
        String data = content;
        FileOutputStream outputStream;
        try{
            outputStream = context.openFileOutput(filename, Context.MODE_APPEND);
            outputStream.write(data.getBytes());
            outputStream.close();
            Log.d("filewrite","success" + filename);
        }catch(IOException e){
            Log.d("filewrite","failed");
            e.printStackTrace();
        }
    }


}
