package com.appia.onetouch.protocol;

import android.util.Log;

import com.appia.onetouch.protocol.ProtocolCallbacks;
import com.appia.onetouch.protocol.bleuart.Bleuart;
import com.appia.onetouch.protocol.bleuart.BleuartCallbacks;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

public class Protocol implements BleuartCallbacks {

    private final static String TAG = "OneTouchProtocol";

    // Abstracts serial communication
    private ProtocolCallbacks protocolCallbacks;

    private final static int PROTOCOL_OVERHEAD = 7;
    private final static int DEVICE_TIME_OFFSET = 946684799; // Year 2000 UNIX time

    public enum State {IDLE, WAITING_TIME_GET, WAITING_TIME_SET, WAITING_MEASUREMENT};
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
        getTotalRecordCount();
    }
    // packing an array of 4 bytes to an int, little endian, clean code
    int intFromByteArray(byte[] bytes) {
        return  ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    /**********************************************************************************************/
    /*                                      Bleuart Callbacks                                     */

    /**
     * Called by Bleuart protocol when a packet is received.
     * @param aBytes
     */
    public void onPacketReceived(byte[] aBytes){
        Log.d(TAG,"Packet received: " + bytesToHex(aBytes));

        if(checkCRC16(aBytes)){
            switch(mState){
                case WAITING_TIME_GET:
                    if(aBytes.length==12){
                        long timeInSeconds = DEVICE_TIME_OFFSET + intFromByteArray(Arrays.copyOfRange(aBytes,5,5+4));
                        handleTimeGet(timeInSeconds);
                    }
                    else if(aBytes.length==8) {
                        handleTimeSet();
                    }
                    break;
                case WAITING_MEASUREMENT:
                    if(aBytes.length==19){

                    }
                    else if(aBytes.length==24) {

                    }
                    break;
            }
        }
        else {
            int computedCRC = computeCRC(aBytes,0,aBytes.length-2);
            int receivedCRC = extractCRC(aBytes);
            Log.e(TAG,"CRC error! Expected " + Integer.toHexString(computedCRC) +
                    " but got " + Integer.toHexString(receivedCRC) + ".");
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

    public boolean getTime(){
        mBleUart.sendPacket(buildPacket(new byte[]{0x20,0x02}));
        mState = State.WAITING_TIME_GET;
        return true;
    }

    public boolean setTime(){
        long currTime = System.currentTimeMillis()/1000-DEVICE_TIME_OFFSET;
        mBleUart.sendPacket(buildPacket(new byte[]{0x20,
                0x01,
                (byte)((currTime&0x000000FF)),
                (byte)((currTime&0x0000FF00)>>8),
                (byte)((currTime&0x00FF0000)>>16),
                (byte)((currTime&0xFF000000)>>24)
        }));
        return true;
    }

    public boolean getHighLimit(){
        byte[] cmd = new byte[3];
        cmd[0] = (byte) 0x0a;
        cmd[1] = (byte) 0x02;
        cmd[2] = (byte) 0x0a;

        mBleUart.sendPacket(cmd);
        return true;
    }

    public boolean setHighLimit(short high){
        byte[] cmd = new byte[7];
        cmd[0] = (byte) 0x0a;
        cmd[1] = (byte) 0x01;
        cmd[2] = (byte) 0x0a;
        cmd[3] = (byte) ((high&0x00FF));
        cmd[4] = (byte) ((high&0xFF00)>>8);
        cmd[5] = (byte) 0x00;
        cmd[6] = (byte) 0x00;

        mBleUart.sendPacket(cmd);
        return true;
    }

    public boolean getLowLimit(){
        byte[] cmd = new byte[3];
        cmd[0] = (byte) 0x0a;
        cmd[1] = (byte) 0x02;
        cmd[2] = (byte) 0x09;

        mBleUart.sendPacket(cmd);
        return true;
    }

    public boolean setLowLimit(short low){
        byte[] cmd = new byte[7];
        cmd[0] = (byte) 0x0a;
        cmd[1] = (byte) 0x01;
        cmd[2] = (byte) 0x09;
        cmd[3] = (byte) (low&0x00FF);
        cmd[4] = (byte) ((low&0xFF00)>>8);
        cmd[5] = (byte) 0x00;
        cmd[6] = (byte) 0x00;

        mBleUart.sendPacket(cmd);
        return true;
    }


    public boolean getTotalRecordCount(){
        byte[] cmd = new byte[3];
        cmd[0] = (byte) 0x0a;
        cmd[1] = (byte) 0x02;
        cmd[2] = (byte) 0x06;

        mBleUart.sendPacket(cmd);
        return true;
    }

    public boolean getCorrectRecordCount(){
        byte[] cmd = new byte[2];
        cmd[0] = (byte) 0x27;
        cmd[1] = (byte) 0x00;

        mBleUart.sendPacket(cmd);
        return true;
    }

    public boolean getMeasurementsByIndex(short index){
        byte[] cmd = new byte[5];
        cmd[0] = (byte) 0x31;
        cmd[1] = (byte) 0x02;
        cmd[2] = (byte) (index&0xFF);
        cmd[3] = (byte) (index&0xFF00);
        cmd[4] = (byte) 0x00;

        mBleUart.sendPacket(cmd);
        return true;
    }

    public boolean getMeasurementsById(short id){
        byte[] cmd = new byte[3];
        cmd[0] = (byte) 0xB3;
        cmd[1] = (byte) (id&0xFF);
        cmd[2] = (byte) (id&0xFF00);

        mBleUart.sendPacket(cmd);
        return true;
    }

    private void handleTimeGet(long aSeconds){
        Log.d(TAG, "Glucometer time is: "+ new Date(1000*aSeconds).toString());
        Log.d(TAG, "System time is: "+ new Date(System.currentTimeMillis()).toString());
        setTime();
    }

    private void handleTimeSet(){
        Log.d(TAG, "Time has been set!");
    }
  /*  private int crc16(byte[] bytes){
            int crc = 0xFFFF;          // initial value
            int polynomial = 0x1021;   // 0001 0000 0010 0001  (0, 5, 12)

            for (byte b : bytes) {
                for (int i = 0; i < 8; i++) {
                    boolean bit = ((b   >> (7-i) & 1) == 1);
                    boolean c15 = ((crc >> 15    & 1) == 1);
                    crc <<= 1;
                    if (c15 ^ bit) crc ^= polynomial;
                }
            }
            crc &= 0xffff;
            return crc;
    }*/


    private static byte[] buildPacket(byte[] payload){
        int N = payload.length;
        int packetLength = PROTOCOL_OVERHEAD + N;
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
