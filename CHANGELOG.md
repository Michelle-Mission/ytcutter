# Changelog

## 1.0.0

Rilis pertama.

### Potong & unduh
- Potong dan unduh hanya rentang waktu yang dipilih.
- Simpan langsung ke galeri dengan nama file rapi.
- Pilihan tinggi video dan audio saja.
- Cookie YouTube untuk video yang butuh login, plus poToken.
- Unduhan tahan gangguan: aria2c multi-koneksi, chunk 10 MB, retry berlapis, fallback format progresif.

### Status ala yt-dlp
- Tahapan unduhan jelas (mengambil tautan, memotong, menggabungkan, menyimpan).
- Progres dari byte/estimasi/fragmen, tidak macet.

### Pembaruan
- Pembaruan delta: unduhan kecil, APK penuh sebagai cadangan.
- yt-dlp diperbarui dari dalam aplikasi (rilis resmi, verifikasi SHA-256, uji jalan).
- FFmpeg, Python, dan aria2c menyatu di APK.

### Lain-lain
- Menyesuaikan performa untuk perangkat kecil.
- Info video dimuat lebih awal saat tautan ditempel.
- Changelog, status komponen, pemeriksaan pembaruan di dalam aplikasi.
- Pengumuman, maintenance, dan feature flag dari GitHub Pages.
