package com.appia.bioland.protocols;


import com.appia.bioland.BiolandInfo;
import com.appia.bioland.BiolandMeasurement;

import java.util.ArrayList;

/**
 * This interface is called by the protocol and must be implemented by the class which manages the
 * device and uses the protocol.
 */
public interface ProtocolCallbacks  {

    /**
     *
     * @param bytes
     */
    void sendData(final byte[] bytes);

    /**
     *
     * @param aValue
     */
    void onCountdownReceived(int aValue);

    /**
     *
     * @param aMeasurements
     */
    void onMeasurementsReceived(ArrayList<BiolandMeasurement> aMeasurements);

    /**
     *
     * @param aInfo
     */
    void onDeviceInfoReceived(BiolandInfo aInfo);

    /**
     *
     */
    void onProtocolError(String aMessage);
}
