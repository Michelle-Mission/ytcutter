# YT Cutter 0.3.0 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Riwayat unduhan persisten + progress real-time lewat WorkManager, dan pengaturan cookie penuh bergaya YTDLnis.

**Architecture:** Satu `QueueWorker` WorkManager unik menghabiskan baris `QUEUED` dari SQLite (satu-satunya sumber kebenaran antrean), menulis progress ke DB tiap ≤250 ms; UI membaca `Flow` dari DB. Cookie disimpan sebagai entri di DB yang sama, digabung menjadi `cookies.txt` hanya saat switch "Pakai cookie" aktif.

**Tech Stack:** Kotlin 2.4.21, Compose, `androidx.work:work-runtime-ktx:2.10.5`, `SQLiteOpenHelper`, youtubedl-android 0.18.1, okhttp 4.12.0, JUnit4.

**Spec:** `docs/superpowers/specs/2026-11-23-downloads-history-cookies-design.md`

## Global Constraints

- minSdk 29, targetSdk 36, compileSdk 36, arm64-v8a, R8 aktif, `useLegacyPackaging = true`, `extractNativeLibs="true"`.
- Versi akhir: `versionCode 14`, `versionName "0.3.0"`.
- Tidak ada KSP/kapt/Room. DB pakai `SQLiteOpenHelper`.
- Repo GPL-3.0; jangan menyalin kode YTDLnis, hanya perilakunya.
- Lisensi/teks UI berbahasa Indonesia seperti sekarang.
- Build verify: `ANDROID_HOME=/opt/android-sdk JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew --no-daemon testDebugUnitTest assembleRelease`.
- Objek murni (testable JVM) tidak boleh mengimpor `android.*`: `ProgressLine`, `NetscapeFile`, `CookieCheck.classify`, `RetryPolicy`.

## Review Focus

- File parsial `.part` dari unduhan lama: resume `--continue` tidak boleh mencampur file video/audio berbeda kualitas — folder per id mencegahnya; test `DownloadArgsTest` memastikan `--continue` ada.
- Baris DB `RUNNING` sisa proses mati: harus jadi `INTERRUPTED` dan jalan lagi, tanpa dobel publish ke MediaStore — satu worker unik, test `HistoryRepo` tak tersedia di CI; verifikasi lewat review alur.
- Cookie kosong/`getCookie` tanpa HttpOnly: ekspor harus tetap menghasilkan entri valid atau `null`, tidak menulis `cookies.txt` kosong — test `NetscapeFileTest`.
- Progress line `N/A` (ranged download): tidak boleh menulis 0% terus-menerus — test `ProgressLineTest`.
- Switch "Pakai cookie" mati: file cookie lama tidak boleh ikut terkirim — verifikasi manual + unit test `CookieGateTest` (fungsi murni pemilih path).

---

### Task 1: Parser progress real-time

**Files:**
- Create: `app/src/main/java/app/ytcutter/data/ProgressLine.kt`
- Test: `app/src/test/java/app/ytcutter/ProgressLineTest.kt`

**Interfaces:**
- Produces: `data class DownloadProgress(val percent: Int, val detail: String)`; `object ProgressLine { fun parse(line: String): DownloadProgress? }`

- [ ] **Step 1: Write the failing test**

```kotlin
class ProgressLineTest {
    @Test fun parsesByteBasedTemplateLine() {
        val p = ProgressLine.parse("YTC 5242880 10485760 50.0% 1.20MiB/s 00:04")!!
        assertEquals(50, p.percent)
        assertEquals("1.20MiB/s · ETA 00:04", p.detail)
    }
    @Test fun byteRatioWinsOverStalePercent() {
        assertEquals(75, ProgressLine.parse("YTC 7864320 10485760 12.0% 900KiB/s 00:02")!!.percent)
    }
    @Test fun fallsBackToPercentWhenTotalUnknown() {
        assertEquals(42, ProgressLine.parse("YTC NA NA 42.0% NA NA")!!.percent)
    }
    @Test fun fallsBackToLegacyDownloadLine() {
        assertEquals(10, ProgressLine.parse("[download]  10.5% of 9.00MiB at 1MiB/s ETA 00:09")!!.percent)
    }
    @Test fun ignoresNonProgressLines() {
        assertNull(ProgressLine.parse("[youtube] Extracting URL"))
        assertNull(ProgressLine.parse(""))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `ANDROID_HOME=/opt/android-sdk JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew --no-daemon testDebugUnitTest --tests 'app.ytcutter.ProgressLineTest'`
Expected: FAIL — unresolved reference `ProgressLine`.

- [ ] **Step 3: Implement `ProgressLine.parse`**

Percent = `floor(100 * down / total)` bila `total > 0` dan `down >= 0`; selain itu pakai token persen (`42.0%`). Detail = `"<speed> · ETA <eta>"` dengan `NA` dihilangkan (bila keduanya NA → `detail` = string kosong). Legacy: regex `\[download\]\s+(\d+(?:\.\d+)?)%`, detail dari teks setelah persen dipangkas 60 karakter. Persen di-clamp 0..100.

- [ ] **Step 4: Run test to verify it passes**

Run same command. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/app/ytcutter/data/ProgressLine.kt app/src/test/java/app/ytcutter/ProgressLineTest.kt
git commit -m "feat: parser progress yt-dlp"
```

