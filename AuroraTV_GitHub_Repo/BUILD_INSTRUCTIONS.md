# Build Aurora TV V2 APK

**Need:** Android Studio with Android SDK Platform 35, Java JDK 17, and Gradle 8.9 (or ability to generate a Gradle wrapper).

## Android Studio (recommended)

1. Extract `AuroraTV_V2_Source.zip`.
2. Open Android Studio -> **Open** -> the extracted `AuroraTV_V2` folder.
3. Configure **Gradle JVM = JDK 17** and **Gradle = 8.9** (local Gradle or generate standard Gradle wrapper JAR using `gradle wrapper --gradle-version 8.9`).
4. Let it sync Android Gradle Plugin 8.7.3 and Android SDK Platform 35.
5. Choose **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
6. Install the debug APK at `app/build/outputs/apk/debug/app-debug.apk` using `adb install -r app-debug.apk`, or Android Studio Run. Confirm installation if the device requires approval.

## Target devices

- **Android phone/tablet**: transfer the APK to the device and allow installation by your chosen installer.
- **Android TV/Google TV**: install with Android Debug Bridge or a file manager; find Aurora TV under TV Apps.
- **Amazon Fire TV Stick**: enable developer installation options as allowed by your firmware, sideload with ADB or an authorized installer, then open Aurora TV from Apps.

The APK generated for API 14+ installs on older Android versions subject to OEM restrictions. **Playback remains stream- and device-dependent.** No APK is included in this source archive because the build toolchain is not available in this environment.

## On first launch

Aurora TV automatically downloads `https://iptv-org.github.io/iptv/index.m3u`, stores the parsed catalog, and displays channel groups. This requires an internet connection and a compatible TLS stack. Tap/press a channel to watch. Long-press a tile to favorite. Use **Refresh** to fetch the current public list, **Public TV** to switch back to IPTV-org, or **Playlist URL** / **Import M3U** to use another playlist.
