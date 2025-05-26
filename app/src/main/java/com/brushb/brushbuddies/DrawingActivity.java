package com.brushb.brushbuddies;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.*;
import android.view.*;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import com.brushb.brushbuddies.services.Database;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import yuku.ambilwarna.AmbilWarnaDialog;

import java.util.HashMap;

public class DrawingActivity extends AppCompatActivity {
    private DrawingView drawingView;
    private String lobbyId, userId;
    private Boolean isHost;
    private MaterialButton brushButton, eraserButton, fillButton, clearButton, circleButton, rectangleButton, triangleButton, starButton, lineButton;
    private TextView countdownText, topicText, timerText, textTopic2;
    private FrameLayout overlayLayout;
    private View colorPreview;

    private Database database;
    private SeekBar brushSizeSeekBar;
    private Spinner brushTypeSpinner;
    private boolean hasSentDrawing = false;
    private boolean isNavigatingToVoting = false;
    private int currentColor = Color.BLACK;
    private FirebaseAuth auth;
    private boolean brushSelectionLocked = false;

    private ValueEventListener drawingsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawing);
        auth = FirebaseAuth.getInstance();
        database = new Database();

        lobbyId = getIntent().getStringExtra("LOBBY_ID");
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        this.getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Toast.makeText(DrawingActivity.this, "Нельзя вернуться назад во время игры", Toast.LENGTH_SHORT).show();
            }
        });
        userId = auth.getCurrentUser().getUid();
        isHost  = getIntent().getBooleanExtra("IS_HOST", false);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        starButton = findViewById(R.id.starButton);
        lineButton = findViewById(R.id.lineButton);
        overlayLayout = findViewById(R.id.overlayLayout);
        countdownText = findViewById(R.id.countdownText);
        topicText = findViewById(R.id.topicText);
        timerText = findViewById(R.id.timerText);
        drawingView = findViewById(R.id.drawingSurfaceView);
        brushButton = findViewById(R.id.brushButton);
        eraserButton = findViewById(R.id.eraserButton);
        fillButton = findViewById(R.id.fillButton);
        clearButton = findViewById(R.id.clearButton);
        circleButton = findViewById(R.id.circleButton);
        rectangleButton = findViewById(R.id.rectangleButton);
        triangleButton = findViewById(R.id.triangleButton);
        brushSizeSeekBar = findViewById(R.id.brushSizeSeekBar);
        colorPreview = findViewById(R.id.colorPreview);
        brushTypeSpinner = findViewById(R.id.brushTypeSpinner);
        textTopic2 = findViewById(R.id.textTopic2);
        overlayLayout.setVisibility(View.VISIBLE);
        database.getCurrentWord(lobbyId, new Database.WordCallback() {
            @Override
            public void onWordSelected(String word) {
                topicText.setText("Тема: " + word);
                textTopic2.setText("Тема: " + word);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(DrawingActivity.this,
                        "Не удалось загрузить тему: " + error,
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        new CountDownTimer(3000, 1000) {
            int count = 3;
            @Override
            public void onTick(long millisUntilFinished) {
                countdownText.setText(String.valueOf(count));
                count--;
            }
            @Override
            public void onFinish() {
                countdownText.setText("Рисуй!");
                countdownText.postDelayed(() -> {
                    overlayLayout.setVisibility(View.GONE);
                    startDrawingTimer();
                }, 1337);
            }
        }.start();


        setButtonActive(brushButton);
        setButtonInactive(eraserButton);
        setButtonInactive(fillButton);
        setButtonInactive(circleButton);
        setButtonInactive(rectangleButton);
        setButtonInactive(triangleButton);
        setButtonInactive(clearButton);

        brushSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                drawingView.setBrushSize(progress);
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        clearButton.setOnClickListener(v -> drawingView.clearCanvas());

        brushButton.setOnClickListener(v -> {
            setButtonActive(brushButton);
            setButtonInactive(eraserButton);
            setButtonInactive(fillButton);
            setButtonInactive(circleButton);
            setButtonInactive(rectangleButton);
            setButtonInactive(triangleButton);
            setButtonInactive(starButton);
            setButtonInactive(lineButton);
            brushSelectionLocked = false;
            drawingView.disableEraser();
            drawingView.disableFillMode();
            drawingView.setShapeType(DrawingView.ShapeType.FREEHAND);
        });

        starButton.setOnClickListener(v -> {
            setButtonActive(starButton);
            setButtonInactive(brushButton);
            setButtonInactive(eraserButton);
            setButtonInactive(fillButton);
            setButtonInactive(circleButton);
            setButtonInactive(rectangleButton);
            setButtonInactive(triangleButton);
            setButtonInactive(lineButton);

            drawingView.disableEraser();
            drawingView.disableFillMode();
            drawingView.setBrushType(DrawingView.BrushType.NORMAL);
            drawingView.setShapeType(DrawingView.ShapeType.STAR);
        });

        lineButton.setOnClickListener(v -> {
            setButtonActive(lineButton);
            setButtonInactive(brushButton);
            setButtonInactive(eraserButton);
            setButtonInactive(fillButton);
            setButtonInactive(circleButton);
            setButtonInactive(rectangleButton);
            setButtonInactive(triangleButton);
            setButtonInactive(starButton);

            drawingView.disableEraser();
            drawingView.disableFillMode();
            drawingView.setBrushType(DrawingView.BrushType.NORMAL);
            drawingView.setShapeType(DrawingView.ShapeType.LINE);
        });

        eraserButton.setOnClickListener(v -> {
            setButtonActive(eraserButton);
            setButtonInactive(brushButton);
            setButtonInactive(fillButton);
            setButtonInactive(circleButton);
            setButtonInactive(rectangleButton);
            setButtonInactive(triangleButton);
            setButtonInactive(starButton);
            setButtonInactive(lineButton);
            brushTypeSpinner.setSelection(0);
            drawingView.setBrushType(DrawingView.BrushType.NORMAL);
            drawingView.disableFillMode();
            drawingView.setShapeType(DrawingView.ShapeType.FREEHAND);
            drawingView.enableEraser();
            brushSelectionLocked = true;
        });

        fillButton.setOnClickListener(v -> {
            setButtonActive(fillButton);
            setButtonInactive(brushButton);
            setButtonInactive(eraserButton);
            setButtonInactive(circleButton);
            setButtonInactive(rectangleButton);
            setButtonInactive(triangleButton);
            setButtonInactive(starButton);
            setButtonInactive(lineButton);
            brushTypeSpinner.setSelection(0);
            drawingView.setBrushType(DrawingView.BrushType.NORMAL);
            drawingView.disableEraser();
            drawingView.setShapeType(DrawingView.ShapeType.FREEHAND);
            drawingView.enableFillMode();
            brushSelectionLocked = true;
        });

        circleButton.setOnClickListener(v -> {
            setButtonActive(circleButton);
            setButtonInactive(brushButton);
            setButtonInactive(eraserButton);
            setButtonInactive(fillButton);
            setButtonInactive(rectangleButton);
            setButtonInactive(triangleButton);
            setButtonInactive(starButton);
            setButtonInactive(lineButton);

            drawingView.disableEraser();
            drawingView.disableFillMode();
            drawingView.setBrushType(DrawingView.BrushType.NORMAL);
            drawingView.setShapeType(DrawingView.ShapeType.CIRCLE);
        });

        rectangleButton.setOnClickListener(v -> {
            setButtonActive(rectangleButton);
            setButtonInactive(brushButton);
            setButtonInactive(eraserButton);
            setButtonInactive(fillButton);
            setButtonInactive(circleButton);
            setButtonInactive(triangleButton);
            setButtonInactive(starButton);
            setButtonInactive(lineButton);

            drawingView.disableEraser();
            drawingView.disableFillMode();
            drawingView.setBrushType(DrawingView.BrushType.NORMAL);
            drawingView.setShapeType(DrawingView.ShapeType.RECTANGLE);
        });

        triangleButton.setOnClickListener(v -> {
            setButtonActive(triangleButton);
            setButtonInactive(brushButton);
            setButtonInactive(eraserButton);
            setButtonInactive(fillButton);
            setButtonInactive(circleButton);
            setButtonInactive(rectangleButton);
            setButtonInactive(starButton);
            setButtonInactive(lineButton);

            drawingView.disableEraser();
            drawingView.disableFillMode();
            drawingView.setBrushType(DrawingView.BrushType.NORMAL);
            drawingView.setShapeType(DrawingView.ShapeType.TRIANGLE);
        });

        colorPreview.setOnClickListener(v -> {
            openColorPicker();
        });
        String time = "45";
        timerText.setText("Осталось: " + time + " сек");

        brushTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (brushSelectionLocked) {
                    brushTypeSpinner.setSelection(0);
                    Toast.makeText(DrawingActivity.this,
                            "Смена кисти недоступна при активном режиме",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                if (drawingView.getCurrentShape() != DrawingView.ShapeType.FREEHAND) {
                    brushTypeSpinner.setSelection(0);
                    Toast.makeText(DrawingActivity.this,
                            "Для фигур доступна только обычная кисть",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                switch (position) {
                    case 0: drawingView.setBrushType(DrawingView.BrushType.NORMAL); break;
                    case 1: drawingView.setBrushType(DrawingView.BrushType.CALLIGRAPHY); break;
                    case 2: drawingView.setBrushType(DrawingView.BrushType.MARKER); break;
                    case 3: drawingView.setBrushType(DrawingView.BrushType.SPRAY); break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        sendFallbackDrawing();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sendFallbackDrawing();
    }
    private void sendFallbackDrawing() {
        if (hasSentDrawing) return;
        hasSentDrawing = true;

        String base64Drawing = drawingView.getBase64Drawing();
        database.saveDrawing(lobbyId, userId, base64Drawing);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();


        sendFallbackDrawing();

        if(!isNavigatingToVoting) {
        database.removePlayerFromLobby(lobbyId, userId, new Database.DatabaseCallback() {
            @Override
            public void onSuccess(String result) {
                Log.d("LOBBY", "Player removed from lobby: " + userId);
            }

            @Override
            public void onError(String errorMessage) {
                Log.e("LOBBY", "Error removing player: " + errorMessage);
            }
        });
        }
    }


    private void startDrawingTimer() {
        new CountDownTimer(45000, 1000) {
            public void onTick(long millisUntilFinished) {
                int secondsLeft = (int) (millisUntilFinished / 1000);
                timerText.setText("Осталось: " + secondsLeft + " сек");
            }
            public void onFinish() {
                drawingView.postDelayed(() -> {
                    String base64Drawing = drawingView.getBase64Drawing();
                    database.saveDrawing(lobbyId, userId, base64Drawing);
                    startCheckingDrawings();
                }, 500);
            }
        }.start();
    }

    private void setButtonActive(MaterialButton button) {
        button.setSelected(true);
    }

    private void setButtonInactive(MaterialButton button) {
        button.setSelected(false);
    }

    private void openColorPicker() {
        AmbilWarnaDialog colorPicker = new AmbilWarnaDialog(this, currentColor,
                new AmbilWarnaDialog.OnAmbilWarnaListener() {
                    @Override public void onCancel(AmbilWarnaDialog dialog) {}
                    @Override public void onOk(AmbilWarnaDialog dialog, int color) {
                        currentColor = color;
                        drawingView.setColor(color);
                        colorPreview.setBackgroundColor(color);
                    }
                });
        colorPicker.show();
    }

    private void startCheckingDrawings() {
        DatabaseReference lobbyRef = database.getLobbiesRef(lobbyId);
        DatabaseReference playersRef = lobbyRef.child("players");
        DatabaseReference drawingsRef = lobbyRef.child("drawings");

        drawingsListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot drawingsSnapshot) {
                playersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot playersSnapshot) {
                        boolean allHaveDrawings = true;
                        for (DataSnapshot playerSnapshot : playersSnapshot.getChildren()) {
                            String playerId = playerSnapshot.getKey();
                            if (!drawingsSnapshot.hasChild(playerId)) {
                                allHaveDrawings = false;
                                break;
                            }
                        }

                        if (allHaveDrawings) {
                            drawingsRef.removeEventListener(drawingsListener);
                            HashMap<String, String> allDrawings = new HashMap<>();
                            for (DataSnapshot drawingSnapshot : drawingsSnapshot.getChildren()) {
                                String playerId = drawingSnapshot.getKey();
                                String drawingBase64 = drawingSnapshot.getValue(String.class);
                                allDrawings.put(playerId, drawingBase64);
                            }
                            goToVotingActivity(allDrawings);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(DrawingActivity.this, "Ошибка получения игроков", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(DrawingActivity.this, "Ошибка получения рисунков", Toast.LENGTH_SHORT).show();
            }
        };

        drawingsRef.addValueEventListener(drawingsListener);
    }

    private void goToVotingActivity(HashMap<String, String> allDrawings) {
        isNavigatingToVoting = true;
        Intent intent = new Intent(DrawingActivity.this, VotingActivity.class);
        intent.putExtra("LOBBY_ID", lobbyId);
        intent.putExtra("IS_HOST", isHost);
        startActivity(intent);
        finish();
    }



}