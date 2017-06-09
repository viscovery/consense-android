package com.viscovery.ad.vmap;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Text;

@Root(name = "AdTagURI", strict = false)
public class AdTagUri {
    public static final String TEMPLATE_TYPE_VAST1 = "vast1";
    public static final String TEMPLATE_TYPE_VAST2 = "vast2";
    public static final String TEMPLATE_TYPE_VAST3 = "vast3";
    public static final String TEMPLATE_TYPE_PROPRIETARY = "proprietary";

    @Attribute(name = "templateType")
    private String mTemplateType;

    @Text
    private String mValue;

    public String getTemplateType() {
        return mTemplateType;
    }

    public String getValue() {
        return mValue;
    }
}
