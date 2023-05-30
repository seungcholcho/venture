package com.dji.sdk.venture;

import static dji.log.GlobalConfig.TAG;

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
import androidx.annotation.Nullable;
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
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.android.gms.maps.model.PolylineOptions;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import dji.common.error.DJIError;
import dji.common.mission.followme.FollowMeHeading;
import dji.common.mission.followme.FollowMeMission;
import dji.common.mission.followme.FollowMeMissionEvent;
import dji.common.model.LocationCoordinate2D;
import dji.common.util.CommonCallbacks;
import dji.log.LogDialog;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.followme.FollowMeMissionOperator;
import dji.sdk.mission.followme.FollowMeMissionOperatorListener;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

//Test
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, AdapterView.OnItemSelectedListener, View.OnClickListener, GoogleMap.OnMapClickListener {
    Handler handler = new Handler();
    Runnable runnable;
    int loadIntervals = 1000; //in ms. 1000ms = 1s

    FlightController flightController;

    TSPI defensiveTSPI;

    TSPI maliciousTSPI;

    BackgroundCallback updateTSPI;

    private GoogleMap mMap;
    private Button mBtnInit, mBtnStart, mBtnStop;
    private TextView mTextTLocation, mTextCLocation, mTextDistance;

    private String currentLocation;
    private String targetLocation;
    private String flightStates;

    private double currentLatitude = 0;
    private double currentLongitude = 0;

    private double targetLatitude = 0;
    private double targetLongitude = 0;

    private static final double ONE_METER_OFFSET = 0.000009005520;
    //0.00000899322;

    private static final double ONE_METER_OFFSET_LON = 0.00000899322;

    private Marker droneMarker = null;

    private FollowMeMissionOperator followMeMissionOperator = null;

    private FollowMeMissionOperatorListener listener;

    List<LatLng> pathPoints = new ArrayList<>();

    private FirebaseFirestore db;

    private LatLng initLocation;

    private void initUI() {

        mBtnInit = (Button) findViewById(R.id.init);
        mBtnStart = (Button) findViewById(R.id.start);
        mBtnStop = (Button) findViewById(R.id.stop);
        mTextCLocation = (TextView) findViewById(R.id.text_current_location);
        mTextTLocation = (TextView) findViewById(R.id.text_target_location);
        mTextDistance = (TextView) findViewById(R.id.text_distance);

        mBtnInit.setOnClickListener(this);
        mBtnStart.setOnClickListener(this);
        mBtnStop.setOnClickListener(this);

    }

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

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        defensiveTSPI = new TSPI();
        maliciousTSPI = new TSPI();

        try {
            flightController = ((Aircraft) DJISDKManager.getInstance().getProduct()).getFlightController();
            updateTSPI = new BackgroundCallback(defensiveTSPI, flightController);
            updateTSPI.start();

            //최대 고도 제한
            flightController.setMaxFlightHeight(100, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                }
            });

            //최대 반경 제한
            flightController.setMaxFlightRadius(500, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                }
            });

            //followMeMissionOpertor 객체 주입
            followMeMissionOperator = MissionControl.getInstance().getFollowMeMissionOperator();

            //folloMeMission Listener 활성화
            setUpListener();


            //flight 상태 + Mission 상태 출력
            flightStates = "Flight state : " + defensiveTSPI.getFlightState().name() + "\nMission state : " + followMeMissionOperator.getCurrentState().getName();
            mTextDistance.setText(flightStates);

        } catch (Exception e) {
            Log.d("FlightControllerState", "not Connected");
        }

        //현재위치 초기화
        currentLatitude = defensiveTSPI.getLatitude();
        currentLongitude = defensiveTSPI.getLongitude();

        targetLatitude = defensiveTSPI.getLatitude();
        targetLongitude = defensiveTSPI.getLongitude();

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

        handler.postDelayed(runnable = new Runnable() {
            //update location of our drone every loadIntervals seconds.
            @Override
            public void run() {
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

                                        Log.d("Firebase","Time : " + String.valueOf(document.getData().get("Time")));
                                        Log.d("Firebase","GpsSignal : " + String.valueOf(document.getData().get("GpsSignal")));
                                        Log.d("Firebase","Altitude : " + String.valueOf(document.getData().get("Altitude")));
                                        Log.d("Firebase","Latitude : " + String.valueOf(document.getData().get("Latitude")));
                                        Log.d("Firebase","Latitude : " + String.valueOf(document.getData().get("Latitude")));

                                        maliciousTSPI.updateTSPIserver(GpsSignal,Altitude,Latitude,Longitude);

                                        i++;
                                        if (i == 1){
                                            break;
                                        }
                                    }
                                } else {
                                    Log.w(TAG, "Error getting documents.", task.getException());
                                }
                            }
                        });

                handler.postDelayed(runnable, loadIntervals);

                //Mark on map in real time
                updateDroneLocation();

                //Change Textview
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        currentLocation = "Lat : " + String.valueOf(defensiveTSPI.getLatitude()) + "\nLon : " + String.valueOf(defensiveTSPI.getLongitude());
                        mTextCLocation.setText(currentLocation);

