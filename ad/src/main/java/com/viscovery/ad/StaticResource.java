package com.viscovery.ad;

public class StaticResource {
    public static final String CREATIVE_TYPE_GIF = "image/gif";
    public static final String CREATIVE_TYPE_JPEG = "image/jpeg";
    public static final String CREATIVE_TYPE_PNG = "image/png";
    public static final String CREATIVE_TYPE_JAVASCRIPT = "application/x-javascript";
    public static final String CREATIVE_TYPE_FLASH = "application/x-shockwave-flash";

    private String mCreativeType;
    private String mUri;

    public StaticResource(String creativeType, String uri) {
        mCreativeType = creativeType;
        mUri = uri;
    }

    public String getCreativeType() {
        return mCreativeType;
    }

    public String getUri() {
        return mUri;
    }
}
