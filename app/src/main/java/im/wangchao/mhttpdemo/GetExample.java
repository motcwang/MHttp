package im.wangchao.mhttpdemo;

import android.util.Log;

import im.wangchao.http.annotations.Callback;
import im.wangchao.http.annotations.Get;
import im.wangchao.mhttp.MHttp;
import im.wangchao.mhttp.Request;
import im.wangchao.mhttp.Response;
import im.wangchao.mhttp.callback.TextCallbackHandler;

/**
 * <p>Description  : Get.</p>
 * <p/>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 16/6/6.</p>
 * <p>Time         : 上午8:38.</p>
 */
public class GetExample {

    public static Request doNormalRequest(){
        return Request.builder().url("https://www.baidu.com")
                .callback(new TextCallbackHandler(){
                    @Override public void onSuccess(String data, Response response) {
                        Log.e(MainActivity.TAG, "normal : " + data);
                    }

                    @Override public void onFailure(Response response, Throwable throwable) {
                        Log.e(MainActivity.TAG, "onFailure : " + response.message() + " " + response.code());
                    }

                    @Override public void onCancel() {
                        Log.e(MainActivity.TAG, "onCancel");
                    }
                })
                .build()
                .enqueue();
    }

    public static void doAnnotationRequest(){
        GetBaidu baidu = MHttp.create(GetBaidu.class);
        baidu.baidu(new TextCallbackHandler(){
            @Override public void onSuccess(String data, Response response) {
                Log.e(MainActivity.TAG, data);
            }
        });
    }

    public interface GetBaidu{
        @Get(url = "https://www.baidu.com")
        void baidu(@Callback TextCallbackHandler callback);
    }
}
