package com.brushb.brushbuddies;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Base64;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.brushb.brushbuddies.classes.PlayerResult;
import com.brushb.brushbuddies.classes.SessionManager;
import com.brushb.brushbuddies.models.Lobby;
import com.brushb.brushbuddies.models.User;
import com.brushb.brushbuddies.services.Database;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VotingActivity extends AppCompatActivity {
    private static final String TAG = "VotingActivity";

    private Database database;

    private ImageView imageDrawing;
    private RadioGroup ratingGroup;

    private TextView textTimer;
    private HashMap<String, String> drawingsMap;
    private List<String> otherPlayers = new ArrayList<>();
    private int currentIndex = 0;
    private CountDownTimer voteTimer;

    private String lobbyId;
    private boolean isHost;
    private String currentUserId;

    private boolean resultsStarted = false;
    private ValueEventListener votesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voting);

        imageDrawing = findViewById(R.id.imageFullDrawing);
        ratingGroup = findViewById(R.id.ratingGroup);
        textTimer = findViewById(R.id.textTimer);
        lobbyId = getIntent().getStringExtra("LOBBY_ID");
        currentUserId = FirebaseAuth.getInstance().getUid();
        isHost  = getIntent().getBooleanExtra("IS_HOST", false);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        database = new Database();

        database.fetchAllDrawings(lobbyId, new Database.DrawingsCallback() {
            @Override
            public void onSuccess(Map<String, String> map) {
                drawingsMap = new HashMap<>(map);
                loadOtherPlayers();
            }
            @Override
            public void onError(String error) {
                Toast.makeText(VotingActivity.this,
                        "Не удалось загрузить рисунки: " + error,
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        if (lobbyId == null || currentUserId == null) {
            Toast.makeText(this, "Ошибка: неверные параметры лобби", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }



        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {

            }
        });
    }


    private void loadOtherPlayers() {
        DatabaseReference playersRef = FirebaseDatabase.getInstance()
                .getReference("lobbies").child(lobbyId).child("players");

        playersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                otherPlayers.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    String pid = snap.getKey();
                    if (!pid.equals(currentUserId) && drawingsMap.containsKey(pid)) {
                        otherPlayers.add(pid);
                        Log.d(TAG, "Added player to vote: " + pid);
                    }
                }

                if (otherPlayers.isEmpty()) {
                    Toast.makeText(VotingActivity.this, "Нет игроков для голосования", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                Log.d(TAG, "Players to vote count: " + otherPlayers.size());
                showNextDrawing();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load players: " + error.getMessage());
                Toast.makeText(VotingActivity.this, "Не удалось загрузить игроков", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void showNextDrawing() {
        Log.d(TAG, "Showing drawing " + (currentIndex + 1) + " of " + otherPlayers.size());
        ratingGroup.check(R.id.rating5);
        if (currentIndex >= otherPlayers.size()) {
            Log.d(TAG, "No more drawings to show");
            waitForAllVotes();
            return;
        }

        String pid = otherPlayers.get(currentIndex);
        String b64 = drawingsMap.get(pid);

        if (b64 == null || b64.isEmpty()) {
            Log.w(TAG, "Empty drawing for player: " + pid);
            Toast.makeText(this, "Пропускаем отсутствующий рисунок", Toast.LENGTH_SHORT).show();
            currentIndex++;
            showNextDrawing();
            return;
        }

        try {
            byte[] data = Base64.decode(b64, Base64.DEFAULT);
            Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);

            if (bmp == null) {
                Log.e(TAG, "Failed to decode bitmap for player: " + pid);
                Toast.makeText(this, "Ошибка декодирования рисунка", Toast.LENGTH_SHORT).show();
                currentIndex++;
                showNextDrawing();
                return;
            }

            imageDrawing.setImageBitmap(bmp);
            ratingGroup.check(R.id.rating5);
            if (voteTimer != null) voteTimer.cancel();

            voteTimer = new CountDownTimer(10000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    textTimer.setText(String.valueOf(millisUntilFinished / 1000));
                }

                @Override
                public void onFinish() {
                    submitVote();
                    textTimer.setText("0");
                }
            }.start();
            Log.d(TAG, "Successfully displayed drawing for player: " + pid);

        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid base64 for player: " + pid, e);
            Toast.makeText(this, "Неверный формат рисунка", Toast.LENGTH_SHORT).show();
            currentIndex++;
            showNextDrawing();
        }
    }

    private void submitVote() {
        if (currentIndex >= otherPlayers.size()) {
            Log.w(TAG, "Invalid voting index");
            return;
        }

        int selId = ratingGroup.getCheckedRadioButtonId();
        if (selId == -1) {
            Toast.makeText(this, "Выберите оценку", Toast.LENGTH_SHORT).show();
            return;
        }



        String pid = otherPlayers.get(currentIndex);
        int rating = Integer.parseInt(((RadioButton) findViewById(selId)).getText().toString());
        Log.d(TAG, "Submitting vote for player " + pid + ": " + rating);

        DatabaseReference voteRef = FirebaseDatabase.getInstance()
                .getReference("lobbies").child(lobbyId)
                .child("votes").child(currentUserId).child(pid);

        voteRef.setValue(rating).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Vote submitted successfully");
                currentIndex++;
                showNextDrawing();
            } else {
                Log.e(TAG, "Failed to submit vote: " + task.getException());
                Toast.makeText(VotingActivity.this, "Ошибка при голосовании", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void waitForAllVotes() {
        DatabaseReference lobbyRef = FirebaseDatabase.getInstance()
                .getReference("lobbies").child(lobbyId);

        lobbyRef.child("players").get().addOnSuccessListener(playersSnap -> {
            List<String> activePlayers = new ArrayList<>();
            for (DataSnapshot p : playersSnap.getChildren()) {
                activePlayers.add(p.getKey());
            }

            DatabaseReference votesRef = lobbyRef.child("votes");

            if (votesListener != null) {
                votesRef.removeEventListener(votesListener);
            }

            votesListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snap) {
                    Log.d(TAG, "Checking if all votes are in...");

                    boolean everyoneVoted = true;
                    int activeCount = activePlayers.size();

                    for (String pid : activePlayers) {
                        int voteCount = 0;
                        for (DataSnapshot voter : snap.getChildren()) {
                            String voterId = voter.getKey();
                            if (!activePlayers.contains(voterId)) {
                                continue;
                            }
                            if (voter.hasChild(pid)) voteCount++;
                        }

                        if (voteCount < activeCount - 1) {
                            everyoneVoted = false;
                            Log.d(TAG, "Player " + pid + " has only " + voteCount + " votes, needs " + (activeCount - 1));
                            break;
                        }
                    }

                    if (everyoneVoted && !resultsStarted) {
                        resultsStarted = true;
                        votesRef.removeEventListener(this);
                        Log.d(TAG, "All votes are in, calculating results");
                        calculateResultsAndGo();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError e) {
                    Log.e(TAG, "Votes listener cancelled: " + e.getMessage());
                    Toast.makeText(VotingActivity.this, "Ошибка загрузки голосов", Toast.LENGTH_SHORT).show();
                }
            };

            votesRef.addValueEventListener(votesListener);

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to load players for voting check: " + e.getMessage());
            Toast.makeText(VotingActivity.this, "Ошибка загрузки игроков для голосования", Toast.LENGTH_SHORT).show();
        });
    }


    private void calculateResultsAndGo() {
        DatabaseReference lobbyRef = FirebaseDatabase.getInstance()
                .getReference("lobbies").child(lobbyId);

        lobbyRef.child("players").get().addOnSuccessListener(playersSnap -> {
                    Map<String,String> playersMap = new HashMap<>();
                    for (DataSnapshot p : playersSnap.getChildren()) {
                        playersMap.put(p.getKey(),        // uid
                                p.getValue(String.class)); // username
                    }

                    lobbyRef.child("votes").get().addOnSuccessListener(votesSnap -> {
                                Map<String,List<Integer>> scoresMap = new HashMap<>();
                                for (DataSnapshot voter : votesSnap.getChildren()) {
                                    for (DataSnapshot voted : voter.getChildren()) {
                                        String votedId = voted.getKey();
                                        Integer score = voted.getValue(Integer.class);
                                        if (score == null) continue;
                                        scoresMap
                                                .computeIfAbsent(votedId, k -> new ArrayList<>())
                                                .add(score);
                                    }
                                }

                                ArrayList<PlayerResult> results = new ArrayList<>();
                                for (Map.Entry<String,String> entry : playersMap.entrySet()) {
                                    String uid      = entry.getKey();
                                    String username = entry.getValue();
                                    List<Integer> scores = scoresMap.get(uid);
                                    Log.d(TAG, "Player: " + uid + " (" + username + "), scores: " + scores);
                                    if (scores == null || scores.isEmpty()) continue;
                                    float sum = 0;
                                    for (int s : scores) sum += s;
                                    float avg = sum / scores.size();

                                    results.add(new PlayerResult(uid, username,
                                            drawingsMap.get(uid),
                                            avg));
                                }

                                if (results.isEmpty()) {
                                    Toast.makeText(VotingActivity.this,
                                            "Нет результатов для отображения",
                                            Toast.LENGTH_SHORT).show();
                                    finish();
                                    return;
                                }

                                results.sort((a, b) -> Float.compare(b.getAverageScore(),
                                        a.getAverageScore()));

                                // Получаем текущего пользователя
                                User currentUser = SessionManager.get().getCurrentUser();
                                if (currentUser == null) {
                                    proceedToResults(results);
                                    return;
                                }

                                // Проверяем, является ли текущий пользователь хостом
                                database.checkIfUserIsHost(lobbyId, currentUser.getUid(), isHost -> {
                                    if (isHost) {
                                        String winnerUid = results.get(0).getPlayerUid();
                                        for (PlayerResult pr : results) {
                                            boolean isWinner = pr.getPlayerUid().equals(winnerUid);

                                            int points = Math.round(pr.getAverageScore() * 10)
                                                    + (isWinner ? 50 : 0);

                                            database.updateUserStats(pr.getPlayerUid(),
                                                    points,
                                                    isWinner);
                                        }
                                    }
                                    proceedToResults(results);
                                });

                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(VotingActivity.this,
                                            "Ошибка загрузки голосов: "+e.getMessage(),
                                            Toast.LENGTH_SHORT).show());

                })
                .addOnFailureListener(e ->
                        Toast.makeText(VotingActivity.this,
                                "Ошибка загрузки игроков: "+e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void proceedToResults(ArrayList<PlayerResult> results) {
        new Database().updateLobbyState(lobbyId, Lobby.STATE_WAITING, new Database.DatabaseCallback() {
            @Override
            public void onSuccess(String id) {
                Intent i = new Intent(VotingActivity.this, ResultsActivity.class);
                i.putExtra("results", (Serializable) results);
                i.putExtra("LOBBY_ID", lobbyId);
                i.putExtra("IS_HOST", getIntent().getBooleanExtra("IS_HOST", false));
                startActivity(i);
                finish();
            }
            @Override
            public void onError(String error) {
                Intent i = new Intent(VotingActivity.this, ResultsActivity.class);
                i.putExtra("results", (Serializable) results);
                i.putExtra("LOBBY_ID", lobbyId);
                i.putExtra("IS_HOST", isHost);
                startActivity(i);
                finish();
            }
        });
    }




    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (voteTimer != null) {
            voteTimer.cancel();
        }
        if (votesListener != null) {
            FirebaseDatabase.getInstance().getReference("lobbies").child(lobbyId)
                    .child("votes").removeEventListener(votesListener);
        }
    }
}
