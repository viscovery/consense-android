package com.viscovery.ad.vmap;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Text;

@Root(name = "horizontal", strict = false)
public class Horizontal {
    public static final String TYPE_CENTER = "center";
    public static final String TYPE_LEFT = "left";
    public static final String TYPE_RIGHT = "right";

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
