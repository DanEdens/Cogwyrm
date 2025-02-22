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


🐉 Cogwyrm MQTT Tasker Plugin - THE GRAND MASTER PLAN! 🧙‍♂️
🎯 Current Mission: Fix Tasker Plugin Implementation
Phase 1: Input/Output Classes
[x] Fix MQTTEventInput implementation
[x] Proper TaskerPluginInput interface
[x] Parcelable implementation
[x] Input field annotations
[x] Validation logic
[ ] Enhance MQTTEventOutput
[ ] Review TaskerOutputObject implementation
[ ] Verify Parcelable implementation for data passing
[ ] Check output variable annotations for message, topic, and timestamp fields
[ ] Add proper timestamp handling
Phase 2: Configuration & UI
[ ] Polish MQTTEventConfigActivity
[ ] Fix view binding and initialization
[ ] Populate input fields from MQTTEventInput object
[ ] Assign user-entered values to MQTTEventInput on save
[ ] Implement proper input validation
[ ] Check for non-empty broker URL
[ ] Verify valid QoS values
[ ] Ensure non-empty topic
[ ] Display error toasts for invalid input
[ ] Enhance UI responsiveness
[ ] Test and ensure configuration persistence across launches
Phase 3: Event Runner & Helper
[ ] Strengthen MQTTEventRunner
[ ] Verify TaskerPluginRunnerConditionEvent implementation
[ ] Establish MQTT connection using MQTTEventInput configuration
[ ] Subscribe to specified topic
[ ] Emit MQTTEventOutput objects when messages received
[ ] Test condition satisfaction logic based on received messages
[ ] Add robust error handling for connection and subscription failures
[ ] Implement proper context usage
[ ] Refine MQTTEventHelper
[ ] Check TaskerPluginConfigHelper implementation
[ ] Define inputClass, outputClass and runnerClass
[ ] Verify input validation in isInputValid()
[ ] Test default input generation
[ ] Add comprehensive error messages
Phase 4: Testing & Documentation
[ ] Unit Tests
[ ] Input validation tests
[ ] Empty broker URL
[ ] Invalid QoS values
[ ] Empty topic
[ ] Output generation tests
[ ] Configuration persistence tests
[ ] Runner condition satisfaction tests
[ ] Error handling tests
[ ] Integration Tests
[ ] Full configuration flow
[ ] Event triggering end-to-end
[ ] MQTT connection failure handling
[ ] Subscription failure handling
[ ] Other error scenarios
Phase 5: Polish & Release
[ ] Optimize imports
[ ] Clean up leftover TODOs and comments
[ ] Documentation
[ ] Update README.md
[ ] Add detailed usage examples
[ ] Document all configuration options
[ ] Create troubleshooting guide for common issues
Phase 6: Publish Action Implementation
[ ] Implement MQTTTaskerAction
[ ] Flesh out run() method to publish messages
[ ] Utilize MQTTClient publish() capability
[ ] Handle publishing errors and communicate to user
[ ] Enhance MQTTConfigActivity for message publishing
[ ] Add input fields for message topic and payload
[ ] Validate topic and message values
[ ] Expand test coverage
[ ] Add unit tests for publishing logic
[ ] Integration tests for end-to-end publishing flow
[ ] Verify publishing error handling
🧪 LESSONS LEARNED (So Far)
Tasker Plugin Library requires careful attention to annotation implementation
Parcelable implementation is crucial for data passing
Input validation should be thorough but user-friendly
UI feedback is essential for configuration issues
5. Robust error handling is critical for MQTT operations
Comprehensive testing is a must for Tasker plugins
🚀 NEXT STEPS
Prioritize implementation of message publishing action
Focus on one phase at a time
Test thoroughly after each component fix
Keep track of any new issues discovered
Document changes and decisions
🐛 KNOWN ISSUES
1. Input validation needs enhancement
UI responsiveness could be improved
Error handling needs to be more comprehensive
Configuration persistence needs verification
5. Publishing action not yet functional
May the code be with us! 🧙‍♂️✨
</result>
