package com.nf.hpsweats;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.nf.hpsweats.util.Constants;
import com.nf.hpsweats.util.LogUtil;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by he_pan on 2017/9/15.
 * The only genius that is worth anything is the genius for hard work
 *
 * @author he_pan
 * @date 2017/9/15
 * @description
 */

public class ConnectActivity extends Activity {
    @Bind(R.id.btn_scan)
    Button btnScan;
    @Bind(R.id.btn_connect)
    Button btnConnect;
    @Bind(R.id.tv_hint)
    TextView tvHint;
    @Bind(R.id.lv_state)
    ListView lvState;
    private int index = -1;
    private String[] address;
    private ServiceConnection mServiceConnection;
    // 蓝牙服务类
    private SweatServiceFastBle mBluetoothLeService;
    private static final int CONNECT_DEVICES = 1;
    private static final int READ_DATA = 3;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case READ_DATA:
                    //读取数据命令
                    if (mBluetoothLeService != null) {
//                        mBluetoothLeService.sendCommandToSweat(SweatConfiguration.COMMAND_TYPE_READ_DATA, 10);
                        //重复读取,间隔为collection_frequency,但最少要间隔5秒
//                        if (collection_frequency >= 5) {
//                            removeMessages(READ_DATA);
//                            sendEmptyMessageDelayed(READ_DATA, collection_frequency * 1000);
//                        } else {
//                            removeMessages(READ_DATA);
//                            sendEmptyMessageDelayed(READ_DATA, 5 * 1000);
//                        }
                    }
                    break;
                case CONNECT_DEVICES:
                    if (++index < address.length) {
                        tvHint.setText("连接第" + index + "个设备,地址\n" + address[index]);
                        mBluetoothLeService.scanMacAndConnect(address[index]);
                    }
                    for (int i = 0; i < address.length; i++) {
//                        mBluetoothLeService.scanMacAndConnect(address[i]);
                    }

                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        ButterKnife.bind(this);
        address = getIntent().getStringArrayExtra("address");
        LogUtil.e(Constants.LOG_TAG, "设备数量" + address.length);
        bindLeService();
    }

    private void bindLeService() {
        mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName,
                                           IBinder service) {
                mBluetoothLeService = ((SweatServiceFastBle.LocalBinder) service).getService();
                if (mBluetoothLeService.initialize()) {
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mHandler.sendEmptyMessage(CONNECT_DEVICES);
                        }
                    }, 3000);
                } else {
                    tvHint.setText("初始化失败...");
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                mBluetoothLeService = null;
            }
        };

        Intent bleService = new Intent(this, SweatServiceFastBle.class);
        bindService(bleService, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (SweatServiceFastBle.ACTION_GATT_START_SCAN_AND_CONNECT.equals(action)) {
                tvHint.setText("尝试扫描并连接...");
            } else if (SweatServiceFastBle.ACTION_GATT_CONNECTED.equals(action)) {
                tvHint.setText("连接成功,尝试获取蓝牙服务...");
            } else if (SweatServiceFastBle.ACTION_GATT_DISCONNECTED.equals(action)) {
                tvHint.setText("蓝牙断开 3 秒后重连...");
            } else if (SweatServiceFastBle.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                //获得采样频率
//                mBluetoothLeService.sendCommandToSweat(SweatConfiguration.COMMAND_TYPE_SET_FREQ, 0);
                //延时发送读取数据
                mHandler.sendEmptyMessageDelayed(CONNECT_DEVICES, 2000);
                tvHint.setText("蓝牙就绪");
            } else if (SweatServiceFastBle.ACTION_DATA_AVAILABLE.equals(action)) {
                byte[] data = intent.getByteArrayExtra(SweatServiceFastBle.EXTRA_DATA);
                if (data == null) {
                    return;
                }
                try {
//                    handleFrameData(data);
                } catch (Exception e) {
                    LogUtil.exception(e);
                }
            } else if (SweatServiceFastBle.ACTION_GATT_DEVICE_NOT_FOUND.equals(action)) {
                tvHint.setText("未发现设备! 3秒后重连...");
            } else if (SweatServiceFastBle.ACTION_SEND_COMMAND_FAILURE.equals(action)) {
                tvHint.setText("发送命令失败!请确认蓝牙连接正常");
            }
        }

    };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, SweatServiceFastBle.makeGattUpdateIntentFilter());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
        //退出时还有未上传数据,上传
//        if (dataList.size() > 0) {
////            provider.AddSweatData("2", dataList, FLAG_ADD_DATA);
//        }
        if (mServiceConnection != null) {
            //做一些关闭蓝牙通道的操作
            unbindService(mServiceConnection);
        }
        mBluetoothLeService = null;
        if (mGattUpdateReceiver != null) {
            unregisterReceiver(mGattUpdateReceiver);
        }
    }
}
