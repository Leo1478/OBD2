package com.oak.obd2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

public class MainView extends View {

    public MainView(Context c) {
        super(c);
    }

    public void onDraw(Canvas c) {
        Paint p = new Paint();
        p.setColor(Color.BLACK);
        //c.drawCircle(100,100,100,p);
    }
}
