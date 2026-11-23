# YT Cutter 0.4.0 Implementation Plan — 14 Modul, Design System, Update Center

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans (inline, sudah dipilih). Steps pakai checkbox.

**Goal:** Pecah aplikasi jadi 14 modul Gradle, seragamkan semua layar lewat satu design system, perbaiki 3 bug UX, tambah Pusat Komponen + notifikasi pembaruan aplikasi.

**Architecture:** `:app` hanya NavHost + bottom bar; `feature:*` berisi layar; `core:*` berisi model, util, DB, prefs, network, notifikasi, design system, rute, dan data/worker. Dependensi satu arah, tanpa Hilt (DI manual `AppGraph`).

**Tech Stack:** Kotlin 2.4.21, AGP 8.13.2, Compose BOM 2025.06.01, navigation-compose 2.9.5, WorkManager 2.10.5, OkHttp 4.12.0, youtubedl-android 0.18.1, NewPipeExtractor v0.27.1, SQLiteOpenHelper.

**Spec:** `docs/superpowers/specs/2026-11-23-modular-architecture-ui-updates-design.md`

## Global Constraints

- minSdk 29, compileSdk/targetSdk 36, arm64-v8a, R8, `useLegacyPackaging = true`, `extractNativeLibs="true"` — tidak berubah.
- Versi akhir 0.4.0 / versionCode 15. Rilis lewat tag `v0.4.0` (CI memakai `KEYSTORE_BASE64`).
- Tanpa KSP/kapt/Room/Hilt. Tanpa convention plugin `build-logic`.
- Semua teks UI bahasa Indonesia. Tidak menyalin kode YTDLnis.
- Nama paket mengikuti modul: `app.ytcutter.core.model`, `app.ytcutter.core.common`, `app.ytcutter.core.database`, `app.ytcutter.core.datastore`, `app.ytcutter.core.network`, `app.ytcutter.core.notifications`, `app.ytcutter.core.designsystem`, `app.ytcutter.core.navigation`, `app.ytcutter.core.data`, `app.ytcutter.feature.home`, `app.ytcutter.feature.downloads`, `app.ytcutter.feature.settings`, `app.ytcutter.feature.cookies`.
- Tidak ada `return@Box` atau layar detail di dalam konten tab; rute detail lewat NavHost.
- Verifikasi tiap task: `ANDROID_HOME=/opt/android-sdk JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew --no-daemon testDebugUnitTest assembleRelease`.

## Review Focus

- 14 modul: resource/Manifest merge tetap menghasilkan satu APK dengan service `dataSync` dan native lib utuh — verifikasi `aapt2 dump badging` + `unzip -l | grep lib/arm64`.
- Migrasi preferensi cookie lama: pengguna yang sudah menyalakan "Pakai cookie" tidak kehilangan pilihan — test `UserPrefsTest` (fungsi migrasi murni).
- Versi aplikasi: perbandingan semver (`0.10.0` > `0.9.9`), bukan string — test `VersionTest`.
- Cookie auto-exit: tidak menutup WebView sebelum penyimpanan selesai; simpan idempoten — test `AuthCookieTest` + `CookieDedupeTest`.
- Worker update: notifikasi tidak berulang per versi dan hormati "Nanti" — test `UpdateDedupTest`.

---

### Task 1: Skeleton 14 modul

**Files:** Modify `settings.gradle.kts`, `gradle/libs.versions.toml`; create `core/*/build.gradle.kts`, `feature/*/build.gradle.kts`; keep `app` existing code.

- [ ] Tambah `android-library` plugin + `androidx-navigation-compose` (2.9.5) ke katalog; `include(":core:...", ":feature:...")` semua 14.
- [ ] Tiap modul library: `plugins { android-library, kotlin-android (+ kotlin-compose untuk designsystem/feature) }`, namespace `app.ytcutter.<modul>`, compileSdk 36, minSdk 29, JVM 17.
- [ ] `./gradlew projects` menampilkan 14 project; `assembleRelease` masih sukses (kode masih di `:app`).
- [ ] Commit `build: skeleton 14 modul`.

### Task 2: `:core:model`

- [ ] Pindah `data/history/DownloadRow.kt` → `core/model/src/main/java/app/ytcutter/core/model/DownloadRow.kt` (paket `app.ytcutter.core.model`, `Action` tetap di `:core:model` sebagai `Action.kt`).
- [ ] Tambah `ComponentInfo(name, version, updatable, note)`, `ReleaseInfo(version, tag, url, notes)`, `AppUpdate(latest: ReleaseInfo, currentVersion: String, available: Boolean)`.
- [ ] Test `DownloadStatusTest` pindah ke `core/model/src/test`; commit.

### Task 3: `:core:common`

- [ ] Pindah `UrlValidator`, `TimeCode`, `Range`, `FileNames`, `RetryPolicy`, `ProgressLine`, `NetscapeCookies`, `data/cookies/NetscapeFile` (paket `app.ytcutter.core.common`).
- [ ] Tambah `Version` (`parse("v0.4.0")`, `compareTo` numerik) dan `AuthCookie.isLoggedIn(lines: List<String>)` (`SID`, `__Secure-1PSID`, `__Secure-3PSID`, `LOGIN_INFO`).
- [ ] Test pindah + `VersionTest` + `AuthCookieTest` (RED→GREEN); commit.

### Task 4: `:core:database`

- [ ] Pindah `HistoryDb`, `HistoryRepo`, `CookiesRepo`, `CookieRow` + `HistoryRules` (paket `app.ytcutter.core.database`), `dependsOn(:core:model, :core:common)`.
- [ ] `CookiesRepo.insertOrReplace(url, description, content)` idempoten (isi identik → update entri terbaru); `insert` lama tetap.
- [ ] Test `HistoryRepoLogicTest` pindah; commit.

