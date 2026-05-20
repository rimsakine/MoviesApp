package com.example.moviapp_rimsakine;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.ImageButton;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TMDB_API_KEY = "5d4a8c0bace78149087b4d6215ef7fe3";
    private static final String TAG          = "MainActivity";
    private static final int    MAX_PAGES    = 5;

    private static final Map<Integer, String> GENRES = new LinkedHashMap<>();
    static {
        GENRES.put(0,    "🎬 Tous");
        GENRES.put(28,   "💥 Action");
        GENRES.put(35,   "😂 Comédie");
        GENRES.put(18,   "🎭 Drame");
        GENRES.put(10749,"❤️ Romance");
        GENRES.put(53,   "😱 Thriller");
        GENRES.put(99,   "🎥 Documentaire");
        GENRES.put(10402,"🎵 Musique");
        GENRES.put(10751,"👨‍👩‍👧 Famille");
    }

    private RecyclerView   recyclerView;
    private MyMovieAdapter myMovieAdapter;
    private EditText       searchEditText;
    private TextView       sectionTitle, tvRecommendBanner, btnAuthNav;
    private LinearLayout   genreChipsLayout;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<String> audioPermissionLauncher;
    private SpeechRecognizer speechRecognizer;

    private final List<MyMovieData> allMovies = new ArrayList<>();
    private int     currentPage   = 1;
    private int     totalPages    = 1;
    private boolean isLoading     = false;
    private int     selectedGenre = 0;

    private FirebaseAuth      auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        searchEditText     = findViewById(R.id.editTextSearch);
        recyclerView       = findViewById(R.id.recyclerView);
        sectionTitle       = findViewById(R.id.sectionTitle);
        genreChipsLayout   = findViewById(R.id.genreChipsLayout);
        tvRecommendBanner  = findViewById(R.id.tvRecommendBanner);
        btnAuthNav         = findViewById(R.id.btnAuthNav);
        TextView btnFavNav = findViewById(R.id.btnFavoritesNav);
        TextView btnNearbyCinemas = findViewById(R.id.btnNearbyCinemas);
        TextView btnProfileNav = findViewById(R.id.btnProfileNav);
        ImageButton btnVoiceSearch = findViewById(R.id.buttonVoiceSearch);
        ImageButton btnCameraSearch = findViewById(R.id.buttonCameraSearch);
        FloatingActionButton fabChatbot = findViewById(R.id.fabChatbot);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // Bouton favoris
        btnFavNav.setOnClickListener(v -> {
            if (auth.getCurrentUser() == null) {
                Toast.makeText(this, "Connectez-vous pour voir vos favoris", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
            } else {
                startActivity(new Intent(this, FavoritesActivity.class));
            }
        });

        btnNearbyCinemas.setOnClickListener(v -> startActivity(new Intent(this, MapActivity.class)));
        btnProfileNav.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        fabChatbot.setOnClickListener(v -> startActivity(new Intent(this, ChatBotActivity.class)));

        // Bouton auth (login/logout)
        updateAuthButton();
        btnAuthNav.setOnClickListener(v -> {
            if (auth.getCurrentUser() != null) {
                auth.signOut();
                updateAuthButton();
                tvRecommendBanner.setVisibility(android.view.View.GONE);
                Toast.makeText(this, "Déconnecté", Toast.LENGTH_SHORT).show();
            } else {
                startActivity(new Intent(this, LoginActivity.class));
            }
        });

        // Genre depuis MovieDetailActivity
        int intentGenreId = getIntent().getIntExtra("genreId", 0);
        if (intentGenreId != 0) selectedGenre = intentGenreId;

        buildGenreChips();
        loadPersonalizedRecommendations();
        fetchMoviesPage(1);

        // Pagination
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy <= 0 || isLoading) return;
                int visible  = layoutManager.getChildCount();
                int total    = layoutManager.getItemCount();
                int firstVis = layoutManager.findFirstVisibleItemPosition();
                if ((visible + firstVis) >= total - 3
                        && currentPage < totalPages
                        && currentPage < MAX_PAGES) {
                    fetchMoviesPage(currentPage + 1);
                }
            }
        });

        setupSearchListener();
        setupVoiceAndCameraSearch(btnVoiceSearch, btnCameraSearch);
    }

    private void setupVoiceAndCameraSearch(ImageButton btnVoiceSearch, ImageButton btnCameraSearch) {
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (Boolean.TRUE.equals(isGranted)) {
                        startActivity(new Intent(this, ProfileActivity.class));
                    } else {
                        Toast.makeText(this, "Permission camera refusee", Toast.LENGTH_SHORT).show();
                    }
                });

        audioPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (Boolean.TRUE.equals(isGranted)) {
                        startSpeechRecognizer();
                    } else {
                        Toast.makeText(this, "Permission micro refusee", Toast.LENGTH_SHORT).show();
                    }
                });

        setupSpeechRecognizer();

        btnVoiceSearch.setOnClickListener(v -> startVoiceSearch());
        btnCameraSearch.setOnClickListener(v -> startCameraSearch());
    }

    private void setupSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        } else if (isPackageInstalled("com.google.android.googlequicksearchbox")) {
            ComponentName googleRecognizer = new ComponentName(
                    "com.google.android.googlequicksearchbox",
                    "com.google.android.voicesearch.serviceapi.GoogleRecognitionService");
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this, googleRecognizer);
        } else {
            return;
        }
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {
                Toast.makeText(MainActivity.this, "Parlez maintenant", Toast.LENGTH_SHORT).show();
            }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onError(int error) {
                Toast.makeText(MainActivity.this, getSpeechErrorMessage(error), Toast.LENGTH_LONG).show();
            }
            @Override public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    applySearchQuery(matches.get(0));
                } else {
                    Toast.makeText(MainActivity.this, "Aucun texte detecte", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void startVoiceSearch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            startSpeechRecognizer();
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void startSpeechRecognizer() {
        if (speechRecognizer == null) setupSpeechRecognizer();
        if (speechRecognizer == null) {
            Toast.makeText(this, "Activez l'application Google et la permission Microphone", Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.FRENCH.toLanguageTag());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.FRENCH.toLanguageTag());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        speechRecognizer.cancel();
        speechRecognizer.startListening(intent);
    }

    private boolean isPackageInstalled(String packageName) {
        try {
            getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private String getSpeechErrorMessage(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "Erreur audio du micro";
            case SpeechRecognizer.ERROR_CLIENT:
                return "Erreur du service vocal";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Permission micro manquante";
            case SpeechRecognizer.ERROR_NETWORK:
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Connexion necessaire pour la reconnaissance vocale";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "Je n'ai pas compris, reessayez";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Micro occupe, reessayez";
            case SpeechRecognizer.ERROR_SERVER:
                return "Service vocal indisponible";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "Aucune voix detectee";
            default:
                return "Erreur micro: " + error;
        }
    }

    private void startCameraSearch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startActivity(new Intent(this, ProfileActivity.class));
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void applySearchQuery(String query) {
        String cleanQuery = query == null ? "" : query.trim();
        if (cleanQuery.isEmpty()) return;
        searchEditText.setText(cleanQuery);
        searchEditText.setSelection(cleanQuery.length());
        if (myMovieAdapter != null) myMovieAdapter.getFilter().filter(cleanQuery);
    }

    private void updateAuthButton() {
        FirebaseUser user = auth.getCurrentUser();
        btnAuthNav.setText(user != null ? "🚪" : "👤");
        btnAuthNav.setContentDescription(user != null ? "Déconnexion" : "Connexion");
    }

    // ── Recommandation personnalisée basée sur les genres favoris ────────────
    private void loadPersonalizedRecommendations() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    List<String> favoriteGenres = (List<String>) doc.get("favoriteGenres");
                    if (favoriteGenres == null || favoriteGenres.isEmpty()) return;

                    // Trouver le genre le plus consulté
                    String topGenre = favoriteGenres.get(favoriteGenres.size() - 1);
                    tvRecommendBanner.setVisibility(android.view.View.VISIBLE);
                    tvRecommendBanner.setText("✨ Recommandé pour vous · Genre : " + topGenre);

                    // Trouver l'id du genre
                    for (Map.Entry<Integer, String> entry : GENRES.entrySet()) {
                        if (entry.getValue().contains(topGenre)) {
                            selectedGenre = entry.getKey();
                            sectionTitle.setText("✨ " + topGenre + " recommandés");
                            buildGenreChips();
                            allMovies.clear();
                            currentPage = 1;
                            myMovieAdapter = null;
                            recyclerView.setAdapter(null);
                            fetchMoviesPage(1);
                            break;
                        }
                    }
                });
    }

    // ── Chips genres ──────────────────────────────────────────────────────────
    private void buildGenreChips() {
        genreChipsLayout.removeAllViews();
        float dp = getResources().getDisplayMetrics().density;

        for (Map.Entry<Integer, String> entry : GENRES.entrySet()) {
            int    genreId    = entry.getKey();
            String genreLabel = entry.getValue();

            TextView chip = new TextView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, (int)(36 * dp));
            params.setMarginEnd((int)(8 * dp));
            chip.setLayoutParams(params);
            chip.setText(genreLabel);
            chip.setTextSize(12);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding((int)(14*dp), 0, (int)(14*dp), 0);
            chip.setTypeface(null, genreId == selectedGenre ? Typeface.BOLD : Typeface.NORMAL);
            chip.setTextColor(genreId == selectedGenre ? Color.WHITE : Color.parseColor("#CCCCCC"));
            chip.setBackgroundResource(genreId == selectedGenre
                    ? R.drawable.bg_rose_gradient : R.drawable.bg_card);

            chip.setOnClickListener(v -> {
                selectedGenre = genreId;
                sectionTitle.setText(genreLabel.replaceAll("[^a-zA-ZÀ-ÿ ]", "").trim());
                allMovies.clear();
                currentPage = 1;
                myMovieAdapter = null;
                recyclerView.setAdapter(null);
                buildGenreChips();
                fetchMoviesPage(1);
            });
            genreChipsLayout.addView(chip);
        }
    }

    // ── Fetch films ───────────────────────────────────────────────────────────
    private void fetchMoviesPage(int page) {
        if (isLoading) return;
        isLoading = true;

        StringBuilder url = new StringBuilder(
                "https://api.themoviedb.org/3/discover/movie"
                        + "?api_key=" + TMDB_API_KEY
                        + "&language=fr-FR&sort_by=popularity.desc"
                        + "&with_origin_country=MA&page=" + page);
        if (selectedGenre != 0) url.append("&with_genres=").append(selectedGenre);

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url.toString(), null,
                response -> {
                    try {
                        totalPages  = response.optInt("total_pages", 1);
                        currentPage = response.optInt("page", page);
                        JSONArray results = response.getJSONArray("results");

                        for (int i = 0; i < results.length(); i++) {
                            JSONObject obj = results.getJSONObject(i);
                            int    id      = obj.getInt("id");
                            String title   = obj.optString("title",
                                    obj.optString("original_title", "Sans titre"));
                            String date    = obj.optString("release_date", "N/A");
                            String poster  = obj.optString("poster_path", "");
                            if (!poster.isEmpty())
                                allMovies.add(new MyMovieData(id, title, date, poster));
                        }

                        MyMovieData[] movies = allMovies.toArray(new MyMovieData[0]);
                        if (myMovieAdapter == null) {
                            myMovieAdapter = new MyMovieAdapter(movies, this, true);
                            recyclerView.setAdapter(myMovieAdapter);
                        } else {
                            myMovieAdapter.updateData(movies);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON error: " + e.getMessage());
                    }
                    isLoading = false;
                },
                error -> { Log.e(TAG, "Error: " + error.toString()); isLoading = false; }
        );
        queue.add(request);
    }

    // ── Recherche ─────────────────────────────────────────────────────────────
    private void setupSearchListener() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (myMovieAdapter != null) myMovieAdapter.getFilter().filter(s);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAuthButton();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) speechRecognizer.destroy();
    }
}
