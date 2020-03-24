package com.appia.bioland;


import android.bluetooth.BluetoothDevice;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.ParcelUuid;
import android.util.Log;
import android.app.PendingIntent;
import android.content.Context;

import androidx.annotation.NonNull;

import com.appia.common.GlucometerCallbacks;
import com.appia.common.GlucoseMeasurement;


import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.BleManagerCallbacks;
import no.nordicsemi.android.ble.callback.DataReceivedCallback;
import no.nordicsemi.android.ble.data.Data;
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;

import no.nordicsemi.android.support.v18.scanner.ScanCallback;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.ArrayList;
import java.util.UUID;

/**
 *
 */
public class BiolandGlucometer extends BleManager<BleManagerCallbacks> implements BleManagerCallbacks{

    ///////////////////////////////////////////////////////////////////////////////////////////////
    //                                          Public
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Create a BiolandGlucometer
     * @param aContext      application context
     * @param aCallbacks    glucometer callbacks
     */
    public BiolandGlucometer(Context aContext, GlucometerCallbacks aCallbacks) {

        super(aContext);
        super.setGattCallbacks(this);

        // Store application callbacks
        mCallbacks = aCallbacks;

        // Finally start scanning for devices
        this.startScan();
    }

    // Todo: mover el escaneo al manager??
    public void startScan(){
        Log.d(TAG,"StartScan()");

//        mActivity.registerReceiver(mScanBroadcastReceiver, new IntentFilter("com.appia.bioland.SCAN_MATCH"));
//        Intent intent = new Intent(mActivity, BroadcastReceiver.class); // explicite intent
//        intent.setAction("com.appia.bioland.SCAN_MATCH");
//        mPendingIntent = PendingIntent.getBroadcast(mActivity, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .setReportDelay(0)
                .setUseHardwareBatchingIfSupported(true)
                .build();
        List<ScanFilter> filters = new ArrayList<>();

        filters.add(new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString("00001000-0000-1000-8000-00805f9b34fb")).build());
        BluetoothLeScannerCompat.getScanner().startScan(filters, settings, mScanCallback);
//        mScanner.startScan(filters, settings, mActivity, mPendingIntent);
    }

    public void stopScan(){
        Log.d(TAG,"StopScan()");

        BluetoothLeScannerCompat.getScanner().stopScan(mScanCallback);
        // mScanner.stopScan(mActivity,mPendingIntent);
    }

    @Override
    public void log(final int priority, @NonNull final String message) {
        Log.println(priority, "BleManager", message);
    }

    @Override
    protected BleManagerGattCallback getGattCallback() {
        return new BiolandGlucometerGattCallback();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////
    //                                   Ble Manager Callbacks                                   //
    ///////////////////////////////////////////////////////////////////////////////////////////////
    // TODO: Ver si los implementa esta clase, o la Main Activity :/
    public void onDeviceConnecting(@NonNull final BluetoothDevice device) {};

    public void onDeviceConnected(@NonNull final BluetoothDevice device) {};

    public void onDeviceDisconnecting(@NonNull final BluetoothDevice device) {};

    public void onDeviceDisconnected(@NonNull final BluetoothDevice device) {};

    public void onLinkLossOccurred(@NonNull final BluetoothDevice device) {};

    public void onServicesDiscovered(@NonNull final BluetoothDevice device, final boolean optionalServicesFound) {};

    public void onDeviceReady(@NonNull final BluetoothDevice device) {};

    public void onBondingRequired(@NonNull final BluetoothDevice device) {};

    public void onBonded(@NonNull final BluetoothDevice device) {};

    public void onBondingFailed(@NonNull final BluetoothDevice device) {};

    public void onError(@NonNull final BluetoothDevice device,
                        @NonNull final String message, final int errorCode) {};

    public void onDeviceNotSupported(@NonNull final BluetoothDevice device) {};

    ///////////////////////////////////////////////////////////////////////////////////////////////
    //                                          Private                                          //
    ///////////////////////////////////////////////////////////////////////////////////////////////
    private final static UUID SERVICE_UUID  = UUID.fromString("00001000-0000-1000-8000-00805f9b34fb");
    private final static UUID RX_UUID       = UUID.fromString("00001001-0000-1000-8000-00805f9b34fb");
    private final static UUID TX_UUID       = UUID.fromString("00001002-0000-1000-8000-00805f9b34fb");
    private BluetoothGattCharacteristic mRxCharacteristic, mTxCharacteristic;
    private PendingIntent mPendingIntent;
    private static final String TAG = "BiolandGlucometer";
    private GlucometerCallbacks mCallbacks;
    private final Queue<GlucoseMeasurement> mMeasurementQueue = new LinkedList<>();
    private BluetoothDevice mBleDevice;

    private ScanCallback mScanCallback = new ScanCallback(){
        @Override
        public void onScanResult(int aCallbackType, ScanResult aResult) {
            Log.d("BiolandScanCallback","Device Name: " + aResult.getDevice().getName() + " rssi: " + aResult.getRssi() + "\n");
            // TODO: Asegurarse que sea un Bioland, ya sea con un mejor filtro o viendo otra cosa

            // Call application callback
            mCallbacks.onScanResult(aCallbackType,aResult);

            // Todo: Esto esta para probar, queda aca?
            stopScan();

            connect(aResult.getDevice())
                    .timeout(100000)
                    .retry(3, 100)
                    .done(device -> Log.i(TAG, "Device connected!"))
                    .fail((device,state) -> Log.i(TAG, "Device connect failed!"))
                    .enqueue();
        }

        @Override
        public void onBatchScanResults(@NonNull final List<ScanResult> results) {
        }

        @Override
        public void onScanFailed(final int errorCode) {
        }
    };

//    @Override
//    public void onScanResult(int callbackType, ScanResult result) {
//        Log.d("BiolandScanCallback","Device Name: " + result.getDevice().getName() + " rssi: " + result.getRssi() + "\n");
//    }

//    private BroadcastReceiver mScanBroadcastReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            Log.d(TAG,"ScanMatchReceiver: onReceive()");
//            int bleCallbackType = intent.getIntExtra(BluetoothLeScannerCompat.EXTRA_CALLBACK_TYPE, -1);
//            if (bleCallbackType != -1) {
//                Log.d(TAG, "Passive background scan callback type: "+bleCallbackType);
//                ArrayList<ScanResult> scanResults = intent.getParcelableArrayListExtra(
//                        BluetoothLeScannerCompat.EXTRA_LIST_SCAN_RESULT);
//
//                // Do something with your ScanResult list here.
//                // These contain the data of your matching BLE advertising packets
//                for (ScanResult result: scanResults) {
//                    Log.d("BiolandScanCallback", "Device Name: " + result.getDevice().getName() + " rssi: " + result.getRssi() + "\n");
//                }
//            }
//        }
//    };



    ///////////////////////////////////////////////////////////////////////////////////
    // GATT Callbacks
    /////////////////////////////////////////////////////////////////////////////////////
    private abstract class RxHandler implements DataReceivedCallback {
        public void onDataReceived(@NonNull final BluetoothDevice device, @NonNull final Data data) {
        }
    }

    /**
     * BluetoothGatt callbacks object.
     */
    private class BiolandGlucometerGattCallback extends BleManagerGattCallback{
        @Override
        protected void initialize() {
            setNotificationCallback(mTxCharacteristic).with(new RxHandler() {
                @Override
                public void onDataReceived(@NonNull final BluetoothDevice device, @NonNull final Data data) {
                    Log.d(TAG,"Tx notification!");
                }
            });
            readCharacteristic(mTxCharacteristic).with(new RxHandler() {
                @Override
                public void onDataReceived(@NonNull final BluetoothDevice device, @NonNull final Data data) {
                    Log.d(TAG,"Tx data received!");
                }
            }).enqueue();
            enableNotifications(mTxCharacteristic).enqueue();
        }


        @Override
        public boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {
            final BluetoothGattService service = gatt.getService(SERVICE_UUID);
            if (service != null) {
                mTxCharacteristic = service.getCharacteristic(TX_UUID);
                mRxCharacteristic = service.getCharacteristic(RX_UUID);
            }
            // Todo: Validate properties
            return mTxCharacteristic != null && mRxCharacteristic != null;
        }

        @Override
        protected void onDeviceDisconnected() {
            Log.d(TAG,"Device Disconected!\n");
            mTxCharacteristic = null;
            mRxCharacteristic = null;
        }
    };
}