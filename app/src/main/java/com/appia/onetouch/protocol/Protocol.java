package com.appia.onetouch.protocol;

import android.util.Log;

import com.appia.onetouch.OnetouchMeasurement;
import com.appia.onetouch.protocol.bleuart.Bleuart;
import com.appia.onetouch.protocol.bleuart.BleuartCallbacks;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Timer;

public class Protocol implements BleuartCallbacks {

    private final static String TAG = "OneTouchProtocol";

    // Abstracts serial communication
    private ProtocolCallbacks protocolCallbacks;

    private final static int PACKET_INITIAL_BYTE = 1; // Always 0x02
    private final static int PACKET_LENGTH_BYTES = 2; // 16 bit packet length (little endian)
    private final static int PACKET_PAYLOAD_BEGIN_BYTE_A = 1; // Always 0x04 before payload
    private final static int PACKET_PAYLOAD_BEGIN_BYTE_B = 1; // Always 0x06 before payload when receiving
    private final static int PACKET_PAYLOAD_END_BYTE = 1; // Always 0x03 after payload
    private final static int PACKET_CRC_BYTES = 2; // 16 bit checksum (little endian)

    private final static int PACKET_PAYLOAD_BEGIN = PACKET_INITIAL_BYTE +
                                                    PACKET_LENGTH_BYTES +
                                                    PACKET_PAYLOAD_BEGIN_BYTE_A +
                                                    PACKET_PAYLOAD_BEGIN_BYTE_B;

    private final static int PROTOCOL_OVERHEAD =    PACKET_INITIAL_BYTE +
                                                    PACKET_LENGTH_BYTES +
                                                    PACKET_PAYLOAD_BEGIN_BYTE_A +
                                                    PACKET_PAYLOAD_BEGIN_BYTE_B +
                                                    PACKET_PAYLOAD_END_BYTE +
                                                    PACKET_CRC_BYTES;

    private final static int PROTOCOL_SENDING_OVERHEAD =    PACKET_INITIAL_BYTE +
                                                            PACKET_LENGTH_BYTES +
                                                            PACKET_PAYLOAD_BEGIN_BYTE_A +
                                                            PACKET_PAYLOAD_END_BYTE +
                                                            PACKET_CRC_BYTES;

    private final static int DEVICE_TIME_OFFSET = 946684799; // Year 2000 UNIX time

    private short mHighestMeasIndex = 0;
    private short mHighestMeasID = 0;
    private short mHighestStoredMeasID = 0;
    private boolean mSynced = false;

    ArrayList<OnetouchMeasurement> mMeasurements  = new ArrayList<>();

    public enum State {
        IDLE,
        WAITING_TIME,
        WAITING_HIGHEST_ID,
        WAITING_OLDEST_INDEX,
        WAITING_MEASUREMENT,
        WAITING_LOW_LIMIT_SET,
        WAITING_LOW_LIMIT_GET,
        WAITING_HIGH_LIMIT_SET,
        WAITING_HIGH_LIMIT_GET
    };

    private Bleuart mBleUart;
    private State mState;
    private static Timer timer;


    // This class abstracts the protocol from the User
    public Protocol(ProtocolCallbacks aCallbacks, int aMaxPacketSize){
        protocolCallbacks = aCallbacks;
        mState = State.IDLE;
        timer = new Timer();
        mBleUart = new Bleuart(this, aMaxPacketSize);
    }

    void getStoredMeasurements(){
        getOldestRecordIndex();
    }

    // packing an array of 4 bytes to an int, little endian, clean code
    static int intFromByteArray(byte[] bytes) {
        return  ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }
    static short shortFromByteArray(byte[] bytes) {
        return  ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getShort();
    }

    /**********************************************************************************************/
    /*                                      Bleuart Callbacks                                     */

