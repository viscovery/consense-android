package com.viscovery.vidsense.api;

import com.google.gson.annotations.SerializedName;

public class VmapResponse {
    @SerializedName("response_code")
    private int mCode;
    @SerializedName("message")
    private String mMessage;
    @SerializedName("status")
    private int mStatus;
    @SerializedName("context")
    private String mContent;

    public int getCode() {
        return mCode;
    }

    public String getMessage() {
        return mMessage;
    }

    public int getStatus() {
        return mStatus;
    }

    public String getContent() {
        return mContent;
    }
}
