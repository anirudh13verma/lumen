# Lumen

An open-source Android media library app: point it at folders on your device and it organises your movies and shows into a clean, poster-driven library. Think Nova Video Player's job with a modern interface, and a theme you can actually make your own.

> **Status: early development.** The app shell, theming and local folder scanning work. Metadata, posters and playback are not built yet.

## Goals

- **Local only, for now.** Scans folders you choose on the device. No server required.
- **Modern UI.** Poster grids, Continue Watching, rich detail screens (Jetpack Compose + Material 3).
- **Your colours.** Pick one accent colour and the whole app re-themes: light, dark, pure-black AMOLED, or Material You system colours.
- **Open source.**

## Roadmap

1. [x] App shell, navigation, seed-colour theming
2. [x] Folder picking, incremental background scanning, Room database
3. [ ] Filename parser and TMDB metadata matching
4. [ ] Home, library, movie and show detail screens with real data
5. [ ] Player (libmpv or Media3 behind a small `Player` interface)
6. [ ] "Fix match" screen, polish, animations

## Building

Requirements: JDK 17+ and the Android SDK (Android Studio sets both up).

```sh
./gradlew assembleDebug      # build a debug APK
./gradlew testDebugUnitTest  # run unit tests
```

Or open the folder in Android Studio and run the `app` configuration.

## Project layout

```
app/src/main/java/app/lumen/
  data/settings/   DataStore-backed user settings (theme)
  data/db/         Room database: folders and scanned video files
  data/scan/       SAF folder walker, WorkManager scan job, video-file rules
  data/            LibraryRepository (add/remove folders, incremental sync)
  ui/theme/        Seed-colour -> Material 3 colour scheme
  ui/navigation/   Bottom-bar destinations
  ui/home|library|settings/   Screens and view models
  ui/components/   Shared composables
```

## Metadata and API keys (planned)

Posters and metadata will come from [TMDB](https://www.themoviedb.org/). Each developer will supply their own free API key in an untracked `local.properties` / `secrets.properties` file; keys must never be committed. TMDB requires attribution, which will be shown in the app's About screen.

> This product uses the TMDB API but is not endorsed or certified by TMDB.

## Contributing

Issues and pull requests are welcome once the project settles. Until a license file is added, all rights are reserved by the author. See below.

## License

Not yet chosen. A license will be added before the first public release.
