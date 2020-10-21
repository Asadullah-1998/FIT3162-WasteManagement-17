package com.example.myapplication;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.io.ByteStreams;
import com.tomtom.online.sdk.common.location.LatLng;
import com.tomtom.online.sdk.location.LocationUpdateListener;
import com.tomtom.online.sdk.map.CameraPosition;
import com.tomtom.online.sdk.map.Icon;
import com.tomtom.online.sdk.map.MapFragment;
import com.tomtom.online.sdk.map.Marker;
import com.tomtom.online.sdk.map.MarkerBuilder;
import com.tomtom.online.sdk.map.OnMapReadyCallback;
import com.tomtom.online.sdk.map.Route;
import com.tomtom.online.sdk.map.RouteBuilder;
import com.tomtom.online.sdk.map.TomtomMap;
import com.tomtom.online.sdk.map.TomtomMapCallback;
import com.tomtom.online.sdk.routing.OnlineRoutingApi;
import com.tomtom.online.sdk.routing.RoutingApi;
import com.tomtom.online.sdk.routing.data.FullRoute;
import com.tomtom.online.sdk.routing.data.RouteQuery;
import com.tomtom.online.sdk.routing.data.RouteQueryBuilder;
import com.tomtom.online.sdk.routing.data.RouteResponse;
import com.tomtom.online.sdk.routing.data.RouteType;
import com.tomtom.online.sdk.search.OnlineSearchApi;
import com.tomtom.online.sdk.search.SearchApi;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements
        LocationUpdateListener, OnMapReadyCallback, TomtomMapCallback.OnMapClickListener {

    private TomtomMap tomtomMap;
    private SearchApi searchApi;
    private RoutingApi routingApi;
    private Route route;
    private LatLng departurePosition;
    private LatLng destinationPosition;
    private LatLng wayPointPosition;
    private Icon departureIcon;
    private Icon destinationIcon;


    private EditText addressEdit;
    private String targetAddress;

    private TextView mainView;

    private Location currentLocation;
    private Location targetLocation;


    private int collectedId = 0;

    private ActionBar ab;


    public String serverDomain = "ec2-54-252-219-65.ap-southeast-2.compute.amazonaws.com:8080";
    private int truckID;
    private String truckStatus;


    private TextView truckIDView;
    private TextView truckLocationView;
    private TextView destinationLocationView;

    private LatLng[] pickUpLocations = new LatLng[10];

    private LatLng[] pickUpLocations1 = new LatLng[3];



    private double startLat = -37.907803;
    private double startLong = 145.133957;

    private int currentLocationIndex = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        initTomTomServices();
        initUIViews();
        setupUIViewListeners();
        initPickUpLocations();


        initSetViews();

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

            StrictMode.setThreadPolicy(policy);
        }

        registerTruck();






    }



    public void initPickUpLocations(){
        for (int i = 0; i < 10; i++){
            LatLng tempLocation = new LatLng(startLat + i + 1, startLong + i + 1);
            pickUpLocations[i] = tempLocation;
        }

        LatLng l = new LatLng(-37.8771, 145.0449);
        LatLng l2 = new LatLng(-38.1526, 145.1361);
        LatLng l3 = new LatLng(-36.1631, 145.1371);

        pickUpLocations1[0] = l;
        pickUpLocations1[1] = l2;
        pickUpLocations1[2] = l3;

    }

    @Override
    public void onMapReady(@NonNull final TomtomMap tomtomMap) {
        this.tomtomMap = tomtomMap;
        this.tomtomMap.setMyLocationEnabled(true);

        this.tomtomMap.addOnMapClickListener(this);
        this.tomtomMap.getMarkerSettings().setMarkersClustering(true);

    }

    void setOnMapChangedListener(TomtomMapCallback.OnMapChangedListener onMapChangedListener){
        if (tomtomMap.getUserLocation() != null){
            mainView.setText("i AM INIT");
        } else{
            mainView.setText("i AM Not INIT");
        }
        tomtomMap.centerOnMyLocation();
        centerOnLocation();
    }

    @Override
    public void onMapClick(@NonNull LatLng latLng) {
        if (tomtomMap.getUserLocation() != null) {
            currentLocation = tomtomMap.getUserLocation();
            tomtomMap.centerOnMyLocation();
        }

    }

    public void startUpdate(){
        currentLocation = tomtomMap.getUserLocation();
        if (currentLocation != null){
            tomtomMap.centerOnMyLocation();
            centerOnLocation();
        } else {
            startUpdate();
        }
    }


    public void onLocationChanged(Location location) {
        currentLocation = tomtomMap.getUserLocation();
        centerOnLocation();
    }

    public void setCurrentLocation(){
        currentLocation = tomtomMap.getUserLocation();
        if (currentLocation != null) {
            String temp = String.format("%.4f, %.4f", currentLocation.getLatitude(), currentLocation.getLongitude());
            truckLocationView.setText(temp);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        this.tomtomMap.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void centerOnLocation(){
        mainView.setText(tomtomMap.getUserLocation().toString());
    }

    private boolean isDestinationPositionSet() {
        return destinationPosition != null;
    }

    private void createMarkerIfNotPresent(LatLng position, Icon icon) {
        Optional<Marker> optionalMarker = tomtomMap.findMarkerByPosition(position);
        if (!optionalMarker.isPresent()) {
            tomtomMap.addMarker(new MarkerBuilder(position)
                    .icon(icon));
        }
    }

    private boolean isDeparturePositionSet() {
        return departurePosition != null;
    }

    private void handleApiError(Throwable e) {
        Toast.makeText(MainActivity.this, getString(R.string.api_response_error, e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
    }

    private RouteQuery createRouteQuery(LatLng start, LatLng stop, LatLng[] wayPoints) {
        return (wayPoints != null) ?
                new RouteQueryBuilder(start, stop).withWayPoints(wayPoints).withRouteType(RouteType.FASTEST).build() :
                new RouteQueryBuilder(start, stop).withRouteType(RouteType.FASTEST).build();
    }

    private void drawRoute(LatLng start, LatLng stop) {
        wayPointPosition = null;
        drawRouteWithWayPoints(start, stop, null);
    }

    private void drawRouteWithWayPoints(LatLng start, LatLng stop, LatLng[] wayPoints) {
        RouteQuery routeQuery = createRouteQuery(start, stop, wayPoints);
        routingApi.planRoute(routeQuery)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableSingleObserver<RouteResponse>() {

                    @Override
                    public void onSuccess(RouteResponse routeResponse) {
                        displayRoutes(routeResponse.getRoutes());
                        tomtomMap.displayRoutesOverview();
                    }

                    private void displayRoutes(List<FullRoute> routes) {
                        for (FullRoute fullRoute : routes) {
                            route = tomtomMap.addRoute(new RouteBuilder(
                                    fullRoute.getCoordinates()).startIcon(departureIcon).endIcon(destinationIcon));
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        handleApiError(e);
                        clearMap();
                    }
                });
    }

    private void initTomTomServices() {
        MapFragment mapFragment = (MapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        mapFragment.getAsyncMap(this);

        searchApi = OnlineSearchApi.create(this);
        routingApi = OnlineRoutingApi.create(this);
    }

    private void initUIViews() {
        departureIcon = Icon.Factory.fromResources(MainActivity.this, R.drawable.ic_map_route_departure);
        destinationIcon = Icon.Factory.fromResources(MainActivity.this, R.drawable.ic_map_route_destination);
    }
    private void setupUIViewListeners() {}

    private void clearMap() {
        tomtomMap.clear();
        departurePosition = null;
        destinationPosition = null;
        route = null;
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

    public LatLng sendAlertToServer(String id, String lat, String long1, String status, String collectedID){
        String param = "/truck-status?id="+id+"&status="+status+"&Latitude="+lat+"&Longitude="+long1;

        if (!(collectedID.equals("0"))){
            param = param +"&collected="+collectedID;
        }

        String temp = "http://"+serverDomain+param;


        try {
            String in = getInput(temp);
            String text = mainView.getText().toString() + "\n";
            mainView.setText(text + "" + in);

            String ok = "ok";

            if (ok.equals(in.toLowerCase())){
                if (!(collectedID.equals("0"))){
                    mainView.setText("Journey is complete");
                }
                return null;
            }

            String[] t2 = in.split(" ");
            collectedId = Integer.parseInt(t2[0]);
            Double newLat = Double.parseDouble(t2[1]);
            Double newLong = Double.parseDouble(t2[2]);

            return new LatLng(newLat, newLong);

        } catch (Exception e){
            mainView.setText(e.toString());
        }

        return null;
    }

    public void registerTruck(){
        String temp = "http://"+serverDomain+"/register-truck";


        String in = getInput(temp);
        String text = mainView.getText().toString() + "\n";
        mainView.setText(text+"Registration Number: "+in);

        truckID = Integer.parseInt(in);

    }


    public String getInput(String tempUrl){


        try {
            URL url = new URL(tempUrl);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            String tempReturn = new String(ByteStreams.toByteArray(in), Charsets.UTF_8);
            mainView.setText(tempReturn);
            return tempReturn;
        } catch(Exception e){
            mainView.setText(e.toString());
        }
        return null;
    }

    public void updateButtonOnClick(View view){

        if (tomtomMap.getUserLocation() != null){
            tomtomMap.centerOnMyLocation();
        }

        clearMap();
        currentLocation = tomtomMap.getUserLocation();
        departurePosition = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());


        createMarkerIfNotPresent(departurePosition, departureIcon);


        try {
            String intString = Integer.toString(truckID);
            String latString = String.format("%f", currentLocation.getLatitude());
            String longString = String.format("%f", currentLocation.getLongitude());
            String collectedString = Integer.toString(collectedId);
            String text = mainView.getText().toString() + "\n";
            mainView.setText(text+"Registration Number: "+ intString + latString + longString);


            destinationPosition = sendAlertToServer(intString, latString, longString, "active", collectedString);

            if (destinationPosition == null){
                return;
            }

            createMarkerIfNotPresent(destinationPosition, destinationIcon);
            drawRoute(departurePosition, destinationPosition);


        } catch (Exception e){
            String text = mainView.getText().toString() + "\n";
            mainView.setText(e.toString());
        }



    }





    public void initSetViews(){
        mainView = (TextView) findViewById(R.id.textViewMain);
    }
}
