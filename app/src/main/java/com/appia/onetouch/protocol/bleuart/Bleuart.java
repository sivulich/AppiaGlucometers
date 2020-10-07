package com.appia.onetouch.protocol.bleuart;

import android.util.Log;

import com.appia.bioland.BuildConfig;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 *
 */
public class Bleuart {

    public Bleuart(BleuartCallbacks aCallbacks, int aMaxPacketSize){
        mCallbacks = aCallbacks;
        mState = State.IDLE;
        mMaxPayloadSize = aMaxPacketSize-BLEUART_HEADER_SIZE;
    }

    /**
     * This function sends a packet of bytes to the device.
     */
    public void sendPacket(byte[] aBytes) {
        if (BuildConfig.DEBUG && !(mState == State.IDLE)) {
            throw new AssertionError("Was busy to send packet!");
        }

        mState = State.SENDING;
        /* Compute the number of packets needed in the transaction. */
        mNpackets = (int) Math.ceil(aBytes.length / (double) mMaxPayloadSize);

        mTxData = new ByteArrayInputStream(aBytes);

        buildAndSendFragment(true);
    }

    /**
     * This function should be called by the upper layer when a bluetooth packet is received
     */
    public void onDataReceived(byte[] aBytes){
        switch (mState){
            case IDLE:
                if(headerIs(aBytes[0],HEADER_FIRST_PACKET)) {

                    mNpackets = aBytes[0]&0x0F;

                    Log.d(TAG, "Receiving 1 of " + mNpackets);

                    mRxData = new ByteArrayOutputStream();

                    handleDataReceived(aBytes);
                }
                break;
            case SENDING:
                if(aBytes.length==1 && headerIs(aBytes[0],HEADER_ACK_PACKET)){
                    // Acknowledge packet
                    int nAck = aBytes[0]&0x0F;

                    if(nAck == mNpackets){
                        mNpackets--;
                        if(mNpackets==0){
                            mTxData = null;
                            mState=State.IDLE;
                            Log.d(TAG,"SENDING -> IDLE.");
                        }
                        else{
                            int nBytesToSend = Math.min(mMaxPayloadSize, BLEUART_HEADER_SIZE + mTxData.available());
                            byte[] bytesToSend = new byte[nBytesToSend];
                            bytesToSend[0] = (byte)(0x40|(0x0F&mNpackets));
                            mTxData.read(bytesToSend,BLEUART_HEADER_SIZE,nBytesToSend-BLEUART_HEADER_SIZE);
                            mCallbacks.sendData(bytesToSend);
                        }
                    }
                    else{
                        Log.e(TAG,"Wrong ACK number!. Expecting " + mNpackets + " but " + nAck + " received.");
                    }
                }
                else{
                    Log.e(TAG,"Expecting ACK but received: " + aBytes.toString());
                }
                break;
            case RECEIVING:
                if(headerIs(aBytes[0],HEADER_FRAG_PACKET)) {
                    int remainingPackets = aBytes[0]&0x0F;
                    if(remainingPackets==mNpackets){
                       handleDataReceived(aBytes);
                    }
                    else{
                        Log.e(TAG,"Wrong packet number!. Expecting " + mNpackets + " but " + remainingPackets + " received.");
                    }
                }
                else{
                    Log.e(TAG,"Wrong header code!. Expecting " + 0x40 + " but " + (aBytes[0]&0xF0) + " received.");
                }
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + mState);
        }
    }

    private void buildAndSendFragment(boolean aFirstPacket) {
        int nBytesToSend = Math.min(mMaxPayloadSize, BLEUART_HEADER_SIZE + mTxData.available());
        byte[] bytesToSend = new byte[nBytesToSend];
        bytesToSend[0] =  (byte)(0x0F&mNpackets);
        bytesToSend[0] |= aFirstPacket ? (byte)0x00 : (byte)0x40;
        mTxData.read(bytesToSend,BLEUART_HEADER_SIZE,nBytesToSend-BLEUART_HEADER_SIZE);
        mCallbacks.sendData(bytesToSend);
    }

    private void handleDataReceived(byte[] aBytes){
        mRxData.write(aBytes,1,aBytes.length-1);

        byte[] bytesToSend = new byte[1];
        bytesToSend[0] = (byte)(0x80|(0x0F&mNpackets));
        mCallbacks.sendData(bytesToSend);

        mNpackets--;

        if(mNpackets>0){
            Log.d(TAG, mNpackets + " remaining.");
            mState = State.RECEIVING;
        }
        else{
            Log.d(TAG, mRxData.size() + " bytes received");
            mTxData = null;
            mState = State.IDLE;
            mCallbacks.onPacketReceived(mRxData.toByteArray());
        }
    }

    /* Check if the header of the packet is the specified one. */
    private boolean headerIs(byte aHeader, byte aHeaderType){
        return (aHeader&(byte)0xF0)==aHeaderType;
    }

    private final static byte HEADER_FIRST_PACKET = (byte)0x00;
    private final static byte HEADER_FRAG_PACKET = (byte)0x40;
    private final static byte HEADER_ACK_PACKET = (byte)0x80;

    private final static String TAG = "BleuartProtocol";
    private final static int BLEUART_HEADER_SIZE = 1;
    /* Interface with upper layer. */
    private BleuartCallbacks mCallbacks;
    /* Stream of data to read when sending. */
    private ByteArrayInputStream mTxData;
    /* Stream of data where to write when receiving. */
    private ByteArrayOutputStream mRxData;
    /* Packet counter for TX/RX */
    private int mNpackets;
    /* Maximum amount of bytes sent in one packet */
    private int mMaxPayloadSize;

    private enum State {IDLE,SENDING,RECEIVING};
    private State mState;
}
