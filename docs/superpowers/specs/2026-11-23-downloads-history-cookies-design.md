# YT Cutter 0.3.0 — Riwayat unduhan persisten, progress real-time, cookie terverifikasi

Tanggal: 2026-11-23

## Intent dan alasan

Saat ini `Downloads` hanya object in-memory (`app/src/main/java/app/ytcutter/data/Downloads.kt`).
Akibatnya progress bar tidak halus, dan kalau proses app dibunuh progress + unduhan hilang tanpa jejak.
Pengguna meminta: progress real-time, unduhan bertahan saat app ditutup (lanjut otomatis), riwayat
unduhan tercatat di aplikasi, serta sistem cookie dipastikan bekerja dan teksnya lebih tepat.

Pendekatan yang dipilih pengguna: **C — WorkManager + database lokal**, dengan unduhan
dilanjutkan otomatis setelah proses mati, plus penandaan gagal dan tombol ulangi.

## Tujuan

- Progress bar bergerak real-time (byte/kecepatan/ETA), juga di notifikasi.
- Unduhan berjalan di WorkManager, tidak bergantung lifecycle Activity.
- Riwayat unduhan persisten: judul, kualitas, rentang, status, waktu, hasil.
- Proses app mati di tengah unduhan → work dijadwalkan ulang, unduhan dilanjutkan (`--continue`).
- Status akhir gagal/terputus punya tombol ulangi.
- Cookie diambil lengkap (termasuk HttpOnly) dan diverifikasi login-nya.
- Teks: "Ambil cookie", status "Cookie ada", toast saat cookie terdeteksi.

## Non-tujuan

- Bukan antrean multi-akun cookie, bukan dukungan situs selain YouTube.
- Tidak menghapus file dari galeri saat riwayat dihapus (file hasil tetap milik pengguna).
- Tidak menambah thumbnail riwayat, playlist queue, atau unduhan paralel.
- Tidak mengubah alur info instan NewPipe / poToken yang sudah ada.

## Kendala

- minSdk 29, targetSdk 36, arm64-v8a, R8 aktif, APK universal + `extractNativeLibs=true`.
- Kotlin 2.4.21 — **tidak ada rilis KSP 2.4.x** di Maven Central (diperiksa: metadata KSP berhenti
  di 2.3.14), jadi Room codegen tidak tersedia. **Keputusan builder:** pakai `SQLiteOpenHelper`
  Android (stdlib) dengan SQL eksplisit, bukan Room. Kapabilitas sama (persisten + query), tanpa
  risiko toolchain. KSP/kapt tidak ditambahkan.
- `youtubedl-android` 0.18.1 memberi progress lewat callback, tapi parameter `progress` dibuang dan
  template output tidak diatur; `--newline` sudah ada.
- Repo wajib tetap GPL-3.0; jangan menyalin kode GPL YTDLnis, hanya memakai pemahaman mekanismenya.

## Pendekatan

- **A. Minimal** — tulis riwayat ke satu file JSON, progress via template, tandai terputus; unduhan
  tetap mati saat proses dibunuh. Ditolak: tidak memenuhi "lanjut otomatis".
- **B. Service persisten + JSON** — memperluas `DownloadService`, antrean di file, resume manual.
  Ditolak: tetap rapuh terhadap pembunuhan proses; tidak ada jaminan reschedule.
- **C. WorkManager + DB (dipilih)** — WorkManager menjadwalkan ulang work setelah proses mati,
  database lokal menjadi sumber kebenaran riwayat/antrean. Riwayat dan progress selamat.
- Room vs SQLiteOpenHelper: dipilih SQLiteOpenHelper (lihat Kendala); pencatatan sebagai keputusan
  builder, bukan keputusan pengguna.

## Rancangan

### 1. Data (`app/src/main/java/app/ytcutter/data/history/`)

`HistoryDb : SQLiteOpenHelper`, satu tabel `downloads`:

| kolom | tipe | catatan |
|---|---|---|
| `id` | INTEGER PK AUTOINCREMENT | juga id work `dl-<id>` |
| `video_id` | TEXT | untuk dedupe ringan |
| `url` | TEXT NOT NULL | |
| `title` | TEXT NOT NULL | |
| `quality_label` | TEXT NOT NULL | mis. "720p", "audio" |
| `height` | INTEGER | null → 0 |
| `start_sec`, `end_sec` | INTEGER | 0 = penuh |
| `status` | TEXT NOT NULL | `QUEUED RUNNING DONE FAILED CANCELED INTERRUPTED` |
| `progress` | INTEGER | 0..100 |
| `detail` | TEXT | baris progress terakhir (kecepatan/ETA) |
| `error` | TEXT | pesan terpetakan |
| `error_action` | TEXT | `NONE LOGIN UPDATE_RETRY` |
| `uri` | TEXT | hasil MediaStore |
| `file_name` | TEXT | |
| `created_at`, `updated_at` | INTEGER | epoch ms |

