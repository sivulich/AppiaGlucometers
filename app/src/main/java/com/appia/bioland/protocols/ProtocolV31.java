package com.appia.bioland.protocols;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ProtocolV31 extends Protocol {
    private SerialCommunicator serial;
    private Communication asyncCom;
    private enum AsyncState {WAITING_INFO_PACKET, WAITING_RESULT_OR_END_PACKET, DONE};
    private AsyncState asyncState;

    static public class Communication{
        public InfoPacket infoPacket;
        public List<ResultPacket> resultPackets;
        public EndPacket endPacket;
        public String error;

        public boolean valid(){
            return (infoPacket!=null && resultPackets !=null && endPacket!= null);
        }
    }

    static public class AppPacket{
        byte startCode;
        byte packetLength;
        byte packetCategory;
        byte year;
        byte month;
        byte day;
        byte hour;
        byte min;
        byte second;
        byte checksum;

        public byte[] to_bytes(){
            byte[] packet = new byte[10];
            packet[0] = startCode;
            packet[1] = packetLength;
            packet[2] = packetCategory;
            packet[3] = year;
            packet[4] = month;
            packet[5] = day;
            packet[6] = hour;
            packet[7] = min;
            packet[8] = second;
            packet[9] = checksum;
            return packet;
        }
    }

    static public class AppInfoPacket extends AppPacket{
        public AppInfoPacket(Calendar now){
            startCode = 0x5A;
            packetLength = 0x0A;
            packetCategory = 0x00;
            year = (byte) now.get(Calendar.YEAR);
            month = (byte) now.get(Calendar.MONTH);
            day = (byte) now.get(Calendar.DAY_OF_MONTH);
            hour = (byte) now.get(Calendar.HOUR);
            min = (byte) now.get(Calendar.MINUTE);
            second = (byte) now.get(Calendar.SECOND);
            int big =(int) (startCode&0xff) + (int) (packetLength&0xff) +
                    (int) (packetCategory&0xff) + (int) (year&0xff) +
                    (int) (month&0xff) + (int) (day&0xff) +
                    (int) (hour&0xff) + (int) (second&0xff) + 2;
            checksum = (byte)(big&0xff);
        }
    }

    static public class AppDataPacket extends AppPacket{
        public AppDataPacket(Calendar now){
            startCode = 0x5A;
            packetLength = 0x0A;
            packetCategory = 0x03;
            year = (byte) now.get(Calendar.YEAR);
            month = (byte) now.get(Calendar.MONTH);
            day = (byte) now.get(Calendar.DAY_OF_MONTH);
            hour = (byte) now.get(Calendar.HOUR);
            min = (byte) now.get(Calendar.MINUTE);
            second = (byte) now.get(Calendar.SECOND);
            int big =(int) (startCode&0xff) + (int) (packetLength&0xff) +
                    (int) (packetCategory&0xff) + (int) (year&0xff) +
                    (int) (month&0xff) + (int) (day&0xff) +
                    (int) (hour&0xff) + (int) (second&0xff) + 2;
            checksum = (byte)(big&0xff);
        }
    }

    static public class AppHandshakePacket{
        byte[] packet;
        public AppHandshakePacket(){
            packet = new byte[4];
            packet[0]=0x5a;
            packet[1]=0x04;
            packet[2]=0x09;
            packet[3]=0x67;
        }
        public byte[] to_bytes(){
            return packet;
        }
    }

    static public class InfoPacket{
        byte startCode;
        byte packetLength;
        byte packetCategory;
        byte versionCode;
        byte clientCode;
        byte modelCode;
        byte typeCode;
        byte retain;
        byte batteryCapacity;
        byte[] rollingCode;
        byte checksum;

        public InfoPacket(byte[] raw) throws IllegalContentException, IllegalLengthException {
            if (raw.length != 15)
                throw new IllegalLengthException("Packet length must be 15");

            startCode = raw[0];
            if (startCode != 0x55)
                throw new IllegalContentException("StartCode must be 0x55");

            packetLength = raw[1];
            if (packetLength != 0x0F)
                throw new IllegalContentException("PacketLength must be 0x0F");

            packetCategory = raw[2];
            if (packetCategory != 0x00)
                throw new IllegalContentException("PacketCategory must be 0x00");

            versionCode = raw[3];
            clientCode = raw[4];
            modelCode = raw[5];
            typeCode = raw[6];
            retain = raw[7];
            batteryCapacity = raw[8];
            rollingCode = new byte[5];
            rollingCode[0] = raw[9];
            rollingCode[1] = raw[10];
            rollingCode[2] = raw[11];
            rollingCode[3] = raw[12];
            rollingCode[4] = raw[13];

            checksum = raw[14];
            int big = (int) (startCode &0xff) +
                    (int) (packetLength&0xff) + (int) (packetCategory&0xff) + (int) (versionCode&0xff) +
                    (int) (clientCode&0xff) + (int) (modelCode&0xff) + (int) (typeCode&0xff) + (int) (retain&0xff) +
                    (int) (batteryCapacity&0xff) + (int) (rollingCode[0]&0xff) + (int) (rollingCode[1]&0xff) +
                    (int) (rollingCode[2]&0xff) + (int) (rollingCode[3]&0xff) + (int) (rollingCode[4]&0xff) + 2;
            //Double check for inconsistency in documentation
            if( !((big&0xff) == (int)(checksum&0xff) || ((big-2)&0xff) == (int)(checksum&0xff)))
                throw new IllegalContentException("Checksum Does Not Match");



        }
    }

    static public class TimingPacket{
        byte startCode;
        byte packetLength;
        byte packetCategory;
        byte retain;
        byte second;
        byte checksum;

        public TimingPacket(byte[] raw) throws IllegalContentException, IllegalLengthException {
            if (raw.length != 6)
                throw new IllegalLengthException("Packet length must be 6");

            startCode = raw[0];
            if (startCode != 0x55)
                throw new IllegalContentException("StartCode must be 0x55");

            packetLength = raw[1];
            if (packetLength != 0x06)
                throw new IllegalContentException("PacketLength must be 0x06");

            packetCategory = raw[2];
            if (packetCategory != 0x02)
                throw new IllegalContentException("PacketCategory must be 0x02");
            retain = raw[3];
            second = raw[4];
            checksum = raw[5];
            int big = (int) (startCode &0xff) +
                    (int) (packetLength&0xff) + (int) (packetCategory&0xff) + (int) (retain&0xff) +
                    (int) (second&0xff) + 2;
            //Double check for inconsistency in documentation
            if( !((big&0xff) == (int)(checksum&0xff) || ((big-2)&0xff) == (int)(checksum&0xff)))
                throw new IllegalContentException("Checksum Does Not Match");



        }
    }

    static public class ResultPacket{
        byte startCode;
        byte packetLength;
        byte packetCategory;
        byte year;
        byte month;
        byte day;
        byte hour;
        byte min;
        byte retain;
        byte[] glucose;
        byte checksum;

        public ResultPacket(byte[] raw) throws IllegalLengthException, IllegalContentException {
            if (raw.length != 12)
                throw new IllegalLengthException("Packet length must be 14");
            startCode = raw[0];
            if (startCode != (byte)0x55)
                throw new IllegalContentException("StartCode must be 0x55");

            packetLength = raw[1];
            if (packetLength != (byte)0x0C)
                throw new IllegalContentException("PacketLength must be 0x0C");

            packetCategory = raw[2];
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
            checksum = raw[11];

            //TODO: Verify if is +2 or not
            int big = (int) (startCode &0xff) +
                    (int) (packetLength&0xff) + (int) (packetCategory&0xff) + (int) (year&0xff) +
                    (int) (month&0xff) + (int) (day&0xff) + (int) (hour&0xff) + (int) (min&0xff) +
                    (int) (retain&0xff) + (int)(glucose[0]&0xff) + (int)(glucose[1]&0xff) +2;
            //Double check for inconsitency in documentation
            if( !((big&0xff) == (int)(checksum&0xff) || ((big-2)&0xff) == (int)(checksum&0xff)))
                throw new IllegalContentException("Checksum Does Not Match");

        }
    }

    static public class EndPacket{
        byte startCode;
        byte packetLength;
        byte packetCategory;
        byte retain;
        byte checksum;

        public EndPacket(byte[] raw) throws IllegalLengthException, IllegalContentException {
            if (raw.length != 5)
                throw new IllegalLengthException("Packet length must be 5");
            startCode = raw[0];
            if (startCode != (byte)0x55)
                throw new IllegalContentException("StartCode must be 0x55");

            packetLength = raw[1];
            if (packetLength != (byte)0x05)
                throw new IllegalContentException("PacketLength must be 0x05");

            packetCategory = raw[2];
            if (packetCategory != (byte)0x05)
                throw new IllegalContentException("PacketCategory must be 0x05");

            retain = raw[3];

            checksum = raw[4];

            int big = (int) (startCode &0xff) +
                    (int) (packetLength&0xff) + (int) (packetCategory&0xff) +
                    (int) (retain&0xff) + 2;

            if( !((big&0xff) == (int)(checksum&0xff) || ((big-2)&0xff) == (int)(checksum&0xff)))
                throw new IllegalContentException("Checksum Does Not Match");

        }
    }



    public ProtocolV31(SerialCommunicator comm){
        serial = comm;
        comm.connect();
    }

    public Communication communicate(){
        if (!serial.connected){
            serial.connect();
        }
        if(!serial.connected)
            return new Communication();
        Communication comm = new Communication();
        Calendar calendar = Calendar.getInstance();
        AppInfoPacket appInfoPacket = new AppInfoPacket(calendar);
        serial.send(appInfoPacket.to_bytes());
        byte[] reply = serial.recieve();
        try{
            comm.infoPacket = new InfoPacket(reply);
        }catch (IllegalLengthException | IllegalContentException e){
            comm.error = e.toString();
            return comm;
        }
        comm.resultPackets = new ArrayList<ResultPacket>();

        while(true){
            AppDataPacket appDataPacket = new AppDataPacket(calendar);
            serial.send(appDataPacket.to_bytes());
            reply = serial.recieve();
            try{
                comm.resultPackets.add(new ResultPacket(reply));

            }catch (IllegalLengthException | IllegalContentException e){
                try {
                    comm.endPacket = new EndPacket(reply);
                    return comm;
                } catch (IllegalLengthException | IllegalContentException k){
                    comm.error = k.toString();
                    return comm;
                }
            }
        }
    }

    public boolean asyncStartCommunication(){
        if (!serial.connected){
            serial.connect();
        }
        if(!serial.connected)
            return false;
        asyncCom = new Communication();
        Calendar calendar = Calendar.getInstance();
        AppInfoPacket appReplyPacket = new AppInfoPacket(calendar);
        serial.send(appReplyPacket.to_bytes());
        asyncState = AsyncState.WAITING_INFO_PACKET;
        return true;
    }

    public boolean asyncDoneCommunication(){
        return (asyncState == AsyncState.DONE);
    }

    public Communication asyncGetCommunication(){
        return asyncCom;
    }

    public void asyncCallbackReceive(byte[] packet){
        switch (asyncState){
            case WAITING_INFO_PACKET:
                try{
                    //Parse the information packet
                    asyncCom.infoPacket = new InfoPacket(packet);

                    //Change state to waiting for results or end packet
                    asyncState = AsyncState.WAITING_RESULT_OR_END_PACKET;

                    //Create the reply packet and send it
                    Calendar calendar = Calendar.getInstance();
                    AppDataPacket appReplyPacket = new AppDataPacket(calendar);
                    serial.send(appReplyPacket.to_bytes());
                }catch (IllegalLengthException | IllegalContentException e){
                    //If an error occurred load it to communication
                    asyncCom.error = e.toString();
                    asyncState = AsyncState.DONE;
                }
                break;
            case WAITING_RESULT_OR_END_PACKET:
                try{
                    //Try to parse as a result packet
                    ResultPacket resultPacket = new ResultPacket(packet);
                    if(asyncCom.resultPackets == null)
                        asyncCom.resultPackets = new ArrayList<ResultPacket>();
                    asyncCom.resultPackets.add(resultPacket);

                    //Create the reply packet and send it
                    Calendar calendar = Calendar.getInstance();
                    AppDataPacket appReplyPacket = new AppDataPacket(calendar);
                    serial.send(appReplyPacket.to_bytes());
                }catch (IllegalLengthException | IllegalContentException e){
                    //If controlled exception occurred
                    try {
                        //Try to parse as End Packet
                        asyncCom.endPacket = new EndPacket(packet);
                        asyncState = AsyncState.DONE;
                    } catch (IllegalLengthException | IllegalContentException k){
                        asyncCom.error = k.toString();
                        asyncState = AsyncState.DONE;
                    }
                }
                break;

            //If entered this function in DONE state an error ocurred, should clear communication
            case DONE:
            default:
                asyncCom = new Communication();
                asyncCom.error = "Received a packet after communication is done";
                asyncState = AsyncState.DONE;
                break;
        }
    }
}
