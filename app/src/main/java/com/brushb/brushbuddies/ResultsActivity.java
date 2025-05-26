package com.brushb.brushbuddies;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.brushb.brushbuddies.classes.PlayerResult;
import com.brushb.brushbuddies.classes.ResultsAdapter;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

public class ResultsActivity extends AppCompatActivity implements ResultsAdapter.OnItemClickListener {
    private RecyclerView recyclerView;
    private ResultsAdapter adapter;
    private ImageView imageFullDrawing;
    private ConstraintLayout championLayout;
    private TextView nameView;
    private TextView scoreView;
    private TextView trophies;
    private String lobbyId;
    private boolean isHost;
    private MaterialButton btnReturn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        lobbyId = getIntent().getStringExtra("LOBBY_ID");
        isHost = getIntent().getBooleanExtra("IS_HOST", false);

        recyclerView = findViewById(R.id.recyclerResults);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        imageFullDrawing = findViewById(R.id.imageFullDrawing);
        championLayout = findViewById(R.id.championLayout);
        nameView = findViewById(R.id.championName);
        scoreView = findViewById(R.id.championScore);
        trophies = findViewById(R.id.trophies);

        btnReturn = findViewById(R.id.btnReturnLobby);
        btnReturn.setOnClickListener(v -> {
            Intent intent = new Intent(ResultsActivity.this, LobbyActivity.class);
            intent.putExtra("LOBBY_ID", lobbyId);
            intent.putExtra("IS_HOST", isHost);
            startActivity(intent);
            finish();
        });

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        ArrayList<PlayerResult> results =
                (ArrayList<PlayerResult>) getIntent().getSerializableExtra("results");

        if (results != null && !results.isEmpty()) {

            results.sort((a, b) -> Float.compare(b.getAverageScore(), a.getAverageScore()));

            PlayerResult champion = results.get(0);
            showChampionScreen(champion);
            onItemClick(results.get(0));
            adapter = new ResultsAdapter(results, this);
            recyclerView.setAdapter(adapter);
        } else {
            Toast.makeText(this, "Нет данных для отображения", Toast.LENGTH_SHORT).show();
        }


    }

    private void showChampionScreen(PlayerResult champion) {
        nameView.setVisibility(View.INVISIBLE);
        scoreView.setVisibility(View.INVISIBLE);
        btnReturn.setVisibility(View.GONE);
        trophies.setAlpha(0f);
        championLayout.setVisibility(View.VISIBLE);

        // Анимация появления текста
        nameView.setVisibility(View.VISIBLE);
        nameView.postDelayed(() -> nameView.setText("И победитель."), 1000);
        nameView.postDelayed(() -> nameView.setText("И победитель.."), 2000);
        nameView.postDelayed(() -> nameView.setText("И победитель..."), 3000);


        nameView.postDelayed(() -> {
            nameView.setText("🏆 Победитель: " + champion.getPlayerName());
            scoreView.setText("Средний балл: " + champion.getAverageScore());


            scoreView.setAlpha(0f);
            scoreView.setVisibility(View.VISIBLE);
            scoreView.animate().alpha(1f).setDuration(1000).start();


            trophies.animate()
                    .alpha(1f)
                    .scaleX(1.5f)
                    .scaleY(1.5f)
                    .setDuration(500)
                    .withEndAction(() -> trophies.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(300)
                            .start())
                    .start();
        }, 3500);


        championLayout.postDelayed(() -> {
            championLayout.setVisibility(View.GONE);

            btnReturn.setVisibility(View.VISIBLE);
            btnReturn.setAlpha(0f);
            btnReturn.animate()
                    .alpha(1f)
                    .setDuration(800)
                    .start();
        }, 7000);

    }


    @Override
    public void onItemClick(PlayerResult result) {
        showFullImage(result.getBase64Image());
    }

    private void showFullImage(String base64Image) {
        if (base64Image == null || base64Image.isEmpty()) return;

        byte[] decoded = Base64.decode(base64Image, Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);

        imageFullDrawing.post(() -> {
            imageFullDrawing.setImageBitmap(bitmap);
        });
    }

}