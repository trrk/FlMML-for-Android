package jp.uguisu.aikotoba.mmlt

import android.os.Build
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset

class HttpGetString {
    @Throws(IOException::class)
    operator fun get(urlstr: String?): String {
        var con: HttpURLConnection? = null
        var reader: InputStreamReader? = null
        return try {
            val url = URL(urlstr)
            val urlCon = url.openConnection() as? HttpURLConnection ?: throw IOException("Not Http")
            con = urlCon
            con.connectTimeout = CONNECT_TIME_OUT
            con.readTimeout = READ_TIME_OUT
            val code = con.responseCode
            if (code != HttpURLConnection.HTTP_OK) throw IOException("Not 200 OK : $code")
            val stream = con.inputStream ?: return ""
            //InputStreamをStringに。
            reader = InputStreamReader(stream, Charset.forName("UTF-8"))
            val builder = StringBuilder()
            val buf = CharArray(10240)
            var numRead: Int
            while (0 <= reader.read(buf).also { numRead = it }) {
                builder.append(buf, 0, numRead)
            }
            builder.toString()
        } finally {
            reader?.close()
            con?.disconnect()
        }
    }

    companion object {
        private const val CONNECT_TIME_OUT = 5000
        private const val READ_TIME_OUT = 15000

        init {
            if (Build.VERSION.SDK.toInt() < Build.VERSION_CODES.FROYO) {
                System.setProperty("http.keepAlive", "false")
            }
        }
    }
}