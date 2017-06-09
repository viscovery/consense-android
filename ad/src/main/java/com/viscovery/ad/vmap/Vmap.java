package com.viscovery.ad.vmap;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

@Root(name = "VMAP", strict = false)
public class Vmap {
    @ElementList(inline = true)
    private List<AdBreak> mAdBreaks;

    public List<AdBreak> getAdBreaks() {
        return mAdBreaks;
    }
}
