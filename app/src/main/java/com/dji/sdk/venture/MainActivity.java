package com.dji.sdk.venture;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
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
import com.google.android.gms.maps.model.LatLng;

import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import dji.sdk.mission.followme.FollowMeMissionOperator;
import dji.sdk.mission.followme.FollowMeMissionOperatorListener;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

//Test
// implemts 뒤에 GoogleMap.OnMapClickListener 추가
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, AdapterView.OnItemSelectedListener, View.OnClickListener,GoogleMap.OnMapClickListener {
//    Handler handler = new Handler();
//    Runnable runnable;
//    int loadIntervals = 1000; //in ms. 1000ms = 1s
//    int refreshTargetIntervals = 5000; //Refresh time to target input

    FlightController flightController;

    TSPI defensiveTSPI;

    TSPI maliciousTSPI;

    BackgroundCallback updateTSPI;

    private GoogleMap mMap;
    private Button mBtnDisable, mBtnEnable, mBtntmp1, mBtntmp2;
    private TextView mTextTLocation, mTextCLocation, mTextDistance, mTextTime, mTextBattery, mTextState, mTextVirtualState, mTextVelocity;

    private String currentLocation;
    private String targetLocation;
    private String flightStates;
    private String InputDataState;
    private String TSPIState;
    private double currentLatitude = 0;
    private double currentLongitude = 0;
    private double targetLatitude = 0;
    private double targetLongitude = 0;

    private Marker droneMarker = null;

    List<LatLng> pathPoints = new ArrayList<>();

    private FirebaseFirestore db;

    private Timer sendDataTimer;

    private BackgroundVirtualStick backgroundVirtualStick;
    private SendVirtualStickDataTask sendVirtualStickDataTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
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

        initUI();

        //Display Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        defensiveTSPI = new TSPI();
        maliciousTSPI = new TSPI();

        try {
            flightController = ((Aircraft) DJISDKManager.getInstance().getProduct()).getFlightController();
            updateTSPI = new BackgroundCallback(defensiveTSPI, flightController);
            updateTSPI.start();

            sendVirtualStickDataTask = new SendVirtualStickDataTask(flightController);
            sendDataTimer = new Timer();
            sendDataTimer.schedule(sendVirtualStickDataTask, 100, 200);

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

            //flight 상태 + Mission 상태 출력
            flightStates = "Flight state : " + defensiveTSPI.getFlightState().name() + "\nVirtualStickController : " + String.valueOf(flightController.isVirtualStickControlModeAvailable());
            mTextDistance.setText(flightStates);

            //Virtual Stick
            flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
            flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
            flightController.setYawControlMode(YawControlMode.ANGLE);
            flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);

        } catch (Exception e) {
            Log.d("FlightControllerState", "not Connected");
        }

        //현재위치 초기화
        currentLatitude = defensiveTSPI.getLatitude();
        currentLongitude = defensiveTSPI.getLongitude();

        //pathPoint값 초기화
