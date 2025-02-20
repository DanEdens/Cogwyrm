# MQTT Commander: The Pocket Portal
*"Control your Swarmonomicon agents from the palm of your hand!"*

## 🌋 Overview
A mobile gateway for commanding your army of AI agents through the mystical arts of MQTT. This Android application serves as both a direct control interface and a Tasker plugin, allowing for automated agent interactions.

## 🧪 Features
- Direct MQTT message summoning
- Tasker plugin integration for automated rituals
- Web Tasker communion protocols
- Background service for maintaining constant contact with your agents
- Material Design 3 UI with proper mad scientist aesthetics
- Local notification system for agent responses
- Widget support for quick incantations

## 🛠️ Development Setup
### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK (API 34)
- Gradle 8.0+
- JDK 17
- A brave soul



### Required Dependencies
- Eclipse Paho MQTT Client (automatically handled by Gradle)
- AndroidX Core and AppCompat libraries
- Material Design 3 components

### Building Instructions
1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/mqtt-commander.git
   ```
2. Open the project in Android Studio
3. Sync Gradle files
4. Build the project:
   ```bash
   ./gradlew assembleDebug
   ```
5. Run on an emulator or device:
   ```bash
   ./gradlew installDebug
   ```

## 📱 Building the Portal


Clone the repository
git clone https://github.com/yourusername/mqtt-commander.git
Enter the sanctum
cd mqtt-commander
Summon the dependencies
./gradlew build


mqtt-commander/
├── app/
│ ├── src/
│ │ ├── main/
│ │ │ ├── java/
│ │ │ │ └── com.madness.mqttcommander/
│ │ │ ├── res/
│ │ │ └── AndroidManifest.xml
│ │ ├── test/
│ │ └── androidTest/
├── gradle/
└── build.gradle
