package com.brushb.brushbuddies.services;

import android.content.Context;

import com.brushb.brushbuddies.models.User;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;


public class Auth {
    private static final String TAG = "AuthService";
    private final FirebaseAuth auth;
    private final Context context;
    private final Database database;

    public Auth(Context context) {
        this.auth = FirebaseAuth.getInstance();
        this.context = context;
        this.database = new Database();
    }


    public void signInAsGuest(AuthCallback callback) {
        auth.signInAnonymously()
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        String uid = user.getUid();
                        User guest = new User(uid);
                        database.saveUserData(guest);
                        callback.onSuccess(user);
                    } else {
                        callback.onError("Не удалось получить гостевой аккаунт");
                    }
                })
                .addOnFailureListener(e -> callback.onError(getErrorMessage(e)));
    }


    public void registerWithEmail(String email, String password, String username, AuthCallback callback) {
        FirebaseUser current = auth.getCurrentUser();
        if (current != null && current.isAnonymous()) {
            AuthCredential credential = EmailAuthProvider.getCredential(email, password);
            current.linkWithCredential(credential)
                    .addOnSuccessListener(authResult -> {
                        FirebaseUser linked = authResult.getUser();
                        if (linked != null) {
                            updateUserProfile(linked, username, () -> {
                                User updated = new User(linked.getUid(), username, email);
                                database.saveUserData(updated);
                                callback.onSuccess(linked);
                            }, callback::onError);
                        } else {
                            callback.onError("Не удалось привязать email");
                        }
                    })
                    .addOnFailureListener(e -> callback.onError(getErrorMessage(e)));
        } else {
            auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        FirebaseUser user = authResult.getUser();
                        if (user != null) {
                            updateUserProfile(user, username, () -> {
                                User newUser = new User(user.getUid(), username, email);
                                database.saveUserData(newUser);
                                callback.onSuccess(user);
                            }, callback::onError);
                        } else {
                            callback.onError("Регистрация не удалась");
                        }
                    })
                    .addOnFailureListener(e -> callback.onError(getErrorMessage(e)));
        }
    }

    public void loginWithEmail(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        database.updateUserField(user.getUid(), "lastOnline", System.currentTimeMillis());
                        callback.onSuccess(user);
                    } else {
                        callback.onError("Не удалось войти");
                    }
                })
                .addOnFailureListener(e -> callback.onError(getErrorMessage(e)));
    }

    public void logout() {
        auth.signOut();
    }

    public boolean isLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    private void updateUserProfile(FirebaseUser user,
                                   String username,
                                   Runnable onSuccess,
                                   java.util.function.Consumer<String> onError) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(username)
                .build();

        user.updateProfile(profileUpdates)
                .addOnSuccessListener(aVoid -> onSuccess.run())
                .addOnFailureListener(e -> onError.accept("Не удалось установить имя пользователя"));
    }

    private String getErrorMessage(Exception exception) {
        return exception == null ? "Неизвестная ошибка" : exception.getMessage();
    }

    public interface AuthCallback {
        void onSuccess(FirebaseUser user);

        void onError(String errorMessage);
    }
}
