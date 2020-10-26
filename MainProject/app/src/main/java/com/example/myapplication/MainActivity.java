package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
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
import android.widget.Button;
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
        OnMapReadyCallback {

    /*
        These variables are related to the map plugin.
            tomtomMap is a reference to the map plugin.
            searchApi is the reference to the search api that will be used to search for points of interest.
            routingApi is the reference to the API that will find the route between truck location and target location.
            route contains the reference to the route between truck location and target location.

            departurePosition is the latitude and longitude values of the position where the truck would depart from.
            destinationPosition is the latitude and longitude values of the position where the truck is supposed to go to.
            currentLocation will contain the reference to the current location of the truck/user.
            destinationIcon is a reference to an icon which will represent the destination in the app.
    */
    private TomtomMap tomtomMap;
    private SearchApi searchApi;
    private RoutingApi routingApi;
    private Route route;
    private LatLng departurePosition;
    private LatLng destinationPosition;
    private Location currentLocation;
    private LatLng wayPointPosition;
    private Icon destinationIcon;


    /*
        startButton contains the reference to the start button in the UI.
        updateButton contains the reference to the update button in the UI.

    */
    private Button startButton;
    private Button updateButton;


    /*
        These values represent the latitude and longitude range that the target location is supposed to be under.
    */
    private Double latUpperLimit = -30.0;
    private Double latLowerLimit = -40.0;
    private Double longUpperLimit = 150.0;
    private Double longLowerLimit = 135.0;

    /*
        These variables are to be used by the app to connect to the server and transfer info to the server.

        serverDomain is the domain of the server.
        truckID is the id that is designated to the truck by the server. The server will identify the truck using this id.
        collectedID is the id of the street that the truck last collected from.
    */
    public String serverDomain = "ec2-3-25-124-215.ap-southeast-2.compute.amazonaws.com:8080";//"ec2-54-252-219-65.ap-southeast-2.compute.amazonaws.com:8080";
    private int truckID;
    private String truckStatus = "active";
    private int collectedId = 0;

/*
    This function is a method that is called when the activity first starts.
    It sets up the activity and initializes all the necessary variables and registers the truck into the server.
 */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        initTomTomServices();
        initUIViews();


        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

            StrictMode.setThreadPolicy(policy);
        }


        startButton = (Button) findViewById(R.id.startButton);
        updateButton = (Button) findViewById(R.id.updateButton);

        registerTruck();


    }

/*
    This method is an asynchronous method that is run when the map is initialized and ready.
    This method will set up the necessary variables for the map.
*/
    @Override
    public void onMapReady(@NonNull final TomtomMap tomtomMap) {
        this.tomtomMap = tomtomMap;
        this.tomtomMap.setMyLocationEnabled(true);

        this.tomtomMap.getMarkerSettings().setMarkersClustering(true);

    }

/*
    This method checks if the user has granted access to internet and location of the user's device to the app.
    If not it will request for access.
*/
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        this.tomtomMap.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

/*
    This method checks if the destination position is set and returns the result.
*/
    private boolean isDestinationPositionSet() {
        return destinationPosition != null;
    }

/*
    This method will create an icon on the target location that is passed in as variable.
 */
    private void createMarkerIfNotPresent(LatLng position, Icon icon) {
        Optional<Marker> optionalMarker = tomtomMap.findMarkerByPosition(position);
        if (!optionalMarker.isPresent()) {
            tomtomMap.addMarker(new MarkerBuilder(position)
                    .icon(icon));
        }
    }

/*
    This method checks if the destination position is set and returns the result.
*/
    private boolean isDeparturePositionSet() {
        return departurePosition != null;
    }


/*
    This method displays a toast which notifies the user of the error.
*/
    private void handleApiError(Throwable e) {
        Toast.makeText(MainActivity.this, getString(R.string.api_response_error, e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
    }

/*
    This method returns the route a vehicle would take depending on the waypoints that they have to stop at.
*/
    private RouteQuery createRouteQuery(LatLng start, LatLng stop, LatLng[] wayPoints) {
        return (wayPoints != null) ?
                new RouteQueryBuilder(start, stop).withWayPoints(wayPoints).withRouteType(RouteType.FASTEST).build() :
                new RouteQueryBuilder(start, stop).withRouteType(RouteType.FASTEST).build();
    }

/*
    This method calls a helper method to draw a route between two locations on the map.
 */
    private void drawRoute(LatLng start, LatLng stop) {
        wayPointPosition = null;
        drawRouteWithWayPoints(start, stop, null);
    }

/*
    This method will draw a route between two locations on the map using inbuilt methods from TomtomMaps API.
 */
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
                                    fullRoute.getCoordinates()));
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        handleApiError(e);
                        clearMap();
                    }
                });
    }


