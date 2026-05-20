package com.example.moviapp_rimsakine;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class FavoritesActivity extends AppCompatActivity {

    private static final String TAG          = "FavoritesActivity";
    private static final String TMDB_API_KEY = "5d4a8c0bace78149087b4d6215ef7fe3";
    private static final String IMAGE_BASE   = "https://image.tmdb.org/t/p/w500";

    private RecyclerView      recyclerView;
    private TextView          tvEmpty;
    private MyMovieAdapter    adapter;
    private FirebaseFirestore db;
    private FirebaseUser      user;
    private RequestQueue      queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        db   = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        queue = Volley.newRequestQueue(this);

        recyclerView = findViewById(R.id.recyclerViewFavorites);
        tvEmpty      = findViewById(R.id.tvEmpty);
        TextView btnBack = findViewById(R.id.btnBack);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        btnBack.setOnClickListener(v -> finish());

        if (user == null) {
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText("Connectez-vous pour voir vos favoris.");
            return;
        }

        loadFavorites();
    }

    private void loadFavorites() {
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) { showEmpty(); return; }

                    List<Long> favoriteIds = (List<Long>) doc.get("favoriteMovies");
                    if (favoriteIds == null || favoriteIds.isEmpty()) { showEmpty(); return; }

                    List<MyMovieData> movieList = new ArrayList<>();
                    int[] loaded = {0};

                    for (Long movieId : favoriteIds) {
                        String url = "https://api.themoviedb.org/3/movie/" + movieId
                                + "?api_key=" + TMDB_API_KEY + "&language=fr-FR";

                        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                                response -> {
                                    try {
                                        int    id         = response.optInt("id");
                                        String title      = response.optString("title", "Sans titre");
                                        String date       = response.optString("release_date", "N/A");
                                        String posterPath = response.optString("poster_path", "");

                                        if (!posterPath.isEmpty()) {
                                            movieList.add(new MyMovieData(id, title, date, posterPath));
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error: " + e.getMessage());
                                    }

                                    loaded[0]++;
                                    if (loaded[0] == favoriteIds.size()) {
                                        runOnUiThread(() -> {
                                            if (movieList.isEmpty()) { showEmpty(); return; }
                                            MyMovieData[] movies = movieList.toArray(new MyMovieData[0]);
                                            adapter = new MyMovieAdapter(movies, this, true);
                                            recyclerView.setAdapter(adapter);
                                        });
                                    }
                                },
                                error -> {
                                    loaded[0]++;
                                    Log.e(TAG, "Volley error: " + error.toString());
                                }
                        );
                        queue.add(req);
                    }
                })
                .addOnFailureListener(e -> { showEmpty(); Log.e(TAG, e.getMessage()); });
    }

    private void showEmpty() {
        tvEmpty.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }
}