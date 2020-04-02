package com.appia.bioland.protocols;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ProtocolV1 extends Protocol {

    public ProtocolV1(SerialCommunicator comm){
        super(comm);
    }

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

    static public class InfoPacketV1 extends InfoPacket{
        byte modelCode;
        byte typeCode;
        byte userID;
        byte productionYear;
        byte productionMonth;
        byte[] serialNumber;

        @Override
        protected byte[] getVariablesInByteArray(){
            byte[] parentBytes = super.getVariablesInByteArray();
            byte[] bytes = new byte[parentBytes.length + 8];
            for(int i=0;i<parentBytes.length;i++){
                bytes[i] = parentBytes[i];
            }
            bytes[parentBytes.length+0] = modelCode;
            bytes[parentBytes.length+1] = typeCode;
            bytes[parentBytes.length+2] = userID;
            bytes[parentBytes.length+3] = productionYear;
            bytes[parentBytes.length+4] = productionMonth;
            bytes[parentBytes.length+5] = serialNumber[0];
            bytes[parentBytes.length+6] = serialNumber[1];
            bytes[parentBytes.length+7] = serialNumber[2];
            return bytes;
        }

        public InfoPacketV1(byte[] raw) throws IllegalContentException, IllegalLengthException {
            super(raw);
            if (raw.length != 16)
                throw new IllegalLengthException("Packet length must be 16");

            if (startCode != 0x55)
                throw new IllegalContentException("StartCode must be 0x55");

            if (packetLength != 0x10)
                throw new IllegalContentException("PacketLength must be 0x10");

            if (packetCategory != 0x00)
                throw new IllegalContentException("PacketCategory must be 0x00");

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

    static public class ResultPacketV1 extends ResultPacket{

        public ResultPacketV1(byte[] raw) throws IllegalLengthException, IllegalContentException {
            super(raw);

            if (raw.length != 14)
                throw new IllegalLengthException("Packet length must be 14");
            if (startCode != 0x55)
                throw new IllegalContentException("StartCode must be 0x55");
            if (packetLength != 0x0E)
                throw new IllegalContentException("PacketLength must be 0x0E");
            if (packetCategory != 0x03)
                throw new IllegalContentException("PacketCategory must be 0x03");

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
                    (int) (retain&0xff);
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

    @Override
    protected AppPacket build_get_info_packet(Calendar calendar){
        return new AppReplyPacket(calendar);
    }
    @Override
    protected AppPacket build_get_meas_packet(Calendar calendar){
        return new AppReplyPacket(calendar);
    }
    @Override
    protected InfoPacket build_info_packet(byte[] raw) throws IllegalLengthException, IllegalContentException {
        return new InfoPacketV1(raw);
    }
    @Override
    protected ResultPacket build_result_packet(byte[] raw) throws IllegalLengthException, IllegalContentException {
        return new ResultPacketV1(raw);
    }
    @Override
    protected DevicePacket build_end_packet(byte[] raw) throws IllegalLengthException, IllegalContentException {
        return new EndPacket(raw);
    }

}
