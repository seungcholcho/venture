package com.dji.sdk.venture;

import android.Manifest;
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
import com.google.android.gms.maps.model.LatLng;

import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dji.common.error.DJIError;
import dji.common.mission.followme.FollowMeHeading;
import dji.common.mission.followme.FollowMeMission;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.model.LocationCoordinate2D;
import dji.common.util.CommonCallbacks;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.followme.FollowMeMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

//Test
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, AdapterView.OnItemSelectedListener, View.OnClickListener, GoogleMap.OnMapClickListener {
    Handler handler = new Handler();
    Runnable runnable;
    int loadIntervals = 100; //in ms. 1000ms = 1s

    FlightController flightController;

    TSPI mTSPI;

    BackgroundCallback mTSPIlogger;

    private GoogleMap mMap;
    private Button mBtnInit, mBtnStart, mBtnStop;
    private TextView mTextTLocation,mTextCLocation;

    private String currentLocation;
    private String targetLocation;


    private double currentLatitude = 0;
    private double currentLongitude = 0;

    private double targetLatitude = 0;
    private double targetLongitude = 0;

    private static final double ONE_METER_OFFSET = 0.000009005520;

    //0.00000899322;

    private static final double ONE_METER_OFFSET_LON = 0.00000899322;


    private Marker droneMarker = null;

    private FollowMeMissionOperator followMeMissionOperator = null;

    @Override
    protected void onCreate(Bundle savedInstanceState){

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

        initUI();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mTSPI = new TSPI();

        try {
            flightController = ((Aircraft) DJISDKManager.getInstance().getProduct()).getFlightController();
            mTSPIlogger = new BackgroundCallback(mTSPI, flightController);
            mTSPIlogger.start();

            flightController.setMaxFlightHeight(100,new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                }
            });

            flightController.setMaxFlightRadius(1000,new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                }
            });

            //followMeMissionOpertor 객체 주입
            followMeMissionOperator = MissionControl.getInstance().getFollowMeMissionOperator();

        } catch (Exception e) {
            Log.d("FlightControllerState", "not Connected");
        }

        //현재위치 초기화
        currentLatitude = mTSPI.getLatitude();
        currentLongitude = mTSPI.getLongitude();
    }

    @Override
    protected void onPause(){
        super.onPause();
    }
    @Override
    protected void onResume() {

        super.onResume();

        handler.postDelayed(runnable = new Runnable() {
            //update location of our drone every loadIntervals seconds.
            @Override
            public void run() {

                handler.postDelayed(runnable,loadIntervals);

                //Mark on map in real time
                updateDroneLocation();

                //Change Textview
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        currentLocation = "Lat : " + String.valueOf(mTSPI.getLatitude()) + "  Lon : " + String.valueOf(mTSPI.getLongitude());
                        mTextCLocation.setText(currentLocation);

                        targetLocation = "Lat : " + String.valueOf(targetLatitude) + "  Lon : " + String.valueOf(targetLongitude);
                        mTextTLocation.setText(targetLocation);

                        //Distance target to Current value
                    }
                });
            }
        },loadIntervals);
    }

    public void onReturn(View view){
        this.finish();
    }

    private void initUI() {

        mBtnInit = (Button) findViewById(R.id.init);
        mBtnStart = (Button) findViewById(R.id.start);
        mBtnStop = (Button) findViewById(R.id.stop);
        mTextCLocation = (TextView)findViewById(R.id.text_current_location);
        mTextTLocation = (TextView)findViewById(R.id.text_target_location);

        mBtnInit.setOnClickListener(this);
        mBtnStart.setOnClickListener(this);
        mBtnStop.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.init:{
                Log.d("onClick","init");
                break;
            }
            case R.id.start:{
                Log.d("onClick","start");
                showToast("Mission Start");
                followMeMissionOperator.startMission(new FollowMeMission(FollowMeHeading.TOWARD_FOLLOW_POSITION,
                        currentLatitude + 1 * ONE_METER_OFFSET, currentLongitude, 30f
                ), new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        Log.d("MissionStart", "Start Suceess");
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                int cnt = 0;
                                while(cnt < 50) {
                                    targetLatitude = currentLatitude + 1 * ONE_METER_OFFSET;
                                    targetLongitude = currentLongitude;
                                    LocationCoordinate2D newLocation = new LocationCoordinate2D(targetLatitude, targetLongitude);

                                    followMeMissionOperator.updateFollowingTarget(newLocation, djiError1 -> {
                                        try {
                                            //Thread sleep 1/1000
                                            Thread.sleep(1500);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    });
                                    cnt++;
                                }
                                Log.d("MissionStart" , "Mission Success");
                                showToast("Mission Success");

                            }
                        }).start();
                    }
                });
                break;
            }
            case R.id.stop:{
                Log.d("onClick","stop");
                //followMeMissionOperator.stopMission(djiError -> ToastUtils.setResultToToast(djiError != null ? "" : djiError.getDescription()));
                break;
            }
            default:
                break;
        }
    }
    private void setResultToToast(final String string){
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
    public void onMapReady(GoogleMap googleMap)  {
        Log.d("Map", "Map ready.");
        mMap = googleMap;
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
        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                Double latitude = location.getLatitude();
                Double longitude = location.getLongitude();
                LatLng curPoint = new LatLng(latitude, longitude);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(curPoint,16));
                locationManager.removeUpdates(this);
            }
        };
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance,  listener);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
    }
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    private void updateDroneLocation(){

        LatLng pos = new LatLng(mTSPI.getLatitude(),mTSPI.getLongitude());

        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(pos);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));

        //below are rotations
        //markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.defensivedrone)); // 1.2cm*1.2cm TODO must adjust to middle.
        //markerOptions.anchor(0.0f,0.0f);
        //float angle = (float)mTSPI.getYaw();
        //markerOptions.rotation(angle);
        //rotation ends

        if (droneMarker != null) {
            droneMarker.remove();
        }

        droneMarker = mMap.addMarker(markerOptions);
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
