package com.appia.bioland;

import no.nordicsemi.android.ble.BleManagerCallbacks;

import android.bluetooth.BluetoothDevice;
import androidx.annotation.NonNull;

public interface BiolandCallbacks extends BleManagerCallbacks {

    void onMeasurementsReceived();

    void onDeviceInfoReceived();

    void onProtocolError(String aMessage);

}
