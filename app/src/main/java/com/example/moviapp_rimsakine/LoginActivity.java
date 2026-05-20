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

public class LoginActivity extends AppCompatActivity {

    private EditText    editEmail, editPassword;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();

        // Si déjà connecté → aller directement à MainActivity
        if (auth.getCurrentUser() != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        editEmail    = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        Button btnLogin     = findViewById(R.id.btnLogin);
        TextView tvRegister = findViewById(R.id.tvGoRegister);
        TextView tvSkip     = findViewById(R.id.tvSkip);

        btnLogin.setOnClickListener(v -> loginUser());

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });

        tvSkip.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    private void loginUser() {
        String email    = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            editEmail.setError("Email requis");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            editPassword.setError("Mot de passe requis");
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    Toast.makeText(this, "Bienvenue !", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}