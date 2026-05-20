package com.example.moviapp_rimsakine;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class MovieDetailActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "MovieDetailActivity";
    private static final String TMDB_API_KEY = "5d4a8c0bace78149087b4d6215ef7fe3";
    private static final String TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500";

    private TextView descriptionTextView;
    private TextView nameTextView;
    private TextView textRating;
    private TextView textYear;
    private ImageView posterImageView;
    private LinearLayout genreChipsDetail;
    private RecyclerView recyclerViewCast;
    private RecyclerView recyclerViewRecommended;
    private Button btnFavorite;
    private String trailerKey;
    private RequestQueue requestQueue;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private int currentMovieId;
    private boolean isFavorite = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dark_bg));
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        requestQueue = Volley.newRequestQueue(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean fine = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                    Boolean coarse = result.get(Manifest.permission.ACCESS_COARSE_LOCATION);
                    if (Boolean.TRUE.equals(fine) || Boolean.TRUE.equals(coarse)) {
                        loadNearbyCinemasOnDetailMap();
                    } else {
                        Toast.makeText(this, "Permission GPS refusee", Toast.LENGTH_SHORT).show();
                    }
                });

        descriptionTextView = findViewById(R.id.Details);
        posterImageView = findViewById(R.id.imageview);
        nameTextView = findViewById(R.id.textName);
        textRating = findViewById(R.id.textRating);
        textYear = findViewById(R.id.textYear);
        genreChipsDetail = findViewById(R.id.genreChipsDetail);
        recyclerViewCast = findViewById(R.id.recyclerViewCast);
        recyclerViewRecommended = findViewById(R.id.recyclerViewRecommended);
        btnFavorite = findViewById(R.id.btnFavorite);
        Button playButton = findViewById(R.id.playButton);

        recyclerViewCast.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerViewRecommended.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        currentMovieId = getIntent().getIntExtra("movieId", -1);
        if (currentMovieId != -1) {
            fetchMovieDetails(currentMovieId);
            fetchCast(currentMovieId);
            fetchTrailer(currentMovieId);
            checkIfFavorite(currentMovieId);
        }

        playButton.setOnClickListener(v -> playTrailer());
        btnFavorite.setOnClickListener(v -> toggleFavorite());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);
    }

    private void fetchMovieDetails(int movieId) {
        String url = "https://api.themoviedb.org/3/movie/" + movieId + "?api_key=" + TMDB_API_KEY + "&language=fr-FR";
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        nameTextView.setText(response.optString("title"));
                        descriptionTextView.setText(response.optString("overview"));
                        textRating.setText(String.format("Note %.1f", response.optDouble("vote_average")));
                        String date = response.optString("release_date");
                        textYear.setText(" - " + (date.length() >= 4 ? date.substring(0, 4) : "----"));
                        Glide.with(this).load(TMDB_IMAGE_BASE_URL + response.optString("poster_path")).into(posterImageView);

                        JSONArray genres = response.optJSONArray("genres");
                        if (genres != null) {
                            buildGenreChips(genres);
                            if (genres.length() > 0) fetchRecommendationsByGenre(genres.getJSONObject(0).getInt("id"), movieId);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Movie detail error", e);
                    }
                }, error -> Log.e(TAG, error.toString()));
        requestQueue.add(req);
    }

    private void buildGenreChips(JSONArray genres) {
        genreChipsDetail.removeAllViews();
        float dp = getResources().getDisplayMetrics().density;
        for (int i = 0; i < genres.length(); i++) {
            try {
                String name = genres.getJSONObject(i).getString("name");
                TextView chip = new TextView(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, (int) (28 * dp));
                params.setMarginEnd((int) (8 * dp));
                chip.setLayoutParams(params);
                chip.setText(name);
                chip.setTextColor(Color.WHITE);
                chip.setBackgroundResource(R.drawable.bg_card);
                chip.setPadding((int) (12 * dp), 0, (int) (12 * dp), 0);
                chip.setGravity(Gravity.CENTER);
                genreChipsDetail.addView(chip);
            } catch (Exception e) {
                Log.e(TAG, "Genre chip error", e);
            }
        }
    }

    private void fetchRecommendationsByGenre(int genreId, int excludeId) {
        String url = "https://api.themoviedb.org/3/discover/movie?api_key=" + TMDB_API_KEY + "&with_genres=" + genreId;
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        List<MyMovieData> list = new ArrayList<>();
                        for (int i = 0; i < Math.min(results.length(), 10); i++) {
                            JSONObject obj = results.getJSONObject(i);
                            if (obj.getInt("id") == excludeId) continue;
                            list.add(new MyMovieData(obj.getInt("id"),
                                    obj.optString("title"),
                                    "",
                                    obj.optString("poster_path")));
                        }
                        recyclerViewRecommended.setAdapter(new MyMovieAdapter(list.toArray(new MyMovieData[0]), this, false));
                    } catch (Exception e) {
                        Log.e(TAG, "Recommendations error", e);
                    }
                }, error -> {});
        requestQueue.add(req);
    }

    private void fetchCast(int movieId) {
        String url = "https://api.themoviedb.org/3/movie/" + movieId + "/credits?api_key=" + TMDB_API_KEY;
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray castArray = response.getJSONArray("cast");
                        List<CastMember> list = new ArrayList<>();
                        for (int i = 0; i < Math.min(castArray.length(), 10); i++) {
                            JSONObject actor = castArray.getJSONObject(i);
                            list.add(new CastMember(actor.getString("name"),
                                    actor.getString("character"),
                                    actor.optString("profile_path")));
                        }
                        recyclerViewCast.setAdapter(new CastAdapter(list, this));
                    } catch (Exception e) {
                        Log.e(TAG, "Cast error", e);
                    }
                }, error -> {});
        requestQueue.add(req);
    }

    private void fetchTrailer(int movieId) {
        String url = "https://api.themoviedb.org/3/movie/" + movieId + "/videos?api_key=" + TMDB_API_KEY;
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray res = response.getJSONArray("results");
                        for (int i = 0; i < res.length(); i++) {
                            JSONObject video = res.getJSONObject(i);
                            if ("Trailer".equals(video.getString("type"))) {
                                trailerKey = video.getString("key");
                                break;
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Trailer error", e);
                    }
                }, error -> {});
        requestQueue.add(req);
    }

    private void playTrailer() {
        if (trailerKey != null) {
            Intent intent = new Intent(this, VideoPlayer.class);
            intent.putExtra("videoUrl", "https://www.youtube.com/watch?v=" + trailerKey);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Bande-annonce non disponible", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkIfFavorite(int movieId) {
        if (currentUser == null) return;
        db.collection("users").document(currentUser.getUid()).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                List<Long> favs = (List<Long>) doc.get("favoriteMovies");
                isFavorite = favs != null && favs.contains((long) movieId);
                updateFavoriteButton();
            }
        });
    }

    private void toggleFavorite() {
        if (currentUser == null) {
            Toast.makeText(this, "Connectez-vous pour ajouter aux favoris", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isFavorite) {
            db.collection("users").document(currentUser.getUid()).update("favoriteMovies", FieldValue.arrayRemove((long) currentMovieId));
            isFavorite = false;
        } else {
            db.collection("users").document(currentUser.getUid()).update("favoriteMovies", FieldValue.arrayUnion((long) currentMovieId));
            isFavorite = true;
        }
        updateFavoriteButton();
    }

    private void updateFavoriteButton() {
        btnFavorite.setText(isFavorite ? "Favoris" : "Ajouter aux favoris");
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof CinemaPlace) {
                CinemaPlace cinema = (CinemaPlace) tag;
                marker.setSnippet(cinema.getAddress() + " - " + formatDistance(cinema.getDistanceMeters()));
                marker.showInfoWindow();
                return true;
            }
            return false;
        });
        requestDetailLocationPermissionIfNeeded();
    }

    private void requestDetailLocationPermissionIfNeeded() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            loadNearbyCinemasOnDetailMap();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void loadNearbyCinemasOnDetailMap() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;
        mMap.setMyLocationEnabled(true);
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location == null) {
                Toast.makeText(this, "Position actuelle indisponible", Toast.LENGTH_SHORT).show();
                return;
            }
            LatLng userPosition = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userPosition, 13));
            fetchNearbyCinemas(location);
        });
    }

    private void fetchNearbyCinemas(Location userLocation) {
        String url = new Uri.Builder()
                .scheme("https")
                .authority("maps.googleapis.com")
                .appendPath("maps")
                .appendPath("api")
                .appendPath("place")
                .appendPath("nearbysearch")
                .appendPath("json")
                .appendQueryParameter("location", userLocation.getLatitude() + "," + userLocation.getLongitude())
                .appendQueryParameter("radius", "5000")
                .appendQueryParameter("type", "movie_theater")
                .appendQueryParameter("key", getGoogleMapsApiKey())
                .build()
                .toString();
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> renderNearbyCinemas(response, userLocation),
                error -> Toast.makeText(this, "Cinémas proches indisponibles", Toast.LENGTH_SHORT).show());
        requestQueue.add(request);
    }

    private void renderNearbyCinemas(JSONObject response, Location userLocation) {
        mMap.clear();
        JSONArray results = response.optJSONArray("results");
        int count = results == null ? 0 : results.length();
        for (int i = 0; i < count; i++) {
            JSONObject item = results.optJSONObject(i);
            if (item == null) continue;
            JSONObject geometry = item.optJSONObject("geometry");
            JSONObject placeLocation = geometry == null ? null : geometry.optJSONObject("location");
            if (placeLocation == null) continue;

            LatLng position = new LatLng(placeLocation.optDouble("lat"), placeLocation.optDouble("lng"));
            float[] distance = new float[1];
            Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(),
                    position.latitude, position.longitude, distance);
            CinemaPlace cinema = new CinemaPlace(
                    item.optString("name", "Cinéma"),
                    item.optString("vicinity", "Adresse non disponible"),
                    position,
                    distance[0]);
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(cinema.getName())
                    .snippet(cinema.getAddress() + " - " + formatDistance(cinema.getDistanceMeters()))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            if (marker != null) marker.setTag(cinema);
        }
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
}
