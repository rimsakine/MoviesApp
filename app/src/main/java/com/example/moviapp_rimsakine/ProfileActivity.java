package com.example.moviapp_rimsakine;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {
    private static final String TAG = "ProfileActivity";

    private ImageView imageProfile;
    private TextView textProfileEmail;
    private TextView textProfileStatus;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private RequestQueue requestQueue;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        imageProfile = findViewById(R.id.imageProfile);
        textProfileEmail = findViewById(R.id.textProfileEmail);
        textProfileStatus = findViewById(R.id.textProfileStatus);
        Button buttonTakeProfilePhoto = findViewById(R.id.buttonTakeProfilePhoto);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();
        requestQueue = Volley.newRequestQueue(this);

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (Boolean.TRUE.equals(isGranted)) openCamera();
                    else Toast.makeText(this, "Permission camera refusee", Toast.LENGTH_SHORT).show();
                });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                    Bundle extras = result.getData().getExtras();
                    Bitmap bitmap = extras == null ? null : (Bitmap) extras.get("data");
                    if (bitmap != null) {
                        uploadProfilePhoto(bitmap);
                    } else {
                        Toast.makeText(this, "Photo introuvable", Toast.LENGTH_SHORT).show();
                    }
                });

        buttonTakeProfilePhoto.setOnClickListener(v -> startCameraWithPermission());
        bindUser();
    }

    private void bindUser() {
        if (currentUser == null) {
            textProfileEmail.setText("Connectez-vous pour enregistrer une photo");
            return;
        }
        textProfileEmail.setText(currentUser.getEmail());
        String localPhotoUrl = getSharedPreferences("profile", Context.MODE_PRIVATE)
                .getString("profilePhotoUrl", "");
        if (!localPhotoUrl.isEmpty()) {
            Glide.with(this).load(localPhotoUrl).into(imageProfile);
        }
        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String photoUrl = documentSnapshot.getString("profilePhotoUrl");
                    if (photoUrl != null && !photoUrl.isEmpty()) {
                        Glide.with(this).load(photoUrl).into(imageProfile);
                    }
                });
    }

    private void startCameraWithPermission() {
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, "Camera non disponible", Toast.LENGTH_SHORT).show();
            return;
        }
        cameraLauncher.launch(intent);
    }

    private void uploadProfilePhoto(Bitmap bitmap) {
        imageProfile.setImageBitmap(bitmap);
        textProfileStatus.setText("Envoi de la photo...");

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 88, stream);
        byte[] data = stream.toByteArray();
        uploadProfilePhotoToSupabase(data);
    }

    private void uploadProfilePhotoToSupabase(byte[] data) {
        String supabaseUrl = getString(R.string.supabase_url).trim();
        String supabaseAnonKey = getString(R.string.supabase_anon_key).trim();
        String bucket = getString(R.string.supabase_profile_bucket).trim();

        if (supabaseUrl.contains("VOTRE_PROJECT_REF") || supabaseAnonKey.contains("VOTRE_SUPABASE_ANON_KEY")) {
            Toast.makeText(this, "Configurez Supabase dans strings.xml", Toast.LENGTH_LONG).show();
            textProfileStatus.setText("");
            return;
        }

        String fileName = currentUser.getUid() + ".jpg";
        String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + fileName + "?upsert=true";
        String publicUrl = supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + fileName;
        Log.d(TAG, "Uploading profile photo to Supabase: " + uploadUrl);

        Request<byte[]> request = new Request<byte[]>(Request.Method.POST, uploadUrl,
                error -> {
                    textProfileStatus.setText("");
                    String message = "Erreur Supabase upload";
                    if (error.networkResponse != null) {
                        message += " HTTP " + error.networkResponse.statusCode;
                        if (error.networkResponse.data != null) {
                            message += ": " + new String(error.networkResponse.data, StandardCharsets.UTF_8);
                        }
                    } else if (error.getMessage() != null) {
                        message += ": " + error.getMessage();
                    }
                    Log.e(TAG, message, error);
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                }) {
            @Override
            public byte[] getBody() {
                return data;
            }

            @Override
            public String getBodyContentType() {
                return "image/jpeg";
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("apikey", supabaseAnonKey);
                headers.put("Authorization", "Bearer " + supabaseAnonKey);
                headers.put("Content-Type", "image/jpeg");
                headers.put("x-upsert", "true");
                return headers;
            }

            @Override
            protected com.android.volley.Response<byte[]> parseNetworkResponse(NetworkResponse response) {
                return com.android.volley.Response.success(response.data,
                        HttpHeaderParser.parseCacheHeaders(response));
            }

            @Override
            protected void deliverResponse(byte[] response) {
                Log.d(TAG, "Supabase upload success: " + publicUrl);
                textProfileStatus.setText("Photo envoyee");
                saveProfilePhotoUrl(publicUrl);
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                15000,
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(request);
    }

    private void saveProfilePhotoUrl(String photoUrl) {
        SharedPreferences prefs = getSharedPreferences("profile", Context.MODE_PRIVATE);
        prefs.edit().putString("profilePhotoUrl", photoUrl).apply();
        textProfileStatus.setText("Photo de profil enregistree");

        Map<String, Object> update = new HashMap<>();
        update.put("profilePhotoUrl", photoUrl);
        db.collection("users").document(currentUser.getUid())
                .set(update, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(unused -> Log.d(TAG, "Profile URL saved in Firestore"))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore profile update failed", e);
                    Toast.makeText(this, "Photo gardee localement, Firestore hors ligne", Toast.LENGTH_LONG).show();
                });
    }

}
