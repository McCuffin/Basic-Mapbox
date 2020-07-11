package com.example.basic_mapbox;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, PermissionsListener {

    private MapView mapView;
    private MapboxMap mapboxMap;

    protected static DirectionsRoute currentRoute;
    private NavigationMapRoute navigationMapRoute;

    protected static Location yourLocation;
    private Point destinationLocation;
    private Point origin;
    private ArrayList<Point> waypoints = new ArrayList<>();
    private ArrayList<Feature> features = new ArrayList<>();
    private static final String TAG = "MainActivity";

    private PermissionsManager permissionsManager;

    private static final String SOURCE_ID = "SOURCE_ID";
    private static final String ICON_ID = "ICON_ID";
    private static final String LAYER_ID = "LAYER_ID";

    private ImageButton navigationButton;
    private ImageButton waypointButton;

    private static final int REQUEST_CODE_AUTOCOMPLETE = 1;
    private static final int REQUEST_CODE_AUTOCOMPLETE_WAYPOINT = 2;
    //private CarmenFeature home;
    //private CarmenFeature work;
    private String geojsonSourceLayerId = "geojsonSourceLayerId";

    private EditText searchLocation;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_main);
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    private void locationEnabled() {
        //TODO: Have to start the GPS
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!gps_enabled && !network_enabled) {
            new AlertDialog.Builder(MainActivity.this)
                    .setMessage("GPS Enable")
                    .setPositiveButton("Settings", new
                            DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                                    startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                                }
                            })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    @Override
    @SuppressWarnings({"MissingPermission"})
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        MainActivity.this.mapboxMap = mapboxMap;

        mapboxMap.setStyle(new Style.Builder().fromUri("mapbox://styles/mapbox/streets-v11"),
                new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        enableLocationComponent(style);

                        initSearchFab();
                        //addUserLocations();
                        setUpSource(style);
                        setupLayer(style);
                    }
                });
    }

    private void generateRoute() {
        origin = Point.fromLngLat(yourLocation.getLongitude(), yourLocation.getLatitude());

        NavigationRoute.Builder routeBuilder = NavigationRoute.builder(this)
                .accessToken(getString(R.string.access_token))
                .origin(origin)
                .profile(DirectionsCriteria.PROFILE_DRIVING)
                .destination(destinationLocation);

        Log.d(TAG, "generateRoute: Total Waypoints : "+waypoints.size());
        for(Point p:waypoints){
            routeBuilder.addWaypoint(p);
            Log.d(TAG, "generateRoute: "+p.latitude()+" , "+p.longitude());
        }
        NavigationRoute newCurrRoute = routeBuilder.build();

        newCurrRoute.getRoute(new Callback<DirectionsResponse>() {
            @Override
            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                Log.d(TAG, "onResponse: "+response);
                Timber.d("Response code: %s", response.code());
                if (response.body() == null) {
                    Timber.e("No routes found, make sure you set the right user and access token.");
                    return;
                } else if (response.body().routes().size() < 1) {
                    Timber.e("No routes found");
                    return;
                }

                currentRoute = response.body().routes().get(0);

                // Draw the route on the map
                if (navigationMapRoute != null) {
                    navigationMapRoute.removeRoute();
                } else {
                    navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute);
                }
                navigationMapRoute.addRoute(currentRoute);
            }

            @Override
            public void onFailure(@NonNull Call<DirectionsResponse> call, @NonNull Throwable throwable) {
                Timber.e("Error: %s", throwable.getMessage());
            }
        });
    }

    @SuppressWarnings({"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
// Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            locationEnabled();
// Get an instance of the component
            LocationComponent locationComponent = mapboxMap.getLocationComponent();

// Activate with options
            locationComponent.activateLocationComponent(
                    LocationComponentActivationOptions.builder(this, loadedMapStyle).build());

// Enable to make component visible
            locationComponent.setLocationComponentEnabled(true);
//Getting current location of user and adding an icon over there
            yourLocation = locationComponent.getLastKnownLocation();
            try {
                Feature currLocation = Feature.fromGeometry(Point.fromLngLat(yourLocation.getLongitude(), yourLocation.getLatitude()));
                loadedMapStyle.addImage(ICON_ID, BitmapFactory.decodeResource(MainActivity.this.getResources(), R.drawable.mapbox_marker_icon_default));
                loadedMapStyle.addSource(new GeoJsonSource(SOURCE_ID, currLocation));
                loadedMapStyle.addLayer(new SymbolLayer(LAYER_ID, SOURCE_ID)
                        .withProperties(
                                iconImage(ICON_ID),
                                iconAllowOverlap(true),
                                iconIgnorePlacement(true)
                        )
                );
            } catch (NullPointerException e) {
                AlertDialog.Builder startGPS = new AlertDialog.Builder(this);
                startGPS.setTitle("GPS not enabled");
                startGPS.setMessage("In order to use the features of this app, kindly start your GPS");
                startGPS.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Needed to be left empty since nothing is to be done.
                    }
                });
                AlertDialog permsDialog = startGPS.create();
                permsDialog.show();
            }
            // Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);

