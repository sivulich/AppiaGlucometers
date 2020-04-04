package com.appia.bioland.protocols;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ProtocolV31 extends Protocol {
    // This class implements the protocol V3.1 of communication with the Bioland G-500
    public ProtocolV31(ProtocolCallbacks aCallbacks){
        super(aCallbacks);
        version = new Version("3.1");
    }
    // Define the packets of the protocol V3.1
    static public class AppPacketV31 extends AppPacket{
        byte second;

        public AppPacketV31(Calendar calendar){
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

    static public class AppInfoPacket extends AppPacketV31{
        public AppInfoPacket(Calendar now){
            super(now);
            startCode = 0x5A;
            packetLength = 0x0A;
            packetCategory = 0x00;
            calculateChecksum(1);
        }
    }

    static public class AppDataPacket extends AppPacketV31{
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

    static public class InfoPacketV31 extends InfoPacket{
        byte modelCode;
        byte typeCode;
        byte retain;
        byte batteryCapacity;
        byte[] rollingCode;

        public InfoPacketV31(byte[] raw) throws IllegalContentException, IllegalLengthException {
            super(raw);
            if (raw.length != 15)
                throw new IllegalLengthException("Packet length must be 15");

            if (startCode != 0x55)
                throw new IllegalContentException("StartCode must be 0x55");

            if (packetLength != 0x0F)
                throw new IllegalContentException("PacketLength must be 0x0F");

            if (packetCategory != 0x00)
                throw new IllegalContentException("PacketCategory must be 0x00");

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
            calculateChecksum(1);

            //Double check for inconsistency in documentation
            if( !(checksum[0] == raw[14] || (checksum[0]+(byte)2) == raw[14]) )
                throw new IllegalContentException("Checksum Does Not Match");
        }

        @Override
        protected byte[] getVariablesInByteArray(){
            byte[] parentBytes = super.getVariablesInByteArray();
            byte[] bytes = new byte[parentBytes.length + 9];
            for(int i=0;i<parentBytes.length;i++){
                bytes[i] = parentBytes[i];
            }
            bytes[parentBytes.length+0] = modelCode;
            bytes[parentBytes.length+1] = typeCode;
            bytes[parentBytes.length+2] = retain;
            bytes[parentBytes.length+3] = batteryCapacity;
            bytes[parentBytes.length+4] = rollingCode[0];
            bytes[parentBytes.length+5] = rollingCode[1];
            bytes[parentBytes.length+6] = rollingCode[2];
            bytes[parentBytes.length+7] = rollingCode[3];
            bytes[parentBytes.length+8] = rollingCode[4];
            return bytes;
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

    static public class ResultPacketV31 extends ResultPacket {


        public ResultPacketV31(byte[] raw) throws IllegalLengthException, IllegalContentException {
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

    // Override the set of functions that allow the FSM on the general protocol to use protocol V3.1.
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
        return new InfoPacketV31(raw);
    }
    @Override
    protected ResultPacket build_result_packet(byte[] raw) throws IllegalLengthException, IllegalContentException {
        return new ResultPacketV31(raw);
    }
    @Override
    protected DevicePacket build_end_packet(byte[] raw) throws IllegalLengthException, IllegalContentException {
        return new EndPacket(raw);
    }
}
