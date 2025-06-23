# AntiCheatX

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Version](https://img.shields.io/badge/version-1.0.0-green.svg)
![Minecraft](https://img.shields.io/badge/minecraft-1.21.1-orange.svg)
![Platform](https://img.shields.io/badge/platform-Paper-red.svg)

A powerful and configurable anti-cheat plugin for Paper Minecraft servers, designed to detect and prevent various forms of cheating while maintaining excellent server performance.

## 🚀 Features

### Core Anti-Cheat Detections
- **Fly Detection** - Detects players flying without permission
- **Speed Hacks** - Identifies unusual movement speeds
- **X-Ray Detection** - Advanced ore mining pattern analysis
- **KillAura/Combat Hacks** - Detects automated combat assistance
- **Reach Hacks** - Prevents extended attack/interaction range
- **NoFall** - Detects fall damage negation
- **Timer Manipulation** - Identifies client-side tick rate changes
- **Scaffold Hacks** - Detects suspicious block placement patterns
- **Irregular Movement** - Catches impossible movement behaviors

### Advanced Features
- **Real-time Monitoring** - Continuous player behavior analysis
- **Configurable Thresholds** - Customize sensitivity for each check
- **Multiple Punishment Types** - Warn, kick, or ban offending players
- **Admin Alerts** - Real-time notifications for staff
- **Comprehensive Logging** - File and console logging with detailed reports
- **Violation Decay** - Automatic violation level reduction over time
- **Multiverse Support** - Seamless world change handling
- **Performance Optimized** - Minimal server impact

## 📋 Requirements

- **Minecraft Version**: 1.21.1
- **Server Software**: Paper (recommended) or any Paper-compatible fork
- **Java Version**: 21 or higher
- **Permissions Plugin**: Any Bukkit-compatible permissions plugin (optional)

## 🔧 Installation

1. **Download** the latest release from the [Releases](../../releases) page
2. **Place** the `AntiCheatX.jar` file in your server's `plugins` folder
3. **Restart** your server
4. **Configure** the plugin by editing `plugins/AntiCheatX/config.yml`
5. **Reload** the plugin with `/acx reload` or restart the server

## ⚙️ Configuration

The plugin creates a comprehensive configuration file at `plugins/AntiCheatX/config.yml`. Here are the key sections:

### Violation Thresholds
```yaml
violations:
  fly: 10          # Number of violations before punishment
  speed: 8
  xray: 3
  killAura: 5
  # ... and more
```

### Punishment Actions
```yaml
punishments:
  actions:
    fly: KICK        # Options: NONE, WARN, KICK, BAN
    speed: KICK
    xray: BAN
    # ... configure for each check type
```

### Check Configuration
```yaml
checks:
  fly:
    enabled: true
    maxAirTime: 1500  # Maximum air time in milliseconds
  
  speed:
    enabled: true
    baseMaxSpeed: 0.75
    sprintMultiplier: 1.3
  
  xray:
    enabled: true
    thresholdRatio: 0.10  # 10% valuable ore ratio triggers detection
    # ... detailed configuration for each check
```

## 🎮 Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/acx` | `anticheatx.admin` | Show plugin help |
| `/acx reload` | `anticheatx.admin` | Reload plugin configuration |
| `/acx status` | `anticheatx.admin` | Display plugin status and statistics |
| `/acx violations <player>` | `anticheatx.admin` | Show violation levels for a player |
| `/acx alerts` | `anticheatx.admin` | Toggle violation alerts on/off |

## 🔐 Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `anticheatx.admin` | `op` | Access to all administrative commands |
| `anticheatx.alerts` | `op` | Receive violation alerts |
| `anticheatx.bypass` | `false` | Bypass all anti-cheat checks |

## 📊 Detection Methods

### X-Ray Detection
- **Pattern Analysis**: Detects unnatural ore-finding patterns
- **Ratio Monitoring**: Tracks valuable ore to total blocks mined ratio
- **Direct Tunneling**: Identifies straight-line movement to ores

### Movement Checks
- **Physics Validation**: Ensures movement follows Minecraft physics
- **Speed Monitoring**: Accounts for effects, sprinting, and game modes
- **Anti-Teleportation**: Prevents location modification by checks

### Combat Detection
- **Attack Rate**: Monitors inhuman attack frequencies
- **Targeting Analysis**: Detects impossible target switching
- **Reach Validation**: Ensures attacks are within normal range

## 📈 Performance

AntiCheatX is designed with performance in mind:
- **Asynchronous Processing**: Non-blocking detection algorithms
- **Smart Sampling**: Efficient data collection and analysis
- **Memory Management**: Automatic cleanup of old data
- **Configurable Intensity**: Adjust checking frequency vs. performance

## 🐛 Troubleshooting

### Common Issues

**False Positives**
- Adjust violation thresholds in `config.yml`
- Check server TPS and lag issues
- Verify player permissions

**Plugin Not Loading**
- Ensure Java 21+ is installed
- Check for conflicting plugins
- Verify Paper server version

**Performance Issues**
- Reduce check frequencies in config
- Increase violation thresholds
- Check server specifications

### Debug Information

Enable detailed logging in `config.yml`:
```yaml
logging:
  console: true
  file: true
```

Logs are saved to `plugins/AntiCheatX/logs/violations.log`

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details.

### Development Setup
1. Clone the repository
2. Import into IntelliJ IDEA or Eclipse
3. Run `mvn clean package` to build
4. Test on a Paper 1.21.1 server

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

- **Issues**: Report bugs on our [Issue Tracker](../../issues)
- **Discussions**: Join our [Discussions](../../discussions)
- **Wiki**: Check our [comprehensive wiki](../../wiki) for detailed documentation
- **Website**: Visit [codenexa.online](https://codenexa.online) for more information

## 🔄 Changelog

### Version 1.0.0
- Initial release
- All core anti-cheat features implemented
- Comprehensive configuration system
- Admin tools and logging system
- Multi-world support

## 🙏 Acknowledgments

- Built for the Minecraft Paper server platform
- Inspired by the need for effective, lightweight anti-cheat solutions
- Thanks to all contributors and testers

---

**Made with ❤️ by [Chamika Samaraweera](https://codenexa.online)**