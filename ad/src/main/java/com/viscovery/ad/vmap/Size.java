package com.viscovery.ad.vmap;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Text;

public class Size {
    public static final String TYPE_WIDTH = "width";

    @Attribute(name = "type")
    private String mType;

    @Text
    private String mValue;

    public String getType() {
        return mType;
    }

    public String getValue() {
        return mValue;
    }
}