    /**
     * Called by Bleuart protocol when a packet is received.
     * @param aBytes
     */
    public void onPacketReceived(byte[] aBytes){
        try {
            byte[] payload = extractPayload(aBytes);
            Log.d(TAG,"Packet received: " + bytesToHex(payload));
            switch (mState) {
                case WAITING_TIME:
                    if (payload.length == 4) { // Time get response
                        handleTimeGet(computeUnixTime(payload));
                    } else if (payload.length == 0) { // Time set response (empty)
                        handleTimeSet();
                    }
                    else{
                        Log.e(TAG, "Unexpected payload waiting for time request!");
                    }
                    break;
                case WAITING_HIGHEST_ID:
                    if (payload.length == 4) {
                        int highestID = intFromByteArray(payload);
                        handleHighestRecordID((short)highestID);
                    }
                    else{
                        Log.e(TAG, "Unexpected payload waiting for highest record ID!");
                    }
                    break;
                case WAITING_OLDEST_INDEX:
                    if (payload.length == 2) {
                        short recordCount = shortFromByteArray(payload);
                        handleTotalRecordCount(recordCount);
                    }
                    else{
                        Log.e(TAG, "Unexpected payload waiting for total record request!");
                    }
                    break;
                case WAITING_MEASUREMENT:
                    if (payload.length == 11) {
                        int measTime = computeUnixTime(Arrays.copyOfRange(payload, 0, 0 + 4));
                        short measValue = shortFromByteArray(Arrays.copyOfRange(payload, 4, 4 + 2));
                        short measError = shortFromByteArray(Arrays.copyOfRange(payload, 9, 9 + 2));
                        handleMeasurementByID(measTime, measValue, measError);
                    }
                    else if(payload.length == 0){
                        // Measurement was not found! Indicate with aMeasTime=0
                        handleMeasurementByID(0,(short)0,(short)0);
                    }
                    else if (payload.length == 16) {
                        short measIndex = shortFromByteArray(Arrays.copyOfRange(payload,0,0+2));
                        short measID = shortFromByteArray(Arrays.copyOfRange(payload,3,3+2));
                        int measTime = computeUnixTime(Arrays.copyOfRange(payload,5,5+4));
                        short measValue = shortFromByteArray(Arrays.copyOfRange(payload,9,9+2));
                        short measUnknownValue = shortFromByteArray(Arrays.copyOfRange(payload,13,13+2));
                        handleMeasurementByIndex(measIndex,measID,measTime,measValue,measUnknownValue);
                    }

                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Called by Bleuart protocol to send bytes over ble
     * @param aBytes
     */
    public void sendData(byte[] aBytes){
        protocolCallbacks.sendData(aBytes);
    }
    /**********************************************************************************************/
    /**
     * Called by the lower layer when a ble data is received
     * @param bytes
     */
    public void onDataReceived(byte[] bytes){
        // Forward data to the bleuart protocol.
        mBleUart.onDataReceived(bytes);
    }
    /**********************************************************************************************/

    // Function to be called when the device connected
    public void connect(){
        if(mState == State.IDLE){
            timer = new Timer();
            getTime();
        }
    }

    // Function to be called when the device disconnects
    public void disconnect() {
        // Cancel any pending schedules
        timer.cancel();
        // Set state to disconnected
        mState = State.IDLE;
    }

    public void getTime(){
        mBleUart.sendPacket(buildPacket(new byte[]{0x20,0x02}));
        mState = State.WAITING_TIME;
    }

    public void setTime(){
        long currTime = computeSystemTime();
        mBleUart.sendPacket(buildPacket(new byte[]{0x20,
                0x01,
                (byte)((currTime&0x000000FF)),
                (byte)((currTime&0x0000FF00)>>8),
                (byte)((currTime&0x00FF0000)>>16),
                (byte)((currTime&0xFF000000)>>24)
        }));
        mState = State.WAITING_TIME;
    }

    public void getHighLimit(){
        mBleUart.sendPacket(buildPacket(new byte[]{0x0A,0x02,0x0A}));
        mState = State.WAITING_HIGH_LIMIT_GET;
    }

    public void setHighLimit(short high){
        mBleUart.sendPacket(buildPacket(new byte[]{0x0A,0x01,0x0A,
                (byte)((high&0x00FF)),
                (byte)((high&0xFF00)>>8),
                    0x00,
                    0x00}));
        mState = State.WAITING_HIGH_LIMIT_SET;
    }

    public void getLowLimit(){
        mBleUart.sendPacket(buildPacket(new byte[]{0x0A,0x02,0x09}));
        mState = State.WAITING_LOW_LIMIT_GET;
    }

    public void setLowLimit(short low){
        mBleUart.sendPacket(buildPacket(new byte[]{0x0A,0x01,0x09,
                (byte)((low&0x00FF)),
                (byte)((low&0xFF00)>>8),
                0x00,
                0x00}));
        mState = State.WAITING_LOW_LIMIT_SET;
    }

    public void getHighestRecordID(){
        mBleUart.sendPacket(buildPacket(new byte[]{0x0A,0x02,0x06}));
        mState = State.WAITING_HIGHEST_ID;
    }

    public void getOldestRecordIndex(){
        mBleUart.sendPacket(buildPacket(new byte[]{0x27,0x00}));
        mState = State.WAITING_OLDEST_INDEX;
    }

    public void getMeasurementsByIndex(int index){
        mBleUart.sendPacket(buildPacket(new byte[]{0x31,0x02,
                (byte) (index&0x00FF),
                (byte) ((index&0xFF00)>>8),
                0x00,}));
        mState = State.WAITING_MEASUREMENT;
    }

    public void getMeasurementsById(int id){
        mBleUart.sendPacket(buildPacket(new byte[]{
                (byte) 0xB3,
                (byte) (id&0x00FF),
                (byte) ((id&0xFF00)>>8)}));
        mState = State.WAITING_MEASUREMENT;
    }

    private void handleTimeGet(long aSeconds){
        Log.d(TAG, "Glucometer time is: "+ new Date(1000*aSeconds).toString());
        Log.d(TAG, "System time is: "+ new Date(System.currentTimeMillis()).toString());
        setTime();
    }

    private void handleTimeSet(){
        Log.d(TAG, "Time has been set!");
        if(!mSynced) {
            getOldestRecordIndex();
        }
        else{
            getHighestRecordID();
        }
    }

    private void handleTotalRecordCount(short aRecordCount) {
        Log.d(TAG, "Total records stored on Glucometer: " + aRecordCount);
        mHighestMeasIndex = aRecordCount;
        // After getting the number of stored measurements, start from the oldest one!
        getMeasurementsByIndex(aRecordCount-1);
    }

    private void handleHighestRecordID(short aRecordID) {
        Log.d(TAG, "Highest record ID: " + aRecordID);
        if(aRecordID > mHighestMeasID) {
            mHighestStoredMeasID = mHighestMeasID;
            mHighestMeasID = aRecordID;
            Log.d(TAG, "There are " + (mHighestMeasID - mHighestStoredMeasID) + " new records!");
            getMeasurementsById(mHighestStoredMeasID+1);
        }
        else{
            Log.d(TAG, "Measurements are up to date!");
            // Enqueue timer to poll new measurements?
        }
    }

    private void handleMeasurementByID(int aMeasTime, short aMeasValue, short aMeasError){
        // Update latest ID
        mHighestStoredMeasID++;

        if(aMeasTime!=0) { // If measurement was found..
            Log.d(TAG, "Measurement - Value: " + aMeasValue +
                    " Time: " + new Date(1000*(long)aMeasTime).toString() +
                    " Error: " + aMeasError);
            Date date = new Date(1000*(long)aMeasTime);
            mMeasurements.add(new OnetouchMeasurement(aMeasValue, date, Integer.toString(mHighestStoredMeasID)));
        }
        else{
            Log.d(TAG, "Measurement with ID: " + mHighestStoredMeasID + " was not found!");
        }

        if(mHighestStoredMeasID < mHighestMeasID){
            Log.d(TAG, "Requesting next measurement, ID: "+ (mHighestStoredMeasID+1));
            getMeasurementsById(mHighestStoredMeasID+1);
        }
        else{
            Log.d(TAG, "Measurement up to date!");
            // Notify application
            protocolCallbacks.onMeasurementsReceived(mMeasurements);
            mMeasurements.clear();
            // Start timer to poll for new measurements??
        }
    }

    private void handleMeasurementByIndex(short aMeasIndex, short aMeasID, int aMeasTime, short aMeasValue, short aMeasUnknownValue){
        Log.d(TAG, "Measurement " + aMeasIndex + " |" +
                            " Value: " + aMeasValue +
                            " Time: " + new Date(1000*(long)aMeasTime).toString() +
                            " ID:" + aMeasID);

        // Update latest ID
        mHighestMeasID = (short) Math.max(aMeasID,mHighestMeasID);
        mHighestStoredMeasID = mHighestMeasID;

        Date date = new Date(1000*(long)aMeasTime);
        mMeasurements.add(new OnetouchMeasurement(aMeasValue,date,Integer.toString(aMeasID)));
        if(aMeasIndex==0){ // The latest measurement
            // Notify application
            protocolCallbacks.onMeasurementsReceived(mMeasurements);
            mMeasurements.clear();
            mSynced = true;
            getHighestRecordID();
            // Start timer to poll for new measurements??
        }
        else{
            Log.d(TAG, "Requesting next measurement: " + (aMeasIndex-1));
            getMeasurementsByIndex(aMeasIndex-1);
        }
    }

    private static byte[] buildPacket(byte[] payload){
        int N = payload.length;
        int packetLength = PROTOCOL_SENDING_OVERHEAD + N;
        byte[] packet = new byte[packetLength];
        packet[0] = (byte) 0x02;
        packet[1] = (byte) packetLength;
        packet[2] = (byte) 0x00;
        packet[3] = (byte) 0x04;
        System.arraycopy(payload,0,packet,4,N);
        packet[4+N] = (byte) 0x03;
        appendCRC16(packet,packetLength-2);
        return packet;
    }

    private static byte[] extractPayload(byte[] packet) throws Exception {
        if(checkCRC16(packet)){
            if(packet.length == extractLength(packet) && packet.length>=PROTOCOL_OVERHEAD){
                return Arrays.copyOfRange(packet,
                                                PACKET_PAYLOAD_BEGIN,
                                                PACKET_PAYLOAD_BEGIN+packet.length-PROTOCOL_OVERHEAD);
            }
            else {
                throw new Exception("Bad Length! Received " + packet.length + " bytes but should have been " + extractLength(packet));
            }
        }
        else{
            int computedCRC = computeCRC(packet,0,packet.length-2);
            int receivedCRC = extractCRC(packet);
            throw new Exception("Bad CRC! Expected " + Integer.toHexString(computedCRC) +
                    " but got " + Integer.toHexString(receivedCRC) + ".");
        }
    }

    private static int computeUnixTime(byte[] sysTime){
        return DEVICE_TIME_OFFSET + intFromByteArray(sysTime);
    }
    private static int computeSystemTime(){
        return (int)(System.currentTimeMillis()/1000)-DEVICE_TIME_OFFSET;
    }

    public static int computeCRC(byte[] data, int offset, int length) {
        if (data == null || offset < 0 || offset > data.length - 1 || offset + length > data.length) {
            return 0;
        }

        int crc = 0xFFFF;
        for (int i = 0; i < length; ++i) {
            crc ^= data[offset + i] << 8;
            for (int j = 0; j < 8; ++j) {
                crc = (crc & 0x8000) > 0 ? (crc << 1) ^ 0x1021 : crc << 1;
            }
        }
        return crc & 0xFFFF;
    }

    private static int extractCRC(byte[] data){
        return (int) (((data[data.length-1]<<8)&0xFF00) | (data[data.length-2]&0x00FF));
    }
    private static int extractLength(byte[] data){
        return (int) (((data[2]<<8)&0xFF00) | (data[1]&0x00FF));
    }

    public static void appendCRC16(byte[] data, int length){
        int crc = computeCRC(data,0,length);
        data[length] = (byte) ((crc&0x00FF));
        data[length+1]   = (byte) ((crc&0xFF00)>>8);
    }

    private static boolean checkCRC16(byte[] data){
        int computedCRC = computeCRC(data,0,data.length-2);
        int receivedCRC = extractCRC(data);
        return receivedCRC==computedCRC;
    }
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
