package com.appia.bioland.protocols;

import android.util.Log;

import com.appia.bioland.BiolandInfo;
import com.appia.bioland.BiolandMeasurement;
import com.appia.bioland.protocols.ProtocolCallbacks;

import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class BiolandProtocol {

    private final static String TAG = "BiolandProtocol";

    public final static String PROTOCOL_TIMEOUT = "Bioland protocol timeout";
    public final static String PROTOCOL_BAD_CHECKSUM = "Bioland Protocol bad checksum";
    public final static String PROTOCOL_BAD_LENGTH  = "Bioland protocol bad length";

    /**
     * Protocol constructor receives the interface with upper layer.
     * @param aCallbacks
     */
    public BiolandProtocol(ProtocolCallbacks aCallbacks) {
        mCallbacks = aCallbacks;
    }

    /**
     * Sends the request to obtain all the records stored in the device.
     */
    public void startCommunication(){
        /* If there is no communication in progress. */
        if(!mBusy) {
            mBusy=true;
            /* DATA READ PACKET */
            Log.d(TAG, " Data packet sent.");
            mMessage = hexStringToByteArray("5A0A031005020F213BEB");
            mRetriesCount = 1;
            mMeasurements = new ArrayList<>();

            mTimer = new Timer();
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    resendMessage();
                }
            }, 300);
        }
    }

    /**
     * Sends the request to obtain the device information.
     */
    public void requestDeviceInfo(){

        /* If there is no communication in progress. */
        if(!mBusy) {
            mBusy=true;
            /* INFO READ PACKET */
            Log.d(TAG, " Info packet sent.");
            mMessage = hexStringToByteArray("5A0A001005020F213BE8");
            mRetriesCount = 1;

            mTimer = new Timer();
            mTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    resendMessage();
                }
            }, 1000, 300);
        }
    }

    /**
     * Method to handle received raw binary data.
     * @param bytes
     */
    public void onDataReceived(byte[] bytes) {

//		// Build packet from bytes received
//		ProtocolPacket packet = new ProtocolPacket(bytes);
//
//		// Check checksum and packet length
//		if(packet.isValid()==false){
//			// Should retry a predefined number of times and if it keeps failing communicate
//			// upper layer of communication error.
//			mRetries++;
//			if(mRetries==5) {
//				mSendPacketTask.cancel();
//				onProtocolError();
//			}
//			return;
//		}
//		// Packet is valid, reset retries counter.
//		mRetries = 0;

        /* Stop timout. */
        mTimer.cancel();

        //if(packet.type() ==Protocol::INFORMATION_PACKET)
        if(bytes[2]==0){
            /* Stop communication. */
            mBusy = false;

            BiolandInfo deviceInfo = new BiolandInfo();
            deviceInfo.protocolVersion = bytes[3];
            deviceInfo.batteryCapacity = bytes[5];
            deviceInfo.serialNumber = Arrays.copyOfRange(bytes,8,16);
            //productionDate.set(year + 1900, month, 1); Only in protocol V1

            Log.d(TAG, "Info received: Bat="+ deviceInfo.batteryCapacity + "% ProtocolVersion = " + deviceInfo.protocolVersion + " SerialN: " + deviceInfo.serialNumber.toString() );

            // Notify device information has been received
            mCallbacks.onDeviceInfoReceived(deviceInfo);
        }
        //else if(packet.type() ==Protocol::MEASUREMENT_PACKET)
        else if(bytes[2]==3) {
            float glucose= (float)(((bytes[10]&0x000000FF)<<8)+ (bytes[9]&0x000000FF));
            BiolandMeasurement measurement = new BiolandMeasurement(glucose,bytes[3],bytes[4],bytes[5],bytes[6],bytes[7]);

            /* Schedule a new timeout. */
            mTimer = new Timer();
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    resendMessage();
                }
            }, 300);

            /* Add measurement to the array. */
            mMeasurements.add(measurement);
        }
        //else if(packet.type() ==Protocol::END_PACKET)
        else if(bytes[2]==5) {
            Log.d(TAG, "END PACKET RECEIVED!!");
            mBusy = false;

            /* Notify measurements have been received and release the reference to the array. */
            if(mMeasurements.size()>0) {
                mCallbacks.onMeasurementsReceived(mMeasurements);
                mMeasurements = null;
            }
        }
        //else if(packet.type() ==Protocol::HANDSHAKE_PACKET)
        else if(bytes[2]==9) {
            Log.d(TAG, "HANDSHAKE PACKET RECEIVED!!");

        }
    }

    /* Private protocol stuff. */
    private ProtocolCallbacks mCallbacks;
    private ArrayList<BiolandMeasurement> mMeasurements = new ArrayList<>();
    private Timer mTimer;
    private byte[] mMessage;
    private int mRetriesCount;
    private boolean mBusy;

    /**
     *
     */
    private void resendMessage() {
        if(mRetriesCount==5) {
            /* Stop communication. */
            mTimer.cancel();
            mTimer.purge();
            mTimer=null;
            mBusy = false;
            /* Notify upper layer of timeout. */
            mCallbacks.onProtocolError(PROTOCOL_TIMEOUT);
        }
        else {
            mTimer.cancel();
            mTimer = new Timer();
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    resendMessage();
                }
            }, 300);
            mCallbacks.sendData(mMessage);
            mRetriesCount++;
        }
    }

    /**
     *
     * @param s
     * @return
     */
    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}