---

### Task 2: Skema DB riwayat + cookie

**Files:**
- Create: `app/src/main/java/app/ytcutter/data/history/HistoryDb.kt`
- Create: `app/src/main/java/app/ytcutter/data/history/DownloadRow.kt`
- Test: `app/src/test/java/app/ytcutter/DownloadStatusTest.kt`

**Interfaces:**
- Produces:
  - `enum class DownloadStatus { QUEUED, RUNNING, DONE, FAILED, CANCELED, INTERRUPTED }` dengan `fun fromDb(text: String): DownloadStatus` (unknown → `FAILED`) dan `val dbValue: String`.
  - `data class DownloadRow(id: Long, videoId: String?, url: String, title: String, qualityLabel: String, height: Int?, startSec: Int, endSec: Int, status: DownloadStatus, progress: Int, detail: String?, error: String?, errorAction: Action, uri: String?, fileName: String?, createdAt: Long, updatedAt: Long)`
  - `class HistoryDb(context: Context) : SQLiteOpenHelper(context, "ytcutter.db", null, 1)` dengan tabel `downloads` + `cookies` (kolom sesuai spec §1 dan §5).

- [ ] **Step 1: Write the failing test** for `DownloadStatus.fromDb` round-trip semua nilai + nilai tak dikenal → `FAILED`.
- [ ] **Step 2: Run** `--tests 'app.ytcutter.DownloadStatusTest'` → FAIL.
- [ ] **Step 3: Implement** enum + `HistoryDb.onCreate` (`CREATE TABLE downloads (...)`; index `idx_downloads_status(status, created_at)`; `CREATE TABLE cookies (...)`) dan `onUpgrade` = drop + create (DB baru).
- [ ] **Step 4: Run** test → PASS.
- [ ] **Step 5: Commit** `feat: skema DB riwayat dan cookie`

---

### Task 3: `HistoryRepo` (antrean + Flow)

**Files:**
- Create: `app/src/main/java/app/ytcutter/data/history/HistoryRepo.kt`
- Test: `app/src/test/java/app/ytcutter/HistoryRepoLogicTest.kt` (hanya logika murni yang diekstrak)

**Interfaces:**
- Produces: `class HistoryRepo private constructor(context: Context)`; `companion object { fun get(context: Context): HistoryRepo }`; `val downloads: StateFlow<List<DownloadRow>>`; `fun enqueue(request: DownloadRequest, title: String): Long`; `fun markRunning(id: Long)`; `fun updateProgress(id: Long, percent: Int, detail: String?)`; `fun finish(id: Long, status: DownloadStatus, uri: String? = null, fileName: String? = null, error: String? = null, action: Action = Action.NONE)`; `fun cancel(id: Long)`; `fun retry(id: Long)`; `fun delete(id: Long)`; `fun nextQueued(): DownloadRow?`; `fun sweepInterrupted(): Int`; `fun get(id: Long): DownloadRow?`; `internal fun refresh()`.
- Pure helper produced: `object HistoryRules { fun canRetry(status: DownloadStatus): Boolean; fun isFinal(status: DownloadStatus): Boolean }`

