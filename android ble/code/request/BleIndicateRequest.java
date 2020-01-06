package com.ftsafe.bluetooth.ftble.request;

import com.ftsafe.bluetooth.ftble.response.IBleResponse;
import com.ftsafe.bluetooth.ftble.response.IBleWriteDescriptorResponse;

import java.util.UUID;

public class BleIndicateRequest extends BleRequest implements IBleWriteDescriptorResponse {
    private UUID mService;
    private UUID mCharater;

    public BleIndicateRequest(UUID service, UUID character, IBleResponse response) {
        super(response);
        mService = service;
        mCharater = character;
    }

    @Override
    public void onRequestCompleted(int status, byte[] value) {
        onRequestCompleted();
        ((IBleWriteDescriptorResponse)mResponse).onDescriptorWirte(status);
    }

    @Override
    public void executeRequest() {
        if(!mWorker.setCharacterIndicate(mService, mCharater, true)){
            onRequestCompleted(-1, null);
        }
    }

    @Override
    public void onDescriptorWirte(int status) {
        onRequestCompleted();
        if(mResponse != null){
            onRequestCompleted(status, null);
        }
    }
}
