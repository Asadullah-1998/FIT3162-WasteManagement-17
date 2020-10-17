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

    private ActionBar ab;


    //public String serverDomain = "ec2-13-210-198-230.ap-southeast-2.compute.amazonaws.com:8080";
    public String serverDomain = "13.210.198.230:8080";
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

        //registerTruck();






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


        /*if (tomtomMap.getUserLocation() != null){
            mainView.setText("i AM INIT");
        } else{
            mainView.setText("i AM Not INIT");
        }*/
        //registerTruck();
        //currentLocation = tomtomMap.getUserLocation();
        //initSetViews();
        //setCurrentLocation();
        //tomtomMap.centerOnMyLocation();
        //registerTruck();
        //centerOnLocation();
        //this.tomtomMap.addOnMapLongClickListener(this);
        this.tomtomMap.addOnMapClickListener(this);
        this.tomtomMap.getMarkerSettings().setMarkersClustering(true);

        //startUpdate();

        /*if (tomtomMap.getUserLocation() != null){
            mainView.setText("i AM INIT");
        } else{
            mainView.setText("i AM Not INIT");
            //onMapReady(tomtomMap);
        }*/
    }

    void setOnMapChangedListener(TomtomMapCallback.OnMapChangedListener onMapChangedListener){
        if (tomtomMap.getUserLocation() != null){
            mainView.setText("i AM INIT");
        } else{
            mainView.setText("i AM Not INIT");
            //onMapReady(tomtomMap);
        }
        tomtomMap.centerOnMyLocation();
        centerOnLocation();
    }

    @Override
    public void onMapClick(@NonNull LatLng latLng) {
        if (tomtomMap.getUserLocation() != null) {
            currentLocation = tomtomMap.getUserLocation();
            //centerOnLocation();
            tomtomMap.centerOnMyLocation();
        }

    }

    public void startUpdate(){
        //tomtomMap.centerOnMyLocation();
        currentLocation = tomtomMap.getUserLocation();
        if (currentLocation != null){
            tomtomMap.centerOnMyLocation();
            //tomtomMap.centerOnMyLocation();
            centerOnLocation();
        } else {
            startUpdate();
        }
    }


    public void onLocationChanged(Location location) {
        //String temp = String.format("%.4f, %.4f", location.getLatitude(), location.getLongitude());
        //truckLocationView.setText(temp);
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
        /*if (tomtomMap.getUserLocation() != null){
            tomtomMap.centerOn(CameraPosition.builder()
                    .focusPosition(new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude()))
                    .zoom(10)
                    .bearing(3)
                    .build());
        }*/

        mainView.setText(tomtomMap.getUserLocation().toString());


        /*if (targetLocation == null){
            /*tomtomMap.centerOn(CameraPosition.builder()
                    .focusPosition(new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude()))
                    .zoom(10)
                    .bearing(3)
                    .build());*/

        //  LatLng focPos = new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude());
        //CameraPosition camPos = CameraPosition.builder().focusPosition(focPos).build();
        //CameraPosition camPos = CameraPosition.builder().build();
        //tomtomMap.centerOn(camPos);
        //LatLng focPos = new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude());
        //CameraPosition camPos = new CameraPosition(facPos,10,2,2,3);
        //}

       /* tomtomMap.centerOn(CameraPosition.builder()
                .focusPosition(new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude()))
                .zoom(10)
                .bearing(3)
                .build());*/


        //tomtomMap.centerOn(CameraPosition)
        //cameraFocusArea, //new AnimationDuration(1500, TimeUnit.MILLISECONDS));
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
        //currentLocation = tomtomMap.getUserLocation();
        //centerOnLocation();

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
        //departurePosition = null;
        destinationPosition = null;
        route = null;
    }

    /*public void addressButtonOnClick(View view){
        addressEdit  = (EditText) findViewById(R.id.addressEdit);
        targetAddress = addressEdit.getText().toString();

        currentLocation = tomtomMap.getUserLocation();

        departurePosition = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

        if (currentLocation == null){
            //destinationLocationView.setText("I am not working");
        } else{
            //setCurrentLocation();
        }


        createMarkerIfNotPresent(departurePosition, departureIcon);


        sendAlertToServer(Integer.toString(truckID), String.valueOf(currentLocation.getLatitude()),
                String.valueOf(currentLocation.getLongitude()), "active", "0");


        if (targetAddress != null){
            //destinationPosition = getLocationFromAddress(this, targetAddress);
            //createMarkerIfNotPresent(destinationPosition, destinationIcon);
            //drawRoute(departurePosition, destinationPosition);
        }
    }*/


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

    public void sendAlertToServer(String id, String lat, String long1, String status, String collectedID){
        String param = "/truck-status?id="+id+"&status="+status+"&Latitude="+lat+"&Longitude="+long1;
        //http://ec2-54-252-248-106.ap-southeast-2.compute.amazonaws.com/register-truck?
        // id=5&status=active&Latitude=34.201&Longitude=151.410
        String temp = "http://"+serverDomain+param;
        String in = getInput(temp);
        //String in = temp;
        String text = mainView.getText().toString() + "\n";
        mainView.setText(text+""+in);

        //String t1 = "2 -38.1526 145.1361 6 34.088 151.410";
        /*8String[] t2 = in.split(" ");
        Double newLat = Double.parseDouble(t2[1]);
        Double newLong = Double.parseDouble(t2[2]);*/

        //mainView.setText(t2[1]+t2[2]);

        //LatLng newLatLng = new LatLng(newLat,newLong);

        //return newLatLng;



    }

    public void registerTruck(){
        String temp = "http://"+serverDomain+"/register-truck";
        //String in = getInput(temp);

        String in = getInput(temp);
        //String in = temp;
        //truckIDView.setText(in);
        String text = mainView.getText().toString() + "\n";
        mainView.setText(text+"Registration Number: "+in);

        truckID = Integer.parseInt(in);

    }


    public String getInput(String tempUrl){


        try {
            URL url = new URL(tempUrl);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            //String tempReturn = IOUtils.toString(in);
            String tempReturn = new String(ByteStreams.toByteArray(in), Charsets.UTF_8);
            return tempReturn;
            //readStream(in);
        } catch(Exception e){
            mainView.setText(e.toString());
        }
        return null;
    }

    public void updateButtonOnClick(View view){

        clearMap();
        currentLocation = tomtomMap.getUserLocation();
        departurePosition = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        destinationPosition = pickUpLocations1[currentLocationIndex];

        createMarkerIfNotPresent(departurePosition, departureIcon);

        //createMarkerIfNotPresent(destinationPosition, destinationIcon);
        //drawRoute(departurePosition, destinationPosition);

        try {
            String intString = Integer.toString(truckID);
            String latString = String.format("%f", currentLocation.getLatitude());
            String longString = String.format("%f", currentLocation.getLongitude());
            String text = mainView.getText().toString() + "\n";
            mainView.setText(text+"Registration Number: "+ intString + latString + longString);

            sendAlertToServer(intString, latString, longString, "active", "0");

            //createMarkerIfNotPresent(destinationPosition, destinationIcon);
            //drawRoute(departurePosition, destinationPosition);

        } catch (Exception e){
            String text = mainView.getText().toString() + "\n";
            mainView.setText(e.toString());
        }


        //String para = Integer.toString(truckID) + String.format("%f",currentLocation.getLatitude()) +
        //      String.format("%f",currentLocation.getLongitude()) + "active" + "0";

        //String text = mainView.getText().toString() + "\n";
        //mainView.setText(text+"Registration Number: "+ intString);

        /*sendAlertToServer(Integer.toString(truckID), String.valueOf(currentLocation.getLatitude()),
                String.valueOf(currentLocation.getLongitude()), "active", "0");*/

        createMarkerIfNotPresent(destinationPosition, destinationIcon);
        drawRoute(departurePosition, destinationPosition);

        currentLocationIndex++;
    }





    public void initSetViews(){
        mainView = (TextView) findViewById(R.id.textViewMain);
        //endButton = (TextView) findViewById(R.id.truckLocationView);
        //destinationLocationView = (TextView) findViewById(R.id.destinationLocationView);
    }

    /*StringReuest stringRequest = new StringRequest(TextLinks.Request.Method.GET, url,
            new Responqse.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    // Display the first 500 characters of the response string.
                    textView.setText("Response is: "+ response.substring(0,500));
                }
            }, new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            textView.setText("That didn't work!");
        }
    });*/


    /*public class GetInputFrom extends AsyncTask {

        @Override
        protected Object doInBackground(Object... urls) {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                //String tempReturn = IOUtils.toString(in);
                String tempReturn = new String(ByteStreams.toByteArray(in), Charsets.UTF_8);
                return tempReturn;
                //readStream(in);
            } catch(Exception e){
                //mainView.setText(e.toString());
            }
            return null;
        }

        /*protected String doInBackground(String... urls){
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                //String tempReturn = IOUtils.toString(in);
                String tempReturn = new String(ByteStreams.toByteArray(in), Charsets.UTF_8);
                return tempReturn;
                //readStream(in);
            } catch(Exception e){
                //mainView.setText(e.toString());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
        }
    }*/




}
