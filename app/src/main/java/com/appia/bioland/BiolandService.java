/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.appia.bioland;

import android.app.NotificationChannel;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;


import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import no.nordicsemi.android.ble.BleManager;

import com.appia.bioland.Ble.BleProfileService;
import com.appia.main.BiolandActivity;

import java.util.Timer;
import java.util.TimerTask;
import java.util.ArrayList;

public class BiolandService extends BleProfileService implements BiolandCallbacks {

    private static final String TAG = "BiolandService";

    /* Notifications channel. */
    private static final String CHANNEL_ID = "channel_id";

    /**
     *  Measurement Broadcast!
     */
    public static final String BROADCAST_MEASUREMENT = "com.appia.bioland.uart.BROADCAST_MEASUREMENT";
    public static final String EXTRA_GLUCOSE_LEVEL = "com.appia.bioland.uart.EXTRA_GLUCOSE_LEVEL";

    public static final String BROADCAST_INFORMATION = "com.appia.bioland.uart.BROADCAST_INFORMATION";
    public static final String EXTRA_BATTERY_CAPACITY = "com.appia.bioland.uart.EXTRA_BATTERY_CAPACITY";
    public static final String EXTRA_SERIAL_NUMBER = "com.appia.bioland.uart.EXTRA_SERIAL_NUMBER";


    public static final String BROADCAST_COMM_FAILED = "com.appia.bioland.uart.BROADCAST_COMM_FAILED";
    public static final String EXTRA_ERROR_STRING = "com.appia.bioland.uart.EXTRA_ERROR_STRING";

    //public static final String EXTRA_GLUCOSE_VALUE = "com.appia.bioland.uart.EXTRA_GLUCOSE_VALUE";
    // TODO: Agregar campos EXTRA??

    /**
     * Action send when user press the DISCONNECT button on the notification.
     */
    public final static String ACTION_DISCONNECT = "com.appia.bioland.uart.ACTION_DISCONNECT";
    public final static String ACTION_GET_MEASUREMENTS = "com.appia.bioland.uart.ACTION_GET_MEASUREMENTS";

    /* Notification things...*/
    private final static int NOTIFICATION_ID = 349; // random
    private final static int OPEN_ACTIVITY_REQ = 67; // random
    private final static int DISCONNECT_REQ = 97; // random

    /* Bioland manager. */
    private BiolandManager mManager;

    /* This binder is an interface for the binded activity to operate with the device*/
    public class BiolandBinder extends LocalBinder {
        /**
         * Returns the measurements stored in the manager.
         */
        public ArrayList<BiolandMeasurement> getMeasurements() {
            return mManager.getMeasurements();
        }

        /**
         * Send a request to read new measurements
         */
        public void readMeasurements(){
            if(isConnected()) {
                mManager.readMeasurements();
            }
        }

        /**
         * Send a request to read device information.
         */
        public void readDeviceInfo(){
            if(isConnected()) {
                mManager.readDeviceInfo();
            }
        }
    }

    private final LocalBinder mBinder = new BiolandBinder();

    /**
     *  Callback called by BiolandManager
     */
    public void onCommunicationStarted(){
        // Nothing to do
    }

    /**
     * Called by BiolandManager when all measurements were received.
     */
    public void onMeasurementsRead() {
        final Intent broadcast = new Intent(BROADCAST_MEASUREMENT);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
    }

    /**
     * Called by BiolandManager when device information is received..
     */
    public void onInformationRead() {
        final Intent broadcast = new Intent(BROADCAST_INFORMATION);
        broadcast.putExtra(EXTRA_BATTERY_CAPACITY,mManager.getBatteryCapacity());
        broadcast.putExtra(EXTRA_SERIAL_NUMBER,mManager.getSerialNumber());
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
    }