//                        targetLocation = "Lat : " + String.valueOf(targetLatitude) + "\nLon : " + String.valueOf(targetLongitude);
                        targetLocation = "Lat : " + String.valueOf(followMeMissionOperator.getFollowingTarget().getLatitude()) + "\nLon : " + String.valueOf(followMeMissionOperator.getFollowingTarget().getLongitude());
                        mTextTLocation.setText(targetLocation);

                        //Distance target to Current value
                        flightStates = "Flight state : " + defensiveTSPI.getFlightState().name() + "\nMission state : " + followMeMissionOperator.getCurrentState().getName();
                        mTextDistance.setText(flightStates);
                    }
                });
            }
        }, loadIntervals);
    }

    public void onReturn(View view) {
        this.finish();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.init: {
                Log.d("onClick", "start");

                //위도 경도 위치 확인하기
                targetLatitude = 40.2248041984068;
                targetLongitude = -87.002733014605;

                double tmp = maliciousTSPI.getLatitude();

                followMeMissionOperator.startMission(new FollowMeMission(FollowMeHeading.TOWARD_FOLLOW_POSITION,currentLatitude,currentLongitude,30f
                ), new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        Log.d("MissionStart", "Mission Start");

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                //showToast("onResult in Thread run");

                            }
                        }).start();
                    }
                });
                break;
            }
            case R.id.start: {
                //showToast("onClickStart");
                Log.d("onClick", "start");
                //showToast("Mission Start");

                targetLatitude = defensiveTSPI.getLatitude();
                targetLongitude = defensiveTSPI.getLongitude();

                double tmp = maliciousTSPI.getLatitude();

                //showToast(String.valueOf(followMeMissionOperator.getCurrentState()));

                Log.d("MissionStart","before Mission start");
                followMeMissionOperator.startMission(new FollowMeMission(FollowMeHeading.TOWARD_FOLLOW_POSITION,
                        currentLatitude + 1 * ONE_METER_OFFSET, currentLongitude, 30f
                ), new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        Log.d("MissionStart", "Mission Start");

                        //ToDo Thread 객체화 하기(OnPause, Stop.onClickListener Thread.interrupt 넣기)
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                int cnt = 0;
                                while (cnt < 5) {

                                    targetLatitude = targetLatitude + 1 * ONE_METER_OFFSET;
                                    targetLongitude = targetLongitude * 1;

                                    LocationCoordinate2D newLocation = new LocationCoordinate2D(targetLatitude, targetLongitude);

                                    followMeMissionOperator.updateFollowingTarget(newLocation, djiError1 -> {
                                        try {
                                            //Thread sleep 1/1000
                                            Thread.sleep(1000);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    });
                                    cnt++;
                                }
                                //showToast("Mission Ended");
                            }
                        }).start();
                    }
                });
                break;
            }
            case R.id.stop: {
                Log.d("onClick", "stop");
                followMeMissionOperator.stopMission(djiError -> showToast("Mission Stop"));
                break;
            }
            default:
                break;
        }
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
    public void onMapClick(LatLng point) {
        setResultToToast("touching map");

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

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
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
        LatLng malPosition = new LatLng(maliciousTSPI.getLatitude(),maliciousTSPI.getLongitude());

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

    private void setUpListener() {
        // Example of Listener
        //showToast("Active Listener");
        listener = new FollowMeMissionOperatorListener() {
            @Override
            public void onExecutionUpdate(@NonNull @NotNull FollowMeMissionEvent followMeMissionEvent) {
                // Example of Execution Listener
                showToast("Update in listener");
                Log.d("onExcutionUpdate","onExcutionUpdate");

//                Log.d("FollowMeMissionListener Active",
//                        (followMeMissionEvent.getPreviousState() == null
//                                ? ""
//                                : followMeMissionEvent.getPreviousState().getName())
//                                + ", "
//                                + followMeMissionEvent.getCurrentState().getName()
//                                + ", "
//                                + followMeMissionEvent.getDistanceToTarget()
//                                + ", "
//                                + followMeMissionEvent.getError().getDescription());
                //updateFollowMeMissionState();
            }

            @Override
            public void onExecutionStart() {
                Log.d("setuplistener" , "onExecutionStart");
                //showToast("Mission started");
                //updateFollowMeMissionState();
            }

            @Override
            public void onExecutionFinish(@Nullable @org.jetbrains.annotations.Nullable DJIError djiError) {
                Log.d("setuplistener" , "onExecutionFinish");
                //showToast("Mission finished");
                //updateFollowMeMissionState();
            }
        };

        //listener.onExecutionStart();
        followMeMissionOperator.addListener(listener);

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


}

