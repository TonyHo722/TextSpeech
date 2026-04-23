# TextSpeech UI Design Overview

This document describes the updated user interface for the TextSpeech Android App.

## Layout Structure
The app uses a top-to-bottom layout optimized for one-handed use:

1.  **Top Bar**: Contains the App Title ("TTS Reader") and a vertical ellipsis (⋮) menu for manual URL input.
2.  **Middle (Content Section)**: A large scrollable list of sentences from the extracted article.
    *   **Highlighting**: The currently spoken sentence is highlighted with a purple background.
    *   **Auto-Scroll**: The list automatically scrolls to keep the active sentence in view.
    *   **Interaction**: Double-tapping any sentence triggers an immediate jump in playback.
3.  **Bottom (Control Box)**:
    *   **Row 1 (Playback)**: `⏮ Prev`, `▶ Play / ⏸ Pause`, `Next ⏭` buttons.
    *   **Row 2 (Speed)**: A playback speed slider (0.5x to 2.0x).

## Interaction Logic
| Action | Response |
| :--- | :--- |
| **Share URL to App** | App opens, extracts text, and starts playing automatically. |
| **Double-Tap Sentence** | Playback jumps to the selected sentence immediately. |
| **Toggle Play/Pause** | UI updates instantly (Optimistic UI) and service updates in the background. |

## UI Simulation (HTML)
I have created a standalone HTML simulation of the UI which can be viewed in any browser.

*   **Simulation File**: [UI_Simulation.html](file:///home/tonyho/workspace/TextSpeech/doc/UI_Simulation.html)
*   **Source File**: [simulation.html](file:///home/tonyho/workspace/TextSpeech/simulation.html) (Root folder)
