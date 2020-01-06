package com.ftsafe.bluetooth.ftble.response;

public interface IBleReadDescriptorResponse extends IBleResponse {

    void onDescriptorRead(int status, byte[] value);
}
