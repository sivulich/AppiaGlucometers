package com.appia.bioland;

import org.junit.Test;

import com.appia.bioland.protocols.ProtocolV2;
import com.appia.bioland.protocols.ProtocolCallbacks;


import java.util.ArrayList;

import static org.junit.Assert.*;

public class CommunicatorV2Test {
    public class SerialCommunicatorTester implements ProtocolCallbacks{
        int status = 0;

        public void sendData(byte[] data){
            if (status == 0){
                assertEquals(data[0], (byte)0x5A);
                assertEquals(data[1], (byte)0x0A);
                assertEquals(data[2], (byte)0x00);
                status+=1;
            }
            else if(status>1){
                assertEquals(data[0], (byte)0x5A);
                assertEquals(data[1], (byte)0x0A);
                assertEquals(data[2], (byte)0x03);
            }
        }

        public void onMeasurementsReceived(ArrayList<BiolandMeasurement> aMeasurements){
            assertEquals(aMeasurements.size(),8);
        }

        public void onCountdownReceived(int value){

        }

        public void onDeviceInfoReceived(BiolandInfo aInfo){
            //Assert if correct status is recieved
            assertEquals(status,2);
        }


        public void onProtocolError(String aMessage){
            assertEquals(true,false);
        }


        public byte[] recieve(){
            if (status ==1){
                byte[] packet = {(byte)0x55, (byte)0x0f, (byte)0x00, (byte)0x2a, (byte)0x03, (byte)0x02, (byte)0x27, (byte)0x00, (byte)0x11, (byte)0x80, (byte)0x06, (byte)0x00, (byte)0x33, (byte)0x0a, (byte)0x90};
                status +=1;
                return packet;
            }
            else if (status == 2 ){
                byte[] packet = {(byte)0x55, (byte)0x0c, (byte)0x03, (byte)0x0e, (byte)0x01, (byte)0x01, (byte)0x05, (byte)0x19, (byte)0x00, (byte)0xc3, (byte)0x02, (byte)0x57};
                status +=1;
                return packet;
            }
            else if (status == 3){
                byte[] packet = {(byte)0x55, (byte)0x0c, (byte)0x03, (byte)0x0e, (byte)0x01, (byte)0x01, (byte)0x05, (byte)0x19, (byte)0x00, (byte)0x12, (byte)0x00, (byte)0xa4};
                status +=1;
                return packet;
            }
            else if (status == 4){
                byte[] packet = {(byte)0x55, (byte)0x0c, (byte)0x03, (byte)0x0e, (byte)0x01, (byte)0x01, (byte)0x05, (byte)0x18, (byte)0x00, (byte)0xa8, (byte)0x00, (byte)0x39};
                status +=1;
                return packet;
            }
            else if (status == 5 || status == 6 || status == 7 || status == 8 || status == 9){
                byte[] packet = {(byte)0x55, (byte)0x0c, (byte)0x03, (byte)0x0e, (byte)0x01, (byte)0x01, (byte)0x05, (byte)0x17, (byte)0x00, (byte)0xb1, (byte)0x00, (byte)0x41};
                status +=1;
                return packet;
            }
            else if (status== 10){
                byte[] packet = {(byte)0x55, (byte)0x05, (byte)0x05, (byte)0x00, (byte)0x5F};
                status +=1;
                return packet;
            }
            return new byte[16];
        }
    }

    @Test
    public void protocolAsyncV2IsCorrect() {
        SerialCommunicatorTester ser = new SerialCommunicatorTester();
        ProtocolV2 protocol = new ProtocolV2(ser);

        //Sent firts packet
        boolean start = protocol.startCommunication();
        ProtocolV2.Communication comm = protocol.getCommunication();
        assertEquals(start, true);
        assertEquals(protocol.doneCommunication(), false);
        assertEquals(comm.infoPacket, null);
        assertEquals(comm.resultPackets, null);
        assertEquals(comm.endPacket, null);

        //Receive INFO packet
        byte[] packet = ser.recieve();
        protocol.onDataReceived(packet);
        comm = protocol.getCommunication();
        assertEquals(protocol.doneCommunication(), false);
        assertNotEquals(comm.infoPacket, null);
        assertEquals(comm.resultPackets, null);
        assertEquals(comm.endPacket, null);

        //Receive 8 Data packets
        for(int i=0;i<8 ;i++)
        {
            packet = ser.recieve();
            protocol.onDataReceived(packet);
            comm = protocol.getCommunication();
            assertEquals(protocol.doneCommunication(), false);
            assertNotEquals(comm.infoPacket, null);
            assertNotEquals(comm.resultPackets, null);
            assertEquals(comm.resultPackets.size(), i+1);
            assertEquals(comm.endPacket, null);
        }

        //Receive END packet
        packet = ser.recieve();
        protocol.onDataReceived(packet);
        comm = protocol.getCommunication();
        assertEquals(protocol.doneCommunication(), true);
        assertNotEquals(comm.infoPacket, null);
        assertNotEquals(comm.resultPackets, null);
        assertNotEquals(comm.endPacket, null);
    }
}
