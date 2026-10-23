package fi.omatv.player

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class Channel(val name: String, val url: String, val logo: String, val group: String)

data class PlayRequest(val list: List<Channel>, val index: Int)

data class Pick(val channel: Channel, val list: List<Channel>)

data class StreamApp(val label: String, val pkg: String, val icon: ImageBitmap, val intent: Intent)

// Official streaming apps OmaTV can open if they are installed. Keep in sync with <queries> in the manifest.
val STREAMING_PACKAGES = listOf(
    "com.netflix.mediaclient", "com.netflix.ninja",
    "com.disney.disneyplus",
    "com.amazon.avod.thirdpartyclient", "com.amazon.amazonvideo.livingroom",
    "com.google.android.youtube", "com.google.android.youtube.tv",
    "com.wbd.stream", "com.hbo.hbonow",
    "com.hulu.plus", "com.hulu.livingroomplus",
    "com.apple.atve.androidtv.appletv",
    "tv.twitch.android.app",
    "com.plexapp.android",
    "tv.pluto.android",
    "com.viaplay.android",
    "com.crunchyroll.crunchyroid",
)

fun findStreamingApps(ctx: Context): List<StreamApp> {
    val pm = ctx.packageManager
    return STREAMING_PACKAGES.mapNotNull { pkg ->
        try {
            val intent = pm.getLaunchIntentForPackage(pkg)
                ?: pm.getLeanbackLaunchIntentForPackage(pkg)
                ?: return@mapNotNull null
            val info = pm.getApplicationInfo(pkg, 0)
            StreamApp(
                info.loadLabel(pm).toString(),
                pkg,
                info.loadIcon(pm).toBitmap(128, 128).asImageBitmap(),
                intent,
            )
        } catch (e: Exception) {
            null
        }
    }.distinctBy { it.label }
}

object M3u {
    private val attr = Regex("([\\w-]+)=\"([^\"]*)\"")

    fun parse(text: String): List<Channel> {
        val out = ArrayList<Channel>()
        var name = ""
        var logo = ""
        var group = ""
        var pending = false
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            if (line.startsWith("#EXTINF", ignoreCase = true)) {
                val attrs = attr.findAll(line).associate { it.groupValues[1].lowercase() to it.groupValues[2] }
                val lastQuote = line.lastIndexOf('"')
                val comma = line.indexOf(',', if (lastQuote >= 0) lastQuote else 0)
                val title = if (comma >= 0) line.substring(comma + 1).trim() else ""
                name = title.ifBlank { attrs["tvg-name"].orEmpty() }.ifBlank { "Channel" }
                logo = attrs["tvg-logo"].orEmpty()
                group = attrs["group-title"].orEmpty().ifBlank { "Other" }
                pending = true
            } else if (line.startsWith("#")) {
                continue
            } else if (pending) {
                out.add(Channel(name, line, logo, group))
                pending = false
            }
        }
        return out
    }
}

object PlaylistRepo {
    private const val UA = "Mozilla/5.0 (Linux; Android 12) OmaTV/1.0"

    private fun cacheFile(ctx: Context) = File(ctx.filesDir, "playlist.m3u")

    fun cacheAgeMs(ctx: Context): Long {
        val f = cacheFile(ctx)
        return if (f.exists()) System.currentTimeMillis() - f.lastModified() else Long.MAX_VALUE
    }

    suspend fun loadCached(ctx: Context): List<Channel> = withContext(Dispatchers.IO) {
        val f = cacheFile(ctx)
        if (f.exists()) M3u.parse(f.readText()) else emptyList()
    }

    suspend fun download(ctx: Context, url: String): List<Channel> = withContext(Dispatchers.IO) {
        downloadBlocking(ctx, url)
    }

    private fun downloadBlocking(ctx: Context, url: String): List<Channel> {
        var current = url.trim()
        for (attempt in 0..6) {
            val conn = URL(current).openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.instanceFollowRedirects = false
            conn.setRequestProperty("User-Agent", UA)
            try {
                val code = conn.responseCode
                if (code in 300..399) {
                    val loc = conn.getHeaderField("Location") ?: error("Bad redirect")
                    current = URL(URL(current), loc).toString()
                    continue
                }
                if (code != 200) error("Server returned $code")
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val list = M3u.parse(text)
                if (list.isEmpty()) error("No channels found in this playlist")
                cacheFile(ctx).writeText(text)
                return list
            } finally {
                conn.disconnect()
            }
        }
        throw IllegalStateException("Too many redirects")
    }
}

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("omatv", Context.MODE_PRIVATE)

    private fun getList(key: String): List<String> =
        sp.getString(key, "")!!.split("\n").filter { it.isNotBlank() }

    private fun putList(key: String, v: List<String>) =
        sp.edit().putString(key, v.joinToString("\n")).apply()

    var playlistUrl: String
        get() = sp.getString("url", "")!!
        set(v) = sp.edit().putString("url", v).apply()

    var favorites: List<String>
        get() = getList("favs")
        set(v) = putList("favs", v)

    var recents: List<String>
        get() = getList("recents")
        set(v) = putList("recents", v)

    var tileSize: Int
        get() = sp.getInt("tileSize", 1)
        set(v) = sp.edit().putInt("tileSize", v).apply()
}
