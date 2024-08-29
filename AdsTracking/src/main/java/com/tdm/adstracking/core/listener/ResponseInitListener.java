package com.tdm.adstracking.core.listener;

public interface ResponseInitListener {
    void onInitSuccess(String url);
    void onInitFailed(String url, int code, String msg);
}
