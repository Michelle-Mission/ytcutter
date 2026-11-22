# YT Cutter — Design

Tanggal: 2026-11-22
Status: disetujui

## Intent

Aplikasi Android pribadi untuk mengambil **potongan** video YouTube tanpa mengunduh
seluruh video: tempel URL → pilih resolusi → tandai detik awal–akhir → file terpotong
masuk galeri.

## Goals

1. Tempel/ambil URL, muat metadata (judul, durasi, thumbnail, daftar format asli).
2. Pilih resolusi dari format yang benar-benar tersedia (2160/1440/1080/720/480/360)
   atau audio-only.
3. Rentang waktu: dual slider + isian `mm:ss`; validasi `end > start + 1`, clamp ke durasi.
4. Progress nyata + cancel; hasil dipublikasikan ke `Movies/YT Cutter` via MediaStore.
5. Tahan challenge YouTube: cookies login + rantai fallback client; error dipetakan
   menjadi instruksi yang bisa ditindaklanjuti.

## Non-goals (MVP)

Playlist/batch, SponsorBlock, subtitle, preview player, riwayat persisten (DB),
iOS/desktop.

## Anti-goal

App yang berhenti bekerja saat YouTube berubah. Mitigasi: **self-update yt-dlp**
(STABLE/NIGHTLY) tersedia di Settings.

## Constraints

- GPL-3.0 (ikut `youtubedl-android`).
- APK besar: Python 3.8 + yt-dlp + ffmpeg. ABI arm64-v8a saja; ABI split aktif.
- Scoped storage: tulis hanya via MediaStore.
- minSdk 29, targetSdk/compileSdk 36. MediaStore `IS_PENDING`/`RELATIVE_PATH` butuh API 29.
- Tanpa JS runtime eksternal; yt-dlp memakai client `visionos/android_vr/tv` fallback.
- Mesin build tidak punya perangkat/emulator → verifikasi runtime di perangkat manual.

## Arsitektur

Satu modul Gradle, Compose M3, tanpa DI framework, tanpa DB, tanpa Retrofit.

| Bagian | Tanggung jawab |
|---|---|
| `core/TimeCode` | parse/format `mm:ss` dan `h:mm:ss` |
| `core/FormatMapper` | daftar kualitas + selector `-f` |
| `data/MetadataParser` | `yt-dlp -J` JSON → `VideoInfo` (pakai `org.json`) |
| `data/YtDlpRepo` | init, versi, update, `json()`, `download()`; satu `Mutex` (proses serial) |
| `data/DownloadArgs` | bangun argv yt-dlp (format, section, cookies) |
| `data/MediaStorePublisher` | temp file → MediaStore `Movies/YT Cutter` |
| `data/ErrorMapper` | string error yt-dlp → pesan + aksi |
| `data/CookieStore` | import `cookies.txt` via SAF |
| `service/DownloadService` | foreground `dataSync`, progress, cancel |
| `data/Downloads` | `StateFlow<DownloadState>` untuk UI |

Unduhan: temp di `cacheDir` → publish MediaStore → hapus temp di `finally`.

### Captcha

MVP: cookies hasil login (import `cookies.txt`) + fallback `player_client`.
Deteksi challenge → UI menyuruh import cookies. **Bukan** solver CAPTCHA.
PO token otomatis (BGUtils/BotGuard via WebView) = fase 2 opsional.

## UX

```
┌ YT Cutter ────────────┐
│ [ https://youtu.be/... ]│  ← tombol Tempel
│  thumbnail 16:9       │
│  Judul video · 12:41  │
│  (2160) (1080•) (720) │
│  (480) (360) (Audio)  │
│  ├────●──────────●──┤ │  dual slider
│   00:15      03:40    │  isian mm:ss
│  [ Ambil 3:25 · 1080p]│
└───────────────────────┘
```

Tab: Home, Downloads. Settings: cookies, update yt-dlp, versi.

## Risiko

| Risiko | Mitigasi |
|---|---|
| yt-dlp/Python 3.8 tertinggal | self-update dari Settings |
| YouTube ubah auth | cookies jalur utama, fallback client |
| APK besar | ABI split, arm64 only |
| Tidak bisa uji di perangkat di sini | unit test JVM + checklist manual |