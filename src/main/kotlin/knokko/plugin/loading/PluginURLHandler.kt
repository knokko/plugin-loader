package knokko.plugin.loading

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler

private class CustomURLConnection(url: URL, private val content: ByteArray): URLConnection(url) {
    override fun connect() {}

    override fun getInputStream(): InputStream {
        return ByteArrayInputStream(content)
    }
}

internal class PluginURLHandler(private val resourceMap: Map<String, ByteArray>): URLStreamHandler() {
    override fun openConnection(url: URL): URLConnection {
        return CustomURLConnection(url, resourceMap[url.file]!!)
    }
}
