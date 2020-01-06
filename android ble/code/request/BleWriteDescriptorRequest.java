package com.ftsafe.bluetooth.ftble.request;

import com.ftsafe.bluetooth.ftble.response.IBleResponse;
import com.ftsafe.bluetooth.ftble.response.IBleWriteDescriptorResponse;

import java.util.UUID;

public class BleWriteDescriptorRequest extends BleRequest implements IBleWriteDescriptorResponse {
    private UUID mService;
    private UUID mCharacter;
    private UUID mDescriptor;
    private byte[] mValue;

    public BleWriteDescriptorRequest(UUID service, UUID character, UUID descriptor, byte[] value, IBleResponse response) {
        super(response);
        mService = service;
        mCharacter = character;
        mDescriptor = descriptor;
        mValue = value;
    }

    @Override
    public void onRequestCompleted(int status, byte[] value) {
        onRequestCompleted();
        ((IBleWriteDescriptorResponse)mResponse).onDescriptorWirte(status);
    }

    @Override
    public void executeRequest() {
        if(mWorker != null){
            if(!mWorker.writeDescriptor(mService, mCharacter, mDescriptor, mValue)){
                onRequestCompleted(-1, null);
            }
        }
    }

    @Override
    public void onDescriptorWirte(int status) {
        onRequestCompleted(status, null);
    }
}
