package com.appia.onetouch.protocol;

import com.appia.onetouch.protocol.ProtocolCallbacks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

public class Protocol {

    private final static String TAG = "OneTouchProtocol";

    // Abstracts serial communication
    private ProtocolCallbacks protocolCallbacks;

    // All protocols have the following states.
    public enum State {DISCONNECTED, WAITING_INFO_PACKET, WAITING_MEASUREMENT,WAITING_RESULT_OR_END_PACKET};
    private State state;

    private int retries_on_current_packet;
    final static public int MAX_RETRIES = 5;
    final static public int RETRY_DELAY_MS = 1000;
    final static public int DELAY_AFTER_RECEIVED = 100;
    private static int CHECKSUM_OFFSET = 2;

    private static Timer timer;
    private static Semaphore  mutex = new Semaphore(1);

    // This class abstracts the protocol from the User
    public Protocol(ProtocolCallbacks aCallbacks){
        protocolCallbacks = aCallbacks;
        state = State.DISCONNECTED;
        timer = new Timer();
    }

    // Function to be called when the device connected
    public void connect(){
        if(state == State.DISCONNECTED){
            state = State.WAITING_INFO_PACKET;
            retries_on_current_packet = 0;
            timer = new Timer();
            sendPacket();
        }
    }

    // Function to be called when the device disconnects
    public void disconnect() {
        // Cancel any pending schedules
        timer.cancel();
        // Set state to disconnected
        state = State.DISCONNECTED;
    }

    public boolean getTime(){
        byte[] cmd = new byte[2];
        cmd[0] = (byte) 0x20;
        cmd[1] = (byte) 0x02;

        return true;
    }

    public boolean setTime(int timestamp){
        byte[] cmd = new byte[6];
        cmd[0] = (byte) 0x20;
        cmd[1] = (byte) 0x01;
        cmd[2] = (byte) (timestamp&0xFF);
        cmd[3] = (byte) (timestamp&0xFF00);
        cmd[4] = (byte) (timestamp&0xFF);
        cmd[5] = (byte) (timestamp&0xFF00);

        return true;
    }

    public boolean getHighLimit(){
        byte[] cmd = new byte[3];
        cmd[0] = (byte) 0x0a;
        cmd[1] = (byte) 0x02;
        cmd[2] = (byte) 0x0a;

        return true;
    }

    public boolean setHighLimit(short high){
        byte[] cmd = new byte[7];
        cmd[0] = (byte) 0x0a;
        cmd[1] = (byte) 0x01;
        cmd[2] = (byte) 0x0a;
        cmd[3] = (byte) (high&0xFF);
        cmd[4] = (byte) (high&0xFF00);
        cmd[5] = (byte) 0x00;
        cmd[6] = (byte) 0x00;

        return true;
    }

    public boolean getLowLimit(){
        byte[] cmd = new byte[3];
        cmd[0] = (byte) 0x0a;
        cmd[1] = (byte) 0x02;
        cmd[2] = (byte) 0x09;

        return true;
    }

    public boolean setLowLimit(short low){
        byte[] cmd = new byte[7];
        cmd[0] = (byte) 0x0a;
        cmd[1] = (byte) 0x01;
        cmd[2] = (byte) 0x09;
        cmd[3] = (byte) (low&0xFF);
        cmd[4] = (byte) (low&0xFF00);
        cmd[5] = (byte) 0x00;
        cmd[6] = (byte) 0x00;

        return true;
    }


    public boolean getTotalRecordCount(){
        byte[] cmd = new byte[3];
        cmd[0] = (byte) 0x0a;
        cmd[1] = (byte) 0x02;
        cmd[2] = (byte) 0x06;

        return true;
    }

    public boolean getCorrectRecordCount(){
        byte[] cmd = new byte[2];
        cmd[0] = (byte) 0x27;
        cmd[1] = (byte) 0x00;

        return true;
    }

    public boolean getMeasurementsByIndex(short index){
        byte[] cmd = new byte[5];
        cmd[0] = (byte) 0x31;
        cmd[1] = (byte) 0x02;
        cmd[2] = (byte) (index&0xFF);
        cmd[3] = (byte) (index&0xFF00);
        cmd[4] = (byte) 0x00;
        return true;
    }

    public boolean getMeasurementsById(short id){
        byte[] cmd = new byte[3];
        cmd[0] = (byte) 0xB3;
        cmd[1] = (byte) (id&0xFF);
        cmd[2] = (byte) (id&0xFF00);

        return true;
    }

    // This function should be called when a bluetooth packet is received
    public void onDataReceived(byte[] bytes){

    }

    // This function sends the packet
    public void sendPacket(){

    }
}
