package com.eju.router.sdk;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.eju.router.sdk.exception.EjuException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * @author SidneyXu (create)
 * @author tangqianwei (edit)
 */
@SuppressWarnings("deprecation")
public class RouterWebViewClient extends WebViewClient implements HtmlHandler {

    private static final String END_HTML = "</html>";
    private static final String SCRIPT =
            "<script type=\"text/javascript\">" +
                    "var router_params = " +
                    (BuildConfig.DEBUG ? "'" : "") +
                    "{" +
                    "%s" +
                    "}" +
                    (BuildConfig.DEBUG ? "'" : "") +
                    ";" +
                    "</script>\n";

    private final Router router;
    private final HttpClient client;

    public RouterWebViewClient(HttpClient client) {
        this.router = Router.getInstance();
        this.client = client;
    }

    @Override
    public final boolean shouldOverrideUrlLoading(final WebView view, final String url) {
        boolean ret = false;

        if(onBeforeOverrideUrlLoading(view, url)) {
            return true;
        }

        if (router.isNativeRouteSchema(url)) {
            try {
                URI uri = new URI(url);
                router.internalRoute(view.getContext(), uri);
            } catch (URISyntaxException e) {
//                e.printStackTrace();
                router.broadcastException(new EjuException(EjuException.UNKNOWN_ERROR, e.getMessage()));
            }
            ret = true;
        }

        if(!ret) {
            ret = super.shouldOverrideUrlLoading(view, url);
        }

        onAfterOverrideUrlLoading(view, url);
        return ret;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public final WebResourceResponse shouldInterceptRequest(WebView view, final WebResourceRequest request) {
        return intercept(view, new HttpClient.Request() {
            @Override
            public String getUrl() {
                return request.getUrl().toString();
            }

            @Override
            public String getMethod() {
                return request.getMethod();
            }

            @Override
            public Map<String, String> getHeaders() {
                return request.getRequestHeaders();
            }

            @Override
            public OutputStream getBody() {
                return null;
            }

            @Override
            public String getContentType() {
                return null;
            }
        });
    }

    @SuppressWarnings("deprecation")
    @Override
    public final WebResourceResponse shouldInterceptRequest(WebView view, final String url) {
        return intercept(view, new HttpClient.Request() {
            @Override
            public String getUrl() {
                return url;
            }

            @Override
            public String getMethod() {
                return "GET";
            }

            @Override
            public Map<String, String> getHeaders() {
                return null;
            }

            @Override
            public OutputStream getBody() {
                return null;
            }

            @Override
            public String getContentType() {
                return null;
            }
        });
    }

    private WebResourceResponse intercept(WebView view, HttpClient.Request request) {
        EjuLog.d("[ROUTER][LOAD] " + request.getUrl());

        onBeforeInterceptRequest(view, request.getUrl());

        WebResourceResponse wrr;
        try {
            wrr = parseRawResponse(getResponseFromChain(view.getContext(), request));
        } catch (Exception e) {
            wrr = null;
        }

        onAfterInterceptRequest(view, request.getUrl(), wrr);
        return wrr;
    }

    private HttpClient.Response getResponseFromChain(
            Context context, HttpClient.Request originRequest) throws Exception {

        List<RequestInterceptor> interceptors = new ArrayList<>();
        interceptors.add(new InsertInterceptor(context, this));
        interceptors.add(new NativeInterceptor(context));
        interceptors.add(new RemoteInterceptor(client));

        RequestInterceptor[] requestInterceptors = interceptors.toArray(new RequestInterceptor[interceptors.size()]);
        return new RequestChain(0, requestInterceptors, originRequest).proceed(originRequest);
    }

    private WebResourceResponse parseRawResponse(HttpClient.Response raw) {
        if(null == raw) {
            return null;
        }

        String mimeType = raw.getMimeType();
        mimeType = null == mimeType ? "text/plain" : mimeType;

        InputStream is = raw.getBody();
        if(null == is) {
            is = new ByteArrayInputStream("no data".getBytes());
            mimeType = "text/plain";
        }

        String encoding = null == raw.getEncoding() ? "utf-8" : raw.getEncoding();
        WebResourceResponse wrr = new WebResourceResponse(mimeType, encoding, is);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            wrr.setResponseHeaders(raw.getHeaders());
            try {
                wrr.setStatusCodeAndReasonPhrase(
                        raw.getStatusCode(), raw.getReasonPhrase());
            } catch (Exception ignored) {}
        }
        return wrr;
    }

    protected boolean onBeforeOverrideUrlLoading(WebView view, String url) {
        return false;
    }

    protected void onAfterOverrideUrlLoading(WebView view, String url) {

    }

    protected void onBeforeInterceptRequest(WebView view, String url) {

    }

    protected void onAfterInterceptRequest(WebView view, String url, WebResourceResponse wrr) {

    }

    @Override
    public byte[] handle(Context context, String url, byte[] contents) throws EjuException {
        String html = new String(contents);

        int i = html.lastIndexOf(END_HTML);
        if (-1 == i) {
            throw new EjuException(String.format("[%s] has wrong html format !", url));
        }

        int length = html.length();
        if (i + END_HTML.length() > length) {
            html = html.substring(0, i + END_HTML.length());
        }

        Bundle bundle = ((Activity)context).getIntent().getExtras();
        if(null != bundle) {
            StringBuilder builder = new StringBuilder();
            for (String key : bundle.keySet()) {
                // no '_url' parameter more.
                // ignore '_url' parameter
//                if (Router.EXTRA_URL.equalsIgnoreCase(key)) {
//                    continue;
//                }
                builder.append(key).append(':').append(parseObjectOfJS(bundle.get(key)))
                        .append(',');
            }
            String params = String.format(SCRIPT, builder.toString());

            i = html.indexOf("</head>");
            html = html.substring(0, i).concat(params).concat(html.substring(i));
        }

        return html.getBytes();
    }

    @NonNull
    private String parseObjectOfJS(Object object) {
        StringBuilder builder = new StringBuilder();

        if (null == object) {
            builder.append("null");
            return builder.toString();
        }

        Class<?> clazz = object.getClass();
        if (String.class.isAssignableFrom(clazz)) {
            builder.append('"').append(object).append('"');
        } else if (ArrayList.class.isAssignableFrom(clazz)
                || clazz.isArray()) {
            builder.append('[');
            for (Object o : ((ArrayList) object)) {
                builder.append(parseObjectOfJS(o)).append(',');
            }
            builder.append(']');
        } else if (Boolean.class.isAssignableFrom(clazz)
                || Byte.class.isAssignableFrom(clazz)
                || Character.class.isAssignableFrom(clazz)
                || Short.class.isAssignableFrom(clazz)
                || Integer.class.isAssignableFrom(clazz)
                || Float.class.isAssignableFrom(clazz)
                || Double.class.isAssignableFrom(clazz)
                || Long.class.isAssignableFrom(clazz)) {
            builder.append(object);
        } else {
            builder.append('{');

            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                // ignore {@code this} field.
                // Is there better solution to this ?
                if (field.getDeclaringClass() != clazz
                        || field.getName().matches(".*this.*")) {
                    continue;
                }

                try {
                    builder.append(field.getName()).append(':')
                            .append(parseObjectOfJS(field.get(object)));
                    builder.append(',');
                } catch (IllegalAccessException ignored) {
                }
            }
            builder.append('}');
        }

        return builder.toString();
    }
}
