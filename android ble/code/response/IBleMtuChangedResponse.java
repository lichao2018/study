package com.ftsafe.bluetooth.ftble.response;

public interface IBleMtuChangedResponse extends IBleResponse {
    void onMtuChanged(int status);
}
