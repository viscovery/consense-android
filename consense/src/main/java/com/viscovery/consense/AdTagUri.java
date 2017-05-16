package com.viscovery.consense;

public class AdTagUri {
    public static final String TEMPLATE_TYPE_VAST1 = "vast1";
    public static final String TEMPLATE_TYPE_VAST2 = "vast2";
    public static final String TEMPLATE_TYPE_VAST3 = "vast3";
    public static final String TEMPLATE_TYPE_PROPRIETARY = "proprietary";

    private String mTemplateType;
    private String mUrl;

    AdTagUri(String templateType) {
        mTemplateType = templateType;
    }

    public String getTemplateType() {
        return mTemplateType;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    public String getUrl() {
        return mUrl;
    }
}
