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


    // This function starts the communication, must be used if the protocol is <3.1
    public boolean requestMeasurements(){

        return true;
    }

    // This function should be called when a bluetooth packet is received
    public void onDataReceived(byte[] bytes){

    }

    // This function sends the packet
    public void sendPacket(){

    }
}
