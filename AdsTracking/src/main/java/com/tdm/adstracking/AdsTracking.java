package com.tdm.adstracking;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.VoiceInteractor;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.TimedMetaData;
import android.net.Uri;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.ads.interactivemedia.pal.ConsentSettings;
import com.google.ads.interactivemedia.pal.NonceLoader;
import com.google.ads.interactivemedia.pal.NonceManager;
import com.google.ads.interactivemedia.pal.NonceRequest;
import com.google.android.exoplayer2.DeviceInfo;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.MediaMetadata;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.Tracks;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.id3.Id3Frame;
import com.google.android.exoplayer2.metadata.id3.TextInformationFrame;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.text.CueGroup;
import com.google.android.exoplayer2.trackselection.TrackSelectionParameters;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.video.VideoSize;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.tdm.adstracking.core.ThreadUtil;
import com.tdm.adstracking.core.item.CacheTrackingItem;
import com.tdm.adstracking.core.listener.ResponseInitListener;
import com.tdm.adstracking.core.listener.ResponseRequest;
import com.tdm.adstracking.core.network.NetworkRequester;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.regex.Pattern;

public class AdsTracking {
    public static String TAG_LISTENER = "AdsTracking_listener";
    private static AdsTracking instance;
    private InitSessionListener initSessionListener;
    private GetTrackingUrlListener getTrackingUrlListener;
    private HashMap<String, JSONObject> cacheTrackingItems = new HashMap<>();
    private NonceLoader nonceLoader;
    private NonceManager nonceManager;
    private Activity currentActivity;
    private ResponseInitListener responseInitListener;
    private String currentSessionUrl = "";
    private String urlSourceFullPath = "";
    private String urlTrackingFullPath = "";
    private ExoPlayer exoPlayer;
    private StyledPlayerView playerView;
    private ImageView iconAdsTrackingView; // Tham chiếu tới ImageView
    private JSONObject iconAdsTrackingData = null;

    public static AdsTracking getInstance() {
        if (instance == null) {
            instance = new AdsTracking();
        }
        return instance;
    }

    public void generateNonceForAdRequest() {
        FullLog.d("generateNonceForAdRequest=>>Generate Nonce Request: " + "width: " + playerView.getWidth() + ", height: "+ playerView.getHeight());

        NonceRequest nonceRequest =
                NonceRequest.builder()
                        .omidVersion("1.0.0")
                        .playerType("ExoPlayer")
                        .playerVersion("2.19.x")
                        .videoPlayerHeight(playerView.getHeight())
                        .videoPlayerWidth(playerView.getWidth())
                        .willAdAutoPlay(true)
                        .willAdPlayMuted(true)
                        .build();

        nonceLoader
                .loadNonceManager(nonceRequest)
                .addOnSuccessListener(
                        new OnSuccessListener<NonceManager>() {
                            @Override
                            public void onSuccess(NonceManager manager) {
                                nonceManager = manager;
                                String nonceString = manager.getNonce();
                                FullLog.d("generateNonceForAdRequest=>>Generated nonce: " + nonceString);
                                // From here you would trigger your ad request and move on to initialize content.
                                FullLog.d("generateNonceForAdRequest=>>Nonce generated: " + currentSessionUrl + "?play_params.nonce="+ nonceString);
                                handleAfterGetNonce(currentSessionUrl + "?play_params.nonce="+ nonceString);
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception error) {
                                FullLog.d("Nonce generation failed");
                                FullLog.d("Nonce generation failed: " + error.getMessage());
                                handleAfterGetNonce(currentSessionUrl);
                            }
                        });
    }

