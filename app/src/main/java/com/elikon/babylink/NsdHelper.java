/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.elikon.babylink;

import android.app.Activity;
import android.content.Context;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.text.format.Formatter;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;

public class NsdHelper {

    Activity mContext;

    NsdManager mNsdManager;
    NsdManager.ResolveListener mResolveListener;
    NsdManager.DiscoveryListener mDiscoveryListener;
    NsdManager.RegistrationListener mRegistrationListener;

    boolean registeredService = false;

    public static final String SERVICE_TYPE = "_http._tcp.";

    public static final String TAG = "NsdHelper";
    public String mServiceName = "BabyLink";

    NsdServiceInfo mService;
    DataOutputStream dataOutputStream;
    InetAddress host = null;
    int port = 0;
    private Socket socket;

    public NsdHelper(Activity context) {
        mContext = context;
        mNsdManager = (NsdManager) context.getSystemService(mContext.NSD_SERVICE);
        this.dataOutputStream = null;
    }

    public void initializeNsd() {
        initializeResolveListener();
        initializeDiscoveryListener();
        initializeRegistrationListener();
    }

    class SocketTextSend extends AsyncTask<String, Void, String> {
        Exception exception;



        protected String doInBackground(String... text) {
            try {
                Socket socket = new Socket(host, port);
                DataOutputStream dataOutputStream = new DataOutputStream(
                        socket.getOutputStream());
                dataOutputStream.writeUTF(text[0]);
            } catch (Exception e) {
                this.exception = e;
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
            return "";
        }
    }


    class SocketAudioSend extends AsyncTask<byte[], Void, String> {
        Exception exception;


        protected String doInBackground(byte[]... packets) {
            try {
                DatagramSocket socket = new DatagramSocket();
                socket.send(new DatagramPacket(packets[0], packets[0].length, host, port));
            } catch (Exception e) {
                this.exception = e;
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return "";
        }
    }

    public void sendMesage(String text) {
        if (host != null) {
            Log.appendLog("sending" + text + "to " + host+ port);
            new SocketTextSend().execute(text);
        }
    }

    public void sendAudioMessage(byte[] audioMessage) {
        if (host != null) {
            new SocketAudioSend().execute(audioMessage);
        }
    }

    private String getLocalIpAddress() {
        WifiManager wm = (WifiManager) mContext.getSystemService(mContext.WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        return ip;
    }

    public void initializeRegistrationListener() {
        mRegistrationListener = new NsdManager.RegistrationListener() {

            @Override
            public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
                mServiceName = NsdServiceInfo.getServiceName();
                Log.appendLog(mContext, "service registered" + mServiceName + getLocalIpAddress() + NsdServiceInfo.getServiceName() + NsdServiceInfo.getServiceType() + NsdServiceInfo.getHost() + NsdServiceInfo.getPort());
                registeredService = true;
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo arg0, int arg1) {
                Log.appendLog(mContext, "service failed");
                registeredService = false;
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0) {
                Log.appendLog(mContext, "service unregistered");
                registeredService = false;
            }


            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.appendLog(mContext, "service registration failed");
                registeredService = true;

            }


        };
    }

    public void initializeDiscoveryListener() {
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            @Override
            public void onDiscoveryStarted(String regType) {
                Log.appendLog(mContext, "Service discovery started. Registration type " + regType);
            }

            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {
                Log.appendLog(mContext, "Service discovery success" + serviceInfo + serviceInfo.getServiceType() + serviceInfo.getServiceName() + serviceInfo.getHost());
                if (!serviceInfo.getServiceType().equals(SERVICE_TYPE)) {
                    Log.appendLog(mContext, "Unknown Service Type: " + serviceInfo.getServiceType());
                } else if (serviceInfo.getServiceName().contains(mServiceName)) {
                    Log.appendLog(mContext, "Going to resolve" + mServiceName);
                    mNsdManager.resolveService(serviceInfo, mResolveListener);
                } else {
                    Log.appendLog(mContext, "Resolve not started for " + serviceInfo.getServiceName() + " different than " + mServiceName);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                Log.appendLog(mContext, "service Lost" + serviceInfo + serviceInfo.getServiceType() + serviceInfo.getServiceName() + serviceInfo.getHost());
                if (mService == serviceInfo) {
                    mService = null;
                }
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.appendLog(mContext, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.appendLog(mContext, "Discovery failed: Error code:" + errorCode + serviceType);
                mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.appendLog(mContext, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }
        };
    }

    public void initializeResolveListener() {
        mResolveListener = new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.appendLog(mContext, "Resolve failed" + errorCode + serviceInfo + serviceInfo.getServiceType() + serviceInfo.getServiceName() + serviceInfo.getHost());
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.appendLog(mContext, "Resolve Succeeded. " + "local:" + getLocalIpAddress() + "remote" + serviceInfo + serviceInfo.getServiceType() + serviceInfo.getServiceName() + serviceInfo.getHost());
                mService = serviceInfo;

                if (serviceInfo.getHost() == null) {
                    Log.appendLog(mContext, "Host Address is null");
                    return;
                }

                host = serviceInfo.getHost();
                if (host.getHostAddress().equals(getLocalIpAddress())) {
                    Log.appendLog(mContext, "Same machine");
                    return;
                } else {
                    Log.appendLog(mContext, host + "different than " + getLocalIpAddress());
                }
                port = serviceInfo.getPort();
                Log.appendLog(mContext, "creating client socket");
                mContext.runOnUiThread(
                        new Runnable() {

                    @Override
                    public void run() {
                        try {
                            String fileName = Environment.getExternalStorageDirectory().getAbsolutePath();
                            fileName += "/audiorecordtest.3gp";

                            MediaRecorder recorder = new MediaRecorder();
                            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                            recorder.setOutputFile(fileName);
                            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                            try {
                                recorder.prepare();
                            } catch (IOException e) {
                                Log.appendLog(mContext, "prepare() failed");
                            }
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            recorder.start();
                            recorder.stop();
                            recorder.release();
                            recorder = null;
                        } catch(Exception e) {
                            Log.appendLog(mContext, e.getMessage());
                        }
                    }
                });
            }
        };
    }


    public void registerService(int port) {
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setPort(port);
        serviceInfo.setServiceName(mServiceName);
        serviceInfo.setServiceType(SERVICE_TYPE);
        Log.appendLog(mContext, "registerService " + mServiceName + getLocalIpAddress() + port);
        mNsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);

    }

    public void discoverServices() {
        mNsdManager.discoverServices(
                SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    public void stopDiscovery() {
        mNsdManager.stopServiceDiscovery(mDiscoveryListener);
    }

    public NsdServiceInfo getChosenServiceInfo() {
        return mService;
    }

    public void unregisterService() {
        if (registeredService) {
            mNsdManager.unregisterService(mRegistrationListener);
        }
    }
}

