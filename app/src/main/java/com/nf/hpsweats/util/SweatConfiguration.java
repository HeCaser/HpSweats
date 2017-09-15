package com.nf.hpsweats.util;

/**
 * Created by he_pan on 2017/4/20.
 * The only genius that is worth anything is the genius for hard work
 *
 * @author he_pan
 */

public class SweatConfiguration {


    public static final String SERVICE_UUID = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E";
    public static final String WRITE_UUID = "6E400002-B5A3-F393-E0A9-E50E24DCCA9E";
    public static final String NOTIFY_UUID = "6E400003-B5A3-F393-E0A9-E50E24DCCA9E";

    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    public static final int COMMAND_TYPE_STATE = 3;//读取设备状态
    public static final int COMMAND_TYPE_START = 4;//开始
    public static final int COMMAND_TYPE_STOP = 5;//停止
    public static final int COMMAND_TYPE_READ_DATA = 6;//读取历史数据
    public static final int COMMAND_TYPE_SET_FREQ = 1;//设置频率


    //解析数据用到的几个类型
    public static final int PARSE_TYPE_ERROR = -1;//数据错误,校验失败,1970年,数据丢失等
    public static final int PARSE_TYPE_NO_NEED_HANDLE = -2;//不需要处理,例如最后一帧,没有有价值的信息
    public static final int PARSE_TYPE_READ_DATA = 0;//读取到数据
    public static final int PARSE_TYPE_READ_FREQ = 1;//获得频率

    //上传类型
 //   public static final int UPLOAD_TYPE_UP = 1;//13路数据
  //  public static final int UPLOAD_TYPE_DOWN = 2;//7路身数据
}