    public void handleAfterGetNonce(String urlSource){
        initSession(
                urlSource,
                new InitSessionListener() {
                    @Override
                    public void onResponse(int code, String url) {
                        currentActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                FullLog.d("InitSessionListener=>>" + url);
                                responseInitListener.onInitSuccess(url);
                            }
                        });
                    }
                    @Override
                    public void onError(int code, String msg) {
                        FullLog.d("InitSessionListener=>>Error_" + code + ":" + msg);
                        responseInitListener.onInitFailed(currentSessionUrl,code, msg);
                    }
                });
    }


    public void init(Activity activity, StyledPlayerView view, String url, ResponseInitListener resInitListener) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        this.currentSessionUrl = url;
        this.currentActivity = activity;
        this.responseInitListener = resInitListener;
        boolean isStorageAllowed = getConsentToStorage();

        ConsentSettings consentSettings =
                ConsentSettings.builder().allowStorage(isStorageAllowed).build();
        nonceLoader = new NonceLoader(this.currentActivity, consentSettings);
        playerView = view;
        generateNonceForAdRequest();
    }

    public void initPlayerView(ExoPlayer exo, String url){
        exoPlayer = exo;
//        playerView = view;
        exoPlayer.addListener(listener);
    }

    private boolean getConsentToStorage() {
        // Code to ask the user for storage consent.
        return false;
    }

    public void initSession(String urlInit, InitSessionListener listener) {

        this.initSessionListener = listener;
        getTrackingUrlListener = new GetTrackingUrlListener() {
            @Override
            public void onResponse(int code, String url) {
                urlTrackingFullPath = url;
            }
            @Override
            public void onError(int code, String msg) {
            }
        };

        if (initSessionListener != null) {
            ThreadUtil.runInBackground(() -> NetworkRequester.initSession(urlInit, new ResponseRequest() {
                @Override
                public void onResponse(int responseCode, JSONObject res) {
                    FullLog.d("initSession=>>onResponse_" + responseCode + ": " +  res);
                    if (res != null){
                        String sourceUrl = res.optString("manifestUrl", "");
                        String trackingUrl = res.optString("trackingUrl", "");
                        if (!sourceUrl.startsWith("http") && !sourceUrl.startsWith("https")) {
                            String regex = "^([a-z]+:)?[\\\\/]";
                            Pattern pattern = Pattern.compile(regex, Pattern.UNICODE_CASE);
                            if (pattern.matcher(sourceUrl).find()) {
                                try {
                                    URL url = new URL(urlInit);
                                    urlSourceFullPath = url.getProtocol() + "://" + url.getAuthority() + sourceUrl;
                                } catch (MalformedURLException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                try {
                                    URL url = new URL(new URL(urlInit), sourceUrl);
                                    urlSourceFullPath = url.toString();
                                } catch (MalformedURLException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        if (!trackingUrl.startsWith("http") && !trackingUrl.startsWith("https")) {
                            String regex = "^([a-z]+:)?[\\\\/]";
                            Pattern pattern = Pattern.compile(regex, Pattern.UNICODE_CASE);
                            if (pattern.matcher(trackingUrl).find()) {
                                try {
                                    URL url = new URL(urlInit);
                                    urlTrackingFullPath = url.getProtocol() + "://" + url.getAuthority() + trackingUrl;

                                } catch (MalformedURLException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                try {
                                    URL url = new URL(new URL(urlInit), trackingUrl);
                                    urlTrackingFullPath = url.toString();
                                } catch (MalformedURLException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        Log.d("Full=>>>urlTrackingFull", urlTrackingFullPath);
                        Log.d("Full=>>>urlSourceFull", urlSourceFullPath);
                        getTrackingUrlListener.onResponse(responseCode, urlTrackingFullPath);
                        initSessionListener.onResponse(responseCode, urlSourceFullPath);
                    }

                }

                @Override
                public void onErrors(int errCode, String mgs) {
                    FullLog.e("InitSessionListener is require! " + errCode + " -- " + mgs);
                    initSessionListener.onError(errCode, mgs);
                    getTrackingUrlListener.onError(errCode, mgs);
                }
            }));
        } else {
            FullLog.d("InitSessionListener is require!");
        }
    }

    private final Runnable runnableTracking = () -> {
        if (urlTrackingFullPath.length() > 0) {
            ThreadUtil.runInBackground(new Runnable() {
                @Override
                public void run() {
                    NetworkRequester.getTracking(urlTrackingFullPath, new ResponseRequest() {
                        @Override
                        public void onResponse(int responseCode, JSONObject res)  {
                            FullLog.d("AdsTracking_runnableTracking_urlTrackingFullPath: "+ urlTrackingFullPath);
                            FullLog.d("AdsTracking_runnableTracking_onResponse: " + res);
                            JSONArray availsArrays = res.optJSONArray("avails");
                            allLoop: for(int i = 0; i < Objects.requireNonNull(availsArrays).length(); i++){
                                JSONObject realAvails = availsArrays.optJSONObject(i);

                                JSONArray adsAvails = realAvails.optJSONArray("ads");
                                FullLog.d("AdsTracking_runnableTracking_adsAvails: " + adsAvails);
                                if (adsAvails != null){
                                    for(int j = 0; j < adsAvails.length(); j++){
                                        JSONObject adsItem = adsAvails.optJSONObject(j);
                                        JSONArray iconAds = adsItem.optJSONArray("icons"); // icons dang tra ve la 1 mang
                                        if(iconAds != null){
                                            for (int icn = 0; icn < iconAds.length(); icn++){
                                                JSONObject itemIcon = iconAds.optJSONObject(icn);
                                                if(itemIcon.optString("program").equals("GoogleWhyThisAd")){ // lay icon quang cao dau tien co program = GoogleWhyThisAd
                                                    iconAdsTrackingData = itemIcon;
                                                    onDrawIcon();
                                                    break;
                                                }
                                            }
                                        }
                                        if(!cacheTrackingItems.containsKey(adsItem.optString("id"))){
                                            //luu lai quang cao dau tien trong adsAvails ma chua duoc goi trong danh sach cacheTrackingItems
                                            cacheTrackingItems.put(adsItem.optString("id"), adsItem);
                                            JSONArray jsonArrayTrackingEvents = adsItem.optJSONArray("trackingEvents");
                                            for(int k = 0; k < Objects.requireNonNull(jsonArrayTrackingEvents).length(); k++){
                                                JSONObject itemTrackingEvent = jsonArrayTrackingEvents.optJSONObject(k);
                                                String eventTypeItemTrackingEvent = itemTrackingEvent.optString("eventType");
                                                if (eventTypeItemTrackingEvent.equals("start") || eventTypeItemTrackingEvent.equals("impression")) {
                                                    JSONArray jsonArrayBeaconUrls = itemTrackingEvent.optJSONArray("beaconUrls");
                                                    try{
                                                        itemTrackingEvent.put("isCalled", true);
                                                    }catch (JSONException e){
                                                        FullLog.d("241_itemTrackingEvent_put_iscalled_Error");
                                                    }

                                                    for(int x = 0; x < Objects.requireNonNull(jsonArrayBeaconUrls).length(); x++){
                                                        NetworkRequester.sendEventTracking(jsonArrayBeaconUrls.optString(x), new ResponseRequest() {
                                                            @Override
                                                            public void onResponse(int responseCode, JSONObject res) {
                                                                FullLog.d("sendEventTracking=>>Success_248");
                                                            }
                                                            @Override
                                                            public void onErrors(int errCode, String mgs) {
                                                                FullLog.e("sendEventTracking=>>Error " + mgs + " -- " + errCode);
                                                            }
                                                        });
                                                    }
                                                }
                                            }
                                            break allLoop;
                                        }
                                    }
                                }
                            }
                        }
                        @Override
                        public void onErrors(int errCode, String mgs) {
                            FullLog.e("AdsTracking_runnableTracking_Errors: " + errCode + " -- " + mgs);
                        }
                    });
                }
            });
        } else {
            FullLog.e("Runnable Tracking exception: " + "URL Tracking is NULL");
        }
    };

    public void onDrawIcon() {
        currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String xPositionIcon = iconAdsTrackingData.optString("xPosition");
                String yPositionIcon = iconAdsTrackingData.optString("yPosition");

                double widthIcon = iconAdsTrackingData.optDouble("width");
                double heightIcon = iconAdsTrackingData.optDouble("height");
                JSONObject staticResourceIcon = iconAdsTrackingData.optJSONObject("staticResource");
                assert staticResourceIcon != null;
                String sourceIcon = staticResourceIcon.optString("uri");
                JSONObject iconClickThrough = iconAdsTrackingData.optJSONObject("iconClickThrough");
                assert iconClickThrough != null;
                String urlClickIcon = iconClickThrough.optString("data");


                iconAdsTrackingView = new ImageView(currentActivity);
//                String imageUrl = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSt3q9O1lr3vhTXJD7Oq7y0EJATknCP3U8f-A&s";
                Glide.with(currentActivity)
                        .load(sourceIcon)
                        .into(iconAdsTrackingView);
                float density = currentActivity.getResources().getDisplayMetrics().density;

                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        (int) (widthIcon * density),
                        (int) (heightIcon * density)

                );
                // Xử lý vị trí xPosition
                if (xPositionIcon.equals("left")) {
                    params.gravity = Gravity.START;
                } else if (xPositionIcon.equals("right")) {
                    params.gravity = Gravity.END;
                } else {
                    try {
                        float xValue = Float.parseFloat(xPositionIcon);
                        params.leftMargin = (int) xValue;
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
                // Xử lý vị trí yPosition
                if (yPositionIcon.equals("top")) {
                    params.gravity |= Gravity.TOP;
                } else if (yPositionIcon.equals("bottom")) {
                    params.gravity |= Gravity.BOTTOM;
                } else {
                    try {
                        float yValue = Float.parseFloat(yPositionIcon);
                        params.topMargin = (int) yValue;
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
                iconAdsTrackingView.setLayoutParams(params);

                playerView.addView(iconAdsTrackingView);
                iconAdsTrackingView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Đường dẫn URL cần mở
//                        String url = "https://adssettings.google.com/whythisad";

                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(urlClickIcon));
                        currentActivity.startActivity(intent);
                    }
                });
            }
        });
    }
    public void onRemoveIcon(){
        if (iconAdsTrackingView != null) {
            playerView.removeView(iconAdsTrackingView);
            iconAdsTrackingView = null;
        }
    }
    public void requestTracking(String id3Frame) {
        FullLog.d("AdsTracking_requestTracking: " + id3Frame);
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(id3Frame, JsonObject.class);

        // Truy cập các giá trị trong đối tượng JSON
        String eventFromId3 = jsonObject.get("event").getAsString();
        String adIdFromId3 = jsonObject.get("adId").getAsString();
        String timeFromId3 = jsonObject.get("time").getAsString();
        FullLog.d("AdsTracking_requestTracking=>>288" + "event: " + eventFromId3 + ",adId:" + adIdFromId3 + ",time:" + timeFromId3);

        if (eventFromId3.equals("start")){
            cacheTrackingItems.clear();
            runnableTracking.run();
            iconAdsTrackingData = null;
            iconAdsTrackingView = null;
        }else{
            if(eventFromId3.equals("complete")){
                onRemoveIcon();
            }
            if(cacheTrackingItems.containsKey(adIdFromId3)){
                JSONObject cacheAdsItemId3 = cacheTrackingItems.get(adIdFromId3);
                assert cacheAdsItemId3 != null;
                JSONArray jsonArrayTrackingEvents = cacheAdsItemId3.optJSONArray("trackingEvents");
                for(int i = 0; i < Objects.requireNonNull(jsonArrayTrackingEvents).length(); i++){
                    JSONObject itemTrackingEvent = jsonArrayTrackingEvents.optJSONObject(i);
                    String eventTypeItemTrackingEvent = itemTrackingEvent.optString("eventType");
                    boolean isCalledItemTrackingEvent = itemTrackingEvent.optBoolean("isCalled");

                    FullLog.d("cacheTrackingItems_containsKey_adIdFromId3=>>eventTypeItemTrackingEvent" + eventTypeItemTrackingEvent);
                    FullLog.d("cacheTrackingItems_containsKey_adIdFromId3=>>isCalled: " + "id:" + adIdFromId3 + ",eventTypeItemTrackingEvent: " + eventTypeItemTrackingEvent + ",has: " + itemTrackingEvent.has("isCalled") + ",isCalledItemTrackingEvent: " + isCalledItemTrackingEvent);

                    //kiem tra eventType cua quang cao nay da goi hay chua, chua goi thi goi
                    if(eventTypeItemTrackingEvent.equals(eventFromId3) && (!itemTrackingEvent.has("isCalled") || !isCalledItemTrackingEvent)){
                        FullLog.d("AdsTracking_requestTracking=>>308" + "event: " + eventFromId3);

                        try{
                            itemTrackingEvent.put("isCalled", true);
                        }catch (JSONException e){
                            FullLog.d("304_itemTrackingEvent_put_iscalled_Error");
                        }

                        JSONArray jsonArrayBeaconUrls = itemTrackingEvent.optJSONArray("beaconUrls");
                        FullLog.d("jsonArrayBeaconUrls=>>308" + jsonArrayBeaconUrls);
                        for(int x = 0; x < Objects.requireNonNull(jsonArrayBeaconUrls).length(); x++){
                            FullLog.d("jsonArrayBeaconUrlsLoop=>>311" + jsonArrayBeaconUrls.optString(x));
                            NetworkRequester.sendEventTracking(jsonArrayBeaconUrls.optString(x), new ResponseRequest() {
                                @Override
                                public void onResponse(int responseCode, JSONObject res) {
                                    FullLog.d("sendEventTracking=>>Success_312");
                                }
                                @Override
                                public void onErrors(int errCode, String mgs) {
                                    FullLog.e("sendEventTracking=>>Error " + mgs + " -- " + errCode);
                                }
                            });
                        }
                    }
                }
            }
        }
    }

    public void destroy() {
        clearAll();
    }
    private void clearAll() {
        instance = null;
        nonceManager = null;
        responseInitListener = null;
        currentSessionUrl  = "";
        urlSourceFullPath  = "";
        urlTrackingFullPath  = null;
        initSessionListener = null;
        getTrackingUrlListener = null;
        cacheTrackingItems.clear();
        iconAdsTrackingView = null;
        iconAdsTrackingData = null;
    }

    private final Player.Listener listener = new Player.Listener() {
        @Override
        public void onMetadata(@NonNull Metadata metadata) {
            Player.Listener.super.onMetadata(metadata);
            ArrayList<TimedMetaData> metadataArray = new ArrayList<>();
            for (int i = 0; i < metadata.length(); i++) {
                Metadata.Entry entry = metadata.get(i);
                if (entry instanceof Id3Frame) {
                    Id3Frame frame = (Id3Frame) metadata.get(i);
                    String value = "";
                    if (frame instanceof TextInformationFrame) {
                        TextInformationFrame txxxFrame = (TextInformationFrame) frame;
                        value = txxxFrame.value;
                    }
                    requestTracking(value);
                }
            }
        }
        @Override
        public void onEvents(@NonNull Player player, @NonNull Player.Events events) {
            Log.d(TAG_LISTENER, "onEvents: " + new Gson().toJson(events));
            Player.Listener.super.onEvents(player, events);
        }

        @Override
        public void onTimelineChanged(Timeline timeline, int reason) {
            Log.d(TAG_LISTENER, "onTimelineChanged: " + new Gson().toJson(timeline));
            Player.Listener.super.onTimelineChanged(timeline, reason);
        }

        @Override
        public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
            Player.Listener.super.onMediaItemTransition(mediaItem, reason);
        }

        @Override
        public void onTracksChanged(Tracks tracks) {
            Log.d(TAG_LISTENER, "onTracksChanged: " + new Gson().toJson(tracks));
            Player.Listener.super.onTracksChanged(tracks);
        }

        @Override
        public void onMediaMetadataChanged(MediaMetadata mediaMetadata) {
            Log.d(TAG_LISTENER, "onMediaMetadataChanged: " + new Gson().toJson(mediaMetadata));
            Player.Listener.super.onMediaMetadataChanged(mediaMetadata);
        }

        @Override
        public void onPlaylistMetadataChanged(MediaMetadata mediaMetadata) {
            Log.d(TAG_LISTENER, "onPlaylistMetadataChanged: " + new Gson().toJson(mediaMetadata));
            Player.Listener.super.onPlaylistMetadataChanged(mediaMetadata);
        }

        @Override
        public void onIsLoadingChanged(boolean isLoading) {
            Player.Listener.super.onIsLoadingChanged(isLoading);
        }

        @Override
        public void onAvailableCommandsChanged(Player.Commands availableCommands) {
            Player.Listener.super.onAvailableCommandsChanged(availableCommands);
        }

        @Override
        public void onTrackSelectionParametersChanged(TrackSelectionParameters parameters) {
            Log.d(TAG_LISTENER, "onTrackSelectionParametersChanged: " + new Gson().toJson(parameters));
            Player.Listener.super.onTrackSelectionParametersChanged(parameters);
        }

        @Override
        public void onPlaybackStateChanged(int playbackState) {
            FullLog.d("MainAc_onPlaybackStateChanged" + playbackState);
            if (playbackState == ExoPlayer.STATE_READY) {
                if(nonceManager != null){
                    nonceManager.sendPlaybackStart();
                }
            } else if (playbackState == ExoPlayer.STATE_ENDED) {
                if(nonceManager != null){
                    nonceManager.sendPlaybackEnd();
                }
            }
            Player.Listener.super.onPlaybackStateChanged(playbackState);
        }

        @Override
        public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
            Player.Listener.super.onPlayWhenReadyChanged(playWhenReady, reason);
        }

        @Override
        public void onPlaybackSuppressionReasonChanged(int playbackSuppressionReason) {
            Player.Listener.super.onPlaybackSuppressionReasonChanged(playbackSuppressionReason);
        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            Player.Listener.super.onIsPlayingChanged(isPlaying);
        }

        @Override
        public void onRepeatModeChanged(int repeatMode) {
            Player.Listener.super.onRepeatModeChanged(repeatMode);
        }

        @Override
        public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
            Player.Listener.super.onShuffleModeEnabledChanged(shuffleModeEnabled);
        }

        @Override
        public void onPlayerError(PlaybackException error) {
            Log.e(TAG_LISTENER, "onPlayerError: " + new Gson().toJson(error));
            Player.Listener.super.onPlayerError(error);
        }

        @Override
        public void onPlayerErrorChanged(@Nullable PlaybackException error) {
            Player.Listener.super.onPlayerErrorChanged(error);
        }

        @Override
        public void onPositionDiscontinuity(int reason) {
            Player.Listener.super.onPositionDiscontinuity(reason);
        }

        @Override
        public void onPositionDiscontinuity(Player.PositionInfo oldPosition, Player.PositionInfo newPosition, int reason) {
            Player.Listener.super.onPositionDiscontinuity(oldPosition, newPosition, reason);
        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
            Player.Listener.super.onPlaybackParametersChanged(playbackParameters);
        }

        @Override
        public void onSeekBackIncrementChanged(long seekBackIncrementMs) {
            Player.Listener.super.onSeekBackIncrementChanged(seekBackIncrementMs);
        }

        @Override
        public void onSeekForwardIncrementChanged(long seekForwardIncrementMs) {
            Player.Listener.super.onSeekForwardIncrementChanged(seekForwardIncrementMs);
        }

        @Override
        public void onMaxSeekToPreviousPositionChanged(long maxSeekToPreviousPositionMs) {
            Player.Listener.super.onMaxSeekToPreviousPositionChanged(maxSeekToPreviousPositionMs);
        }

        @Override
        public void onSeekProcessed() {
            Player.Listener.super.onSeekProcessed();
        }

        @Override
        public void onAudioSessionIdChanged(int audioSessionId) {
            Player.Listener.super.onAudioSessionIdChanged(audioSessionId);
        }

        @Override
        public void onAudioAttributesChanged(AudioAttributes audioAttributes) {
            Player.Listener.super.onAudioAttributesChanged(audioAttributes);
        }

        @Override
        public void onVolumeChanged(float volume) {
            Player.Listener.super.onVolumeChanged(volume);
        }

        @Override
        public void onSkipSilenceEnabledChanged(boolean skipSilenceEnabled) {
            Player.Listener.super.onSkipSilenceEnabledChanged(skipSilenceEnabled);
        }

        @Override
        public void onDeviceInfoChanged(DeviceInfo deviceInfo) {
            Player.Listener.super.onDeviceInfoChanged(deviceInfo);
        }

        @Override
        public void onDeviceVolumeChanged(int volume, boolean muted) {
            Player.Listener.super.onDeviceVolumeChanged(volume, muted);
        }

        @Override
        public void onVideoSizeChanged(VideoSize videoSize) {
            Player.Listener.super.onVideoSizeChanged(videoSize);
        }

        @Override
        public void onSurfaceSizeChanged(int width, int height) {
            Player.Listener.super.onSurfaceSizeChanged(width, height);
        }

        @Override
        public void onRenderedFirstFrame() {
            Player.Listener.super.onRenderedFirstFrame();
        }

        @Override
        public void onCues(CueGroup cueGroup) {
            Player.Listener.super.onCues(cueGroup);
        }
    };


    public interface InitSessionListener {
        void onResponse(int code, String url);

        void onError(int code, String msg);
    }

    public interface GetTrackingUrlListener {
        void onResponse(int code, String url);

        void onError(int code, String msg);
    }
}


