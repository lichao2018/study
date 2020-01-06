package com.ftsafe.bluetooth.ftble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;

import com.ftsafe.bluetooth.FTBleErrCode;
import com.ftsafe.bluetooth.ftble.response.IBleConnectResponse;
import com.ftsafe.bluetooth.ftble.response.IBleMtuChangedResponse;
import com.ftsafe.bluetooth.ftble.response.IBleReadDescriptorResponse;
import com.ftsafe.bluetooth.ftble.response.IBleReadResponse;
import com.ftsafe.bluetooth.ftble.response.IBleReadRssi;
import com.ftsafe.bluetooth.ftble.response.IBleResponse;
import com.ftsafe.bluetooth.ftble.response.IBleWriteDescriptorResponse;
import com.ftsafe.bluetooth.ftble.response.IBleWriteResponse;
import com.ftsafe.bluetooth.util.LogUtil;
import com.ftsafe.bluetooth.util.StrUtil;

import java.util.Arrays;
import java.util.UUID;

import static com.ftsafe.bluetooth.ftble.BleData.BONDEDTAG;

public class BleCoW {
    private Context mContext;
    private BluetoothGatt mGatt;
    private boolean mIsConnected;
    private Object sendDataLock = new Object(); //用于同步发送数据
    private String mac;
    private IBleResponse mResponse;
    private IBleResponse mConnectResponse;

    public BleCoW(Context context, String mac){
        mContext = context;
        this.mac = mac;
    }

    public void registerResponse(IBleResponse response){
        mResponse = response;
    }

    public void registerConnectResponse(IBleResponse response){
        mConnectResponse = response;
    }

