package com.nf.hpsweats;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;

import com.clj.fastble.BleManager;
import com.clj.fastble.conn.BleCharacterCallback;
import com.clj.fastble.conn.BleGattCallback;
import com.clj.fastble.exception.BleException;
import com.nf.hpsweats.util.LogUtil;
import com.nf.hpsweats.util.SweatConfiguration;

/**
 * Created by he_pan on 2017/3/21.
 * The only genius that is worth anything is the genius for hard work
 *
 * @author he_pan
 * @Description 简化蓝牙服务类
 */

public class SweatServiceFastBle extends Service {
    private static final int SCAN_AND_CONNECT = 10;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case SCAN_AND_CONNECT:
                    scanMacAndConnect(mAddress);
                    break;
                default:
                    break;
            }
        }
    };
    private BleManager mBleManager;
    private String mAddress;
    private final int TIME_OUT = 3 * 1000;//扫描连接超时
    private final int RECONNECT_WAIT_TIME = 3 * 1000;//重连等待时间

    public class LocalBinder extends Binder {
        public SweatServiceFastBle getService() {
            return SweatServiceFastBle.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle("爱尚康");
        builder.setContentText("I am the bluetooth service");
        builder.setContentInfo("Content Info");
        builder.setWhen(System.currentTimeMillis());
        Intent activityIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);
        Notification notification = builder.build();
        startForeground(2, notification);
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mBleManager.closeBluetoothGatt();
        mBleManager.stopListenConnectCallback();
        mBleManager.disableBluetooth();
        handler.removeCallbacksAndMessages(null);
        handler = null;
        return super.onUnbind(intent);
    }

    /**
     * 初始化蓝牙,成功返回true
     *
     * @return
     */
    public boolean initialize() {
        mBleManager = new BleManager(this);
        if (mBleManager.isSupportBle()) {
            mBleManager.enableBluetooth();
            return true;
        } else {
            return false;
        }

    }

    public void scanMacAndConnect(final String macAddress) {
        mAddress = macAddress;
        broadcastUpdate(ACTION_GATT_START_SCAN_AND_CONNECT);
        mBleManager.scanMacAndConnect(macAddress, TIME_OUT, false, new BleGattCallback() {
            @Override
            public void onNotFoundDevice() {
                broadcastUpdate(ACTION_GATT_DEVICE_NOT_FOUND);
                System.out.println(macAddress + " 没有设备");
                if (handler != null) {
                    handler.removeMessages(SCAN_AND_CONNECT);//移除残留队列的重连
                    handler.sendEmptyMessageDelayed(SCAN_AND_CONNECT, RECONNECT_WAIT_TIME);
                }
            }

            @Override
            public void onConnectSuccess(BluetoothGatt gatt, int status) {
                broadcastUpdate(ACTION_GATT_CONNECTED);
                System.out.println("设备连接成功,地址: " + macAddress);
                gatt.discoverServices();
            }

            @Override
            public void onConnectFailure(BleException exception) {
                broadcastUpdate(ACTION_GATT_DISCONNECTED);
                System.out.println("设备连接失败,地址: "+ macAddress);
                if (handler != null) {
//                    handler.removeMessages(SCAN_AND_CONNECT);//移除残留队列的重连
//                    handler.sendEmptyMessageDelayed(SCAN_AND_CONNECT, RECONNECT_WAIT_TIME);
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                if (handler != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            setNotify();
                        }
                    });
                }
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                try {
                    broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                } catch (Exception e) {
                    LogUtil.exception(e);
                }
            }
        });
    }

    /**
     * 设置数据通知
     */
    private void setNotify() {
        mBleManager.notify(
                SweatConfiguration.SERVICE_UUID,
                SweatConfiguration.NOTIFY_UUID,
                new BleCharacterCallback() {//这个回调要单独写,若写成全局

                    @Override
                    public void onFailure(BleException exception) {
                        broadcastUpdate(ACTION_SEND_COMMAND_FAILURE);
                    }

                    @Override
                    public void onSuccess(BluetoothGattCharacteristic characteristic) {
                        //实测,这个回调从未触发 ?
//                        LogUtil.e("设置通知回调线程="+Thread.currentThread().getName());
//                        final byte[] receivedData = characteristic.getValue();
//                        try {
//                            LogUtil.e("notify接收数据" + ProtocolParser.binaryToHexString(receivedData) + ">>>" + receivedData.length);
//                            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
//                        } catch (Exception e) {
//                            LogUtil.exception(e);
//                        }
                    }
                });
    }

