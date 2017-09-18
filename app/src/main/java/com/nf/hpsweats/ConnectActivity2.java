package com.nf.hpsweats;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.nf.hpsweats.util.Constants;
import com.nf.hpsweats.util.LogUtil;
import com.nf.hpsweats.util.ProtocolParser;
import com.nf.hpsweats.util.SweatBluetoothUtils;
import com.nf.hpsweats.util.SweatConfiguration;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

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

public class ConnectActivity2 extends Activity implements View.OnClickListener {
    @Bind(R.id.tv_hint)
    TextView tvHint;
    @Bind(R.id.tv_hint2)
    TextView tvHint2;
    @Bind(R.id.lv_state)
    ListView lvState;

    StringBuffer sb = new StringBuffer();
    @Bind(R.id.btn_start)
    Button btnStart;
    @Bind(R.id.btn_stop)
    Button btnStop;
    @Bind(R.id.btn_read_history)
    Button btnReadHistory;
    @Bind(R.id.btn_clear)
    Button btnClear;
    private int index = -1;
    private int retry_time = 0;
    private String[] address;
    private BluetoothAdapter mBTAdapter;
    private HashMap<String, BluetoothGatt> mBluetoothGatt;


    private static final int CHECK_INTERVAL = 7 * 1000;
    private static final int CONNECT = 1;
    private static final int CHECK_SUCCESS = 2;


    private static class MyHandler extends Handler {
        private final WeakReference<ConnectActivity2> mActivity;

        public MyHandler(ConnectActivity2 activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            ConnectActivity2 activity = mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case CONNECT:
                        activity.connect();
                        break;
                    case CHECK_SUCCESS:
                        activity.checkSuccess();
                        break;
                    default:
                        break;
                }
            }
        }
    }


    private final MyHandler mHandler = new MyHandler(this);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect2);
        ButterKnife.bind(this);
        address = getIntent().getStringArrayExtra("address");
        mBluetoothGatt = new HashMap<>();
        LogUtil.e(Constants.LOG_TAG, "设备数量" + address.length);
        initListener();
        initBluetooth();
    }

    private void initListener() {
        btnReadHistory.setOnClickListener(this);
        btnStart.setOnClickListener(this);
        btnStop.setOnClickListener(this);
        btnClear.setOnClickListener(this);
    }

    private void initBluetooth() {
        //获取蓝牙适配器
        BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBTAdapter = manager.getAdapter();
        //打开蓝牙,建议使用下面方式
        if (!mBTAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, 33);
        }
        //延时发送连接蓝牙
        mHandler.sendEmptyMessageDelayed(CONNECT, 1000);
    }

    public boolean connect() {
        if (++index < address.length) {
            tvHint.setText("连接第" + index + "个设备,地址\n" + address[index]);
        } else {
            return false;
        }
        BluetoothDevice bleDevie = mBTAdapter.getRemoteDevice(address[index]);
        if (bleDevie == null) {
            LogUtil.e(Constants.LOG_TAG, "Device not found. Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        BluetoothGatt mConnGatt = bleDevie.connectGatt(this, false, mGattcallback);
        mHandler.sendEmptyMessageDelayed(CHECK_SUCCESS, CHECK_INTERVAL);//产看是否连接成功
        return true;
    }

    /**
     * 查看是否连接成功
     */
    private void checkSuccess() {
        mHandler.removeCallbacksAndMessages(null);
        if (index >= address.length) {
            tvHint.setText("连接完毕,设备数: " + address.length + "  连接数: " + mBluetoothGatt.size());
            return;
        }
        String add = address[index];
        if (mBluetoothGatt.containsKey(add)) {
            //成功了
            retry_time = 0;
            mHandler.sendEmptyMessageDelayed(CONNECT, 500);
            mHandler.sendEmptyMessageDelayed(CHECK_SUCCESS, CHECK_INTERVAL);
            sb.append("蓝牙就绪,地址 " + add + "\n");
            tvHint2.setText(sb.toString());
        } else {
            //失败了
            retry_time++;
            if (retry_time >= 2) {
                retry_time = 0;
                tvHint.setText("重试次数结束,连接下一个..");
                mHandler.sendEmptyMessageDelayed(CONNECT, 2000);
                mHandler.sendEmptyMessageDelayed(CHECK_SUCCESS, CHECK_INTERVAL);
            } else {
                tvHint.setText("连接失败,尝试重连..,地址:\n " + add);
                index--;
                mHandler.sendEmptyMessageDelayed(CONNECT, 2000);
                mHandler.sendEmptyMessageDelayed(CHECK_SUCCESS, CHECK_INTERVAL);
            }

        }
    }

    /*Gatt回调*/
    private BluetoothGattCallback mGattcallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
            String address = gatt.getDevice().getAddress();
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        GetToast.useString(cnt, "设备连接成功");
                        gatt.connect();
                        //搜索支持的 服务
                        gatt.discoverServices();
                    }
                });

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
//                GetToast.useString(cnt, "设备断开连接");
                tvHint2.setText("设备断开,地址: " + address);
                mBluetoothGatt.remove(address);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            String address = gatt.getDevice().getAddress();
            if (!mBluetoothGatt.containsKey(address)) {
                LogUtil.e(Constants.LOG_TAG, "发现服务:地址 " + address);
                mBluetoothGatt.put(address, gatt);
            }