// Set the component's render mode
            locationComponent.setRenderMode(RenderMode.COMPASS);
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    private void initSearchFab() {
        searchLocation = findViewById(R.id.search_location);
        searchLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new PlaceAutocomplete.IntentBuilder()
                        .accessToken(Mapbox.getAccessToken() != null ? Mapbox.getAccessToken() : getString(R.string.access_token))
                        .placeOptions(PlaceOptions.builder()
                                .backgroundColor(Color.parseColor("#EEEEEE"))
                                .limit(10)
                                // .addInjectedFeature(home) // For Ease of Location Access
                                // .addInjectedFeature(work)
                                .build(PlaceOptions.MODE_CARDS))
                        .build(MainActivity.this);
                startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE);
            }
        });
    }

    /**
     * Not to be worked on right now
     * private void addUserLocations() {
     * home = CarmenFeature.builder().text("Mapbox SF Office")
     * .geometry(Point.fromLngLat(-122.3964485, 37.7912561))
     * .placeName("50 Beale St, San Francisco, CA")
     * .id("mapbox-sf")
     * .properties(new JsonObject())
     * .build();
     * <p>
     * work = CarmenFeature.builder().text("Mapbox DC Office")
     * .placeName("740 15th Street NW, Washington DC")
     * .geometry(Point.fromLngLat(-77.0338348, 38.899750))
     * .id("mapbox-dc")
     * .properties(new JsonObject())
     * .build();
     * }
     */

    private void setUpSource(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addSource(new GeoJsonSource(geojsonSourceLayerId));
    }

    private void setupLayer(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addLayer(new SymbolLayer("SYMBOL_LAYER_ID", geojsonSourceLayerId).withProperties(
                iconImage(ICON_ID),
                iconOffset(new Float[]{0f, -8f})
        ));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_AUTOCOMPLETE) {

// Retrieve selected location's CarmenFeature
            CarmenFeature selectedCarmenFeature = PlaceAutocomplete.getPlace(data);
            destinationLocation = (Point) selectedCarmenFeature.geometry(); //Gives destination Location

//Changing Edit Text to the destination location name
            searchLocation.setText(selectedCarmenFeature.placeName().substring(0, selectedCarmenFeature.placeName().indexOf(',')));
// Create a new FeatureCollection and add a new Feature to it using selectedCarmenFeature above.
// Then retrieve and update the source designated for showing a selected location's symbol layer icon
            if (features.size() != 0)
                features.clear();

            features.add(Feature.fromJson(selectedCarmenFeature.toJson()));

            if (mapboxMap != null) {
                Style style = mapboxMap.getStyle();
                if (style != null) {
                    GeoJsonSource source = style.getSourceAs(geojsonSourceLayerId);
                    if (source != null) {
                        source.setGeoJson(FeatureCollection.fromFeatures(features));
                    }

// Move map camera to the selected location
                    mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(new LatLng(destinationLocation.latitude(),
                                            destinationLocation.longitude()))
                                    .zoom(14)
                                    .build()), 4000);
                    generateRoute();
                }
            }
            navigationButton = findViewById(R.id.navigation_button);
            navigationButton.setEnabled(true);
            navigationButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(MainActivity.this, NavigationActivity.class);
                    startActivity(intent);
                }
            });
            waypointButton = findViewById(R.id.waypoint_button);
            waypointButton.setVisibility(View.VISIBLE);
            waypointButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new PlaceAutocomplete.IntentBuilder()
                            .accessToken(Mapbox.getAccessToken() != null ? Mapbox.getAccessToken() : getString(R.string.access_token))
                            .placeOptions(PlaceOptions.builder()
                                    .backgroundColor(Color.parseColor("#EEEEEE"))
                                    .limit(10)
                                    // .addInjectedFeature(home) // For Ease of Location Access
                                    // .addInjectedFeature(work)
                                    .build(PlaceOptions.MODE_CARDS))
                            .build(MainActivity.this);
                    startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE_WAYPOINT);
                }
            });
        } else if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_AUTOCOMPLETE_WAYPOINT) {
            CarmenFeature selectedCarmenFeature = PlaceAutocomplete.getPlace(data);
            if(!waypoints.contains(selectedCarmenFeature.geometry()))
                waypoints.add((Point) selectedCarmenFeature.geometry());
            features.add(Feature.fromJson(selectedCarmenFeature.toJson()));


            if (mapboxMap != null) {
                Style style = mapboxMap.getStyle();
                if (style != null) {
                    GeoJsonSource source = style.getSourceAs(geojsonSourceLayerId);
                    if (source != null) {
                        source.setGeoJson(FeatureCollection.fromFeatures(features));
                    }

                    mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(new LatLng(destinationLocation.latitude(),
                                            destinationLocation.longitude()))
                                    .zoom(11.5)
                                    .build()), 4000);
                    generateRoute();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        AlertDialog.Builder reasonOfPerms = new AlertDialog.Builder(this);
        reasonOfPerms.setTitle(getString(R.string.user_location_permission_title));
        reasonOfPerms.setMessage(getString(R.string.user_location_permission_explanation));
        reasonOfPerms.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //Needed to be left empty since nothing is to be done.
            }
        });
        AlertDialog permsDialog = reasonOfPerms.create();
        permsDialog.show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            mapboxMap.getStyle(new Style.OnStyleLoaded() {
                @Override
                public void onStyleLoaded(@NonNull Style style) {
                    enableLocationComponent(style);
                }
            });
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            finish();
        }
    }


}