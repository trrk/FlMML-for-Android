package jp.uguisu.aikotoba.mmlt;

import android.text.TextUtils;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.CookieStore;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.zip.GZIPInputStream;

public class HttpGetString implements ResponseHandler<String> {

    private DefaultHttpClient mClient;

    @Override
    public String handleResponse(HttpResponse response) throws IOException {
        switch (response.getStatusLine().getStatusCode()) {
            case HttpStatus.SC_OK:
                InputStream stream = null;
                if (isGZipHttpResponse(response)) {
                    stream = new GZIPInputStream(response.getEntity().getContent());
                } else {
                    stream = response.getEntity().getContent();
                }
                //InputStreamをStringに。
                InputStreamReader reader = new InputStreamReader(stream, "UTF-8");
                StringBuilder builder = new StringBuilder();
                char[] buf = new char[10240];
                int numRead;
                while (0 <= (numRead = reader.read(buf))) {
                    builder.append(buf, 0, numRead);
                }
                return builder.toString();
            default:
                throw new IOException("StatusCode=" + response.getStatusLine().getStatusCode());
        }
    }

    private boolean isGZipHttpResponse(HttpResponse response) {
        Header header = response.getEntity().getContentEncoding();
        if (header == null) return false;

        String value = header.getValue();
        return (!TextUtils.isEmpty(value) && value.contains("gzip"));
    }

    public String get(String url, Cookie[] cookies) throws IOException {
        HttpGet req = new HttpGet();
        URI uri = URI.create(url);
        req.setURI(uri);
        req.setHeader("Accept-Encoding", "gzip, deflate");
        CookieStore cbox = mClient.getCookieStore();
        cbox.clear();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                cbox.addCookie(cookies[i]);
            }
        }
        return mClient.execute(req, this);
    }

    public void open() {
        mClient = new DefaultHttpClient();
    }

    public void close() {
        mClient.getConnectionManager().shutdown();
        mClient = null;
    }

}