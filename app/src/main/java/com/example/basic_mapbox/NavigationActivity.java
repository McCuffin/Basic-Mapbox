package com.example.basic_mapbox;

import android.content.DialogInterface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.services.android.navigation.ui.v5.NavigationView;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.ui.v5.OnNavigationReadyCallback;
import com.mapbox.services.android.navigation.ui.v5.listeners.BannerInstructionsListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.InstructionListListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.RouteListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.SpeechAnnouncementListener;
import com.mapbox.services.android.navigation.ui.v5.voice.SpeechAnnouncement;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener;
import com.mapbox.services.android.navigation.v5.route.FasterRouteListener;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NavigationActivity extends AppCompatActivity implements OnNavigationReadyCallback,
        NavigationListener, ProgressChangeListener, InstructionListListener, SpeechAnnouncementListener,
        BannerInstructionsListener, RouteListener, OffRouteListener, FasterRouteListener {

    private static final int INITIAL_ZOOM = 16;
    private final String TAG = "NavigationActivity";

    private NavigationView navigationView;
    private View spacer;
    private TextView speedWidget;
    private DirectionsRoute newCurrentRoute;

    private boolean bottomSheetVisible = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.Theme_AppCompat_Light_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        navigationView = findViewById(R.id.navigationView);
        speedWidget = findViewById(R.id.speed_limit);
        spacer = findViewById(R.id.spacer);
        setSpeedWidgetAnchor(R.id.summaryBottomSheet);

        CameraPosition initialPosition = new CameraPosition.Builder()
                .target(new LatLng(MainActivity.yourLocation.getLatitude(),MainActivity.yourLocation.getLongitude()))
                .zoom(INITIAL_ZOOM)
                .build();
        navigationView.onCreate(savedInstanceState);
        navigationView.initialize(this, initialPosition);
    }

    @Override
    public void onNavigationReady(boolean isRunning) {
        startNavigation(MainActivity.currentRoute);
    }

    @Override
    public void onStart() {
        super.onStart();
        navigationView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        navigationView.onResume();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        navigationView.onLowMemory();
    }

    @Override
    public void onBackPressed() {
        // If the navigation view didn't need to do anything, call super
        if (!navigationView.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        navigationView.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        navigationView.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
        navigationView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        navigationView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        navigationView.onDestroy();
        if (isFinishing()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
        }
    }

    @Override
    public void onCancelNavigation() {
        // Navigation canceled, finish the activity
        finish();
    }

    @Override
    public void onNavigationFinished() {
        // Intentionally empty
    }

    @Override
    public void onNavigationRunning() {
        // Intentionally empty
    }

    @Override
    public void onProgressChange(Location location, RouteProgress routeProgress) {
        //empty
    }

    @Override
    public void onInstructionListVisibilityChanged(boolean shown) {
        speedWidget.setVisibility(shown ? View.GONE : View.VISIBLE);
    }

    @Override
    public BannerInstructions willDisplay(BannerInstructions instructions) {
        return instructions;
    }

    private void startNavigation(DirectionsRoute directionsRoute) {
        NavigationViewOptions.Builder options =
                NavigationViewOptions.builder()
                        .navigationListener(this)
                        .directionsRoute(directionsRoute)
                        .shouldSimulateRoute(false)
                        .progressChangeListener(this)
                        .instructionListListener(this)
                        .speechAnnouncementListener(this)
                        .bannerInstructionsListener(this);

        setBottomSheetCallback(options);
        navigationView.startNavigation(options.build());
    }

    /**
     * Sets the anchor of the spacer for the speed widget, thus setting the anchor for the speed widget
     * (The speed widget is anchored to the spacer, which is there because padding between items and
     * their anchors in CoordinatorLayouts is finicky.
     *
     * @param res resource for view of which to anchor the spacer
     */
    private void setSpeedWidgetAnchor(@IdRes int res) {
        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) spacer.getLayoutParams();
        layoutParams.setAnchorId(res);
        spacer.setLayoutParams(layoutParams);
    }

    private void setBottomSheetCallback(NavigationViewOptions.Builder options) {
        options.bottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_HIDDEN:
                        bottomSheetVisible = false;
                        setSpeedWidgetAnchor(R.id.recenterBtn);
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:
                        bottomSheetVisible = true;
                        break;
                    case BottomSheetBehavior.STATE_SETTLING:
                        if (!bottomSheetVisible) {
                            // View needs to be anchored to the bottom sheet before it is finished expanding
                            // because of the animation
                            setSpeedWidgetAnchor(R.id.summaryBottomSheet);
                        }
                        break;
                    default:
                        return;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });
    }

    @Override
    public SpeechAnnouncement willVoice(SpeechAnnouncement announcement) {
        return announcement.toBuilder()
                .announcement("All Instructions are same")
                .build();
    }

    @Override
    public boolean allowRerouteFrom(Point offRoutePoint) {
        return true;
    }

    @Override
    public void onOffRoute(Point offRoutePoint) {

    }

    @Override
    public void onRerouteAlong(DirectionsRoute directionsRoute) {

    }

    @Override
    public void onFailedReroute(String errorMessage) {
        new AlertDialog.Builder(this)
                .setTitle("Unable to find route")
                .setMessage(errorMessage)
                .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Intentionally left empty since nothing is to be done.
                    }
                })
                .create()
                .show();
    }

    @Override
    public void onArrival() {
        if (!MainActivity.waypoints.isEmpty()) {
            Point currWaypoint = MainActivity.waypoints.remove(0);
            Geocoder converter = new Geocoder(this);
            String location = "";
            try {
                List<Address> address = converter.getFromLocation(currWaypoint.latitude(), currWaypoint.longitude(), 1);
                if (address != null && address.size() > 0) {
                    location = address.get(0).getAddressLine(0);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            Toast.makeText(this, "You have arrived at " + location, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "You have reached your destination", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void userOffRoute(Location location) {
        NavigationRoute.Builder routeBuilder = NavigationRoute.builder(this)
                .accessToken(getString(R.string.access_token))
                .profile(DirectionsCriteria.PROFILE_DRIVING)
                .origin(Point.fromLngLat(location.getLongitude(), location.getLatitude()))
                .destination(MainActivity.destinationLocation);

        for(Point p: MainActivity.waypoints) {
            routeBuilder.addWaypoint(p);
        }

        NavigationRoute newRoute = routeBuilder.build();
        newRoute.getRoute(new Callback<DirectionsResponse>() {
            @Override
            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                Log.d(TAG, "onResponse: "+ response);
                Log.d(TAG, "Response code: "+ response.code());
                if (response.body() == null) {
                    Log.e(TAG, "No routes found, make sure you set the right user and access token.");
                    return;
                } else if (response.body().routes().size() < 1) {
                    Log.e(TAG,"No routes found");
                    return;
                }

                newCurrentRoute = response.body().routes().get(0);
                startNavigation(newCurrentRoute);
            }

            @Override
            public void onFailure(Call<DirectionsResponse> call, Throwable t) {

            }
        });
    }

    @Override
    public void fasterRouteFound(DirectionsRoute directionsRoute) {
        startNavigation(directionsRoute);
    }
}
// TODO : Have to test OffRouteListener and RouteListener when going out