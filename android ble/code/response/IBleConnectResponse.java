package com.ftsafe.bluetooth.ftble.response;

public interface IBleConnectResponse extends IBleResponse{

    void onConnectStatusChanged(int status);
}
