package im.wangchao.mhttp;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>Description  : RequestParams.</p>
 * <p/>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 15/8/17.</p>
 * <p>Time         : 下午2:16.</p>
 */
public class RequestParams {
    final public static Charset UTF_8       = Charset.forName("UTF-8");
    final public static String  UTF_8_STR   = "utf-8";

    /**
     * 字节流
     */
    final public static String APPLICATION_OCTET_STREAM     = "application/octet-stream";
    /**
     * JSON
     */
    final public static String APPLICATION_JSON             = "application/json";
    /**
     * Form 表单
     */
    final public static String APPLICATION_FORM             = "application/x-www-form-urlencoded";

    //params
    final private ConcurrentHashMap<String, String>         urlParams       = new ConcurrentHashMap<>();
    final private ConcurrentHashMap<String, StreamWrapper>  streamParams    = new ConcurrentHashMap<>();
    final private ConcurrentHashMap<String, FileWrapper>    fileParams      = new ConcurrentHashMap<>();

    //default
    private String contentEncoding  = UTF_8_STR;
    private String contentType      = APPLICATION_JSON;

    public RequestParams(){
        this((Map<String, String>)null);
    }

    public RequestParams(Map<String, String> params){
        if (params != null){
            for(Map.Entry<String, String> entry : params.entrySet()){
                put(entry.getKey(), entry.getValue());
            }
        }
    }

    public RequestParams(Object...keysAndValues){
        int len = keysAndValues.length;
        if (len % 2 != 0){
            throw new IllegalArgumentException("Supplied arguments must be even.");
        }
        for (int i = 0; i < len; i += 2){
            String key = String.valueOf(keysAndValues[i]);
            String val = String.valueOf(keysAndValues[i + 1]);
            put(key, val);
        }
    }

    public RequestParams(RequestParams params){
        if (params == null){
            return;
        }
        this.urlParams.putAll(params.getUrlParams());
        this.streamParams.putAll(params.getStreamParams());
        this.fileParams.putAll(params.getFileParams());
        this.contentEncoding = params.getContentEncoding();
        this.contentType = params.getContentType();
    }

    /**
     * 判断请求 Content-Type 是否为 JSON
     */
    public boolean isJSON(){
        return contentType.equals(APPLICATION_JSON) && streamParams.size() == 0
                && fileParams.size() == 0;
    }

    /**
     * 判断请求 Content-Type 是否为 Form
     */
    public boolean isForm(){
        return contentType.equals(APPLICATION_FORM) && streamParams.size() == 0
                && fileParams.size() == 0;
    }

    /**
     * 判断请求 Content-Type 是否为混合类型
     */
    public boolean isMultipart(){
        return !isJSON() && !isForm();
    }

    /**
     * 设置请求参数编码
     */
    public RequestParams setContentEncoding(@NonNull String encoding) {
        this.contentEncoding = encoding;
        return this;
    }

    public String getContentEncoding(){
        return contentEncoding;
    }

    public RequestParams setContentType(@Nullable String contentType){
        this.contentType = contentType == null || contentType.isEmpty() ? APPLICATION_JSON : contentType;
        return this;
    }

    public String getContentType(){
        return contentType;
    }

    public void put(Map<String, String> params){
        if (params != null){
            for(Map.Entry<String, String> entry : params.entrySet()){
                put(entry.getKey(), entry.getValue());
            }
        }
    }

    public void put(String key, JSONArray value){
        put(key, value.toString());
    }

    public void put(String key, JSONObject value){
        put(key, value.toString());
    }

    public void put(String key, String value) {
        if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value)) {
            urlParams.put(key, value);
        }
    }

    public void put(String key, Object value) {
        if (!TextUtils.isEmpty(key) && (value != null)) {
            urlParams.put(key, String.valueOf(value));
        }
    }



    public void put(String key, InputStream stream){
        put(key, stream, null);
    }

    public void put(String key, InputStream stream, String name){
        put(key, stream, name, null);
    }

    public void put(String key, InputStream stream, String name, String contentType){
        if (!TextUtils.isEmpty(key) && stream != null){
            streamParams.put(key, StreamWrapper.newInstance(stream, name, contentType));
        }
    }

    public void put(String key, File file) throws FileNotFoundException {
        put(key, file, null);
    }

    public void put(String key, File file, String contentType) throws FileNotFoundException {
        if (file == null || !file.exists()){
            throw new FileNotFoundException();
        }
        if (!TextUtils.isEmpty(key)){
            fileParams.put(key, new FileWrapper(file, contentType));
        }
    }

    public void remove(String key){
        urlParams.remove(key);
        streamParams.remove(key);
        fileParams.remove(key);
    }

    public boolean has(String key){
        return (urlParams.containsKey(key) || streamParams.containsKey(key) || fileParams.containsKey(key));
    }

    public String parseJSON(){
        JSONObject json = new JSONObject();

        String key, value;
        JSONObject tempObj;
        JSONArray tempArray;
        for (Map.Entry<String, String> entry: urlParams.entrySet()){
            key = entry.getKey();
            value = entry.getValue();

            if (key.isEmpty() || value.isEmpty()){
                continue;
            }

            try {
                tempObj = new JSONObject(value);
                json.put(key, tempObj);
            } catch (JSONException e) {
                try {
                    tempArray = new JSONArray(value);
                    json.put(key, tempArray);
                } catch (JSONException e1) {
                    try {
                        json.put(key, value);
                    } catch (JSONException e2) {
                       //Silent
                    }
                }
            }
        }

        return json.toString();
    }

    public ConcurrentHashMap<String, StreamWrapper> getStreamParams() {
        return streamParams;
    }

    public ConcurrentHashMap<String, FileWrapper> getFileParams() {
        return fileParams;
    }

    public ConcurrentHashMap<String, String> getUrlParams() {
        return urlParams;
    }

    /**
     * format get params
     * A=a&B=b..
     */
    public String formatURLParams() throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        if (urlParams.size() != 0) {
            for (Map.Entry<String, String> entry : urlParams.entrySet()) {
                sb.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), contentEncoding));
                sb.append("&");
            }
            sb.deleteCharAt(sb.length() - 1);
        }

        return sb.toString().replace(" ", "%20");
    }

    /**
     * Url params convert to List<Pair<String, String>>
     */
    public List<Pair<String, String>> getParamsList(){
        List<Pair<String, String>> params = new LinkedList<>();

        for (ConcurrentHashMap.Entry<String, String> entry : urlParams.entrySet()) {
            params.add(new Pair<>(entry.getKey(), entry.getValue()));
        }

        return params;
    }

    public static class FileWrapper {
        public final File file;
        public final String contentType;

        public FileWrapper(File file, String contentType) {
            this.file = file;
            this.contentType = contentType;
        }
    }

    public static class StreamWrapper {
        public final InputStream inputStream;
        public final String name;
        public final String contentType;

        public StreamWrapper(InputStream inputStream, String name, String contentType) {
            this.inputStream = inputStream;
            this.name = name;
            this.contentType = contentType;
        }

        static StreamWrapper newInstance(InputStream inputStream, String name, String contentType) {
            return new StreamWrapper(
                    inputStream,
                    name,
                    TextUtils.isEmpty(contentType) ? APPLICATION_OCTET_STREAM : contentType);
        }
    }
}