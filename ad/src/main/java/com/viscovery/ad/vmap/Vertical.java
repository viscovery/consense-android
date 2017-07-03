package com.viscovery.ad.vmap;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Text;

@Root(name = "vertical", strict = false)
public class Vertical {
    public static final String TYPE_BOTTOM = "bottom";

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