- [ ] **Step 1: Failing test** `HistoryRepoLogicTest`: `canRetry` true untuk FAILED/INTERRUPTED/CANCELED, false untuk QUEUED/RUNNING/DONE.
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3: Implement** `HistoryRules` + repo (tulis via `HistoryDb.writableDatabase`, `refresh()` memuat 200 baris terbaru ke `MutableStateFlow`; `retry` set QUEUED/progress 0/error null/updatedAt now; `cancel` set CANCELED bila belum final; `nextQueued` ORDER BY created_at ASC LIMIT 1; `sweepInterrupted` UPDATE RUNNING→QUEUED, progress 0).
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `feat: repo riwayat unduhan`

---

### Task 4: Berkas cookie (gabung/parse murni) + `CookiesRepo`

**Files:**
- Create: `app/src/main/java/app/ytcutter/data/cookies/NetscapeFile.kt`
- Create: `app/src/main/java/app/ytcutter/data/cookies/CookiesRepo.kt`
- Create: `app/src/main/java/app/ytcutter/data/cookies/CookiePrefs.kt`
- Test: `app/src/test/java/app/ytcutter/NetscapeFileTest.kt`

**Interfaces:**
- Produces:
  - `object NetscapeFile { const val HEADER = "# Netscape HTTP Cookie File\n"; fun build(contents: List<String>): String; fun parseClipboard(text: String): String? }`
  - `data class CookieRow(id: Long, url: String, description: String, content: String, enabled: Boolean, createdAt: Long)`
  - `class CookiesRepo private constructor(context: Context)`; `companion object { fun get(context: Context): CookiesRepo }`; `val entries: StateFlow<List<CookieRow>>`; `fun insert(url: String, description: String, content: String): Long`; `fun update(id: Long, url: String, description: String)`; `fun toggle(id: Long, enabled: Boolean)`; `fun delete(id: Long)`; `fun deleteAll()`; `fun buildEnabledText(): String`; `fun rebuildFile()`; `fun enabledCount(): Int`
  - `object CookiePrefs { fun useCookies(context: Context): Boolean; fun setUseCookies(context: Context, on: Boolean) }` (prefs `"settings"`, key `"use_cookies"`, default false)

- [ ] **Step 1: Failing test**

```kotlin
class NetscapeFileTest {
    @Test fun dedupesLinesAndKeepsHttpOnly() {
        val a = "#HttpOnly_.youtube.com\tTRUE\t/\tTRUE\t0\tSID\tabc\n.youtube.com\tTRUE\t/\tTRUE\t0\tPREF\tx"
        val b = "#HttpOnly_.youtube.com\tTRUE\t/\tTRUE\t0\tSID\tabc"
        val out = NetscapeFile.build(listOf(a, b))
        assertTrue(out.startsWith(NetscapeFile.HEADER))
        assertEquals(1, out.lines().count { it.contains("\tSID\t") })
        assertTrue(out.lines().any { it.startsWith("#HttpOnly_") })
    }
    @Test fun keepsEmptyValueAsSevenFields() {
        val out = NetscapeFile.build(listOf(".youtube.com\tTRUE\t/\tTRUE\t0\tEMPTY\t"))
        assertEquals(7, out.lines().last().split('\t').size)
    }
    @Test fun clipboardRequiresCookieLines() {
        assertNull(NetscapeFile.parseClipboard("halo dunia"))
        assertNotNull(NetscapeFile.parseClipboard("# Netscape HTTP Cookie File\n.youtube.com\tTRUE\t/\tTRUE\t0\tA\tb"))
    }
}
```

- [ ] **Step 2: Run** `--tests 'app.ytcutter.NetscapeFileTest'` → FAIL.
- [ ] **Step 3: Implement** `NetscapeFile` (buang baris kosong/komentar kecuali `#HttpOnly_`, dedupe via LinkedHashSet, `trimEnd('\r')`, pertahankan baris ber-7 kolom; `parseClipboard` menerima teks yang punya ≥1 baris cookie valid, mengembalikan body tanpa header/komentar); `CookiePrefs`; `CookiesRepo` (DB `cookies`, `refresh()`, `rebuildFile()` menulis `filesDir/cookies.txt` = `buildEnabledText()` atau menghapus file bila kosong).
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `feat: penyimpanan cookie berbasis entri`

---

### Task 5: Ekspor cookie dari DB WebView

**Files:**
- Create: `app/src/main/java/app/ytcutter/data/cookies/WebViewCookieReader.kt`
- Test: `app/src/test/java/app/ytcutter/WebViewCookieReaderTest.kt` (fungsi murni)

