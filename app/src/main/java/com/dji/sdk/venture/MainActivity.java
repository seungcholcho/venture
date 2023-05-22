package com.dji.sdk.venture;

import static org.greenrobot.eventbus.EventBus.TAG;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
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
import android.widget.LinearLayout;
import android.widget.RadioGroup;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dji.common.error.DJIError;
import dji.common.mission.followme.FollowMeHeading;
import dji.common.mission.followme.FollowMeMission;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.model.LocationCoordinate2D;
import dji.common.util.CommonCallbacks;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.followme.FollowMeMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

//Test
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, AdapterView.OnItemSelectedListener, View.OnClickListener, GoogleMap.OnMapClickListener {
    Handler handler = new Handler();
    Runnable runnable;
    int loadIntervals = 100; //in ms. 1000ms = 1s

    FlightController flightController;

    TSPI mTSPI;

    TSPIlogger mTSPIlogger;
    //TextView tspidata;

    private GoogleMap mMap;
    private Button mBtnInit, mBtnStart, mBtnStop;
    private TextView mTextTLocation;
    private TextView mTextCLocation;

    private String currentLocation;

    private double currentLatitude = 0;
    private double currentLongitude = 0;

    private static final double ONE_METER_OFFSET = 0.00000899322;

    private Marker droneMarker = null;
    private final Map<Integer, Marker> mMarkers = new ConcurrentHashMap<Integer, Marker>();
    private List<Waypoint> waypointList = new ArrayList<>();

    public static WaypointMission.Builder waypointMissionBuilder;
    private WaypointMissionOperator instance;
    private WaypointMissionFinishedAction mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
    private WaypointMissionHeadingMode mHeadingMode = WaypointMissionHeadingMode.AUTO;

    private float altitude = 100.0f;
    private float mSpeed = 10.0f;

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

        try {
            flightController = ((Aircraft) DJISDKManager.getInstance().getProduct()).getFlightController();
            mTSPI = new TSPI();
            mTSPIlogger = new TSPIlogger(mTSPI, flightController);
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
        currentLatitude = mTSPI.getCurrentLatitude();
        currentLongitude = mTSPI.getCurrentLongitude();
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

                //int loadIntervals = 100; //in ms. 1000ms = 1s
                handler.postDelayed(runnable,loadIntervals);
                //Log.d("Hello from MAPP","I am running in background");

                //Mark on map in real time
                updateDroneLocation();

                //Change Textview
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        currentLocation = "Lat : " + String.valueOf(mTSPI.getCurrentLatitude()) + "/nLon : " + String.valueOf(mTSPI.getCurrentLongitude());
                        mTextCLocation.setText(currentLocation);

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
                followMeMissionOperator.startMission(new FollowMeMission(FollowMeHeading.TOWARD_FOLLOW_POSITION,
                        currentLatitude + 5 * ONE_METER_OFFSET, currentLongitude + 5 * ONE_METER_OFFSET, 30f
                ), new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        Log.d("MissionStart", "Start Suceess");
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                int cnt = 0;
                                while(cnt < 100) {
                                    currentLatitude = currentLatitude + 5 * ONE_METER_OFFSET;
                                    currentLongitude = currentLongitude + 5 * ONE_METER_OFFSET;
                                    LocationCoordinate2D newLocation = new LocationCoordinate2D(currentLatitude, currentLongitude);
                                    followMeMissionOperator.updateFollowingTarget(newLocation, djiError1 -> {
                                        try {
                                            Thread.sleep(1500);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    });
                                    cnt++;
                                }
                            }
                        }).start();
                    }
                });

                break;
            }
            case R.id.stop:{
                Log.d("onClick","stop");
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

        LatLng pos = new LatLng(mTSPI.getCurrentLatitude(),mTSPI.getCurrentLongitude());

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



}
