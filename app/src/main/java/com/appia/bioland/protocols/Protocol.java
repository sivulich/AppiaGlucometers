package com.appia.bioland.protocols;

import java.util.Calendar;

public class Protocol {
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

}
