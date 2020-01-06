package com.ftsafe.bluetooth.ftble;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.ftsafe.bluetooth.ftble.request.BleConnectRequest;
import com.ftsafe.bluetooth.ftble.request.BleIndicateRequest;
import com.ftsafe.bluetooth.ftble.request.BleNotifyRequest;
import com.ftsafe.bluetooth.ftble.request.BleReadDescriptorRequest;
import com.ftsafe.bluetooth.ftble.request.BleReadRequest;
import com.ftsafe.bluetooth.ftble.request.BleReadRssiRequest;
import com.ftsafe.bluetooth.ftble.request.BleRequest;
import com.ftsafe.bluetooth.ftble.request.BleRequestMtuRequest;
import com.ftsafe.bluetooth.ftble.request.BleUnnotifyRequest;
import com.ftsafe.bluetooth.ftble.request.BleWriteDescriptorRequest;
import com.ftsafe.bluetooth.ftble.request.BleWriteRequest;
import com.ftsafe.bluetooth.ftble.request.BleWriteRequestNoRsp;
import com.ftsafe.bluetooth.ftble.response.IBleResponse;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class BleDispatcher implements Handler.Callback, IBleDispatcher{

    private static BleDispatcher instance = null;
    private List<BleRequest> mBleWorkList;
    private BleRequest mCurrentRequest;
    private Handler mHandler;
    private BleCoW mWorker;

    private static final int MSG_SCHEDULE_NEXT = 0x12;

    public BleDispatcher(Context context, String mac){
        mBleWorkList = new LinkedList<>();
        mHandler = new Handler(Looper.myLooper(), this);
        mWorker = new BleCoW(context, mac);
    }

    public void connect(IBleResponse response){
        addNewRequest(new BleConnectRequest(response));
    }

    public void disconnect(){
        mBleWorkList.clear();
        mCurrentRequest = null;
        mWorker.disconnect();
    }

    public void write(UUID service, UUID character, byte[] value, IBleResponse response){
        addNewRequest(new BleWriteRequest(service, character, value, response));
    }

    public void writeNoRsp(UUID service, UUID character, byte[] value, IBleResponse response){
        addNewRequest(new BleWriteRequestNoRsp(service, character, value, response));
    }

    public void read(UUID service, UUID character, IBleResponse response){
        addNewRequest(new BleReadRequest(service, character, response));
    }

    public void writeDescriptor(UUID service, UUID character, UUID descriptor, byte[] value, IBleResponse response){
        addNewRequest(new BleWriteDescriptorRequest(service, character, descriptor, value, response));
    }

    public void readDescriptor(UUID service, UUID character, UUID descriptor, IBleResponse response){
        addNewRequest(new BleReadDescriptorRequest(service, character, descriptor, response));
    }

    public void notify(UUID service, UUID character, IBleResponse response){
        addNewRequest(new BleNotifyRequest(service, character, response));
    }

    public void unnotify(UUID service, UUID character, IBleResponse response){
        addNewRequest(new BleUnnotifyRequest(service, character, response));
    }

    public void indicate(UUID service, UUID character, IBleResponse response){
        addNewRequest(new BleIndicateRequest(service, character, response));
    }

    public void readRemoteRssi(IBleResponse response){
        addNewRequest(new BleReadRssiRequest(response));
    }

    public void requestMtu(int mtu, IBleResponse response){
        addNewRequest(new BleRequestMtuRequest(mtu, response));
    }

    public boolean isConnected(){
        return mWorker.isConnected();
    }

    public void onRequestCompleted(){
        mCurrentRequest = null;
        scheduleNextRequest(10);
    }

    private void addNewRequest(BleRequest request){
        request.setWorker(mWorker);
        mBleWorkList.add(request);
        scheduleNextRequest(10);
    }

    private void scheduleNextRequest(long delayInMillis){
        mHandler.sendEmptyMessageDelayed(MSG_SCHEDULE_NEXT, delayInMillis);
    }

    private void scheduleNextRequest(){
        if(mCurrentRequest != null){
            return;
        }
        if(mBleWorkList == null || mBleWorkList.size() == 0){
            return;
        }
        mCurrentRequest = mBleWorkList.remove(0);
        mCurrentRequest.execute(this);
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what){
            case MSG_SCHEDULE_NEXT:
                scheduleNextRequest();
                break;
        }
        return true;
    }
}
