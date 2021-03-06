package com.viscovery.ad.vmap;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

@Root(name = "placement", strict = false)
public class Placement {
    public static final String TYPE_INSTREAM = "instream";
    public static final String TYPE_OUTSTREAM = "outstream";

    @Attribute(name = "type")
    private String mType;

    public String getType() {
        return mType;
    }
}
