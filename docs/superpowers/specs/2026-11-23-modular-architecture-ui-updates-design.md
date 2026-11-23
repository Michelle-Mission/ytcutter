# YT Cutter 0.4.0 — Arsitektur Multi-Module, Design System, Update Center

**Tanggal:** 2026-11-23 · **Status:** menunggu persetujuan · **Menggantikan:** struktur datar 0.3.0

## 1. Intent

Aplikasi 0.3.0 berfungsi tapi tidak rapi: seluruh kode di satu modul (`app`), tiap layar mengatur
padding/tipografi sendiri, update hanya yt-dlp, dan tiga bug UX (status cookie di WebView, bottom bar
mati di layar Cookie, desain Cookie tidak konsisten). 0.4.0 menata ulang jadi **14 modul Gradle**
mengikuti pola `android/nowinandroid`, menyeragamkan seluruh tampilan lewat satu design system,
memperbaiki bug tersebut, dan menambah **Pusat Komponen + notifikasi pembaruan aplikasi**.

## 2. Tujuan

- 14 modul Gradle dengan batas tanggung jawab jelas dan arah dependensi satu arah.
- Satu design system: token jarak/tipografi/bentuk + komponen bersama; **semua** layar memakai itu.
- Bottom navigation custom (pill mengambang, indikator animasi) yang tahu rute detail.
- Layar Cookie aktif: status benar, keluar otomatis setelah cookie login, gaya seragam.
- Layar Komponen: versi semua bagian + update yt-dlp di tempat + update aplikasi dari GitHub.
- Notifikasi + banner bila rilis aplikasi lebih baru.
- Nomor versi 0.4.0 (versionCode 15), update in-place dengan sertifikat rilis yang sama
  (`76ef1c7e…`). Rilis hanya lewat tag `v0.4.0`; CI membangun ulang keystore dari secret
  `KEYSTORE_BASE64` (terbukti dipakai rilis 0.3.0).

## 3. Non-goal / anti-goal

- Bukan dynamic feature / multi-APK; tetap satu APK arm64-v8a, minSdk 29.
- Tidak memakai Hilt/Dagger (KSP tidak tersedia untuk Kotlin 2.4.21; kapt memperlambat build).
  DI manual lewat `AppGraph` singleton di `:core:data` + `Application`.
- Tidak menambah ability update untuk library ter-compile (NewPipeExtractor/ffmpeg/PO token):
  itu mustahil saat runtime; layar Komponen menampilkannya sebagai "ikut pembaruan aplikasi".
- Tidak mengubah perilaku mesin unduh (argumen yt-dlp, antrean WorkManager) selain yang disebut.
- Tidak menyalin kode GPL dari YTDLnis.

## 4. Pendekatan yang dipertimbangkan

| Opsi | Isi | Konsekuensi |
|---|---|---|
| A | 9 modul kelompok fitur | Cepat, tapi `core:data` besar |
| B | 5 modul | Paling cepat, batas kabur |
| **C (dipilih)** | 14 modul ala NIA | Batas tegas, build config lebih banyak, sesuai permintaan |

## 5. Peta modul (14)

| Modul | Isi | Depend ke |
|---|---|---|
| `:app` | `MainActivity`, `YtCutterApp`, `NavHost`, `AppBottomBar` wiring, signing/R8 | semua `feature:*`, `core:navigation`, `core:designsystem`, `core:data` |
| `:core:model` | `DownloadRow/Status`, `ComponentInfo`, `ReleaseInfo`, `AppVersion`, enum murni | — |
| `:core:common` | `UrlValidator`, `TimeCode`, `Range`, `FileNames`, `RetryPolicy`, `ProgressLine`, `NetscapeFile`, `Version`, `AuthCookie` | `:core:model` |
| `:core:database` | `HistoryDb`, `HistoryRepository`, `CookiesRepository` (SQLite, tanpa Android UI) | `:core:model` |
| `:core:datastore` | `UserPrefs` (pakai cookie), `UpdatePrefs` (cek terakhir, versi terbaru, versi diabaikan) | — |
| `:core:network` | `HttpClient` OkHttp, `GitHubReleasesApi` (parse `org.json`), `CookieProbeApi` | `:core:model`, `:core:common` |
| `:core:notifications` | kanal `downloads` & `updates`, `DownloadNotifications`, `UpdateNotifier` | — |
| `:core:designsystem` | tema, `AppSpacing/AppDimens`, komponen bersama (daftar §7) | — |
| `:core:navigation` | `Route` sealed (Home, Downloads, Settings, Cookies, Components), `topLevelRoutes` | — |
| `:core:data` | `YtDlpRepo`, `Downloads` fasad, `QueueWorker`, `InfoCache`, `MetadataParser`, `NewPipe*`, `PoToken`, `MediaStorePublisher`, `CookieStore/Gate`, `WebViewCookieReader`, `CookieStatusChecker`, `ComponentRepository`, `AppUpdateRepository`, `UpdateCheckWorker`, `CrashLog` | `model, common, database, datastore, network, notifications` |
| `:feature:home` | `HomeScreen`, `FormatSheet`, state + pemanggilan data | `designsystem, model, data, navigation` |
| `:feature:downloads` | `DownloadsScreen` + kartu riwayat | `designsystem, model, data, navigation` |
| `:feature:settings` | `SettingsScreen`, `ComponentsScreen`, `AboutScreen` (dalam satu modul) | `designsystem, model, data, navigation` |
| `:feature:cookies` | `CookieScreen`, `CookieLoginActivity` | `designsystem, model, data, navigation` |

