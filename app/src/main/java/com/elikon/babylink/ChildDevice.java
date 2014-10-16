package com.elikon.babylink;

import android.app.Activity;
import android.os.Handler;
import android.widget.TextView;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ChildDevice implements Device {
    private NsdHelper nsdHelper;
    private SoundMeter sensor = new SoundMeter();
    private Handler handler;
    private Activity activityContext;

    public ChildDevice(final Activity activityContext, final TextView statusView) {
        this.activityContext = activityContext;

        try {
            sensor.start();
        } catch (Exception e) {
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            String stackTrace = writer.toString();
            Log.appendLog(activityContext, stackTrace);
        }

        handler = new Handler();
        final Runnable r = new Runnable() {
            String oldVisualVolume = "";

            public void run() {
                activityContext.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Get the volume from 0 to 255 in 'int'
                        double volume = 10 * sensor.getTheAmplitude() / 32768;

                        //updateTextView(R.id.volumeLevel, "Volume: " + String.valueOf(volumeToSend));
                        String volumeVisual = "";
                        volumeVisual = "";
                        for (int i = 0; i < volume; i++) {
                            volumeVisual += "|";
                        }
                        if (!volumeVisual.equals(oldVisualVolume) ) {
                            oldVisualVolume = volumeVisual;
                            updateView("Volume: " + String.valueOf(volumeVisual));
                        }
                        handler.postDelayed(this, 250); // amount of delay between every cycle of volume level detection + sending the data out
                    }
                });
            }

        };
        // Is this line necessary? --- YES IT IS, or else the loop never runs
        // this tells Java to run "r"
        handler.postDelayed(r, 250);
        nsdHelper = new NsdHelper(activityContext);
        nsdHelper.initializeNsd();
        nsdHelper.discoverServices();
    }

    private void updateView(String text) {
        TextView logText = (TextView)activityContext.findViewById(R.id.statusViewChild);
        logText.setText(text, TextView.BufferType.EDITABLE);
        nsdHelper.sendMesage(text);
    }


    private String getView() {
        TextView logText = (TextView)activityContext.findViewById(R.id.statusViewChild);
        return logText.getText().toString();
    }

    @Override
    public void tearDown() {
        sensor.stop();
        nsdHelper.stopDiscovery();
    }
}
