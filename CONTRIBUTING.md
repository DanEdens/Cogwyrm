# Contributing to Cogwyrm MQTT Tasker Plugin

## Getting Started

1. Fork the repository
2. Clone your fork: `git clone https://github.com/YOUR_USERNAME/Cogwyrm.git`
3. Create a feature branch: `git checkout -b feature/amazing-feature`
4. Make your changes
5. Test thoroughly
6. Create a PR

## Development Environment Setup

1. Install Android Studio
2. Install Tasker on your test device
3. Configure the development environment:
   ```properties
   android.useAndroidX=true
   android.enableJetifier=true
   kotlin.code.style=official
   android.suppressUnsupportedCompileSdk=34
   ```

## Code Style Guidelines

### Kotlin Style
- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Keep functions focused and small
- Add proper documentation for public APIs

### Android Best Practices
- Use ViewBinding for views
- Implement proper lifecycle management
- Handle configuration changes appropriately
- Follow Material Design guidelines

### MQTT Best Practices
- Implement proper connection handling
- Use appropriate QoS levels
- Handle connection failures gracefully
- Implement proper cleanup

## Testing Guidelines

### Unit Tests
- Write tests for all new functionality
- Use meaningful test names
- Follow arrange-act-assert pattern
- Mock external dependencies

### Integration Tests
- Test Tasker integration thoroughly
- Test MQTT operations end-to-end
- Verify error handling
- Test configuration persistence

## Documentation Requirements

### Code Documentation
- Document all public APIs
- Include usage examples
- Document error conditions
- Add inline comments for complex logic

### User Documentation
- Update README.md for new features
- Document configuration options
- Provide usage examples
- Include troubleshooting guides

## Security Guidelines

### Authentication
- Never store credentials in code
- Use secure storage for sensitive data
- Implement proper SSL/TLS
- Follow Android security best practices

### Error Handling
- Never expose sensitive info in errors
- Log appropriately
- Handle all error conditions
- Provide user-friendly error messages

## Pull Request Process

1. Update documentation
2. Add/update tests
3. Follow PR template
4. Request review
5. Address feedback
6. Update changelog

## Commit Message Guidelines

Format:
```
type(scope): subject

body

footer
```

Types:
- feat: New feature
- fix: Bug fix
- docs: Documentation
- style: Formatting
- refactor: Code restructuring
- test: Adding tests
- chore: Maintenance

## Need Help?

- Check existing issues
- Create a new issue
- Ask in discussions
- Read the documentation

## Code of Conduct

Please read and follow our [Code of Conduct](CODE_OF_CONDUCT.md).

## License

By contributing, you agree that your contributions will be licensed under the project's license. 
