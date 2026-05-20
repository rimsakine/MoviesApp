package com.example.moviapp_rimsakine;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText      editName, editEmail, editPassword;
    private FirebaseAuth  auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        editName     = findViewById(R.id.editName);
        editEmail    = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        Button   btnRegister = findViewById(R.id.btnRegister);
        TextView tvGoLogin   = findViewById(R.id.tvGoLogin);

        btnRegister.setOnClickListener(v -> registerUser());
        tvGoLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {
        String name     = editName.getText().toString().trim();
        String email    = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name)) { editName.setError("Nom requis"); return; }
        if (TextUtils.isEmpty(email)) { editEmail.setError("Email requis"); return; }
        if (password.length() < 6) { editPassword.setError("Min. 6 caractères"); return; }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = auth.getCurrentUser();
                    if (user != null) {
                        // Créer le profil utilisateur dans Firestore
                        Map<String, Object> userProfile = new HashMap<>();
                        userProfile.put("name", name);
                        userProfile.put("email", email);
                        userProfile.put("favoriteGenres", new ArrayList<>());
                        userProfile.put("favoriteMovies", new ArrayList<>());

                        db.collection("users").document(user.getUid())
                                .set(userProfile)
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(this, "Compte créé !", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(this, MainActivity.class));
                                    finish();
                                });
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}