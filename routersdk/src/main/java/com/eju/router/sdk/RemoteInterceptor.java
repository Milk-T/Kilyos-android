package com.eju.router.sdk;

import java.io.IOException;

/**
 * Class description.
 *
 * @author tangqianwei.
 * @date 6/2/17.
 */
class RemoteInterceptor implements RequestInterceptor {

    private HttpClient client;

    RemoteInterceptor(HttpClient client) {
        this.client = client;
    }

    @Override
    public HttpClient.Response intercept(Chain chain) throws IOException {
        return this.client.execute(chain.request());
    }
}
