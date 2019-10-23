package com.oak.obd2;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toolbar;

import java.io.IOException;
import java.util.Random;

import de.nitri.gauge.Gauge;
import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout mDrawerLayout;
    private Bluetooth mBluetooth = null;
    private Gauge mGauge;
    private Obd2 mObd2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        android.support.v7.widget.Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        mGauge = findViewById(R.id.gauge1);
        mGauge.setValue(120);
        mBluetooth = new Bluetooth();
        try {
            mBluetooth.connect();
        } catch (IOException e) {
        }
        mObd2 = new Obd2(mBluetooth);
        //mObd2.command(obd2CommandEnum.RESET);
        mObd2.command(obd2CommandEnum.ECHO_OFF);
        mObd2.command(obd2CommandEnum.VERSION);
        mObd2.command(obd2CommandEnum.MAX_TIMEOUT);
        mObd2.command(obd2CommandEnum.SELECT_PROTOCOL);
        mGauge.setValue(mObd2.rpm()/100);
        runRPMThread();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = new MenuInflater(this);
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    private void runRPMThread() {

        new Thread() {
            public void run() {
                while (true) {
                    try {
                        runOnUiThread(new Runnable() {

                            public void run() {
                                int i = new Random().nextInt(50);
                                //int i = mObd2.rpm()/100;
                                mGauge.moveToValue(i);
                            }
                        });
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

}
