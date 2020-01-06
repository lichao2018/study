package com.ftsafe.bluetooth.ftble.request;

import com.ftsafe.bluetooth.ftble.response.IBleReadResponse;
import com.ftsafe.bluetooth.ftble.response.IBleResponse;

import java.util.UUID;

public class BleReadRequest extends BleRequest implements IBleReadResponse {

    private UUID mService;
    private UUID mCharacter;

    public BleReadRequest(UUID service, UUID character, IBleResponse response) {
        super(response);
        mService = service;
        mCharacter = character;
    }

    @Override
    public void onRequestCompleted(int status, byte[] value) {
        onRequestCompleted();
        ((IBleReadResponse)mResponse).onCharacteristicRead(status, value);
    }

    @Override
    public void executeRequest() {
        if(!mWorker.readCharacteristic(mService, mCharacter)){
            onRequestCompleted(-1, null);
        }
    }

    @Override
    public void onCharacteristicRead(int status, byte[] value) {
        onRequestCompleted();
        onRequestCompleted(status, value);
    }
}
