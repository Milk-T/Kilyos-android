package com.eju.router.sdk;

/**
 * Class description.
 *
 * @author tangqianwei.
 * @date 6/2/17.
 */
class RequestChain implements RequestInterceptor.Chain {

    private int index;
    private RequestInterceptor[] interceptors;
    private HttpClient.Request request;

    RequestChain(int index, RequestInterceptor[] interceptors, HttpClient.Request request) {
        this.index = index;
        this.interceptors = interceptors;
        this.request = request;
    }

    @Override
    public HttpClient.Request request() {
        return request;
    }

    @Override
    public HttpClient.Response proceed(HttpClient.Request request) throws Exception {
        if(index >= this.interceptors.length) {
            throw new IllegalStateException("wrong size in chain.");
        }

        RequestInterceptor interceptor = interceptors[index];
        RequestChain next = new RequestChain(index + 1, interceptors, request);
        return interceptor.intercept(next);
    }
}