`HistoryRepo` membungkus DAO: `insertQueued`, `running(id)`, `progress(id,pct,detail)`,
`finish(id,status,uri,fileName,error,action)`, `retry(id)`, `delete(id)`, `list(): Flow<List<DownloadRow>>`,
`sweepInterrupted(): List<Long>`, `pending(): List<Long>`.
Flow memakai `MutableStateFlow` yang di-refresh setelah tiap tulis (riwayat kecil; tanpa trigger DB).

### 2. WorkManager

- Dependensi: `androidx.work:work-runtime-ktx:2.10.5` (tanpa KSP).
- Satu `QueueWorker : CoroutineWorker` dengan unique work `download-queue` (KEEP). Worker mengambil
  baris `QUEUED` tertua, mengunduh, menulis hasil, lalu lanjut ke baris berikutnya sampai kosong.
  **DB adalah satu-satunya sumber kebenaran antrean**; WorkManager hanya penjadwal. Ini menghapus
  kemungkinan dua eksekusi untuk satu item (tidak ada work per item, tidak ada chain APPEND).
- Pembatalan per item: tandai baris `CANCELED`; kalau item itu sedang berjalan, panggil
  `YtDlpRepo.cancel(processId)` dan worker berhenti pada item tersebut lalu lanjut.
- Retry otomatis hanya untuk kegagalan jaringan yang terlihat dari `ErrorMapper`/stderr
  (`timed out`, `Connection reset`, `HTTP Error 5xx`, `Unable to download`), maksimal 2x, dengan
  jeda 5 detik; bot-check, format tidak tersedia, dan cookie tidak valid tidak pernah di-retry otomatis.
- Sweep di awal `QueueWorker`: baris `RUNNING` sisa proses lama diubah ke `INTERRUPTED`, lalu
  dijadwalkan ulang sebagai `QUEUED` dan dijalankan dengan `--continue`. Karena hanya ada satu
  unique worker, tidak ada eksekusi ganda; worker idempoten terhadap `status`.
- Foreground: `setForeground(createForegroundInfo(...))` dengan progress notifikasi (kanal lama),
  `setProgress(workDataOf(KEY_PCT, KEY_DETAIL))`.
- Direktori kerja per unduhan: `cacheDir/dl/<id>/download.<ext>` (bukan `cache/out` bersama),
  supaya file parsial tidak saling menimpa dan bisa di-resume.
- Proses mati: WorkManager menjadwalkan ulang unique work. Saat `QueueWorker` mulai, `sweepInterrupted()`
  mengubah `RUNNING` → `INTERRUPTED` lalu antre ulang dengan `--continue`. Kalau resume tidak mungkin
  (yt-dlp memulai ulang), progress kembali dari 0 — diterima dan ditampilkan apa adanya.
- `video_id` hanya disimpan, **tidak** dipakai untuk menolak unduhan; mengunduh ulang video yang sama
  dengan kualitas/rentang berbeda tetap boleh (perilaku sekarang dipertahankan).
- Notifikasi selesai/gagal tetap seperti sekarang, tap membuka tab Unduhan.

### 3. Progress real-time

`DownloadArgs` menambah:

```
--progress-template "download:YTC %(progress.downloaded_bytes)s %(progress.total_bytes_estimate)s %(progress._percent_str)s %(progress._speed_str)s %(progress._eta_str)s"
```

`ProgressLine.parse(line): Progress?` menerima `YTC <down> <total> <pct> <speed> <eta>`:
persen dihitung dari byte bila `total > 0` (lebih akurat untuk `--download-sections`), fallback ke
`pct`, fallback terakhir regex `[download] N%`. Tulis DB + notifikasi maksimal 4x/detik;
selalu kirim update saat status berubah.

### 4. Riwayat UI (`DownloadsScreen`)

