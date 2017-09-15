package com.nf.hpsweats.util;

/**
 * Created by he_pan on 2017/9/15.
 * The only genius that is worth anything is the genius for hard work
 *
 * @author he_pan
 * @date 2017/9/15
 * @description
 */

public class ProtocolParser {
    private static String hexStr;
    private static String[] binaryArray;

    static {
        hexStr = "0123456789ABCDEF";
        binaryArray = new String[]{"0000", "0001", "0010", "0011", "0100", "0101", "0110", "0111", "1000", "1001", "1010", "1011", "1100", "1101", "1110", "1111"};
    }
    public static String binaryToHexString(byte[] bytes) {
        String result = "";
        String hex = "";

        for(int i = 0; i < bytes.length; ++i) {
            hex = String.valueOf(hexStr.charAt((bytes[i] & 240) >> 4));
            hex = hex + String.valueOf(hexStr.charAt(bytes[i] & 15));
            result = result + hex + " ";
        }

        return result;
    }
}