/*
    This method initializes the map plugin and the required APIs from TomtomMaps API.
*/
    private void initTomTomServices() {
        MapFragment mapFragment = (MapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        mapFragment.getAsyncMap(this);

        searchApi = OnlineSearchApi.create(this);
        routingApi = OnlineRoutingApi.create(this);
    }

/*
    This method initalizes the icons that will be used by the app.
*/
    private void initUIViews() {
        destinationIcon = Icon.Factory.fromResources(MainActivity.this, R.drawable.ic_map_route_destination);
    }


/*
    This method will clear the map.
*/
    private void clearMap() {
        tomtomMap.clear();
        tomtomMap.getDrivingSettings().stopTracking();
        departurePosition = null;
        destinationPosition = null;
        route = null;
    }

/*
    This method will be called by the start button of this activity.
    This method will ping the server once to let the server know of the user's location.
    This method will then hide the start button and display the update button.
*/
    public void startJourneyButton(View view){

        if (tomtomMap.getUserLocation() != null){
            tomtomMap.centerOnMyLocation();
            currentLocation = tomtomMap.getUserLocation();
        }

        String[] temp = sendAlertToServer("active");

        startButton.setVisibility(View.GONE);
        updateButton.setVisibility(View.VISIBLE);


    }



/*
    This method will ping the server to get the location that the truck should go to.
    This methods returns a LatLng value or null depending on if the journey has ended or will continue.
*/
    public String[] sendAlertToServer(String status) {

        try {
            String id = Integer.toString(truckID);
            String lat1 = String.format("%f", currentLocation.getLatitude());
            String long1 = String.format("%f", currentLocation.getLongitude());
            String collectedID = Integer.toString(collectedId);

            String param = "/truck-status?id="+id+"&status="+status+"&Latitude="+lat1+"&Longitude="+long1;

            //Check if we have already collected from a street. If we have then we pass on the street's id to the server
            if (!(collectedID.equals("0"))){
                param = param +"&collected="+collectedID;
            }

            String temp = "http://"+serverDomain+param;

            String in = getInput(temp);

            String ok = "ok";

            // check's the response from the server. if the response is ok. then it checks if we should end the journey or not.
            if (ok.equals(in.toLowerCase())){
                if (!(collectedID.equals("0"))){
                    endJourney();
                }
                return null;
            }

            String[] t2 = in.split(" ");
            return t2;

        } catch (Exception e){
        }

        return null;
    }


/*
    This method will end the journey and take us back to the starting activity
*/
    public void endJourney(){
        Intent goToMain = new Intent(MainActivity.this, StartingActivity.class);
        startActivity(goToMain);
    }

/*
    This method will call the function endJourney.
*/
    public void endJourneyButton(View view){
        endJourney();
    }

/*
    This method will register the truck into the server.
*/

    public void registerTruck(){

        try {
            String temp = "http://" + serverDomain + "/register-truck";


            String in = getInput(temp);


            truckID = Integer.parseInt(in);

            if (truckID < 1) {
                registerTruck();
            }
        } catch (Exception e){

        }

    }



/*
    This method will open the url of the server and read and return the hypertext in string format.
*/
    public String getInput(String tempUrl){


        try {
            URL url = new URL(tempUrl);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            String tempReturn = new String(ByteStreams.toByteArray(in), Charsets.UTF_8);
            return tempReturn;
        } catch(Exception e){
        }
        return null;
    }


/*
    This method will call the function sendAlertToServer() and then display the return LatLng on the map
    and call emthod draw route to display a route between the truck location and target location on the map.
*/
    public void updateButtonOnClick(View view){

        if (tomtomMap.getUserLocation() != null){
            tomtomMap.centerOnMyLocation();
        }

        clearMap();
        currentLocation = tomtomMap.getUserLocation();
        departurePosition = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());


        try {
            String[] result  = sendAlertToServer("active");

            if (result == null){
                return;
            }


            Double newLat = Double.parseDouble(result[1]);
            Double newLong = Double.parseDouble(result[2]);


            if ((newLong > longUpperLimit || newLong < longLowerLimit) ||
                    (newLat > latUpperLimit || newLat < latLowerLimit)){
                Toast.makeText(MainActivity.this, "Location is not in Melbourne", Toast.LENGTH_SHORT).show();
            } else {
                int tempID = Integer.parseInt(result[0]);
                if (tempID > 0) {
                    collectedId = tempID;
                }
                destinationPosition = new LatLng(newLat, newLong);
                createMarkerIfNotPresent(destinationPosition, destinationIcon);
                drawRoute(departurePosition, destinationPosition);
            }


        } catch (Exception e){

        }
    }
}