- Sumber data `HistoryRepo.list()`; kartu per item, terbaru di atas.
- Item `RUNNING`: progress bar + detail (kecepatan/ETA) + tombol batal.
- Item `DONE`: judul, kualitas + rentang waktu, waktu selesai, aksi Buka/Bagikan/Hapus riwayat.
- Item `FAILED`/`INTERRUPTED`/`CANCELED`: pesan error petakan + tombol **Ulangi**. Aksi mengikuti
  `ErrorMapper`: `LOGIN` → buka CookieLoginActivity lalu ulangi; `UPDATE_YTDLP` → perbarui yt-dlp
  lalu ulangi; `CHOOSE_FORMAT` (format tidak tersedia) → buka FormatSheet, bukan ulangi buta;
  `RETRY` → ulangi dengan argumen sama. Enum DB `error_action` diselaraskan dengan aksi ini.
- "Hapus riwayat" hanya menghapus baris DB (file di galeri tidak disentuh); teks tombol menjelaskan.
- Empty state: teks lama dipertahankan.
- Tab Unduhan tidak lagi memakai `DownloadState` in-memory; `DownloadState` dipertahankan hanya
  untuk kompatibilitas kartu di layar Home (dipetakan dari baris berjalan) atau dihapus bila tidak
  terpakai — builder memutuskan setelah melihat pemakaian.

### 5. Cookie — perbaikan berdasarkan cara kerja YTDLnis

Temuan dari `deniscerri/ytdlnis` (dibaca lewat GitHub MCP):

- `CookieViewModel.getCookiesFromDB` membuka **database cookie WebView** langsung
  (`app_webview/**/Cookies`, atau store incognito), query `host_key LIKE %youtube.com`, lalu menulis
  Netscape dengan `host_key`, `path`, `is_secure`, `expires_utc` asli. Ini menangkap cookie
  **HttpOnly** (SID/HSID/SSID) yang tidak dikembalikan `CookieManager.getCookie`.
- `WebViewActivity` mengarahkan WebView ke `youtube.com/robots.txt` sebelum ekspor, `flush()` cookie,
  dan bila hasil kosong menampilkan toast "no cookies found" tanpa menutup halaman.

Rancangan untuk YT Cutter:

- `WebViewCookieReader.export(context): String?` — cari `Cookies` di bawah `dataDir/app_webview`,
  salin ke cache (aman terhadap lock/WAL), buka read-only, ambil host `%youtube.com` dan
  `%google.com`, tulis Netscape memakai path/secure/expiry asli. Kalau gagal/kosong, fallback ke
  penangkapan header request yang sudah ada (tetap menyimpan HttpOnly).
- `CookieLoginActivity`: judul "Ambil cookie", subjudul "Login, lalu tekan Selesai". Halaman awal
  `https://m.youtube.com/`; setelah `onPageFinished`, ekspor DB dibaca **hanya untuk status hidup**
  ("Cookie ada (N)" / "Belum ada cookie — login dulu") tanpa menyimpan. Penyimpanan hanya saat
  Selesai/back: `flush()` → ekspor → simpan; bila kosong, tampilkan toast "Belum ada cookie ditemukan"
  dan tetap di halaman. Bila tersimpan, `Toast` "Cookie tersimpan (N cookie)" + hasil probe.
- `CookieCheck.probe(context)`: GET `https://www.youtube.com/feed/subscriptions` dengan header
  `Cookie` dan `followRedirects=true` (maks. 3 lompatan); URL akhir di host `accounts.google.com`
  → `NotLoggedIn`; host `youtube.com` → `LoggedIn`; lainnya/exception/5xx → `Unknown`. Dipakai otomatis
  setelah simpan dan oleh tombol **Cek cookie** di Settings; hasil jadi toast.
- Teks Settings: tombol "Ambil cookie"; baris status "Cookie ada — login YouTube aktif" /
  "Cookie ada, tapi belum login — coba ambil ulang" / "Belum ada cookie." Dihitung dari
  `CookieStore` + hasil probe terakhir (disimpan di SharedPreferences kecil, bukan cookie file).

### 6. Versi

`versionCode 14`, `versionName 0.3.0`; footer Settings ikut.

## Pengujian

Tes unit murni (JUnit, tanpa device):
- `ProgressLineTest` — byte-based persen, `N/A`, fallback `[download]`, baris sampah.
- `CookieExportTest` — domain/path/secure/expiry di Netscape, host tanpa titik jadi `.host`.
- `CookieCheckTest` — `classify(finalUrl, httpCode)` untuk host youtube/accounts/consent/unknown.
- `HistoryRowMappingTest` — baris DB → model UI, termasuk status tak dikenal.
DB/Worker tidak diuji di CI (tanpa emulator); diverifikasi lewat build + review.

## Yang diserahkan ke builder

- Tata letak kartu riwayat dan pemetaan `DownloadState` sisa.
- Nilai throttle progress dan jumlah retry WorkManager.
- Nama kelas/file akhir dan pembagian modul.