# Text-to-Speech (TTS) Reader Implementation Plan

This plan outlines the staged development of an Android Text-to-Speech application. Based on priority, the primary focus (Stage 1) is providing a seamless "Web Content to Speech" experience, similar to the core functionality of "@Voice Aloud Reader". Other features will be progressively added in later stages.

---

## 🚀 Stage 1: Web Content to Speech (MVP)

The goal of this stage is to build a robust foundation that allows users to share a web article to the app, extract the main text, and listen to it clearly in the background.

### Features
1. **Web Content Extraction:**
   - Use `Jsoup` or a Readability-port library to extract the main article text (headline + body) while stripping away ads, navigation menus, and sidebars.
2. **System Share Integration:**
   - Intent filters to allow users to "Share" a URL from a mobile browser (Chrome, Edge, etc.) directly to the app.
3. **Core TTS Engine (Chinese Support):**
   - Integration with Android Native `TextToSpeech` API.
   - Detect and set the TTS Locale specifically for Chinese (`Locale.CHINESE`, `Locale.SIMPLIFIED_CHINESE`, or `Locale.TRADITIONAL_CHINESE`).
   - Graceful handling if Chinese voice data is missing on the device (prompting user to download it via system settings).
   - Basic controls: Play/Pause, Stop, Next/Previous Sentence, and Speed adjustment.
4. **Background Playback & Media Controls:**
   - Integration with Android `Media3` (MediaSession) to allow audio to continue playing when the app is in the background or screen is locked.
   - Lock screen and notification media controls.
5. **Basic UI (Jetpack Compose):**
   - A simple screen displaying the extracted text.
   - Bottom sheet or persistent bar for playback controls.

### Technical Implementation (Stage 1)
- **`ArticleExtractor.kt`:** Logic to fetch HTML from URL and parse it into clean text. Will explicitly enforce UTF-8 encoding to ensure Chinese characters render correctly.
- **`TtsPlaybackService.kt`:** A foreground service managing the `TextToSpeech` engine (configured for Chinese locale) and `MediaSession`.
- **`MainActivity.kt`:** Handles incoming "Share" intents and UI rendering using Jetpack Compose.

---

## ⏳ Future Stages

Once Stage 1 is stable and validated, the following stages can be planned and executed iteratively.

### Stage 2: Document Support & Library Management
- **Local Storage:** Implement Room Database to save parsed articles for offline reading.
- **Document Parsing:** Add support for local files: PDF (`androidx.pdf` / MuPDF), EPUB (Librum), and TXT.
- **Library UI:** A home screen to manage saved articles and documents (lists, folders, search).

### Stage 3: Advanced Voice & Playback Features
- **Cloud Voices:** Add optional integration for premium cloud-based neural TTS engines (e.g., Google Cloud TTS API) for more natural sounding voices.
- **Text Highlighting:** Synchronize the audio playback with the UI to highlight the currently spoken word or sentence.
- **Granular Settings:** Save voice, speed, and pitch preferences per language or per document.

### Stage 4: Premium Features
- **OCR (Optical Character Recognition):** Integrate ML Kit to allow users to snap a photo of a page and have it read aloud.
- **Audio Export:** Ability to save the synthesized speech as an MP3 or WAV file.
- **Clipboard Monitoring:** Automatically detect copied text and prompt the user to listen to it.

---

## Technology Stack

| Component | Technology |
| :--- | :--- |
| **Language** | Kotlin |
| **UI Framework** | Jetpack Compose (Material 3) |
| **Web Content Parsing**| Jsoup |
| **Audio Playback** | Media3 + MediaSession + Android TTS |
| **Concurrency** | Kotlin Coroutines & Flows |
| **Dependency Injection**| Hilt |

---

## Open Questions for Stage 1
- **Extraction Accuracy:** Would you prefer to rely purely on local HTML parsing (Jsoup), or consider a lightweight API (if one exists) that guarantees perfectly clean article text? Local parsing is usually preferred for privacy and cost.
- **Visuals:** Do you want the app to also extract and display the main article image header, or strictly focus on the text?

---

## Verification Plan (Stage 1)

### Automated Tests
- Unit tests for `ArticleExtractor` using sample HTML snippets from news sites to guarantee accurate ad/menu stripping.
- Unit tests for chunking large texts into manageable TTS payload sizes.

### Manual Verification
- Share various Chinese article links (e.g., from local news sites or WeChat articles) to verify UTF-8 text extraction and punctuation handling.
- Verify the Android TTS engine correctly identifies and reads the Chinese text, handling edge cases like mixed English/Chinese content.
- Start reading an article, put the app in the background, and verify the audio continues.
- Pause/Play via the lock screen notification.
