package com.appia.bioland;
import com.appia.bioland.protocols.Protocol;
import com.appia.bioland.protocols.ProtocolV32;
import com.appia.bioland.protocols.ProtocolCallbacks;

import org.junit.Test;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class CommunicatorV32Test {
    public class SerialCommunicatorTesterActive implements ProtocolCallbacks{
        int status = 0;
        public void sendData(byte[] data){
            if (status == 0){
                assertEquals(data[0], (byte)0x5A);
                assertEquals(data[1], (byte)0x0A);
                assertEquals(data[2], (byte)0x00);
                assertEquals(data.length, 10);
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
        public void onDeviceInfoReceived(BiolandInfo aInfo){
            //Assert if correct status is recieved
            assertEquals(status,2);
        }
        public void onProtocolError(String aMessage){
            assertEquals(true,false);
        }
        public void onCountdownReceived(int value){

        }
        public byte[] recieve(){
            if (status ==1){
                byte[] packet = {(byte)0x55, (byte)0x12, (byte)0x00, (byte)0x20, (byte)0x03, (byte)0x32, (byte)0x02, (byte)0x00, (byte)0x11, (byte)0x22, (byte)0x33, (byte)0x44, (byte)0x55, (byte)0x66, (byte)0x77, (byte)0x88, (byte)0x99,(byte)0xbd};
                status +=1;
                return packet;
            }
            else if (status == 2 ){
                byte[] packet = {(byte)0x55, (byte)0x0c, (byte)0x03, (byte)0x0e, (byte)0x01, (byte)0x01, (byte)0x05, (byte)0x19, (byte)0x00, (byte)0xc3, (byte)0x02, (byte)0x59};
                status +=1;
                return packet;
            }
            else if (status == 3){
                byte[] packet = {(byte)0x55, (byte)0x0c, (byte)0x03, (byte)0x0e, (byte)0x01, (byte)0x01, (byte)0x05, (byte)0x19, (byte)0x00, (byte)0x12, (byte)0x00, (byte)0xa6};
                status +=1;
                return packet;
            }
            else if (status == 4){
                byte[] packet = {(byte)0x55, (byte)0x0c, (byte)0x03, (byte)0x0e, (byte)0x01, (byte)0x01, (byte)0x05, (byte)0x18, (byte)0x00, (byte)0xa8, (byte)0x00, (byte)0x3b};
                status +=1;
                return packet;
            }
            else if (status == 5 || status == 6 || status == 7 || status == 8 || status == 9){
                byte[] packet = {(byte)0x55, (byte)0x0c, (byte)0x03, (byte)0x0e, (byte)0x01, (byte)0x01, (byte)0x05, (byte)0x17, (byte)0x00, (byte)0xb1, (byte)0x00, (byte)0x43};
                status +=1;
                return packet;
            }
            else if (status== 10){
                byte[] packet = {(byte)0x55, (byte)0x05, (byte)0x05, (byte)0x00, (byte)0x61};
                status +=1;
                return packet;
            }
            return new byte[16];
        }
    }


    public class SerialCommunicatorTesterPasive implements ProtocolCallbacks{
        int status = 0;
        public void sendData(byte[] data){
            if (status == 0){
                // Test if sending a info request on connect
                assertEquals(data[0], (byte)0x5A);
                assertEquals(data[1], (byte)0x0A);
                assertEquals(data[2], (byte)0x00);
                assertEquals(data.length, 10);
            }
            else if(status>7){
                // Test if sending a request measurement
                assertEquals(data[0], (byte)0x5A);
                assertEquals(data[1], (byte)0x0A);
                assertEquals(data[2], (byte)0x03);
                assertEquals(data.length, 10);
            }
        }
        public void onMeasurementsReceived(ArrayList<BiolandMeasurement> aMeasurements){
            assertEquals(aMeasurements.size(),9);
        }
        public void onDeviceInfoReceived(BiolandInfo aInfo){
            //Assert if correct status is recieved
            assertEquals(status,1);
        }
        public void onProtocolError(String aMessage){
            assertEquals(true,false);
        }
        public void onCountdownReceived(int value){
            assertEquals(5-status+1, value);
        }
        public byte[] recieve(){
            if (status==0){
                byte[] packet = {(byte)0x55, (byte)0x12, (byte)0x00, (byte)0x20, (byte)0x03, (byte)0x32, (byte)0x02, (byte)0x00, (byte)0x11, (byte)0x22, (byte)0x33, (byte)0x44, (byte)0x55, (byte)0x66, (byte)0x77, (byte)0x88, (byte)0x99,(byte)0xbd};
                status +=1;
                return packet;
            }
            else if (1<=status && status <= 5){
                byte[] packet = {(byte)0x55, (byte)0x06, (byte)0x02 , (byte)0x00, (byte)(5-status), (byte)((0x55+0x06+0x02+4-status+2)&0xff)};
                status +=1;
                return packet;
            }
            else if (status == 6){
                byte[] packet = {(byte)0x55, (byte)0x0c, (byte)0x03, (byte)0x0e, (byte)0x01, (byte)0x01, (byte)0x05, (byte)0x19, (byte)0x00, (byte)0x12, (byte)0x00, (byte)0xa6};
                status +=1;
                return packet;
            }
            else if (status == 7 ){
                byte[] packet = {(byte)0x55, (byte)0x0c, (byte)0x03, (byte)0x0e, (byte)0x01, (byte)0x01, (byte)0x05, (byte)0x19, (byte)0x00, (byte)0xc3, (byte)0x02, (byte)0x59};
                status +=1;
                return packet;
            }
            else if (status == 8){
                byte[] packet = {(byte)0x55, (byte)0x0c, (byte)0x03, (byte)0x0e, (byte)0x01, (byte)0x01, (byte)0x05, (byte)0x19, (byte)0x00, (byte)0x12, (byte)0x00, (byte)0xa6};
                status +=1;
                return packet;
            }
            else if (status == 9){
                byte[] packet = {(byte)0x55, (byte)0x0c, (byte)0x03, (byte)0x0e, (byte)0x01, (byte)0x01, (byte)0x05, (byte)0x18, (byte)0x00, (byte)0xa8, (byte)0x00, (byte)0x3b};
                status +=1;
                return packet;
            }
            else if ( status == 10 || status == 11 || status == 12 || status == 13 || status==14){
                byte[] packet = {(byte)0x55, (byte)0x0c, (byte)0x03, (byte)0x0e, (byte)0x01, (byte)0x01, (byte)0x05, (byte)0x17, (byte)0x00, (byte)0xb1, (byte)0x00, (byte)0x43};
                status +=1;
                return packet;
            }
            else if (status== 15){
                byte[] packet = {(byte)0x55, (byte)0x05, (byte)0x05, (byte)0x00, (byte)0x61};
                status +=1;
                return packet;
            }
            return new byte[16];
        }
    }
    @Test
    public void protocolAsyncPasiveV32IsCorrect()  {
        SerialCommunicatorTesterPasive ser = new SerialCommunicatorTesterPasive();
        Protocol protocol = new ProtocolV32(ser);
        protocol.testing_mode = true;
        //Sent firts packet
        byte[] packet;

        // Connect
        assertEquals(protocol.state, Protocol.State.DISCONNECTED);
        protocol.connect();
        assertEquals(protocol.state, Protocol.State.WAITING_INFO_PACKET);
        // Receive info packet
        packet = ser.recieve();
        protocol.onDataReceived(packet);
        assertEquals(protocol.state, Protocol.State.WAITING_MEASUREMENT);

        // Receive timing packets
        for(int i=0;i<=4;i++){
            packet = ser.recieve();
            protocol.onDataReceived(packet);
            if (i<3)
                assertEquals(protocol.state, Protocol.State.WAITING_MEASUREMENT);
        }
        assertEquals(protocol.state, Protocol.State.WAITING_RESULT_OR_END_PACKET);

        // Receive result packet
        packet = ser.recieve();
        protocol.onDataReceived(packet);
        assertEquals(protocol.state, Protocol.State.WAITING_RESULT_OR_END_PACKET);

        //Receive 8 more result packets
        for(int i=0;i<8 ;i++)
        {
            packet = ser.recieve();
            protocol.onDataReceived(packet);
            assertEquals(protocol.state, Protocol.State.WAITING_RESULT_OR_END_PACKET);
        }

        //Receive END packet
        packet = ser.recieve();
        protocol.onDataReceived(packet);
        assertEquals(protocol.state, Protocol.State.WAITING_MEASUREMENT);

        protocol.disconnect();
        assertEquals(protocol.state, Protocol.State.DISCONNECTED);
    }

}
