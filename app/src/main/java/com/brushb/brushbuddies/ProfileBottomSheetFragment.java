package com.brushb.brushbuddies;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.brushb.brushbuddies.models.User;
import com.brushb.brushbuddies.services.Auth;
import com.brushb.brushbuddies.services.Database;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseUser;

public class ProfileBottomSheetFragment extends BottomSheetDialogFragment {

    private TextView tvUsername, tvEmail, tvStats;
    private MaterialButton btnLogout;
    private Auth auth;
    private Database db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_profile_bottom_sheet, container, false);
        tvUsername = v.findViewById(R.id.tvUsername);
        tvStats = v.findViewById(R.id.tvStats);
        btnLogout = v.findViewById(R.id.btnLogout);

        auth = new Auth(getContext());
        db = new Database();

        loadUserData();

        btnLogout.setOnClickListener(view -> {
            auth.logout();
            // Перенаправим на экран авторизации
            startActivity(new Intent(getActivity(), AuthActivity.class));
            getActivity().finish();
        });

        return v;
    }

    private void loadUserData() {
        FirebaseUser current = auth.getCurrentUser();
        if (current == null) {
            dismiss();
            return;
        }

        db.fetchUser(current.getUid(), new Database.UserFetchCallback() {
            @Override
            public void onSuccess(User user) {
                tvUsername.setText(user.getUsername());
                tvStats.setText(
                        "Игры: " + user.getTotalGames() +
                                "  Победы: " + user.getWins() +
                                "  Очки: " + user.getTotalScore()
                );
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Ошибка загрузки профиля: " + error,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}

