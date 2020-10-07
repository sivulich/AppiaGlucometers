package com.appia.onetouch.protocol;

import com.appia.onetouch.OnetouchMeasurement;

import java.util.ArrayList;

/**
 * This interface is called by the protocol and must be implemented by the class which manages the
 * device and uses the protocol.
 */
public interface ProtocolCallbacks {

    /**
     *
     * @param bytes
     */
    void sendData(final byte[] bytes);

    /**
     *
     * @param aMeasurements
     */
    void onMeasurementsReceived(ArrayList<OnetouchMeasurement> aMeasurements);

    /**
     *
     */
    void onProtocolError(String aMessage);
}
