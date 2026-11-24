# Arsitektur YT Cutter

## Prinsip
- APK dari GitHub Releases; data publik (config, changelog, manifest update, patch delta) dari GitHub Pages.
- Tidak ada rahasia di repo: config hanya data publik. Jangan taruh token/password/data pengguna.
- Semua kegagalan jaringan bersifat fail-open, kecuali `maintenance` dan `minVersionCode`.

## Alur
1. CI (tag `v*`/`build-*`) build + test + sign APK, lalu unggah **hanya APK** ke rilis.
2. CI menulis `docs/update.json` dan `docs/deltas/latest.patch`, commit ke `main` dengan `[skip ci]`.
3. Aplikasi membaca:
   - `https://nurfilardi.github.io/ytcutter/config.json` (maintenance, pengumuman, feature flag, min version)
   - `https://nurfilardi.github.io/ytcutter/changelog.json`
   - `https://nurfilardi.github.io/ytcutter/update.json` (+ `deltas/latest.patch`)
4. Update in-app: bandingkan `versionCode`; pakai patch bila `fromVersionCode` cocok, selain itu APK penuh; verifikasi sha256 + tanda tangan sebelum `PackageInstaller`.
5. Cache disk di `cacheDir/remote` sebagai cadangan saat offline; status ditampilkan di Komponen.

## Feature flag
`features.downloader` dan `features.player`; flag false menyembunyikan/menonaktifkan bagian terkait tanpa build ulang.

## Maintenance & versi minimum
`maintenance.enabled=true` menampilkan layar maintenance. `minVersionCode` lebih besar dari versi terpasang memaksa update.

## Keterbatasan
- Halaman rilis selalu menampilkan source code ZIP bawaan GitHub; aset yang kita unggah hanya APK.
- Play Protect tetap bisa memperingatkan karena aplikasi dipasang di luar Play.
