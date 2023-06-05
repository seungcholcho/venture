package com.dji.sdk.venture;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;

import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;

import dji.common.error.DJIError;
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
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, AdapterView.OnItemSelectedListener, View.OnClickListener {
    Handler handler = new Handler();
    Runnable runnable;
    int loadIntervals = 1000; //in ms. 1000ms = 1s
    int refreshTargetIntervals = 5000; //Refresh time to target input

    FlightController flightController;

    TSPI defensiveTSPI;

    TSPI maliciousTSPI;

    BackgroundCallback updateTSPI;

    private GoogleMap mMap;
    private Button mBtnStart, mBtnStop, mBtnDisable, mBtnEnable,
            mBtntmp1,mBtntmp2,mBtntmp3,mBtntmp4,mBtntmp5,mBtntmp6,mBtntmp7;
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
        mBtntmp3 = (Button) findViewById(R.id.btn_tmp3);
        mBtntmp4 = (Button) findViewById(R.id.btn_tmp4);
        mBtntmp5 = (Button) findViewById(R.id.btn_tmp5);
        mBtntmp6 = (Button) findViewById(R.id.btn_tmp6);
        mBtntmp7 = (Button) findViewById(R.id.btn_tmp7);

        mBtnEnable.setOnClickListener(this);
        mBtnDisable.setOnClickListener(this);

        mBtntmp1.setOnClickListener(this);
        mBtntmp2.setOnClickListener(this);
        mBtntmp3.setOnClickListener(this);
        mBtntmp4.setOnClickListener(this);
        mBtntmp5.setOnClickListener(this);
        mBtntmp6.setOnClickListener(this);
        mBtntmp7.setOnClickListener(this);
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
                    }
                });

                break;
            }
            case R.id.btn_disable: {
                Log.d("onClick", "Disable");

                flightController.setVirtualStickModeEnabled(false, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                    }
                });

                break;
            }
            case R.id.btn_tmp1: {
                //직진
                Log.d("onClick", "tmp1");

                sendDataTimer = new Timer();
                //Test1
                //Pitch, Roll, Yaw, Throttler 값들의 임계값 찾기.
                //0.5초후 backgroundVirtualStick이 2초에 한번씩 실행
                //sendDataTimer.schedule(backgroundVirtualStick,500,1000);

                //Test2
                //Sample Code주기랑 똑같이 설정해서 끊기는 현상 해결해보기.
                sendDataTimer.schedule(backgroundVirtualStick,100,200);

                break;
            }
            case R.id.btn_tmp2: {
                //우(횡) 이동
                Log.d("onClick", "tmp2");
                break;
            }
            case R.id.btn_tmp3: {
                // 제자리에서 오른쪽으로 회전
                Log.d("onClick", "tmp3");
                break;
            }
            case R.id.btn_tmp4: {
                // 고도 상승
                Log.d("onClick", "tmp4");
                break;
            }
            case R.id.btn_tmp5: {
                Log.d("onClick", "tmp5");
                break;
            }
            case R.id.btn_tmp6: {
                Log.d("onClick", "tmp6");
                break;
            }
            case R.id.btn_tmp7: {
                Log.d("onClick", "tmp7");
                //InputData 초기화
                backgroundVirtualStick.UpdateInputData(0,0,0,0);
            }
            default:
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
//        db = FirebaseFirestore.getInstance();
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
//        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
//        mapFragment.getMapAsync(this);

        defensiveTSPI = new TSPI();
        maliciousTSPI = new TSPI();

        try {
            flightController = ((Aircraft) DJISDKManager.getInstance().getProduct()).getFlightController();
            updateTSPI = new BackgroundCallback(defensiveTSPI, flightController);
            updateTSPI.start();

            backgroundVirtualStick = new BackgroundVirtualStick(flightController);

            //최대 고도 제한
            flightController.setMaxFlightHeight(100, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    Log.d("setFlightHeight","Completed");
                }
            });

            //최대 반경 제한
            flightController.setMaxFlightRadius(500, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    Log.d("setFlightRadius","Completed");
                }
            });

            //flight 상태 + Mission 상태 출력
            flightStates = "Flight state : " + defensiveTSPI.getFlightState().name() + "\nVirtualStickController : " + String.valueOf(flightController.isVirtualStickControlModeAvailable());
            mTextDistance.setText(flightStates);

            //Virtual Stick
            //속도
            flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
            //속도
            flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
            //각도
            flightController.setYawControlMode(YawControlMode.ANGLE);
            //드론 기준 수평되는지
            flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);

            Log.d("Controller Mode", "Test");
            Log.d("Controller Mode" , String.valueOf(flightController.getVerticalControlMode()));
            Log.d("Controller Mode" , String.valueOf(flightController.getRollPitchControlMode()));
            Log.d("Controller Mode" , String.valueOf(flightController.getYawControlMode()));
            Log.d("Controller Mode" , String.valueOf(flightController.getRollPitchCoordinateSystem()));

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
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
//        handler.postDelayed(runnable = new Runnable() {
//            //update location of our drone every loadIntervals seconds.
//            @Override
//            public void run() {
//                // Connection DB code
////                db.collection("0526_test").orderBy("Time", Query.Direction.DESCENDING).get()
////                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
////                            @Override
////                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
////                                if (task.isSuccessful()) {
////
////                                    int i = 0;
////                                    for (QueryDocumentSnapshot document : task.getResult()) {
////
////                                        //showToast(String.valueOf(document.getData()));
////
////                                        //String Time = String.valueOf(document.getData().get("Time").getClass().getName());
////                                        //Date Timestamp = changeUnixTime(Time);
////                                        String GpsSignal = (String) document.getData().get("GpsSignal");
////                                        double Altitude = (double) document.getData().get("Altitude");
////                                        double Latitude = (double) document.getData().get("Latitude");
////                                        double Longitude = (double) document.getData().get("Longitude");
////
////                                        Log.d("Firebase","Time : " + String.valueOf(document.getData().get("Time")));
////                                        Log.d("Firebase","GpsSignal : " + String.valueOf(document.getData().get("GpsSignal")));
////                                        Log.d("Firebase","Altitude : " + String.valueOf(document.getData().get("Altitude")));
////                                        Log.d("Firebase","Latitude : " + String.valueOf(document.getData().get("Latitude")));
////                                        Log.d("Firebase","Latitude : " + String.valueOf(document.getData().get("Latitude")));
////
////                                        maliciousTSPI.updateTSPIserver(GpsSignal,Altitude,Latitude,Longitude);
////
////                                        i++;
////                                        if (i == 1){
////                                            break;
////                                        }
////                                    }
////                                } else {
////                                    Log.w(TAG, "Error getting documents.", task.getException());
////                                }
////                            }
////                        });
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
//                                            "\nLon : " + String.valueOf(defensiveTSPI.getLongitude());
//                        mTextCLocation.setText(currentLocation);
//
//                        flightStates = "Flight state : " + defensiveTSPI.getFlightState().name() +
//                                        "\nVirtualStickController : " + String.valueOf(flightController.isVirtualStickControlModeAvailable());
//                        mTextDistance.setText(flightStates);
//
//                        TSPIState = "TSPI State\n Pitch : " + String.valueOf(defensiveTSPI.getPitch()) +
//                                        "\nYaw : " + String.valueOf(defensiveTSPI.getYaw()) +
//                                        "\nRoll : " + String.valueOf(defensiveTSPI.getRoll());
//                        mTextState.setText(TSPIState);
//
//                        InputDataState = "Input Data State\n Pitch : " + String.valueOf(backgroundVirtualStick.getPitch()) +
//                                        "\nYaw : " + String.valueOf(backgroundVirtualStick.getYaw()) +
//                                        "\nRoll : " + String.valueOf(backgroundVirtualStick.getRoll()) +
//                                        "\nThrottle : " + String.valueOf(backgroundVirtualStick.getThrottle());
//                        mTextVirtualState.setText(InputDataState);
//
//                        String Velocity = "Velocity\nX : " + String.valueOf(defensiveTSPI.getvX()) + "\nY: " + String.valueOf(defensiveTSPI.getvY())
//                                + "\nZ: " + String.valueOf(defensiveTSPI.getvZ()) + "\nXYZ : " + String.valueOf(defensiveTSPI.getxXYZ());
//                        mTextVelocity.setText(Velocity);
//
//
//                    }
//                });
//            }
//        }, loadIntervals);
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
//        Log.d("Map", "Map ready.");
//        mMap = googleMap;
//
//        LatLng initLocation = new LatLng(0.0, 0.0);
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
//        mMap.setMyLocationEnabled(true);
//
//        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
//
//        long minTime = 1000; //millisecond
//        float minDistance = 0;
//
//        // this location listener is to the device this application is running on only
//        // to follow a different device, we will need a check box or radio button of some sort to
//        // either pick to follow the good drone or enemy drone
//
//        // 현재 위치 표시
//        LocationListener listener = new LocationListener() {
//            @Override
//            public void onLocationChanged(@NonNull Location location) {
//                Double latitude = location.getLatitude();
//                Double longitude = location.getLongitude();
//                LatLng curPoint = new LatLng(latitude, longitude);
//                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(curPoint, 18));
//                locationManager.removeUpdates(this);
//            }
//        };
//
//        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, listener);

    }

    private void updateDroneLocation() {

        LatLng curPosition = new LatLng(defensiveTSPI.getLatitude(), defensiveTSPI.getLongitude());

        final MarkerOptions markerOur = new MarkerOptions();
        markerOur.position(curPosition);
        markerOur.title("OurDrone");
        markerOur.snippet("OurDrone");
        markerOur.zIndex(1);
        markerOur.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));

        //followMeMission Marker
        LatLng tarPosition = new LatLng(targetLatitude, targetLongitude);

        final MarkerOptions markerMission = new MarkerOptions();
        markerMission.position(tarPosition);
        markerMission.zIndex(0);
        markerMission.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));


        //malicious Marker
        LatLng malPosition = new LatLng(maliciousTSPI.getLatitude(), maliciousTSPI.getLongitude());

        final MarkerOptions markerMal = new MarkerOptions();
        markerMal.position(malPosition);
        markerMal.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));

        mMap.clear();

        droneMarker = mMap.addMarker(markerOur);
        droneMarker = mMap.addMarker(markerMission);
        droneMarker = mMap.addMarker(markerMal);

        //drawPolyline();

        //drawLine
//        pathPoints.set(0, curPosition);
//        pathPoints.set(1, tarPosition);

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

