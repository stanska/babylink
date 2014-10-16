package com.elikon.babylink;


import android.app.Activity;
import android.content.Context;
import android.os.Looper;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ParentDevice implements Device {

    private NsdHelper nsdHelper;
    private Activity activityContext;

    private SocketServerThread socketServerThread;
    public ParentDevice(Context applicationContext, final Activity activityContext, final TextView statusView) {
        this.activityContext = activityContext;

        nsdHelper = new NsdHelper(activityContext);
        nsdHelper.initializeNsd();
        socketServerThread = new SocketServerThread();
        socketServerThread.start();
        nsdHelper.registerService(socketServerThread.getPort());

    }


    @Override
    public void tearDown() {
        nsdHelper.unregisterService();
        socketServerThread.finish();
    }

    private class SocketServerThread extends Thread {
        private int port = 32479;

        private boolean stopped = true;

        public int getPort() {
            return port;
        }
        @Override
        public void run() {
            Looper.prepare();

            Log.appendLog(activityContext, "Parent Server running");
            Socket socket = null;
            DataInputStream dataInputStream = null;
            try {
                Log.appendLog(activityContext, "Creating server socket");
                ServerSocket serverSocket = new ServerSocket(port);
                Log.appendLog(activityContext, "set port number to " + port);
                port = serverSocket.getLocalPort();
                while ( !stopped ) {
                    socket = serverSocket.accept();
                    dataInputStream = new DataInputStream(
                            socket.getInputStream());
                    final String messageFromClient = dataInputStream.readUTF();
                    activityContext.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateView(messageFromClient);
                        }
                    });
                }

            } catch (IOException e) {
                Log.appendLog("socket ioex");
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    try {
                        Log.appendLog("socket close");
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.appendLog("socket close ex");
                    }
                }

                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                        Log.appendLog("data input stream close");
                    } catch (IOException e) {
                        Log.appendLog("data input stream close ex");
                        e.printStackTrace();
                    }
                }
            }

        }
        public void finish() {
            Log.appendLog("finish");
            stopped = true;
        }
    }
    private void updateView(String text) {
        TextView logText = (TextView)activityContext.findViewById(R.id.statusViewParent);
        logText.setText(text, TextView.BufferType.EDITABLE);
    }


}
