# MusicWear Companion 📱
<!-- Test -->
A sleek, Samsung-inspired media manager and music player for Android, built with modern Jetpack Compose. Designed to function seamlessly as a standalone player on your phone or as an optional companion app to sync and manage music on your **MusicWear** Wear OS device.

---
### DISCLAIMER
**APP IS IN DEVELOPMENT -- PLACE HOLDER THIS IS JUST A COPY OF MY OTHER MD FROM MY OTHER REPO SLIGHTLY MODIFIED, BUILD INSTRUCTIONS NOT COMPLETE**
* *PLACEHOLDER*

---
## ✨ Features

* **Sleek Design:** Will be intuitive, Samsung-inspired user interface built with Jetpack Compose.
* **Dual Functionality:** Will work fully as a standalone phone player or as an optional companion.
* **[IN DEVELOPMENT]** 
* **[IN DEVELOPMENT]** 
* **[IN DEVELOPMENT]** 
* **Future-Proof:** Will be built on modern Android APIs for high efficiency and long-term compatibility.

---

## 🚀 Technical Highlights

* **UI Framework:** [Jetpack Compose (Material 3)](https://developer.android.com/jetpack/compose) for a smooth, modern UI.
* **Audio Engine:** [Media3 / ExoPlayer](https://developer.android.com/guide/topics/media/media3) for high-performance audio processing.
* **Wear OS Syncing:** Prepared for low-energy cross-device communication via the Wearable Data Layer API.

---

## 🛠 Building from Source

To get the best performance, it is highly recommended to build and run the app in **Release Mode**.

---

### Prerequisites
* TBD
* [Android Studio Koala](https://developer.android.com/studio) or newer.
* Android SDK Platform 30+.

---

### Build Steps
TBD

1. **Clone the repository.**
2. **Open the project** in Android Studio and allow Gradle to sync.
3. **Select the Build Variant:**
    * Open the **Build Variants** tab (bottom-left of Android Studio).
    * Change the `:app` module variant from `debug` to **`release`**.
4. **Compile the APK:**
    * Go to **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
    * Once finished, click **Locate** to find your `app-release.apk`.

---
### Build Steps w/out Android Studio:
TBD

1. **Clone the repo either by downloading here, or by running:**
```bash
git clone [https://github.com/halftrackz/music-companion](https://github.com/halftrackz/music-companion)
```
2. **Install Android-SDK**
Android-SDK is a major dependency of this project, if you skip this step the app will **not build.**
3. **Install Java (JDK)**
   Gradle requires a Java Development Kit (JDK). Install a recent JDK, such as: OpenJDK Oracle JDK. If you also skip this step, the app will **not build.**
4. **Build the project**
Simply run gradlew.bat and select release or there will be performance issues, or if you are on Linux/macOS
```bash
chmod +x gradlew
```
**Then**
```bash
./gradlew assembleRelease
```
   *Gradle **should** automatically install, if not download it manually*

5. **Find the APK, it should be in:**
```bash
/app/build/outputs/apk/release
```
---

### Installation
TBD

Connect your phone via ADB and run:
```bash
adb install app-release.apk
```

---

### 🤝 Contributing
Contributions are welcome! If you have ideas for new features or performance improvements, feel free to open an issue or submit a pull request.

---

### 📜 License
This project is licensed under the GNU General Public License v3.0 (GNU GPLv3) - see the LICENSE file for details.
