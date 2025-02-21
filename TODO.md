# Cogwyrm MQTT Tasker Plugin - Project TODO

## Current Status
The project is a Tasker plugin for MQTT communication, allowing automation of MQTT messages through Tasker. Currently, we have basic structure but several critical issues need addressing.

### Components Overview
- `MainActivity`: Basic MQTT client UI
- `MQTTService`: Android Service for MQTT operations
- `MQTTClient`: MQTT client wrapper
- `MQTTActionReceiver`: Tasker action plugin for publishing messages
- `MQTTEventReceiver`: Tasker event plugin for MQTT message triggers (TODO)
- `MQTTConfigActivity`: Tasker configuration UI for publishing
- `MQTTEventConfigActivity`: Tasker configuration UI for triggers (TODO)

## Critical Issues

### 1. MQTT Connection Stability
- [ ] Fix connection handling in MQTTClient
- [ ] Implement proper connection lifecycle management
- [ ] Add connection retry mechanism
- [ ] Handle network changes gracefully

### 2. Tasker Integration
#### Action Plugin (Publishing)
- [ ] Fix synchronization issues in MQTTActionReceiver
- [ ] Implement proper async handling for MQTT operations
- [ ] Add better error reporting to Tasker
- [ ] Support more MQTT operations (subscribe, disconnect)

#### Event Plugin (Triggers)
- [ ] Create MQTTEventReceiver for handling MQTT triggers
- [ ] Implement event configuration activity
- [ ] Add topic pattern matching (wildcards)
- [ ] Provide message content as Tasker variables:
  - %mqtt_topic - The received topic
  - %mqtt_message - The message content
  - %mqtt_qos - QoS level
  - %mqtt_retained - Whether message was retained
  - %mqtt_timestamp - Message receipt timestamp

### 3. Security
- [ ] Implement proper SSL/TLS support
- [ ] Add username/password authentication
- [ ] Add certificate-based authentication
- [ ] Secure credential storage

### 4. UI/UX Improvements
- [ ] Add connection status indicators
- [ ] Improve error messages and user feedback
- [ ] Add message history view
- [ ] Add QoS level support
- [ ] Add retained message support

### 5. Testing
- [ ] Add unit tests for MQTT operations
- [ ] Add integration tests for Tasker integration
- [ ] Add UI tests
- [ ] Test with various MQTT brokers
- [ ] Test event plugin trigger scenarios
- [ ] Test variable passing to Tasker

### 6. Documentation
- [ ] Add code documentation
- [ ] Create user guide
- [ ] Add setup instructions
- [ ] Document Tasker integration steps:
  - How to set up MQTT publish tasks
  - How to create MQTT trigger profiles
  - Available variables and their usage
  - Example use cases and recipes

## Next Steps (Priority Order)

1. **Connection Stability**
   - Implement proper connection lifecycle
   - Add retry mechanism
   - Handle network changes

2. **Core MQTT Features**
   - Fix SSL/TLS support
   - Add authentication
   - Implement QoS levels
   - Add retained message support

3. **Tasker Integration**
   - Fix async operations
   - Improve error handling
   - Add more MQTT operations
   - Implement event plugin for triggers:
     * Topic subscription configuration
     * Pattern matching support
     * Variable passing to Tasker
     * Background service for maintaining subscriptions

4. **Testing & Documentation**
   - Add basic tests
   - Document setup process
   - Create user guide

## Architecture Improvements

### Service Layer
- [ ] Implement proper dependency injection
- [ ] Add repository pattern for message history
- [ ] Implement proper background service handling
- [ ] Add subscription management service for event plugin

### Error Handling
- [ ] Create centralized error handling
- [ ] Add proper error reporting
- [ ] Implement crash reporting

### Configuration
- [ ] Add proper configuration management
- [ ] Support multiple broker configurations
- [ ] Add import/export settings
- [ ] Persist event plugin subscriptions

## Long-term Goals

1. **Enhanced Features**
   - MQTT 5.0 support
   - Multiple simultaneous connections
   - Message templates
   - Offline message queueing
   - Advanced trigger conditions:
     * JSON path matching
     * Value comparisons
     * Regular expressions
     * Multiple topic monitoring

2. **Integration**
   - HomeAssistant integration
   - Other automation platforms
   - Custom protocols support
   - Integration with other Tasker plugins

3. **Analytics**
   - Usage tracking
   - Error reporting
   - Performance monitoring

## Example Use Cases
1. **Smart Home Automation**
   - Trigger: MQTT message from temperature sensor
   - Action: Control HVAC through MQTT

2. **Device Tracking**
   - Trigger: Location-based MQTT message
   - Action: Update device status via MQTT

3. **Notification Bridge**
   - Trigger: Phone notification
   - Action: Forward to MQTT topic

4. **Data Collection**
   - Trigger: Periodic sensor reading
   - Action: Publish to MQTT broker

## Notes
- Current focus should be on stabilizing core MQTT functionality
- Need to improve error handling and user feedback
- Consider implementing proper testing framework
- Documentation needs significant improvement
- Event plugin requires persistent background service
- Need to handle Tasker variable substitution in topics/messages

## Recent Progress

### Manifest Enhancements
- [x] Added package declaration
- [x] Summoned FOREGROUND_SERVICE permission
- [x] Enhanced application metadata
- [x] Fortified MQTTService configuration
- [x] Expanded Tasker plugin components
  - Added MQTTEventConfigActivity
  - Added MQTTEventReceiver
  - Prepared for event plugin integration

### Immediate Next Steps
- [ ] Validate Tasker event plugin functionality
- [ ] Test background service interactions
- [ ] Implement robust error handling in new components
- [ ] Create comprehensive logging for new service configurations

## Updated Priority Shifts
1. **Tasker Event Plugin Stabilization**
   - Complete MQTTEventReceiver implementation
   - Ensure proper topic subscription mechanisms
   - Add comprehensive error reporting

2. **Background Service Optimization**
   - Refine foreground service handling
   - Implement connection retry strategies
   - Add network change resilience

3. **Security Hardening**
   - Review permission scopes
   - Implement secure communication protocols
   - Add credential management

## Potential Chaos Vectors 🌪️
- Investigate potential race conditions in service startup
- Profile memory and battery usage of background services
- Explore edge cases in MQTT connection lifecycle

## Experimental Incantations 🧙‍♂️
- Consider reactive programming models for MQTT interactions
- Explore coroutine-based asynchronous handling
- Implement circuit breaker pattern for connection management
