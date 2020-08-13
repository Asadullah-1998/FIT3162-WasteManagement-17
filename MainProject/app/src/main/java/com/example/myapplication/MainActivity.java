package com.example.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    SupportMapFragment smf;
    FusedLocationProviderClient client;
    Location location1;

    GoogleMap googleMap1;
    boolean haveInput;
    String targetAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        smf = (SupportMapFragment) getSupportFragmentManager().
                findFragmentById(R.id.google_map);

        client = LocationServices.getFusedLocationProviderClient(this);

        //EditText editAddress  = (EditText) findViewById(R.id.address_edit);
        //targetAddress = editAddress.getText().toString();

        if (ActivityCompat.checkSelfPermission(
                MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            /*if (!targetAddress.isEmpty()) {
                getLocationFromAddress(this, targetAddress);
            } else {
                getCurrentLocation();
            }*/
            getCurrentLocation();
//            updateMap();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},44);
        }
    }

    private void getCurrentLocation() {
        Task<Location> task = client.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                location1 = location;
                if (location1 != null) {
                    smf.getMapAsync(new OnMapReadyCallback() {
                        @Override
                        public void onMapReady(GoogleMap googleMap) {
                            LatLng latLing = new LatLng(location1.getLatitude(), location1.getLongitude());
                            MarkerOptions options = new MarkerOptions().position(latLing).title("Target");

                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLing, 10));

                            googleMap.addMarker(options);

                        }
                    });
                }
            }
        });

    }

    /*public void updateMap(){
        LatLng latLing = getLocationFromAddress(this,targetAddress);
        MarkerOptions options = new MarkerOptions().position(latLing).title("Target");

        googleMap1.animateCamera(CameraUpdateFactory.newLatLngZoom(latLing, 10));

        googleMap1.addMarker(options);
    }*/

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        //super.onRequestPermissionsResult(requestCode, pemissions, grantResults);
        if (requestCode == 44){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                getCurrentLocation();
            } /*else {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},44);
            }*/
        }
    }




    public LatLng getLocationFromAddress(Context context, String strAddress){
        Geocoder coder = new Geocoder(context);
        List<Address> address;
        LatLng p1 = null;

        try {
            address = coder.getFromLocationName(strAddress, 5);
            if (address == null) {
                return null;
            }
            Address location = address.get(0);
            p1 = new LatLng(location.getLatitude(), location.getLongitude() );
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return p1;
    }

    public void addressButtonOnCick(View view){
    }

    /*public void addressButtonOnCick(View view){
        EditText editAddress  = (EditText) findViewById(R.id.address_edit);
        targetAddress = editAddress.getText().toString();
        haveInput = true;
        //updateMap();
    }*/


        /*Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/
    //}

    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }*/
}
