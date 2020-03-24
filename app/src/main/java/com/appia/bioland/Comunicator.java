package com.appia.bioland;

import java.math.BigInteger;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

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

    static public class IllegalLengthException extends Exception{
        String error;

        public String toString() {
            return "IllegalLength[" + error + "]";
        }
        IllegalLengthException(String s){
            error = s;
        }
    }
    static public class IllegalContentException extends Exception{
        String error;

        public String toString() {
            return "IllegalContent[" + error + "]";
        }
        IllegalContentException(String s){
            error = s;
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
                BigInteger big = BigInteger.valueOf((startCode + packetLength + packetCategory + year + month + day + hour + min + second));
                byte[] check = big.toByteArray();
                for(int i=0; i< 3; i++)
                {
                    if (i< check.length)
                        checksum[i] = check[i];
                    else
                        checksum[i] = 0x00;
                }

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
                BigInteger big = BigInteger.valueOf((startCode + packetLength + packetCategory + year + month + day + hour + min + second));
                byte[] check = big.toByteArray();
                checksum[0] = check[0];
                checksum[1] = check[1];
                checksum[2] = check[2];
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

            public InfoPacket(byte[] raw) throws IllegalContentException, IllegalLengthException {
                if (raw.length != 16)
                    throw new IllegalLengthException("Packet length must be 16");

                startCode = raw[0];
                if (startCode != 0x55)
                    throw new IllegalContentException("StartCode must be 0x55");

                packetLength = raw[1];
                if (packetLength != 0x10)
                    throw new IllegalContentException("PacketLength must be 0x10");

                packetCategory = raw[2];
                if (packetCategory != 0x00)
                    throw new IllegalContentException("PacketCategory must be 0x00");

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
                int serial =  (int)(serialNumber[0]&0xff) + (int)((serialNumber[1]&0xff)<<8) + (int)((serialNumber[2]&0xff)<<16) ;
                int big = (int) (startCode &0xff) +
                        (int) (packetLength&0xff) + (int) (packetCategory&0xff) + (int) (versionCode&0xff) +
                        (int) (clientCode&0xff) + (int) (modelCode&0xff) + (int) (typeCode&0xff) + (int) (userID&0xff) +
                        (int) (productionYear&0xff) + (int) (productionMonth&0xff);
                big += serial;
                int check = (int) (checksum[0]&0xff) + (int) ((checksum[1]&0xff)<<8) + (int) ((checksum[2]&0xff)<<16);
                if(big != check)
                    throw new IllegalContentException("Checksum Does Not Match");



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

            public ResultPacket(byte[] raw) throws IllegalLengthException, IllegalContentException {
                if (raw.length != 14)
                    throw new IllegalLengthException("Packet length must be 14");
                startCode = raw[0];
                if (startCode != 0x55)
                    throw new IllegalContentException("StartCode must be 0x55");

                packetLength = raw[1];
                if (packetLength != 0x0E)
                    throw new IllegalContentException("PacketLength must be 0x0E");

                packetCategory = raw[2];
                if (packetCategory != 0x03)
                    throw new IllegalContentException("PacketCategory must be 0x03");

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


                int measurement = (int)(glucose[0]&0xff) + (int)((glucose[1]&0xff)<<8) + (int)((glucose[2]&0xff)<<16);
                int big = (int) (startCode &0xff) +
                          (int) (packetLength&0xff) + (int) (packetCategory&0xff) + (int) (year&0xff) +
                          (int) (month&0xff) + (int) (day&0xff) + (int) (hour&0xff) + (int) (min&0xff) +
                          (int) (save&0xff);
                big += measurement;
                int check = (int) (checksum[0]&0xff) + (int) ((checksum[1]&0xff)<<8) + (int) ((checksum[2]&0xff)<<16);

                if( big!=check )
                    throw new IllegalContentException("Checksum Does Not Match");
            }
        }

        static public class EndPacket{
            byte startCode;
            byte packetLength;
            byte packetCategory;
            byte[] checksum;

            public EndPacket(byte[] raw) throws IllegalLengthException, IllegalContentException {
                if (raw.length != 6)
                    throw new IllegalLengthException("Packet length must be 6");
                startCode = raw[0];
                if (startCode != 0x55)
                    throw new IllegalContentException("StartCode must be 0x55");

                packetLength = raw[1];
                if (packetLength != 0x06)
                    throw new IllegalContentException("PacketLength must be 0x06");

                packetCategory = raw[2];
                if (packetCategory != 0x04)
                    throw new IllegalContentException("PacketCategory must be 0x04");
                checksum = new byte[3];
                checksum[0] = raw[3];
                checksum[1] = raw[4];
                checksum[2] = raw[5];

                int big = (int) (startCode &0xff) +
                        (int) (packetLength&0xff) + (int) (packetCategory&0xff);
                int check = (int) (checksum[0]&0xff) + (int) ((checksum[1]&0xff)<<8) + (int) ((checksum[2]&0xff)<<16);

                if( big!=check )
                    throw new IllegalContentException("Checksum Does Not Match");
            }
        }

        static public class Communication{
            InfoPacket infoPacket;
            List<ResultPacket> resultPackets;
            EndPacket endPacket;
            String error;

            public boolean valid(){
                return (infoPacket!=null && resultPackets !=null && endPacket!= null);
            }
        }

        V1Protocol(SerialComunicator comm){
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
            AppReplyPacket appReplyPacket = new AppReplyPacket(calendar);
            serial.send(appReplyPacket.to_bytes());
            byte[] reply = serial.recieve();
            try{
                comm.infoPacket = new InfoPacket(reply);
            }catch (IllegalLengthException | IllegalContentException e){
                comm.error = e.toString();
                return comm;
            }
            comm.resultPackets = new ArrayList<ResultPacket>();
            while(true){
                appReplyPacket = new AppReplyPacket(calendar);
                serial.send(appReplyPacket.to_bytes());
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


    }

}
