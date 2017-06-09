package com.viscovery.ad.vast;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name = "Ad", strict = false)
public class Ad {
    @Element(name = "InLine")
    private InLine mInLine;

    public InLine getInLine() {
        return mInLine;
    }
}
