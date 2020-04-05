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
// TODO
import android.media.AudioAttributes;
import android.provider.Settings;

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
    public static final String BROADCAST_MEASUREMENT = "com.appia.bioland.BROADCAST_MEASUREMENT";
    public static final String EXTRA_GLUCOSE_LEVEL = "com.appia.bioland.EXTRA_GLUCOSE_LEVEL";

    public static final String BROADCAST_INFORMATION = "com.appia.bioland.BROADCAST_INFORMATION";
    public static final String EXTRA_BATTERY_CAPACITY = "com.appia.bioland.EXTRA_BATTERY_CAPACITY";
    public static final String EXTRA_SERIAL_NUMBER = "com.appia.bioland.EXTRA_SERIAL_NUMBER";


    public static final String BROADCAST_COMM_FAILED = "com.appia.bioland.BROADCAST_COMM_FAILED";
    public static final String EXTRA_ERROR_MSG = "com.appia.bioland.EXTRA_ERROR_MSG";

    /**
     * Action send when user press the DISCONNECT button on the notification.
     */
    public final static String ACTION_DISCONNECT = "com.appia.bioland.uart.ACTION_DISCONNECT";

    /* Notification things...*/
    private final static int NOTIFICATION_ID = 349; // random
    private final static int OPEN_ACTIVITY_REQ = 67; // random
    private final static int DISCONNECT_REQ = 97; // random

    /* Bioland manager. */
    private BiolandManager mManager;

    /* This binder is an interface for the binded activity to operate with the device. */
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
        public void requestMeasurements(){
            if(isConnected()) {
                mManager.requestMeasurements();
            }
        }

        /**
         * Send a request to read device information.
         */
        public void requestDeviceInfo(){
            if(isConnected()) {
                mManager.requestDeviceInfo(); // Todo:
            }
        }
    }

    private final LocalBinder mBinder = new BiolandBinder();

    /**
     * Called by BiolandManager when all measurements were received.
     */
    public void onMeasurementsReceived() {
        final Intent broadcast = new Intent(BROADCAST_MEASUREMENT);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);

        if(!bound) {
            ArrayList<BiolandMeasurement> measurements = mManager.getMeasurements();
            createNotification(R.string.notification_new_measurements_message);
        }
    }

    /**
     * Called by BiolandManager when device information is received..
     */
    public void onDeviceInfoReceived() {
        final Intent broadcast = new Intent(BROADCAST_INFORMATION);
        broadcast.putExtra(EXTRA_BATTERY_CAPACITY,mManager.getBatteryCapacity());
        broadcast.putExtra(EXTRA_SERIAL_NUMBER,mManager.getSerialNumber());
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
    }

    /**
     * Called by BiolandManager when an error has occured in the communication with the device.
     */
    public void onProtocolError(String aMessage) {
        final Intent broadcast = new Intent(BROADCAST_COMM_FAILED);
        broadcast.putExtra(EXTRA_ERROR_MSG,aMessage);
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
        Log.d(TAG,"Device connected");
        if(!bound) {
            updateNotification(R.string.notification_connected_message);
        }
    }
    @Override
    public void onLinkLossOccurred(@NonNull final BluetoothDevice device) {
       super.onLinkLossOccurred(device);

        Log.d(TAG,"Link loss ocurred");
        if(!bound) {
            updateNotification(R.string.notification_disconnected_message);
        }
    }

    @Override
    public void onDeviceDisconnected(@NonNull final BluetoothDevice device) {
        super.onDeviceDisconnected(device);
        Log.d(TAG,"Device disconnected");
        if(!bound) {
            updateNotification(R.string.notification_disconnected_message);
        }
    }


    @Override
    public void onDeviceReady(@NonNull final BluetoothDevice device) {
        super.onDeviceReady(device);

        // Always read measurements when device connects!
        mManager.requestMeasurements();

    }

    @Override
    protected void onBluetoothDisabled(){

    }

    @Override
    protected void onBluetoothEnabled(){
        super.onBluetoothEnabled();

        Log.d(TAG,"onBluetoothEnabled");


        BluetoothDevice device =  getBluetoothDevice();

        if (device != null && !isConnected()) {
            Log.d(TAG,"Reconnecting...");
            mManager.connect(getBluetoothDevice()).enqueue();
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
        final Notification notification = createNotification(R.string.notification_connected_message);
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
                    "Bioland Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            serviceChannel.setDescription(getString(R.string.channel_connected_devices_description));
            serviceChannel.setShowBadge(false);
            serviceChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            serviceChannel.enableVibration(true);
            serviceChannel.setSound(Settings.System.DEFAULT_NOTIFICATION_URI,new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION).build());

            NotificationManager mManager = getSystemService(NotificationManager.class);
            mManager.createNotificationChannel(serviceChannel);
        }
    }


    protected void updateNotification(final int messageResId) {
        Notification notification = createNotification(messageResId);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(NOTIFICATION_ID, notification);
    }

    /**
     * Creates the notification
     *
     * @param messageResId message resource id. The message must have one String parameter,<br />
     *                     f.e. <code>&lt;string name="name"&gt;%s is connected&lt;/string&gt;</code>

     */
    @SuppressWarnings("SameParameterValue")
    protected Notification createNotification(final int messageResId) {

        final Intent intent = new Intent(this, BiolandActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // both activities above have launchMode="singleTask" in the AndroidManifest.xml file, so if the task is already running, it will be resumed
        final PendingIntent pendingIntent = PendingIntent.getActivity(this, OPEN_ACTIVITY_REQ, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentIntent(pendingIntent)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(messageResId, getDeviceName()))
                .setSmallIcon(R.drawable.ic_stat_notify_cgms)
                .setShowWhen(true)
                .setAutoCancel(true)
                .setOngoing(true);

        if(isConnected()){
            final Intent disconnect = new Intent(ACTION_DISCONNECT);
            final PendingIntent disconnectAction = PendingIntent.getBroadcast(this, DISCONNECT_REQ, disconnect, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(new NotificationCompat.Action(R.drawable.ic_action_bluetooth, getString(R.string.notification_action_disconnect), disconnectAction));
        }

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
