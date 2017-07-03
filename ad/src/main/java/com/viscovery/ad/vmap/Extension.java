package com.viscovery.ad.vmap;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.ElementListUnion;
import org.simpleframework.xml.Root;

import java.util.List;

@Root(name = "Extension", strict = false)
public class Extension {
    public static final String TYPE_POSITION = "position";
    public static final String TYPE_SIZE = "size";

    @Attribute(name = "type")
    private String mType;

    @ElementListUnion({
            @ElementList(entry = "placement", inline = true, type = Placement.class),
            @ElementList(entry = "horizontal", inline = true, type = Horizontal.class),
            @ElementList(entry = "vertical", inline = true, type = Vertical.class),
            @ElementList(entry = "size", inline = true, type = Size.class)
    })
    private List<Object> mValues;

    public String getType() {
        return mType;
    }

    public List<Object> getValues() {
        return mValues;
    }
}