//    public boolean sendCommandToSweat(int commandType, int sampleFre) {
//        //获得发送命令
//        byte[] writeByte = SweatBluetoothUtils.getCommandByte(commandType, sampleFre);
//
//        try {
//            if (commandType == SweatConfiguration.COMMAND_TYPE_READ_DATA) {
//                LogUtil.e("设备1发送读取命令", ProtocolParser.binaryToHexString(writeByte) + ">>>>" + writeByte.length);
//            } else if (commandType == SweatConfiguration.COMMAND_TYPE_SET_FREQ) {
//                LogUtil.e("设备1发送频率命令", ProtocolParser.binaryToHexString(writeByte) + ">>>>" + writeByte.length);
//            }
//            byte[] writeByteOne = new byte[20];
//            byte[] writeByteTwo = new byte[writeByte.length - 20];
//            System.arraycopy(writeByte, 0, writeByteOne, 0, 20);
//            System.arraycopy(writeByte, 20, writeByteTwo, 0, writeByteTwo.length);
//            LogUtil.e("设备1分块", ProtocolParser.binaryToHexString(writeByteOne) + "\n" + ProtocolParser.binaryToHexString(writeByteTwo));
//            mBleManager.writeDevice(SweatConfiguration.SERVICE_UUID, SweatConfiguration.WRITE_UUID, writeByteOne, mBleCharacterCallback);
//            SystemClock.sleep(20);
//            mBleManager.writeDevice(SweatConfiguration.SERVICE_UUID, SweatConfiguration.WRITE_UUID, writeByteTwo, mBleCharacterCallback);
//        } catch (Exception e) {
//            LogUtil.e("设备1发送命令失败");
//            LogUtil.exception(e);
//            return false;
//        }
//        return true;
//    }

    /**
     * 发送命令的回调
     */
    private final BleCharacterCallback mBleCharacterCallback = new BleCharacterCallback() {

        @Override
        public void onFailure(BleException exception) {
            broadcastUpdate(ACTION_SEND_COMMAND_FAILURE);
            LogUtil.e("设备1发送命令失败回调,地址: "+ mAddress + exception.getDescription());
        }

        @Override
        public void onSuccess(BluetoothGattCharacteristic characteristic) {
            //实测,此处回调每次发命令触发,但不是所需要的
            LogUtil.e("设备1发送命令成功回调");
        }
    };


    public static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SweatServiceFastBle.ACTION_GATT_CONNECTED);
        intentFilter.addAction(SweatServiceFastBle.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(SweatServiceFastBle.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(SweatServiceFastBle.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(SweatServiceFastBle.ACTION_GATT_DEVICE_NOT_FOUND);
        intentFilter.addAction(SweatServiceFastBle.ACTION_GATT_CONNECT_FAILURE);
        intentFilter.addAction(SweatServiceFastBle.ACTION_GATT_START_SCAN_AND_CONNECT);
        intentFilter.addAction(SweatServiceFastBle.ACTION_SEND_COMMAND_FAILURE);
        return intentFilter;
    }

    public final static String ACTION_GATT_START_SCAN_AND_CONNECT = "com.nf.down.service.ACTION_GATT_START_SCAN_AND_CONNECT";
    public final static String ACTION_SEND_COMMAND_FAILURE = "com.nf.down.service.ACTION_SEND_COMMAND_FAILURE";
    public final static String ACTION_GATT_CONNECTED = "com.nf.down.service.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DEVICE_NOT_FOUND = "com.nf.down.service.ACTION_GATT_DEVICE_NOT_FOUND";
    public final static String ACTION_GATT_CONNECT_FAILURE = "com.nf.down.service.ACTION_GATT_CONNECT_FAILURE";
    public final static String ACTION_GATT_DISCONNECTED = "com.nf.down.service.ACTION_GATT_ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.nf.down..service.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.nf.down.service.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "com.nf.down.service.EXTRA_DATA";

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        final byte[] data = characteristic.getValue();
        intent.putExtra(EXTRA_DATA, data);
        sendBroadcast(intent);
    }
}
