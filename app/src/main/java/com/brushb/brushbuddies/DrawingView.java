package com.brushb.brushbuddies;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.View;
import com.brushb.brushbuddies.classes.FloodFill;

import java.io.ByteArrayOutputStream;
import java.util.Random;

public class DrawingView extends View {

    public enum BrushType {
        NORMAL, MARKER, SPRAY, CALLIGRAPHY
    }

    public enum ShapeType {
        FREEHAND, CIRCLE, RECTANGLE, TRIANGLE, LINE, STAR
    }

    private Paint paint;
    private Path path;
    private Bitmap canvasBitmap;
    private Canvas drawCanvas;

    private int currentColor = Color.BLACK;
    private float brushSize = 10f;
    private boolean isErasing = false;
    private boolean isFillMode = false;
    private BrushType currentBrushType = BrushType.NORMAL;
    private FloodFill floodFill = new FloodFill();
    private Random random = new Random();

    private ShapeType currentShape = ShapeType.FREEHAND;
    private float startX, startY, currentX, currentY;
    private boolean isDrawingShape = false;

    private Paint previewPaint = new Paint();

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupDrawing();
        setupPreviewPaint();
        setFocusable(true);
    }

    private void setupDrawing() {
        path = new Path();
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
        updatePaint();
    }

    private void setupPreviewPaint() {
        previewPaint.setColor(Color.GRAY);
        previewPaint.setStyle(Paint.Style.STROKE);
        previewPaint.setStrokeWidth(2);
        previewPaint.setAntiAlias(true);
        previewPaint.setDither(true);
        previewPaint.setPathEffect(new DashPathEffect(new float[]{15, 10}, 0));
    }

    private void updatePaint() {
        paint.setColor(isErasing ? Color.WHITE : currentColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setMaskFilter(null);
        paint.setPathEffect(null);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setShader(null);
        paint.setAlpha(255);

        switch (currentBrushType) {
            case NORMAL:
                paint.setStrokeWidth(brushSize);
                break;
            case MARKER:
                paint.setStrokeWidth(brushSize * 2f);
                paint.setStrokeCap(Paint.Cap.SQUARE);
                paint.setStrokeJoin(Paint.Join.BEVEL);
                break;
            case SPRAY:
                paint.setStrokeWidth(brushSize * 0.5f);
                break;
            case CALLIGRAPHY:
                paint.setStrokeWidth(brushSize);
                paint.setAlpha(100);
                paint.setMaskFilter(new BlurMaskFilter(brushSize / 2, BlurMaskFilter.Blur.NORMAL));
                break;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (canvasBitmap != null) {
            canvasBitmap.recycle();
        }
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        drawCanvas = new Canvas(canvasBitmap);
        drawCanvas.drawColor(Color.WHITE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(canvasBitmap, 0, 0, null);

        if (currentShape == ShapeType.FREEHAND) {
            canvas.drawPath(path, paint);
        }

        if (isDrawingShape && currentShape != ShapeType.FREEHAND) {
            switch (currentShape) {
                case CIRCLE:
                    float radius = (float) Math.hypot(currentX - startX, currentY - startY);
                    canvas.drawCircle(startX, startY, radius, previewPaint);
                    break;
                case RECTANGLE:
                    RectF rect = new RectF(startX, startY, currentX, currentY);
                    canvas.drawRect(rect, previewPaint);
                    break;
                case TRIANGLE:
                    Path trianglePath = new Path();
                    trianglePath.moveTo(startX, startY);
                    trianglePath.lineTo(currentX, currentY);
                    trianglePath.lineTo(startX * 2 - currentX, currentY);
                    trianglePath.close();
                    canvas.drawPath(trianglePath, previewPaint);
                    break;
                case LINE:
                    canvas.drawLine(startX, startY, currentX, currentY, previewPaint);
                    break;
                case STAR:
                    drawStar(canvas, startX, startY, currentX, currentY, previewPaint);
                    break;
            }
        }
    }
    private void drawStar(Canvas canvas, float x1, float y1, float x2, float y2, Paint paint) {
        float radiusOuter = (float) Math.hypot(x2 - x1, y2 - y1);
        float radiusInner = radiusOuter * 0.4f;
        int points = 5;

        Path path = new Path();

        for (int i = 0; i < points * 2; i++) {
            float radius = (i % 2 == 0) ? radiusOuter : radiusInner;
            double angle = Math.PI / points * i - Math.PI / 2;
            float x = x1 + (float) (radius * Math.cos(angle));
            float y = y1 + (float) (radius * Math.sin(angle));

            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.close();
        canvas.drawPath(path, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();


        if ((currentBrushType == BrushType.SPRAY && currentShape != ShapeType.FREEHAND) ||
                (isFillMode && currentShape != ShapeType.FREEHAND)) {
            return false;
        }

        if (isFillMode && event.getAction() == MotionEvent.ACTION_DOWN) {
            performFloodFill((int) x, (int) y);
            invalidate();
            return true;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = x;
                startY = y;
                currentX = x;
                currentY = y;
                isDrawingShape = true;

                if (currentShape == ShapeType.FREEHAND) {
                    path.moveTo(x, y);
                    if (currentBrushType == BrushType.SPRAY) {
                        drawSpray(x, y);
                    }
                }
                invalidate();
                break;

            case MotionEvent.ACTION_MOVE:
                currentX = x;
                currentY = y;
                if (currentShape == ShapeType.FREEHAND) {
                    path.lineTo(x, y);
                    if (currentBrushType == BrushType.SPRAY) {
                        drawSpray(x, y);
                    }
                }
                invalidate();
                break;

            case MotionEvent.ACTION_UP:
                if (isDrawingShape) {
                    if (currentShape != ShapeType.FREEHAND) {
                        drawShape();
                    } else if (currentBrushType != BrushType.SPRAY) {
                        drawCanvas.drawPath(path, paint);
                    }
                    isDrawingShape = false;
                }
                path.reset();
                invalidate();
                break;
        }
        return true;
    }

    private void drawShape() {
        switch (currentShape) {
            case CIRCLE:
                float radius = (float) Math.hypot(currentX - startX, currentY - startY);
                drawCanvas.drawCircle(startX, startY, radius, paint);
                break;
            case RECTANGLE:
                RectF rect = new RectF(startX, startY, currentX, currentY);
                drawCanvas.drawRect(rect, paint);
                break;
            case TRIANGLE:
                Path trianglePath = new Path();
                trianglePath.moveTo(startX, startY);
                trianglePath.lineTo(currentX, currentY);
                trianglePath.lineTo(startX * 2 - currentX, currentY);
                trianglePath.close();
                drawCanvas.drawPath(trianglePath, paint);
                break;
            case LINE:
                drawCanvas.drawLine(startX, startY, currentX, currentY, paint);
                break;
            case STAR:
                drawStar(drawCanvas, startX, startY, currentX, currentY, paint);
        }
    }

    private void drawSpray(float x, float y) {
        int particles = 15 + (int) (brushSize / 2);
        float radius = brushSize * 2;

        for (int i = 0; i < particles; i++) {
            float angle = random.nextFloat() * 2 * (float) Math.PI;
            float distance = random.nextFloat() * radius;
            float px = x + (float) (distance * Math.cos(angle));
            float py = y + (float) (distance * Math.sin(angle));
            drawCanvas.drawCircle(px, py, brushSize / 4, paint);
        }
    }

    private void performFloodFill(int x, int y) {
        if (canvasBitmap == null) return;
        int targetColor = canvasBitmap.getPixel(x, y);
        if (targetColor == currentColor) return;
        floodFill.floodFill(canvasBitmap, new Point(x, y), targetColor, currentColor);
    }

    public Bitmap getBitmap() {
        return canvasBitmap;
    }

    public void setBrushType(BrushType type) {

        if (isErasing || isFillMode || currentShape != ShapeType.FREEHAND) return;
        this.currentBrushType = type;
        updatePaint();
        invalidate();
    }

    public void enableEraser() {
        isErasing = true;
        isFillMode = false;
        updatePaint();
        invalidate();
    }

    public void enableFillMode() {
        isFillMode = true;
        isErasing = false;
        updatePaint();
        invalidate();
    }
    public void disableEraser() {
        if (isErasing) {
            isErasing = false;
            updatePaint();
            invalidate();
        }
    }

    public void disableFillMode() {
        if (isFillMode) {
            isFillMode = false;
            updatePaint();
            invalidate();
        }
    }

    public void clearCanvas() {
        if (drawCanvas != null) {
            drawCanvas.drawColor(Color.WHITE);
            invalidate();
        }
    }

    public void setColor(int color) {
        currentColor = color;
        isErasing = false;
        updatePaint();
        invalidate();
    }

    public void setBrushSize(float size) {
        brushSize = size;
        updatePaint();
        invalidate();
    }

    public void setShapeType(ShapeType shapeType) {
        this.currentShape = shapeType;
        this.isFillMode = false;
        this.isErasing = false;
        this.currentBrushType = BrushType.NORMAL;
        updatePaint();
        invalidate();
    }
    public ShapeType getCurrentShape() {
        return currentShape;
    }

    public String getBase64Drawing() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        canvasBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        byte[] byteArray = outputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }
}