**Interfaces:**
- Produces: `object WebViewCookieReader { fun export(context: Context, hostSuffixes: List<String> = listOf("youtube.com", "google.com")): String?; internal fun toLines(rows: List<RawCookie>): List<String>; data class RawCookie(val host: String, val name: String, val value: String, val path: String, val secure: Boolean, val expiry: Long) }`

- [ ] **Step 1: Failing test**: host tanpa titik → `.host`; baris punya 7 kolom; `#HttpOnly_` dipakai bila nama cookie di daftar HttpOnly YouTube (`SID`, `HSID`, `SSID`, `APISID`, `SAPISID`, `__Secure-1PSID`, `__Secure-3PSID`) — detail: tandai HttpOnly bila `host` asli diawali `.`? Ganti aturan: `RawCookie` punya `httpOnly: Boolean`; test memastikan prefix.
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3: Implement** `toLines`; `export` mencari `File(context.applicationInfo.dataDir).walkTopDown()` untuk file bernama `Cookies`, salin ke `cacheDir/cookies-snapshot.db` (+ `-wal`, `-shm` bila ada), buka `SQLiteDatabase.openDatabase(..., OPEN_READONLY)`, query `cookies` kolom `host_key,name,value,path,is_secure,expires_utc,is_httponly` dengan `host_key LIKE ?` per suffix; gagal/kosong → `null`.
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `feat: baca cookie HttpOnly dari DB WebView`

---

### Task 6: Cek login cookie

**Files:**
- Create: `app/src/main/java/app/ytcutter/data/cookies/CookieCheck.kt`
- Test: `app/src/test/java/app/ytcutter/CookieCheckTest.kt`

**Interfaces:**
- Produces: `enum class CookieStatus { LOGGED_IN, NOT_LOGGED_IN, UNKNOWN }`; `object CookieCheck { fun classify(finalUrl: String?, code: Int): CookieStatus; suspend fun probe(context: Context): CookieStatus }`

- [ ] **Step 1: Failing test**

```kotlin
class CookieCheckTest {
    @Test fun accountsHostMeansLoggedOut() =
        assertEquals(CookieStatus.NOT_LOGGED_IN, CookieCheck.classify("https://accounts.google.com/ServiceLogin", 200))
    @Test fun youtubeHostMeansLoggedIn() =
        assertEquals(CookieStatus.LOGGED_IN, CookieCheck.classify("https://www.youtube.com/feed/subscriptions", 200))
    @Test fun consentHostIsUnknown() =
        assertEquals(CookieStatus.UNKNOWN, CookieCheck.classify("https://consent.youtube.com/m", 200))
    @Test fun serverErrorIsUnknown() =
        assertEquals(CookieStatus.UNKNOWN, CookieCheck.classify("https://www.youtube.com/feed/subscriptions", 503))
}
```

- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3: Implement** `classify` (code !in 200..399 → UNKNOWN; host berakhiran `accounts.google.com` → NOT_LOGGED_IN; host berakhiran `youtube.com` → LOGGED_IN; selain itu UNKNOWN). `probe`: header `Cookie` dari `NetscapeCookies.header(repo.buildEnabledText().lines())`; OkHttp client `followRedirects(true)`; 10 s timeout; exception → UNKNOWN.
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `feat: verifikasi login cookie`

---

### Task 7: Worker unduhan + argumen resume/progress

**Files:**
- Create: `app/src/main/java/app/ytcutter/work/QueueWorker.kt`
- Create: `app/src/main/java/app/ytcutter/work/DownloadNotifications.kt`
- Create: `app/src/main/java/app/ytcutter/work/ActiveWork.kt`
- Create: `app/src/main/java/app/ytcutter/data/RetryPolicy.kt`
- Modify: `app/src/main/java/app/ytcutter/data/DownloadArgs.kt`
- Modify: `app/src/main/java/app/ytcutter/data/YtDlpRepo.kt` (callback progress)
- Delete: `app/src/main/java/app/ytcutter/service/DownloadService.kt`
- Modify: `app/src/main/AndroidManifest.xml` (hapus service, tambah WorkManager tidak perlu deklarasi)
- Test: `app/src/test/java/app/ytcutter/DownloadArgsTest.kt` (tambah), `app/src/test/java/app/ytcutter/RetryPolicyTest.kt`

