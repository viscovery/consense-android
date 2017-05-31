package com.viscovery.vidsense;

public class AdSource {
    private AdTagUri mAdTagUri;

    AdSource() {
    }

    public void setAdTagUri(AdTagUri adTagUri) {
        mAdTagUri = adTagUri;
    }

    public AdTagUri getAdTagUri() {
        return mAdTagUri;
    }
}
