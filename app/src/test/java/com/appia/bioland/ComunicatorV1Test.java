package com.appia.bioland;

import org.junit.Test;
import com.appia.bioland.Comunicator.SerialComunicator;
import com.appia.bioland.Comunicator.V1Protocol;

import java.util.Calendar;

import static org.junit.Assert.*;

public class ComunicatorV1Test {
    public class SerialComunicatorTester extends SerialComunicator{
        int status = 0;

        @Override
        public void connect(){
            connected = true;
        }

        @Override
        public boolean send(byte[] data){
            if (status == 0){
                assertEquals(data[0], (byte)0x5A);
                assertEquals(data[1], (byte)0x0B);
                assertEquals(data[2], (byte)0x05);
                status+=1;
            }
            return true;
        }
        @Override
        public byte[] recieve(){
            if (status ==1){
                byte[] packet = {(byte)0x55, (byte)0x10, (byte)0x00, (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04, (byte)0x05, (byte)0x06, (byte)0x07, (byte)0x11, (byte)0x22, (byte)0x33, (byte)0x92, (byte)0x22, (byte)0x33};
                return packet;
            }
            return new byte[16];
        }
    }

    @Test
    public void protocolIsCorrect() {
        SerialComunicatorTester ser = new SerialComunicatorTester();
        V1Protocol protocol = new V1Protocol(ser);
        V1Protocol.Communication comm = protocol.communicate();
        assertNotEquals(comm.infoPacket, null);
    }
}
