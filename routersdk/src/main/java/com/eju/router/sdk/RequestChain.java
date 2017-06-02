package com.eju.router.sdk;

import java.util.List;

/**
 * Class description.
 *
 * @author tangqianwei.
 * @date 6/2/17.
 */
class RequestChain implements RequestInterceptor.Chain {

    private RequestInterceptor[] interceptors;
    private HttpClient.Request request;

    RequestChain(RequestInterceptor[] interceptors, HttpClient.Request request) {
        this.interceptors = interceptors;
        this.request = request;
    }

    @Override
    public HttpClient.Request request() {
        return request;
    }

    @Override
    public HttpClient.Response proceed(HttpClient.Request request) throws Exception {
        return null;
    }
}
