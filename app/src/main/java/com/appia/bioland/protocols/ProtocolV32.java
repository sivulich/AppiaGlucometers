package com.appia.bioland.protocols;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ProtocolV32 extends Protocol {
    // This class implements the protocol V3.2 of communication with the Bioland G-500
    public ProtocolV32(ProtocolCallbacks aCallbacks){
        super(aCallbacks);
        version = new Version("3.2");
    }
    // Define the packets of the protocol V3.2
    static public class AppPacketV32 extends AppPacket{

        byte second;

        public AppPacketV32(Calendar calendar){
            super(calendar);
            second = (byte) calendar.get(Calendar.SECOND);
        }

        @Override
        protected byte[] getVariablesInByteArray(){
            byte[] parentBytes = super.getVariablesInByteArray();
            byte[] bytes = new byte[parentBytes.length + 1];
            for(int i=0;i<parentBytes.length;i++){
                bytes[i] = parentBytes[i];
            }
            bytes[parentBytes.length] = second;
            return bytes;
        }
    }

    static public class AppInfoPacket extends AppPacketV32{
        public AppInfoPacket(Calendar now){
            super(now);
            startCode = 0x5A;
            packetLength = 0x0A;
            packetCategory = 0x00;
            calculateChecksum(1);
        }
    }

    static public class AppDataPacket extends AppPacketV32{
        public AppDataPacket(Calendar now){
            super(now);
            startCode = 0x5A;
            packetLength = 0x0A;
            packetCategory = 0x03;
            calculateChecksum(1);
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

    static public class InfoPacketV32 extends InfoPacket{
        byte batteryCapacity;
        byte modelCode;
        byte typeCode;
        byte[] seriesNumber;

        @Override
        protected byte[] getVariablesInByteArray(){
            byte[] parentBytes = super.getVariablesInByteArray();
            byte[] bytes = new byte[parentBytes.length + 12];
            for(int i=0;i<parentBytes.length;i++){
                bytes[i] = parentBytes[i];
            }
            bytes[parentBytes.length+0] = batteryCapacity;
            bytes[parentBytes.length+1] = modelCode;
            bytes[parentBytes.length+2] = typeCode;
            bytes[parentBytes.length+3] = seriesNumber[0];
            bytes[parentBytes.length+4] = seriesNumber[1];
            bytes[parentBytes.length+5] = seriesNumber[2];
            bytes[parentBytes.length+6] = seriesNumber[3];
            bytes[parentBytes.length+7] = seriesNumber[4];
            bytes[parentBytes.length+8] = seriesNumber[5];
            bytes[parentBytes.length+9] = seriesNumber[6];
            bytes[parentBytes.length+10] = seriesNumber[7];
            bytes[parentBytes.length+11] = seriesNumber[8];
            return bytes;
        }

        public InfoPacketV32(byte[] raw) throws IllegalContentException, IllegalLengthException {
            super(raw);
            if (raw.length != 18)
                throw new IllegalLengthException("Packet length must be 18");

            if (startCode != 0x55)
                throw new IllegalContentException("StartCode must be 0x55");

            if (packetLength != 0x12)
                throw new IllegalContentException("PacketLength must be 0x12");

            if (packetCategory != 0x00)
                throw new IllegalContentException("PacketCategory must be 0x00");

            batteryCapacity = raw[5];
            modelCode = raw[6];
            typeCode = raw[7];
            seriesNumber = new byte[9];
            seriesNumber[0] = raw[8];
            seriesNumber[1] = raw[9];
            seriesNumber[2] = raw[10];
            seriesNumber[3] = raw[11];
            seriesNumber[4] = raw[12];
            seriesNumber[5] = raw[13];
            seriesNumber[6] = raw[14];
            seriesNumber[7] = raw[15];
            seriesNumber[8] = raw[16];

            calculateChecksum(1);
            //Double check for inconsistency in documentation
            if( !(checksum[0] == raw[17] || (checksum[0]+(byte)2) == raw[17]))
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

    static public class ResultPacketV32 extends ResultPacket {


        public ResultPacketV32(byte[] raw) throws IllegalLengthException, IllegalContentException {
            super(raw);
            if (raw.length != 12)
                throw new IllegalLengthException("Packet length must be 14");
            if (startCode != (byte)0x55)
                throw new IllegalContentException("StartCode must be 0x55");
            if (packetLength != (byte)0x0C)
                throw new IllegalContentException("PacketLength must be 0x0C");
            if (packetCategory != (byte)0x03)
                throw new IllegalContentException("PacketCategory must be 0x03");

            calculateChecksum(1);
            if( !(checksum[0] == raw[11] || (checksum[0]+(byte)2) == raw[11]) )
                throw new IllegalContentException("Checksum Does Not Match");

        }
    }

    static public class EndPacket extends DevicePacket{
        byte retain;

        public EndPacket(byte[] raw) throws IllegalLengthException, IllegalContentException {
            super(raw);
            if (raw.length != 5)
                throw new IllegalLengthException("Packet length must be 5");
            if (startCode != (byte)0x55)
                throw new IllegalContentException("StartCode must be 0x55");

            if (packetLength != (byte)0x05)
                throw new IllegalContentException("PacketLength must be 0x05");

            if (packetCategory != (byte)0x05)
                throw new IllegalContentException("PacketCategory must be 0x05");

            retain = raw[3];

            calculateChecksum(1);



            if( !(checksum[0] == raw[4] || (checksum[0]+(byte)2) == raw[4]))
                throw new IllegalContentException("Checksum Does Not Match");

        }
    }
//    @Override
//    protected byte[] build_handshake_packet(){
//        AppHandshakePacket packet = new AppHandshakePacket();
//        return packet.to_bytes();
//    }

    // Override the set of functions that allow the FSM on the general protocol to use protocol V3.2.
    @Override
    protected AppPacket build_get_info_packet(Calendar calendar){
        return new AppInfoPacket(calendar);
    }

    @Override
    protected AppPacket build_get_meas_packet(Calendar calendar){
        return new AppDataPacket(calendar);
    }

    @Override
    protected InfoPacket build_info_packet(byte[] raw) throws IllegalLengthException, IllegalContentException {
        return new InfoPacketV32(raw);
    }

    @Override
    protected ResultPacket build_result_packet(byte[] raw) throws IllegalLengthException, IllegalContentException {
        return new ResultPacketV32(raw);
    }

    @Override
    protected DevicePacket build_end_packet(byte[] raw) throws IllegalLengthException, IllegalContentException {
        return new EndPacket(raw);
    }
}
