package com.ftsafe.bluetooth.ftble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;

import com.ftsafe.bluetooth.FTBleErrCode;
import com.ftsafe.bluetooth.util.LogUtil;
import com.ftsafe.bluetooth.util.StrUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BleScan {
    private static BleScan instance = null;
    private Context mContext;
    private BluetoothAdapter mBluetoothAdapter;
    private FTBleScanCallback mScanCallback;
    private Handler timeoutHandler;
    private Runnable timeoutRunnable;
    private List<byte[]> mDeviceMacList;
    private boolean scanning;
    private int mTimeout;

    private BleScan(Context context){
        mContext = context;
        mDeviceMacList = new ArrayList<>();
        IntentFilter btEnabledIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        context.getApplicationContext().registerReceiver(new BtReceiver(), btEnabledIntentFilter);
    }

    public static BleScan getInstance(Context context){
        if(instance == null){
            synchronized (BleScan.class){
                if(instance == null){
                    instance = new BleScan(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public FTBleErrCode scan(int timeout, FTBleScanCallback scanCallback) {
        mDeviceMacList.clear();
        mTimeout = timeout;
        mScanCallback = scanCallback;
        final BluetoothManager bluetoothManager =
                (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null || (mBluetoothAdapter = bluetoothManager.getAdapter()) == null) {
            return FTBleErrCode.FT_BT_NOT_SUPPORT;
        }
        if (!mBluetoothAdapter.isEnabled()) {
            return FTBleErrCode.FT_BT_NOT_ENABLED;
        }
        if(mBluetoothAdapter.getBluetoothLeScanner() == null) {
            return FTBleErrCode.FT_SCAN_FAILED;
        }

        if(scanning){
            LogUtil.log("ble is scanning");
            return FTBleErrCode.FT_SUCCESS;
        }
        scanning = true;

        ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        List<ScanFilter> filters = new ArrayList<>();
        ScanFilter filter = new ScanFilter.Builder()
                .setDeviceName(BleData.DEVICE_NAME)
                .build();
        filters.add(filter);
        mBluetoothAdapter.getBluetoothLeScanner().startScan(filters, settings, mLeScanCallback);

        timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                stopScan();
                mScanCallback.onScanStop();
            }
        };
        if(timeout > 0) {
            timeoutHandler = new Handler();
            timeoutHandler.postDelayed(timeoutRunnable, timeout * 1000);
        }
        return FTBleErrCode.FT_SUCCESS;
    }

    public void stopScan() {
        scanning = false;
        mDeviceMacList.clear();
        if(mBluetoothAdapter != null && mBluetoothAdapter.getBluetoothLeScanner() != null) {
            mBluetoothAdapter.getBluetoothLeScanner().stopScan(mLeScanCallback);
        }
        if(timeoutHandler != null && timeoutRunnable != null){
            timeoutHandler.removeCallbacks(timeoutRunnable);
        }
    }

    private ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if(mScanCallback == null){
                LogUtil.log("mScanCallback null");
                return;
            }
            BluetoothDevice bluetoothDevice = result.getDevice();
            ScanRecord scanRecord = result.getScanRecord();
            if(scanRecord == null){
                LogUtil.log("scanRecord null");
                return;
            }
            byte[] manuData = scanRecord.getManufacturerSpecificData(BleData.MANUDATA_ID_INT);
            if(manuData == null){
                LogUtil.log("manuData null");
                return;
            }
            if(manuData.length != BleData.MANUDATA_LENGTH){
                LogUtil.log("manuData length : " + manuData.length);
                return;
            }
            if(manuData[0] != BleData.MANUDATA_START_BYTE[0]){
                LogUtil.log("manuData : " + StrUtil.byteArr2HexStr(manuData));
                return;
            }
            int index = getDeviceIndex(manuData);

            String sn = StrUtil.byteArr2HexStr(BleData.MANUDATA_ID) + StrUtil.byteArr2HexStr(manuData, 0, BleData.MANUDATA_SN_LENGTH);
            LogUtil.log("device(" + sn + " - " + StrUtil.byteArr2HexStr(manuData) + ")");
            if(index >= 0){
                if(flagHasAdd(manuData, index)){
                    mScanCallback.onScanDevice(bluetoothDevice, sn, true);
                }else{
                    mScanCallback.onScanDevice(bluetoothDevice, sn, false);
                }
            }else {
                mDeviceMacList.add(manuData);
                mScanCallback.onScanDevice(bluetoothDevice, sn, false);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            LogUtil.log("scan failed. error code : " + errorCode);
            switch (errorCode){
                case SCAN_FAILED_ALREADY_STARTED:
                    scanning = true;
                    break;
                case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                case SCAN_FAILED_INTERNAL_ERROR:
                case SCAN_FAILED_FEATURE_UNSUPPORTED:
                default:
                    scanning = false;
                    break;
            }
        }
    };

    class BtReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())){
                int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                switch (blueState){
                    case BluetoothAdapter.STATE_TURNING_ON:
                        break;
                    case BluetoothAdapter.STATE_ON:
//                        scan(mTimeout, mScanCallback);
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        scanning = false;
                        break;
                }
            }
        }
    }

    //设备自定义广播信息，前9位为序列号，接着6位为mac，最后一位为flag，每按一次设备，flag加一
    //返回设备所在列表的索引，不存在则返回-1
    private int getDeviceIndex(byte[] manuData){
        for(int i = 0; i < mDeviceMacList.size(); i ++){
            byte[] localData = mDeviceMacList.get(i);

            if(localData.length != manuData.length){
                continue;
            }
            int snLen = BleData.MANUDATA_SN_LENGTH;

            byte[] localSn = new byte[snLen];
            byte[] manuDataSn = new byte[snLen];
            System.arraycopy(localData, 0, localSn, 0, snLen);
            System.arraycopy(manuData, 0, manuDataSn, 0, snLen);
            if(Arrays.equals(localSn, manuDataSn)){
                return i;
            }
        }
        return -1;
    }

    private boolean flagHasAdd(byte[] manuData, int index){
        if(mDeviceMacList.size() <= index){
            return false;
        }
        byte localFlag = mDeviceMacList.get(index)[manuData.length - 1];
        byte manuDataFlag = manuData[manuData.length - 1];
        mDeviceMacList.set(index, manuData);
        return manuDataFlag > localFlag;
    }

    public interface FTBleScanCallback{
        void onScanDevice(BluetoothDevice bluetoothDevice, String sn, boolean flagHasAdded);
        void onScanStop();
    }
}
