package com.ftsafe.bluetooth.ftble.response;

public interface IBleReadResponse extends IBleResponse{

    void onCharacteristicRead(int status, byte[] value);
}
