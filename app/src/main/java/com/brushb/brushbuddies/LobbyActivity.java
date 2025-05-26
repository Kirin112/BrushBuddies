package com.brushb.brushbuddies;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.brushb.brushbuddies.classes.LobbyPlayersAdapter;
import com.brushb.brushbuddies.classes.SafeClickListener;
import com.brushb.brushbuddies.classes.SessionManager;
import com.brushb.brushbuddies.models.Lobby;
import com.brushb.brushbuddies.models.PlayerInfo;
import com.brushb.brushbuddies.models.User;
import com.brushb.brushbuddies.services.Database;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class LobbyActivity extends AppCompatActivity {
    private Database database;
    private String lobbyId;
    private boolean gameStarted = false;
    private boolean isNavigatingToDrawing = false;
    private LobbyPlayersAdapter adapter;
    private TextView playerCountText;
    private TextView lobbyCodeText;
    private Button startBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        lobbyId = getIntent().getStringExtra("LOBBY_ID");
        database = new Database();

        String currentUserId = SessionManager.get().getCurrentUser().getUid();
        database.setPlayerDisconnectCleanup(lobbyId, currentUserId);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });

        setupUI();
        setupLobbyListener();
    }

    private void setupUI() {
        lobbyCodeText = findViewById(R.id.lobbyCodeText);
        database.getLobbyCode(lobbyId, code -> lobbyCodeText.setText("Код лобби: " + code));

        playerCountText = findViewById(R.id.playerCountText);

        RecyclerView rv = findViewById(R.id.playersRecycler);
        rv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        adapter = new LobbyPlayersAdapter(new ArrayList<>());
        rv.setAdapter(adapter);

        startBtn = findViewById(R.id.startGameBtn);
        startBtn.setVisibility(View.GONE);

        database.checkIfUserIsHost(lobbyId, SessionManager.get().getCurrentUser().getUid(), isHost -> {
            startBtn.setVisibility(isHost ? View.VISIBLE : View.GONE);
        });

        startBtn.setOnClickListener(new SafeClickListener() {
            @Override
            public void onSafeClick(View v) {
                database.fetchRandomThemeWord(new Database.WordCallback() {
                    @Override
                    public void onWordSelected(String word) {
                        database.setCurrentWord(lobbyId, word, new Database.DatabaseCallback() {
                            @Override
                            public void onSuccess(String id) {
                                database.updateLobbyState(lobbyId, Lobby.STATE_DRAWING, new Database.DatabaseCallback() {
                                    @Override
                                    public void onSuccess(String id) {

                                    }

                                    @Override
                                    public void onError(String error) {
                                        Toast.makeText(LobbyActivity.this,
                                                "Ошибка запуска игры: " + error,
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(LobbyActivity.this,
                                        "Ошибка сохранения темы: " + error,
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(LobbyActivity.this,
                                "Ошибка загрузки темы: " + error,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (!isNavigatingToDrawing) {
            String currentUserId = SessionManager.get().getCurrentUser().getUid();
            database.removePlayerFromLobby(lobbyId, currentUserId, new Database.DatabaseCallback() {
                @Override
                public void onSuccess(String id) {
                }

                @Override
                public void onError(String error) {
                }
            });
        }
    }

    private void setupLobbyListener() {
        ValueEventListener lobbyListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Lobby lobby = snapshot.getValue(Lobby.class);

                if (lobby == null || lobby.getPlayers() == null || lobby.getPlayers().isEmpty()) {
                    database.deleteLobby(lobbyId);
                    finish();
                    return;
                }

                String currentHostId = lobby.getHost();
                Map<String, String> players = lobby.getPlayers();
                if (!players.containsKey(currentHostId)) {
                    String newHostId = players.keySet().iterator().next();
                    database.changeHost(lobbyId, newHostId, new Database.DatabaseCallback() {
                        @Override
                        public void onSuccess(String id) {
                            updatePlayersList(lobby);
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(LobbyActivity.this, "Ошибка смены хоста: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    updatePlayersList(lobby);
                }

                String currentUserId = SessionManager.get().getCurrentUser().getUid();
                boolean isCurrentUserHost = lobby.isHost(currentUserId);
                startBtn.setVisibility(isCurrentUserHost ? View.VISIBLE : View.GONE);

                if (Lobby.STATE_DRAWING.equals(lobby.getState()) && !gameStarted) {
                    gameStarted = true;
                    startGame(lobby);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LobbyActivity.this, "Ошибка подключения", Toast.LENGTH_SHORT).show();
            }
        };

        database.listenToLobby(lobbyId, lobbyListener);
    }


    private void startGame(Lobby lobby) {
        isNavigatingToDrawing = true;
        Intent intent = new Intent(LobbyActivity.this, DrawingActivity.class);
        intent.putExtra("LOBBY_ID", lobbyId);
        intent.putExtra("CURRENT_WORD", lobby.getCurrentWord());
        startActivity(intent);
        finish();
    }

    private void updatePlayersList(Lobby lobby) {
        Map<String, String> map = lobby.getPlayers();
        String hostId = lobby.getHost();
        List<PlayerInfo> list = new ArrayList<>();

        for (Map.Entry<String, String> e : map.entrySet()) {
            String uid = e.getKey();
            String name = e.getValue();
            boolean isHost = uid.equals(hostId);

            database.fetchUser(uid, new Database.UserFetchCallback() {
                @Override
                public void onSuccess(User user) {
                    list.add(new PlayerInfo(
                            uid,
                            name,
                            user.getTotalGames(),
                            user.getWins(),
                            user.getTotalScore(),
                            isHost
                    ));
                    Collections.sort(list, (a, b) -> Boolean.compare(!a.isHost, !b.isHost));
                    adapter.updateData(list);
                    playerCountText.setText("Игроков в лобби: " + list.size());
                }

                @Override
                public void onError(String error) {
                }
            });
        }
    }
}
