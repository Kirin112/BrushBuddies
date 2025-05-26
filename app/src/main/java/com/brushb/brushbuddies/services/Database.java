package com.brushb.brushbuddies.services;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.brushb.brushbuddies.models.Lobby;
import com.brushb.brushbuddies.models.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


public class Database {
    private static final String TAG = "DatabaseService";
    private final DatabaseReference database;
    private final DatabaseReference usersRef;
    private final DatabaseReference lobbiesRef;
    private final FirebaseDatabase realtimeDb;
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();


    public Database() {
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        this.database = db.getReference();
        this.usersRef = database.child("users");
        this.lobbiesRef = database.child("lobbies");
        this.realtimeDb = db;
    }

    //юзеры
    public void saveUserData(User user) {
        usersRef.child(user.getUid()).setValue(user)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User data saved"))
                .addOnFailureListener(e -> Log.e(TAG, "Save user failed", e));
    }

    public void updateUserStats(String uid, int scoreToAdd, boolean isWin) {
        DatabaseReference userRef = usersRef.child(uid);
        userRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                User user = currentData.getValue(User.class);
                if (user == null) {
                    return Transaction.success(currentData);
                }
                user.setTotalGames(user.getTotalGames() + 1);
                user.setTotalScore(user.getTotalScore() + scoreToAdd);
                user.setAverageScore((float) user.getTotalScore() / user.getTotalGames());
                if (isWin) {
                    user.setWins(user.getWins() + 1);
                }
                currentData.setValue(user);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error,
                                   boolean committed,
                                   @Nullable DataSnapshot snapshot) {
                if (error != null) {
                    Log.e(TAG, "updateUserStats transaction failed", error.toException());
                }
            }
        });
    }

    public interface DrawingsCallback {
        void onSuccess(Map<String, String> drawingsMap);

        void onError(String errorMessage);
    }

    public void fetchAllDrawings(String lobbyId, DrawingsCallback callback) {
        lobbiesRef
                .child(lobbyId)
                .child("drawings")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Map<String, String> drawings = new HashMap<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            String uid = child.getKey();
                            String b64 = child.getValue(String.class);
                            if (uid != null && b64 != null) {
                                drawings.put(uid, b64);
                            }
                        }
                        callback.onSuccess(drawings);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError(error.getMessage());
                    }
                });
    }

    public interface UserFetchCallback {
        void onSuccess(User user);

        void onError(String errorMessage);
    }

    public void fetchUser(String uid, UserFetchCallback callback) {
        usersRef.child(uid).get()
                .addOnSuccessListener(dataSnapshot -> {
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null) {
                        callback.onSuccess(user);
                    } else {
                        callback.onError("Профиль не найден");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public DatabaseReference getUserRef(String uid) {
        return usersRef.child(uid);
    }

    public void updateUserField(String uid, String field, Object value) {
        usersRef.child(uid).child(field).setValue(value);
    }

    public void setPlayerDisconnectCleanup(String lobbyId, String userId) {
        DatabaseReference ref = realtimeDb.getReference("lobbies")
                .child(lobbyId)
                .child("players")
                .child(userId);
        ref.onDisconnect().removeValue();
    }

    //Лобби
    public void createLobby(String hostUid, DatabaseCallback callback) {
        usersRef.child(hostUid).child("username").get()
                .addOnSuccessListener(snapshot -> {
                    String hostName = snapshot.getValue(String.class);
                    if (hostName == null) {
                        callback.onError("Не удалось получить имя хоста");
                        return;
                    }

                    Lobby newLobby = new Lobby(hostUid, hostName);
                    lobbiesRef.child(newLobby.getLobbyId()).setValue(newLobby)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    callback.onSuccess(newLobby.getLobbyId());
                                } else {
                                    callback.onError("Не удалось создать лобби");
                                }
                            });

                }).addOnFailureListener(e -> {
                    callback.onError("Ошибка получения имени пользователя: " + e.getMessage());
                });
    }


    public void addPlayerToLobby(String lobbyId, String userId, DatabaseCallback callback) {
        usersRef.child(userId).child("username").get()
                .addOnSuccessListener(snapshot -> {
                    String username = snapshot.getValue(String.class);

                    Map<String, Object> update = new HashMap<>();
                    update.put("players/" + userId, username);

                    lobbiesRef.child(lobbyId).updateChildren(update)
                            .addOnSuccessListener(aVoid -> callback.onSuccess(lobbyId))
                            .addOnFailureListener(e -> callback.onError("Не удалось присоединиться к лобби: " + e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError("Не удалось получить имя пользователя"));
    }


    public interface WordCallback {
        void onWordSelected(String word);

        void onError(String error);
    }

    public void fetchRandomThemeWord(WordCallback callback) {
        firestore.collection("drawing_themes")
                .document("themes_1")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> words = (List<String>) documentSnapshot.get("names");
                        if (words == null || words.isEmpty()) {
                            callback.onError("Нет слов в массиве");
                            return;
                        }
                        Random random = new Random();
                        String randomWord = words.get(random.nextInt(words.size()));
                        callback.onWordSelected(randomWord);
                    } else {
                        callback.onError("Документ не найден");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void setCurrentWord(String lobbyId, String word, DatabaseCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("current_word", word);
        lobbiesRef.child(lobbyId).updateChildren(updates)
                .addOnSuccessListener(unused -> callback.onSuccess(lobbyId))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getCurrentWord(String lobbyId, Database.WordCallback callback) {
        lobbiesRef
                .child(lobbyId)
                .child("current_word")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String word = snapshot.getValue(String.class);
                        if (word != null) {
                            callback.onWordSelected(word);
                        } else {
                            callback.onError("Тема не найдена");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError(error.getMessage());
                    }
                });
    }

    public void changeHost(String lobbyId, String newHostId, DatabaseCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("host", newHostId);
        lobbiesRef.child(lobbyId).updateChildren(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess(newHostId))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void saveDrawing(String lobbyId, String userId, String drawingBase64) {
        Log.d(TAG, "Saving drawing for userId=" + userId + " in lobby=" + lobbyId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("drawings/" + userId, drawingBase64);
        lobbiesRef.child(lobbyId).updateChildren(updates)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Drawing saved for userId=" + userId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save drawing for userId=" + userId, e));
    }


    public void deleteLobby(String lobbyId) {
        lobbiesRef.child(lobbyId).removeValue();
    }


    public void listenToLobby(String lobbyId, ValueEventListener listener) {
        lobbiesRef.child(lobbyId).addValueEventListener(listener);
    }

    public interface LobbyCallback {
        void onSuccess(Lobby lobby);

        void onError(String errorMessage);
    }

    public void getLobbyById(String lobbyId, LobbyCallback callback) {
        lobbiesRef.child(lobbyId).get()
                .addOnSuccessListener(dataSnapshot -> {
                    Lobby lobby = dataSnapshot.getValue(Lobby.class);
                    if (lobby != null) {
                        callback.onSuccess(lobby);
                    } else {
                        callback.onError("Лобби не найдено");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void findLobbyByCode(String code, DatabaseCallback callback) {
        Log.d(TAG, "Trying to find lobby with code: " + code);
        lobbiesRef.orderByChild("code").equalTo(code).limitToFirst(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Log.d(TAG, "Found lobby: " + snapshot.exists());
                        if (snapshot.exists()) {
                            for (DataSnapshot child : snapshot.getChildren()) {
                                callback.onSuccess(child.getKey()); // lobbyId
                                return;
                            }
                        } else {
                            callback.onError("Лобби не существует");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError("Ошибка подключения к базе данных.");
                    }
                });
    }

    public void getLobbyCode(String lobbyId, LobbyCodeCallback callback) {
        lobbiesRef.child(lobbyId).child("code").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onCodeReceived(task.getResult().getValue(String.class));
                    }
                });
    }

    public void removePlayerFromLobby(String lobbyId, String userId, DatabaseCallback callback) {
        lobbiesRef.child(lobbyId).child("players").child(userId).removeValue()
                .addOnSuccessListener(unused -> callback.onSuccess(lobbyId))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public DatabaseReference getLobbiesRef(String lobbyId) {
        return lobbiesRef.child(lobbyId);
    }

    public interface LobbyCodeCallback {
        void onCodeReceived(String code);
    }

    public void checkIfUserIsHost(String lobbyId, String userId, HostCheckCallback callback) {
        lobbiesRef.child(lobbyId).child("host").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String hostId = task.getResult().getValue(String.class);
                        callback.onResult(hostId != null && hostId.equals(userId));
                    } else {
                        callback.onResult(false);
                    }
                });
    }

    public interface HostCheckCallback {
        void onResult(boolean isHost);
    }

    public void updateLobbyState(String lobbyId, String newState, DatabaseCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("state", newState);

        lobbiesRef.child(lobbyId).updateChildren(updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess(lobbyId);
                    } else {
                        callback.onError("Не удалось обновить статус лобби");
                    }
                });
    }

    public interface DatabaseCallback {
        void onSuccess(String lobbyId);

        void onError(String errorMessage);
    }
}
