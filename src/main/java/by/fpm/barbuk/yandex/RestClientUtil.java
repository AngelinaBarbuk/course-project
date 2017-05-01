package by.fpm.barbuk.yandex;

import com.squareup.okhttp.OkHttpClient;
import com.yandex.disk.rest.Credentials;
import com.yandex.disk.rest.OkHttpClientFactory;
import com.yandex.disk.rest.RestClient;

public class RestClientUtil {

    public static RestClient getInstance(final Credentials credentials) {
        OkHttpClient client = OkHttpClientFactory.makeClient();

        return new RestClient(credentials, client);
    }
}
