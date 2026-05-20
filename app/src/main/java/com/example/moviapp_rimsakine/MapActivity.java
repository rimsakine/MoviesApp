package com.example.moviapp_rimsakine;

import android.Manifest;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "MapActivity";
    private static final String PLACES_NEARBY_URL = "https://places.googleapis.com/v1/places:searchNearby";
    private static final double CASABLANCA_LAT = 33.5731;
    private static final double CASABLANCA_LNG = -7.5898;

    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private RequestQueue requestQueue;
    private TextView textMapStatus;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private Location lastCinemaSearchLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        textMapStatus = findViewById(R.id.textMapStatus);
        requestQueue = Volley.newRequestQueue(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean fine = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                    Boolean coarse = result.get(Manifest.permission.ACCESS_COARSE_LOCATION);
                    if (Boolean.TRUE.equals(fine) || Boolean.TRUE.equals(coarse)) {
                        startLocationUpdates();
                    } else {
                        textMapStatus.setText("Permission GPS refusee. Test autour de Casablanca.");
                        useCasablancaFallback();
                    }
                });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fullscreenMap);
        if (mapFragment != null) mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(CASABLANCA_LAT, CASABLANCA_LNG), 11));
        textMapStatus.setText("Carte prete. Recherche de votre position...");
        googleMap.setOnMarkerClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof CinemaPlace) {
                CinemaPlace cinema = (CinemaPlace) tag;
                marker.setSnippet(cinema.getAddress() + " - " + formatDistance(cinema.getDistanceMeters()));
                marker.showInfoWindow();
                return true;
            }
            return false;
        });
        requestLocationPermissionIfNeeded();
    }

    private void requestLocationPermissionIfNeeded() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        googleMap.setMyLocationEnabled(true);
        textMapStatus.setText("GPS active. Recuperation de la position...");

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        handleLocation(location);
                    } else if (lastCinemaSearchLocation == null) {
                        textMapStatus.setText("Position GPS indisponible. Test autour de Casablanca...");
                        useCasablancaFallback();
                    }
                })
                .addOnFailureListener(error -> {
                    Log.e(TAG, "Last location error", error);
                    if (lastCinemaSearchLocation == null) useCasablancaFallback();
                });

        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 6000)
                .setMinUpdateIntervalMillis(3000)
                .build();
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) handleLocation(location);
            }
        };
        fusedLocationClient.requestLocationUpdates(request, locationCallback, getMainLooper());
    }

    private void handleLocation(Location location) {
        LatLng userPosition = new LatLng(location.getLatitude(), location.getLongitude());
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userPosition, 14));
        if (lastCinemaSearchLocation == null || lastCinemaSearchLocation.distanceTo(location) > 500) {
            lastCinemaSearchLocation = location;
            fetchNearbyCinemas(location);
        }
    }

    private void useCasablancaFallback() {
        if (googleMap == null) return;
        Location fallback = new Location("casablanca_fallback");
        fallback.setLatitude(CASABLANCA_LAT);
        fallback.setLongitude(CASABLANCA_LNG);
        lastCinemaSearchLocation = fallback;
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(CASABLANCA_LAT, CASABLANCA_LNG), 12));
        fetchNearbyCinemas(fallback);
    }

    private void fetchNearbyCinemas(Location userLocation) {
        String apiKey = getGoogleMapsApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            textMapStatus.setText("Cle Google Maps manquante dans AndroidManifest.xml");
            return;
        }

        textMapStatus.setText("Recherche des cinemas proches...");
        JSONObject body = buildNearbySearchBody(userLocation);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, PLACES_NEARBY_URL, body,
                response -> renderCinemas(response, userLocation),
                error -> {
                    String message = describeVolleyError(error);
                    Log.e(TAG, "Places request failed: " + message, error);
                    textMapStatus.setText("Erreur reseau Places: " + message);
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("X-Goog-Api-Key", apiKey);
                headers.put("X-Goog-FieldMask",
                        "places.displayName,places.formattedAddress,places.location");
                headers.put("X-Android-Package", getPackageName());
                headers.put("X-Android-Cert", getSigningCertificateSha1());
                return headers;
            }
        };
        request.setRetryPolicy(new DefaultRetryPolicy(
                12000,
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(request);
    }

    private JSONObject buildNearbySearchBody(Location userLocation) {
        JSONObject center = new JSONObject();
        JSONObject circle = new JSONObject();
        JSONObject restriction = new JSONObject();
        JSONObject body = new JSONObject();
        try {
            center.put("latitude", userLocation.getLatitude());
            center.put("longitude", userLocation.getLongitude());
            circle.put("center", center);
            circle.put("radius", 5000.0);
            restriction.put("circle", circle);
            body.put("includedTypes", new JSONArray().put("movie_theater"));
            body.put("maxResultCount", 20);
            body.put("locationRestriction", restriction);
        } catch (Exception e) {
            Log.e(TAG, "Places body error", e);
        }
        return body;
    }

    private void renderCinemas(JSONObject response, Location userLocation) {
        googleMap.clear();
        JSONArray places = response.optJSONArray("places");
        int count = places == null ? 0 : places.length();
        if (count == 0) {
            textMapStatus.setText("Aucun cinema trouve dans un rayon de 5 km.");
            return;
        }

        int added = 0;
        for (int i = 0; i < count; i++) {
            JSONObject item = places.optJSONObject(i);
            if (item == null) continue;
            JSONObject placeLocation = item.optJSONObject("location");
            if (placeLocation == null) continue;
            LatLng position = new LatLng(
                    placeLocation.optDouble("latitude"),
                    placeLocation.optDouble("longitude"));
            float[] distance = new float[1];
            Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(),
                    position.latitude, position.longitude, distance);
            JSONObject displayName = item.optJSONObject("displayName");
            CinemaPlace cinema = new CinemaPlace(
                    displayName == null ? "Cinema" : displayName.optString("text", "Cinema"),
                    item.optString("formattedAddress", "Adresse non disponible"),
                    position,
                    distance[0]);
            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(cinema.getName())
                    .snippet(cinema.getAddress() + " - " + formatDistance(cinema.getDistanceMeters()))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            if (marker != null) marker.setTag(cinema);
            added++;
        }
        textMapStatus.setText(added + " cinema(s) trouve(s) dans un rayon de 5 km");
    }

    private String getGoogleMapsApiKey() {
        try {
            Bundle metaData = getPackageManager().getApplicationInfo(getPackageName(),
                    PackageManager.GET_META_DATA).metaData;
            return metaData.getString("com.google.android.geo.API_KEY", "");
        } catch (Exception e) {
            return "";
        }
    }

    private String formatDistance(float meters) {
        if (meters >= 1000f) return String.format("%.1f km", meters / 1000f);
        return Math.round(meters) + " m";
    }

    private String describeVolleyError(VolleyError error) {
        if (error == null) return "erreur inconnue";
        if (error.networkResponse != null) {
            String body = "";
            if (error.networkResponse.data != null) {
                body = new String(error.networkResponse.data, StandardCharsets.UTF_8);
            }
            return "HTTP " + error.networkResponse.statusCode + (body.isEmpty() ? "" : " - " + body);
        }
        return error.getMessage() == null ? error.toString() : error.getMessage();
    }

    private String getSigningCertificateSha1() {
        try {
            Signature[] signatures;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageInfo packageInfo = getPackageManager().getPackageInfo(
                        getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
                signatures = packageInfo.signingInfo.getApkContentsSigners();
            } else {
                PackageInfo packageInfo = getPackageManager().getPackageInfo(
                        getPackageName(), PackageManager.GET_SIGNATURES);
                signatures = packageInfo.signatures;
            }
            if (signatures == null || signatures.length == 0) return "";

            MessageDigest digest = MessageDigest.getInstance("SHA1");
            byte[] sha1 = digest.digest(signatures[0].toByteArray());
            StringBuilder result = new StringBuilder();
            for (byte b : sha1) {
                result.append(String.format("%02X", b));
            }
            return result.toString();
        } catch (Exception e) {
            Log.e(TAG, "Unable to read signing certificate", e);
            return "";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}
