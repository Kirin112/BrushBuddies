package com.brushb.brushbuddies;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.brushb.brushbuddies.services.Auth;
import com.google.firebase.auth.FirebaseUser;

public class RegisterActivity extends AppCompatActivity {

    private Auth auth;
    private EditText etUsername, etEmail, etPassword;
    private TextView tvError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        auth = new Auth(this);

        // Если уже залогинены, переходим сразу
        if (auth.isLoggedIn()) {
            startMainActivity();
            return;
        }


        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        tvError = findViewById(R.id.tvError);

        findViewById(R.id.btnSubmit).setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                showError("Заполните все поля");
                return;
            }

            auth.registerWithEmail(email, password, username, new Auth.AuthCallback() {
                @Override
                public void onSuccess(FirebaseUser user) {

                    startMainActivity();
                }

                @Override
                public void onError(String errorMessage) {
                    showError(errorMessage);
                }
            });
        });
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(TextView.VISIBLE);
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}