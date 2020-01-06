package com.ftsafe.bluetooth.ftble.request;

import com.ftsafe.bluetooth.ftble.response.IBleMtuChangedResponse;
import com.ftsafe.bluetooth.ftble.response.IBleResponse;

public class BleRequestMtuRequest extends BleRequest implements IBleMtuChangedResponse {
    private int mMtu;

    public BleRequestMtuRequest(int mtu, IBleResponse response) {
        super(response);
        mMtu = mtu;
    }

    @Override
    public void onRequestCompleted(int status, byte[] value) {
        onRequestCompleted();
        ((IBleMtuChangedResponse)mResponse).onMtuChanged(status);
    }

    @Override
    public void executeRequest() {
        if(mWorker != null){
            if(!mWorker.requestMtu(mMtu)){
                onRequestCompleted(-1, null);
            }
        }
    }

    @Override
    public void onMtuChanged(int status) {
        onRequestCompleted(status, null);
    }
}