    public boolean connect() {
        if(mGatt != null){
            return true;
        }

        BluetoothDevice bluetoothDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mac);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mGatt = bluetoothDevice.connectGatt(mContext, false, mGattCallback, BluetoothDevice.TRANSPORT_LE);
        } else {
            mGatt = bluetoothDevice.connectGatt(mContext, false, mGattCallback);
        }
        return mGatt != null;
    }

    public void disconnect() {
        if(mGatt != null){
            mGatt.disconnect();
        }
    }

    public boolean isConnected() {
        return mIsConnected;
    }

    //发送数据，发送成功有响应
    public boolean writeCharacteristic(UUID service, UUID character, byte[] value){
        BluetoothGattCharacteristic characteristic = getCharacter(service, character);
        if(characteristic == null){
            return false;
        }
        if(mGatt == null){
            return false;
        }
        characteristic.setValue(value != null ? value : new byte[]{});
        return mGatt.writeCharacteristic(characteristic);
    }

    //发送数据，发送成功无响应
    public boolean writeCharacteristicWithNoRsp(UUID service, UUID character, byte[] value){
        BluetoothGattCharacteristic characteristic = getCharacter(service, character);
        if(characteristic == null){
            return false;
        }
        if(mGatt == null){
            return false;
        }
        characteristic.setValue(value != null ? value : new byte[]{});
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        return mGatt.writeCharacteristic(characteristic);
    }

    //读取特征中的数据
    public boolean readCharacteristic(UUID service, UUID character){
        BluetoothGattCharacteristic characteristic = getCharacter(service, character);
        if(characteristic == null){
            return false;
        }
        if(mGatt == null){
            return false;
        }
        return mGatt.readCharacteristic(characteristic);
    }

    public boolean writeDescriptor(UUID service, UUID character, UUID descriptor, byte[] value){
        BluetoothGattCharacteristic characteristic = getCharacter(service, character);
        if(characteristic == null){
            return false;
        }
        BluetoothGattDescriptor gattDescriptor = characteristic.getDescriptor(descriptor);
        if(gattDescriptor == null){
            return false;
        }
        if(mGatt == null){
            return false;
        }
        if(!gattDescriptor.setValue(value != null ? value : new byte[]{})){
            return false;
        }
        return mGatt.writeDescriptor(gattDescriptor);
    }

    public boolean readDescriptor(UUID service, UUID character, UUID descriptor){
        BluetoothGattCharacteristic characteristic = getCharacter(service, character);
        if(characteristic == null){
            return false;
        }
        BluetoothGattDescriptor gattDescriptor = characteristic.getDescriptor(descriptor);
        if(gattDescriptor == null){
            return false;
        }
        if(mGatt == null){
            return false;
        }
        return mGatt.readDescriptor(gattDescriptor);
    }

    //notify方式，在收到数据时，不回复ack
    public boolean setCharacterNotify(UUID service, UUID character, boolean enable){
        BluetoothGattCharacteristic characteristic = getCharacter(service, character);
        if(characteristic == null){
            return false;
        }
        if(mGatt == null){
            return false;
        }
        if(!mGatt.setCharacteristicNotification(characteristic, enable)){
            return false;
        }
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(BleData.CHARACTER_CONFIG);
        if(descriptor == null){
            return false;
        }
        byte[] value = enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
        if(!descriptor.setValue(value)){
            return false;
        }
        return mGatt.writeDescriptor(descriptor);
    }

    //indicate方式，在收到数据时，回复ack
    public boolean setCharacterIndicate(UUID service, UUID character, boolean enable){
        BluetoothGattCharacteristic characteristic = getCharacter(service, character);
        if(characteristic == null){
            return false;
        }
        if(mGatt == null){
            return false;
        }
        if(!mGatt.setCharacteristicNotification(characteristic, enable)){
            return false;
        }
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(BleData.CHARACTER_CONFIG);
        if(descriptor == null){
            return false;
        }
        byte[] value = enable ? BluetoothGattDescriptor.ENABLE_INDICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
        if(!descriptor.setValue(value)){
            return false;
        }
        return mGatt.writeDescriptor(descriptor);
    }

    public boolean readRemoteRssi(){
        if(mGatt == null){
            return false;
        }
        return mGatt.readRemoteRssi();
    }

    public boolean requestMtu(int mtu){
        if(mGatt == null){
            return false;
        }
        return mGatt.requestMtu(mtu);
    }

    private BluetoothGattCharacteristic getCharacter(UUID service, UUID character){
        BluetoothGattCharacteristic characteristic = null;
        if(mGatt != null){
            BluetoothGattService gattService = mGatt.getService(service);
            if(gattService != null){
                characteristic = gattService.getCharacteristic(character);
            }
        }
        return characteristic;
    }

    private void closeGatt(){
        if(mGatt != null){
            mGatt.close();
            mGatt = null;
        }
        mIsConnected = false;
    }

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if(newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS){
                mGatt.discoverServices();
            }else if(newState == BluetoothProfile.STATE_DISCONNECTED && status != 0x85 && status != 0x89){
                closeGatt();
                if(mConnectResponse != null) {
                    ((IBleConnectResponse) mConnectResponse).onConnectStatusChanged(FTBleErrCode.FT_BT_DISCONNECTED.getValue());
                }
            }else{
                closeGatt();
                if(mConnectResponse != null) {
                    ((IBleConnectResponse) mConnectResponse).onConnectStatusChanged(FTBleErrCode.FT_FAILED.getValue());
                }
            }
            LogUtil.log("onConnected state changed status = " + status + ", newState = " + newState);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            LogUtil.log("on service descovered status = " + status);
            if(status == BluetoothGatt.GATT_SUCCESS){
                mIsConnected = true;
                if(mResponse != null){
                    ((IBleConnectResponse)mConnectResponse).onConnectStatusChanged(FTBleErrCode.FT_SUCCESS.getValue());
                }
            }else{
                closeGatt();
                if(mConnectResponse != null) {
                    ((IBleConnectResponse) mConnectResponse).onConnectStatusChanged(FTBleErrCode.FT_FAILED.getValue());
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            LogUtil.log("on characteristic read status = " + status + ", data = " + StrUtil.byteArr2HexStr(characteristic.getValue()));
            if(mResponse instanceof IBleReadResponse){
                ((IBleReadResponse)mResponse).onCharacteristicRead(status, characteristic.getValue());
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            LogUtil.log("on characteristic write status = " + status);
            if(mResponse instanceof IBleWriteResponse){
                ((IBleWriteResponse) mResponse).onCharacteristicWrite(status, null);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            LogUtil.log("on characteristic changed");
            byte[] data = characteristic.getValue();
            LogUtil.log("recv data len = " + data.length + ", data = " + StrUtil.byteArr2HexStr(data));
            //某些手机在配对成功后，收第一包数据的时候才返5003，要把这个数据剔除掉
            if (Arrays.equals(data, BONDEDTAG)) {
                return;
            }
            if(mResponse instanceof IBleWriteResponse){
                ((IBleWriteResponse)mResponse).onCharacteristicWrite(IBleWriteResponse.RECV_DATA, data);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            if(mResponse instanceof IBleWriteDescriptorResponse){
                ((IBleWriteDescriptorResponse)mResponse).onDescriptorWirte(status);
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            if(mResponse instanceof IBleReadDescriptorResponse){
                ((IBleReadDescriptorResponse)mResponse).onDescriptorRead(status, descriptor.getValue());
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            if(mResponse instanceof IBleReadRssi){
                ((IBleReadRssi)mResponse).onReadRemoteRssi(status, rssi);
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            if(mResponse instanceof IBleMtuChangedResponse){
                ((IBleMtuChangedResponse)mResponse).onMtuChanged(status);
            }
        }
    };
}