    /**
     * Called by BiolandManager when an error has occured in the communication with the device.
     */
    public void onCommunicationFailed() {
        final Intent broadcast = new Intent(BROADCAST_COMM_FAILED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
    }


    @Override
    protected LocalBinder getBinder() {
        return mBinder;
    }

    @Override
    protected BleManager<BiolandCallbacks> initializeManager() {
        return mManager = new BiolandManager(this);
    }

    @Override
    protected boolean shouldAutoConnect() {
        return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        /* Receive disconnect action.*/
        registerReceiver(disconnectActionBroadcastReceiver, new IntentFilter(ACTION_DISCONNECT));
        /* Receive tbd actions.*/
        //registerReceiver(intentBroadcastReceiver,new IntentFilter(ACTION_TBD));
    }

    @Override
    public void onServiceStarted(){
        createNotificationChannel();
    }

    @Override
    public void onDestroy() {
        // when user has disconnected from the sensor, we have to cancel the notification that we've created some milliseconds before using unbindService
        stopForegroundService();
        unregisterReceiver(disconnectActionBroadcastReceiver);
        //unregisterReceiver(intentBroadcastReceiver);
        super.onDestroy();
    }

    @Override
    protected void onRebind() {
        stopForegroundService();
    }

    @Override
    protected void onUnbind() {
        startForegroundService();
    }

    @Override
    public void onDeviceConnected(@NonNull final BluetoothDevice device) {
        super.onDeviceConnected(device);

    }

    @Override
    public void onDeviceReady(@NonNull final BluetoothDevice device) {
        super.onDeviceReady(device);

        // If activity is not bounded, service is running in background.
        // Read measurements when device connects!
        if(bound == false) {
            mManager.readMeasurements();
        }
    }

    @Override
    protected boolean stopWhenDisconnected() {
        return false;
    }



    /**
     * Sets the service as a foreground service
     */
    private void startForegroundService() {
        // when the activity closes we need to show the notification that user is connected to the peripheral sensor
        // We start the service as a foreground service as Android 8.0 (Oreo) onwards kills any running background services
        final Notification notification = createNotification(com.appia.bioland.R.string.uart_notification_connected_message, 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(NOTIFICATION_ID, notification);
        } else {
            final NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(NOTIFICATION_ID, notification);
        }
    }

    /**
     * Stops the service as a foreground service
     */
    private void stopForegroundService() {
        // when the activity rebinds to the service, remove the notification and stop the foreground service
        // on devices running Android 8.0 (Oreo) or above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true);
        } else {
            cancelNotification();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            serviceChannel.setDescription(getString(R.string.channel_connected_devices_description));
            serviceChannel.setShowBadge(false);
            serviceChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManager mManager = getSystemService(NotificationManager.class);
            mManager.createNotificationChannel(serviceChannel);
        }
    }

    /**
     * Creates the notification
     *
     * @param messageResId message resource id. The message must have one String parameter,<br />
     *                     f.e. <code>&lt;string name="name"&gt;%s is connected&lt;/string&gt;</code>
     * @param defaults     signals that will be used to notify the user
     */
    @SuppressWarnings("SameParameterValue")
    protected Notification createNotification(final int messageResId, final int defaults) {

        final Intent intent = new Intent(this, BiolandActivity.class);

        final Intent disconnect = new Intent(ACTION_DISCONNECT);
        final PendingIntent disconnectAction = PendingIntent.getBroadcast(this, DISCONNECT_REQ, disconnect, PendingIntent.FLAG_UPDATE_CURRENT);

        // both activities above have launchMode="singleTask" in the AndroidManifest.xml file, so if the task is already running, it will be resumed
        final PendingIntent pendingIntent = PendingIntent.getActivities(this, OPEN_ACTIVITY_REQ, new Intent[]{intent}, PendingIntent.FLAG_UPDATE_CURRENT);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        builder.setContentIntent(pendingIntent);
        builder.setContentTitle(getString(R.string.app_name)).setContentText(getString(messageResId, getDeviceName()));
       // builder.setSmallIcon(R.drawable.ic_stat_notify_uart);
        builder.setShowWhen(defaults != 0).setDefaults(defaults).setAutoCancel(true).setOngoing(true);
        builder.addAction(new NotificationCompat.Action(R.drawable.ic_action_bluetooth, getString(R.string.uart_notification_action_disconnect), disconnectAction));

        return builder.build();
    }

    /**
     * Cancels the existing notification. If there is no active notification this method does nothing
     */
    private void cancelNotification() {
        final NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancel(NOTIFICATION_ID);
    }

    /**
     * This broadcast receiver listens for {@link #ACTION_DISCONNECT} that may be fired by pressing Disconnect action button on the notification.
     */
    private final BroadcastReceiver disconnectActionBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (isConnected())
                getBinder().disconnect();
            else
                stopSelf();
        }
    };

    /**
     * Quizas no hace falta recibir broadcasts!!!
     *  TODO
     */
//    private BroadcastReceiver intentBroadcastReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(final Context context, final Intent intent) {
//
//        }
//    };
}
