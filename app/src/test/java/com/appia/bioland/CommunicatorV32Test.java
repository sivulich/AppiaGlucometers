package com.appia.bioland;
import com.appia.bioland.protocols.ProtocolV32;
import com.appia.bioland.protocols.SerialCommunicator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class CommunicatorV32Test {
    public class SerialCommunicatorTester extends SerialCommunicator{
        int status = 0;

        @Override
        public void connect(){
            connected = true;
        }

        @Override
        public boolean send(byte[] data){
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
            return true;
        }
        @Override
        public byte[] recieve(){
            if (status ==1){
                byte[] packet = {(byte)0x55, (byte)0x12, (byte)0x00, (byte)0x20, (byte)0x03, (byte)0x32, (byte)0x02, (byte)0x00, (byte)0x11, (byte)0x22, (byte)0x33, (byte)0x44, (byte)0x55, (byte)0x66, (byte)0x77, (byte)0x88, (byte)0x99,(byte)0xbb};
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
    public void protocolV32IsCorrect() {
        SerialCommunicatorTester ser = new SerialCommunicatorTester();
        ProtocolV32 protocol = new ProtocolV32(ser);
        ProtocolV32.Communication comm = protocol.communicate();
        assertNotEquals(comm.infoPacket, null);
        assertEquals(comm.resultPackets.size(), 8);
        assertNotEquals(comm.endPacket, null);
    }
}
