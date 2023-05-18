package com.dji.sdk.venture;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;


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



public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, AdapterView.OnItemSelectedListener{
    Handler handler = new Handler();
    Runnable runnable;
    int loadIntervals = 100; //in ms. 1000ms = 1s

    private GoogleMap mMap;
    ArrayList<Drone> dronesToTrack = new ArrayList<>();
    Drone selectedDrone;
    Button trackDronebtn;
    TextView tspidata;
    TSPI mTSPI;
    TSPIlogger mTSPIlogger;

    private Marker droneMarker = null;

    @Override
    protected void onCreate(Bundle savedInstanceState){

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

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

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //tspidata = (TextView) findViewById(R.id.TSPIView);

        mTSPI = new TSPI();
        mTSPIlogger = new TSPIlogger(mTSPI);
        mTSPIlogger.start();

    }

    @Override
    protected void onResume() {
        handler.postDelayed(runnable = new Runnable() {
            //update location of our drone every loadIntervals seconds.
            @Override
            public void run() {

                //batteryView.setText(batteryResidual);
                handler.postDelayed(runnable,loadIntervals);

                updateDroneLocation();
            }
        },loadIntervals);
        super.onResume();
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

        setDroneMarkers();

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

    public void setDroneMarkers() {
        for (int i = 0; i < dronesToTrack.size(); i++ ) {
            Drone drone = dronesToTrack.get(i);
            LatLng location = drone.getLocation();
            String markerTitle = drone.getId();
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(location);
            markerOptions.title(markerTitle);
            markerOptions.draggable(false);
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            mMap.addMarker(markerOptions);
        }

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        selectedDrone = dronesToTrack.get(position);
        trackDronebtn.setVisibility(View.VISIBLE);

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        trackDronebtn.setVisibility(View.INVISIBLE);
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
