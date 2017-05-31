package com.viscovery.ad;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class VmapParser {
    private static final String TAG = "VmapParser";

    private static final String TAG_VMAP = "VMAP";
    private static final String TAG_AD_BREAK = "AdBreak";
    private static final String TAG_AD_SOURCE = "AdSource";
    private static final String TAG_AD_TAG_URI = "AdTagURI";

    private static final String ATTRIBUTE_TIME_OFFSET = "timeOffset";
    private static final String ATTRIBUTE_BREAK_TYPE = "breakType";
    private static final String ATTRIBUTE_TEMPLATE_TYPE = "templateType";

    static List<AdBreak> parse(String document) throws IOException, XmlPullParserException {
        final XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        final XmlPullParser parser = factory.newPullParser();
        final StringReader reader = new StringReader(document);
        parser.setInput(reader);

        final List<AdBreak> adBreaks = new ArrayList<>();
        parser.nextTag();
        parser.require(XmlPullParser.START_TAG, null, TAG_VMAP);
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() == XmlPullParser.START_TAG) {
                if (parser.getName().equals(TAG_AD_BREAK)) {
                    final String timeOffset = parser.getAttributeValue(null, ATTRIBUTE_TIME_OFFSET);
                    final String breakType = parser.getAttributeValue(null, ATTRIBUTE_BREAK_TYPE);
                    final AdBreak adBreak = new AdBreak(timeOffset, breakType);

                    while (parser.next() != XmlPullParser.END_TAG
                            || !parser.getName().equals(TAG_AD_BREAK)) {
                        if (parser.getEventType() == XmlPullParser.START_TAG) {
                            final String tag = parser.getName();
                            switch (tag) {
                                case TAG_AD_SOURCE: {
                                    final AdSource adSource = new AdSource();

                                    parser.nextTag();
                                    if (parser.getName().equals(TAG_AD_TAG_URI)) {
                                        final String templateType = parser.getAttributeValue(
                                                null, ATTRIBUTE_TEMPLATE_TYPE);
                                        final AdTagUri adTagUri = new AdTagUri(templateType);

                                        int type;
                                        do {
                                            type = parser.nextToken();
                                        } while (type != XmlPullParser.CDSECT);
                                        final String url = parser.getText().trim();
                                        adTagUri.setUrl(url);

                                        adSource.setAdTagUri(adTagUri);
                                    }

                                    adBreak.setAdSource(adSource);
                                    break;
                                }
                                default: {
                                    break;
                                }
                            }
                        }
                    }

                    adBreaks.add(adBreak);
                } else {
                    // TODO: throw exception
                }
            }
        }

        return adBreaks;
    }
}