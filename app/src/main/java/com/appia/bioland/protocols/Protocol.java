package com.appia.bioland.protocols;

import com.appia.bioland.BiolandInfo;
import com.appia.bioland.BiolandMeasurement;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class Protocol {
    // Abstracts serial communication
    private ProtocolCallbacks protocolCallbacks;
    // Allows to hold state for communication protocol agnostic
    private Communication asyncCom;

    protected String version;

    // All protocols have the following states.
    protected enum AsyncState {WAITING_INFO_PACKET, WAITING_RESULT_OR_END_PACKET, DONE};
    private AsyncState asyncState;

    // This class abstracts the protocol from the User
    public Protocol(ProtocolCallbacks aCallbacks){
        protocolCallbacks = aCallbacks;
        asyncState=AsyncState.DONE;
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

    // This set of functions allow an asynchronous communication to the device
    // This function starts the communication
    public boolean startCommunication(){
        if(asyncState!= AsyncState.DONE)
            return false;
        asyncCom = new Communication();
        Calendar calendar = Calendar.getInstance();
        AppPacket appReplyPacket = build_get_info_packet(calendar);
        protocolCallbacks.sendData(appReplyPacket.to_bytes());
        asyncState = AsyncState.WAITING_INFO_PACKET;
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
        switch (asyncState){
            case WAITING_INFO_PACKET:
                try{
                    //Parse the information packet
                    asyncCom.infoPacket = build_info_packet(packet);

                    /* Notify application. */
                    // TODO!!! Fill
                    BiolandInfo info = new BiolandInfo();
                    info.batteryCapacity = 0;
                    info.protocolVersion = 40;
                    info.serialNumber = new byte[]{1,2,3,4};
                    protocolCallbacks.onDeviceInfoReceived(info);

                    //Change state to waiting for results or end packet
                    asyncState = AsyncState.WAITING_RESULT_OR_END_PACKET;

                    //Create the reply packet and send it
                    Calendar calendar = Calendar.getInstance();
                    AppPacket appReplyPacket = build_get_meas_packet(calendar);
                    protocolCallbacks.sendData(appReplyPacket.to_bytes());
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
                    if(asyncCom.resultPackets == null)
                        asyncCom.resultPackets = new ArrayList<>();
                    asyncCom.resultPackets.add(resultPacket);

                    //Create the reply packet and send it
                    Calendar calendar = Calendar.getInstance();
                    AppPacket appReplyPacket = build_get_meas_packet(calendar);
                    protocolCallbacks.sendData(appReplyPacket.to_bytes());
                }catch (IllegalLengthException | IllegalContentException e){
                    //If controlled exception occurred
                    try {
                        //Try to parse as End Packet
                        asyncCom.endPacket = build_end_packet(packet);
                        asyncState = AsyncState.DONE;

                        ArrayList<BiolandMeasurement> arr = new ArrayList<>();

                        for(int i=0; i<asyncCom.resultPackets.size(); i++) {
                            ResultPacket p = asyncCom.resultPackets.get(i);
                            arr.add(new BiolandMeasurement(p.getGlucose(),
                                    2000+p.year&0xff,
                                    p.month&0xff,
                                    p.day&0xff,
                                    p.hour&0xff,
                                    p.min&0xff));
                        }
                        protocolCallbacks.onMeasurementsReceived(arr);

                    } catch (IllegalLengthException | IllegalContentException k){
                        asyncCom.error = k.toString();
                        asyncState = AsyncState.DONE;
                        protocolCallbacks.onProtocolError(asyncCom.error);
                    }
                }
                break;

            //If entered this function in DONE state an error ocurred, should clear communication
            case DONE:
            default:
                asyncCom = new Communication();
                asyncCom.error = "Received a packet after communication is done";
                asyncState = AsyncState.DONE;
                protocolCallbacks.onProtocolError(asyncCom.error);
                break;
        }
    }

    // Define all class of packets in protocols, abstracting the version of the protocol
    static public class ProtocolPacket{
        byte startCode;
        byte packetLength;
        byte packetCategory;
        byte[] checksum;

        protected void calculateChecksum(int length){
            byte[] bytes = getVariablesInByteArray();
            int sum = 0;
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
            year = (byte) calendar.get(Calendar.YEAR);
            month = (byte) calendar.get(Calendar.MONTH);
            day = (byte) calendar.get(Calendar.DAY_OF_MONTH);
            hour = (byte) calendar.get(Calendar.HOUR);
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
