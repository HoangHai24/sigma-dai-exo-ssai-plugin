package com.tdm.adstracking.core.network;

import com.tdm.adstracking.core.listener.ResponseRequest;

import java.util.HashMap;

public class NetworkRequester {
    public static void initSession(String url, ResponseRequest listener) {
        RequestUtil.requestGetUrl(url, new HashMap<>(), listener);
    }

    public static void getTracking(String url, ResponseRequest listener) {
        RequestUtil.requestGetUrl(url, new HashMap<>(), listener);
    }

    public static void sendEventTracking(String url, ResponseRequest listener) {
        RequestUtil.requestGetUrl(url, new HashMap<>(), listener);
    }
}
