package com.viscovery.consense;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;

public class VastParser {
    private static final String TAG = "VastParser";

    private static final String TAG_VAST = "VAST";

    private static final String TAG_NON_LINEAR = "NonLinear";
    private static final String ATTRIBUTE_WIDTH = "width";
    private static final String ATTRIBUTE_HEIGHT = "height";
    private static final String ATTRIBUTE_MIN_SUGGESTED_DURATION = "minSuggestedDuration";

    private static final String TAG_STATIC_RESOURCE = "StaticResource";

    private static final String TAG_NON_LINEAR_CLICK_THROUGH = "NonLinearClickThrough";
    private static final String TAG_NON_LINEAR_CLICK_TRACKING = "NonLinearClickTracking";
    private static final String TAG_AD_PARAMETERS = "AdParameters";

    static NonLinear parse(String vast) throws IOException, XmlPullParserException {
        final XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        final XmlPullParser parser = factory.newPullParser();
        final StringReader reader = new StringReader(vast);
        parser.setInput(reader);

        NonLinear nonLinear = null;
        parser.nextTag();
        parser.require(XmlPullParser.START_TAG, null, TAG_VAST);
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() == XmlPullParser.START_TAG
                    && parser.getName().equals(TAG_NON_LINEAR)) {
                final int width = Integer.parseInt(parser.getAttributeValue(null, ATTRIBUTE_WIDTH));
                final int height = Integer.parseInt(
                        parser.getAttributeValue(null, ATTRIBUTE_HEIGHT)
                );
                final String suggestedDuration =
                        parser.getAttributeValue(null, ATTRIBUTE_MIN_SUGGESTED_DURATION);
                String resourceUrl = null;
                String clickThroughUrl = null;
                String clickTrackingUrl = null;
                String adParameters = null;
                while (parser.next() != XmlPullParser.END_TAG
                        || !parser.getName().equals(TAG_NON_LINEAR)) {
                    if (parser.getEventType() == XmlPullParser.START_TAG) {
                        final String tag = parser.getName();
                        int type;
                        do {
                            type = parser.nextToken();
                        } while (type != XmlPullParser.CDSECT);
                        switch (tag) {
                            case TAG_STATIC_RESOURCE:
                                resourceUrl = parser.getText().trim();
                                break;
                            case TAG_NON_LINEAR_CLICK_THROUGH:
                                clickThroughUrl = parser.getText().trim();
                                break;
                            case TAG_NON_LINEAR_CLICK_TRACKING:
                                clickTrackingUrl = parser.getText().trim();
                                break;
                            case TAG_AD_PARAMETERS:
                                adParameters = parser.getText().trim();
                                break;
                            default:
                                break;
                        }
                    }
                }
                nonLinear = new NonLinear(width, height, resourceUrl);
                nonLinear.setSuggestedDuration(suggestedDuration);
                nonLinear.setClickThroughUrl(clickThroughUrl);
                nonLinear.setClickTrackingUrl(clickTrackingUrl);
                nonLinear.setAdParameters(adParameters);
            }
        }

        return nonLinear;
    }
}
