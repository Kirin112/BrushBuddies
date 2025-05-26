package com.brushb.brushbuddies;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.brushb.brushbuddies.classes.SafeClickListener;
import com.brushb.brushbuddies.classes.SessionManager;
import com.brushb.brushbuddies.models.Lobby;
import com.brushb.brushbuddies.models.User;
import com.brushb.brushbuddies.services.Auth;
import com.brushb.brushbuddies.services.Database;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {
    private Database database;
    private Auth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        database = new Database();
        auth = new Auth(this);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        String uid = auth.getCurrentUser().getUid();
        database.fetchUser(uid, new Database.UserFetchCallback() {
            @Override
            public void onSuccess(User user) {
                SessionManager.get().setCurrentUser(user);
            }

            @Override
            public void onError(String errorMessage) {

            }
        });


        DrawerLayout drawer = findViewById(R.id.drawer_layout);

        MaterialButton btnProfile = findViewById(R.id.profileBtn);

        btnProfile.setOnClickListener(v -> {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.profile_drawer, new ProfileFragment())
                    .commit();

            drawer.openDrawer(GravityCompat.END);
        });

        //кнопка создания лобби
        findViewById(R.id.createLobbyBtn).setOnClickListener(new SafeClickListener() {
            @Override
            public void onSafeClick(View v) {
                createLobby();
            }
        });

        findViewById(R.id.joinLobbyBtn).setOnClickListener(new SafeClickListener() {
            @Override
            public void onSafeClick(View v) {
                showJoinDialog();
            }
        });
    }

    private void createLobby() {
        String userId = auth.getCurrentUser().getUid();
        database.createLobby(userId, new Database.DatabaseCallback() {
            @Override
            public void onSuccess(String lobbyId) {
                startLobbyActivity(lobbyId, true);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showJoinDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Введите код лобби (4 символа)");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        builder.setView(input);

        builder.setPositiveButton("Подключиться", (dialog, which) -> {
            String code = input.getText().toString().trim().toUpperCase();
            if (code.length() == 4) {
                joinLobby(code);
            } else {
                showError("Код должен содержать 4 символа");
            }
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void joinLobby(String code) {
        Log.d("lobby", "Trying to find lobby with code: " + code);
        showLoading(true);

        database.findLobbyByCode(code, new Database.DatabaseCallback() {
            @Override
            public void onSuccess(String lobbyId) {
                database.getLobbyById(lobbyId, new Database.LobbyCallback() {
                    @Override
                    public void onSuccess(Lobby lobby) {
                        if (!Lobby.STATE_WAITING.equals(lobby.getState())) {
                            showLoading(false);
                            showError("Нельзя подключиться, игра уже началась или лобби неактивно");
                            return;
                        }

                        String currentUserId = auth.getCurrentUser().getUid();
                        database.checkIfUserIsHost(lobbyId, currentUserId, isHost -> {
                            if (isHost) {
                                showError("Вы уже являетесь хостом этого лобби");
                                showLoading(false);
                            } else {
                                addPlayerToLobby(lobbyId, currentUserId);
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        showLoading(false);
                        showError("Ошибка загрузки лобби: " + error);
                    }
                });
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                showError(error);
            }
        });
    }

    private void addPlayerToLobby(String lobbyId, String userId) {
        database.addPlayerToLobby(lobbyId, userId, new Database.DatabaseCallback() {
            @Override
            public void onSuccess(String lobbyId) {
                showLoading(false);
                startLobbyActivity(lobbyId, false);
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                showError(error);
            }
        });
    }

    private void showLoading(boolean show) {
        findViewById(R.id.progressBar).setVisibility(show ? View.VISIBLE : View.GONE);
        findViewById(R.id.createLobbyBtn).setEnabled(!show);
        findViewById(R.id.joinLobbyBtn).setEnabled(!show);
    }

    private void startLobbyActivity(String lobbyId, boolean isHost) {
        Intent intent = new Intent(this, LobbyActivity.class);
        intent.putExtra("LOBBY_ID", lobbyId);
        intent.putExtra("IS_HOST", isHost);
        startActivity(intent);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}