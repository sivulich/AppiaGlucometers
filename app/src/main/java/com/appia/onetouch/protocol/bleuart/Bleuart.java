package com.appia.onetouch.protocol.bleuart;

import com.appia.bioland.BuildConfig;
import com.appia.onetouch.protocol.ProtocolCallbacks;

import java.util.Timer;

public class Bleuart {

    private final static String TAG = "BleuartProtocol";

    // Abstracts serial communication
    private BleuartCallbacks mCallbacks;

    // All protocols have the following states.
    public enum State {IDLE,SENDING,RECEIVING};
    private State mState;

    // This class abstracts the protocol from the User
    public Bleuart(BleuartCallbacks aCallbacks){
        mCallbacks = aCallbacks;
        mState = State.IDLE;
    }

    // This function should be called when a bluetooth packet is received
    public void onDataReceived(byte[] aBytes){
        switch (mState){
            case IDLE:
                // TODO
                mState = State.RECEIVING;
                break;
            case SENDING:
                // Process acknowledge
                // TODO
                mState = State.IDLE;
                mState = State.SENDING;
                break;
            case RECEIVING:
                // TODO
                mState = State.IDLE;
                mState = State.RECEIVING;
                mCallbacks.onPacketReceived(aBytes);
                break;
        }
    }

    // This function sends the packet
    public void sendPacket(byte[] bytes) {
        if (BuildConfig.DEBUG && !(mState == State.IDLE)) {
            throw new AssertionError("Was busy to send packet!");
        }


        // TODO: Fragmentar paquete
        mState = State.SENDING;
        mCallbacks.sendData(bytes);
    }
}
