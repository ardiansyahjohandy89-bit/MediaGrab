# MediaGrab

Aplikasi Android (Kotlin + Jetpack Compose) untuk mengunduh video, foto, dan audio dari banyak situs,
memakai engine yt-dlp + ffmpeg yang ikut di dalam APK ([youtubedl-android](https://github.com/yausername/youtubedl-android)).

## Fitur

| # | Fitur | Di mana |
|---|-------|---------|
| 1 | Unduh video live & siaran (TikTok, YouTube, dll.). Tombol **Stop** menyimpan bagian yang sudah terekam | Tab Riwayat → kartu unduhan |
| 2 | Unduh playlist & album foto | Tempel link playlist → pilih item |
| 3 | Preview video / foto / audio sebelum diunduh | Ikon ▶ di tiap item |
| 4 | Riwayat unduhan | Tab Riwayat |
| 5 | Putar / buka langsung file hasil unduhan (pemutar internal atau aplikasi lain) | Ketuk item riwayat |
| 6 | Ganti tema: sistem/terang/gelap, warna dinamis, 6 warna aksen | Tab Pengaturan |
| 7 | Atur jumlah unduhan: parameter (`5`, `3-7`, `1,4,9`) atau ketuk item langsung | Kartu playlist |
| 8 | Build lewat GitHub Actions | `.github/workflows/build.yml` |

Tambahan: terima link lewat menu **Bagikan** aplikasi lain, unduhan berjalan di latar belakang (notifikasi),
hasil disimpan ke `Movies/`, `Music/`, `Pictures/` folder **MediaGrab**.

## Parameter jumlah

| Isi | Arti |
|-----|------|
| `5` | 5 item pertama |
| `3-7` | nomor 3 sampai 7 |
| `1,4,9` | hanya nomor 1, 4, dan 9 |
| `1-3,8,10-12` | gabungan |
| `5-5` | hanya nomor 5 |
| `semua` | semua item |

## Build lewat GitHub Actions

1. Buat repo baru di GitHub, lalu unggah seluruh isi folder ini (termasuk `.github`).
2. Buka tab **Actions** → workflow **Build APK** berjalan otomatis (atau jalankan manual lewat *Run workflow*).
3. Setelah selesai, unduh APK dari bagian **Artifacts** (`MediaGrab-debug`). Pilih
   `app-arm64-v8a-debug.apk` untuk hampir semua HP modern, atau `app-universal-debug.apk` bila ragu.
4. Untuk membuat GitHub Release otomatis: `git tag v1.0.0 && git push origin v1.0.0`.

Tidak perlu `gradlew`; workflow memasang Gradle 8.9 sendiri.

## Build lokal (opsional)

Butuh JDK 17 + Android SDK (platform 35).

```
gradle :app:assembleDebug
```

Hasil: `app/build/outputs/apk/debug/`.

## Catatan penting

- **Engine perlu diperbarui.** Situs sering mengubah sistemnya. Bila unduhan tiba-tiba gagal, buka
  Pengaturan → *Perbarui yt-dlp*, atau naikkan `youtubedlAndroid` di `app/build.gradle.kts` lalu build ulang.
- Tidak semua situs/konten bisa diunduh (konten privat, butuh login, atau diproteksi DRM tidak didukung).
  Dukungan foto/album bergantung pada situs; yt-dlp lebih kuat untuk video dan audio.
- Ukuran APK besar (puluhan MB) karena membawa Python + yt-dlp + ffmpeg. Itu sebabnya dibuat per arsitektur.
- Gunakan hanya untuk konten milikmu atau yang diizinkan pemiliknya, dan patuhi ketentuan layanan situs.
  Aplikasi seperti ini umumnya tidak diterima di Google Play; pasang lewat APK.

## Struktur

```
app/src/main/java/com/mediagrab/app/
  App.kt, MainActivity.kt, MainViewModel.kt
  data/      Models, Store (pengaturan + riwayat), Extractor (baca link/preview), Engine, Fmt
  download/  Downloads (antrian + simpan ke galeri), DownloadService (notifikasi latar belakang)
  ui/        Theme, HomeScreen, HistoryScreen, SettingsScreen, PlayerScreen
```
