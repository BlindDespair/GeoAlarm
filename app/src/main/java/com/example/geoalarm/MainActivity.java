package com.example.geoalarm;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends FragmentActivity {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_map);
        setUpMapIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.setMyLocationEnabled(true);
        final MarkerOptions theMarker = new MarkerOptions();
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    showAlertDialog();
                }
                return false;
            }
        });
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                mMap.clear();
                mMap.addMarker(theMarker
                        .position(new LatLng(latLng.latitude, latLng.longitude))
                        .title("")
                        .draggable(false));
            }
        });
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    if (mMap.getMyLocation() != null) {
                        float[] results = new float[1];
                        Location.distanceBetween(mMap.getMyLocation().getLatitude(), mMap.getMyLocation().getLongitude(),
                                marker.getPosition().latitude, marker.getPosition().longitude, results);
                        if (results[0] > 1000) {
                            results[0] = results[0] / 1000;
                            marker.setTitle("Расстояние: " + Float.toString(Math.round(results[0])) + "км");
                        } else {
                            marker.setTitle("Расстояние: " + Float.toString(Math.round(results[0])) + "м");
                        }
                    } else {
                        marker.setTitle("Нет данных о геолокации. Невозможно рассчитать расстояние.");
                    }
                } else {
                    showAlertDialog();
                }
                return false;
            }
        });
    }


    protected void showAlertDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("ИДИ НАХУЙ!")
                .setMessage("Включи службу сука!")
                .setCancelable(false)
                .setNegativeButton("Я ЕБАЛ",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        })
                .setPositiveButton("OKAY",
                        new DialogInterface.OnClickListener(){
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(new Intent(
                                                Settings.ACTION_LOCATION_SOURCE_SETTINGS
                                        )
                                );
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
    }
}
