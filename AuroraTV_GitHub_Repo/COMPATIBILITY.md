# Aurora TV V2 compatibility checklist

## Minimum app installation level

API 14 / Android 4.0. The APK contains only Java platform UI/video components; it does not rely on Play Services, AndroidX or Android TV Leanback UI libraries. Install on Android TV/Google TV, Amazon Fire TV Stick and mobile devices. **Minimum API is an installation requirement, not a stream-format guarantee.**

## Manual test matrix (not yet run)

- Android 4.0 API14 emulator/physical device: install; first launch; touch/DPAD focus; HTTPS default playlist (expect potential TLS failure); local M3U import; H.264 MP4 stream
- Android 4.4 API19: HLS playback using live accessible H.264/AAC feed; orientation changes; background/resume
- Fire TV Stick 2nd Gen (Fire OS 5, Android 5.1): app appears in Apps; remote focus; on-screen buttons; channel up/down; HLS
- Fire TV Stick 3rd Gen (Fire OS 7, Android 9): as above
- Android TV / Google TV 9+: leanback launcher tile; DPAD category selection and channel playback
- Android mobile 8, 13, 15+: portrait browse, keyboard search, long-press favorite, video pause/resume, landscape rotation
- Network failure: turn off network on first launch and retry; verify error message
- Playlist failure: enter invalid URL, verify old cache remains available
- Playlist size limit: verify oversized import rejected
- Format limitations: `.mpd` shows unsupported message; DRM and token-protected feeds cannot be unlocked

## Streaming capabilities

API 14 supports baseline MediaPlayer but formats supported depend on OEM firmware, decoders and HTTPS stack. HLS arrived in Android 3.x but actual compatibility is inconsistent; MPEG-DASH needs a modern streaming engine and is intentionally unsupported in V2's single min-14 APK. Native playback and channel reachability must be manually tested per device.
