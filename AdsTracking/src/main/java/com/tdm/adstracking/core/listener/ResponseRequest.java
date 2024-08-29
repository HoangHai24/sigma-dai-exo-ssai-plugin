package com.tdm.adstracking.core.listener;

import org.json.JSONException;
import org.json.JSONObject;

public interface ResponseRequest {
    void onResponse(int responseCode, JSONObject res) throws JSONException;
    void onErrors(int errCode, String mgs);
}
