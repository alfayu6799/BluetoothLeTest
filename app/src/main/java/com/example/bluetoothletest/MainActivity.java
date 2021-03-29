package com.example.bluetoothletest;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.example.bluetoothletest.BleService.ACTION_DATA_AVAILABLE;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";

    private final int REQUEST_ENABLE_BT = 1;
    private RecyclerView rvDeviceList;
    private Button btnScan, send, takePhoto;
    private BluetoothAdapter mBtAdapter;
    private BleService mBleService;
    private BroadcastReceiver mBleReceiver;
    private DeviceListAdapter mDeviceListAdapter;
    private List<BluetoothDevice> mBluetoothDeviceList;
    private List<String> mRssiList;

    private String bleDeviceAddress;

    private String mPath;
    public final int REQUEST_IMAGE = 101;
    private ImageView picture;

    private TextView result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();


        //初始化蓝牙
        initBle();

        //初始化数据
        initData();

        //注册蓝牙信息接收器
        registerBleReceiver();

    }

    private void initView() {
        rvDeviceList = findViewById(R.id.rv_device_list);
        btnScan = findViewById(R.id.button);
        btnScan.setOnClickListener(this);
        send = findViewById(R.id.btnSend);
        send.setOnClickListener(this);
        takePhoto = findViewById(R.id.btnShots);
        takePhoto.setOnClickListener(this);
        picture = findViewById(R.id.imageView);
        result = findViewById(R.id.tvResult);
    }

    private void initData() {
        // 蓝牙设备列表
        mBluetoothDeviceList = new ArrayList<>();
        // 蓝牙设备RSSI列表
        mRssiList = new ArrayList<>();
        mDeviceListAdapter = new DeviceListAdapter(mBluetoothDeviceList, mRssiList);
        rvDeviceList.setLayoutManager(new LinearLayoutManager(this));
        rvDeviceList.setAdapter(mDeviceListAdapter);

        // 连接蓝牙设备
        mDeviceListAdapter.setOnItemClickListener(new DeviceListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Toast.makeText(MainActivity.this, "開始連接" + mBluetoothDeviceList.get(position).getAddress(), Toast.LENGTH_SHORT).show();
                bleDeviceAddress = mBluetoothDeviceList.get(position).getAddress();

                mBtAdapter.stopLeScan(mLeScanCallback);
//                mBleService.connect(mBtAdapter, mBluetoothDeviceList.get(position).getAddress());
                mBleService.connect(mBluetoothDeviceList.get(position).getAddress());

            }
        });

    }

    //初始化藍芽
    private void initBle() {
        //檢查設備是否有藍芽
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "藍芽不可用", Toast.LENGTH_SHORT).show();
            return;
        }

        //詢問是否開啟藍芽
        if (!mBtAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_ENABLE_BT);
            return;
        }

        //搜索蓝牙设备
        scanBleDevice();
    }

    /**
     * 注册蓝牙信息接收器
     */
    private void registerBleReceiver() {
        // 绑定服务
        Intent intent = new Intent(this, BleService.class);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        startService(intent);

        mBleReceiver = new BleReceiver();

        registerReceiver(mBleReceiver,makeGattUpdateIntentFilter());
    }

    public static IntentFilter makeGattUpdateIntentFilter(){
        // 注册蓝牙信息广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(BleService.ACTION_GATT_CONNECTED);
        filter.addAction(BleService.ACTION_GATT_DISCONNECTED);
        filter.addAction(BleService.ACTION_GATT_SERVICES_DISCOVERED);
        filter.addAction(BleService.ACTION_DATA_AVAILABLE);
        filter.addAction(BleService.ACTION_NOTIFY_ON);
        filter.addAction(BleService.ACTION_CONNECTING_FAIL);
        filter.addAction(BleService.EXTRA_MAC);
        return  filter;
    }

    public static IntentFilter makeGattUpdateIntentFilter(String address){
        // 注册蓝牙信息广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(address + BleService.ACTION_GATT_CONNECTED);
        filter.addAction(address + BleService.ACTION_GATT_DISCONNECTED);
        filter.addAction(address + BleService.ACTION_GATT_SERVICES_DISCOVERED);
        filter.addAction(address + BleService.ACTION_DATA_AVAILABLE);
        filter.addAction(address + BleService.ACTION_CONNECTING_FAIL);
        return  filter;
    }


    /**
     * 搜索蓝牙设备回调
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
            if (!mBluetoothDeviceList.contains(bluetoothDevice)) {
                if(bluetoothDevice.getName() != null)
                mBluetoothDeviceList.add(bluetoothDevice);
                mRssiList.add(String.valueOf(i));
                mDeviceListAdapter.notifyDataSetChanged();
            }
        }
    };

    /**
     * 背景服务
     */
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mBleService = ((BleService.LocalBinder) rawBinder).getService();

            Log.d(TAG, "onServiceConnected: " + bleDeviceAddress);
            //auto connect to the device upon successful start-up init
