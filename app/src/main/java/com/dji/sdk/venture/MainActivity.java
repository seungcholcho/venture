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
    private int taskInterval = 200;
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
        private float bearing;
        private float predictionPeriod; // predictionPeriod 초 마다 거리를 malDrone의 위치를 예측함.
        private float predictedVelocity;
        private float targetYaw;
        private double targetLatitude;
        private double targetLongitude;
        private boolean enableVirtualStick;

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
        public int getUpdateCount(){
            return this.updateCount;
        }

        @Override
        public void run() {

            if (updateCount < 5) {
                // Connection DB code
                db.collection("0612_test_0").orderBy("Time", Query.Direction.DESCENDING).get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (task.isSuccessful()) {

                                    int i = 0;
                                    for (QueryDocumentSnapshot document : task.getResult()) {

                                        //showToast(String.valueOf(document.getData()));

                                        //String Time = String.valueOf(document.getData().get("Time").getClass().getName());
                                        //Date Timestamp = changeUnixTime(Time);
                                        String GpsSignal = (String) document.getData().get("GpsSignal");
                                        double Altitude_seaTohome = (double) document.getData().get("Altitude_seaTohome");
                                        double Altitude = (double) document.getData().get("Altitude");
                                        double Latitude = (double) document.getData().get("Latitude");
                                        double Longitude = (double) document.getData().get("Longitude");

                                        Log.d("Firebase", "Time : " + String.valueOf(document.getData().get("Time")));
                                        Log.d("Firebase", "GpsSignal : " + String.valueOf(document.getData().get("GpsSignal")));
                                        Log.d("Firebase", "Altitude : " + String.valueOf(document.getData().get("Altitude")));
                                        Log.d("Firebase", "Latitude : " + String.valueOf(document.getData().get("Latitude")));
                                        Log.d("Firebase", "Latitude : " + String.valueOf(document.getData().get("Latitude")));

                                        maliciousTSPI.updateTSPIserver(GpsSignal, Altitude_seaTohome, Altitude, Latitude, Longitude);

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

                if (updateCount == 4) {
                    updateCount = 0;
                }
            }


            //UI 변경
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

                    maliciousLocation = "Lat : " + String.valueOf(maliciousTSPI.getLatitude()) + "\nLon : " + String.valueOf(maliciousTSPI.getLongitude());
                    mTextMaliciousLocation.setText(maliciousLocation);

                    trajectoryLocation = "Lat : " + String.valueOf(sendVirtualStickDataTask.getTargetLatitude()) + "\nLon : " + String.valueOf(sendVirtualStickDataTask.getTargetLongitude());
                    mTextTrajectoryLocation.setText(trajectoryLocation);

                    //------------------------------------------------------------------------------

                    defensiveTSPIState = "Altitude_seatohome : " + String.valueOf(defensiveTSPI.getAltitude_seaTohome()) +
                            "\nAltitude : " + String.valueOf(defensiveTSPI.getAltitude()) +
                            "\nPitch : " + String.valueOf(defensiveTSPI.getPitch()) +
                            "\nYaw : " + String.valueOf(defensiveTSPI.getYaw()) +
                            "\nRoll : " + String.valueOf(defensiveTSPI.getRoll());
                    mTextDefensiveTSPI.setText(defensiveTSPIState);

                    maliciousTSPIState = "Remaing distance of the Defensive : " + String.valueOf(getMaliciousDrone_flyDistance()) +
                            "\nAltitude_seatohome : " + String.valueOf(defensiveTSPI.getAltitude_seaTohome()) +
                            "\nAltitude : " + String.valueOf(defensiveTSPI.getAltitude()) +
                            "\nRemaing Altitude of the Defensive : " + String.valueOf(getAltitudeDifference());

                    mTextMaliciousTSPI.setText(maliciousTSPIState);

                    trajectoryTSPIState = "Remaing distance of the Defensive : " + getDistance_defenToTrajectory();

                    //InputData
                    InputDataState = "Input Data State\n Pitch : " + String.valueOf(sendVirtualStickDataTask.getPitch()) +
                            "\nYaw : " + String.valueOf(sendVirtualStickDataTask.getYaw()) +
                            "\nRoll : " + String.valueOf(sendVirtualStickDataTask.getRoll()) +
                            "\nThrottle : " + String.valueOf(sendVirtualStickDataTask.getThrottle()) +
                            "\nCount : " + String.valueOf(sendVirtualStickDataTask.getUpdateCount());

                    Log.d("SendData", InputDataState);
                    mTextTrajectoryTSPI.setText(InputDataState);
                }
            });

            if (getEnableVirtualStick()) {

                calculateTSPI();
                defTSPI.setTargetLat(targetLatitude);
                defTSPI.setTargetLon(targetLongitude);
                malTSPI.appendLatLonToQueue(malTSPI.getLatitude(), malTSPI.getLongitude());
                Log.d("TaskCalculate", String.valueOf(getPitch()));

                send();
                Log.d("TaskSend", "Succeed updated data");

                //Write Log
                defTSPI.writeLogfile(mContext, fileName, defTSPI.logResults());

            }
        }

        public void calculateTSPI() {
            Log.d("calculateTSPI", "Run");

            float time = 2.0F;

            //Change throttle
//            defensiveAltitude = defTSPI.getAltitude_seaTohome() + defTSPI.getAltitude();
//            maliciousAltitude = malTSPI.getAltitude_seaTohome() + malTSPI.getAltitude();
//            AltitudeDifference = maliciousAltitude - defensiveAltitude;
//
//            if (AltitudeDifference < 0) {
//                setThrottle(2);
//            } else if (AltitudeDifference <= 0 && AltitudeDifference >= -3) {
//                setThrottle(0);
//            } else if (AltitudeDifference < -3) {
//                setThrottle(-1);
//            }


            //Change Yaw
            if (malTSPI.latQueue.empty() != true) {
                //베어링 계산
                bearing = (float) GPSUtil.calculateBearing(malTSPI.latQueue.getFront(), malTSPI.lonQueue.getFront(), malTSPI.latQueue.getRear(), malTSPI.lonQueue.getRear());
                //비행 거리 계산
                maliciousDrone_flyDistance = (float) GPSUtil.haversine(malTSPI.latQueue.getFront(), malTSPI.lonQueue.getFront(), malTSPI.latQueue.getRear(), malTSPI.lonQueue.getRear()); // is in Km
                //속도 계산
                predictedVelocity = maliciousDrone_flyDistance / predictionPeriod; // km/s
                Log.d("PosPredBDV", "bearing: " + String.valueOf(bearing) + "distance: " + String.valueOf(maliciousDrone_flyDistance) + "Velocity " + String.valueOf(predictedVelocity));

                // time초 뒤의 위치 예측
                targetLatitude = GPSUtil.calculateDestinationLatitude(malTSPI.latQueue.getRear(), predictedVelocity * time, bearing);  // time 초 뒤의 위도 예측
                targetLongitude = GPSUtil.calculateDestinationLongitude(malTSPI.latQueue.getRear(), malTSPI.lonQueue.getRear(), predictedVelocity * time, bearing); //time 초 뒤의 경도 예측

                targetYaw = (float) GPSUtil.calculateBearing(defTSPI.getLatitude(), defTSPI.getLongitude(), targetLatitude, targetLongitude);

                Log.d("PosPred", "myPos: lat: " + String.valueOf(defTSPI.getLatitude()) + " lon: " + String.valueOf(defTSPI.getLongitude()));
                Log.d("PosPred", "tarPos: lat: " + String.valueOf(targetLatitude) + " lon: " + String.valueOf(targetLongitude) + " yaw: " + String.valueOf(targetYaw));

                setYaw(targetYaw);
                setPitch(1);

                //Calculation of the difference between the Defensive location and trajectory location
                distance_defenToTrajectory = (float) GPSUtil.haversine(defTSPI.getLatitude(), defTSPI.getLongitude(), targetLatitude, targetLongitude); // is in Km

                //Change pitch
                //상대 드론와 3미터 차이 나면 속도0
//                if (Math.abs(distance_defenToTrajectory) > 0.003) {
//                    setPitch(1);
//                }//상대 드론과
//                else if (Math.abs(distance_defenToTrajectory) <= 0.003 && Math.abs(distance_defenToTrajectory) >= 0.002) {
//                    setPitch(1);
//                } else if (Math.abs(distance_defenToTrajectory) < 0.002 && Math.abs(distance_defenToTrajectory) >= 0.000) {
//                    setPitch(-1);
//                }

            } else {
                Log.d("PosPred", "queue empty!");
            }
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

        // this location listener is to the device this application is running on only
        // to follow a different device, we will need a check box or radio button of some sort to
        // either pick to follow the good drone or enemy drone
        // 현재 위치 표시
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