//        initLocation = new LatLng(currentLatitude, currentLongitude);
//
//        pathPoints.add(initLocation);
//        pathPoints.add(initLocation);
    }

    @Override
    protected void onResume() {
        super.onResume();
//        handler.postDelayed(runnable = new Runnable() {
//            //update location of our drone every loadIntervals seconds.
//            @Override
//            public void run() {
//                // Connection DB code
//                db.collection("0526_test").orderBy("Time", Query.Direction.DESCENDING).get()
//                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//                            @Override
//                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                                if (task.isSuccessful()) {
//
//                                    int i = 0;
//                                    for (QueryDocumentSnapshot document : task.getResult()) {
//
//                                        //showToast(String.valueOf(document.getData()));
//
//                                        //String Time = String.valueOf(document.getData().get("Time").getClass().getName());
//                                        //Date Timestamp = changeUnixTime(Time);
//                                        String GpsSignal = (String) document.getData().get("GpsSignal");
//                                        double Altitude = (double) document.getData().get("Altitude");
//                                        double Latitude = (double) document.getData().get("Latitude");
//                                        double Longitude = (double) document.getData().get("Longitude");
//
//                                        Log.d("Firebase", "Time : " + String.valueOf(document.getData().get("Time")));
//                                        Log.d("Firebase", "GpsSignal : " + String.valueOf(document.getData().get("GpsSignal")));
//                                        Log.d("Firebase", "Altitude : " + String.valueOf(document.getData().get("Altitude")));
//                                        Log.d("Firebase", "Latitude : " + String.valueOf(document.getData().get("Latitude")));
//                                        Log.d("Firebase", "Latitude : " + String.valueOf(document.getData().get("Latitude")));
//
//                                        maliciousTSPI.updateTSPIserver(GpsSignal, Altitude, Latitude, Longitude);
//
//                                        i++;
//                                        if (i == 1) {
//                                            break;
//                                        }
//                                    }
//                                } else {
//                                    Log.w("Error", "Error getting documents.", task.getException());
//                                }
//                            }
//                        });
//
//                handler.postDelayed(runnable, loadIntervals);
//
//                //Mark on map in real time
//                //updateDroneLocation();
//
//                //Change Textview
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//
//                        currentLocation = "Lat : " + String.valueOf(defensiveTSPI.getLatitude()) +
//                                "\nLon : " + String.valueOf(defensiveTSPI.getLongitude());
//                        mTextCLocation.setText(currentLocation);
//
//                        flightStates = "Flight state : " + defensiveTSPI.getFlightState().name() +
//                                "\nVirtualStickController : " + String.valueOf(flightController.isVirtualStickControlModeAvailable());
//                        mTextDistance.setText(flightStates);
//
//                        TSPIState = "TSPI State\n Pitch : " + String.valueOf(defensiveTSPI.getPitch()) +
//                                "\nYaw : " + String.valueOf(defensiveTSPI.getYaw()) +
//                                "\nRoll : " + String.valueOf(defensiveTSPI.getRoll());
//                        mTextState.setText(TSPIState);
//
//                        InputDataState = "Input Data State\n Pitch : " + String.valueOf(backgroundVirtualStick.getPitch()) +
//                                "\nYaw : " + String.valueOf(backgroundVirtualStick.getYaw()) +
//                                "\nRoll : " + String.valueOf(backgroundVirtualStick.getRoll()) +
//                                "\nThrottle : " + String.valueOf(backgroundVirtualStick.getThrottle());
//                        mTextVirtualState.setText(InputDataState);
//
//                        String Velocity = "Velocity\nX : " + String.valueOf(defensiveTSPI.getvX()) + "\nY: " + String.valueOf(defensiveTSPI.getvY())
//                                + "\nZ: " + String.valueOf(defensiveTSPI.getvZ()) + "\nXYZ : " + String.valueOf(defensiveTSPI.getxXYZ());
//                        mTextVelocity.setText(Velocity);
//
//                    }
//                });
//            }
//        }, loadIntervals);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void initUI() {

        mTextTime = (TextView) findViewById(R.id.text_time);
        mTextBattery = (TextView) findViewById(R.id.text_battery);

        mTextCLocation = (TextView) findViewById(R.id.text_current_location);
        mTextTLocation = (TextView) findViewById(R.id.text_target_location);
        mTextDistance = (TextView) findViewById(R.id.text_distance);

        mTextState = (TextView) findViewById(R.id.text_tmp1);
        mTextVirtualState = (TextView) findViewById(R.id.text_tmp2);
        mTextVelocity = (TextView) findViewById(R.id.text_tmp3);

        mBtnEnable = (Button) findViewById(R.id.btn_enable);
        mBtnDisable = (Button) findViewById(R.id.btn_disable);
        mBtntmp1 = (Button) findViewById(R.id.btn_tmp1);
        mBtntmp2 = (Button) findViewById(R.id.btn_tmp2);

        mBtnEnable.setOnClickListener(this);
        mBtnDisable.setOnClickListener(this);
        mBtntmp1.setOnClickListener(this);
        mBtntmp2.setOnClickListener(this);

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
            case R.id.btn_tmp1: {
                //직진
                Log.d("onClick", "tmp1");
                sendVirtualStickDataTask.UpdateInputData(0, 0, 0, 0);
                break;
            }
            case R.id.btn_tmp2: {
                //우(횡) 이동
                Log.d("onClick", "tmp2");
                break;
            }
            default:
                break;
        }
    }

    @Override
    public void onMapClick(@NonNull LatLng latLng) {
        Log.d("onMapClick","Click,Click,Click");
    }

    private class SendVirtualStickDataTask extends TimerTask {

        private float pitch;
        private float roll;
        private float yaw;
        private float throttle;
        private float temp;
        private boolean enableVirtualStick;
        FlightController mflightController;

        //생성자
        public SendVirtualStickDataTask(FlightController mflightController) {
            this.pitch = (float) 2;
            this.roll = 0;
            this.yaw = -179;
            this.throttle = 0;
            this.temp = (float) 1;
            this.enableVirtualStick = false;
            this.mflightController = mflightController;
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

        public boolean getEnableVirtualStick() {
            return this.enableVirtualStick;
        }

        @Override
        public void run() {

            long time = System.currentTimeMillis();
            SimpleDateFormat simpl = new SimpleDateFormat("yyyy년 MM월 dd일 aa hh시 mm분 ss초");
            String s = simpl.format(time);
            Log.d("TaskLog", s);

            // Connection DB code
            db.collection("0526_test").orderBy("Time", Query.Direction.DESCENDING).get()
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
                                    double Altitude = (double) document.getData().get("Altitude");
                                    double Latitude = (double) document.getData().get("Latitude");
                                    double Longitude = (double) document.getData().get("Longitude");

                                    Log.d("Firebase", "Time : " + String.valueOf(document.getData().get("Time")));
                                    Log.d("Firebase", "GpsSignal : " + String.valueOf(document.getData().get("GpsSignal")));
                                    Log.d("Firebase", "Altitude : " + String.valueOf(document.getData().get("Altitude")));
                                    Log.d("Firebase", "Latitude : " + String.valueOf(document.getData().get("Latitude")));
                                    Log.d("Firebase", "Latitude : " + String.valueOf(document.getData().get("Latitude")));

                                    maliciousTSPI.updateTSPIserver(GpsSignal, Altitude, Latitude, Longitude);

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


            //UI 변경
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    //Mark on map in real time
                    updateDroneLocation();

                    currentLocation = "Lat : " + String.valueOf(defensiveTSPI.getLatitude()) +
                            "\nLon : " + String.valueOf(defensiveTSPI.getLongitude());
                    mTextCLocation.setText(currentLocation);

                    targetLocation = "Lat : " + String.valueOf(maliciousTSPI.getLatitude()) + "\nLon : " + String.valueOf(maliciousTSPI.getLongitude());
                    mTextTLocation.setText(targetLocation);

                    flightStates = "Flight state : " + defensiveTSPI.getFlightState().name() +
                            "\nVirtualStickController : " + String.valueOf(flightController.isVirtualStickControlModeAvailable());
                    mTextDistance.setText(flightStates);

                    TSPIState = "TSPI State\n Pitch : " + String.valueOf(defensiveTSPI.getPitch()) +
                            "\nYaw : " + String.valueOf(defensiveTSPI.getYaw()) +
                            "\nRoll : " + String.valueOf(defensiveTSPI.getRoll());
                    mTextState.setText(TSPIState);

                    InputDataState = "Input Data State\n Pitch : " + String.valueOf(sendVirtualStickDataTask.getPitch()) +
                            "\nYaw : " + String.valueOf(sendVirtualStickDataTask.getYaw()) +
                            "\nRoll : " + String.valueOf(sendVirtualStickDataTask.getRoll()) +
                            "\nThrottle : " + String.valueOf(sendVirtualStickDataTask.getThrottle());
                    mTextVirtualState.setText(InputDataState);

                    String Velocity = "Velocity\nX : " + String.valueOf(defensiveTSPI.getvX()) + "\nY: " + String.valueOf(defensiveTSPI.getvY())
                            + "\nZ: " + String.valueOf(defensiveTSPI.getvZ()) + "\nXYZ : " + String.valueOf(defensiveTSPI.getxXYZ());
                    mTextVelocity.setText(Velocity);
                }
            });

            if (getEnableVirtualStick()) {
                calculateTSPI();
                Log.d("TaskCalculate", String.valueOf(getPitch()));

                send();
                Log.d("TaskSend", "Succeed updated data");
            }
        }

        public void calculateTSPI() {
            Log.d("calculateTSPI", "Run");

            //예상 움직임
//            후진 하강 -> 전진 상승 반복

            setPitch(this.pitch + temp);
            if (getPitch() > 4 || getPitch() < -4)
                temp = temp * -1;

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

    private void setResultToToast(final String string) {
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, string, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d("Map", "Map ready.");
        mMap = googleMap;

        LatLng initLocation = new LatLng(0.0, 0.0);

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

        mMap.setMyLocationEnabled(true);

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
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(curPoint, 18));
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
//        markerOur.zIndex(1);
        defensiveMarker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));

        //malicious Drone Marker
        LatLng malPosition = new LatLng(maliciousTSPI.getLatitude(), maliciousTSPI.getLongitude());

        final MarkerOptions maliciousMarker = new MarkerOptions();
        maliciousMarker.position(malPosition);
        defensiveMarker.title("MalDrone");
        defensiveMarker.snippet("MalDrone");
        maliciousMarker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));

