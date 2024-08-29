package com.tdm.adstracking.core.item;

import java.util.List;

public class CacheTrackingItem {
    private String id;
    private List<String> urlTrackingList;
    private long timeSend;

    public CacheTrackingItem(String id, List<String> urlTrackingList, long timeSend) {
        this.id = id;
        this.urlTrackingList = urlTrackingList;
        this.timeSend = timeSend;
    }

    public String getId() {
        return id;
    }

    public List<String> getUrlTrackingList() {
        return urlTrackingList;
    }

    public long getTimeSend() {
        return timeSend;
    }
}
