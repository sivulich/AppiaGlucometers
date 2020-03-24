package com.appia.bioland;

import java.util.Calendar;
import java.util.Date;

public class Comunicator {
    // TODO: Remove this from here
    static public class SerialComunicator{
        boolean connected = false;

        public void connect(){
            connected = true;
        }


        public boolean send(byte[] data){
            return true;
        }

        public byte[] recieve(){
            return new byte[16];
        }

    }


    static public class V1Protocol{
        private SerialComunicator serial;

        static public class AppPacket{
            byte startCode;
            byte packetLength;
            byte packetCategory;
            byte year;
            byte month;
            byte day;
            byte hour;
            byte min;
            byte second;
            byte[] checksum;

            public byte[] to_bytes(){
                byte[] packet = new byte[12];
                packet[0] = startCode;
                packet[1] = packetLength;
                packet[2] = packetCategory;
                packet[3] = year;
                packet[4] = month;
                packet[5] = day;
                packet[6] = hour;
                packet[7] = min;
                packet[8] = second;
                packet[9] = checksum[0];
                packet[10] = checksum[1];
                packet[11] = checksum[2];
                return packet;
            }
        }
        static public class AppReplyPacket extends AppPacket{
            public AppReplyPacket(Calendar now){
                startCode = 0x5A;
                packetLength = 0x0B;
                packetCategory = 0x05;
                year = (byte) now.get(Calendar.YEAR);
                month = (byte) now.get(Calendar.MONTH);
                day = (byte) now.get(Calendar.DAY_OF_MONTH);
                hour = (byte) now.get(Calendar.HOUR);
                min = (byte) now.get(Calendar.MINUTE);
                second = (byte) now.get(Calendar.SECOND);
                checksum = new byte[3];
                checksum[0] = (byte)(startCode + packetLength + packetCategory + year + month + day + hour + min + second);
            }
        }
        static public class AppTerminationPacket extends AppPacket{
            public AppTerminationPacket(Calendar now){
                startCode = 0x5A;
                packetLength = 0x0B;
                packetCategory = 0x06;
                year = (byte) now.get(Calendar.YEAR);
                month = (byte) now.get(Calendar.MONTH);
                day = (byte) now.get(Calendar.DAY_OF_MONTH);
                hour = (byte) now.get(Calendar.HOUR);
                min = (byte) now.get(Calendar.MINUTE);
                second = (byte) now.get(Calendar.SECOND);
                checksum = new byte[3];
                checksum[0] = (byte)(startCode + packetLength + packetCategory + year + month + day + hour + min + second);
            }
        }
        static public class InfoPacket{
            byte startCode;
            byte packetLength;
            byte packetCategory;
            byte versionCode;
            byte clientCode;
            byte modelCode;
            byte typeCode;
            byte userID;
            byte productionYear;
            byte productionMonth;
            byte[] serialNumber;
            byte[] checksum;

            public InfoPacket(byte[] raw){
                if (raw.length != 16)
                    throw new IllegalArgumentException("Packet length must be 16");
                startCode = raw[0];
                packetLength = raw[1];
                packetCategory = raw[2];
                versionCode = raw[3];
                clientCode = raw[4];
                modelCode = raw[5];
                typeCode = raw[6];
                userID = raw[7];
                productionYear = raw[8];
                productionMonth = raw[9];
                serialNumber = new byte[3];
                serialNumber[0] = raw[10];
                serialNumber[1] = raw[11];
                serialNumber[2] = raw[12];
                checksum = new byte[3];
                checksum[0] = raw[13];
                checksum[1] = raw[14];
                checksum[2] = raw[15];
            }
        }
        static public class ResultPacket{
            byte startCode;
            byte packetLength;
            byte packetCategory;
            byte year;
            byte month;
            byte day;
            byte hour;
            byte min;
            byte save;
            byte[] glucose;
            byte[] checksum;

            public ResultPacket(byte[] raw){
                if (raw.length != 14)
                    throw new IllegalArgumentException("Packet length must be 14");
                startCode = raw[0];
                packetLength = raw[1];
                packetCategory = raw[2];
                year = raw[3];
                month = raw[4];
                day = raw[5];
                hour = raw[6];
                min = raw[7];
                save = raw[8];
                glucose = new byte[2];
                glucose[0] = raw[9];
                glucose[1] = raw[10];
                checksum = new byte[3];
                checksum[0] = raw[11];
                checksum[1] = raw[12];
                checksum[2] = raw[13];
            }
        }
        static public class EndPacket{
            byte startCode;
            byte packetLength;
            byte packetCategory;
            byte[] checksum;

            public EndPacket(byte[] raw){
                if (raw.length != 6)
                    throw new IllegalArgumentException("Packet length must be 6");
                startCode = raw[0];
                packetLength = raw[1];
                packetCategory = raw[2];
                checksum = new byte[3];
                checksum[0] = raw[3];
                checksum[1] = raw[4];
                checksum[2] = raw[5];
            }
        }

        V1Protocol(SerialComunicator comm){
            serial = comm;
            comm.connect();
        }



    }

}
