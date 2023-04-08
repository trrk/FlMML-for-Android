package jp.uguisu.aikotoba.mmlt;

import android.os.Build;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;

public class HttpGetString {

    private static final int CONNECT_TIME_OUT = 5000;
    private static final int READ_TIME_OUT = 15000;

    static {
        if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.FROYO) {
            System.setProperty("http.keepAlive", "false");
        }
    }

    public String get(String urlstr) throws IOException {
        HttpURLConnection con = null;
        InputStreamReader reader = null;
        try {
            URL url = new URL(urlstr);
            URLConnection urlCon = url.openConnection();
            if (!(urlCon instanceof HttpURLConnection))
                throw new IOException("Not Http");
            con = (HttpURLConnection) urlCon;
            con.setConnectTimeout(CONNECT_TIME_OUT);
            con.setReadTimeout(READ_TIME_OUT);
            int code = con.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK)
                throw new IOException("Not 200 OK : " + code);
            InputStream stream = con.getInputStream();
            if (stream == null) return "";
            //InputStreamをStringに。
            reader = new InputStreamReader(stream, Charset.forName("UTF-8"));
            StringBuilder builder = new StringBuilder();
            char[] buf = new char[10240];
            int numRead;
            while (0 <= (numRead = reader.read(buf))) {
                builder.append(buf, 0, numRead);
            }
            return builder.toString();
        } finally {
            if (reader != null) reader.close();
            if (con != null) con.disconnect();
        }
    }

}