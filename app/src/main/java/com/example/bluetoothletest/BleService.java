package com.example.bluetoothletest;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import java.util.UUID;

public class BleService extends Service {

    private final String TAG = BleService.class.getSimpleName();

    private BluetoothGatt mBluetoothGatt;

    // 蓝牙连接状态
    private int mConnectionState = 0;
    // 蓝牙连接已断开
    private final int STATE_DISCONNECTED = 0;
    // 蓝牙正在连接
    private final int STATE_CONNECTING = 1;
    // 蓝牙已连接
    private final int STATE_CONNECTED = 2;

    // 蓝牙已连接
    public final static String ACTION_GATT_CONNECTED = "com.example.bluetoothletest.ACTION_GATT_CONNECTED";
    // 蓝牙已断开
    public final static String ACTION_GATT_DISCONNECTED = "com.example.bluetoothletest.ACTION_GATT_DISCONNECTED";
    // 发现GATT服务
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetoothletest.ACTION_GATT_SERVICES_DISCOVERED";
    // 收到蓝牙数据
    public final static String ACTION_DATA_AVAILABLE = "com.example.bluetoothletest.ACTION_DATA_AVAILABLE";
    // 连接失败
    public final static String ACTION_CONNECTING_FAIL = "com.example.bluetoothletest.ACTION_CONNECTING_FAIL";
    // 蓝牙数据
    public final static String EXTRA_DATA = "com.example.bluetoothletest.EXTRA_DATA";

    // 服务标识
//    private final UUID SERVICE_UUID = UUID.fromString("0000ace0-0000-1000-8000-00805f9b34fb");
    private final UUID SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    // 特征标识（读取数据）
//    private final UUID CHARACTERISTIC_READ_UUID = UUID.fromString("0000ace0-0001-1000-8000-00805f9b34fb");
    private final UUID CHARACTERISTIC_READ_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
    // 特征标识（发送数据）
//    private final UUID CHARACTERISTIC_WRITE_UUID = UUID.fromString("0000ace0-0003-1000-8000-00805f9b34fb");
    private final UUID CHARACTERISTIC_WRITE_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    // 描述标识
//    private final UUID DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private final UUID DESCRIPTOR_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

    // 服务相关
    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public BleService getService() {
            return BleService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        release();
        return super.onUnbind(intent);
    }

    /**
     * 蓝牙操作回调
     * 蓝牙连接状态才会回调
     */
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // 蓝牙已连接
                mConnectionState = STATE_CONNECTED;
                sendBleBroadcast(ACTION_GATT_CONNECTED);
                // 搜索GATT服务
                mBluetoothGatt.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // 蓝牙已断开连接
                mConnectionState = STATE_DISCONNECTED;
                sendBleBroadcast(ACTION_GATT_DISCONNECTED);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            // 发现GATT服务
            if (status == BluetoothGatt.GATT_SUCCESS) {
                setBleNotification(); //通知
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicRead: ???");
            // 收到数据
            sendBleBroadcast(ACTION_DATA_AVAILABLE, characteristic);
            
        }

