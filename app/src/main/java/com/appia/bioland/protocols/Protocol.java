package com.appia.bioland.protocols;

import android.util.Log;

import com.appia.bioland.BiolandInfo;
import com.appia.bioland.BiolandMeasurement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;


public abstract class Protocol {
    // Abstracts serial communication
    private ProtocolCallbacks protocolCallbacks;
    // Allows to hold state for communication protocol agnostic
    private Communication asyncCom;

    // Contains the current protocol version
    protected Version version;

    // All protocols have the following states.
    protected enum AsyncState {WAITING_HANDSHAKE_PACKET, WAITING_INFO_PACKET, WAITING_RESULT_OR_END_PACKET, DONE};
    private AsyncState asyncState;
    private int retries_on_current_packet;
    private int MAX_RETRIES = 20;
    private int RETRY_DELAY = 100;
    private int DELAY_AFTER_RECEIVED = 100;
    private static int CHECKSUM_OFFSET = 2;

    private static Timer timer;
    private static Semaphore  mutex = new Semaphore(1);

    // This class abstracts the protocol from the User
    public Protocol(ProtocolCallbacks aCallbacks){
        protocolCallbacks = aCallbacks;
        asyncState=AsyncState.DONE;
        timer = new Timer();
    }

    // This class holds a communication with an Bioland G-500 device
    static public class Communication{
        public InfoPacket infoPacket;
        public List<ResultPacket> resultPackets;
        public DevicePacket endPacket;
        public String error;

        public boolean valid(){
            return (infoPacket!=null && resultPackets !=null && endPacket!= null);
        }
    }

