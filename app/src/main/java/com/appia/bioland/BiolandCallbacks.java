package com.appia.bioland;

import no.nordicsemi.android.ble.BleManagerCallbacks;

import android.bluetooth.BluetoothDevice;
import androidx.annotation.NonNull;

public interface BiolandCallbacks extends BleManagerCallbacks {

    /**
     * Called when a communication starts with the device.
     */
    void onCommunicationStarted();

    void onMeasurementsRead();

    void onInformationRead();

    void onCommunicationFailed();

}
