package com.dji.sdk.venture;

import static java.lang.Double.isNaN;
import static dji.log.GlobalConfig.TAG;

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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
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
import java.util.HashMap;
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
    private TextView mTextTime;
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
    private FirebaseFirestore db;

    //Map
    private GoogleMap googleMap;
    private Marker droneMarker = null;
    private static final int PATTERN_GAP_LENGTH_PX = 20;
    private static final int PATTERN_DASH_LENGTH_PX = 20;
    private static final PatternItem GAP = new Gap(PATTERN_GAP_LENGTH_PX);
    private static final PatternItem DASH = new Dash(PATTERN_DASH_LENGTH_PX);
    private static final List<PatternItem> patternList = Arrays.asList(GAP, DASH);

    //자체 클래스
    FlightController flightController;
    BackgroundCallback updateTSPI;
    TSPI defensiveTSPI;
    TSPI maliciousTSPI;
    private SendVirtualStickDataTask sendVirtualStickDataTask;

    //write log
    Date date;
    DateFormat dateFormat;
    private String strDate;
    private String fileName;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //권한허용
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

        //Database 연동
        db = FirebaseFirestore.getInstance();

        //UI 초기화
        initUI();

        //Display Map
//        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_map);
//        mapFragment.getMapAsync(this);

        //Write log