//            mBluetoothLeService.connect(mBluetoothAdapter, deviceAddress);

        }

        public void onServiceDisconnected(ComponentName classname) {
            mBleService = null;
        }
    };

    //搜尋藍芽設備
    private void scanBleDevice() {
        mBtAdapter.stopLeScan(mLeScanCallback);
        mBtAdapter.startLeScan(mLeScanCallback);
        // 搜索10s
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mBtAdapter.stopLeScan(mLeScanCallback);
            }
        }, 10000);
    }

    /**
     * 蓝牙信息接收器
     */
    private class BleReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TextUtils.isEmpty(action)) {
                return;
            }
            switch (action) {
                case BleService.ACTION_GATT_CONNECTED:
                    Toast.makeText(MainActivity.this, "蓝牙已连接", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "onReceive: ");
//                    result.setText("蓝牙已连接");
                    break;

                case BleService.ACTION_GATT_DISCONNECTED:
                    Toast.makeText(MainActivity.this, "蓝牙已断开", Toast.LENGTH_SHORT).show();
                    result.setText("藍芽已斷開");
                    Log.d(TAG, "藍芽已斷開???: " + bleDeviceAddress);
                    mBleService.release();
                    break;

                case BleService.ACTION_CONNECTING_FAIL:
                    Toast.makeText(MainActivity.this, "蓝牙已断开", Toast.LENGTH_SHORT).show();
                    result.setText("蓝牙已断开");
                    mBleService.disconnect();
                    break;
                case BleService.ACTION_NOTIFY_ON:
                    Toast.makeText(MainActivity.this, "啟動通知成功", Toast.LENGTH_SHORT).show();
                    result.setText("藍芽已連接");
                    break;
                case ACTION_DATA_AVAILABLE:
                    byte[] data = intent.getByteArrayExtra(BleService.EXTRA_DATA);
                    String address = intent.getStringExtra(BleService.EXTRA_MAC);
                    Log.i("蓝牙", "收到的原始數據：" + ByteUtils.byteArrayToString(data) + " device:" + address);
                    break;

                default:
                    break;
            }
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnScan: // 搜索蓝牙
                // 搜索蓝牙设备
                scanBleDevice();
                // 初始化数据
                initData();
                // 注册蓝牙信息接收器
                registerBleReceiver();
                break;
            case R.id.btnSend:
                sendCommand(); //量測command
                break;
            case R.id.btnShots:
                openCamera();  //開啟相機功能
                break;
            default:
                break;
        }
    }

    //開啟相機功能
    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File imageFile = getImageFile();
        Uri imageUri = FileProvider.getUriForFile(this,"com.example.bluetoothletest.fileprovider", imageFile);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(cameraIntent, REQUEST_IMAGE);
    }

    //取得相片的URL及檔名
    private File getImageFile() {
        String time = new SimpleDateFormat("yyMMdd").format(new Date());
        String fileName = time+"_";
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try {
            File imageFile = File.createTempFile(fileName,".jpg",dir);
            mPath = imageFile.getAbsolutePath();
            return imageFile;
        } catch (IOException e) {
            return null;
        }
    }

    //量測command
    private void sendCommand(){
        String request = "AIDO,0";
        byte[] messageBytes = new byte[0];
        try {
            messageBytes = request.getBytes("UTF-8");

            mBleService.writeDataToDevice(messageBytes,bleDeviceAddress);
            //Log.d(TAG, "sendCommand: " + messageBytes + " device:" + bleDeviceAddress);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                scanBleDevice(); //搜索蓝牙设备
                break;
            case REQUEST_IMAGE:  //顯示照片
                showPhoto();
                break;
            default:
                break;
        }
    }

    //顯示照片
    private void showPhoto() {
        new Thread(()->{
            //在BitmapFactory中以檔案URI路徑取得相片檔案，並處理為AtomicReference<Bitmap>，方便後續旋轉圖片
            AtomicReference<Bitmap> getHighImage = new AtomicReference<>(BitmapFactory.decodeFile(mPath));
            Matrix matrix = new Matrix();
//            matrix.setRotate(90f);//轉90度
            getHighImage.set(Bitmap.createBitmap(getHighImage.get()
                    ,0,0
                    ,getHighImage.get().getWidth()
                    ,getHighImage.get().getHeight()
                    ,matrix,true));
            runOnUiThread(()->{
                //以Glide設置圖片(因為旋轉圖片屬於耗時處理，故會LAG一下，且必須使用Thread執行緒)
                Glide.with(this)
                        .load(getHighImage.get())
                        .centerCrop()
                        .into(picture);
            });
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
        if (mBleReceiver != null) {
            unregisterReceiver(mBleReceiver);
            mBleReceiver = null;
        }
        unbindService(mServiceConnection);
        mBleService = null;
    }
}