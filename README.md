# Ninja Auto Editor

**Ninja Auto Editor** is a native Android application built with Kotlin and Jetpack Compose for channel **King Ninja55**. It automatically analyzes long gaming videos and exports ready-to-publish vertical **Auto Shorts (9:16, 1080x1920)** and **Auto Long Videos (16:9)** locally on device.

---

## Brand Theme & Aesthetic
- **Primary Color Accent**: Glowing Crimson Red (`#FF0033`)
- **Theme**: Pure Dark Gaming Canvas with high-contrast esports cards (`#0D0D11` & `#181820`)
- **Channel Identity**: King Ninja55
- **Style**: Premium Esports / Gaming

---

## Core Features & Workflow

1. **Gameplay Video Selection**:
   - Pick long gameplay recordings using native Android system picker.
   - Extracts metadata: duration, resolution, frame rate, audio channels.

2. **Auto Shorts Generator**:
   - Automatically crops 16:9 gameplay to vertical 9:16 (1080x1920 or 720x1280).
   - Generates 3 to 10 unique clips (15s to 35s each).
   - Adds 1-second hook titles (e.g. "INSANE CLUTCH 🔥", "GOD MODE ACTIVATED 💥").
   - Overlays channel watermark in top-right corner.
   - Appends channel 2-second outro MP4.
   - Exports directly to `Movies/NinjaAutoEditor` folder via MediaStore.

3. **Auto Long Video Generator**:
   - Maintains 16:9 widescreen layout.
   - Detects and trims long silent spans (>3 seconds) to remove boring sections.
   - Applies subtle zooms and speech subtitles on high-energy peaks.
   - Inserts selected meme overlays and sound effects without repetitive overuse.
   - Appends watermark and channel outro.

4. **Local Meme & SFX Library**:
   - Import custom PNG, JPG, GIF, MP3, WAV, and MP4 meme assets.
   - Organized into categories: *Funny, Fail, Headshot, Victory, Shock, Rage*.
   - Meme Frequency control: *Low (Default), Medium, High*, or Master Disable Toggle.

5. **Personal Settings Screen**:
   - Custom Channel Name (Default: "King Ninja55")
   - Import Channel Logo PNG
   - Import 2-Second Outro MP4
   - Import Custom Watermark PNG & Position/Scale
   - Audio Mixers for Game Audio and Meme SFX
   - Target Shorts Count (3 to 10 clips) & Short Duration bounds (15s to 35s)
   - Legal Rights Disclaimer

6. **Project History & Media Export**:
   - Complete record of exported videos in Room Database.
   - Built-in ExoPlayer video preview.
   - Native Android Share Intent and direct File Explorer access.

---

## Technical Details & Heuristics

| Feature | Implementation Status | Method Used |
| :--- | :--- | :--- |
| **Video Editing & Export** | **Fully Functional** | Android Jetpack Media3 Transformer, ExoPlayer, MediaExtractor, MediaMuxer |
| **Local Storage & Persistence** | **Fully Functional** | Android Room Database, KSP, WorkManager, MediaStore |
| **Offline Highlight Detection** | **Heuristic Engine** | Analyzes audio RMS energy windows, peak burst spikes, and signal variance |
| **Silent Gap Removal** | **Heuristic Engine** | Threshold-based audio amplitude scanning over time windows |
| **Meme & Sound Placement** | **Heuristic Randomizer** | Energy peak triggers respecting user frequency settings (Low/Med/High) without repeating memes |

---

## Build & Run Instructions

1. Clone or open the project in Android Studio (Ladybug or newer recommended).
2. Sync Gradle dependencies:
   ```bash
   ./gradlew assembleDebug
   ```
3. Run on a physical Android device or emulator running Android 10 (API 29) or higher.
4. On first launch, grant media read permissions when prompted.
