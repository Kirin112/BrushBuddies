package com.brushb.brushbuddies.classes;

import android.view.View;

public abstract class SafeClickListener implements View.OnClickListener {
    private static final long DEFAULT_DELAY = 1000;
    private long lastClickTime = 0;

    public abstract void onSafeClick(View v);

    @Override
    public final void onClick(View v) {
        long now = System.currentTimeMillis();
        if (now - lastClickTime > DEFAULT_DELAY) {
            lastClickTime = now;
            onSafeClick(v);
        }
    }
}