### Task 5: `:core:datastore`

- [ ] `UserPrefs(context)` kunci `use_cookies` (baca nilai lama `settings` bila ada, lalu tulis ke `users`), `UpdatePrefs` (`lastCheckMs`, `latestVersion`, `ignoredVersion`).
- [ ] Test `UserPrefsTest` (fungsi murni `migrate(old: Boolean?, stored: Boolean?): Boolean`); commit.

### Task 6: `:core:network`

- [ ] `HttpClient` (OkHttp, timeout 10 s), `GitHubReleasesApi.latest(owner, repo): ReleaseInfo?` (`org.json`, tolak tag non-`v`), `CookieProbeApi.probe(cookieHeader): String?` (URL akhir), `classify` pindah dari `CookieCheck`.
- [ ] Test `Version` dipakai di `ReleaseInfo`; test `CookieClassifyTest` (accounts → NOT_LOGGED_IN, consent → UNKNOWN, youtube → LOGGED_IN); commit.

### Task 7: `:core:notifications`

- [ ] Pindah `DownloadNotifications`; tambah `UpdateNotifier(context, release, onOpen)` kanal `updates` (IMPORTANCE_DEFAULT, aksi buka Settings).
- [ ] Tanpa test; commit.

### Task 8: `:core:designsystem`

- [ ] Pindah tema (`Theme.kt`) + `Motion.kt`; tambah `AppSpacing`, `AppBottomBar`, `ScreenScaffold`, `ScreenTitle`, `SectionHeader`, `AppCard`, `SettingsRow`, `StatusPill`, `PrimaryAction`, `SecondaryAction`, `EmptyState`, `ProgressRow`, `ValueRow`.
- [ ] `@Preview` minimal; commit.

### Task 9: `:core:navigation`

- [ ] `Route` sealed: `Home("home")`, `Downloads("downloads")`, `Settings("settings")`, `Components("settings/components")`, `About("settings/about")`, `Cookies("cookies")`; `object Routes { val topLevel = listOf(Home, Downloads, Settings); fun hidesBottomBar(route: String?): Boolean }` + test `RoutesTest`; commit.

### Task 10: `:core:data`

- [ ] Pindah seluruh sisa `data/*` + `work/*` (paket `app.ytcutter.core.data`, `app.ytcutter.core.data.work`), depend ke `model, common, database, datastore, network, notifications`.
- [ ] `AppGraph(context)` singleton: `historyRepo`, `cookiesRepo`, `userPrefs`, `updatePrefs`, `componentRepo`, `appUpdateRepo`, `downloads`.
- [ ] `ComponentRepository.components()`, `updateYtDlp()`; `AppUpdateRepository.check()`; `UpdateCheckWorker` (harian, CONNECTED; dedupe `ignoredVersion`); `Downloads` fasad memakai `UserPrefs`.
- [ ] Hapus `autoUpdateOnce` dari jalur start.
- [ ] Test pindah + `UpdateDedupTest`; commit.

### Task 11: `:feature:home`

- [ ] Pindah `HomeScreen`, `FormatSheet` (paket `app.ytcutter.feature.home`); ganti padding/judul dengan `ScreenScaffold` + token; perilaku sama.
- [ ] Commit.

### Task 12: `:feature:downloads`

- [ ] Pindah `DownloadsScreen`; `ScreenScaffold("Unduhan")`, kartu pakai `AppCard` + `StatusPill` + `ProgressRow`; ongkos jarak `xs/sm`.
- [ ] Commit.

### Task 13: `:feature:cookies`

- [ ] Pindah `CookieScreen`, `CookieLoginActivity` (paket `app.ytcutter.feature.cookies`).
- [ ] `CookieScreen`: `ScreenScaffold(title="Cookie", onBack)`, switch+aksi, daftar `AppCard` jarak `xs`, menu 3 titik sama.
- [ ] `CookieLoginActivity`: status dari `WebViewCookieReader` setelah `onPageFinished`; auto-save+finish bila `AuthCookie.isLoggedIn`; `insertOrReplace`; pakai `ScreenScaffold`/token.
- [ ] Commit.

### Task 14: `:feature:settings`

- [ ] Pindah `SettingsScreen`; kategori Unduhan/Cookie/Tentang memakai `SectionHeader` + `SettingsRow`; banner update (`Nanti` → `ignoredVersion`).
- [ ] Tambah `ComponentsScreen` (daftar komponen + "Periksa pembaruan" + update yt-dlp) dan `AboutScreen`.
- [ ] Commit.

### Task 15: `:app` final

- [ ] `YtCutterApp` (init `AppGraph`, jadwalkan `UpdateCheckWorker` harian), `MainActivity` dengan `NavHost` + `AppBottomBar` (tampil hanya `Routes.topLevel`, `hidesBottomBar`).
- [ ] Hapus seluruh kode lama di `app/src/main/java/app/ytcutter/{data,ui,work,core}` yang sudah pindah; `app` hanya `MainActivity`, `YtCutterApp`, `AppNavHost`, `AppBottomBar` wiring.
- [ ] Manifest: activity Cookie di paket baru; R8 keep tetap; commit.

### Task 16: Verifikasi & rilis

- [ ] `versionCode 15`, `versionName "0.4.0"`; full `clean testDebugUnitTest assembleRelease`.
- [ ] Cek `aapt2 badging` (15/0.4.0), `apksigner` (cert `76ef1c7e…` dari CI), `unzip -l | grep lib/arm64-v8a/libpython` ada.
- [ ] Push `main` + tag `v0.4.0`, tunggu CI, verifikasi asset rilis (nama `YT-Cutter-0.4.0.apk`), laporkan URL + sha256.