**Interfaces:**
- Produces:
  - `DownloadArgs.build(req, outPath, cookiesPath)` kini juga menambah `--continue`, `--newline`, `--progress-template` persis:
    `download:YTC %(progress.downloaded_bytes)s %(progress.total_bytes_estimate)s %(progress._percent_str)s %(progress._speed_str)s %(progress._eta_str)s`
  - `object RetryPolicy { fun isTransient(message: String): Boolean }` (pola: `timed out`, `connection reset`, `unable to download`, `http error 5`, `temporary failure`)
  - `object ActiveWork { @Volatile var processId: String?; @Volatile var cancelId: Long? }`
  - `class QueueWorker(...) : CoroutineWorker` + `companion object { const val NAME = "download-queue"; fun ensureRunning(context: Context) }`
  - `object DownloadNotifications { fun progress(context: Context, title: String, percent: Int, detail: String?): ForegroundInfo }`

- [ ] **Step 1: Failing tests** — `DownloadArgsTest.assertOptionsContain("--continue")`, template string, tidak ada `--cookies` saat null; `RetryPolicyTest` pola transient true, `"Sign in to confirm you're not a bot"` false, `"requested format is not available"` false.
- [ ] **Step 2: Run** `--tests 'app.ytcutter.DownloadArgsTest' --tests 'app.ytcutter.RetryPolicyTest'` → FAIL.
- [ ] **Step 3: Implement** args + policy; `QueueWorker.doWork`: `repo.sweepInterrupted()`; loop `nextQueued()`; `markRunning`; folder `cacheDir/dl/<id>`; `ActiveWork.processId = YtDlpRepo.newProcessId()`; `YtDlpRepo.execute(...)` dengan callback yang memanggil `ProgressLine.parse` lalu `repo.updateProgress` + `setProgress` + notifikasi, throttle 250 ms; sukses → `MediaStorePublisher.publish` → `finish(DONE, uri, name)`; exit≠0 → transient ? `Result.retry()` (maks 2 percobaan: `runAttemptCount < 2`) : `finish(FAILED, error, action)` dan lanjut; `cancelId == id` → `finish(CANCELED)`. Selesai loop → `Result.success()`.
- [ ] **Step 4: Run** tests → PASS; `assembleRelease` (R8) → BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `feat: worker unduhan persisten + progress real-time`

---

### Task 8: UI unduhan dari DB

**Files:**
- Modify: `app/src/main/java/app/ytcutter/data/Downloads.kt` (jadi fasad: `fun enqueue(context, request, title): Boolean`, `fun cancel(context, id: Long)`, `fun retry(context, id: Long)`, hapus state in-memory/`retry(context)`)
- Modify: `app/src/main/java/app/ytcutter/ui/DownloadsScreen.kt` (list dari `HistoryRepo.downloads`)
- Modify: `app/src/main/java/app/ytcutter/MainActivity.kt` (koleksi `HistoryRepo.get(context).downloads`, kirim ke screen, `Downloads.enqueue` di `onStart`, auto-update cek pakai `entries.any { it.status == RUNNING }`)
- Test: tidak ada unit test UI; verifikasi build + review.

**Interfaces:**
- Consumes: `HistoryRepo` (Task 3), `QueueWorker.ensureRunning` (Task 7)
- Produces: `Downloads.enqueue(context: Context, request: DownloadRequest, title: String): Boolean` (true bila baris dibuat & worker dijadwalkan)

- [ ] **Step 1: Implement** fasad + UI list (LazyColumn; `RUNNING` progress+batal; `DONE` buka/bagikan/hapus riwayat; `FAILED/INTERRUPTED/CANCELED` pesan+Ulangi sesuai `Action`: `IMPORT_COOKIES` → buka `CookieLoginActivity`, `UPDATE_YTDLP` → update lalu retry, `RETRY` → buka FormatSheet lewat callback baru `onChooseFormatFor(row)`).
- [ ] **Step 2: Build** `assembleRelease` → BUILD SUCCESSFUL.
- [ ] **Step 3: Commit** `feat: riwayat unduhan di UI`

---

### Task 9: Halaman Cookie + WebView berbasis URL

