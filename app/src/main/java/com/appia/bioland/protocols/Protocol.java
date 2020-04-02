package com.appia.bioland.protocols;

import java.util.Calendar;
import java.util.List;

public class Protocol {
    protected SerialCommunicator serial;
    protected Communication asyncCom;
    protected enum AsyncState {WAITING_INFO_PACKET, WAITING_RESULT_OR_END_PACKET, DONE};
    protected AsyncState asyncState;

    public Protocol(SerialCommunicator ser){
        serial = ser;
        ser.connect();
    }

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


    static public class Communication{
        public InfoPacket infoPacket;
        public List<ResultPacket> resultPackets;
        public DevicePacket endPacket;
        public String error;

        public boolean valid(){
            return (infoPacket!=null && resultPackets !=null && endPacket!= null);
        }
    }

    protected AppPacket build_get_info_packet(Calendar calendar){
        return new AppPacket(calendar);
    }

    protected AppPacket build_get_meas_packet(Calendar calendar){
        return new AppPacket(calendar);
    }

    protected InfoPacket build_info_packet(byte[] raw) throws IllegalLengthException, IllegalContentException {
        return new InfoPacket(raw);
    }






//    public Communication communicate(){
//        if (!serial.connected){
//            serial.connect();
//        }
//        if(!serial.connected)
//            return new Communication();
//        Communication comm = new Communication();
//        Calendar calendar = Calendar.getInstance();
//        AppPacket appReplyPacket = build_get_info_packet();
//        serial.send(appReplyPacket.to_bytes());
//        byte[] reply = serial.recieve();
//        try{
//            comm.infoPacket = new InfoPacketV1(reply);
//        }catch (IllegalLengthException | IllegalContentException e){
//            comm.error = e.toString();
//            return comm;
//        }
//        comm.resultPackets = new ArrayList<>();
//        while(true){
//            appReplyPacket = new AppReplyPacket(calendar);
//            serial.send(appReplyPacket.to_bytes());
//            reply = serial.recieve();
//            try{
//                ResultPacket resultPacket = new ResultPacket(reply);
//                if(comm.resultPackets == null)
//                    comm.resultPackets = new ArrayList<>();
//                comm.resultPackets.add(resultPacket);
//
//            }catch (IllegalLengthException | IllegalContentException e){
//                try {
//                    comm.endPacket = new EndPacket(reply);
//                    return comm;
//                } catch (IllegalLengthException | IllegalContentException k){
//                    comm.error = k.toString();
//                    return comm;
//                }
//            }
//        }
//    }

}