Aturan: `feature:*` tidak saling bergantung; `core:*` tidak bergantung ke `feature:*`; `:app` satunya
yang tahu semua fitur. Tidak ada convention plugin `build-logic` (diputuskan: 14 modul × ~25 baris
build.gradle masih terang-terangan; tambah nanti bila modul >20).

## 6. Design system

- `AppSpacing`: `xxs=4, xs=8, sm=12, md=16, lg=20, xl=24, xxl=32`.
- Layar: padding horizontal `lg`, jarak antarseksi `lg`, jarak antaritem `xs/sm`.
- `ScreenScaffold(title, subtitle?, onBack?, actions?, scroll=true, bottomBar?)` — satu-satunya
  pembungkus layar; menangani status bar inset, judul `headlineSmall`, subjudul `bodySmall`.
- `SectionHeader(text)` — label kapital kecil warna `primary`, jarak bawah `xs`.
- `AppCard(onClick?, content)` — `shapes.large`, `surface`, padding `md`.
- `SettingsRow(icon, title, subtitle, trailing?, showChevron, onClick)`.
- `StatusPill(text, tone: Neutral|Active|Error)`.
- `PrimaryAction`, `SecondaryAction` (tombol seragam dengan ikon opsional).
- `EmptyState(icon, title, message)`, `ProgressRow(percent, label)`, `ValueRow(label, value)`.
- `AppBottomBar(items, currentRoute, onSelect)`: Surface `surface` + shadow, `extraLarge`, tinggi 64dp +
  navigasi inset; tiap item ikon+label `labelSmall`; indikator `primaryContainer` di belakang item
  aktif, animasi `spring`; hanya tampil di rute top-level.

Semua padding/ukuran font di layar lama diganti token; dilarang `padding(18.dp)` ad-hoc.

## 7. Navigasi

- `navigation-compose` (AndroidX, tanpa codegen) sebagai satu-satunya navigator.
- Rute: `home`, `downloads`, `settings`, `settings/components`, `settings/about`, `cookies` (detail
  dari Settings). Ketuk tab = `popUpTo(start) { saveState }` + `restoreState`.
- Bottom bar disembunyikan otomatis untuk `settings/*` dan `cookies`; tombol back sistem kembali.
- **Bug fix:** layar detail tidak lagi dirender di dalam konten tab, jadi bottom bar tidak pernah
  "hidup tapi mati".

## 8. Perbaikan layar Cookie

- Status awal "Memeriksa cookie…"; setelah tiap `onPageFinished`, `WebViewCookieReader` dibaca di IO
  dan status dihitung dari DB (bukan header request) → HttpOnly terlihat.
- `AuthCookie.isLoggedIn(lines)` benar bila ada `SID` atau `__Secure-1PSID` atau `__Secure-3PSID`
  atau `LOGIN_INFO`.
- Bila cookie login terdeteksi: tunggu 800 ms agar redirect selesai, simpan, nyalakan
  `UserPrefs.useCookies`, `RESULT_OK`, tutup activity (keluar otomatis). Berlaku untuk semua alur
  (tambah entri maupun retry unduhan) — pengguna tetap bisa membatalkan dengan tombol back.
- Simpan bersifat idempoten: bila sudah ada entri cookie dengan isi identik, entri itu diperbarui
  (bukan ditambah), jadi keluar otomatis berulang tidak membuat duplikat.
- Tombol "Selesai" manual tetap ada dan menyimpan cookie apa pun yang ada (bukan hanya auth).
- Tampilan memakai `ScreenScaffold` + `AppCard` + `StatusPill`; daftar entri memakai `SettingsRow`-style
  dengan jarak `xs`; sama dengan layar lain.

