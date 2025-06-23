# Contributing to AntiCheatX

Thank you for your interest in contributing to AntiCheatX! We welcome contributions from the community and appreciate your help in making this anti-cheat solution better.

## 🚀 Quick Start

1. **Fork** the repository on GitHub
2. **Clone** your fork locally
3. **Create** a new branch for your feature/fix
4. **Make** your changes
5. **Test** thoroughly
6. **Submit** a pull request

## 📋 Table of Contents

- [Code of Conduct](#code-of-conduct)
- [How Can I Contribute?](#how-can-i-contribute)
- [Development Setup](#development-setup)
- [Coding Standards](#coding-standards)
- [Pull Request Process](#pull-request-process)
- [Issue Guidelines](#issue-guidelines)
- [Documentation](#documentation)
- [Testing](#testing)
- [Community](#community)

## 📜 Code of Conduct

This project adheres to a code of conduct. By participating, you are expected to uphold this code:

- **Be respectful** and inclusive
- **Be constructive** in discussions and feedback
- **Focus on the issue**, not the person
- **Help others learn** and grow
- **Respect different viewpoints** and experiences

## 🤝 How Can I Contribute?

### Reporting Bugs

Before submitting a bug report:
- **Check existing issues** to avoid duplicates
- **Use the latest version** of AntiCheatX
- **Test on a clean server** when possible

When submitting a bug report, include:
- **Clear title** describing the issue
- **Steps to reproduce** the problem
- **Expected vs actual behavior**
- **Environment details** (server version, Java version, etc.)
- **Configuration files** (remove sensitive information)
- **Log files** or error messages
- **Screenshots** if applicable

### Suggesting Features

Feature requests are welcome! Please:
- **Check existing requests** first
- **Describe the feature** clearly and in detail
- **Explain the use case** - why would this be useful?
- **Consider implementation** - how might it work?
- **Think about configuration** - should it be configurable?

### Code Contributions

We welcome code contributions for:
- **Bug fixes**
- **New detection methods**
- **Performance improvements**
- **Code quality enhancements**
- **Documentation improvements**
- **Test coverage**

## 🛠️ Development Setup

### Prerequisites

- **Java Development Kit (JDK) 21** or higher
- **Maven 3.6+** for dependency management
- **Git** for version control
- **IntelliJ IDEA** (recommended) or Eclipse
- **Paper 1.21.1** test server

### Setting Up Your Environment

1. **Fork and clone the repository**:
   ```bash
   git clone https://github.com/yourusername/AntiCheatX.git
   cd AntiCheatX
   ```

2. **Import into your IDE**:
   - IntelliJ IDEA: File → Open → Select the project folder
   - Eclipse: File → Import → Existing Maven Projects

3. **Build the project**:
   ```bash
   mvn clean package
   ```

4. **Set up test server**:
   ```bash
   # Create test server directory
   mkdir test-server
   cd test-server
   
   # Download Paper 1.21.1
   wget https://api.papermc.io/v2/projects/paper/versions/1.21.1/builds/latest/downloads/paper-1.21.1-latest.jar
   
   # Copy your built plugin
   mkdir plugins
   cp ../target/AntiCheatX-*.jar plugins/
   ```

### Project Structure

```
AntiCheatX/
├── src/
│   └── main/
│       ├── java/
│       │   └── online/codenexa/
│       │       └── antiCheatX.java     # Main plugin class
│       └── resources/
│           ├── config.yml              # Default configuration
│           └── plugin.yml              # Plugin metadata
├── pom.xml                             # Maven configuration
├── README.md                           # Project documentation
└── CONTRIBUTING.md                     # This file
```

## 📏 Coding Standards

### Java Code Style

We follow standard Java conventions with some specific preferences:

#### Formatting
- **Indentation**: 4 spaces (no tabs)
- **Line length**: 120 characters maximum
- **Braces**: Opening brace on same line
- **Imports**: Group and sort imports

#### Naming Conventions
```java
// Classes: PascalCase
public class PlayerDataManager

// Methods and variables: camelCase
private void checkPlayerMovement()
private int violationLevel

// Constants: UPPER_SNAKE_CASE
private static final int MAX_VIOLATION_THRESHOLD = 100

// Packages: lowercase
package online.codenexa.anticheatx.detection
```

#### Documentation
```java
/**
 * Checks for fly hacks by analyzing player movement patterns.
 * 
 * @param player The player to check
 * @param playerData The player's tracking data
 * @param from Previous location
 * @param to Current location
 */
private void checkFly(Player player, PlayerData playerData, Location from, Location to) {
    // Implementation
}
```

### Code Quality Guidelines

#### Performance Considerations
- **Avoid blocking operations** on the main thread
- **Use efficient data structures** (HashMap over LinkedList for lookups)
- **Minimize object creation** in hot paths
- **Cache expensive calculations**
- **Profile performance** impact of new features

#### Security Best Practices
- **Validate all inputs** from configuration and commands
- **Never trust client data** completely
- **Sanitize user-provided content**
- **Use permissions appropriately**
- **Log security-relevant events**

#### Error Handling
```java
// Good: Specific exception handling
try {
    double ratio = playerData.getValuableRatio();
    if (ratio > threshold) {
        handleViolation(player, "XRay", ratio);
    }
} catch (ArithmeticException e) {
    logger.warning("Division by zero in XRay calculation for " + player.getName());
} catch (Exception e) {
    logger.severe("Unexpected error in XRay detection: " + e.getMessage());
}

// Bad: Generic catch-all
try {
    // ... complex logic
} catch (Exception e) {
    // Ignoring all errors
}
```

## 🔄 Pull Request Process

### Before Submitting

1. **Create an issue** first (unless it's a minor fix)
2. **Fork the repository** and create a feature branch
3. **Write tests** for new functionality
4. **Update documentation** if needed
5. **Test thoroughly** on a real server
6. **Check code style** and formatting

### Branch Naming

- `feature/description` - New features
- `fix/description` - Bug fixes
- `docs/description` - Documentation updates
- `refactor/description` - Code refactoring
- `test/description` - Test improvements

Examples:
- `feature/timer-detection`
- `fix/xray-false-positives`
- `docs/configuration-guide`

### Commit Messages

Follow the conventional commit format:

```
type(scope): brief description

Longer description if needed.

Fixes #123
```

Types:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation
- `style`: Code formatting
- `refactor`: Code refactoring
- `test`: Testing
- `chore`: Maintenance

Examples:
```
feat(detection): add timer hack detection system

Implements client-side tick rate manipulation detection by analyzing
movement frequency patterns and comparing against expected server ticks.

Fixes #45
```

### Pull Request Template

When submitting a PR, include:

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Documentation update
- [ ] Performance improvement
- [ ] Code refactoring

## Testing
- [ ] Tested on Paper 1.21.1
- [ ] No performance regression
- [ ] All existing tests pass
- [ ] New tests added (if applicable)

## Configuration Changes
- [ ] No configuration changes
- [ ] Backward compatible changes
- [ ] Breaking changes (describe migration)

## Checklist
- [ ] Code follows project style guidelines
- [ ] Self-review completed
- [ ] Documentation updated
- [ ] Tests added/updated
```

## 🐛 Issue Guidelines

### Bug Reports

Use this template for bug reports:

```markdown
**Describe the Bug**
A clear description of what the bug is.

**To Reproduce**
1. Go to '...'
2. Click on '....'
3. See error

**Expected Behavior**
What you expected to happen.

**Environment:**
- AntiCheatX Version: [e.g. 1.0.0]
- Server Software: [e.g. Paper 1.21.1-123]
- Java Version: [e.g. OpenJDK 21]
- Other Plugins: [list relevant plugins]

**Configuration**
```yaml
# Your config.yml (remove sensitive info)
```

**Additional Context**
Add any other context or screenshots.
```

### Feature Requests

Use this template for feature requests:

```markdown
**Feature Description**
Clear description of the requested feature.

**Use Case**
Explain why this feature would be useful.

**Proposed Implementation**
How do you think this could be implemented?

**Alternatives Considered**
Other approaches you've considered.

**Additional Context**
Any other relevant information.
```

## 📝 Documentation

### Code Documentation

- **Document public APIs** with JavaDoc
- **Explain complex algorithms** with inline comments
- **Document configuration options** in config.yml
- **Update README** for major changes
- **Add wiki pages** for new features

### Wiki Contributions

We welcome improvements to our wiki:
- **Fix typos** and errors
- **Add examples** and tutorials
- **Improve clarity** of existing content
- **Add new guides** for advanced topics

## 🧪 Testing

### Manual Testing

Before submitting:
1. **Build and test** on a clean Paper server
2. **Test with real players** if possible
3. **Verify no false positives** with normal gameplay
4. **Check performance impact** with `/tps`
5. **Test edge cases** and error conditions

### Automated Testing

We encourage adding unit tests for:
- **Detection algorithms**
- **Configuration parsing**
- **Utility functions**
- **Edge cases**

Example test:
```java
@Test
public void testSpeedDetection() {
    Player mockPlayer = mock(Player.class);
    PlayerData playerData = new PlayerData();
    
    // Test normal movement
    Location from = new Location(world, 0, 0, 0);
    Location to = new Location(world, 0.5, 0, 0);
    
    assertFalse(speedDetection.isViolation(mockPlayer, playerData, from, to));
    
    // Test speed hack
    Location toFast = new Location(world, 2.0, 0, 0);
    assertTrue(speedDetection.isViolation(mockPlayer, playerData, from, toFast));
}
```

## 🏗️ Development Best Practices

### Performance Guidelines

- **Profile before optimizing** - measure actual performance impact
- **Use async operations** for I/O and complex calculations
- **Cache frequently accessed data**
- **Minimize object allocation** in hot paths
- **Consider memory usage** for large servers

### Security Guidelines

- **Validate configuration input** to prevent exploits
- **Rate limit expensive operations** to prevent DoS
- **Log security events** appropriately
- **Follow principle of least privilege**
- **Never trust client-side data** completely

### Compatibility Guidelines

- **Maintain backward compatibility** when possible
- **Document breaking changes** clearly
- **Provide migration guides** for major updates
- **Test against multiple Paper versions** when feasible
- **Consider plugin compatibility** with popular plugins

## 🌟 Recognition

Contributors will be recognized in:
- **README.md contributors section**
- **Release notes** for significant contributions
- **GitHub contributor stats**
- **Special thanks** for major features or fixes

## 💬 Community

### Getting Help

- **GitHub Discussions** - General questions and ideas
- **GitHub Issues** - Bug reports and feature requests
- **Code Reviews** - Learning and improvement opportunities

### Communication Guidelines

- **Be patient** - maintainers are volunteers
- **Be specific** - provide detailed information
- **Be helpful** - help others in the community
- **Stay on topic** - keep discussions relevant

## 📞 Contact

- **GitHub Issues**: Technical problems and bugs
- **GitHub Discussions**: General questions and community
- **Pull Requests**: Code contributions
- **Email**: [contact info] for security issues

---

Thank you for contributing to AntiCheatX! Your efforts help make Minecraft servers safer and more enjoyable for everyone.

**Happy coding!** 🎮