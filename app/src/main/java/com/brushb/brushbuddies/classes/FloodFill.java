package com.brushb.brushbuddies.classes;

import android.graphics.Bitmap;
import android.graphics.Point;

import java.util.ArrayDeque;
import java.util.Arrays;

public class FloodFill {
    public void floodFill(Bitmap image, Point start, int target, int replacement) {
        if (image == null || start == null) return;

        final int w = image.getWidth();
        final int h = image.getHeight();
        final int x = Math.max(0, Math.min(start.x, w - 1));
        final int y = Math.max(0, Math.min(start.y, h - 1));

        int[] pixels = new int[w * h];
        image.getPixels(pixels, 0, w, 0, 0, w, h);

        if (pixels[y * w + x] != target || target == replacement) return;

        ArrayDeque<Integer> stack = new ArrayDeque<>(w * h / 4);
        stack.push(y * w + x);

        while (!stack.isEmpty()) {
            int pos = stack.pop();
            int px = pos % w;
            int py = pos / w;
            if (pixels[pos] != target) continue;
            int left = px;
            while (left >= 0 && pixels[py * w + left] == target) left--;
            left++;

            int right = px;
            while (right < w && pixels[py * w + right] == target) right++;
            right--;
            Arrays.fill(pixels, py * w + left, py * w + right + 1, replacement);
            for (int i = left; i <= right; i++) {
                if (py > 0 && pixels[(py - 1) * w + i] == target) stack.push((py - 1) * w + i);
                if (py < h - 1 && pixels[(py + 1) * w + i] == target) stack.push((py + 1) * w + i);
            }
        }
        image.setPixels(pixels, 0, w, 0, 0, w, h);
    }
}