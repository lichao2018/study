package com.ftsafe.bluetooth.ftble.response;

public interface IBleReadRssi extends IBleResponse {
    void onReadRemoteRssi(int status, int rssi);
}