**Files:**
- Create: `app/src/main/java/app/ytcutter/ui/cookies/CookieScreen.kt`
- Modify: `app/src/main/java/app/ytcutter/ui/CookieLoginActivity.kt` (terima `EXTRA_URL`, `EXTRA_DESCRIPTION`; simpan entri via `CookiesRepo`; judul "Ambil cookie"; status hidup; toast)
- Modify: `app/src/main/AndroidManifest.xml` bila perlu (activity tetap sama)
- Test: `app/src/test/java/app/ytcutter/CookieClipboardTest.kt` bila menambah helper murni; selain itu build+review.

**Interfaces:**
- Consumes: `CookiesRepo`, `CookiePrefs`, `CookieCheck`, `WebViewCookieReader`
- Produces: `@Composable fun CookieScreen(onBack: () -> Unit)`; `CookieLoginActivity.EXTRA_URL`, `EXTRA_DESCRIPTION`, `EXTRA_RESULT_COUNT`

- [ ] **Step 1: Implement** layar: switch "Pakai cookie" atas; tombol "+ Cookie" (aktif hanya saat switch nyala) → bottom sheet URL default `https://` + deskripsi; daftar entri (switch per entri, ketuk detail/salin, hapus); menu titik tiga: hapus semua (dialog), impor papan klip (`NetscapeFile.parseClipboard`), ekspor papan klip (`buildEnabledText`), ekspor berkas (`ACTION_CREATE_DOCUMENT`); tombol "Cek cookie" → toast hasil.
- [ ] **Step 2: Implement** `CookieLoginActivity`: load `EXTRA_URL` apa adanya, tombol Selesai, ekspor `WebViewCookieReader.export` → `CookiesRepo.insert` (gagal → toast "Belum ada cookie ditemukan", jangan tutup), lalu `CookiePrefs.setUseCookies(true)` + `CookieCheck.probe` toast.
- [ ] **Step 3: Build** `assembleRelease` → BUILD SUCCESSFUL.
- [ ] **Step 4: Commit** `feat: halaman cookie penuh`

---

### Task 10: Pengaturan berkategori + versi 0.3.0

**Files:**
- Modify: `app/src/main/java/app/ytcutter/ui/SettingsScreen.kt` (kategori "Unduhan", "Cookie" → buka `CookieScreen`, "Tentang"; hapus kartu cookie lama)
- Modify: `app/src/main/java/app/ytcutter/MainActivity.kt` (rute cookie + back)
- Modify: `app/build.gradle.kts` (versionCode 14, versionName "0.3.0")
- Modify: `app/src/main/java/app/ytcutter/data/CookieStore.kt` (pensiunkan pemakaian langsung; `path()` hanya dipakai lewat gerbang `CookiePrefs`)
- Modify: `app/src/main/java/app/ytcutter/data/DownloadArgs.kt`/pemanggil agar cookie path = `if (CookiePrefs.useCookies(ctx)) CookieStore.path(ctx) else null`
- Test: `app/src/test/java/app/ytcutter/CookieGateTest.kt` (fungsi murni `CookieGate.path(file: File?, enabled: Boolean)`)
- Modify: `README.md` (bagian cookie opsional)

- [ ] **Step 1: Failing test** `CookieGateTest`: `enabled=false` + file ada → null; `enabled=true` + file ada → path; file null → null.
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3: Implement** `CookieGate` + pakai di `YtDlpRepo` (json & download) dan `NewPipeDownloader`; UI kategori; versi; README.
- [ ] **Step 4: Run** semua test + `assembleRelease` → PASS + BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `feat: pengaturan berkategori, cookie opsional, 0.3.0`

---

### Task 11: Verifikasi menyeluruh + rilis

**Files:** tidak ada file baru; hanya verifikasi dan rilis.

- [ ] **Step 1: Full verify**

Run: `ANDROID_HOME=/opt/android-sdk JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew --no-daemon clean testDebugUnitTest assembleRelease`
Expected: `BUILD SUCCESSFUL`, semua test PASS.

- [ ] **Step 2: Cek APK** `aapt2 dump badging` → `versionCode='14' versionName='0.3.0'`; `apksigner verify` → cert `76ef1c7e…`; `unzip -t` tanpa error.

- [ ] **Step 3: Push + CI** commit, push `main`, tunggu workflow sukses, unduh artifact, bandingkan sha256 dengan build lokal.

- [ ] **Step 4: Rilis** buat GitHub Release `v0.3.0` + asset `YT-Cutter-0.3.0.apk`, laporkan URL + sha256 + ukuran.