package com.nf.hpsweats;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.nf.hpsweats.util.GetToast;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends Activity implements View.OnClickListener {
    private static final long INTERVAL_TIME = 5 * 1000;
    private static final int REQUEST_COARSE_LOCATION = 0;
    @Bind(R.id.btn_scan)
    Button btnScan;
    @Bind(R.id.lv_devices)
    ListView lvDevices;
    @Bind(R.id.tv_hint)
    TextView tvHint;
    @Bind(R.id.btn_connect)
    Button btnConnect;
    @Bind(R.id.btn_test)
    Button btnTest;

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback;
    private BluetoothAdapter mBluetoothAdapter;
    private List<BluetoothDevice> devices = new ArrayList<>();
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            super.handleMessage(msg);
            switch (msg.what) {
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initBluetooth();
        initListener();
        mayRequestLocation();
    }

    /********
     * 初始化蓝牙
     *******/
    private void initBluetooth() {
        // 检查当前手机是否支持blue 蓝牙,如果不支持退出程序
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //扫描ble蓝牙的回调
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (device != null) {
                                if (!TextUtils.isEmpty(device.getName())) {
                                    if (device.getName().contains("NFHY")) {//"NFSMA"
                                        if (!devices.contains(device)) {
                                            devices.add(device);
                                        }
                                    }
                                }
                            }
                        }
                    });
                }
            };
        } else {
            GetToast.useString(this, "设备蓝牙版本过低");
            finish();
            return;
        }
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        }
    }

    private void initListener() {
        btnScan.setOnClickListener(this);
        btnConnect.setOnClickListener(this);
        btnTest.setOnClickListener(this);
    }

    private void mayRequestLocation() {
        if (Build.VERSION.SDK_INT >= 23) {
            int checkCallPhonePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
            if (checkCallPhonePermission != PackageManager.PERMISSION_GRANTED) {
                //判断是否需要 向用户解释，为什么要申请该权限
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION))
                    Toast.makeText(this, "动态请求权限", Toast.LENGTH_LONG).show();
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_COARSE_LOCATION);
                return;
            } else {

            }
        } else {

        }
    }

    //系统方法,从requestPermissions()方法回调结果
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //确保是我们的请求
        if (requestCode == REQUEST_COARSE_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "权限被授予", Toast.LENGTH_SHORT).show();
            } else if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "权限被拒绝", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_connect:
                Intent intent = new Intent(this, ConnectActivity2.class);
                String[] address = new String[devices.size()];
                for (int i = 0; i < devices.size(); i++) {
                    address[i] = devices.get(i).getAddress();
                }
                intent.putExtra("address", address);
                startActivity(intent);
                break;
            case R.id.btn_scan:
                scanLeDevice(true);
                break;
            default:
                break;
        }
    }

    /**
     * 扫描设备
     ********/
    private void scanLeDevice(final boolean enable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (enable) {
                tvHint.setText("查找设备中..");
                devices.clear();
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                // Stops scanning after a pre-defined scan period.
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                            mBluetoothAdapter.stopLeScan(mLeScanCallback);
                            try {
                                showBluetoothNames();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }, INTERVAL_TIME);
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            } else {
                try {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                } catch (Exception e) {
                }
            }
        }
    }

    private void showBluetoothNames() {
        if (devices.size() <= 0) {
            if (!isFinishing()) {
                tvHint.setText("未扫描到设备");
            }
            return;
        }
        final String[] deviceNames = new String[devices.size()];
        for (int i = 0; i < devices.size(); i++) {
            String address = devices.get(i).getAddress();
            deviceNames[i] = "汗液仪-" + address.replace(":", "");
        }
        lvDevices.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, deviceNames));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //关闭蓝牙
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.disable();
        }
    }

}
