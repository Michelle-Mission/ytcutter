# YT Cutter 0.2.0 — Info via NewPipeExtractor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Info video YouTube tampil hampir instan lewat NewPipeExtractor, unduhan tetap yt-dlp secepat 0.1.11, dengan fallback otomatis ke yt-dlp.

**Architecture:** NewPipeExtractor (JVM) dipakai hanya untuk membaca metadata + daftar resolusi YouTube. yt-dlp tetap satu-satunya pengunduh; JSON `-J` diprefetch di latar setelah info tampil untuk `--load-info-json`. poToken WebView yang sudah ada dipasang sebagai `PoTokenProvider` NewPipe.

**Tech Stack:** Kotlin, Jetpack Compose, NewPipeExtractor v0.27.1 (JitPack), okhttp3, youtubedl-android 0.18.1, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-11-23-ytdlnis-architecture-design.md`

## Global Constraints

- minSdk 29, compileSdk/targetSdk 36, JDK 21 build, Kotlin 2.4.21, AGP 8.13.2.
- R8 (`isMinifyEnabled = true`, `isShrinkResources = true`) tetap aktif di release.
- Satu APK `arm64-v8a`; `packaging.jniLibs.useLegacyPackaging = true` tidak boleh berubah.
- App menjadi GPL-3.0: `LICENSE` wajib ada, repo GitHub wajib publik.
- Tidak ada blokir UI baru: info NewPipe tampil dulu, prefetch yt-dlp berjalan di latar.
- Semua jalur gagal NewPipe harus jatuh ke `YtDlpRepo.json` yang sudah ada.
- Versi rilis 0.2.0 / versionCode 12.
- Perintah verifikasi: `./gradlew --no-daemon testDebugUnitTest assembleRelease` (dengan `ANDROID_HOME=/opt/android-sdk`, `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`).

## Review Focus

- Video YouTube dengan cookies aktif: tidak boleh muncul "The page needs to be reloaded"; fallback ke yt-dlp harus jalan.
- Video non-YouTube (contoh Vimeo): tetap lewat yt-dlp, tidak menyentuh NewPipe.
- URL YouTube umur-restricted/poToken gagal: UI menampilkan pesan informatif, bukan crash.
- JSON prefetch milik URL lain (user cepat ganti URL): unduhan berikutnya tidak boleh memakai file JSON yang salah.
- Shorts/live URL: id video harus terpetakan benar (11 karakter), judul dan durasi terisi.

---

### Task 1: Dependensi NewPipeExtractor, lisensi GPL, repo publik

**Files:**
- Modify: `settings.gradle.kts`
- Modify: `app/build.gradle.kts`
- Modify: `app/proguard-rules.pro`
- Create: `LICENSE`
- Modify: `README.md`

**Interfaces:**
- Consumes: none
- Produces: dependency `libs.newpipeextractor`, classpath JitPack; app resolvable for later tasks.

- [ ] **Step 1: Tambah repositori JitPack**

In `settings.gradle.kts`, add `maven("https://jitpack.io")` inside `dependencyResolutionManagement.repositories` after `mavenCentral()`.

- [ ] **Step 2: Tambah versi + library**

In `gradle/libs.versions.toml`:

```toml
[versions]
newpipeextractor = "v0.27.1"
okhttp = "4.12.0"
```

```toml
[libraries]
newpipeextractor = { group = "com.github.TeamNewPipe", name = "NewPipeExtractor", version.ref = "newpipeextractor" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
```

In `app/build.gradle.kts` dependencies, add:

```kotlin
implementation(libs.newpipeextractor)
implementation(libs.okhttp)
```

- [ ] **Step 3: Verifikasi dependency resolve**

Run: `ANDROID_HOME=/opt/android-sdk JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew --no-daemon :app:dependencies --configuration releaseRuntimeClasspath | grep -i newpipe`
Expected: satu baris `com.github.TeamNewPipe:NewPipeExtractor:v0.27.1` (tanpa `FAILED`).

- [ ] **Step 4: R8 keep untuk NewPipeExtractor**

Append to `app/proguard-rules.pro`:

```
-keep class org.schabi.newpipe.extractor.** { *; }
-dontwarn org.schabi.newpipe.extractor.**
-dontwarn javax.annotation.**
-dontwarn org.jsoup.**
```

- [ ] **Step 5: Tambah LICENSE GPL-3.0**

Run: `curl -sL https://www.gnu.org/licenses/gpl-3.0.txt -o LICENSE`
Expected: file > 30000 byte, diawali `GNU GENERAL PUBLIC LICENSE`.

- [ ] **Step 6: README notice**

Tambahkan di akhir `README.md`:

```markdown
## Lisensi

YT Cutter adalah perangkat lunak bebas di bawah GNU General Public License v3.0.
Ini karena proyek memakai NewPipeExtractor (GPL-3.0). Source: https://github.com/Michelle-Mission/ytcutter
```

- [ ] **Step 7: Commit**

```bash
git add settings.gradle.kts gradle/libs.versions.toml app/build.gradle.kts app/proguard-rules.pro LICENSE README.md
git commit -m "build: add NewPipeExtractor dependency, GPL-3.0 license"
```

- [ ] **Step 8: Jadikan repo publik**

Run:
```bash
TOKEN=$(python3 -c "import json;print((json.load(open('/root/.config/opencode/opencode.json'))['mcp']['github']['headers']['Authorization']).split(' ',1)[1])")
curl -s -X PATCH -H "Authorization: Bearer $TOKEN" -H "Accept: application/vnd.github+json" \
  https://api.github.com/repos/Michelle-Mission/ytcutter -d '{"private":false}'
```
Expected: JSON body berisi `"private": false`.

---

### Task 2: PoToken — buka token mentah untuk NewPipe

**Files:**
- Modify: `app/src/main/java/app/ytcutter/data/PoToken.kt`
- Test: `app/src/test/java/app/ytcutter/PoTokenArgsTest.kt`

**Interfaces:**
- Consumes: internal `PoToken.extractorArgs(context: Context, url: String): String?` (ada).
- Produces:
  - `data class PoToken.Tokens(val visitorData: String, val streaming: String, val player: String)`
  - `suspend fun PoToken.tokens(context: Context, url: String): Tokens?`
  - `fun PoToken.buildExtractorArgs(tokens: Tokens): String` — pure, returns `"youtube:player_client=default,-tv_downgraded;po_token=web.gvs+<s>,web.player+<p>;visitor_data=<v>"`.

- [ ] **Step 1: Tulis test gagal untuk format args**

Create `app/src/test/java/app/ytcutter/PoTokenArgsTest.kt`:

```kotlin
package app.ytcutter

import app.ytcutter.data.PoToken
import org.junit.Assert.assertEquals
import org.junit.Test

class PoTokenArgsTest {

    @Test
    fun buildsQualifiedExtractorArgs() {
        val tokens = PoToken.Tokens(visitorData = "VD", streaming = "STREAM", player = "PLAY")
        assertEquals(
            "youtube:player_client=default,-tv_downgraded;po_token=web.gvs+STREAM,web.player+PLAY;visitor_data=VD",
            PoToken.buildExtractorArgs(tokens)
        )
    }
}
```

- [ ] **Step 2: Jalankan, pastikan gagal**

Run: `./gradlew --no-daemon testDebugUnitTest --tests app.ytcutter.PoTokenArgsTest`
Expected: FAIL — `Unresolved reference: Tokens` / `buildExtractorArgs`.

- [ ] **Step 3: Implementasi di PoToken.kt**

Add inside `object PoToken`:

```kotlin
data class Tokens(val visitorData: String, val streaming: String, val player: String)

fun buildExtractorArgs(tokens: Tokens): String =
    "youtube:player_client=default,-tv_downgraded;" +
        "po_token=web.gvs+${tokens.streaming},web.player+${tokens.player};" +
        "visitor_data=${tokens.visitorData}"

suspend fun tokens(context: Context, url: String): Tokens? {
    val videoId = UrlValidator.videoId(url) ?: return null
    return runCatching {
        withTimeout(25_000) { withContext(Dispatchers.IO) { sessionTokens(context, videoId) } }
    }.getOrNull()
}
```

Refactor existing private `args(context, videoId)` into `sessionTokens(context, videoId): Tokens` returning the three values, and make `extractorArgs` return `tokens(...)?.let(::buildExtractorArgs)`.

- [ ] **Step 4: Jalankan test, pastikan lulus**

Run: `./gradlew --no-daemon testDebugUnitTest --tests app.ytcutter.PoTokenArgsTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/app/ytcutter/data/PoToken.kt app/src/test/java/app/ytcutter/PoTokenArgsTest.kt
git commit -m "refactor: expose poToken values for NewPipe provider"
```

---

### Task 3: Mapper murni NewPipe → VideoInfo

**Files:**
- Create: `app/src/main/java/app/ytcutter/data/NewPipeMapper.kt`
- Test: `app/src/test/java/app/ytcutter/NewPipeMapperTest.kt`

**Interfaces:**
- Consumes: `VideoInfo(id, title, durationSec, thumbnailUrl, heights, hasAudio)`.
- Produces: `object NewPipeMapper { fun toVideoInfo(videoId: String, title: String, durationSec: Int, thumbnail: String?, heights: Collection<Int>, hasAudio: Boolean): VideoInfo }` — filters heights `< 144` seperti `FormatMapper.MIN_VIDEO_HEIGHT`, dedup, urut menurun.

- [ ] **Step 1: Tulis test gagal**

Create `app/src/test/java/app/ytcutter/NewPipeMapperTest.kt`:

```kotlin
package app.ytcutter

import app.ytcutter.data.NewPipeMapper
import org.junit.Assert.assertEquals
import org.junit.Test

class NewPipeMapperTest {

    @Test
    fun mapsAndCleansHeights() {
        val info = NewPipeMapper.toVideoInfo(
            videoId = "dQw4w9WgXcQ",
            title = "Judul",
            durationSec = 212,
            thumbnail = "https://i.ytimg.com/vi/dQw4w9WgXcQ/hq.jpg",
            heights = listOf(360, 1080, 144, 720, 1080, 90),
            hasAudio = true
        )
        assertEquals("dQw4w9WgXcQ", info.id)
        assertEquals("Judul", info.title)
        assertEquals(212, info.durationSec)
        assertEquals(listOf(1080, 720, 360, 144), info.heights.toList())
        assertEquals(true, info.hasAudio)
    }

    @Test
    fun keepsAudioOnlyVideo() {
        val info = NewPipeMapper.toVideoInfo("abc12345678", "Podcast", 60, null, emptyList(), true)
        assertEquals(emptySet<Int>(), info.heights)
        assertEquals(true, info.hasAudio)
        assertEquals(null, info.thumbnailUrl)
    }
}
```

- [ ] **Step 2: Jalankan, pastikan gagal**

Run: `./gradlew --no-daemon testDebugUnitTest --tests app.ytcutter.NewPipeMapperTest`
Expected: FAIL — unresolved `NewPipeMapper`.

- [ ] **Step 3: Implementasi NewPipeMapper.kt**

```kotlin
package app.ytcutter.data

object NewPipeMapper {

    private const val MIN_VIDEO_HEIGHT = 144

    fun toVideoInfo(
        videoId: String,
        title: String,
        durationSec: Int,
        thumbnail: String?,
        heights: Collection<Int>,
        hasAudio: Boolean
    ): VideoInfo = VideoInfo(
        id = videoId,
        title = title,
        durationSec = durationSec,
        thumbnailUrl = thumbnail?.takeIf { it.isNotBlank() },
        heights = heights.filter { it >= MIN_VIDEO_HEIGHT }.toSortedSet(compareByDescending { it }),
        hasAudio = hasAudio
    )
}
```

- [ ] **Step 4: Jalankan test, pastikan lulus**

Run: `./gradlew --no-daemon testDebugUnitTest --tests app.ytcutter.NewPipeMapperTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/app/ytcutter/data/NewPipeMapper.kt app/src/test/java/app/ytcutter/NewPipeMapperTest.kt
git commit -m "feat: map NewPipe metadata to VideoInfo"
```

---

### Task 4: Downloader NewPipe + YouTubeInfo

**Files:**
- Create: `app/src/main/java/app/ytcutter/data/NewPipeDownloader.kt`
- Create: `app/src/main/java/app/ytcutter/data/YouTubeInfo.kt`

**Interfaces:**
- Consumes: `PoToken.tokens(context, url)`, `PoToken.Tokens`, `CookieStore.file(context)`, `NewPipeMapper.toVideoInfo(...)`.
- Produces: `suspend fun YouTubeInfo.fetch(context: Context, url: String): Result<VideoInfo>` (blocking work on `Dispatchers.IO`, inisialisasi NewPipe lazy + idempoten).

- [ ] **Step 1: Implementasi NewPipeDownloader**

`NewPipeDownloader.kt` — subclass `org.schabi.newpipe.extractor.downloader.Downloader`, memakai `okhttp3.OkHttpClient` (timeout 15 s), menambahkan header `User-Agent` Chrome-Android, `Accept-Language: en-US,en;q=0.9`, dan `Cookie` dari baris Netscape di `CookieStore.file(context)` (format `name=value; ...`, lewati baris `#`). Implementasikan `execute(Request): Response` yang mengubah `Request.httpMethod()`, `Request.url()`, `Request.headers()`, body `Request.dataToSend()` menjadi okhttp dan mengembalikan `Response(response.code, response.message, response.headers.toMultimap(), response.body?.bytes())`.

- [ ] **Step 2: Implementasi YouTubeInfo**

`YouTubeInfo.kt`:

```kotlin
object YouTubeInfo {
    @Volatile private var initialized = false

    suspend fun fetch(context: Context, url: String): Result<VideoInfo> = withContext(Dispatchers.IO) {
        runCatching {
            ensureInit(context)
            val extractor = YoutubeStreamExtractor(url)
            extractor.fetchPage()
            val heights = (extractor.videoStreams + extractor.videoOnlyStreams)
                .map { it.getHeight() }
                .filter { it > 0 }
            val hasAudio = extractor.audioStreams.isNotEmpty()
            NewPipeMapper.toVideoInfo(
                videoId = extractor.id,
                title = extractor.name,
                durationSec = extractor.length.toInt(),
                thumbnail = extractor.thumbnails.maxByOrNull { it.height }?.url,
                heights = heights,
                hasAudio = hasAudio
            )
        }
    }

    private fun ensureInit(context: Context) {
        if (initialized) return
        NewPipe.init(NewPipeDownloader(context.applicationContext))
        // v0.27.1: pilih yang ada saat kompilasi — YoutubeStreamExtractor.setPoTokenProvider(...)
        // atau YoutubeParsingHelper.setPoTokenProvider(...). Cek dengan: grep -rn "setPoTokenProvider" ~/.gradle/caches
        setNewPipePoTokenProvider(NewPipePoTokenProvider(context.applicationContext))
        initialized = true
    }
}
```

`NewPipePoTokenProvider` + `setNewPipePoTokenProvider` (satu file `NewPipePoTokenProvider.kt`) implements `org.schabi.newpipe.extractor.services.youtube.PoTokenProvider`, memanggil `runBlocking { PoToken.tokens(context, "https://www.youtube.com/watch?v=$videoId") }` dan mengembalikan `PoTokenResult(visitorData, player, streaming)` untuk `getWebClientPoToken`; method lain return null.

- [ ] **Step 3: Verifikasi kompilasi**

Run: `./gradlew --no-daemon :app:compileReleaseKotlin`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/app/ytcutter/data/NewPipeDownloader.kt app/src/main/java/app/ytcutter/data/YouTubeInfo.kt app/src/main/java/app/ytcutter/data/NewPipePoTokenProvider.kt
git commit -m "feat: NewPipe info extractor with cookies and poToken"
```

---

### Task 5: Alur Beranda — NewPipe dulu, prefetch yt-dlp di latar

**Files:**
- Create: `app/src/main/java/app/ytcutter/data/InfoCache.kt`
- Modify: `app/src/main/java/app/ytcutter/MainActivity.kt` (blok `onSubmit` di `MainContent`)
- Modify: `app/src/main/java/app/ytcutter/ui/HomeScreen.kt` (hanya jika perlu status teks)

**Interfaces:**
- Consumes: `YouTubeInfo.fetch`, `NewPipeMapper`, `YtDlpRepo.json`, `UrlValidator.isYouTube/videoId`, `MetadataParser.parse`.
- Produces: `object InfoCache { fun save(context: Context, url: String, json: String): String?; fun pathFor(url: String): String? }` — file `cacheDir/info-<videoId>.json`, ingat `url → path` di memori.

- [ ] **Step 1: Implementasi InfoCache**

Simpan JSON ke nama per-video (`info-<videoId>.json`) supaya URL berbeda tidak saling menimpa; `pathFor` mengembalikan path hanya jika URL yang diminta sama dengan yang tersimpan.

- [ ] **Step 2: Ubah onSubmit**

```kotlin
homeState = HomeState.Loading
scope.launch {
    val youtube = UrlValidator.isYouTube(url)
    val fast = if (youtube) YouTubeInfo.fetch(context, url).getOrNull() else null
    if (fast != null) {
        val infoJson = InfoCache.pathFor(url)
        homeState = HomeState.Loaded(fast, url, infoJson)
        scope.launch {
            val meta = YtDlpRepo.json(context, url)
            if (meta.json != null) InfoCache.save(context, url, meta.json)
        }
    } else {
        val result = YtDlpRepo.json(context, url)
        val info = result.json?.let { MetadataParser.parse(it) }
        homeState = when {
            info == null -> HomeState.Error(ErrorMapper.map(result.error ?: "").message)
            info.heights.isEmpty() && !info.hasAudio -> HomeState.Error(
                "URL ini bukan video tunggal (playlist/kanal). Tempel tautan video."
            )
            else -> HomeState.Loaded(info, url, result.json?.let { InfoCache.save(context, url, it) })
        }
    }
}
```

- [ ] **Step 3: Verifikasi kompilasi + unit test**

Run: `./gradlew --no-daemon testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, semua test lulus.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/app/ytcutter/data/InfoCache.kt app/src/main/java/app/ytcutter/MainActivity.kt
git commit -m "feat: instant YouTube info with background yt-dlp prefetch"
```

---

### Task 6: Rilis 0.2.0 — versi, build, APK, unggah

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/java/app/ytcutter/ui/SettingsScreen.kt`

**Interfaces:**
- Consumes: all previous tasks.
- Produces: APK release 0.2.0 di CI + tautan gofile.

- [ ] **Step 1: Naikkan versi**

`versionCode = 12`, `versionName = "0.2.0"`; footer Settings `YT Cutter · 0.2.0 · arm64-v8a`.

- [ ] **Step 2: Build lokal**

Run: `ANDROID_HOME=/opt/android-sdk JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew --no-daemon testDebugUnitTest assembleRelease`
Expected: `BUILD SUCCESSFUL`; APK di `app/build/outputs/apk/release/app-release.apk`.

- [ ] **Step 3: Commit + push**

```bash
git add -A
git commit -m "chore: release 0.2.0"
git push origin main
```

- [ ] **Step 4: Tunggu CI, ambil artifact, verifikasi signature, unggah**

Poll `https://api.github.com/repos/Michelle-Mission/ytcutter/actions/runs` sampai run head terbaru `success`; unduh artifact `ytcutter-apk`; jalankan `apksigner verify --print-certs` (harus SHA-256 `76ef1c7e0ad42a081065a5f2b6d22274a44f8dca500bc640936aa8df4faa9dc0`); unggah ke gofile; laporkan ukuran + sha256 + tautan.