    // This function starts the communication, must be used if the protocol is <3.1
    public boolean startCommunication(){

        try {
            mutex.acquire();
        }catch (java.lang.InterruptedException a){
            return false;
        }
        asyncCom = new Communication();
        Calendar calendar = Calendar.getInstance();
        AppPacket appReplyPacket;
        appReplyPacket = build_get_info_packet(calendar);
        protocolCallbacks.sendData(appReplyPacket.to_bytes());
        asyncState = AsyncState.WAITING_INFO_PACKET;
        retries_on_current_packet = 0;
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                sendPacket();
            }
        }, RETRY_DELAY);
        mutex.release(1);
        return true;
    }

    // This function tells you if the communication is done
    public boolean doneCommunication(){
        return (asyncState == AsyncState.DONE);
    }

    // This function returns the current communication
    public Communication getCommunication(){
        return asyncCom;
    }

    // This function should be called when a bluetooth packet is received
    public void onDataReceived(byte[] packet){
        Calendar calendar;
        // Aquire mutex not to step on send function
        try {
            mutex.acquire();
        }catch (java.lang.InterruptedException a){
            return;
        }
        // Cancel any pending schedules
        timer.cancel();
        timer = new Timer();
        switch (asyncState){
            // If entered this function in DONE a measurement was sent without being requested,
            // must be a new measurement!
            case DONE:
                try{
                    try{
                        // Try to parse as a timing packet
                        DevicePacket timing_packet = build_timing_packet(packet);
                        byte[] variables = timing_packet.getVariablesInByteArray();
                        // Get timing from packet, it's in position 4
                        protocolCallbacks.onCountdownReceived(variables[4]);
                    }catch (IllegalLengthException | IllegalContentException e) {
                        //Try to parse as a result packet
                        ResultPacket resultPacket = build_result_packet(packet);
                        // If a measurement is recived a new communication must start
                        asyncCom = new Communication();
                        asyncCom.resultPackets =  new ArrayList<>();
                        asyncCom.resultPackets.add(resultPacket);
                        // Next i should request info packet
                        asyncState = AsyncState.WAITING_INFO_PACKET;
                    }
                }catch (IllegalLengthException | IllegalContentException e) {
                    asyncCom = new Communication();
                    asyncCom.error = "Received a packet that is not a measurement or timer on idle";
                    asyncState = AsyncState.DONE;
                    protocolCallbacks.onProtocolError(asyncCom.error);
                }

                break;
            // If waiting for an Information packet
            case WAITING_INFO_PACKET:
                try{
                    // Try to parse  as an information packet
                    asyncCom.infoPacket = build_info_packet(packet);
                    // Fill in application information with available infromation
                    BiolandInfo info = new BiolandInfo();
                    if( version.equals(new Version("1.0"))){
                        ProtocolV1.InfoPacketV1 v1_info_packet = (ProtocolV1.InfoPacketV1) asyncCom.infoPacket;
                        info.productionDate = new GregorianCalendar();
                        info.productionDate.set(v1_info_packet.productionYear,v1_info_packet.productionMonth,0);
                    } else if (version.equals(new Version("2.0"))){
                        ProtocolV2.InfoPacketV2 v2_info_packet = (ProtocolV2.InfoPacketV2) asyncCom.infoPacket;
                        info.batteryCapacity = v2_info_packet.batteryCapacity;
                        info.serialNumber = v2_info_packet.rollingCode;
                    } else if (version.equals(new Version("3.1"))) {
                        ProtocolV31.InfoPacketV31 v31_info_packet = (ProtocolV31.InfoPacketV31) asyncCom.infoPacket;
                        info.batteryCapacity = v31_info_packet.batteryCapacity;
                        info.serialNumber = v31_info_packet.rollingCode;
                    } else if (version.equals(new Version("3.1"))){
                        ProtocolV32.InfoPacketV32 v32_info_packet = (ProtocolV32.InfoPacketV32) asyncCom.infoPacket;
                        info.batteryCapacity = v32_info_packet.batteryCapacity;
                        info.serialNumber = v32_info_packet.seriesNumber;
                    }
                    // Notify applicantion
                    protocolCallbacks.onDeviceInfoReceived(info);

                    //Change state to waiting for results or end packet
                    asyncState = AsyncState.WAITING_RESULT_OR_END_PACKET;

                }catch (IllegalLengthException | IllegalContentException e){

                    //If an error occurred load it to communication
                    asyncCom.error = e.toString();
                    asyncState = AsyncState.DONE;
                    protocolCallbacks.onProtocolError(asyncCom.error);

                }
                break;
            case WAITING_RESULT_OR_END_PACKET:
                try{
                    //Try to parse as a result packet
                    ResultPacket resultPacket = build_result_packet(packet);
                    // If the result packet array is empty
                    if(asyncCom.resultPackets == null) {
                        asyncCom.resultPackets = new ArrayList<>();
                    }
                    asyncCom.resultPackets.add(resultPacket);

                }catch (IllegalLengthException | IllegalContentException e){
                    //If controlled exception occurred
                    try {
                        //Try to parse as End Packet
                        asyncCom.endPacket = build_end_packet(packet);

                        // Notify the application of the received measurements
                        if(asyncCom.resultPackets!= null) {
                            ArrayList<BiolandMeasurement> arr = new ArrayList<>();
                            for (int i = 0; i < asyncCom.resultPackets.size(); i++) {
                                ResultPacket p = asyncCom.resultPackets.get(i);
                                arr.add(new BiolandMeasurement(p.getGlucose()/(float)18,
                                        2000 + p.year & 0xff,
                                        p.month & 0xff,
                                        p.day & 0xff,
                                        p.hour & 0xff,
                                        p.min & 0xff,
                                        Arrays.toString(p.getVariablesInByteArray())));
                            }
                            protocolCallbacks.onMeasurementsReceived(arr);
                        }

                        // Set state as done
                        asyncState = AsyncState.DONE;

                    } catch (IllegalLengthException | IllegalContentException k){
                        asyncCom.error = k.toString();
                        asyncState = AsyncState.DONE;
                        protocolCallbacks.onProtocolError(asyncCom.error);
                    }
                }
                break;
        }
        // All states except DONE require to send a packet after a delay
        if (asyncState != AsyncState.DONE){
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    sendPacket();
                }
            }, DELAY_AFTER_RECEIVED);
        }
        retries_on_current_packet=0;
        mutex.release(1);
    }

    // This function sends the packet
    public void sendPacket(){
        // Acquire the mutex not to step on receive
        try {
            mutex.acquire();
        }catch (java.lang.InterruptedException a){
            return;
        }
        // If i haven't retried the max number of tries
        if (retries_on_current_packet<MAX_RETRIES){

            retries_on_current_packet+=1;
            Calendar calendar;
            switch (asyncState){
                // Request information packet
                case WAITING_INFO_PACKET:
                    // Build information packet with current date
                    calendar = Calendar.getInstance();
                    AppPacket appInfoPacket = build_get_info_packet(calendar);
                    protocolCallbacks.sendData(appInfoPacket.to_bytes());
                    // Schedule next packet in RETRY_DELAY milliseconds
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            sendPacket();
                        }
                    }, RETRY_DELAY);
                    break;
                // Request measurement packet
                case WAITING_RESULT_OR_END_PACKET:
                    // Create packet with current date
                    calendar = Calendar.getInstance();
                    AppPacket appDataPacket = build_get_meas_packet(calendar);
                    protocolCallbacks.sendData(appDataPacket.to_bytes());
                    // Schedule next packet in RETRY_DELAY milliseconds
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            sendPacket();
                        }
                    }, RETRY_DELAY);
                    break;
                case DONE:
                    break;
            }

        } else {
            // If the max number of retries was reached, stop and mark communication as error.
            retries_on_current_packet = 0;
            asyncState = AsyncState.DONE;
            asyncCom.error = "Max retries reached on current state";
            protocolCallbacks.onProtocolError(asyncCom.error);
        }
        mutex.release(1);
    }

    // Define all class of packets in protocols, abstracting the version of the protocol
    static public class ProtocolPacket{
        byte startCode;
        byte packetLength;
        byte packetCategory;
        byte[] checksum;

        protected void calculateChecksum(int length){
            byte[] bytes = getVariablesInByteArray();
            int sum = CHECKSUM_OFFSET;
            for (int i=0; i< bytes.length;i++){
                sum+= (int)(bytes[i]&0xff);
            }
            checksum = new byte[length];
            for (int i=0; i<length; i++){
                checksum[i] = (byte) ((sum>>(8*i))&0xff);
            }
        }
        protected byte[] getVariablesInByteArray(){
            byte[] bytes = new byte[3];
            bytes[0] = startCode;
            bytes[1] = packetLength;
            bytes[2] = packetCategory;
            return bytes;
        }



    }

    static public class DevicePacket extends ProtocolPacket{
        public DevicePacket(byte[] raw_packet)  throws IllegalContentException, IllegalLengthException {
            if(raw_packet.length<3)
                throw new IllegalLengthException("Packet length must be bigger than 3");
            startCode = raw_packet[0];
            packetLength = raw_packet[1];
            packetCategory = raw_packet[2];
        }
    }

    static public class InfoPacket extends DevicePacket{
        byte versionCode;
        byte clientCode;

        @Override
        protected byte[] getVariablesInByteArray(){
            byte[] parentBytes = super.getVariablesInByteArray();
            byte[] bytes = new byte[parentBytes.length + 2];
            for(int i=0;i<parentBytes.length;i++){
                bytes[i] = parentBytes[i];
            }
            bytes[parentBytes.length+0] = versionCode;
            bytes[parentBytes.length+1] = clientCode;
            return bytes;
        }

        public InfoPacket(byte[] raw) throws IllegalLengthException, IllegalContentException {
            super(raw);
            versionCode = raw[3];
            clientCode = raw[4];
        }
    }

    static public class ResultPacket extends DevicePacket{
        byte year;
        byte month;
        byte day;
        byte hour;
        byte min;
        byte retain;
        byte[] glucose;

        @Override
        protected byte[] getVariablesInByteArray(){
            byte[] parentBytes = super.getVariablesInByteArray();
            byte[] bytes = new byte[parentBytes.length + 9];
            for(int i=0;i<parentBytes.length;i++){
                bytes[i] = parentBytes[i];
            }
            bytes[parentBytes.length+0] = year;
            bytes[parentBytes.length+1] = month;
            bytes[parentBytes.length+2] = day;
            bytes[parentBytes.length+3] = hour;
            bytes[parentBytes.length+5] = min;
            bytes[parentBytes.length+6] = retain;
            bytes[parentBytes.length+7] = glucose[0];
            bytes[parentBytes.length+8] = glucose[1];
            return bytes;
        }

        //Returns glucose in mg/dL
        public int getGlucose(){
            return (int)((glucose[0]&0xff)<<0)+(int)((glucose[1]&0xff)<<8);
        }

        public ResultPacket(byte[] raw) throws IllegalLengthException, IllegalContentException {
            super(raw);
            if (startCode != (byte)0x55)
                throw new IllegalContentException("StartCode must be 0x55");
            if (packetCategory != (byte)0x03)
                throw new IllegalContentException("PacketCategory must be 0x03");
            year = raw[3];
            month = raw[4];
            day = raw[5];
            hour = raw[6];
            min = raw[7];
            retain = raw[8];
            glucose = new byte[2];
            glucose[0] = raw[9];
            glucose[1] = raw[10];
        }
    }

    static public class AppPacket extends ProtocolPacket{
        byte year;
        byte month;
        byte day;
        byte hour;
        byte min;

        @Override
        protected byte[] getVariablesInByteArray(){
            byte[] parentBytes = super.getVariablesInByteArray();
            byte[] bytes = new byte[parentBytes.length + 5];
            for(int i=0;i<parentBytes.length;i++){
                bytes[i] = parentBytes[i];
            }
            bytes[parentBytes.length+0] = year;
            bytes[parentBytes.length+1] = month;
            bytes[parentBytes.length+2] = day;
            bytes[parentBytes.length+3] = hour;
            bytes[parentBytes.length+4] = min;
            return bytes;
        }

        public byte[] to_bytes(){
            byte[] variables = getVariablesInByteArray();
            byte[] bytes = new byte[variables.length+checksum.length];
            for (int i=0; i< variables.length;i++){
                bytes[i] = variables[i];
            }
            for (int i=0; i< checksum.length;i++){
                bytes[variables.length+i] = checksum[i];
            }
            return bytes;
        }


        public AppPacket(Calendar calendar){
            super();
            year = (byte) (calendar.get(Calendar.YEAR)-2000);
            month = (byte) calendar.get(Calendar.MONTH);
            day = (byte) calendar.get(Calendar.DAY_OF_MONTH);
            hour = (byte) calendar.get(Calendar.HOUR_OF_DAY);
            min = (byte) calendar.get(Calendar.MINUTE);
        }
    }

    // Defines builders for different packets allowing different protocols to override them
    protected AppPacket build_get_info_packet(Calendar calendar){
        return new AppPacket(calendar);
    }

    protected AppPacket build_get_meas_packet(Calendar calendar){
        return new AppPacket(calendar);
    }

    protected byte[] build_handshake_packet(){
        return new byte[0];
    }

    protected DevicePacket build_timing_packet(byte[] raw) throws IllegalLengthException, IllegalContentException {
        throw  new IllegalContentException("Protocol"+version+" does not support timing packet");
    }

    protected InfoPacket build_info_packet(byte[] raw) throws IllegalLengthException, IllegalContentException {
        return new InfoPacket(raw);
    }

    protected ResultPacket build_result_packet(byte[] raw) throws IllegalLengthException, IllegalContentException {
        return new ResultPacket(raw);
    }

    protected DevicePacket build_end_packet(byte[] raw) throws IllegalLengthException, IllegalContentException {
        return new DevicePacket(raw);
    }

    // Define of own exceptions used for error checking
    static public class IllegalLengthException extends Exception{
        String error;
        public String toString() {
            return "IllegalLength[" + error + "]";
        }
        IllegalLengthException(String s){
            error = s;
        }
    }
    static public class IllegalContentException extends Exception{
        String error;

        public String toString() {
            return "IllegalContent[" + error + "]";
        }
        IllegalContentException(String s){
            error = s;
        }
    }
}
