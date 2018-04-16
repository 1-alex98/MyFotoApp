package com.example.alex.sarasfotoapp;

import java.io.BufferedInputStream;
import java.io.IOException;

/**
 * Created by alexa on 07.03.2018.
 */

class IOUtils {
    public static byte[] bytesToInt(int size) {
        byte[] bytes= new byte[8];
        for(int i=7;i>=0;i--){
            int temp=size;
            for(int j=7;j>i;j--){
                temp= temp-(temp%(Byte.MAX_VALUE+1));
                temp= temp/(Byte.MAX_VALUE+1);
            }
            bytes[i]= (byte) (temp%(Byte.MAX_VALUE+1));
        }
        return bytes;
    }

    public static int bytesToInt(byte[] bytes) {
        int res=0;
        for(int i=0; i<8;i++){
            res+=bytes[i]*Math.pow((Byte.MAX_VALUE+1),(7-i));
        }

        return res;
    }

    public static int readInt(BufferedInputStream fileInputStream) throws IOException {
        byte[] bytes= new byte[8];
        fileInputStream.read(bytes);
        return IOUtils.bytesToInt(bytes);
    }
}
