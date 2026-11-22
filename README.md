# YT Cutter

Downloader potongan video YouTube untuk Android. Tempel URL, pilih resolusi dan
rentang detik, hasilnya masuk galeri. Mesin: `yt-dlp` + `ffmpeg` yang dibundel
(`youtubedl-android`), jadi tidak butuh server.

## Fitur

- Tempel/ambil URL, ambil metadata (judul, durasi, thumbnail).
- Pilih resolusi dari format asli video (2160/1440/1080/720/480/360) atau audio-only (m4a).
- Tandai detik awal–akhir (dual slider + isian `mm:ss`); hasil langsung terpotong.
- Progress nyata, batal, buka/bagikan hasil.
- Unduhan lanjut saat app di background (foreground service).
- Import `cookies.txt` untuk video private/age-restricted/verifikasi.
- Update `yt-dlp` dari Pengaturan tanpa memasang APK baru.

## Build

Butuh JDK 17+ dan Android SDK 36.

```bash
export ANDROID_HOME=/opt/android-sdk
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
```

Android 10 (API 29) ke atas, ABI `arm64-v8a` (perangkat 64-bit modern). Debug APK ~70 MB karena membawa
Python + yt-dlp + ffmpeg.

Pasang:

```bash
adb install -r app/build/outputs/apk/release/app-arm64-v8a-release.apk
```

## Captcha / "Sign in to confirm you're not a bot"

App ini **tidak** memecahkan CAPTCHA dan tidak memakai layanan solver. Yang dipakai:

1. **Cookies login.** Ekspor `cookies.txt` dari browser (ekstensi "Get cookies.txt"),
   lalu Pengaturan → Impor cookies.txt. Ini menyelesaikan hampir semua challenge,
   video private, dan age-gate.
2. **Fallback client yt-dlp** (`visionos,android_vr,tv`) dan update `yt-dlp` terbaru.

Belum ada pembuatan PO token otomatis (BGUtils/BotGuard via WebView) — direncanakan
fase 2, dan bersifat rapuh karena mengikuti perubahan YouTube.

Catatan: mengunduh konten YouTube dibatasi Terms of Service YouTube; cookies memberi
akses ke akunmu, jadi simpan hanya di perangkat sendiri dan jangan bagikan.

## Checklist uji di perangkat

1. URL bukan YouTube (`https://vimeo.com/...`) → pesan "Bukan URL YouTube", tidak crash.
2. URL playlist/`watch?v=...&list=...` → hanya video tunggal yang diproses.
3. Rentang: geser akhir ke durasi penuh; isi `99:99` → kembali ke nilai valid.
4. Judul beremoji/`%`/`/` → file tersimpan dengan nama aman.
5. Batal saat progress berjalan → tidak ada file di galeri, cache bersih.
6. Kunci layar saat unduh → progress lanjut di notifikasi.

## Batasan

- Tidak ada playlist/batch, subtitle, SponsorBlock, preview player, riwayat persisten.
- Tidak ada verifikasi runtime otomatis di mesin build (tidak ada perangkat/emulator);
  unit test JVM mencakup parsing waktu, mapping format, metadata, argumen yt-dlp,
  nama file, dan pesan error.
- APK debug tidak di-minify; rilis perlu signing sendiri.

## Lisensi

GPL-3.0, karena `youtubedl-android` berlisensi GPL-3.0.