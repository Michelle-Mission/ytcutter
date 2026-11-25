## YT Cutter 1.0.0

Rilis pertama, dipakai harian untuk memotong dan mengunduh video YouTube.

### Potong & unduh
- Pilih rentang waktu lalu unduh hanya bagian itu; tidak ada unduhan sia-sia.
- Unduhan langsung ke galeri dengan nama file yang rapi.
- Pilihan tinggi video (hingga sesuai sumber) dan audio saja.
- Cookie YouTube untuk video yang butuh login, plus poToken untuk video terbatas.
- Unduhan tahan gangguan: multi-koneksi aria2c, chunk 10 MB, retry berlapis, dan otomatis pindah ke format progresif bila format pilihan ditolak.

### Status ala yt-dlp
- Tahapan jelas: mengambil tautan, menyiapkan format, memotong, menggabungkan, mengekstrak audio, menulis metadata, menyimpan ke galeri.
- Progres memakai byte terunduh, estimasi total, lalu jumlah fragmen — bukan lagi bar yang macet.

### Pembaruan
- Pembaruan delta: hanya mengunduh selisih APK (ratusan KB), APK penuh sebagai cadangan.
- yt-dlp diperbarui dari dalam aplikasi, langsung dari rilis resmi, dengan verifikasi SHA-256 dan uji jalan sebelum dipakai.
- FFmpeg, Python, dan aria2c menyatu di APK; tidak perlu memasang paket tambahan.

### Lain-lain
- Ringan di perangkat kecil: jumlah koneksi, animasi, dan timeout menyesuaikan perangkat.
- Info video dimuat lebih awal saat tautan ditempel, jadi nyaris instan.
- Changelog, status komponen, dan pemeriksaan pembaruan di dalam aplikasi.
- Pengumuman/maintenance/feature flag bisa diatur dari GitHub Pages.
