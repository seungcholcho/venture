package com.dji.sdk.venture;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import com.dji.sdk.venture.Utils.GPSUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;

import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import dji.common.error.DJIError;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.util.CommonCallbacks;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, AdapterView.OnItemSelectedListener, View.OnClickListener, GoogleMap.OnMapClickListener {
    //User Interface Widget
    private Button mBtnDisable;
    private Button mBtnEnable;
    private Button mBtntmp1;
    private TextView mTextMaliciousLocation;
    private TextView mTextDefensiveLocation;
    private TextView mTextTrajectoryLocation;
    private TextView mTextBattery;
    private TextView mTextDefensiveTSPI;
    private TextView mTextMaliciousTSPI;
    private TextView mTextTrajectoryTSPI;

    private String defensiveLocation;
    private String maliciousLocation;
    private String trajectoryLocation;
    private String virtualStickState;
    private String defensiveTSPIState;
    private String maliciousTSPIState;
    private String trajectoryTSPIState;
    private String InputDataState;

    //interval time
    private int taskInterval = 200; // taskInterval/1000 s
    private Timer sendDataTimer;

    //Database
    private FirebaseFirestore database;

    //To use google Map
    private GoogleMap googleMap;
    private Marker droneMarker = null;
    private static final int PATTERN_GAP_LENGTH_PX = 20;
    private static final int PATTERN_DASH_LENGTH_PX = 20;
    private static final PatternItem GAP = new Gap(PATTERN_GAP_LENGTH_PX);
    private static final PatternItem DASH = new Dash(PATTERN_DASH_LENGTH_PX);
    private static final List<PatternItem> patternList = Arrays.asList(GAP, DASH);

    //Itself class
    FlightController flightController;
    BackgroundCallback updateTSPI;
    TSPI defensiveTSPI;
    TSPI maliciousTSPI;
    private SendVirtualStickDataTask sendVirtualStickDataTask;

    //To write log
    Date date;
    DateFormat dateFormat;
    private String strDate;
    private String fileName;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //This is the code to check permission grant.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            if
            (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        1);
            }
        }

        //Create an object to interface with the database.
        database = FirebaseFirestore.getInstance();

        //User interface initialization
        initUI();

        //Display Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_map);
        mapFragment.getMapAsync(this);

        //Write log
        mContext = getApplicationContext();
        date = Calendar.getInstance().getTime();
        dateFormat = new SimpleDateFormat("yyMMddHHmmss");
        strDate = dateFormat.format(date);
        fileName = (strDate + ".csv");

        try {
            flightController = ((Aircraft) DJISDKManager.getInstance().getProduct()).getFlightController();

            //Create TSPI object
            defensiveTSPI = new TSPI();
            maliciousTSPI = new TSPI();

            //Run background thread
            updateTSPI = new BackgroundCallback(defensiveTSPI, flightController);
            updateTSPI.start();

            //Create an object to use sendVirtualStick mode
            sendVirtualStickDataTask = new SendVirtualStickDataTask(flightController, defensiveTSPI, maliciousTSPI);

            //Use a timer to send a repetitive signal to virtualStick
            sendDataTimer = new Timer();
            sendDataTimer.schedule(sendVirtualStickDataTask, 100, taskInterval);
            defensiveTSPI.setTaskInterval(taskInterval);

            //limit maximum Flight altitude
            flightController.setMaxFlightHeight(100, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    Log.d("setFlightHeight", "Completed");
                }
            });

            //limit maximum Flight radius
            flightController.setMaxFlightRadius(500, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    Log.d("setFlightRadius", "Completed");
                }
            });

            //Change Virtual Stick Mode
            flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
            flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
            flightController.setYawControlMode(YawControlMode.ANGLE);
            flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);

        } catch (Exception e) {
            Log.d("FlightControllerState", "not Connected");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    //Functions for linking user interface objects
    private void initUI() {

        mTextBattery = (TextView) findViewById(R.id.text_battery);

        mTextDefensiveLocation = (TextView) findViewById(R.id.text_defensive_location);
        mTextMaliciousLocation = (TextView) findViewById(R.id.text_malicious_location);
        mTextTrajectoryLocation = (TextView) findViewById(R.id.text_trajectory_location);

        mTextDefensiveTSPI = (TextView) findViewById(R.id.text_defensive_TSPI);
        mTextMaliciousTSPI = (TextView) findViewById(R.id.text_malicious_TSPI);
        mTextTrajectoryTSPI = (TextView) findViewById(R.id.text_trajectory_TSPI);

        mBtnEnable = (Button) findViewById(R.id.btn_enable);
        mBtnDisable = (Button) findViewById(R.id.btn_disable);
        mBtntmp1 = (Button) findViewById(R.id.btn_tmp);

        mBtnEnable.setOnClickListener(this);
        mBtnDisable.setOnClickListener(this);
        mBtntmp1.setOnClickListener(this);

    }

    //Assign a function to a button
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_enable: {
                Log.d("onClick", "Enable");

                flightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        flightController.setVirtualStickAdvancedModeEnabled(true);
                        sendVirtualStickDataTask.setEnableVirtualStick(true);
                    }
                });

                break;
            }
            case R.id.btn_disable: {
                Log.d("onClick", "Disable");

                flightController.setVirtualStickModeEnabled(false, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        flightController.setVirtualStickAdvancedModeEnabled(false);
                        sendVirtualStickDataTask.setEnableVirtualStick(false);
                    }
                });

                break;
            }
            case R.id.btn_tmp: {
                //직진
                Log.d("onClick", "tmp1");
                sendVirtualStickDataTask.UpdateInputData(0, 0, 0, 0);
                break;
            }
            default:
                break;
        }
    }

    @Override
    public void onMapClick(@NonNull LatLng latLng) {
        Log.d("onMapClick", "Click,Click,Click");
    }

    private class SendVirtualStickDataTask extends TimerTask {
        private int updateCount;
        private float pitch;
        private float roll;
        private float yaw;
        private float throttle;

        private double defensiveAltitude;
        private double maliciousAltitude;
        private double AltitudeDifference;
        private float maliciousDrone_flyDistance;
        private float distance_defenToTrajectory;
        private float distance_defenTomal;
        private float bearing;
        private float predictionPeriod; //Predict malDrone's position by distance every predictionPeriod seconds.
        private float predictedVelocity;
        private float targetYaw;
        private double targetLatitude;
        private double targetLongitude;
        private boolean enableVirtualStick;
        private boolean missionCompleted;

        TSPI defTSPI;
        TSPI malTSPI;
        FlightController mflightController;

        //Constructor
        public SendVirtualStickDataTask(FlightController mflightController, TSPI defTSPI, TSPI malTSPI) {
            this.updateCount = 0;
            this.pitch = (float) 0;
            this.roll = 0;
            this.yaw = 0;
            this.throttle = 0;

            this.predictionPeriod = 2.0F;
            this.targetLatitude = 0.0;
            this.targetLongitude = 0.0;
            this.targetYaw = 0.0F;

            this.mflightController = mflightController;
            this.defTSPI = defTSPI;
            this.malTSPI = malTSPI;
            this.missionCompleted = false;
        }

        //Update data in real time
        public void UpdateInputData(float pitch, float roll, float yaw, float throttle) {
            this.pitch = pitch;
            this.roll = roll;
            this.yaw = yaw;
            this.throttle = throttle;
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
        public void setEnableVirtualStick(boolean enableVirtualStick) {
            this.enableVirtualStick = enableVirtualStick;
        }
        public void setMissionCompleted(boolean missionCompleted) {
            this.missionCompleted = missionCompleted;
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
        public float getMaliciousDrone_flyDistance() {
            return this.maliciousDrone_flyDistance;
        }
        public float getDistance_defenToTrajectory() {
            return this.distance_defenToTrajectory;
        }
        public float getDistance_defenTomal() {
            return this.distance_defenTomal;
        }
        public double getAltitudeDifference() {
            return this.AltitudeDifference;
        }
        public double getTargetLatitude() {
            return this.targetLatitude;
        }
        public double getTargetLongitude() {
            return this.targetLongitude;
        }
        public boolean getEnableVirtualStick() {
            return this.enableVirtualStick;
        }
        public boolean getMissionCompeleted() {
            return this.missionCompleted;
        }


        @Override
        public void run() {
            //Display current time
//            long time = System.currentTimeMillis();
//            SimpleDateFormat simpl = new SimpleDateFormat("yyMMddHHmmss");
//            String currentTime = simpl.format(time);
//            Log.d("TaskLog", currentTime);

            //The experimental results indicate a network delay of 3-4 seconds.
            //When the database data is requested every 0.2 seconds,
            //the same data is retrieved and the same value is continuously displayed.
            //This situation leads to an input issue.
            //To address this problem, the cycle of accessing the database has been modified to once per second using an if statement.
            if (updateCount < 5) {
                updateCount++;
                Log.d("updateCount",String.valueOf(updateCount));
            } else {
                // Connection DB code
                database.collection("0617_test_1").orderBy("Time", Query.Direction.DESCENDING).get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (task.isSuccessful()) {
                                    int i = 0;
                                    for (QueryDocumentSnapshot document : task.getResult()) {

                                        String Time = String.valueOf(document.getData().get("Time"));
                                        String GpsSignal = (String) document.getData().get("GpsSignal");
                                        double Altitude_seaTohome = (double) document.getData().get("Altitude_seaTohome");
                                        double Altitude = (double) document.getData().get("Altitude");
                                        double Latitude = (double) document.getData().get("Latitude");
                                        double Longitude = (double) document.getData().get("Longitude");

                                        Log.d("Firebase", "Time : " + Time);

                                        maliciousTSPI.updateTSPIserver(Time, GpsSignal, Altitude_seaTohome, Altitude, Latitude, Longitude);
                                        defTSPI.setDatabaseTime(Time);

                                        i++;
                                        if (i == 1) {
                                            break;
                                        }
                                    }
                                } else {
                                    Log.w("Error", "Error getting documents.", task.getException());
                                }
                            }
                        });

                malTSPI.appendLatLonToQueue(malTSPI.getLatitude(), malTSPI.getLongitude());
                updateCount = 0;
            }

            //Change User Interface
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    //Mark on map in real time
                    updateDroneLocation();

                    virtualStickState = "VirtualStickController : " + String.valueOf(flightController.isVirtualStickControlModeAvailable());
                    mTextBattery.setText(virtualStickState);

                    defensiveLocation = "Lat : " + String.valueOf(defensiveTSPI.getLatitude()) +
                            "\nLon : " + String.valueOf(defensiveTSPI.getLongitude());
                    mTextDefensiveLocation.setText(defensiveLocation);

                    maliciousLocation = "Lat : " + String.valueOf(maliciousTSPI.getLatitude()) +
                            "\nLon : " + String.valueOf(maliciousTSPI.getLongitude());
                    mTextMaliciousLocation.setText(maliciousLocation);

                    trajectoryLocation = "Lat : " + String.valueOf(sendVirtualStickDataTask.getTargetLatitude()) +
                            "\nLon : " + String.valueOf(sendVirtualStickDataTask.getTargetLongitude());
                    mTextTrajectoryLocation.setText(trajectoryLocation);

                    //------------------------------------------------------------------------------

                    defensiveTSPIState = "Altitude_seaTodrone : " + String.valueOf(defensiveTSPI.getAltitude_seaTohome()) +
                            "\nAltitude : " + String.valueOf(defensiveTSPI.getAltitude()) +
                            "\nPitch : " + String.valueOf(defensiveTSPI.getPitch()) +
                            "\nYaw : " + String.valueOf(defensiveTSPI.getYaw()) +
                            "\nRoll : " + String.valueOf(defensiveTSPI.getRoll());
                    mTextDefensiveTSPI.setText(defensiveTSPIState);

                    maliciousTSPIState = "Radius from the DeDrone : " + String.valueOf(getDistance_defenTomal()) +
                            "\nAltitude from the DeDrone : " + String.valueOf(getAltitudeDifference());
                    mTextMaliciousTSPI.setText(maliciousTSPIState);

                    trajectoryTSPIState = "Remaing distance of the Defensive : " + getDistance_defenToTrajectory();

                    //InputData to virtualStick
//                    InputDataState = "Input Data State\n Pitch : " + String.valueOf(sendVirtualStickDataTask.getPitch()) +
//                            "\nYaw : " + String.valueOf(sendVirtualStickDataTask.getYaw()) +
//                            "\nRoll : " + String.valueOf(sendVirtualStickDataTask.getRoll()) +
//                            "\nThrottle : " + String.valueOf(sendVirtualStickDataTask.getThrottle()) +
//                            "\nMission : " + String.valueOf(getMissionCompeleted());

                    mTextTrajectoryTSPI.setText(trajectoryTSPIState);
                }
            });


            //Run when enable button is pressed
            if (getEnableVirtualStick()) {

                calculateTSPI();
                defTSPI.setTargetLat(targetLatitude);
                defTSPI.setTargetLon(targetLongitude);

                Log.d("TaskCalculate", String.valueOf(getPitch()));

                send();
                Log.d("TaskSend", "Succeed updated data");

                //Write Log
                defTSPI.writeLogfile(mContext, fileName, defTSPI.logResults());

            }
        }

        public void calculateTSPI() {
            Log.d("calculateTSPI", "Run");

            //Predicted time
            float time = 8.0F;

            //Change throttle
            //Calculate the output value by calculating the malicious drone altitude and defensive drone altitude.
            defensiveAltitude = defTSPI.getAltitude_seaTohome() + defTSPI.getAltitude();
            maliciousAltitude = malTSPI.getAltitude_seaTohome() + malTSPI.getAltitude();
            AltitudeDifference = maliciousAltitude - defensiveAltitude;

            if (AltitudeDifference > 0) {
                setThrottle(2);
            } else if (AltitudeDifference <= 0 && AltitudeDifference > -3) {
                setThrottle(0);
            } else if (AltitudeDifference <= -3) {
                setThrottle(-1);
            }

            //Change Yaw
            if (malTSPI.latQueue.isEmpty() != true) {
                //Calculate Bearing
                bearing = (float) GPSUtil.calculateBearing((Double) malTSPI.latQueue.peek(), (Double)malTSPI.lonQueue.peek(), malTSPI.getLatitude(), malTSPI.getLongitude());

                //Calculate flight distance
                maliciousDrone_flyDistance = (float) GPSUtil.haversine((Double)malTSPI.latQueue.peek(), (Double)malTSPI.lonQueue.peek(), malTSPI.getLatitude(), malTSPI.getLongitude()); // is in Km

                //Calculate velocity
                predictedVelocity = maliciousDrone_flyDistance / predictionPeriod; // km/s

                //Prediction of position after time seconds
                targetLatitude = GPSUtil.calculateDestinationLatitude(malTSPI.getLatitude(), predictedVelocity * time, bearing);  // time 초 뒤의 위도 예측
                targetLongitude = GPSUtil.calculateDestinationLongitude(malTSPI.getLatitude(), malTSPI.getLongitude(), predictedVelocity * time, bearing); //time 초 뒤의 경도 예측

                targetYaw = (float) GPSUtil.calculateBearing(defTSPI.getLatitude(), defTSPI.getLongitude(), targetLatitude, targetLongitude);
                setYaw(targetYaw);

                //Calculation of the difference between the Defensive location and trajectory location
                distance_defenToTrajectory = (float) GPSUtil.haversine(defTSPI.getLatitude(), defTSPI.getLongitude(), targetLatitude, targetLongitude); // is in Km
                distance_defenTomal = (float) GPSUtil.haversine(defTSPI.getLatitude(), defTSPI.getLongitude(), malTSPI.getLatitude(), malTSPI.getLongitude()); // is in Km

                setPitch(4);

                //Change pitch
                //Speed changes depending on the predicted position
//                if (distance_defenToTrajectory <= 10 && distance_defenToTrajectory > 0) {
//                    setPitch(3);
//                    setMissionCompleted(false);
//                    defTSPI.setMission(false);
//                }
//                } else if (distance_defenToTrajectory <= 0.005 && distance_defenToTrajectory > 0.003) {
//                    setPitch(0.5F);
//                    setMissionCompleted(false);
//                    defTSPI.setMission(false);
//                } else if (distance_defenToTrajectory <= 0.003 && distance_defenToTrajectory > 0) {
//                    setPitch(0);
//                    setMissionCompleted(true);
//                    defTSPI.setMission(true);
//                }
            } else {
                Log.d("PosPred", "queue empty!");
            }
        }

        public void send() {
            Log.d("SendInputData", "Start send");

            //Sends flight control data using virtual stick commands
            this.mflightController.sendVirtualStickFlightControlData(
                    new FlightControlData(getRoll(), getPitch(), getYaw(), getThrottle()),
                    new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            Log.d("onResult", "Succeed input data into virtual stick");
                        }
                    });
        }
    }

    public void onReturn(View view) {
        this.finish();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        //To use Google Map API
        this.googleMap = googleMap;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            if
            (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        1);
            }
        }

        this.googleMap.setMyLocationEnabled(true);

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        long minTime = 1000; //millisecond
        float minDistance = 0;

        //This location listener is to the device this application is running on only
        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                Double latitude = location.getLatitude();
                Double longitude = location.getLongitude();
                LatLng curPoint = new LatLng(latitude, longitude);
                MainActivity.this.googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(curPoint, 18));
                locationManager.removeUpdates(this);
            }
        };

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, listener);

    }

    private void updateDroneLocation() {

        //defensive Drone Marker
        LatLng curPosition = new LatLng(defensiveTSPI.getLatitude(), defensiveTSPI.getLongitude());

        final MarkerOptions defensiveMarker = new MarkerOptions();
        defensiveMarker.position(curPosition);
        defensiveMarker.title("DefenDrone");
        defensiveMarker.snippet("DefenDrone");
        defensiveMarker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));

        //malicious Drone Marker
        LatLng malPosition = new LatLng(maliciousTSPI.getLatitude(), maliciousTSPI.getLongitude());

        final MarkerOptions maliciousMarker = new MarkerOptions();
        maliciousMarker.position(malPosition);
        defensiveMarker.title("MalDrone");
        defensiveMarker.snippet("MalDrone");
        maliciousMarker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));

        //trajectory Marker
        LatLng tarPosition = new LatLng(sendVirtualStickDataTask.getTargetLatitude(), sendVirtualStickDataTask.getTargetLongitude());

        final MarkerOptions trajectoryTarget = new MarkerOptions();
        trajectoryTarget.position(tarPosition);
        trajectoryTarget.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

        googleMap.clear();

        droneMarker = googleMap.addMarker(defensiveMarker);
        droneMarker = googleMap.addMarker(maliciousMarker);
        droneMarker = googleMap.addMarker(trajectoryTarget);

        //Draw a line from the current position to the predicted position
        Polyline polyline = googleMap.addPolyline(new PolylineOptions()
                .clickable(true)
                .add(curPosition, tarPosition));

        polyline.setColor(0xffdee2e6);
        polyline.setWidth(10);
        polyline.setEndCap(new RoundCap());

        polyline.setPattern(patternList);


        //below are rotations
        //markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.defensivedrone)); // 1.2cm*1.2cm TODO must adjust to middle.
        //markerOptions.anchor(0.0f,0.0f);
        //float angle = (float)mTSPI.getYaw();
        //markerOptions.rotation(angle);
        //rotation ends
    }

    private void showToast(final String toastMsg) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }
}



