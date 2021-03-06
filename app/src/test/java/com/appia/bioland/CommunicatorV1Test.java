package com.appia.bioland;

import org.junit.Test;

import com.appia.bioland.protocols.Protocol;
import com.appia.bioland.protocols.ProtocolCallbacks;
import com.appia.bioland.protocols.ProtocolV1;


import java.util.ArrayList;

import static org.junit.Assert.*;

public class CommunicatorV1Test {
    public class SerialCommunicatorTester implements ProtocolCallbacks{
        int status = 0;

        public void sendData(byte[] data){

            assertEquals(data[0], (byte)0x5A);
            assertEquals(data[1], (byte)0x0B);
            assertEquals(data[2], (byte)0x05);

        }

        public void onCountdownReceived(int value){

        }

        public void onMeasurementsReceived(ArrayList<BiolandMeasurement> aMeasurements){
            assertEquals(aMeasurements.size(),3);
        }


        public void onDeviceInfoReceived(BiolandInfo aInfo){
            //Assert if correct status is recieved
            assertEquals(status,1);
        }


        public void onProtocolError(String aMessage){
            assertEquals(true,false);
        }


        public byte[] recieve(){
            if (status ==0){
                byte[] packet = {(byte)0x55, (byte)0x10, (byte)0x00, (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04, (byte)0x05, (byte)0x06, (byte)0x07, (byte)0x11, (byte)0x22, (byte)0x33, (byte)0x92, (byte)0x22, (byte)0x33};
                status +=1;
                return packet;
            }
            else if (status == 1 || status == 2 || status == 3 ){
                byte[] packet = {(byte)0x55, (byte)0x0e, (byte)0x03, (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04, (byte)0x05, (byte)0x00, (byte)0x11, (byte)0x22, (byte)0x86, (byte)0x22, (byte)0x00};
                status +=1;
                return packet;
            }
            else if (status== 4){
                byte[] packet = {(byte)0x55, (byte)0x06, (byte)0x04, (byte)0x61, (byte)0x00, (byte)0x00};
                status +=1;
                return packet;
            }
            return new byte[16];
        }
    }

    @Test
    public void protocolAsyncV1IsCorrect() {
        SerialCommunicatorTester ser = new SerialCommunicatorTester();
        ProtocolV1 protocol = new ProtocolV1(ser);

        //Sent firts packet
        boolean start = protocol.requestMeasurements();
        assertEquals(start, true);
        assertEquals(protocol.state, Protocol.State.WAITING_INFO_PACKET);
        //Receive INFO packet
        byte[] packet = ser.recieve();
        protocol.onDataReceived(packet);
        assertEquals(protocol.state, Protocol.State.WAITING_RESULT_OR_END_PACKET);
        //Receive 3 Data packets
        for(int i=0;i<3 ;i++)
        {
            packet = ser.recieve();
            protocol.onDataReceived(packet);
            assertEquals(protocol.state, Protocol.State.WAITING_RESULT_OR_END_PACKET);
        }

        //Receive END packet
        packet = ser.recieve();
        protocol.onDataReceived(packet);
        assertEquals(protocol.state, Protocol.State.WAITING_MEASUREMENT);
    }

}
