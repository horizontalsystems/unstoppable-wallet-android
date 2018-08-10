package bitcoin.walllet.kit.common.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class HttpUtils {

    static int TIMEOUT = 10_000;

    public static String getJson(String url) {
        return getJson(url, null);
    }

    public static String getJson(String url, Map<String, String> params) {
        if (params != null) {
            List<String> list = new ArrayList<>(params.size());
            try {
                for (Entry<String, String> kv : params.entrySet()) {
                    String key = kv.getKey();
                    String value = kv.getValue();
                    list.add(key + "=" + URLEncoder.encode(value, "UTF-8"));
                }
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            url = url + (url.indexOf('?') == (-1) ? "?" : "&") + String.join("&", list);
        }
        try {
            URL theUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) theUrl.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setAllowUserInteraction(false);
            conn.setUseCaches(false);
            conn.setReadTimeout(TIMEOUT);
            conn.setConnectTimeout(TIMEOUT);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
            }
            StringBuilder sb = new StringBuilder(1024);
            try (InputStream input = conn.getInputStream()) {
                BufferedReader br = new BufferedReader(new InputStreamReader(input, "UTF-8"));
                char[] buffer = new char[1024];
                for (; ; ) {
                    int n = br.read(buffer);
                    if (n == (-1)) {
                        break;
                    }
                    sb.append(buffer, 0, n);
                }
            }
            conn.disconnect();
            return sb.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String postForm(String url, Object data) {
        return post(url, "application/x-www-form-urlencoded", data);
    }

    public static String postJson(String url, Object data) {
        return post(url, "application/json", data);
    }

    static String post(String url, String contentType, Object data) {
        try {
            URL theUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) theUrl.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setAllowUserInteraction(false);
            conn.setUseCaches(false);
            conn.setReadTimeout(TIMEOUT);
            conn.setConnectTimeout(TIMEOUT);
            conn.setRequestMethod("POST");
            conn.setDoOutput(data != null);
            conn.setRequestProperty("Accept", "*/*");
            if (data != null) {
                byte[] postData = serialize(contentType, data);
                conn.setRequestProperty("Content-Type", contentType);
                conn.setRequestProperty("Content-Length", String.valueOf(postData.length));
                try (OutputStream output = conn.getOutputStream()) {
                    output.write(postData);
                }
            }
            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
            }
            StringBuilder sb = new StringBuilder(1024);
            try (InputStream input = conn.getInputStream()) {
                BufferedReader br = new BufferedReader(new InputStreamReader(input, "UTF-8"));
                char[] buffer = new char[1024];
                for (; ; ) {
                    int n = br.read(buffer);
                    if (n == (-1)) {
                        break;
                    }
                    sb.append(buffer, 0, n);
                }
            }
            conn.disconnect();
            return sb.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static byte[] serialize(String contentType, Object data) {
        if ("application/json".equals(contentType)) {
            String json = JsonUtils.toJson(data);
            return json.getBytes(StandardCharsets.UTF_8);
        }
        if ("application/x-www-form-urlencoded".equals(contentType)) {
            String formData = null;
            if (data instanceof String) {
                formData = (String) data;
            } else if (data instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, String> map = (Map<String, String>) data;
                List<String> list = new ArrayList<>(map.size());
                try {
                    for (Entry<String, String> kv : map.entrySet()) {
                        list.add(kv.getKey() + "=" + URLEncoder.encode(kv.getValue(), "UTF-8"));
                    }
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
                formData = String.join("&", list);
            }
            if (formData == null) {
                throw new IllegalArgumentException("Invalid type of data: " + data);
            }
            return formData.getBytes(StandardCharsets.UTF_8);
        }
        throw new IllegalArgumentException("Invalid content type: " + contentType);
    }
}
