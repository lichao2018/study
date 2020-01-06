package com.ftsafe.bluetooth.ftble.request;

import com.ftsafe.bluetooth.ftble.response.IBleReadDescriptorResponse;
import com.ftsafe.bluetooth.ftble.response.IBleResponse;

import java.util.UUID;

public class BleReadDescriptorRequest extends BleRequest implements IBleReadDescriptorResponse {
    private UUID mService;
    private UUID mCharacter;
    private UUID mDescriptor;

    public BleReadDescriptorRequest(UUID service, UUID character, UUID descriptor, IBleResponse response) {
        super(response);
        mService = service;
        mCharacter = character;
        mDescriptor = descriptor;
    }

    @Override
    public void onRequestCompleted(int status, byte[] value) {
        onRequestCompleted();
        ((IBleReadDescriptorResponse)mResponse).onDescriptorRead(status, value);
    }

    @Override
    public void executeRequest() {
        if(mWorker != null){
            if(!mWorker.readDescriptor(mService, mCharacter, mDescriptor)){
                onRequestCompleted(-1, null);
            }
        }
    }

    @Override
    public void onDescriptorRead(int status, byte[] value) {
        onRequestCompleted(status, value);
    }
}
