package com.kaltura.playkit.providers;

import android.util.Base64;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import androidx.annotation.Nullable;
import java.io.UnsupportedEncodingException;

public class BaseMediaAsset {

    private static final String KS = "ks";
    private static final String GSON = "gson";

    String ks;
    String referrer;

    public Gson gson;


    public String getKs() {
        return ks;
    }

    public BaseMediaAsset setKs(String ks) {
        this.ks = ks;
        return this;
    }

    public String getReferrer() {
        return referrer;
    }

    public BaseMediaAsset setReferrer(String referrer) {
        this.referrer = referrer;
        return  this;
    }

    public void createGsonObject() {

        if (gson != null) {
            return;
        }
        ExclusionStrategy strategy = new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes field) {
                if (field.getDeclaringClass() == BaseMediaAsset.class && KS.equals(field.getName())) {
                    return true;
                }
                if (field.getDeclaringClass() == BaseMediaAsset.class && GSON.equals(field.getName())) {
                    return true;
                }
                return false;
            }

            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                return false;
            }
        };

        gson = new GsonBuilder()
                .addSerializationExclusionStrategy(strategy)
                .create();
    }

    @Nullable
    public String toBase64(String mediaAssetJson) {
        byte[] data = new byte[0];
        try {
            data = mediaAssetJson.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
        return Base64.encodeToString(data, Base64.DEFAULT);
    }
}
