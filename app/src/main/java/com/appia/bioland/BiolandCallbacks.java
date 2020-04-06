package com.appia.bioland;

import no.nordicsemi.android.ble.BleManagerCallbacks;

import android.bluetooth.BluetoothDevice;
import androidx.annotation.NonNull;

import java.util.ArrayList;

public interface BiolandCallbacks extends BleManagerCallbacks {

    void onCountdownReceived(int aCount);

    /**
     * Called when new measurements are available. This measurements must be stores by the one who
     * implements this interface.
     * @param aMeasurements
     */
    void onMeasurementsReceived(ArrayList<BiolandMeasurement> aMeasurements);

    void onDeviceInfoReceived(BiolandInfo aInfo);

    void onProtocolError(String aMessage);

}
