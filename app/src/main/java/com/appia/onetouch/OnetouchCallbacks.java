package com.appia.onetouch;

import com.appia.onetouch.OnetouchInfo;
import com.appia.onetouch.OnetouchMeasurement;

import java.util.ArrayList;

import no.nordicsemi.android.ble.BleManagerCallbacks;

public interface OnetouchCallbacks extends BleManagerCallbacks {

    /**
     * Called when new measurements are available. This measurements must be stored by the one who
     * implements this interface.
     * @param aMeasurements
     */
    void onMeasurementsReceived(ArrayList<OnetouchMeasurement> aMeasurements);

    /**
     * Called when device information is received.
     * @param aInfo
     */
    void onDeviceInfoReceived(OnetouchInfo aInfo);

    /**
     * Called when an error occurs during the communication.
     * @param aMessage
     */
    void onProtocolError(String aMessage);

}
