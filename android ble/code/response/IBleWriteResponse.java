package com.ftsafe.bluetooth.ftble.response;

public interface IBleWriteResponse extends IBleResponse{

    int RECV_DATA = 0xff;

    void onCharacteristicWrite(int status, byte[] value);
}
