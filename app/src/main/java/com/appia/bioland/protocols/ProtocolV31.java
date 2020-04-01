package com.appia.bioland.protocols;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ProtocolV31 extends Protocol {
    private SerialCommunicator serial;
    private Communication asyncCom;
    private enum AsyncState {WAITING_INFO_PACKET, WAITING_RESULT_OR_END_PACKET, DONE};
    private AsyncState asyncState;

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

    static public class InfoPacket extends DevicePacket{
        byte versionCode;
        byte clientCode;
        byte modelCode;
        byte typeCode;
        byte retain;
        byte batteryCapacity;
        byte[] rollingCode;

        public InfoPacket(byte[] raw) throws IllegalContentException, IllegalLengthException {
            super(raw);
            if (raw.length != 15)
                throw new IllegalLengthException("Packet length must be 15");

            if (startCode != 0x55)
                throw new IllegalContentException("StartCode must be 0x55");

            if (packetLength != 0x0F)
                throw new IllegalContentException("PacketLength must be 0x0F");

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
            calculateChecksum(1);

            //Double check for inconsistency in documentation
            if( !(checksum[0] == raw[14] || (checksum[0]+(byte)2) == raw[14]) )
                throw new IllegalContentException("Checksum Does Not Match");
        }

        @Override
        protected byte[] getVariablesInByteArray(){
            byte[] parentBytes = super.getVariablesInByteArray();
            byte[] bytes = new byte[parentBytes.length + 11];
            for(int i=0;i<parentBytes.length;i++){
                bytes[i] = parentBytes[i];
            }
            bytes[parentBytes.length+0] = versionCode;
            bytes[parentBytes.length+1] = clientCode;
            bytes[parentBytes.length+2] = modelCode;
            bytes[parentBytes.length+3] = typeCode;
            bytes[parentBytes.length+4] = retain;
            bytes[parentBytes.length+5] = batteryCapacity;
            bytes[parentBytes.length+6] = rollingCode[0];
            bytes[parentBytes.length+7] = rollingCode[1];
            bytes[parentBytes.length+8] = rollingCode[2];
            bytes[parentBytes.length+9] = rollingCode[3];
            bytes[parentBytes.length+10] = rollingCode[4];
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

    static public class ResultPacket extends DevicePacket{
        byte year;
        byte month;
        byte day;
        byte hour;
        byte min;
        byte retain;
        byte[] glucose;

        public ResultPacket(byte[] raw) throws IllegalLengthException, IllegalContentException {
            super(raw);
            if (raw.length != 12)
                throw new IllegalLengthException("Packet length must be 14");
            if (startCode != (byte)0x55)
                throw new IllegalContentException("StartCode must be 0x55");

            if (packetLength != (byte)0x0C)
                throw new IllegalContentException("PacketLength must be 0x0C");

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

            calculateChecksum(1);

            //Double check for inconsitency in documentation
            if(!(checksum[0] == raw[11] || (checksum[0]+(byte)2) == raw[11]))
                throw new IllegalContentException("Checksum Does Not Match");
        }

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
            bytes[parentBytes.length+5] = retain;
            bytes[parentBytes.length+6] = glucose[0];
            bytes[parentBytes.length+7] = glucose[1];
            return bytes;
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
        comm.resultPackets = new ArrayList<>();

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
                        asyncCom.resultPackets = new ArrayList<>();
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
