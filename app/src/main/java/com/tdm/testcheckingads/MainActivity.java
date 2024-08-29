package com.tdm.testcheckingads;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.media.TimedMetaData;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.ads.interactivemedia.v3.api.ImaSdkFactory;
import com.google.ads.interactivemedia.v3.api.ImaSdkSettings;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DeviceInfo;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.MediaMetadata;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.Tracks;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.ext.ima.ImaAdsLoader;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.id3.Id3Frame;
import com.google.android.exoplayer2.metadata.id3.TextInformationFrame;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceFactory;
import com.google.android.exoplayer2.source.ads.AdsLoader;
import com.google.android.exoplayer2.source.ads.AdsMediaSource;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.CueGroup;
import com.google.android.exoplayer2.trackselection.TrackSelectionParameters;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.video.VideoSize;
import com.google.gson.Gson;
import com.tdm.adstracking.AdsTracking;
//import com.tdm.adstracking.AdsTracking;
import com.tdm.adstracking.FullLog;
import com.tdm.adstracking.core.listener.ResponseInitListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class MainActivity extends AppCompatActivity implements  Player.Listener {
    ExoPlayer exoPlayer;
    StyledPlayerView playerView;

    public String SESSION_URL =
            "https://ssai-stream-dev.sigmaott.com/manifest/manipulation/session/bea37c7f-bea6-4fc4-8a49-6a2dc385f2b8/origin04/scte35-av4s-clear/master.m3u8";
//            "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8";
//            "http://playertest.longtailvideo.com/adaptive/wowzaid3/playlist.m3u8";
//            "https://ssai-stream-dev.sigmaott.com/manifest/manipulation/session/7d47b94e-7e65-4f9f-9fcf-9f104032ac0d/origin04/scte35-av4s-clear/master.m3u8";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playerView = findViewById(R.id.player_view_id);

        Activity mainActivity = this;
        playerView.post(new Runnable() {
            @Override
            public void run() {
                // Lấy width và height sau khi playerView đã được layout
                int width = playerView.getWidth();
                int height = playerView.getHeight();

                // In ra kích thước
                FullLog.d("PlayerViewSize=>>" + "Width: " + width + ", Height: " + height);
                AdsTracking.getInstance().init(
                        mainActivity,
                        playerView,
                        SESSION_URL,
                        new ResponseInitListener() {
                            @Override
                            public void onInitSuccess(String url) {
                                FullLog.d("AdsTracking=>>onInitSuccess" + url);
                                configPlayer(url);
                            }
                            @Override
                            public void onInitFailed(String url, int code, String msg) {
                                FullLog.d("AdsTracking=>>onInitFailed" + code + ':' + url);
                                FullLog.d("AdsTracking=>>onInitFailed_msg:" + msg);
                            }
                        });
            }
        });
    }

    private void configPlayer(String url) {
        exoPlayer = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(this).setLiveTargetOffsetMs(5000))
                .build();
        MediaItem mediaItem =
                new MediaItem.Builder()
                        .setUri(url)
                        .setLiveConfiguration(new MediaItem.LiveConfiguration.Builder().build())
                        .build();
        exoPlayer.setMediaItem(mediaItem);
        exoPlayer.prepare();
        exoPlayer.setPlayWhenReady(false);
        playerView.setPlayer(exoPlayer);

        AdsTracking.getInstance().initPlayerView(exoPlayer, url);
    }
    @SuppressLint("SetTextI18n")


    @Override
    protected void onDestroy() {
        AdsTracking.getInstance().destroy();
        super.onDestroy();
    }



}