        @Override  //寫資料
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.d(TAG, "onCharacteristicWrite: ");
        }

        @Override  //接受到手機端的command後藍芽回覆的資料
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
                if (characteristic.getValue() != null){
                    String result = new String(characteristic.getValue());
                    String[] str = result.split(","); //以,切割
                    String temp = str[2];
                    double degree = Double.parseDouble(temp)/100;  //25.0
                    Log.d(TAG, "onCharacteristicChanged: " + degree);
                }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            Log.d(TAG, "onDescriptorRead: ");
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.d(TAG, "onDescriptorWrite: ");
        }
    };

    /**
     * 发送通知
     *
     * @param action 广播Action
     */
    private void sendBleBroadcast(String action) {
        Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    /**
     * 发送通知
     *
     * @param action         广播Action
     * @param characteristic 数据
     */
    private void sendBleBroadcast(String action, BluetoothGattCharacteristic characteristic) {
        Intent intent = new Intent(action);
        if (CHARACTERISTIC_READ_UUID.equals(characteristic.getUuid())) {
            intent.putExtra(EXTRA_DATA, characteristic.getValue());
            Log.d(TAG, "sendBleBroadcast: ????????");
        }
        sendBroadcast(intent);
    }

    /**
     * 蓝牙连接
     *
     * @param bluetoothAdapter BluetoothAdapter
     * @param address          设备mac地址
     * @return true：成功 false：
     */
    public boolean connect(BluetoothAdapter bluetoothAdapter, String address) {
        if (bluetoothAdapter == null || TextUtils.isEmpty(address)) {
            return false;
        }

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            return false;
        }
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * 蓝牙断开连接
     */
    public void disconnect() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * 释放相关资源
     */
    public void release() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * 设置蓝牙设备在数据改变时，通知App
     */
//    public void setBleNotification() {
//        if (mBluetoothGatt == null) {
//            sendBleBroadcast(ACTION_CONNECTING_FAIL);
//            return;
//        }
//
//        // 获取蓝牙设备的服务
//        BluetoothGattService gattService = mBluetoothGatt.getService(SERVICE_UUID);
//        if (gattService == null) {
//            sendBleBroadcast(ACTION_CONNECTING_FAIL);
//            return;
//        }
//
//        // 获取蓝牙设备的特征
//        BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(DESCRIPTOR_UUID);
//        if (gattCharacteristic == null) {
//            sendBleBroadcast(ACTION_CONNECTING_FAIL);
//            return;
//        }
//        Log.d(TAG, "setBleNotification: gattCharacteristic : " + gattCharacteristic.getUuid().toString());
////        // 获取蓝牙设备特征的描述符
////        for(BluetoothGattDescriptor descriptor : gattCharacteristic.getDescriptors()){
////            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
////            // 蓝牙设备在数据改变时，通知App，App在收到数据后回调onCharacteristicChanged方法
////            mBluetoothGatt.setCharacteristicNotification(gattCharacteristic, true);
////
////        }
//        BluetoothGattDescriptor descriptor = gattCharacteristic.getDescriptor(DESCRIPTOR_UUID);
//        Log.d(TAG, "setBleNotification: descriptor:" + descriptor);
//        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//        if (mBluetoothGatt.writeDescriptor(descriptor)) {
//            Log.d(TAG, "setBleNotification: ????");
//            // 蓝牙设备在数据改变时，通知App，App在收到数据后回调onCharacteristicChanged方法
//            mBluetoothGatt.setCharacteristicNotification(gattCharacteristic, true);
//        }
//    }

    private void setBleNotification(){
        //1.获取蓝牙设备的服务
        BluetoothGattService gattService = mBluetoothGatt.getService(SERVICE_UUID);
        if (gattService != null){
            BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(DESCRIPTOR_UUID);
            if(gattCharacteristic != null){
                boolean success = mBluetoothGatt.setCharacteristicNotification(gattCharacteristic,true);
                if(success){
                    for(BluetoothGattDescriptor dp: gattCharacteristic.getDescriptors()){
                        if (dp != null){
                            dp.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            boolean isSuccess = mBluetoothGatt.writeDescriptor(dp);
                            Log.d(TAG, "setBleNotification: " + isSuccess + " dp:" + dp.getUuid().toString());
                        }
                    }
                }
            }
        }
    }


    /**
     * 发送数据
     *
     * @param data 数据
     * @return true：发送成功 false：发送失败
     */
    public boolean sendData(byte[] data) {
        //Log.d(TAG, "sendData: " + ByteUtils.byteArrayToHexString(data)); //4149444F2C30
        // 获取蓝牙设备的服务
        BluetoothGattService gattService = null;
        if (mBluetoothGatt != null) {
            gattService = mBluetoothGatt.getService(SERVICE_UUID);
        }
        if (gattService == null) {
            return false;
        }

        // 获取蓝牙设备的特征
        BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(CHARACTERISTIC_WRITE_UUID);
        if (gattCharacteristic == null) {
            return false;
        }

        // 发送数据
        gattCharacteristic.setValue(data);

        return mBluetoothGatt.writeCharacteristic(gattCharacteristic);
    }
}