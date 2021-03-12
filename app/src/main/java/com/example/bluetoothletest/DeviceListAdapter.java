package com.example.bluetoothletest;

import android.bluetooth.BluetoothDevice;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DeviceListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    private static final String TAG = "DeviceListAdapter";

    private OnItemClickListener mItemClickListener;
    private List<BluetoothDevice> mBluetoothDeviceList;
    private List<String> mRssiList;

    public DeviceListAdapter(List<BluetoothDevice> mBluetoothDeviceList, List<String> mRssiList) {
        this.mBluetoothDeviceList = mBluetoothDeviceList;
        this.mRssiList = mRssiList;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.adapter_device_list, viewGroup, false);
        return new DeviceListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, final int position) {
        final DeviceListViewHolder deviceListViewHolder = (DeviceListViewHolder) viewHolder;
        BluetoothDevice bluetoothDevice = mBluetoothDeviceList.get(position);
        String rssi = mRssiList.get(position);

        deviceListViewHolder.tvName.setText(bluetoothDevice.getName());
        deviceListViewHolder.tvMac.setText(bluetoothDevice.getAddress());
        deviceListViewHolder.tvRssi.setText("Rssi = " + rssi);
        //Log.d(TAG, "onBindViewHolder: " + bluetoothDevice.getName());

        // 回调点击事件
        if (mItemClickListener != null) {
            deviceListViewHolder.rlInfo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mItemClickListener.onItemClick(deviceListViewHolder.rlInfo, position);
                }
            });
        }

    }

    @Override
    public int getItemCount() {
        return mBluetoothDeviceList.size();
    }

    private class DeviceListViewHolder extends RecyclerView.ViewHolder {

        RelativeLayout rlInfo;
        TextView tvName;
        TextView tvMac;
        TextView tvRssi;

        DeviceListViewHolder(View itemView) {
            super(itemView);
            rlInfo = itemView.findViewById(R.id.rl_info);
            tvName = itemView.findViewById(R.id.tv_name);
            tvMac = itemView.findViewById(R.id.tv_mac);
            tvRssi = itemView.findViewById(R.id.tv_rssi);
        }
    }

    /**
     * 设置Item点击监听
     *
     * @param onItemClickListener 点击回调接口
     */
    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.mItemClickListener = onItemClickListener;
    }

    /**
     * 点击回调接口
     */
    public interface OnItemClickListener {
        /**
         * 点击回调方法
         *
         * @param view     当前view
         * @param position 点击位置
         */
        void onItemClick(View view, int position);
    }
}
