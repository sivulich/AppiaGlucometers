package com.appia.onetouch.protocol.bleuart;


import java.util.ArrayList;

/**
 * This interface is called by the bleuart protocol and must be implemented by the class which uses
 * the protocol.
 */
public interface BleuartCallbacks {

    void sendData(byte[] aBytes);
    /**
     *
     * @param aBytes
     */
    void onPacketReceived(byte[] aBytes);
}
