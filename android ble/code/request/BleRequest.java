package com.ftsafe.bluetooth.ftble.request;

import com.ftsafe.bluetooth.ftble.BleCoW;
import com.ftsafe.bluetooth.ftble.IBleDispatcher;
import com.ftsafe.bluetooth.ftble.response.IBleResponse;

public abstract class BleRequest implements IBleResponse {
    private IBleDispatcher mDispatcher;
    BleCoW mWorker;
    IBleResponse mResponse;

    public BleRequest(IBleResponse response){
        mResponse = response;
    }

    public void setWorker(BleCoW worker){
        mWorker = worker;
    }

    public void execute(IBleDispatcher dispatcher){
        mDispatcher = dispatcher;
        mWorker.registerResponse(this);
        executeRequest();
    }

    void onRequestCompleted(){
        mDispatcher.onRequestCompleted();
    }

    public abstract void onRequestCompleted(int status, byte[] value);

    public abstract void executeRequest();
}
