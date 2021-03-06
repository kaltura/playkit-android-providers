/*
 * ============================================================================
 * Copyright (C) 2017 Kaltura Inc.
 *
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 *
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.playkit.providers.mock;

import android.content.Context;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.kaltura.netkit.connect.response.ResultElement;
import com.kaltura.netkit.utils.CallableLoader;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.netkit.utils.OnCompletion;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.providers.MediaEntryProvider;
import com.kaltura.playkit.providers.base.OnMediaLoadCompletion;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.kaltura.playkit.PKMediaEntry.MediaEntryType.Unknown;

/**
 * Created by tehilarozin on 06/11/2016.
 */

public class MockMediaProvider implements MediaEntryProvider {

    public static final String TAG = "MockMediaProvider";

    private JsonObject inputJson;
    private String inputFile;
    private String id;
    private Context context;

    private Future<Void> currentLoad;
    private ExecutorService exSvc;
    private Object syncObject = new Object();

    public MockMediaProvider(JsonObject inputJson, String id) {
        this.inputJson = inputJson;
        this.id = id;
        exSvc = Executors.newSingleThreadExecutor();//?? do i need this?
    }

    public MockMediaProvider(String inputFile, Context context, String id) {
        this.inputFile = inputFile;
        this.id = id;
        this.context = context;
        exSvc = Executors.newSingleThreadExecutor();//?? do i need this?
    }

    public MockMediaProvider id(String id) {
        this.id = id;
        return this;
    }

    @Override
    public void load(final OnMediaLoadCompletion completion) {
        synchronized (syncObject) {
            cancel();
            currentLoad = exSvc.submit(new MockMediaProvider.Loader(completion));
        }
    }

    @Override
    public synchronized void cancel() {
        if (currentLoad != null && !currentLoad.isDone() && !currentLoad.isCancelled()) {
            PKLog.i(TAG, "has running load operation, canceling current load operation - " + currentLoad.toString());
            currentLoad.cancel(true);
        } else {
            PKLog.i(TAG, (currentLoad != null ? currentLoad.toString() : "") + ": no need to cancel operation," + (currentLoad == null ? "operation is null" : (currentLoad.isDone() ? "operation done" : "operation canceled")));
        }
    }

    /**
     * reads entries Json input from a file. the data is "saved" in the inputJson member.
     * next load on the same {@link MockMediaProvider} object will not read the file again, but
     * use the inputJson member, and search for the "id" entry.
     *
     * @return - The Media entry identified by "id"
     * @throws IOException
     */
    private PKMediaEntry getFromFile() throws IOException {
        JsonParser parser = new JsonParser();

        if (context != null) {
            JsonReader jsonReader = new JsonReader(new InputStreamReader(context.getAssets().open(inputFile)));
            inputJson = parser.parse(jsonReader).getAsJsonObject();
        } else {
            inputJson = parser.parse(new FileReader(inputFile)).getAsJsonObject();
        }

        return getFromJson(inputJson);
    }

    private PKMediaEntry getFromJson(JsonObject data) throws JsonSyntaxException {
        if (data.has(id)) { // data holds multiple entry objects
            return MockMediaParser.parseMedia(data.getAsJsonObject(id));

        } else if (data.has("id") && data.getAsJsonPrimitive("id").getAsString().equals(id)) { // data is the actual inputJson object
            return MockMediaParser.parseMedia(data);
        }
        return null;
    }


    static class MockMediaParser {

        static PKMediaEntry parseMedia(JsonObject mediaObject) throws JsonSyntaxException {

            PKMediaEntry mediaEntry = new Gson().fromJson(mediaObject, PKMediaEntry.class);
            if (mediaEntry.getMediaType() == null) {
                mediaEntry.setMediaType(Unknown);
            }
            List<PKMediaSource> mediaSources = mediaEntry.getSources();
            for (PKMediaSource mediaSource : mediaSources) {
                PKMediaFormat format = PKMediaFormat.valueOfUrl(mediaSource.getUrl());
                if (format == null) {
                    String mimeType = getMimeTypeFromJson(mediaObject);
                    if (mimeType != null) {
                        if (mimeType.equals(PKMediaFormat.dash.mimeType)) {
                            format = PKMediaFormat.dash;
                        } else if (mimeType.equals(PKMediaFormat.hls.mimeType)) {
                            format = PKMediaFormat.hls;
                        } else if (mimeType.equals(PKMediaFormat.wvm.mimeType)) {
                            format = PKMediaFormat.wvm;
                        } else if (mimeType.equals(PKMediaFormat.mp4.mimeType)) {
                            format = PKMediaFormat.mp4;
                        } else if (mimeType.equals(PKMediaFormat.mp3.mimeType)) {
                            format = PKMediaFormat.mp3;
                        }
                    }
                }
                mediaSource.setMediaFormat(format);
            }
            return mediaEntry;
        }
    }

    @Nullable
    private static String getMimeTypeFromJson(JsonObject mediaObject) {
        String mimeType = null;
        try {
            JSONObject jsonObj = new JSONObject(mediaObject.toString());
            JSONArray sources = jsonObj.getJSONArray("sources");
            if (sources != null && sources.length() > 0) {
                JSONObject sourcesJson = sources.getJSONObject(0);
                mimeType = sourcesJson.getString("mimeType");
                return mimeType;
            }
        } catch (JSONException e) {
            //e.printStackTrace();
            PKLog.w(TAG, "Sources does not contain mime type in it - hope url extention is valid...");
        }
        return mimeType;
    }


    class Loader extends CallableLoader<Void> {

        ErrorElement error = null;
        PKMediaEntry mediaEntry = null;

        Loader(OnCompletion completion) {
            super(MockMediaProvider.TAG, completion);
        }


        @Override
        protected Void load() throws InterruptedException {
            //parse Json input to MediaEntry
            try {
                mediaEntry = inputJson != null ? getFromJson(inputJson) : getFromFile();

            } catch (IOException | JsonSyntaxException ex) {
                error = ErrorElement.LoadError;
                mediaEntry = null;
            }

            if (mediaEntry == null) {
                error = ErrorElement.NotFound;
            }

            if (completion != null) {
                final ErrorElement finalError = error;
                final PKMediaEntry finalEntry = mediaEntry;
                completion.onComplete(new ResultElement<PKMediaEntry>() {

                    @Override
                    public PKMediaEntry getResponse() {
                        return finalEntry;
                    }

                    @Override
                    public boolean isSuccess() {
                        return finalEntry != null;
                    }

                    @Override
                    public ErrorElement getError() {
                        return finalError;
                    }
                });
            }
            return null;
        }

        @Override
        protected synchronized void cancel() {

        }
    }
}
