package com.eju.router.sdk;

import android.content.Context;
import com.eju.router.sdk.exception.EjuException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Class description.
 *
 * @author tangqianwei.
 * @date 6/2/17.
 */
class InsertInterceptor implements RequestInterceptor {

    private Context context;
    private HtmlHandler htmlHandler;

    InsertInterceptor(Context context, HtmlHandler htmlHandler) {
        this.context = context;
        this.htmlHandler = htmlHandler;
    }

    @Override
    public HttpClient.Response intercept(Chain chain) throws Exception {
        RequestChain requestChain = (RequestChain) chain;
        HttpClient.Request request = chain.request();
        final HttpClient.Response rawResponse = requestChain.proceed(request);

        final InputStream is;
        switch (rawResponse.getMimeType()) {
            case "text/html": {
                byte[] data = readStream(rawResponse.getBody());
                try {
                    // parameter
                    data = this.htmlHandler.handle(this.context, request.getUrl(), data);
                } catch (EjuException ignored) {}
                is = new ByteArrayInputStream(data);
                break;
            }
            default: {
                is = rawResponse.getBody();
                break;
            }
        }

        return new HttpClient.Response() {
            @Override
            public InputStream getBody() {
                return is;
            }

            @Override
            public String getMimeType() {
                return rawResponse.getMimeType();
            }

            @Override
            public String getEncoding() {
                return rawResponse.getEncoding();
            }

            @Override
            public Map<String, String> getHeaders() {
                return rawResponse.getHeaders();
            }

            @Override
            public int getStatusCode() {
                return rawResponse.getStatusCode();
            }

            @Override
            public String getReasonPhrase() {
                return rawResponse.getReasonPhrase();
            }
        };
    }

    private byte[] readStream(InputStream is) {
        byte[] buffer = new byte[0xff];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            int count;
            while(-1 != (count = is.read(buffer))) {
                baos.write(buffer, 0, count);
                baos.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
                baos.close();
            } catch (IOException ignored) {}
        }
        return baos.toByteArray();
    }

}
