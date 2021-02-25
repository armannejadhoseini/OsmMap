package com.example.myapplication.ui.main;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.myapplication.R;

import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.Objects;

public class MainFragment extends Fragment {

    private MainViewModel homeViewModel;
    private MapController mapController;
    private MapView map;
    private MyLocationNewOverlay mLocationOverlay;

    public static MainFragment newInstance() {
        return new MainFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.main_fragment, container, false);

        map = root.findViewById(R.id.mapview);
        return root;

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Configuration.getInstance().load(getActivity(), PreferenceManager.getDefaultSharedPreferences(getContext()));
        homeViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        checkInternetPermission();
        checkCoarseLocationPermission();
        checkFineLocationPermission();

        map.setTileSource(TileSourceFactory.MAPNIK);
        mapController = (MapController) map.getController();

        homeViewModel.getMap().setValue(map);
        homeViewModel.getMap().observe(getViewLifecycleOwner(), new Observer<MapView>() {
            @Override
            public void onChanged(MapView mapView) {
                map.setMultiTouchControls(true);
                getmyLocation();
                mapController.setCenter(homeViewModel.getDefaultLocation().getValue());
                mapController.setZoom(10);
                getmCompassOverlay();
                maker();
                if (Objects.equals(homeViewModel.getFlag().getValue(), 1)) {
                    resume();
                }

            }
        });


    }

    public void getmCompassOverlay() {
        CompassOverlay mCompassOverlay = new CompassOverlay(requireContext(), new InternalCompassOrientationProvider(requireContext()), map);
        mCompassOverlay.enableCompass();
        map.getOverlays().add(mCompassOverlay);
    }

    public void getmyLocation() {
        GpsMyLocationProvider provider1 = new GpsMyLocationProvider(requireContext());
        provider1.addLocationSource(LocationManager.NETWORK_PROVIDER);
        mLocationOverlay = new MyLocationNewOverlay(provider1, map);
        mLocationOverlay.enableMyLocation();
        map.getOverlays().add(mLocationOverlay);

    }

    public void done() {

        mapController.setZoom(15);
        mapController.setCenter(homeViewModel.getDest().getValue());
        if (Objects.equals(homeViewModel.getFlag().getValue(), 0)) {
            double destination = Objects.requireNonNull(homeViewModel.getSrc().getValue()).distanceToAsDouble(homeViewModel.getDest().getValue());
            destination = Math.round(destination * 10) / 10.0;
            if (destination > 1000) {
                Toast.makeText(getContext(), "Your Total Destination is : " + destination + " KiloMeters ", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getContext(), "Your Total Destination is : " + destination + " Meters ", Toast.LENGTH_LONG).show();
            }
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                RoadManager roadManager = new OSRMRoadManager(getContext());
                ArrayList<GeoPoint> waypoints = new ArrayList<>();
                waypoints.add(homeViewModel.getSrc().getValue());
                waypoints.add(homeViewModel.getDest().getValue());
                Road road = roadManager.getRoad(waypoints);
                Polyline roadOverlay = RoadManager.buildRoadOverlay(road);
                map.getOverlays().add(roadOverlay);
            }
        }).start();


    }

    public void maker() {
        getmyLocation();
        getmCompassOverlay();
        MapEventsReceiver mReceive = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                if (Objects.equals(homeViewModel.getFlag().getValue(), 0)) {
                    homeViewModel.getSrc().setValue(mLocationOverlay.getMyLocation());
                    homeViewModel.getDest().setValue(p);
                    map.getOverlays().clear();
                    maker();
                    Marker startMarker = new Marker(map);
                    startMarker.setPosition(p);
                    startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    map.getOverlays().add(startMarker);
                    done();
                    homeViewModel.getFlag().setValue(1);
                } else {
                    Toast.makeText(getContext(), "Please Clear The Route First", Toast.LENGTH_SHORT).show();
                }

                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                map.getOverlays().clear();
                maker();
                homeViewModel.getFlag().setValue(0);
                return false;
            }
        };

        MapEventsOverlay OverlayEvents = new MapEventsOverlay(mReceive);
        map.getOverlays().add(OverlayEvents);
    }

    public void resume() {
        map.getOverlays().clear();
        Marker startMarker = new Marker(map);
        startMarker.setPosition(homeViewModel.getDest().getValue());
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(startMarker);
        done();
    }

    private void checkFineLocationPermission() {

        if (ContextCompat.checkSelfPermission(
                Objects.requireNonNull(getContext()), Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {

            Log.d("permission : ", "granted");
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(Objects.requireNonNull(getActivity()),
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            new AlertDialog.Builder(getContext())
                    .setTitle("Permission Required ")
                    .setMessage("You Cant Use Apps Full Functionality Until You Allow The Required Permission ")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ActivityCompat.requestPermissions(Objects.requireNonNull(getActivity()), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                        }
                    })
                    .create()
                    .show();

        } else {

            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    private void checkCoarseLocationPermission() {

        if (ContextCompat.checkSelfPermission(
                Objects.requireNonNull(getContext()), Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {

            Log.d("permission : ", "granted");
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(Objects.requireNonNull(getActivity()),
                Manifest.permission.ACCESS_COARSE_LOCATION)) {
            new AlertDialog.Builder(getContext())
                    .setTitle("Permission Required ")
                    .setMessage("You Cant Use Apps Full Functionality Until You Allow The Required Permission ")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ActivityCompat.requestPermissions(Objects.requireNonNull(getActivity()), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 2);
                        }
                    })
                    .create()
                    .show();

        } else {

            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 2);
        }
    }

    private void checkInternetPermission() {

        if (ContextCompat.checkSelfPermission(
                Objects.requireNonNull(getContext()), Manifest.permission.INTERNET) ==
                PackageManager.PERMISSION_GRANTED) {
            Log.d("permission : ", "granted");
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(Objects.requireNonNull(getActivity()),
                Manifest.permission.INTERNET)) {
            new AlertDialog.Builder(getContext())
                    .setTitle("Permission Required ")
                    .setMessage("You Cant Use Apps Full Functionality Until You Allow The Required Permission ")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ActivityCompat.requestPermissions(Objects.requireNonNull(getActivity()), new String[]{Manifest.permission.INTERNET}, 3);
                        }
                    })
                    .create()
                    .show();

        } else {

            requestPermissions(new String[]{Manifest.permission.INTERNET}, 3);
        }

    }


}


