package com.tdm.adstracking.core.network;

//import static com.google.common.net.HttpHeaders.USER_AGENT;

import com.tdm.adstracking.FullLog;
import com.tdm.adstracking.core.StringUtil;
import com.tdm.adstracking.core.listener.ResponseInitListener;
import com.tdm.adstracking.core.listener.ResponseRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class RequestUtil {

    public static void requestGetUrl(String url, Map<String, String> mapParams, ResponseRequest responseRequest) {
        String params = "";
        String sessionTmp = "";
        for (Map.Entry m : mapParams.entrySet()) {
            String value = m.getValue().toString();
            params = params + m.getKey() + "=" + StringUtil.encodeURL(value) + "&";
            if (m.getKey() == "session") {
                sessionTmp = m.getValue().toString();
            }
        }
        if (params.length() > 0) {
            params = params.substring(0, params.length() - 1);
        }
        doGetRequest(url, params, responseRequest);
    }

    public static void requestPostUrl(String url, Map<String, String> mapParams, ResponseRequest responseRequest) {
        String params = "";
        String sessionTmp = "";
        for (Map.Entry m : mapParams.entrySet()) {
            String value = m.getValue().toString();
            params = params + m.getKey() + "=" + StringUtil.encodeURL(value) + "&";
            if (m.getKey() == "session") {
                sessionTmp = m.getValue().toString();
            }
        }
        doPostRequest(url, params.substring(0, params.length() - 1), responseRequest);
    }

    private static synchronized void doGet(String url, String params, ResponseRequest responseRequest) {
        URL urlConnect = null;
        int _ec = -1;
        try {
            urlConnect = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) urlConnect.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setAllowUserInteraction(false);
            conn.setInstanceFollowRedirects(true);
            conn.connect();
            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(params);
            writer.flush();
            responseRequest.onErrors(conn.getResponseCode()," 00 ");

            switch (conn.getResponseCode()) {
                case 200:
                    JSONObject myResponse = null;
                    try {
                        myResponse = new JSONObject(getMessageResponse(conn.getInputStream()));
                        try {
                            _ec = myResponse.optInt("ec", -1);
                        } catch (Exception ignored) {
                        }
                        responseRequest.onResponse(_ec, myResponse);
                    } catch (Exception e) {
                        responseRequest.onResponse(conn.getResponseCode(), myResponse);
                    }
                    break;
                default: {
                    responseRequest.onErrors(conn.getResponseCode(),"");
                }
            }
            writer.close();
            os.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    private static synchronized void doGetRequest(String url, String params, ResponseRequest responseRequest) {
        FullLog.d("doGetRequest=>>: " + url);
        URL urlConnect = null;
        int _ec = -1;
        try {
            urlConnect = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) urlConnect.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
//            conn.setRequestProperty("User-Agent", USER_AGENT);

            switch (conn.getResponseCode()) {
                case 200:
                    FullLog.d("doGetRequest=>>getResponseCode: " + conn.getResponseCode());
                    JSONObject myResponse = null;
                    try {
                        myResponse = new JSONObject(getMessageResponse(conn.getInputStream()));
                        try {
                            _ec = myResponse.optInt("ec", -1);
                        } catch (Exception ignored) {
                        }
                        FullLog.d("doGetRequest=>>myResponse: " + myResponse);
                        responseRequest.onResponse(_ec, myResponse);
                    } catch (Exception e) {
                        responseRequest.onResponse(conn.getResponseCode(), myResponse);
                    }
                    break;
                default: {
                    responseRequest.onErrors(conn.getResponseCode(),"");
                }
            }

        } catch (IOException | JSONException e) {
            FullLog.e("IOException "+ e.getMessage());
            e.printStackTrace();
        }
    }

    private static synchronized void doPostRequest(String url, String params, ResponseRequest responseRequest) {
        URL urlConnect = null;
        int _ec = -1;
        try {
            urlConnect = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) urlConnect.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setAllowUserInteraction(false);
            conn.setInstanceFollowRedirects(true);
            conn.connect();
            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(params);
            writer.flush();
            switch (conn.getResponseCode()) {
                case 200:
                    JSONObject myResponse = null;
                    try {
                        myResponse = new JSONObject(getMessageResponse(conn.getInputStream()));
                        try {
                            _ec = myResponse.optInt("ec", -1);
                        } catch (Exception ignored) {
                        }
                        responseRequest.onResponse(_ec, myResponse);
                    } catch (Exception e) {
                        responseRequest.onResponse(conn.getResponseCode(), myResponse);
                    }
                    break;
                default: {
                    responseRequest.onErrors(conn.getResponseCode(),getMessageResponse(conn.getInputStream()));
                }
            }
            writer.close();
            os.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    public static String getMessageResponse(InputStream inputStream) throws IOException {
        BufferedReader in = null;
        String line;
        StringBuffer response = new StringBuffer();
        in = new BufferedReader(
                new InputStreamReader(inputStream));
        while ((line = in.readLine()) != null) {
            response.append(line);
        }
        in.close();
        return response.toString();
    }

}
