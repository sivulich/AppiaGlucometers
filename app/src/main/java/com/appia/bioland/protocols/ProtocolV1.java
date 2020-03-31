package com.appia.bioland.protocols;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ProtocolV1 extends Protocol {
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

//    static public class AppPacket{
//        byte startCode;
//        byte packetLength;
//        byte packetCategory;
//        byte year;
//        byte month;
//        byte day;
//        byte hour;
//        byte min;
//        byte[] checksum;
//
//        public byte[] to_bytes(){
//            byte[] packet = new byte[11];
//            packet[0] = startCode;
//            packet[1] = packetLength;
//            packet[2] = packetCategory;
//            packet[3] = year;
//            packet[4] = month;
//            packet[5] = day;
//            packet[6] = hour;
//            packet[7] = min;
//            packet[8] = checksum[0];
//            packet[9] = checksum[1];
//            packet[10] = checksum[2];
//            return packet;
//        }
//    }
    static public class AppReplyPacket extends AppPacket{
        public AppReplyPacket(Calendar now){
            super(now);
            startCode = 0x5A;
            packetLength = 0x0B;
            packetCategory = 0x05;
            calculateChecksum(3);
        }

    }
    static public class AppTerminationPacket extends AppPacket{
        public AppTerminationPacket(Calendar now){
            super(now);
            startCode = 0x5A;
            packetLength = 0x0B;
            packetCategory = 0x06;
            calculateChecksum(3);
        }
    }

    static public class InfoPacket extends DevicePacket{
        byte versionCode;
        byte clientCode;
        byte modelCode;
        byte typeCode;
        byte userID;
        byte productionYear;
        byte productionMonth;
        byte[] serialNumber;

        @Override
        protected byte[] getVariablesInByteArray(){
            byte[] parentBytes = super.getVariablesInByteArray();
            byte[] bytes = new byte[parentBytes.length + 5];
            for(int i=0;i<parentBytes.length;i++){
                bytes[i] = parentBytes[i];
            }
            bytes[parentBytes.length+0] = versionCode;
            bytes[parentBytes.length+1] = clientCode;
            bytes[parentBytes.length+2] = modelCode;
            bytes[parentBytes.length+3] = typeCode;
            bytes[parentBytes.length+4] = userID;
            bytes[parentBytes.length+4] = productionYear;
            bytes[parentBytes.length+4] = productionMonth;
            bytes[parentBytes.length+4] = serialNumber[0];
            bytes[parentBytes.length+4] = serialNumber[1];
            bytes[parentBytes.length+4] = serialNumber[2];
            return bytes;
        }

        public InfoPacket(byte[] raw) throws IllegalContentException, IllegalLengthException {
            super(raw);
            if (raw.length != 16)
                throw new IllegalLengthException("Packet length must be 16");

            if (startCode != 0x55)
                throw new IllegalContentException("StartCode must be 0x55");

            if (packetLength != 0x10)
                throw new IllegalContentException("PacketLength must be 0x10");

            if (packetCategory != 0x00)
                throw new IllegalContentException("PacketCategory must be 0x00");

            versionCode = raw[3];
            clientCode = raw[4];
            modelCode = raw[5];
            typeCode = raw[6];
            userID = raw[7];
            productionYear = raw[8];
            productionMonth = raw[9];

            serialNumber = new byte[3];
            serialNumber[0] = raw[10];
            serialNumber[1] = raw[11];
            serialNumber[2] = raw[12];

            //Cannot use calculate checksum of parent as in this protocol the checksum is calculated
            //different
            checksum = new byte[3];
            checksum[0] = raw[13];
            checksum[1] = raw[14];
            checksum[2] = raw[15];
            int serial =  (int)(serialNumber[0]&0xff) + (int)((serialNumber[1]&0xff)<<8) + (int)((serialNumber[2]&0xff)<<16) ;
            int big = (int) (startCode &0xff) +
                    (int) (packetLength&0xff) + (int) (packetCategory&0xff) + (int) (versionCode&0xff) +
                    (int) (clientCode&0xff) + (int) (modelCode&0xff) + (int) (typeCode&0xff) + (int) (userID&0xff) +
                    (int) (productionYear&0xff) + (int) (productionMonth&0xff);
            big += serial;
            int check = (int) (checksum[0]&0xff) + (int) ((checksum[1]&0xff)<<8) + (int) ((checksum[2]&0xff)<<16);
            if(big != check)
                throw new IllegalContentException("Checksum Does Not Match");
        }
    }

    static public class ResultPacket extends DevicePacket{
        byte year;
        byte month;
        byte day;
        byte hour;
        byte min;
        byte save;
        byte[] glucose;

        @Override
        protected byte[] getVariablesInByteArray(){
            byte[] parentBytes = super.getVariablesInByteArray();
            byte[] bytes = new byte[parentBytes.length + 8];
            for(int i=0;i<parentBytes.length;i++){
                bytes[i] = parentBytes[i];
            }
            bytes[parentBytes.length+0] = year;
            bytes[parentBytes.length+1] = month;
            bytes[parentBytes.length+2] = day;
            bytes[parentBytes.length+3] = hour;
            bytes[parentBytes.length+4] = min;
            bytes[parentBytes.length+4] = save;
            bytes[parentBytes.length+4] = glucose[0];
            bytes[parentBytes.length+4] = glucose[1];
            return bytes;
        }

        public ResultPacket(byte[] raw) throws IllegalLengthException, IllegalContentException {
            super(raw);
            if (raw.length != 14)
                throw new IllegalLengthException("Packet length must be 14");

            if (startCode != 0x55)
                throw new IllegalContentException("StartCode must be 0x55");
            if (packetLength != 0x0E)
                throw new IllegalContentException("PacketLength must be 0x0E");
            if (packetCategory != 0x03)
                throw new IllegalContentException("PacketCategory must be 0x03");

            year = raw[3];
            month = raw[4];
            day = raw[5];
            hour = raw[6];
            min = raw[7];
            save = raw[8];
            glucose = new byte[2];
            glucose[0] = raw[9];
            glucose[1] = raw[10];
            checksum = new byte[3];
            checksum[0] = raw[11];
            checksum[1] = raw[12];
            checksum[2] = raw[13];

            //Cannot use calculate checksum of parent as in this protocol the checksum is calculated
            //different
            int measurement = (int)(glucose[0]&0xff) + (int)((glucose[1]&0xff)<<8);
            int big = (int) (startCode &0xff) +
                    (int) (packetLength&0xff) + (int) (packetCategory&0xff) + (int) (year&0xff) +
                    (int) (month&0xff) + (int) (day&0xff) + (int) (hour&0xff) + (int) (min&0xff) +
                    (int) (save&0xff);
            big += measurement;
            int check = (int) (checksum[0]&0xff) + (int) ((checksum[1]&0xff)<<8) + (int) ((checksum[2]&0xff)<<16);

            if( big!=check )
                throw new IllegalContentException("Checksum Does Not Match");
        }
    }

    static public class EndPacket extends DevicePacket{

        public EndPacket(byte[] raw) throws IllegalLengthException, IllegalContentException {
            super(raw);
            if (raw.length != 6)
                throw new IllegalLengthException("Packet length must be 6");
            if (startCode != 0x55)
                throw new IllegalContentException("StartCode must be 0x55");
            if (packetLength != 0x06)
                throw new IllegalContentException("PacketLength must be 0x06");
            if (packetCategory != 0x04)
                throw new IllegalContentException("PacketCategory must be 0x04");
            calculateChecksum(3);

            if( checksum[0]!= raw[3] || checksum[1]!= raw[4] || checksum[2]!= raw[5])
                throw new IllegalContentException("Checksum Does Not Match");
        }
    }



    public ProtocolV1(SerialCommunicator comm){
        serial = comm;
        asyncState = AsyncState.DONE;
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
        AppReplyPacket appReplyPacket = new AppReplyPacket(calendar);
        serial.send(appReplyPacket.to_bytes());
        byte[] reply = serial.recieve();
        try{
            comm.infoPacket = new InfoPacket(reply);
        }catch (IllegalLengthException | IllegalContentException e){
            comm.error = e.toString();
            return comm;
        }
        comm.resultPackets = new ArrayList<ResultPacket>();
        while(true){
            appReplyPacket = new AppReplyPacket(calendar);
            serial.send(appReplyPacket.to_bytes());
            reply = serial.recieve();
            try{
                ResultPacket resultPacket = new ResultPacket(reply);
                if(comm.resultPackets == null)
                    comm.resultPackets = new ArrayList<ResultPacket>();
                comm.resultPackets.add(resultPacket);

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
        AppReplyPacket appReplyPacket = new AppReplyPacket(calendar);
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
                    AppReplyPacket appReplyPacket = new AppReplyPacket(calendar);
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
                    AppReplyPacket appReplyPacket = new AppReplyPacket(calendar);
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
