package com.dji.sdk.venture;

import java.util.Date;

public class TSPI {
    StringBuffer loggedTSPI;
    private Date timestamp;
    private String gpsSignalStrength;
    private int satelliteCount;

    private double homepointLatitude;
    private double homepointLongitude;
    private double homepointAltitude;

    private double currentLatitude;
    private double currentLongitude;
    private double currentAltitude;

    private double sealevelAltitude;

    private double pitch;
    private double yaw;
    private double roll;

    float vX;
    float vY;
    float vZ;
    float vXYZ;

    TSPI(){
        this.loggedTSPI = new StringBuffer();
    }

    public void setTimestamp(Date time){this.timestamp = time;}
    public void setGpsSignalStrength(String GpsSignalStrength){this.gpsSignalStrength = GpsSignalStrength; }

    public void setHomepointLatitude(double homepointLatitude) {this.homepointLatitude = homepointLatitude;}
    public void setHomepointLongitude(double homepointLongitude) {this.homepointLongitude = homepointLongitude;}
    public void setHomepointAltitude(double homepointAltitude){this.homepointAltitude = homepointAltitude;}

    public void setCurrentLatitude(double latitude){
        this.currentLatitude = latitude;
    }
    public void setCurrentLongitude(double longitude){this.currentLongitude = longitude;}
    public void setCurrentAltitude(double altitude){this.currentAltitude = altitude;}

    public void setPitch(double pitch){
        this.pitch = pitch;
    }
    public void setYaw(double yaw){
        this.yaw = yaw;
    }
    public void setRoll(double roll){ this.roll = roll; }

    public String getGpsSignalStrength(){
        return gpsSignalStrength;
    }
    public double getCurrentLatitude(){
        return currentLatitude;
    }
    public double getCurrentLongitude(){
        return currentLongitude;
    }
    public double getCurrentAltitude(){return currentAltitude;}
    public double getPitch(){ return pitch;}
    public double getYaw() { return yaw;}

    public String logResults(){
        loggedTSPI.delete(0, loggedTSPI.length());
        loggedTSPI.append(timestamp).append("\n");
        loggedTSPI.append(gpsSignalStrength).append("\n");
        loggedTSPI.append(currentLatitude).append("\n");
        loggedTSPI.append(currentLongitude).append("\n");
        loggedTSPI.append(yaw).append("\n");
        return String.valueOf(loggedTSPI);
    }
}
