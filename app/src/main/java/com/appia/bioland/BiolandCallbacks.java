package com.appia.bioland;

import no.nordicsemi.android.ble.BleManagerCallbacks;

import android.bluetooth.BluetoothDevice;
import androidx.annotation.NonNull;

import java.util.ArrayList;

public interface BiolandCallbacks extends BleManagerCallbacks {

    /**
     * Called each time the device updates the countdown while performing a measurement.
     * @param aCount
     */
    void onCountdownReceived(int aCount);

    /**
     * Called when new measurements are available. This measurements must be stored by the one who
     * implements this interface.
     * @param aMeasurements
     */
    void onMeasurementsReceived(ArrayList<BiolandMeasurement> aMeasurements);

    /**
     * Called when device information is received.
     * @param aInfo
     */
    void onDeviceInfoReceived(BiolandInfo aInfo);

    /**
     * Called when an error occurs during the communication.
     * @param aMessage
     */
    void onProtocolError(String aMessage);

}
