package com.eju.router.sdk;

import android.content.Context;
import android.content.res.Resources;
import android.webkit.MimeTypeMap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Class description.
 *
 * @author tangqianwei.
 * @date 6/2/17.
 */
class NativeInterceptor implements RequestInterceptor {

    private final String ASSETS_BASE = "file:///android_asset/";

    private Context context;

    NativeInterceptor(Context context) {
        this.context = context;
    }

    @Override
    public HttpClient.Response intercept(Chain chain) throws Exception {
        String requestUrl = chain.request().getUrl();
        if(!Router.getInstance().isNativeRouteSchema(requestUrl)) {
            return chain.proceed(chain.request());
        }

        final String url = "file".concat(requestUrl.substring(requestUrl.indexOf(':')));
        if(!url.startsWith(ASSETS_BASE)) {
            return chain.proceed(chain.request());
        }

        return new HttpClient.Response() {
            @Override
            public InputStream getBody() {
                Resources resources = context.getResources();
                try {
                    return resources.getAssets().open(url.substring(ASSETS_BASE.length()));
                } catch (IOException e) {
                    return new ByteArrayInputStream(new byte[0]);
                }
            }

            @Override
            public String getMimeType() {
                String extension = MimeTypeMap.getFileExtensionFromUrl(url);
                switch (extension) {
                    case "js":
                        return "text/javascript";
                    default:
                        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                }
            }

            @Override
            public String getEncoding() {
                return "utf-8";
            }

            @Override
            public Map<String, String> getHeaders() {
                return null;
            }

            @Override
            public int getStatusCode() {
                return 200;
            }

            @Override
            public String getReasonPhrase() {
                return "OK";
            }
        };
    }
}
