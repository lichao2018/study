package com.ftsafe.bluetooth.ftble.request;

import com.ftsafe.bluetooth.FTBleErrCode;
import com.ftsafe.bluetooth.ftble.response.IBleConnectResponse;
import com.ftsafe.bluetooth.ftble.response.IBleResponse;

public class BleConnectRequest extends BleRequest implements IBleConnectResponse {

    public BleConnectRequest(IBleResponse response){
        super(response);
    }

    @Override
    public void onRequestCompleted(int status, byte[] value) {
        onRequestCompleted();
        ((IBleConnectResponse)mResponse).onConnectStatusChanged(status);
    }

    @Override
    public void executeRequest() {
        if(mWorker != null){
            mWorker.registerConnectResponse(this);
            if(!mWorker.connect()){
                onRequestCompleted(FTBleErrCode.FT_FAILED.getValue(), null);
            }
        }
    }

    @Override
    public void onConnectStatusChanged(int status) {
        onRequestCompleted(status, null);
    }
}
