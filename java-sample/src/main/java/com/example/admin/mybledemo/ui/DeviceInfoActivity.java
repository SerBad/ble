package com.example.admin.mybledemo.ui;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.admin.mybledemo.R;
import com.example.admin.mybledemo.Utils;
import com.example.admin.mybledemo.adapter.DeviceInfoAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.BleLog;
import cn.com.heaton.blelibrary.ble.callback.BleConnectCallback;
import cn.com.heaton.blelibrary.ble.callback.BleNotifyCallback;
import cn.com.heaton.blelibrary.ble.model.BleDevice;
import cn.com.heaton.blelibrary.ble.utils.ByteUtils;

public class DeviceInfoActivity extends AppCompatActivity {

    public static final String EXTRA_TAG = "device";
    private static final String TAG = "DeviceInfoActivity";
    private BleDevice bleDevice;
    private Ble<BleDevice> ble;
    private ActionBar actionBar;
    private RecyclerView recyclerView;
    private DeviceInfoAdapter adapter;
    private List<BluetoothGattService> gattServices;
    private BleConnectCallback<BleDevice> connectCallback = new BleConnectCallback<BleDevice>() {
        @Override
        public void onConnectionChanged(BleDevice device) {
            Log.e(TAG, "onConnectionChanged: " + device.getConnectionState() + Thread.currentThread().getName());
            if (device.isConnected()) {
                actionBar.setSubtitle("已连接");
            } else if (device.isConnecting()) {
                actionBar.setSubtitle("连接中...");
            } else if (device.isDisconnected()) {
                actionBar.setSubtitle("未连接");
            }
        }

        @Override
        public void onConnectFailed(BleDevice device, int errorCode) {
            super.onConnectFailed(device, errorCode);
            Utils.showToast("连接异常，异常状态码:" + errorCode);
        }

        @Override
        public void onConnectCancel(BleDevice device) {
            super.onConnectCancel(device);
            Log.e(TAG, "onConnectCancel: " + device.getBleName());
        }

        @Override
        public void onServicesDiscovered(BleDevice device, BluetoothGatt gatt) {
            super.onServicesDiscovered(device, gatt);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    gattServices.addAll(gatt.getServices());
                    adapter.notifyDataSetChanged();
                }
            });

        }

        @Override
        public void onReady(BleDevice device) {
            super.onReady(device);
            //连接成功后，设置通知
            ble.enableNotify(device, true, new BleNotifyCallback<BleDevice>() {
                @Override
                public void onChanged(BleDevice device, BluetoothGattCharacteristic characteristic) {
                    UUID uuid = characteristic.getUuid();
                    BleLog.e(TAG, "onChanged==uuid:" + uuid.toString());
                    BleLog.e(TAG, "onChanged==data:" + ByteUtils.toHexString(characteristic.getValue()));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Utils.showToast(String.format("收到设备通知数据: %s", ByteUtils.toHexString(characteristic.getValue())));
                        }
                    });
                }

                @Override
                public void onNotifySuccess(BleDevice device) {
                    super.onNotifySuccess(device);
                    BleLog.e(TAG, "onNotifySuccess: " + device.getBleName());
                }
            });
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deviceinfo);
        initView();
        initData();
    }

    private void initData() {
        ble = Ble.getInstance();
        bleDevice = getIntent().getParcelableExtra(EXTRA_TAG);
        if (bleDevice == null) return;
        ble.connect(bleDevice, connectCallback);
    }

    private void initView() {
        actionBar = getSupportActionBar();
        actionBar.setTitle("详情信息");
        actionBar.setDisplayHomeAsUpEnabled(true);

        recyclerView = findViewById(R.id.recyclerView);
        gattServices = new ArrayList<>();
        adapter = new DeviceInfoAdapter(this, gattServices);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        recyclerView.getItemAnimator().setChangeDuration(300);
        recyclerView.getItemAnimator().setMoveDuration(300);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:// 点击返回图标事件
                this.finish();
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bleDevice != null) {
            if (bleDevice.isConnecting()) {
                ble.cancelConnecting(bleDevice);
            } else if (bleDevice.isConnected()) {
                ble.disconnect(bleDevice);
            }
        }
        ble.cancelCallback(connectCallback);
    }
}
