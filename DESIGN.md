# DESIGN.md — YT Cutter

Satu bahasa visual untuk semua layar. Modul acuan: `:core:designsystem`.

## Mode
Operate. Tujuan tiap layar: menyelesaikan tugas cepat dan jelas. Konsistensi dan keterbacaan mengalahkan ekspresi.

## Kerangka layar

| Jenis layar | Komponen | Contoh |
|---|---|---|
| Semua halaman | `ScreenScaffold` + TopAppBar: judul `titleLarge` bold, posisi sama | Beranda, Unduhan, Pengaturan, Cookie, Komponen, Tentang |
| Lembar bawah | Judul `titleMedium` | Pilih format |
| Status boot | Judul `titleLarge` bold | Menyiapkan mesin |

Padding layar selalu 20dp kiri/kanan, 4dp atas (di bawah TopAppBar), 24dp bawah. TopAppBar menangani area status bar; tidak ada judul tambahan di dalam konten.

## Skala tipe

| Peran | Style | Ukuran |
|---|---|---|
| Judul halaman (semua) | `titleLarge` | 22sp bold |
| Judul menonjol (video, sheet) | `titleMedium` | 18sp semibold |
| Judul kartu/baris | `titleSmall` | 15sp semibold |
| Isi | `bodyMedium` | 15sp |
| Meta/sekunder | `bodySmall` | 13sp |
| Tombol | `labelLarge` | 14sp semibold |
| Pil/label kecil | `labelSmall` | 12sp medium |

Aturan: `titleMedium` hanya untuk judul video/lembar; semua judul kartu dan baris memakai `titleSmall`. Teks sekunder selalu `bodySmall` + `onSurfaceVariant`.

## Jarak (`Spacing`)

`xs 4 · sm 8 · md 12 · lg 16 · xl 20 · xxl 24 · card 18`

- Isi kartu: 18dp (`AppCard`).
- Baris daftar (`SettingsRow`): 16dp horizontal, 12dp vertikal.
- Jarak antar kartu daftar: 10dp.
- Antar bagian di layar tab: 18–20dp; header bagian ke isi: 8dp (`SectionHeader`).
- Teks meta ke judul: 3–4dp.

## Komponen wajib

- `ScreenScaffold`, `AppCard`, `SectionHeader`, `StatusPill`, `SettingsRow`, `UpdateBanner`, `UpdateCard`, `CookieStatusLine`.
- Kartu tidak bersarang. Satu `AppCard` per entri.
- Tombol memakai `MaterialTheme.shapes.small` untuk aksi dalam kartu, `medium` untuk aksi utama.

## Warna & permukaan

- Aksen `primary` (merah), teks sekunder `onSurfaceVariant`, peringatan `error`/`errorContainer`.
- `StatusPill`: sukses `primaryContainer`, gagal `errorContainer`, teks `labelSmall`.
- Tanpa gradien teks, bayangan keras, atau border kiri berwarna.

## Gerak

Satu transisi masuk per perubahan state (`AnimatedContent`, 220ms), tanpa animasi berulang di luar indikator progres.
