package com.nf.hpsweats.util;


import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by he_pan on 2017/3/21.
 * The only genius that is worth anything is the genius for hard work
 *
 * @author he_pan
 */

public class SweatBluetoothUtils {
    //public static int UP_MAX_CHANNEL = 13;//上身最大测量数
    //public static int UP_MAX_CHANNEL = 13;//上身最大测量数
    public static int DOWN_CURRENT_CHANNEL = 2;//当前只有两路

    /**
     * @param commandType 0 读取命令 1 设置采集频率
     * @param sampleFre   采集频率,当commandType为1时用到
     * @return 返回命令byte[]
     */
    public static byte[] getCommandByte(int commandType, int sampleFre) {
        if (sampleFre < 0 || sampleFre > 300) {
            return new byte[]{1};//
        }
        //现用集合记录,因为发送命令长度是不确定的
        ArrayList<Byte> writeByteList = new ArrayList<>();
        //1,包头NF0106
        writeByteList.add((byte) 0x4E);
        writeByteList.add((byte) 0x46);
        writeByteList.add((byte) 0x00);
        writeByteList.add((byte) 0x09);

        //2，通配地址  8字节AA
        for (int i = 0; i < 6; i++) {
            writeByteList.add((byte) 0xAA);
        }

        //3,当前时间 8字节
        byte nowTime[] = new byte[4];
        //时间戳占四个字节
        long dataLong = System.currentTimeMillis();//1500960053
        writeByteList.add((byte) (dataLong / 0x1000000));
        writeByteList.add((byte) ((dataLong % 0x1000000) / 0x10000));
        writeByteList.add((byte) ((dataLong % 0x10000) / 0x100));
        writeByteList.add((byte) (dataLong % 0x100));

        //4,标志位 1字节 0代表最后一帧，大于0时表示帧序号，且还有下一帧
        writeByteList.add((byte) 0x00);
        writeByteList.add((byte) 0x00);

//        sweatOrderTypeNoOpention = 0x00 ,//没有操作过。
//                sweatOrderTypeState= 0x03 ,//读取汗液仪的状态。返回==> 00无操作，03正在读 ，04停止状态
//                sweatOrderTypeStart = 0x04,//开始读数据
//                sweatOrderTypeStop = 0x05 ,//停止读数据
//                sweatOrderTypeRead = 0x06 ,//读取历史数据
//                sweatOrderTypeCollectOver  = 0xff,//历史数据已经读取完毕

        //5，命令 4字节
        if (commandType == SweatConfiguration.COMMAND_TYPE_START) {
            writeByteList.add((byte) 0x00);
            writeByteList.add((byte) 0x04);
        } else if (commandType == SweatConfiguration.COMMAND_TYPE_STOP) {
            writeByteList.add((byte) 0x00);
            writeByteList.add((byte) 0x05);
        } else if (commandType == SweatConfiguration.COMMAND_TYPE_READ_DATA) {
            writeByteList.add((byte) 0x00);
            writeByteList.add((byte) 0x06);
        }

        //6 标识两位 0x00 0x00
        writeByteList.add((byte) 0x00);
        writeByteList.add((byte) 0x00);

        //命令数据 长度2位(固定) 数据位不固定
        writeByteList.add((byte) 0x00);
        writeByteList.add((byte) 0x00);


        //8，校验位 1位  list还原为byte数组,添加校验和结束位后发送命令
        byte[] writeByte = new byte[writeByteList.size() + 3];
        for (int i = 0; i < writeByteList.size(); i++) {
            writeByte[i] = writeByteList.get(i);
        }
        byte crcCheck = CRC8.calcCrc8(Arrays.copyOf(writeByte, writeByte.length - 3));
        writeByte[writeByte.length - 3] = crcCheck;
        //9结束位 2位
        writeByte[writeByte.length - 2] = 0x2E;
        writeByte[writeByte.length - 1] = 0x0A;
        return writeByte;
    }


    private static ArrayList<byte[]> listData = new ArrayList<>();//存储数据(蓝牙一次接收的byte)
    private static ArrayList<byte[]> frameData = new ArrayList<>();//完整一帧的数据

    private static ArrayList<byte[]> listDataDown = new ArrayList<>();//存储数据(蓝牙一次接收的byte)
    private static ArrayList<byte[]> frameDataDown = new ArrayList<>();//完整一帧的数据

//
}
