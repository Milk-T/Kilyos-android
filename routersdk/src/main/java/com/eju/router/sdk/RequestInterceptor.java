package com.eju.router.sdk;

/**
 * Class description.
 *
 * @author tangqianwei.
 * @date 6/2/17.
 */
interface RequestInterceptor {

    HttpClient.Response intercept(Chain chain) throws Exception;

    interface Chain {

        HttpClient.Request request();

        HttpClient.Response proceed(HttpClient.Request request) throws Exception;
    }
}
