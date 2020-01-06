package com.ftsafe.bluetooth.ftble.response;

public interface IBleWriteDescriptorResponse extends IBleResponse{

    void onDescriptorWirte(int status);
}
