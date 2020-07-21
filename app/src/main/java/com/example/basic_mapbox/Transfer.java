package com.example.basic_mapbox;

import android.location.Location;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;

import java.util.ArrayList;

public class Transfer
{
    private DirectionsRoute currentRoute;
    private Location yourLocation;
    private Point destinationLocation;
    private ArrayList<Point> waypoints = new ArrayList<>();
    private static Transfer transferObj;

    void setCurrentRoute(DirectionsRoute currentRoute) {
        this.currentRoute = currentRoute;
    }
    DirectionsRoute getCurrentRoute() {
        return currentRoute;
    }
    void setYourLocation(Location yourLocation) {
        this.yourLocation = yourLocation;
    }
    Location getYourLocation() {
        return yourLocation;
    }
    void setDestinationLocation(Point destinationLocation) {
    this.destinationLocation = destinationLocation;
    }
    Point getDestinationLocation() {
        return destinationLocation;
    }
    void setWaypoints(ArrayList<Point> waypoints) {
        this.waypoints = waypoints;
    }
    ArrayList<Point> getWaypoints() {
        return waypoints;
    }
    static void setTransfer(Transfer transferObj) {
        Transfer.transferObj = transferObj;
    }
    static Transfer getTransfer() {
        return Transfer.transferObj;
    }
}