//        //followMeMission Marker
//        LatLng tarPosition = new LatLng(targetLatitude, targetLongitude);
//
//        final MarkerOptions markerMission = new MarkerOptions();
//        markerMission.position(tarPosition);
//        markerMission.zIndex(0);
//        markerMission.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

        mMap. clear();

        droneMarker = mMap.addMarker(defensiveMarker);
        droneMarker = mMap.addMarker(maliciousMarker);
//        droneMarker = mMap.addMarker(markerMission);

        //drawPolyline();

        //drawLine
        //pathPoints.set(0, curPosition);
        //pathPoints.set(1, tarPosition);

        //below are rotations
        //markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.defensivedrone)); // 1.2cm*1.2cm TODO must adjust to middle.
        //markerOptions.anchor(0.0f,0.0f);
        //float angle = (float)mTSPI.getYaw();
        //markerOptions.rotation(angle);
        //rotation ends

        //LatLng tarPos = new LatLng(targetLatitude,targetLongitude);
    }

    private void selectColor(MarkerOptions markerOptions, String color) {

        switch (color) {
            case "red":
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                break;
            case "green":
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                break;
            default:
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
                break;
        }
    }

    private void drawPolyline() {

        PolylineOptions polylineOptions = new PolylineOptions();
        polylineOptions.color(Color.RED);
        polylineOptions.width(5);
        polylineOptions.addAll(pathPoints);
        mMap.addPolyline(polylineOptions);

    }

    private Date changeUnixTime(String unixTimeStamp) {

        long timestamp = Long.parseLong(unixTimeStamp);
        SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
        Date date = new Date();
        date.setTime(timestamp);

        return date;
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


