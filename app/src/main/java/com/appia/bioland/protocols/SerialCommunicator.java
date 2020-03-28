package com.appia.bioland.protocols;

public class SerialCommunicator{
    public boolean connected = false;

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
