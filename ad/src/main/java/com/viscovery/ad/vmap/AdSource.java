package com.viscovery.ad.vmap;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name = "AdSource", strict = false)
public class AdSource {
    @Element(name = "AdTagURI")
    private AdTagUri mAdTagUri;

    public AdTagUri getAdTagUri() {
        return mAdTagUri;
    }
}
