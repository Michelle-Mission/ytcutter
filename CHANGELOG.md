# Changelog

Semua perubahan kecil tetap di bawah versi `1.0.0`. Nomor versi hanya naik untuk rilis besar.

## 1.0.0

### UI
- Satu sistem desain untuk semua layar lewat `ScreenScaffold` + `TopAppBar`: judul `titleLarge` bold 22sp, padding konten 20dp samping, 4dp atas, 24dp bawah.
- `ScreenHeader` dihapus; kartu (`AppCard`, padding 18dp), baris (`SettingsRow`, 16/12dp), dan status (`StatusPill`) kini seragam.
- Skala tipografi dibesarkan: titleMedium 18, titleSmall 15, bodyMedium 15, bodySmall 13, labelLarge 14, labelSmall 12.
- Halaman Komponen & Tentang ditambahkan; banner update dan status cookie memakai komponen bersama.

### Cookie
- Jumlah cookie dibaca dari database setiap `onPageFinished`.
- Login selesai menunggu 800 ms lalu menyimpan cookie + menutup halaman.
- Penyimpanan cookie idempoten (`findIdByContent`).

### Update
- Cek rilis harian via `UpdateCheckWorker` + notifikasi.
- Banner update per halaman dengan tombol buka rilis.

### Arsitektur
- Modularisasi 14 modul (`:app`, `:core:*`, `:feature:*`) dengan DI manual `AppGraph`.
- Semua 27 suite unit test lulus.

## Catatan rilis berikutnya
- Perbaikan kecil tidak mengubah `versionName`; hanya `versionCode` naik agar Android mau memasang update.
- Rencana update delta (unduh patch, bukan APK penuh) menyusul.