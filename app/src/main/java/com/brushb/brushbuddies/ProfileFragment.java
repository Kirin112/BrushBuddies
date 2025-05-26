package com.brushb.brushbuddies;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.brushb.brushbuddies.classes.SessionManager;
import com.brushb.brushbuddies.models.User;
import com.brushb.brushbuddies.services.Auth;
import com.brushb.brushbuddies.services.Database;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public class ProfileFragment extends Fragment {
    private TextView tvUsername, tvStats;
    private Button btnLogout;
    private Database db;
    private Auth auth;
    private ValueEventListener userListener;
    private DatabaseReference userRef;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        tvUsername = view.findViewById(R.id.tvUsername);
        tvStats = view.findViewById(R.id.tvStats);
        btnLogout = view.findViewById(R.id.btnLogout);

        db = new Database();
        auth = new Auth(requireContext());
        FirebaseUser current = auth.getCurrentUser();
        if (current == null) {
            Toast.makeText(getContext(), "Не авторизован", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = current.getUid();
        userRef = db.getUserRef(uid);
        userListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                User user = snap.getValue(User.class);
                if (user != null) {
                    SessionManager.get().setCurrentUser(user);
                    tvUsername.setText(user.getUsername());
                    tvStats.setText(String.format(
                            "🎮 %d\n🏆 %d\n⭐ %d",
                            user.getTotalGames(),
                            user.getWins(),
                            user.getTotalScore()
                    ));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(),
                        "Ошибка загрузки профиля: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        };

        userRef.addValueEventListener(userListener);

        btnLogout.setOnClickListener(v -> {
            auth.logout();
            requireActivity().finish();
            startActivity(new Intent(getContext(), AuthActivity.class));
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userRef != null && userListener != null) {
            userRef.removeEventListener(userListener);
        }
    }
}

