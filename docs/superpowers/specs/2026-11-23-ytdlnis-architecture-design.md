# YT Cutter 0.2.0 — arsitektur ala YTDLnis (NewPipe info + yt-dlp unduh)

Tanggal: 2026-11-23
Status: disetujui (cakupan A)

## Intent

Info video YouTube harus muncul hampir instan seperti YTDLnis, tanpa mengorbankan
kecepatan dan keandalan unduhan yang sudah ada. YTDLnis memakai NewPipeExtractor
untuk info YouTube dan yt-dlp hanya untuk unduh (lihat deniscerri/ytdlnis issue #142).

## Goals

- Info YouTube <1 detik pada jaringan normal (setelah mesin siap).
- Unduh tetap secepat versi 0.1.11 (potong tanpa re-encode, 8 fragment).
- Video non-YouTube tetap berfungsi lewat yt-dlp.
- Tidak ada jalan buntu: kalau NewPipe menolak, fallback otomatis ke yt-dlp.

## Non-goals

- Playlist, pencarian, antrean multi-unduh (tahap B, terpisah).
- Daftar format manual dan pengaturan per unduhan.
- Mengganti yt-dlp sebagai pengunduh.

## Anti-goals

- Menambah waktu tunggu baru di UI (tidak boleh ada blokir saat info tampil).
- Menambah dependensi yang memaksa perubahan besar pada layar yang sudah ada.

## Constraints

- NewPipeExtractor v0.27.1 berlisensi GPL-3.0. Menambahkannya membuat seluruh app
  GPL-3.0. Repo `ytcutter` dipublikasikan dan `LICENSE` ditambahkan, sesuai
  persetujuan pemilik (2026-11-23).
- minSdk 29, Kotlin/Compose, R8 aktif, satu APK arm64-v8a.
- APK harus tetap memakai `libpython.zip.so`/`libffmpeg.zip.so` (ekstraksi manual).

## Pendekatan

Dipilih: **NewPipeExtractor untuk info YouTube + yt-dlp untuk unduh**, meniru
YTDLnis, dengan prefetch JSON yt-dlp di latar agar unduh tidak kehilangan
`--load-info-json`.

Ditolak:
- Rewrite Flutter/React Native: tidak memperbaiki kecepatan (botolnya di proses
  yt-dlp), menghilangkan fitur poToken/cookies yang sudah jalan.
- Tetap yt-dlp untuk info: lambat karena startup Python + ekstraksi YouTube.

## Desain

1. Dependensi baru: `com.github.TeamNewPipe:NewPipeExtractor:v0.27.1` (JitPack),
   okhttp3. R8 keep rule untuk paket `org.schabi.newpipe.**`.
2. `YouTubeInfo.kt` (baru): init NewPipe sekali (Downloader okhttp, user agent
   konsisten), ambil `YoutubeStreamExtractor` dari URL, hasilkan `VideoInfo`
   yang sama bentuknya dengan hasil `MetadataParser` (judul, durasi, heights,
   hasAudio, thumbnail).
3. poToken: implementasi `PoTokenProvider` NewPipe memakai `PoToken.kt`
   (token `gvs` + `player`, visitorData sama). Cookies `CookieStore` dikirim
   sebagai header `Cookie` lewat Downloader NewPipe.
4. Alur Beranda:
   - URL YouTube → `YouTubeInfo.fetch` (UI langsung).
   - Setelah info tampil, `scope.launch { YtDlpRepo.json(url) }` di latar,
     simpan ke `cacheDir/info.json` (dipakai `--load-info-json`). Hasil prefetch
     disimpan juga di memori per URL supaya tidak diulang.
   - URL non-YouTube → `YtDlpRepo.json` seperti sekarang.
   - NewPipe gagal/exception → fallback `YtDlpRepo.json` (pesan error tetap
     informatif).
5. Unduhan tidak berubah (DownloadArgs, DownloadService). Kalau `info.json`
   belum siap saat user menekan unduh, unduh jalan tanpa `--load-info-json`.

## Bagian yang diserahkan ke pembangun

- Bentuk `VideoInfo`/`MetadataParser` saat ini menentukan field yang harus diisi
  NewPipe; pemetaan detail (mis. label resolusi) mengikuti FormatMapper.
- Apakah NewPipe init perlu `NewPipe.init` di Application atau lazy; pilih lazy
  agar boot tetap ringan.
- Penanganan kasus `--load-info-json` kedaluwarsa: kalau unduh gagal dengan
  JSON itu, sudah ada fallback tanpa token; tambahkan retry tanpa
  `--load-info-json` bila perlu.

## Verifikasi

- Unit test: pemetaan `VideoInfo` dari fixtures NewPipe (tanpa jaringan), plus
  test prefetch cache key.
- Build local: `testDebugUnitTest assembleRelease`.
- Uji manual: info YouTube <1 dtk, unduh potongan 1080p, video non-YouTube, dan
  akun dengan cookies (tidak muncul "page needs to be reloaded").