//            BluetoothGattService service = gatt.getService(UUID.fromString(SweatConfiguration.SERVICE_UUID));
//            writeCharacter = service.getCharacteristic(UUID.fromString(SweatConfiguration.NOTIFY_UUID));
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            System.out.println("读取数据");
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            System.out.println("写入数据");
            setCharacteristicNotification(gatt, UUID.fromString(SweatConfiguration.SERVICE_UUID), UUID.fromString(SweatConfiguration.NOTIFY_UUID), true);
        }


        @SuppressLint("NewApi")
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            final String res = new String(characteristic.getValue());
            LogUtil.e(Constants.LOG_TAG, "收到数据: " + ProtocolParser.binaryToHexString(characteristic.getValue()));
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
//                    handleResult(res);

                }
            });
        }
    };

    /**
     * 设置收到数据提醒
     */
    @SuppressLint("NewApi")
    public boolean setCharacteristicNotification(BluetoothGatt gatt, UUID serviceUuid, UUID characteristicUuid, boolean enable) {
        BluetoothGattCharacteristic characteristic = gatt.getService(serviceUuid).getCharacteristic(characteristicUuid);
        gatt.setCharacteristicNotification(characteristic, enable);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(SweatConfiguration.CLIENT_CHARACTERISTIC_CONFIG));
        descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : new byte[]{0x00, 0x00});
        return gatt.writeDescriptor(descriptor); // descriptor write
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_read_history:
                sendCmdToDevice(SweatConfiguration.COMMAND_TYPE_READ_DATA);
                break;
            case R.id.btn_start:
                sendCmdToDevice(SweatConfiguration.COMMAND_TYPE_START);
                break;
            case R.id.btn_stop:
                sendCmdToDevice(SweatConfiguration.COMMAND_TYPE_STOP);
                break;
            case R.id.btn_clear:
                sb = new StringBuffer();
                tvHint2.setText("");
                break;
            default:
                break;
        }
    }

    private boolean sendCmdToDevice(int cmdType) {
        String add;
        BluetoothGatt gatt;
        BluetoothGattService service;
        BluetoothGattCharacteristic writeCharacter;
        try {
            if (mBluetoothGatt.size() == 0) {
                tvHint2.setText("所有设备均已断开,请重新连接.");
                return true;
            }
            Iterator iter = mBluetoothGatt.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                add = (String) entry.getKey();
                gatt = (BluetoothGatt) entry.getValue();
                service = gatt.getService(UUID.fromString(SweatConfiguration.SERVICE_UUID));
                if (service == null) {
                    tvHint.setText("设备: " + add + " 未发现服务");
//                    continue;
                }
                writeCharacter = service.getCharacteristic(UUID.fromString(SweatConfiguration.WRITE_UUID));
                if (writeCharacter == null) {
                    tvHint.setText("设备: " + add + " 未发现写入特征");
//                    continue;
                }
                //获得发送命令
                byte[] writeByte = SweatBluetoothUtils.getCommandByte(cmdType, 10);
                byte[] writeByteOne = new byte[20];
                byte[] writeByteTwo = new byte[writeByte.length - 20];
                System.arraycopy(writeByte, 0, writeByteOne, 0, 20);
                System.arraycopy(writeByte, 20, writeByteTwo, 0, writeByteTwo.length);
                LogUtil.e("设备1分块", ProtocolParser.binaryToHexString(writeByteOne) + "\n" + ProtocolParser.binaryToHexString(writeByteTwo));
                writeCharacter.setValue(writeByteOne);
                if (gatt.writeCharacteristic(writeCharacter)) {
                    sb.append("设备: " + add + " 发送命令1成功\n");
                    tvHint2.setText(sb.toString());
                } else {
                    sb.append("设备: " + add + " 发送命令1失败\n");
                    tvHint2.setText(sb.toString());
                }
//                SystemClock.sleep(20);
                writeCharacter.setValue(writeByteTwo);
                if (gatt.writeCharacteristic(writeCharacter)) {
                    sb.append("设备: " + add + " 发送命令2成功\n");
                    tvHint2.setText(sb.toString());
                } else {
                    sb.append("设备: " + add + " 发送命令2失败\n");
                    tvHint2.setText(sb.toString());
                }
                SystemClock.sleep(500);//下一个设备延后发送命令
            }
        } catch (Exception e) {
            tvHint.setText("设备 发送命令失败");
            e.printStackTrace();
            return false;
        } finally {
            return true;
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (int i = 0; i < mBluetoothGatt.size(); i++) {
            try {
                mBluetoothGatt.get(i).close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

//        //关闭蓝牙
//        if (mBTAdapter != null) {
//            mBTAdapter.disable();
//        }
    }

}
