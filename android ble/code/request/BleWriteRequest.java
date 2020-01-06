package com.ftsafe.bluetooth.ftble.request;

import com.ftsafe.bluetooth.ftble.response.IBleResponse;
import com.ftsafe.bluetooth.ftble.response.IBleWriteResponse;

import java.util.UUID;

public class BleWriteRequest extends BleRequest implements IBleWriteResponse {

    private UUID mService;
    private UUID mCharacter;
    private byte[] mValue;

    public BleWriteRequest(UUID service, UUID character, byte[] value, IBleResponse response) {
        super(response);
        mService = service;
        mCharacter = character;
        mValue = value;
    }

    @Override
    public void onRequestCompleted(int status, byte[] value) {
        onRequestCompleted();
        ((IBleWriteResponse)mResponse).onCharacteristicWrite(status, value);
    }

    @Override
    public void executeRequest() {
        if(!mWorker.writeCharacteristic(mService, mCharacter, mValue)){
            onRequestCompleted(-1, null);
        }
    }

    @Override
    public void onCharacteristicWrite(int status, byte[] value) {
        onRequestCompleted();
        onRequestCompleted(status, value);
    }
}
