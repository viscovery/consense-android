package com.viscovery.ad.vmap;

import android.util.Log;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.lang.reflect.Type;

public class VmapTypeAdapter implements JsonDeserializer<Vmap> {
    @Override
    public Vmap deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        final String text = json.getAsString();
        final Serializer serializer = new Persister();
        try {
            return serializer.read(Vmap.class, text);
        } catch (Exception e) {
            Log.d("VmapTypeAdapter", e.getMessage());
            throw new JsonParseException("Invalid VMAP document");
        }
    }
}
