package com.elikon.babylink;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Log {
    private static DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    public static void appendLog(final Activity context,final String  text)
    {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // dispaly toast here;
                Toast.makeText(context, text, Toast.LENGTH_LONG).show();
            }
        });
        appendLog(text);
    }

    public static void appendLog(final String  text)
    {   android.util.Log.d("BABY_LINK", dateFormat.format(new Date()) + text);
        File logFile = new File("sdcard/babyLinks.log");
        if (!logFile.exists())
        {
            try
            {
                logFile.createNewFile();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try
        {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(dateFormat.format(new Date()) + " - " + text + "\n");
            buf.newLine();
            buf.flush();
            buf.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