//        mContext = getApplicationContext();
//
//        date = Calendar.getInstance().getTime();
//        dateFormat = new SimpleDateFormat("yyMMddHHmmss");
//
//        strDate = dateFormat.format(date);
//        fileName = (strDate + ".csv");

        try {
            flightController = ((Aircraft) DJISDKManager.getInstance().getProduct()).getFlightController();

            //TSPI 객체 생성
            defensiveTSPI = new TSPI();
            maliciousTSPI = new TSPI();
            updateTSPI = new BackgroundCallback(defensiveTSPI, flightController);
            updateTSPI.start();

            sendVirtualStickDataTask = new SendVirtualStickDataTask(flightController, defensiveTSPI, maliciousTSPI);
            sendDataTimer = new Timer();
            sendDataTimer.schedule(sendVirtualStickDataTask, 100, taskInterval);
            defensiveTSPI.setTaskInterval(taskInterval);

            //최대 고도 제한
            flightController.setMaxFlightHeight(100, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    Log.d("setFlightHeight", "Completed");
                }
            });

            //최대 반경 제한
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

    private void initUI() {

        mTextBattery = (TextView) findViewById(R.id.text_battery);

        mTextDefensiveLocation = (TextView) findViewById(R.id.text_defensive_location);
//        mTextMaliciousLocation = (TextView) findViewById(R.id.text_malicious_location);
        mTextTrajectoryLocation = (TextView) findViewById(R.id.text_trajectory_location);

        mTextDefensiveTSPI = (TextView) findViewById(R.id.text_defensive_TSPI);
//        mTextMaliciousTSPI = (TextView) findViewById(R.id.text_malicious_TSPI);
        mTextTrajectoryTSPI = (TextView) findViewById(R.id.text_trajectory_TSPI);

        mBtnEnable = (Button) findViewById(R.id.btn_enable);
        mBtnDisable = (Button) findViewById(R.id.btn_disable);
        mBtntmp1 = (Button) findViewById(R.id.btn_tmp);

        mBtnEnable.setOnClickListener(this);
        mBtnDisable.setOnClickListener(this);
        mBtntmp1.setOnClickListener(this);

    }

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
        private float predictionPeriod; // predictionPeriod 초 마다 거리를 malDrone의 위치를 예측함.
        private float predictedVelocity;
        private float targetYaw;
        private double targetLatitude;
        private double targetLongitude;
        private boolean enableVirtualStick;
        private boolean missionCompleted;

        TSPI defTSPI;
        TSPI malTSPI;
        FlightController mflightController;

        //생성자
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

        public void setMissionCompleted(boolean missionCompleted){
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
        public float getDistance_defenTomal(){return this.distance_defenTomal;}

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

        public boolean getMissionCompeleted(){
            return this.missionCompleted;
        }

        @Override
        public void run() {
            long time = System.currentTimeMillis();
            SimpleDateFormat simpl = new SimpleDateFormat("yyMMddHHmmss");
            String currentTime = simpl.format(time);
            Log.d("TaskLog", currentTime);

            HashMap result = new HashMap<>();
            result.put("Time",  defTSPI.getTimestamp());
            result.put("GpsSignal", String.valueOf(defTSPI.getGpsSignalStrength()));
            result.put("Altitude_seaTohome",defTSPI.getAltitude_seaTohome());
            result.put("Altitude", defTSPI.getAltitude());
            result.put("Latitude", defTSPI.getLatitude());
            result.put("Longitude", defTSPI.getLongitude());
            result.put("Pitch", defTSPI.getPitch());
            result.put("Yaw", defTSPI.getYaw());


            if (!(isNaN((double)result.get("Latitude"))) && !(isNaN((double)result.get("Longitude")))){
                db.collection("0614_test2_1600_A").add(result).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding document", e);
                    }
                });
            }
            else{
                System.out.println("Latitude and Logitude are NaN");
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    //Mark on map in real time
                    //updateDroneLocation();

                    virtualStickState = "VirtualStickController : " + String.valueOf(flightController.isVirtualStickControlModeAvailable());
                    mTextBattery.setText(virtualStickState);

                    defensiveLocation = "Lat : " + String.valueOf(defensiveTSPI.getLatitude()) +
                            "\nLon : " + String.valueOf(defensiveTSPI.getLongitude());
                    mTextDefensiveLocation.setText(defensiveLocation);

//                    maliciousLocation = "Lat : " + String.valueOf(maliciousTSPI.getLatitude()) + "\nLon : " + String.valueOf(maliciousTSPI.getLongitude());
//                    mTextMaliciousLocation.setText(maliciousLocation);

                    trajectoryLocation = "Lat : " + String.valueOf(sendVirtualStickDataTask.getTargetLatitude()) + "\nLon : " + String.valueOf(sendVirtualStickDataTask.getTargetLongitude());
                    mTextTrajectoryLocation.setText(trajectoryLocation);

                    //------------------------------------------------------------------------------

                    defensiveTSPIState = "Altitude_seatohome : " + String.valueOf(defensiveTSPI.getAltitude_seaTohome()) +
                            "\nAltitude : " + String.valueOf(defensiveTSPI.getAltitude()) +
                            "\nPitch : " + String.valueOf(defensiveTSPI.getPitch()) +
                            "\nYaw : " + String.valueOf(defensiveTSPI.getYaw()) +
                            "\nRoll : " + String.valueOf(defensiveTSPI.getRoll());
                    mTextDefensiveTSPI.setText(defensiveTSPIState);

//                    maliciousTSPIState = "Radius from the DeDrone : " + String.valueOf(getDistance_defenTomal()) +
//                            "\nAltitude from the DeDrone : " + String.valueOf(getAltitudeDifference()) +
//                            "\nCurrent Time : " + currentTime+
//                            "\nDatabase Time : " + malTSPI.getTimestamp();
//
//                    mTextMaliciousTSPI.setText(maliciousTSPIState);

                    trajectoryTSPIState = "Remaing distance of the Defensive : " + getDistance_defenToTrajectory();

                    //InputData
                    InputDataState = "Input Data State\n Pitch : " + String.valueOf(sendVirtualStickDataTask.getPitch()) +
                            "\nYaw : " + String.valueOf(sendVirtualStickDataTask.getYaw()) +
                            "\nRoll : " + String.valueOf(sendVirtualStickDataTask.getRoll()) +
                            "\nThrottle : " + String.valueOf(sendVirtualStickDataTask.getThrottle());

                    Log.d("SendData", InputDataState);
                    mTextTrajectoryTSPI.setText(InputDataState);
                }
            });

            if (getEnableVirtualStick()) {

                calculateTSPI();
//                defTSPI.setTargetLat(targetLatitude);
//                defTSPI.setTargetLon(targetLongitude);
//
//                malTSPI.appendLatLonToQueue(malTSPI.getLatitude(), malTSPI.getLongitude());

                Log.d("TaskCalculate", String.valueOf(getPitch()));

                send();
                Log.d("TaskSend", "Succeed updated data");

                //Write Log
                //defTSPI.writeLogfile(mContext, fileName, defTSPI.logResults());
            }
        }

        public void calculateTSPI() {
            Log.d("calculateTSPI", "Run");

            targetLatitude = 40.22468163009505559558;
            targetLongitude = -87.00319254972414739768;

            distance_defenToTrajectory = (float) GPSUtil.haversine(defTSPI.getLatitude(), defTSPI.getLongitude(), targetLatitude, targetLongitude); // is in Km

            targetYaw = (float) GPSUtil.calculateBearing(defTSPI.getLatitude(), defTSPI.getLongitude(), targetLatitude, targetLongitude);
            setYaw(targetYaw);

            if (distance_defenToTrajectory <= 0.0005 && distance_defenToTrajectory > 0){
                setPitch(0);
            }else{
                setPitch(5);
            }

//            float time = 2.0F;

            //Change throttle
//            defensiveAltitude = defTSPI.getAltitude_seaTohome() + defTSPI.getAltitude();
//            maliciousAltitude = malTSPI.getAltitude_seaTohome() + malTSPI.getAltitude();
//            AltitudeDifference = maliciousAltitude - defensiveAltitude;
//
//            if (AltitudeDifference > 0) {
//                setThrottle(2);
//            } else if (AltitudeDifference <= 0 && AltitudeDifference > -3) {
//                setThrottle(0);
//            } else if (AltitudeDifference <= -3) {
//                setThrottle(-1);
//            }

            //Change Yaw
//            if (malTSPI.latQueue.empty() != true) {
//                //베어링 계산
//                bearing = (float) GPSUtil.calculateBearing(malTSPI.latQueue.getFront(), malTSPI.lonQueue.getFront(), malTSPI.latQueue.getRear(), malTSPI.lonQueue.getRear());
//
//                //비행 거리 계산
//                maliciousDrone_flyDistance = (float) GPSUtil.haversine(malTSPI.latQueue.getFront(), malTSPI.lonQueue.getFront(), malTSPI.latQueue.getRear(), malTSPI.lonQueue.getRear()); // is in Km
//                //속도 계산
//                predictedVelocity = maliciousDrone_flyDistance / predictionPeriod; // km/s
//                Log.d("PosPredBDV", "bearing: " + String.valueOf(bearing) + "distance: " + String.valueOf(maliciousDrone_flyDistance) + "Velocity " + String.valueOf(predictedVelocity));
//
//                // time초 뒤의 위치 예측
//                //targetLatitude = GPSUtil.calculateDestinationLatitude(malTSPI.latQueue.getRear(), predictedVelocity * time, bearing);  // time 초 뒤의 위도 예측
//                //targetLongitude = GPSUtil.calculateDestinationLongitude(malTSPI.latQueue.getRear(), malTSPI.lonQueue.getRear(), predictedVelocity * time, bearing); //time 초 뒤의 경도 예측
//
//                //malicious drone의 위치로 이동할때
//                targetLatitude = malTSPI.getLatitude();
//                targetLongitude = malTSPI.getLongitude();
//
//                targetYaw = (float) GPSUtil.calculateBearing(defTSPI.getLatitude(), defTSPI.getLongitude(), targetLatitude, targetLongitude);
//
//
////                Log.d("PosPred", "myPos: lat: " + String.valueOf(defTSPI.getLatitude()) + " lon: " + String.valueOf(defTSPI.getLongitude()));
////                Log.d("PosPred", "tarPos: lat: " + String.valueOf(targetLatitude) + " lon: " + String.valueOf(targetLongitude) + " yaw: " + String.valueOf(targetYaw));
//
//                setYaw(targetYaw);
//
//                //Calculation of the difference between the Defensive location and trajectory location
//                distance_defenToTrajectory = (float) GPSUtil.haversine(defTSPI.getLatitude(), defTSPI.getLongitude(), targetLatitude, targetLongitude); // is in Km
//                distance_defenTomal = (float) GPSUtil.haversine(defTSPI.getLatitude(), defTSPI.getLongitude(), malTSPI.getLatitude(), malTSPI.getLongitude()); // is in Km
//
//                //Change pitch
//                //상대 드론 위치에 따라 속도 변화
//                //반경 1000m 이내 3
//                //반경 5~3m 이내 1
//                //반경 3m 이내 0
//                if (distance_defenTomal <= 1 && distance_defenTomal > 0.003){
//                    setPitch(3);
//                    setMissionCompleted(false);
//                    defTSPI.setMission(false);
//                }
//                else if (distance_defenTomal <= 0.003 && distance_defenTomal > 0.001) {
//                    setPitch(1);
//                    setMissionCompleted(false);
//                    defTSPI.setMission(false);
//                }else if (distance_defenTomal <= 0.0005 && distance_defenTomal > 0){
//                    setPitch(0);
//                    setMissionCompleted(true);
//                    defTSPI.setMission(true);
//                }
//
//            } else {
//                Log.d("PosPred", "queue empty!");
//            }
        }

        public void send() {
            Log.d("SendInputData", "Start send");

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
//
//        this.googleMap = googleMap;
//
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
//                PackageManager.PERMISSION_GRANTED) {
//            if
//            (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
//            } else {
//                ActivityCompat.requestPermissions(this,
//                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
//                        1);
//            }
//        }
//
//        this.googleMap.setMyLocationEnabled(true);
//
//        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
//
//        long minTime = 1000; //millisecond
//        float minDistance = 0;
//
//        // this location listener is to the device this application is running on only
//        // to follow a different device, we will need a check box or radio button of some sort to
//        // either pick to follow the good drone or enemy drone
//        // 현재 위치 표시
//        LocationListener listener = new LocationListener() {
//            @Override
//            public void onLocationChanged(@NonNull Location location) {
//                Double latitude = location.getLatitude();
//                Double longitude = location.getLongitude();
//                LatLng curPoint = new LatLng(latitude, longitude);
//                MainActivity.this.googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(curPoint, 18));
//                locationManager.removeUpdates(this);
//            }
//        };
//
//        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, listener);

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

        //현재 위치 - 예상 위치
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



