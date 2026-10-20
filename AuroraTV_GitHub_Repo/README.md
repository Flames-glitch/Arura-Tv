# Aurora TV V2 — One codebase for Android TV, Fire TV and Android phones

**Built-in catalog URL:** `https://iptv-org.github.io/iptv/index.m3u`

This V2 source-code project uses an Apple TV-*inspired* dark interface, and is **not** an Apple tvOS app. It is a native Android application for Android TV, Google TV, Amazon Fire TV/Fire Stick and touchscreen Android phones/tablets.

## What works in the source project

- **Android 4.0 / API 14 minimum**, compiled against API 35; no Media3, AppCompat, Leanback, Google Play Services or AndroidX dependencies
- Same APK/application module targets **Android mobile launcher + Android TV launcher**; Fire OS can launch it from Apps
- Smartphone portrait layout, landscape TV layout, touch targets and DPAD navigation
- Auto-load IPTV-org's public community playlist on first launch, caching channel metadata for subsequent launches
- Refresh stored playlist; switch back to built-in public playlist; optional custom M3U URL and file import
- Filter by category, search channel names, long-press tile for favorite, or favorite via player button
- Video playback via built-in Android `VideoView`/`MediaPlayer`; on-screen controls suitable for touch and remotes, plus remote Up/Down/Channel-Up/Down to switch channels
- Local storage of the last playlist and favorites; no login, no tracking, no server backend
- `android.hardware.touchscreen` and `android.software.leanback` both **not required** for cross-device compatibility

## Device support and important limits

| Class | Install target | Playback reality |
|---|---|---|
| Android phones/tablets (Android 4.0+) | API 14+ source configuration | Old OS has limited codec, HLS and TLS support. Modern devices support more. |
| Android TV / Google TV (Android 4.0+) | API 14+ source configuration | TV remote browsing supported; stream compatibility varies. |
| Fire TV Stick 2nd Gen (Fire OS 5, Android 5.1) | API 22 | App expected to install, stream formats still vary. |
| Fire TV Stick 3rd Gen (Fire OS 7, Android 9) | API 28 | App expected to install, more formats may work. |
| Fire TV Stick 4K family | Varies | Android-based Fire OS models expected to install, subject to testing. |

**This is compatibility-oriented, not a guarantee of all streams playing.** Platform `MediaPlayer` does not provide universal modern codec/manifest support. DASH `.mpd` explicitly not supported by this legacy player, and RTSP/HLS support differs by device and OS. IPTV-org contains third-party stream URLs that can be offline, geo-restricted, incompatible, or changed. Android 4.x may fail HTTPS connections to newer TLS endpoints (including the default playlist) depending on device security stack. Adding a custom server/proxy or user-supplied local playlist is an option where legally permitted; **no proxy is included**. DRM content is unsupported. No channel stream payloads are included in the APK: the live playlist is fetched at runtime.

## Compile to APK

1. Open `AuroraTV_V2` as a project in Android Studio with **JDK 17**, **Android SDK Platform 35** and an internet connection for Gradle Plugin download.
2. Sync dependencies. Root project pins Android Gradle Plugin 8.7.3 and Gradle 8.9; install Gradle locally or generate/download the wrapper JAR if needed (only wrapper properties are bundled).
3. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**. Or with a working Gradle installation run `gradle :app:assembleDebug`.
4. Sideload the resulting `app/build/outputs/apk/debug/app-debug.apk` onto your Android phone, Android TV or Fire TV. You may need to enable app installation from your trusted installer in device settings.
5. Open Aurora TV. On first launch with network connectivity it fetches IPTV-org. If download fails, check the status message, try Refresh, or import another authorized playlist.

**Not yet compiled or hardware-tested:** The source was generated and static-checked without the Android SDK or Gradle CLI, so an actual APK isn't bundled. Test Android 4, Fire OS 5/7, and Android 13+ hardware before claiming certified support. Target SDK is 34 for sideload/prototype use; publishing to Play may require a newer target SDK and compliance work.

## Upgrading the media engine

V2 deliberately uses a **single universal, minimum-API-14 app**. For better HLS/DASH playback on supported modern hardware, a later version can include a separate modern build flavor or dynamic implementation using a maintained player library with a higher minimum SDK, while keeping a legacy APK for Android 4. Do not simply add modern Media3 to the API14 build: its minimum platform requirements are higher and manifest merging would become invalid without unsafe overrides.

## Credits, legality and privacy

IPTV-org is a public, third-party, community-maintained directory: <https://github.com/iptv-org/iptv>. Inclusion in that playlist does not guarantee a broadcaster licenses playback in every location, and IPTV-org is not affiliated with this app. Users should only watch streams they're authorized to access. Aurora TV does not bypass DRM, sign into protected providers or redistribute copyrighted video streams. Stream requests connect from the device to the respective stream hosts; hosts may receive the device IP. The app locally stores the playlist and favorites.

## Known limitations / pending improvements

- No on-device APK verification; no automated UI test coverage yet
- No XMLTV program guide, timeshift, Chromecast, AirPlay, recording or offline viewing
- No logos/CDN caching yet (initials are used to reduce memory/network usage on older devices)
- No special stream headers/token refresh or EPG; no DASH/DRM; no adaptive engine on API14
- Playlist updates replace cached catalog; manual refresh; source and favorites preserved
- TV layout and DPAD use basic platform widgets; Fire OS launch behavior and remote key mapping should be checked on hardware
- For Android 15+ the status/navigation insets and store distribution should be verified and modernized
