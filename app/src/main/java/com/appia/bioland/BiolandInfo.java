package com.appia.bioland;

import androidx.annotation.NonNull;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class BiolandInfo {
    public int protocolVersion;
    public int batteryCapacity;
    public byte[] serialNumber;
    public GregorianCalendar productionDate;

    @NonNull
    @Override
    public String toString() {


        return "Battery: " + batteryCapacity + "% Protocol: " + protocolVersion + " Serial N°: " + bytesToHex(serialNumber)  ;
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}