## 9. Pusat Komponen & update

- `ComponentRepository.components()` → daftar: yt-dlp (`YoutubeDL.version`), ffmpeg (terpasang/tidak),
  NewPipeExtractor (`NewPipe.getVersion()`), PO token (versi aplikasi), aplikasi (`PackageManager`).
- `ComponentRepository.updateYtDlp()` memanggil `YoutubeDL.update` dan mengembalikan pesan hasil.
- Auto-update senyap 12 detik setelah start **dihapus**; penggantinya `UpdateCheckWorker` harian yang
  memperbarui yt-dlp **dan** memeriksa rilis aplikasi. Kartu "Perbarui yt-dlp" lama pindah dari
  Pengaturan ke layar Komponen (satu tempat untuk semua pembaruan).
- `AppUpdateRepository.check()` → `GitHubReleasesApi.latest()`; bandingkan `Version.parse(tag)` dengan
  versi terpasang (semantik numerik, bukan string).
- `UpdateCheckWorker` (WorkManager, unik harian, constraint CONNECTED): cek rilis + update yt-dlp,
  simpan di `UpdatePrefs`, kirim `UpdateNotifier` sekali per versi (dedupe lewat `UpdatePrefs`).
- Banner/notifikasi punya aksi **"Nanti"** yang mengisi `UpdatePrefs.ignoredVersion`; notifikasi dan
  banner bungkam untuk versi itu, dan otomatis muncul lagi saat ada versi yang lebih baru.
- `ComponentsScreen`: baris per komponen (nama, versi, keterangan, aksi), tombol "Periksa pembaruan";
  baris aplikasi menampilkan "vX tersedia" + tombol "Buka rilis" (browser ke URL rilis GitHub);
  banner di `SettingsScreen` atas bila ada versi baru + tombol "Lihat".
- Label tegas di UI: "hanya yt-dlp yang bisa diperbarui tanpa memasang APK baru".

## 10. Penyimpanan

Tabel DB tidak berubah (`downloads`, `cookies`, versi 1) → data pengguna 0.3.0 aman. Preferensi
cookie lama (`settings/use_cookies`) dibaca sekali dan dipindah ke `UserPrefs` (kunci sama, file
prefs baru), tanpa kehilangan pilihan pengguna.

## 11. Pengujian

Unit test murni (JVM) dipindah ke modul masing-masing:
`:core:common` — `UrlValidatorTest`, `TimeCodeTest`, `RangeTest`, `FileNamesTest`, `RetryPolicyTest`,
`ProgressLineTest`, `NetscapeFileTest`, `VersionTest` (baru), `AuthCookieTest` (baru).
`:core:database`/`:core:data` — `DownloadStatusTest`, `HistoryRepoLogicTest`, `CookieCheckTest`,
`CookieGateTest`, `WebViewCookieReaderTest`, `PoTokenArgsTest`, `DownloadArgsTest`, `NewPipeMapperTest`,
`InfoCacheTest`, `MetadataParserTest`, `ErrorMapperTest`, `FormatMapperTest`, `CancelRulesTest`,
`UpdateDedupTest` (baru: dedupe notifikasi versi).
`:feature:*` tidak punya test UI (tidak ada device); diverifikasi build + review.

## 12. Migrasi (urutan aman)

1. Skeleton 14 modul + `settings.gradle.kts` + version catalog; `:app` masih berisi kode lama.
2. Pindah `core:model`/`core:common` + test; build hijau.
3. `core:database`, `core:datastore`, `core:network`, `core:notifications`; build hijau.
4. `core:designsystem` (token + komponen) dengan satu pemakai contoh.
5. `core:data` (pindah seluruh data/work), routing ulang `:app`.
6. Pecah UI ke `feature:*`, terapkan design system + `ScreenScaffold` + bottom bar custom.
7. Update center + worker + notifikasi + layar Komponen/About.
8. Hapus sisa kode lama, naikkan versi 0.4.0, build rilis + verifikasi + rilis GitHub.

Setiap langkah: `testDebugUnitTest` hijau dan `assembleRelease` sukses sebelum lanjut.

## 13. Risiko

- 14 modul menambah waktu konfigurasi Gradle (~30–60 detik) — diterima.
- R8: aturan keep tetap di `:app`; `consumer-rules.pro` hanya bila modul butuh.
- Pemisahan `core:data` besar; batas dijaga lewat arah dependensi, bukan jumlah file.
- Cek rilis GitHub memakai API tanpa token (60 req/jam); 1×/hari aman.
- WorkManager initialization tetap otomatis (androidx.startup via `core:data`); foreground service
  type `dataSync` tetap dideklarasikan.