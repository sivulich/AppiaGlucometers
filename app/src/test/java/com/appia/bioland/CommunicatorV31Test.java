//package com.appia.bioland;
//
//import org.junit.Test;
//
//import com.appia.bioland.protocols.ProtocolV2;
//import com.appia.bioland.protocols.ProtocolV31;
//import com.appia.bioland.protocols.SerialCommunicator;
//
//
//import static org.junit.Assert.*;
//
//public class CommunicatorV31Test {
//    public class SerialCommunicatorTester extends SerialCommunicator{
//        int status = 0;
//
//        @Override
//        public void connect(){
//            connected = true;
//        }
//
//        @Override
//        public boolean send(byte[] data){
//            if (status == 0){
//                assertEquals(data[0], (byte)0x5A);
//                assertEquals(data[1], (byte)0x0A);
//                assertEquals(data[2], (byte)0x00);
//                status+=1;
//            }
//            else if(status>1){
//                assertEquals(data[0], (byte)0x5A);
//                assertEquals(data[1], (byte)0x0A);
//                assertEquals(data[2], (byte)0x03);
//            }
//            return true;
//        }
//        @Override
//        public byte[] recieve(){
//            if (status ==1){
//                byte[] packet = {(byte)0x55, (byte)0x0f, (byte)0x00, (byte)0x2a, (byte)0x03, (byte)0x02, (byte)0x27, (byte)0x00, (byte)0x11, (byte)0x80, (byte)0x06, (byte)0x00, (byte)0x33, (byte)0x0a, (byte)0x90};
//                status +=1;
//                return packet;
//            }
//            else if (status == 2 ){
//                byte[] packet = {(byte)0x55, (byte)0x0c, (byte)0x03, (byte)0x0e, (byte)0x01, (byte)0x01, (byte)0x05, (byte)0x19, (byte)0x00, (byte)0xc3, (byte)0x02, (byte)0x57};
//                status +=1;
//                return packet;
//            }
//            else if (status == 3){
//                byte[] packet = {(byte)0x55, (byte)0x0c, (byte)0x03, (byte)0x0e, (byte)0x01, (byte)0x01, (byte)0x05, (byte)0x19, (byte)0x00, (byte)0x12, (byte)0x00, (byte)0xa4};
//                status +=1;
//                return packet;
//            }
//            else if (status == 4){
//                byte[] packet = {(byte)0x55, (byte)0x0c, (byte)0x03, (byte)0x0e, (byte)0x01, (byte)0x01, (byte)0x05, (byte)0x18, (byte)0x00, (byte)0xa8, (byte)0x00, (byte)0x39};
//                status +=1;
//                return packet;
//            }
//            else if (status == 5 || status == 6 || status == 7 || status == 8 || status == 9){
//                byte[] packet = {(byte)0x55, (byte)0x0c, (byte)0x03, (byte)0x0e, (byte)0x01, (byte)0x01, (byte)0x05, (byte)0x17, (byte)0x00, (byte)0xb1, (byte)0x00, (byte)0x41};
//                status +=1;
//                return packet;
//            }
//            else if (status== 10){
//                byte[] packet = {(byte)0x55, (byte)0x05, (byte)0x05, (byte)0x00, (byte)0x5F};
//                status +=1;
//                return packet;
//            }
//            return new byte[16];
//        }
//    }
//
//    @Test
//    public void protocolV31IsCorrect() {
//        SerialCommunicatorTester ser = new SerialCommunicatorTester();
//        ProtocolV31 protocol = new ProtocolV31(ser);
//        ProtocolV31.Communication comm = protocol.communicate();
//        assertNotEquals(comm.infoPacket, null);
//        assertEquals(comm.resultPackets.size(), 8);
//        assertNotEquals(comm.endPacket, null);
//    }
//
//    @Test
//    public void protocolAsyncV31IsCorrect() {
//        SerialCommunicatorTester ser = new SerialCommunicatorTester();
//        ProtocolV31 protocol = new ProtocolV31(ser);
//
//        //Sent firts packet
//        boolean start = protocol.asyncStartCommunication();
//        ProtocolV31.Communication comm = protocol.asyncGetCommunication();
//        assertEquals(start, true);
//        assertEquals(protocol.asyncDoneCommunication(), false);
//        assertEquals(comm.infoPacket, null);
//        assertEquals(comm.resultPackets, null);
//        assertEquals(comm.endPacket, null);
//
//        //Receive INFO packet
//        byte[] packet = ser.recieve();
//        protocol.asyncCallbackReceive(packet);
//        comm = protocol.asyncGetCommunication();
//        assertEquals(protocol.asyncDoneCommunication(), false);
//        assertNotEquals(comm.infoPacket, null);
//        assertEquals(comm.resultPackets, null);
//        assertEquals(comm.endPacket, null);
//
//        //Receive 8 Data packets
//        for(int i=0;i<8 ;i++)
//        {
//            packet = ser.recieve();
//            protocol.asyncCallbackReceive(packet);
//            comm = protocol.asyncGetCommunication();
//            assertEquals(protocol.asyncDoneCommunication(), false);
//            assertNotEquals(comm.infoPacket, null);
//            assertNotEquals(comm.resultPackets, null);
//            assertEquals(comm.resultPackets.size(), i+1);
//            assertEquals(comm.endPacket, null);
//        }
//
//        //Receive END packet
//        packet = ser.recieve();
//        protocol.asyncCallbackReceive(packet);
//        comm = protocol.asyncGetCommunication();
//        assertEquals(protocol.asyncDoneCommunication(), true);
//        assertNotEquals(comm.infoPacket, null);
//        assertNotEquals(comm.resultPackets, null);
//        assertNotEquals(comm.endPacket, null);
//    }
//}
