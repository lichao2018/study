package com.ftsafe.bluetooth.ftble.request;

import com.ftsafe.bluetooth.ftble.response.IBleReadRssi;
import com.ftsafe.bluetooth.ftble.response.IBleResponse;

public class BleReadRssiRequest extends BleRequest implements IBleReadRssi {

    public BleReadRssiRequest(IBleResponse response) {
        super(response);
    }

    @Override
    public void onRequestCompleted(int status, byte[] value) {
        onRequestCompleted();
        ((IBleReadRssi)mResponse).onReadRemoteRssi(status, value == null ? 0 : value[0]);
    }

    @Override
    public void executeRequest() {
        if(mWorker != null){
            if(!mWorker.readRemoteRssi()){
                onRequestCompleted(-1, null);
            }
        }
    }

    @Override
    public void onReadRemoteRssi(int status, int rssi) {
        onRequestCompleted(status, new byte[]{(byte) rssi});
    }
}
