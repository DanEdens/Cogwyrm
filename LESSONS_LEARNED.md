# Lessons Learned from Tasker Plugin Sample Project

## Manifest Declaration
- Declare configuration activities with appropriate intent filter actions for each plugin type (action, event, condition)
- Set `android:exported` to `true` for all plugin components 
- Specify `android:icon` and `android:label` for each plugin component

## Base Configuration Activity
- Create a base class that implements `TaskerPluginConfig` for all configuration activities
- Use generics to specify input, output, runner, and helper types
- Delegate Tasker-specific logic to a `TaskerPluginConfigHelper` instance
- Override `onKeyDown` to handle back button and validate configuration 
- Provide subclasses for common scenarios (no output, no input, no output or input)

## Tasker-Specific Utilities
- Utilize `TaskerPluginConfigHelper` for handling Tasker-specific setup and validation
- Use `TaskerPluginRunner` for executing plugin functionality
- Leverage `TaskerInput` and `TaskerPluginResult` classes for input/output handling

## Code Organization 
- Organize plugin components into separate packages based on functionality (action, event, condition)
- Use consistent naming convention for configuration activities, runners, and helpers

## Error Handling
- Use `TaskerPluginResultError` to report configuration validation errors
- Display user-friendly error messages using `alert` or similar utility functions 
