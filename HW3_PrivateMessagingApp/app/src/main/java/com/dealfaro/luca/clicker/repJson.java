package com.dealfaro.luca.clicker;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Alex on 4/30/2015.
 */
public class repJson {
    @SerializedName("msg")
    String msg;

    @SerializedName("msgid")
    String msgid;

    @SerializedName("ts")
    String ts;

    public String getMsg() {
        return msg;
    }

    public String getMsgid() {
        return msgid;
    }

    public String getTS() {
        return ts;
    }
}
