package com.brushb.brushbuddies;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.brushb.brushbuddies.classes.SafeClickListener;
import com.brushb.brushbuddies.services.Auth;
import com.google.firebase.auth.FirebaseUser;

public class AuthActivity extends AppCompatActivity {

    private Auth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        Log.d("AUTH_ACTIVITY", "Activity created");

        auth = new Auth(this);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        if (auth.isLoggedIn()) {
            startMainActivity();
            return;
        }

        // Кнопка входа
        findViewById(R.id.btnLogin).setOnClickListener(new SafeClickListener() {
            @Override
            public void onSafeClick(View v) {
                startActivity(new Intent(AuthActivity.this, LoginActivity.class));
            }
        });

        // Кнопка регистрации
        findViewById(R.id.btnRegister).setOnClickListener(new SafeClickListener() {
            @Override
            public void onSafeClick(View v) {
                startActivity(new Intent(AuthActivity.this, RegisterActivity.class));
            }
        });

        // Гостевой вход
        findViewById(R.id.btnGuest).setOnClickListener(new SafeClickListener() {
            @Override
            public void onSafeClick(View v) {
                auth.signInAsGuest(new Auth.AuthCallback() {
                    @Override
                    public void onSuccess(FirebaseUser user) {
                        startMainActivity();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(AuthActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
