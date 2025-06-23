package online.codenexa;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

public class antiCheatX extends JavaPlugin implements Listener {

    private FileConfiguration config;
    private Logger logger;
    private String prefix;
    private Map<UUID, PlayerData> playerDataMap = new HashMap<>();
    private Set<UUID> alertReceivers = new HashSet<>();

    // Welcome message settings
    private boolean welcomeMessageEnabled;
    private String welcomeMessage;
    private String serverName;
    private int welcomeMessageDelay;

    // Violation thresholds
    private int flyViolationThreshold;
    private int speedViolationThreshold;
    private int reachViolationThreshold;
    private int xrayViolationThreshold;
    private int irregularMovementViolationThreshold;
    private int killAuraViolationThreshold;
    private int noFallViolationThreshold;
    private int timerViolationThreshold;
    private int scaffoldViolationThreshold;

    // Messages
    private String adminAlertMessage;
    private String kickMessage;
    private String warningMessage;
    private String banMessage;

    // Punishment settings
    private Map<String, String> punishmentActions;
    private int banDuration;
    private boolean resetAfterBan;

    // XRay detection
    private Set<Material> valuableOres;
    private int miningSampleSize;
    private double xrayThresholdRatio;
    private int xrayDetectionInterval; // Time interval in seconds to reset mining stats

    // Logging settings
    private boolean logToConsole;
    private boolean logToFile;
    private String logFilePath;
    private FileWriter logWriter;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();
        config = getConfig();
        logger = getLogger();

        // Setup log file
        setupLogFile();

        // Register events
        getServer().getPluginManager().registerEvents(this, this);

        // Register commands
        getCommand("acx").setExecutor(new AntiCheatXCommand());

        // Load configuration
        loadConfiguration();

        // Start scheduled tasks
        startScheduledTasks();

        // Plugin enabled message
        logger.info(prefix + " AntiCheatX v1.0.0 has been enabled!");
    }

    @Override
    public void onDisable() {
        // Close log file if open
        if (logWriter != null) {
            try {
                logWriter.close();
            } catch (IOException e) {
                logger.severe("Error closing log file: " + e.getMessage());
            }
        }

        // Plugin disabled message
        logger.info(prefix + " AntiCheatX has been disabled!");
    }

    private void setupLogFile() {
        // Create logs directory if it doesn't exist
        File logsDir = new File(getDataFolder(), "logs");
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }

        // Setup log file
        try {
            File logFile = new File(getDataFolder(), "logs/violations.log");
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
            logWriter = new FileWriter(logFile, true);
        } catch (IOException e) {
            logger.severe("Error setting up log file: " + e.getMessage());
        }
    }

    private void startScheduledTasks() {
        // Schedule task to reset mining statistics periodically
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (PlayerData playerData : playerDataMap.values()) {
                // Reset mining stats but keep violations
                playerData.resetMiningStats();
            }
        }, 20L * xrayDetectionInterval, 20L * xrayDetectionInterval);

        // Schedule task to periodically save logs and check for inactivity
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            // Flush logs
            if (logWriter != null) {
                try {
                    logWriter.flush();
                } catch (IOException e) {
                    logger.warning("Error flushing log file: " + e.getMessage());
                }
            }

            // Check for player inactivity and decay violation levels
            long currentTime = System.currentTimeMillis();
            for (Map.Entry<UUID, PlayerData> entry : playerDataMap.entrySet()) {
                PlayerData playerData = entry.getValue();

                // Decay violation levels for inactive players
                if (currentTime - playerData.lastActivityTime > 300000) { // 5 minutes
                    decayViolationLevels(playerData);
                }
            }
        }, 6000L, 6000L); // Every 5 minutes
    }

    private void decayViolationLevels(PlayerData playerData) {
        // Decay violation levels over time
        if (playerData.flyViolationLevel > 0) playerData.flyViolationLevel--;
        if (playerData.speedViolationLevel > 0) playerData.speedViolationLevel--;
        if (playerData.reachViolationLevel > 0) playerData.reachViolationLevel--;
        if (playerData.xrayViolationLevel > 0) playerData.xrayViolationLevel--;
        if (playerData.irregularMovementViolationLevel > 0) playerData.irregularMovementViolationLevel--;
        if (playerData.killAuraViolationLevel > 0) playerData.killAuraViolationLevel--;
        if (playerData.noFallViolationLevel > 0) playerData.noFallViolationLevel--;
        if (playerData.timerViolationLevel > 0) playerData.timerViolationLevel--;
        if (playerData.scaffoldViolationLevel > 0) playerData.scaffoldViolationLevel--;
    }

    private void loadConfiguration() {
        // Load prefix
        prefix = ChatColor.translateAlternateColorCodes('&', config.getString("prefix", "&8[&c&lAntiCheatX&8]"));

        // Load welcome message settings
        welcomeMessageEnabled = config.getBoolean("welcomeMessage.enabled", true);
        welcomeMessage = ChatColor.translateAlternateColorCodes('&',
                config.getString("welcomeMessage.message", "&a%server% &fserver protected by using &8[&c&lAntiCheatX&8]"));

        // Get server name from config or use server default
        String configServerName = config.getString("welcomeMessage.serverName", "");
        if (configServerName.isEmpty()) {
            // Try to get server name from server properties or use a default
            try {
                serverName = Bukkit.getServer().getName();
                if (serverName == null || serverName.isEmpty() || serverName.equals("Unknown Server")) {
                    serverName = "Minecraft"; // Default fallback
                }
            } catch (Exception e) {
                serverName = "Minecraft"; // Fallback if error occurs
            }
        } else {
            serverName = configServerName;
        }

        welcomeMessageDelay = config.getInt("welcomeMessage.delay", 40);

        // Load violation thresholds
        flyViolationThreshold = config.getInt("violations.fly", 10);
        speedViolationThreshold = config.getInt("violations.speed", 8);
        reachViolationThreshold = config.getInt("violations.reach", 6);
        xrayViolationThreshold = config.getInt("violations.xray", 3);
        irregularMovementViolationThreshold = config.getInt("violations.irregularMovement", 8);
        killAuraViolationThreshold = config.getInt("violations.killAura", 5);
        noFallViolationThreshold = config.getInt("violations.noFall", 5);
        timerViolationThreshold = config.getInt("violations.timer", 5);
        scaffoldViolationThreshold = config.getInt("violations.scaffold", 5);

        // Load messages
        adminAlertMessage = ChatColor.translateAlternateColorCodes('&', config.getString("messages.adminAlert",
                "&8[&c&lAntiCheatX&8] &f%player% &cmay be using &f%cheat% &c(&f%violations% violations&c)"));
        kickMessage = ChatColor.translateAlternateColorCodes('&', config.getString("messages.kick",
                "&8[&c&lAntiCheatX&8] &cYou have been kicked for suspicious activity.\n&fReason: %cheat%"));
        warningMessage = ChatColor.translateAlternateColorCodes('&', config.getString("messages.warning",
                "&8[&c&lAntiCheatX&8] &cPlease disable any cheats or hacks you may be using."));
        banMessage = ChatColor.translateAlternateColorCodes('&', config.getString("messages.ban",
                "&8[&c&lAntiCheatX&8] &cYou have been banned for using hacks.\n&fReason: %cheat%\n&fDuration: %duration%"));

        // Load punishment actions
        punishmentActions = new HashMap<>();
        punishmentActions.put("fly", config.getString("punishments.actions.fly", "KICK").toUpperCase());
        punishmentActions.put("speed", config.getString("punishments.actions.speed", "KICK").toUpperCase());
        punishmentActions.put("reach", config.getString("punishments.actions.reach", "KICK").toUpperCase());
        punishmentActions.put("xray", config.getString("punishments.actions.xray", "KICK").toUpperCase());
        punishmentActions.put("irregularMovement", config.getString("punishments.actions.irregularMovement", "KICK").toUpperCase());
        punishmentActions.put("killAura", config.getString("punishments.actions.killAura", "KICK").toUpperCase());
        punishmentActions.put("noFall", config.getString("punishments.actions.noFall", "KICK").toUpperCase());
        punishmentActions.put("timer", config.getString("punishments.actions.timer", "KICK").toUpperCase());
        punishmentActions.put("scaffold", config.getString("punishments.actions.scaffold", "KICK").toUpperCase());

        // Load ban duration (in minutes)
        banDuration = config.getInt("punishments.banDuration", 60);
        resetAfterBan = config.getBoolean("punishments.resetAfterBan", true);

        // Load XRay detection settings
        valuableOres = new HashSet<>();
        for (String oreName : config.getStringList("checks.xray.valuableOres")) {
            try {
                Material material = Material.valueOf(oreName.toUpperCase());
                valuableOres.add(material);
            } catch (IllegalArgumentException e) {
                logger.warning(prefix + " Invalid ore material in config: " + oreName);
            }
        }

        // If no ores are specified, add defaults
        if (valuableOres.isEmpty()) {
            valuableOres.add(Material.DIAMOND_ORE);
            valuableOres.add(Material.DEEPSLATE_DIAMOND_ORE);
            valuableOres.add(Material.ANCIENT_DEBRIS);
            valuableOres.add(Material.EMERALD_ORE);
            valuableOres.add(Material.DEEPSLATE_EMERALD_ORE);
            valuableOres.add(Material.GOLD_ORE);
            valuableOres.add(Material.DEEPSLATE_GOLD_ORE);
        }

        miningSampleSize = config.getInt("checks.xray.miningSampleSize", 100);
        xrayThresholdRatio = config.getDouble("checks.xray.thresholdRatio", 0.10);
        xrayDetectionInterval = config.getInt("checks.xray.detectionInterval", 1800); // Default 30 minutes

        // Load logging settings
        logToConsole = config.getBoolean("logging.console", true);
        logToFile = config.getBoolean("logging.file", true);
        logFilePath = config.getString("logging.filePath", "logs/violations.log");
    }

    /**
     * Reloads the entire plugin
     * @return True if reload was successful
     */
    public boolean reloadPlugin() {
        try {
            // Unregister all listeners
            org.bukkit.event.HandlerList.unregisterAll((Plugin) this);

            // Save default config if it doesn't exist
            saveDefaultConfig();

            // Reload config from disk
            reloadConfig();
            config = getConfig();

            // Reload configuration settings
            loadConfiguration();

            // Re-register all events
            getServer().getPluginManager().registerEvents(this, this);

            // Log reload success
            logger.info(prefix + " Plugin successfully reloaded!");
            return true;
        } catch (Exception e) {
            // Log error
            logger.severe(prefix + " Error reloading plugin: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Listener for player join
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        playerDataMap.put(player.getUniqueId(), new PlayerData());

        // Check if player has permission to bypass
        if (player.hasPermission("anticheatx.bypass")) {
            logger.info(player.getName() + " has bypass permission for AntiCheatX.");
        }

        // Add player to alert receivers if they have the permission
        if (player.hasPermission("anticheatx.alerts")) {
            alertReceivers.add(player.getUniqueId());
        }

        // Send welcome message if enabled
        if (welcomeMessageEnabled) {
            // Schedule the welcome message to be sent after a delay
            Bukkit.getScheduler().runTaskLater(this, () -> {
                // Check if player is still online
                if (player.isOnline()) {
                    String message = welcomeMessage
                            .replace("%server%", serverName)
                            .replace("%player%", player.getName());
                    player.sendMessage(message);
                }
            }, welcomeMessageDelay);
        }
    }

    // Listener for player quit
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Remove from alert receivers
        alertReceivers.remove(player.getUniqueId());

        // Keep player data for a while in case they rejoin
        // But clear it after some time (handled by the periodic task)
    }

    // Listener for player movement (check for fly, speed, and irregular movements)
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // CRITICAL: Never cancel or modify this event to prevent teleportation bugs
        Player player = event.getPlayer();

        // Skip players with bypass permission
        if (player.hasPermission("anticheatx.bypass")) {
            return;
        }

        // Get player data
        PlayerData playerData = playerDataMap.get(player.getUniqueId());
        if (playerData == null) {
            playerData = new PlayerData();
            playerDataMap.put(player.getUniqueId(), playerData);
        }

        // Update activity time
        playerData.lastActivityTime = System.currentTimeMillis();

        // Get locations - create safe copies to avoid any reference issues
        Location from = event.getFrom();
        Location to = event.getTo();

        // Skip if one of the locations is null or if movement is too small
        if (from == null || to == null || from.distanceSquared(to) < 0.0001) {
            return;
        }

        // Schedule checks on next tick to avoid interfering with movement
        PlayerData finalPlayerData = playerData;
        Bukkit.getScheduler().runTask(this, () -> {
            // Double-check player is still online
            if (!player.isOnline()) {
                return;
            }

            // CRITICAL: Check for world changes before any calculations
            if (finalPlayerData.currentWorld != null && !finalPlayerData.currentWorld.equals(to.getWorld().getName())) {
                // Player changed worlds - reset all position data
                finalPlayerData.previousPositions.clear();
                finalPlayerData.currentWorld = to.getWorld().getName();
                finalPlayerData.lastJumpY = to.getY();
                return;
            }

            // Store previous position if this is a new tick and same world
            if (finalPlayerData.lastMovementTick != player.getWorld().getFullTime()) {
                // Only add position if it's from the same world as current
                if (finalPlayerData.currentWorld == null || finalPlayerData.currentWorld.equals(from.getWorld().getName())) {
                    finalPlayerData.previousPositions.add(from.clone());

                    // Keep only the last 20 positions (1 second worth of data)
                    if (finalPlayerData.previousPositions.size() > 20) {
                        finalPlayerData.previousPositions.remove(0);
                    }
                } else {
                    // World mismatch - clear and restart
                    finalPlayerData.previousPositions.clear();
                }

                finalPlayerData.lastMovementTick = player.getWorld().getFullTime();
                finalPlayerData.currentWorld = to.getWorld().getName();
            }

            // Check for fly hacks
            if (config.getBoolean("checks.fly.enabled", true)) {
                checkFly(player, finalPlayerData, from.clone(), to.clone());
            }

            // Check for speed hacks
            if (config.getBoolean("checks.speed.enabled", true)) {
                checkSpeed(player, finalPlayerData, from.clone(), to.clone());
            }

            // Check for irregular movements - only if same world
            if (config.getBoolean("checks.irregularMovement.enabled", true) &&
                    from.getWorld().equals(to.getWorld())) {
                checkIrregularMovements(player, finalPlayerData, from.clone(), to.clone());
            }

            // Check for timer hack (uses accumulated data)
            if (config.getBoolean("checks.timer.enabled", true) && finalPlayerData.previousPositions.size() > 5) {
                checkTimer(player, finalPlayerData);
            }
        });
    }

    // Listener for block break events (XRay detection)
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        // Skip players with bypass permission
        if (player.hasPermission("anticheatx.bypass")) {
            return;
        }

        // Get player data
        PlayerData playerData = playerDataMap.get(player.getUniqueId());
        if (playerData == null) {
            playerData = new PlayerData();
            playerDataMap.put(player.getUniqueId(), playerData);
        }

        // Update activity time
        playerData.lastActivityTime = System.currentTimeMillis();

        // Check for XRay
        if (config.getBoolean("checks.xray.enabled", true)) {
            checkXRay(player, playerData, event);
        }
    }

    // Listener for block place events (Scaffold detection)
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        // Skip players with bypass permission
        if (player.hasPermission("anticheatx.bypass")) {
            return;
        }

        // Get player data
        PlayerData playerData = playerDataMap.get(player.getUniqueId());
        if (playerData == null) {
            playerData = new PlayerData();
            playerDataMap.put(player.getUniqueId(), playerData);
        }

        // Update activity time
        playerData.lastActivityTime = System.currentTimeMillis();

        // Check for Scaffold
        if (config.getBoolean("checks.scaffold.enabled", true)) {
            checkScaffold(player, playerData, event);
        }
    }

    // Listener for entity damage events (KillAura detection)
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getDamager();

        // Skip players with bypass permission
        if (player.hasPermission("anticheatx.bypass")) {
            return;
        }

        // Get player data
        PlayerData playerData = playerDataMap.get(player.getUniqueId());
        if (playerData == null) {
            playerData = new PlayerData();
            playerDataMap.put(player.getUniqueId(), playerData);
        }

        // Update activity time
        playerData.lastActivityTime = System.currentTimeMillis();

        // Check for KillAura
        if (config.getBoolean("checks.killAura.enabled", true)) {
            checkKillAura(player, playerData, event);
        }
    }

    // Listener for fall damage events (NoFall detection)
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player) || event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }

        Player player = (Player) event.getEntity();

        // Skip players with bypass permission
        if (player.hasPermission("anticheatx.bypass")) {
            return;
        }

        // Get player data
        PlayerData playerData = playerDataMap.get(player.getUniqueId());
        if (playerData == null) {
            playerData = new PlayerData();
            playerDataMap.put(player.getUniqueId(), playerData);
        }

        // Update activity time
        playerData.lastActivityTime = System.currentTimeMillis();

        // Check for NoFall
        if (config.getBoolean("checks.noFall.enabled", true)) {
            checkNoFall(player, playerData, event);
        }
    }

    // Check for fly hacks
    private void checkFly(Player player, PlayerData playerData, Location from, Location to) {
        // Skip if player is allowed to fly
        if (player.getAllowFlight() || player.isFlying()) {
            return;
        }

        // Skip if player is in creative or spectator mode
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE ||
                player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
            return;
        }

        // Check if player is not on ground and not in water/lava for extended period
        if (!player.isOnGround() && !player.isInWater() && !player.isInLava() &&
                !player.isGliding() && !player.isSwimming()) {

            long currentTime = System.currentTimeMillis();

            // First detection of being off ground
            if (playerData.lastOffGroundTime == 0) {
                playerData.lastOffGroundTime = currentTime;
                return;
            }

            // Get max air time from config
            long maxAirTime = config.getLong("checks.fly.maxAirTime", 1500);

            // If player has been off ground for too long
            if (currentTime - playerData.lastOffGroundTime > maxAirTime) {
                // Check if they're still moving upward (against gravity)
                if (to.getY() > from.getY() && playerData.previousPositions.size() >= 2) {
                    Location prevPos = playerData.previousPositions.get(playerData.previousPositions.size() - 2);

                    // If they're consistently moving upward without a jump
                    if (from.getY() > prevPos.getY()) {
                        // Increment violation level
                        playerData.flyViolationLevel++;

                        // Log detailed debug info
                        logDetection(player, "Fly",
                                String.format("Air time: %dms, Y-velocity trend: %.2f",
                                        currentTime - playerData.lastOffGroundTime,
                                        to.getY() - from.getY()));

                        // Handle violation - NEVER modify player location here
                        handleViolation(player, "Fly", playerData.flyViolationLevel, flyViolationThreshold);
                    }
                }
            }
        } else {
            // Reset off ground time
            playerData.lastOffGroundTime = 0;

            // Gradually decrease violation level if player is not violating
            if (playerData.flyViolationLevel > 0 && Math.random() < 0.1) {
                playerData.flyViolationLevel--;
            }
        }
    }

    // Check for speed hacks
    private void checkSpeed(Player player, PlayerData playerData, Location from, Location to) {
        // Skip if player is in creative or spectator mode
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE ||
                player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
            return;
        }

        // Skip if player is flying, riding a vehicle, or using elytra
        if (player.isFlying() || player.isInsideVehicle() || player.isGliding()) {
            return;
        }

        // Calculate horizontal distance moved
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);

        // Get max allowed speed (adjusted for sprint and effects)
        double baseMaxSpeed = config.getDouble("checks.speed.baseMaxSpeed", 0.75);
        double maxSpeed = baseMaxSpeed;

        // Adjust for sprinting
        if (player.isSprinting()) {
            maxSpeed *= config.getDouble("checks.speed.sprintMultiplier", 1.3);
        }

        // Adjust for speed effect
        if (player.hasPotionEffect(org.bukkit.potion.PotionEffectType.SPEED)) {
            int amplifier = 0;
            for (org.bukkit.potion.PotionEffect effect : player.getActivePotionEffects()) {
                if (effect.getType().equals(org.bukkit.potion.PotionEffectType.SPEED)) {
                    amplifier = effect.getAmplifier() + 1;
                    break;
                }
            }
            maxSpeed *= (1 + (config.getDouble("checks.speed.speedEffectMultiplier", 0.2) * amplifier));
        }

        // Check if distance exceeds max speed
        if (distance > maxSpeed) {
            // Increment violation level
            playerData.speedViolationLevel++;

            // Log detailed debug info
            logDetection(player, "Speed",
                    String.format("Distance: %.2f blocks, Max allowed: %.2f blocks",
                            distance, maxSpeed));

            // Handle violation - NEVER modify player location here
            handleViolation(player, "Speed", playerData.speedViolationLevel, speedViolationThreshold);
        } else {
            // Gradually decrease violation level if player is not violating
            if (playerData.speedViolationLevel > 0 && Math.random() < 0.1) {
                playerData.speedViolationLevel--;
            }
        }
    }

    // Check for irregular movements
    private void checkIrregularMovements(Player player, PlayerData playerData, Location from, Location to) {
        // CRITICAL: Check if locations are from the same world to prevent multiverse errors
        if (!from.getWorld().equals(to.getWorld())) {
            // Player changed worlds - clear position history and reset data
            playerData.previousPositions.clear();
            playerData.lastJumpY = to.getY();
            playerData.currentWorld = to.getWorld().getName();
            return;
        }

        // Check if player's world changed since last check
        if (playerData.currentWorld != null && !playerData.currentWorld.equals(to.getWorld().getName())) {
            // World changed - clear position history
            playerData.previousPositions.clear();
            playerData.lastJumpY = to.getY();
            playerData.currentWorld = to.getWorld().getName();
            return;
        }

        // Set initial world if not set
        if (playerData.currentWorld == null) {
            playerData.currentWorld = to.getWorld().getName();
        }

        // 1. Check for impossible direction changes
        if (playerData.previousPositions.size() >= 3 && player.getVelocity().length() > 0.1) {
            List<Location> positions = playerData.previousPositions;
            int lastIdx = positions.size() - 1;

            if (lastIdx >= 2) {
                // Ensure all positions are from the same world
                Location pos1 = positions.get(lastIdx);
                Location pos2 = positions.get(lastIdx - 1);

                if (!pos1.getWorld().equals(pos2.getWorld()) ||
                        !pos1.getWorld().equals(to.getWorld()) ||
                        !pos2.getWorld().equals(from.getWorld())) {
                    // Mixed worlds detected - clear history
                    playerData.previousPositions.clear();
                    return;
                }

                try {
                    Vector v1 = pos1.subtract(pos2).toVector().normalize();
                    Vector v2 = to.subtract(from).toVector().normalize();

                    double angle = v1.angle(v2);

                    // If angle change is very sharp while moving fast
                    if (angle > Math.PI * 0.8 && player.getVelocity().length() > 0.4) {
                        // Increment violation level
                        playerData.irregularMovementViolationLevel++;

                        // Log detailed debug info
                        logDetection(player, "IrregularMovement",
                                String.format("Sharp angle change: %.2f radians while moving at %.2f speed",
                                        angle, player.getVelocity().length()));

                        // Handle violation
                        handleViolation(player, "IrregularMovement",
                                playerData.irregularMovementViolationLevel, irregularMovementViolationThreshold);
                        return;
                    }
                } catch (IllegalArgumentException e) {
                    // If we still get location errors, clear the history
                    playerData.previousPositions.clear();
                    return;
                }
            }
        }

        // 2. Check for wall climbing
        if (!player.isFlying() && !player.isClimbing()) {
            boolean touchingWall = false;
            Location loc = player.getLocation();

            try {
                // Check surrounding blocks
                for (int x = -1; x <= 1; x += 2) {
                    Block block = loc.clone().add(x, 0, 0).getBlock();
                    if (block.getType().isSolid()) {
                        touchingWall = true;
                        break;
                    }
                }

                for (int z = -1; z <= 1; z += 2) {
                    Block block = loc.clone().add(0, 0, z).getBlock();
                    if (block.getType().isSolid()) {
                        touchingWall = true;
                        break;
                    }
                }

                // If touching wall and moving upward without jump
                if (touchingWall && to.getY() > from.getY() + 0.1 &&
                        !player.isOnGround() && to.getY() - playerData.lastJumpY > 1.0) {

                    // Increment violation level
                    playerData.irregularMovementViolationLevel++;

                    // Log detailed debug info
                    logDetection(player, "IrregularMovement",
                            String.format("Wall climbing detected: %.2f blocks above last jump",
                                    to.getY() - playerData.lastJumpY));

                    // Handle violation
                    handleViolation(player, "IrregularMovement",
                            playerData.irregularMovementViolationLevel, irregularMovementViolationThreshold);
                    return;
                }
            } catch (Exception e) {
                // Skip this check if there's any world-related error
                return;
            }
        }

        // If we're on ground now but weren't before, record this position as a possible jump start
        if (player.isOnGround() && !playerData.wasOnGround) {
            playerData.lastJumpY = to.getY();
        }
        playerData.wasOnGround = player.isOnGround();

        // Gradually decrease violation level if no issues detected
        if (playerData.irregularMovementViolationLevel > 0 && Math.random() < 0.05) {
            playerData.irregularMovementViolationLevel--;
        }
    }

    // Check for XRay hacks
    private void checkXRay(Player player, PlayerData playerData, BlockBreakEvent event) {
        // Update mining statistics
        playerData.totalBlocksMined++;
        Material blockType = event.getBlock().getType();

        // Store last mined location
        Location minedLocation = event.getBlock().getLocation();
        playerData.lastMinedLocations.add(minedLocation);

        // Keep only last 100 locations
        if (playerData.lastMinedLocations.size() > 100) {
            playerData.lastMinedLocations.remove(0);
        }

        // Check if this is a valuable ore
        if (valuableOres.contains(blockType)) {
            playerData.valuableBlocksMined++;
            playerData.valuableOreLocations.add(minedLocation);

            // Keep only last 50 valuable locations
            if (playerData.valuableOreLocations.size() > 50) {
                playerData.valuableOreLocations.remove(0);
            }

            // Check for direct tunneling pattern
            if (playerData.valuableOreLocations.size() >= 3) {
                boolean directTunneling = checkDirectTunneling(playerData.valuableOreLocations);
                if (directTunneling) {
                    // Increment violation level
                    playerData.xrayViolationLevel++;

                    // Log detailed debug info
                    logDetection(player, "XRay", "Direct tunneling to valuable ores detected");

                    // Handle violation
                    handleViolation(player, "XRay", playerData.xrayViolationLevel, xrayViolationThreshold);
                    return;
                }
            }

            // Only check ratio after we have enough sample size
            if (playerData.totalBlocksMined >= miningSampleSize) {
                double ratio = playerData.getValuableRatio();

                // If the ratio exceeds the threshold, flag as potential XRay
                if (ratio > xrayThresholdRatio) {
                    // Increment violation level
                    playerData.xrayViolationLevel++;

                    // Log detailed debug info
                    logDetection(player, "XRay",
                            String.format("%d valuable ores out of %d blocks mined (%.2f%%, threshold: %.2f%%)",
                                    playerData.valuableBlocksMined,
                                    playerData.totalBlocksMined,
                                    ratio * 100,
                                    xrayThresholdRatio * 100));

                    // Handle violation
                    handleViolation(player, "XRay", playerData.xrayViolationLevel, xrayViolationThreshold);
                }
            }
        }
    }

    // Check for direct tunneling to ores (XRay pattern)
    private boolean checkDirectTunneling(List<Location> oreLocations) {
        // We need at least 3 locations to check for patterns
        if (oreLocations.size() < 3) {
            return false;
        }

        int patternCount = 0;

        // Check the last few ore locations
        int endIdx = oreLocations.size() - 1;
        for (int i = endIdx; i >= 2 && i > endIdx - 5; i--) {
            Location current = oreLocations.get(i);
            Location prev1 = oreLocations.get(i-1);
            Location prev2 = oreLocations.get(i-2);

            // Calculate distances
            double dist1 = current.distance(prev1);
            double dist2 = prev1.distance(prev2);

            // Check if the player is tunneling in a straight line to ores
            // with unusual distance patterns (typical of XRay)
            if (dist1 > 3 && dist2 > 3) {
                // Calculate vectors
                Vector v1 = current.toVector().subtract(prev1.toVector()).normalize();
                Vector v2 = prev1.toVector().subtract(prev2.toVector()).normalize();

                // Check if directions are similar (tunneling in same direction)
                double angle = v1.angle(v2);
                if (angle < Math.PI / 4) { // Less than 45 degrees change
                    patternCount++;
                }
            }
        }

        // If we've found multiple instances of this pattern
        return patternCount >= 2;
    }

    // Check for KillAura hack
    private void checkKillAura(Player player, PlayerData playerData, EntityDamageByEntityEvent event) {
        Entity target = event.getEntity();
        long currentTime = System.currentTimeMillis();

        // Check attack rate
        if (playerData.lastAttackTime > 0) {
            long timeDiff = currentTime - playerData.lastAttackTime;

            // If attacks are too quick (inhuman reaction time)
            if (timeDiff < 120) { // Less than 120ms between attacks
                playerData.killAuraViolationLevel++;

                // Log detailed debug info
                logDetection(player, "KillAura",
                        String.format("Attack interval: %dms (too fast)", timeDiff));

                // Handle violation
                handleViolation(player, "KillAura", playerData.killAuraViolationLevel, killAuraViolationThreshold);
            }
        }

        // Check attack angle (if player attacked multiple entities in sequence)
        if (playerData.lastAttackedEntity != null && playerData.lastAttackedEntity != target) {
            // Calculate angle between last target and current target
            Vector v1 = player.getLocation().getDirection();
            Vector v2 = target.getLocation().subtract(player.getLocation()).toVector().normalize();

            double angle = v1.angle(v2);

            // If player is attacking entities that are not in line of sight
            if (angle > Math.PI / 2) { // More than 90 degrees
                playerData.killAuraViolationLevel++;

                // Log detailed debug info
                logDetection(player, "KillAura",
                        String.format("Attack angle: %.2f degrees (suspicious targeting)", angle * 180 / Math.PI));

                // Handle violation
                handleViolation(player, "KillAura", playerData.killAuraViolationLevel, killAuraViolationThreshold);
            }
        }

        // Update last attack data
        playerData.lastAttackTime = currentTime;
        playerData.lastAttackedEntity = target;

        // Gradually decrease violation level
        if (playerData.killAuraViolationLevel > 0 && Math.random() < 0.1) {
            playerData.killAuraViolationLevel--;
        }
    }

    // Check for NoFall hack
    private void checkNoFall(Player player, PlayerData playerData, EntityDamageEvent event) {
        // If player took significantly less damage than expected
        float fallDistance = player.getFallDistance();
        float damage = (float) event.getDamage();

        // Expected damage calculation (approximate Minecraft formula)
        float expectedDamage = fallDistance - 3.0f; // Fall damage starts at 3 blocks

        // If the damage is much lower than expected (with some tolerance)
        if (expectedDamage > 0 && damage < expectedDamage * 0.6) {
            playerData.noFallViolationLevel++;

            // Log detailed debug info
            logDetection(player, "NoFall",
                    String.format("Fall distance: %.2f blocks, Damage taken: %.2f, Expected: %.2f",
                            fallDistance, damage, expectedDamage));

            // Handle violation
            handleViolation(player, "NoFall", playerData.noFallViolationLevel, noFallViolationThreshold);
        }
    }

    // Check for Timer hack (manipulation of client-side tick rate)
    private void checkTimer(Player player, PlayerData playerData) {
        // We need enough movement samples
        if (playerData.previousPositions.size() < 10) {
            return;
        }

        // Calculate average movement frequency
        long currentTick = player.getWorld().getFullTime();
        long timeSinceLastCheck = currentTick - playerData.lastTimerCheck;

        // Only check every 20 ticks (1 second)
        if (timeSinceLastCheck < 20) {
            return;
        }

        // Count how many positions were recorded in this time period
        int moveCount = 0;
        for (int i = playerData.previousPositions.size() - 1; i >= 0; i--) {
            if (currentTick - playerData.previousPositions.get(i).getWorld().getFullTime() <= timeSinceLastCheck) {
                moveCount++;
            }
        }

        // Expected move count (approximately 1 per tick)
        int expectedMoveCount = (int) timeSinceLastCheck;

        // If significantly more movements than expected
        if (moveCount > expectedMoveCount * 1.3 && moveCount > expectedMoveCount + 3) {
            playerData.timerViolationLevel++;

            // Log detailed debug info
            logDetection(player, "Timer",
                    String.format("Movements: %d, Expected: %d in %d ticks",
                            moveCount, expectedMoveCount, timeSinceLastCheck));

            // Handle violation
            handleViolation(player, "Timer", playerData.timerViolationLevel, timerViolationThreshold);
        }

        // Update last check time
        playerData.lastTimerCheck = currentTick;
    }

    // Check for Scaffold hack (suspicious block placement patterns)
    private void checkScaffold(Player player, PlayerData playerData, BlockPlaceEvent event) {
        long currentTime = System.currentTimeMillis();

        // Record this block placement
        playerData.lastPlacedLocations.add(event.getBlockPlaced().getLocation());
        playerData.lastPlaceTimes.add(currentTime);

        // Keep only last 20 placements
        while (playerData.lastPlacedLocations.size() > 20) {
            playerData.lastPlacedLocations.remove(0);
            playerData.lastPlaceTimes.remove(0);
        }

        // Skip if we don't have enough data
        if (playerData.lastPlacedLocations.size() < 3 || playerData.lastPlaceTimes.size() < 3) {
            return;
        }

        // Check for placement speed
        boolean speedViolation = false;
        if (playerData.lastPlaceTimes.size() >= 2) {
            long timeDiff = playerData.lastPlaceTimes.get(playerData.lastPlaceTimes.size() - 1) -
                    playerData.lastPlaceTimes.get(playerData.lastPlaceTimes.size() - 2);

            // If blocks are being placed too quickly
            if (timeDiff < 100) { // Less than 100ms between placements
                speedViolation = true;
            }
        }

        // Check for placement patterns
        boolean patternViolation = false;
        if (playerData.lastPlacedLocations.size() >= 3) {
            // Check if blocks are being placed in a perfect line while moving
            List<Location> locations = playerData.lastPlacedLocations;
            int last = locations.size() - 1;

            // Calculate vectors
            Vector v1 = locations.get(last).toVector().subtract(locations.get(last-1).toVector()).normalize();
            Vector v2 = locations.get(last-1).toVector().subtract(locations.get(last-2).toVector()).normalize();

            // Check if directions are very similar (straight line)
            double angle = v1.angle(v2);
            if (angle < Math.PI / 10) { // Less than 18 degrees difference
                // Check if player was looking away while placing (suspicious behavior)
                Vector lookDir = player.getLocation().getDirection();
                Vector placeDir = event.getBlockPlaced().getLocation().subtract(player.getLocation()).toVector().normalize();

                double lookAngle = lookDir.angle(placeDir);
                if (lookAngle > Math.PI / 2) { // Looking more than 90 degrees away
                    patternViolation = true;
                }
            }
        }

        // If any type of violation was detected
        if (speedViolation || patternViolation) {
            playerData.scaffoldViolationLevel++;

            // Log detailed debug info
            String reason = speedViolation ? "Fast placement" : "Suspicious pattern";
            logDetection(player, "Scaffold", reason);

            // Handle violation
            handleViolation(player, "Scaffold", playerData.scaffoldViolationLevel, scaffoldViolationThreshold);
        } else {
            // Gradually decrease violation level
            if (playerData.scaffoldViolationLevel > 0 && Math.random() < 0.1) {
                playerData.scaffoldViolationLevel--;
            }
        }
    }

    // Handle violations
    private void handleViolation(Player player, String cheatType, int violationLevel, int threshold) {
        // CRITICAL: Ensure this method NEVER teleports or modifies player locations

        // Alert admins
        if (violationLevel % 3 == 0 || violationLevel >= threshold) { // Alert every 3 violations or when threshold is reached
            String alert = adminAlertMessage
                    .replace("%player%", player.getName())
                    .replace("%cheat%", cheatType)
                    .replace("%violations%", String.valueOf(violationLevel));

            // Send alert to all players with admin permission
            for (UUID uuid : alertReceivers) {
                Player admin = Bukkit.getPlayer(uuid);
                if (admin != null && admin.isOnline()) {
                    admin.sendMessage(alert);
                }
            }

            // Log to console
            if (logToConsole) {
                logger.warning(ChatColor.stripColor(alert));
            }
        }

        // Warn player
        if (violationLevel == threshold / 2) {
            player.sendMessage(warningMessage);
        }

        // Apply punishment if violation level exceeds threshold
        if (violationLevel >= threshold) {
            // Get the configured punishment type for this cheat
            String action = punishmentActions.getOrDefault(cheatType.toLowerCase(), "KICK");

            switch (action) {
                case "NONE":
                    // Just log the violation but take no action
                    logger.warning(player.getName() + " reached " + cheatType + " violation threshold, but no action is configured.");
                    break;

                case "WARN":
                    // Send a warning message to the player
                    player.sendMessage(warningMessage);

                    // Log the warning
                    logger.warning(player.getName() + " was warned for " + cheatType + " (" + violationLevel + " violations)");
                    break;

                case "KICK":
                    // Prepare kick message
                    String reason = kickMessage.replace("%cheat%", cheatType);

                    // Schedule kick on main thread with delay to avoid interfering with movement
                    Bukkit.getScheduler().runTaskLater(this, () -> {
                        if (player.isOnline()) {
                            player.kickPlayer(reason);
                        }
                    }, 5L); // 5 tick delay

                    // Log the kick
                    logger.warning(player.getName() + " was kicked for " + cheatType + " (" + violationLevel + " violations)");
                    break;

                case "BAN":
                    // Format ban duration
                    String duration = formatBanDuration(banDuration);

                    // Prepare ban message
                    String banReason = banMessage
                            .replace("%cheat%", cheatType)
                            .replace("%duration%", duration);

                    // Schedule ban on main thread with delay
                    Bukkit.getScheduler().runTaskLater(this, () -> {
                        if (player.isOnline()) {
                            // Ban the player
                            if (banDuration <= 0) {
                                // Permanent ban
                                Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(
                                        player.getName(),
                                        ChatColor.stripColor(banReason),
                                        null, // No expiration (permanent)
                                        "AntiCheatX"
                                );
                            } else {
                                // Temporary ban
                                java.util.Date expiration = new java.util.Date(System.currentTimeMillis() + banDuration * 60 * 1000);
                                Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(
                                        player.getName(),
                                        ChatColor.stripColor(banReason),
                                        expiration,
                                        "AntiCheatX"
                                );
                            }

                            // Kick player to enforce the ban
                            player.kickPlayer(banReason);
                        }
                    }, 5L); // 5 tick delay

                    // Log the ban
                    logger.warning(player.getName() + " was banned for " + cheatType + " (" + violationLevel + " violations, " + duration + ")");

                    // Reset violations if configured
                    if (resetAfterBan) {
                        PlayerData playerData = playerDataMap.get(player.getUniqueId());
                        if (playerData != null) {
                            playerData.resetViolations();
                        }
                    }
                    break;

                default:
                    // Unknown action type, default to kick
                    Bukkit.getScheduler().runTaskLater(this, () -> {
                        if (player.isOnline()) {
                            player.kickPlayer(kickMessage.replace("%cheat%", cheatType));
                        }
                    }, 5L);
                    logger.warning("Unknown punishment action '" + action + "' for " + cheatType + ", defaulting to KICK");
                    break;
            }
        }
    }

    // Log detection details to file/console
    private void logDetection(Player player, String cheatType, String details) {
        String message = String.format("[%s] %s detected for %s: %s",
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),
                cheatType,
                player.getName(),
                details);

        // Log to console
        if (logToConsole) {
            logger.info(message);
        }

        // Log to file
        if (logToFile && logWriter != null) {
            try {
                logWriter.write(message + "\n");
                logWriter.flush();
            } catch (IOException e) {
                logger.warning("Failed to write to log file: " + e.getMessage());
            }
        }
    }

    // Format ban duration for display
    private String formatBanDuration(int minutes) {
        if (minutes <= 0) {
            return "Permanent";
        }

        if (minutes < 60) {
            return minutes + " minute" + (minutes == 1 ? "" : "s");
        }

        int hours = minutes / 60;
        if (hours < 24) {
            return hours + " hour" + (hours == 1 ? "" : "s");
        }

        int days = hours / 24;
        return days + " day" + (days == 1 ? "" : "s");
    }

    // Command handler
    private class AntiCheatXCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!sender.hasPermission("anticheatx.admin")) {
                sender.sendMessage(prefix + ChatColor.RED + " You don't have permission to use this command.");
                return true;
            }

            if (args.length == 0) {
                // Display help
                sender.sendMessage(ChatColor.GRAY + "=== " + prefix + ChatColor.GRAY + " Commands ===");
                sender.sendMessage(ChatColor.YELLOW + "/acx reload" + ChatColor.GRAY + " - Reload the entire plugin");
                sender.sendMessage(ChatColor.YELLOW + "/acx status" + ChatColor.GRAY + " - Show plugin status");
                sender.sendMessage(ChatColor.YELLOW + "/acx violations <player>" + ChatColor.GRAY + " - Show player violations");
                sender.sendMessage(ChatColor.YELLOW + "/acx alerts" + ChatColor.GRAY + " - Toggle violation alerts");
                return true;
            }

            if (args[0].equalsIgnoreCase("reload")) {
                // Reload plugin
                boolean success = ((antiCheatX) Bukkit.getPluginManager().getPlugin("AntiCheatX")).reloadPlugin();
                if (success) {
                    sender.sendMessage(prefix + ChatColor.GREEN + " Plugin successfully reloaded!");
                } else {
                    sender.sendMessage(prefix + ChatColor.RED + " Error reloading plugin. Check console for details.");
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("status")) {
                // Show plugin status
                sender.sendMessage(ChatColor.GRAY + "=== " + prefix + ChatColor.GRAY + " Status ===");
                sender.sendMessage(ChatColor.YELLOW + "Version: " + ChatColor.WHITE + "1.0.0");
                sender.sendMessage(ChatColor.YELLOW + "Players monitored: " + ChatColor.WHITE + playerDataMap.size());
                sender.sendMessage(ChatColor.YELLOW + "Welcome message: " + ChatColor.WHITE + (welcomeMessageEnabled ? "Enabled" : "Disabled"));
                sender.sendMessage(ChatColor.YELLOW + "Server name: " + ChatColor.WHITE + serverName);
                sender.sendMessage(ChatColor.YELLOW + "Thresholds:");
                sender.sendMessage(ChatColor.GRAY + " - Fly: " + ChatColor.WHITE + flyViolationThreshold);
                sender.sendMessage(ChatColor.GRAY + " - Speed: " + ChatColor.WHITE + speedViolationThreshold);
                sender.sendMessage(ChatColor.GRAY + " - Reach: " + ChatColor.WHITE + reachViolationThreshold);
                sender.sendMessage(ChatColor.GRAY + " - XRay: " + ChatColor.WHITE + xrayViolationThreshold);
                sender.sendMessage(ChatColor.GRAY + " - Irregular Movement: " + ChatColor.WHITE + irregularMovementViolationThreshold);
                sender.sendMessage(ChatColor.GRAY + " - KillAura: " + ChatColor.WHITE + killAuraViolationThreshold);
                sender.sendMessage(ChatColor.GRAY + " - NoFall: " + ChatColor.WHITE + noFallViolationThreshold);
                sender.sendMessage(ChatColor.GRAY + " - Timer: " + ChatColor.WHITE + timerViolationThreshold);
                sender.sendMessage(ChatColor.GRAY + " - Scaffold: " + ChatColor.WHITE + scaffoldViolationThreshold);
                return true;
            }

            if (args[0].equalsIgnoreCase("violations") && args.length > 1) {
                // Show player violations
                Player targetPlayer = Bukkit.getPlayer(args[1]);
                if (targetPlayer == null) {
                    sender.sendMessage(prefix + ChatColor.RED + " Player not found.");
                    return true;
                }

                PlayerData playerData = playerDataMap.get(targetPlayer.getUniqueId());
                if (playerData == null) {
                    sender.sendMessage(prefix + ChatColor.RED + " No data for player.");
                    return true;
                }

                sender.sendMessage(ChatColor.GRAY + "=== " + prefix + ChatColor.GRAY + " Violations for " + targetPlayer.getName() + " ===");
                sender.sendMessage(ChatColor.GRAY + "Fly: " + getViolationColor(playerData.flyViolationLevel, flyViolationThreshold) + playerData.flyViolationLevel);
                sender.sendMessage(ChatColor.GRAY + "Speed: " + getViolationColor(playerData.speedViolationLevel, speedViolationThreshold) + playerData.speedViolationLevel);
                sender.sendMessage(ChatColor.GRAY + "Reach: " + getViolationColor(playerData.reachViolationLevel, reachViolationThreshold) + playerData.reachViolationLevel);
                sender.sendMessage(ChatColor.GRAY + "XRay: " + getViolationColor(playerData.xrayViolationLevel, xrayViolationThreshold) + playerData.xrayViolationLevel);
                sender.sendMessage(ChatColor.GRAY + "Irregular Movement: " + getViolationColor(playerData.irregularMovementViolationLevel, irregularMovementViolationThreshold) + playerData.irregularMovementViolationLevel);
                sender.sendMessage(ChatColor.GRAY + "KillAura: " + getViolationColor(playerData.killAuraViolationLevel, killAuraViolationThreshold) + playerData.killAuraViolationLevel);
                sender.sendMessage(ChatColor.GRAY + "NoFall: " + getViolationColor(playerData.noFallViolationLevel, noFallViolationThreshold) + playerData.noFallViolationLevel);
                sender.sendMessage(ChatColor.GRAY + "Timer: " + getViolationColor(playerData.timerViolationLevel, timerViolationThreshold) + playerData.timerViolationLevel);
                sender.sendMessage(ChatColor.GRAY + "Scaffold: " + getViolationColor(playerData.scaffoldViolationLevel, scaffoldViolationThreshold) + playerData.scaffoldViolationLevel);

                // Additional mining stats if the player has some mining activity
                if (playerData.totalBlocksMined > 0) {
                    double ratio = playerData.getValuableRatio() * 100; // Convert to percentage
                    sender.sendMessage(ChatColor.GRAY + "Mining Stats: " +
                            ChatColor.WHITE + playerData.valuableBlocksMined +
                            ChatColor.GRAY + " valuable ores out of " +
                            ChatColor.WHITE + playerData.totalBlocksMined +
                            ChatColor.GRAY + " total blocks (" +
                            ChatColor.YELLOW + String.format("%.2f%%", ratio) +
                            ChatColor.GRAY + ")");
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("alerts")) {
                // Toggle alerts for this sender
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    UUID uuid = player.getUniqueId();

                    if (alertReceivers.contains(uuid)) {
                        alertReceivers.remove(uuid);
                        sender.sendMessage(prefix + ChatColor.YELLOW + " Alerts disabled.");
                    } else {
                        alertReceivers.add(uuid);
                        sender.sendMessage(prefix + ChatColor.GREEN + " Alerts enabled.");
                    }
                } else {
                    sender.sendMessage(prefix + ChatColor.RED + " This command can only be used by players.");
                }
                return true;
            }

            // Unknown command, show help
            sender.sendMessage(prefix + ChatColor.RED + " Unknown command. Use /acx for help.");
            return true;
        }

        private ChatColor getViolationColor(int level, int threshold) {
            if (level >= threshold) {
                return ChatColor.DARK_RED;
            } else if (level >= threshold / 2) {
                return ChatColor.RED;
            } else if (level > 0) {
                return ChatColor.YELLOW;
            } else {
                return ChatColor.GREEN;
            }
        }
    }

    // Player data class
    private static class PlayerData {
        // Last activity time (for cleanup)
        long lastActivityTime = System.currentTimeMillis();

        // Current world tracking for multiverse support
        String currentWorld = null;

        // Violation levels
        int flyViolationLevel = 0;
        int speedViolationLevel = 0;
        int reachViolationLevel = 0;
        int xrayViolationLevel = 0;
        int irregularMovementViolationLevel = 0;
        int killAuraViolationLevel = 0;
        int noFallViolationLevel = 0;
        int timerViolationLevel = 0;
        int scaffoldViolationLevel = 0;

        // Fly detection
        long lastOffGroundTime = 0;

        // Movement tracking
        List<Location> previousPositions = new ArrayList<>();
        long lastMovementTick = 0;
        boolean wasOnGround = true;
        double lastJumpY = 0;

        // XRay detection
        int totalBlocksMined = 0;
        int valuableBlocksMined = 0;
        List<Location> lastMinedLocations = new ArrayList<>();
        List<Location> valuableOreLocations = new ArrayList<>();

        // KillAura detection
        long lastAttackTime = 0;
        Entity lastAttackedEntity = null;

        // Timer detection
        long lastTimerCheck = 0;

        // Scaffold detection
        List<Location> lastPlacedLocations = new ArrayList<>();
        List<Long> lastPlaceTimes = new ArrayList<>();

        // Calculate valuable ore ratio
        double getValuableRatio() {
            if (totalBlocksMined == 0) {
                return 0.0;
            }
            return (double) valuableBlocksMined / totalBlocksMined;
        }

        // Reset mining statistics (for periodic reset)
        void resetMiningStats() {
            totalBlocksMined = 0;
            valuableBlocksMined = 0;
            lastMinedLocations.clear();
            valuableOreLocations.clear();
        }

        // Reset all violation levels
        void resetViolations() {
            flyViolationLevel = 0;
            speedViolationLevel = 0;
            reachViolationLevel = 0;
            xrayViolationLevel = 0;
            irregularMovementViolationLevel = 0;
            killAuraViolationLevel = 0;
            noFallViolationLevel = 0;
            timerViolationLevel = 0;
            scaffoldViolationLevel = 0;
        }

        // Clean up world-specific data when player changes worlds
        void resetWorldData() {
            previousPositions.clear();
            lastMinedLocations.clear();
            valuableOreLocations.clear();
            lastPlacedLocations.clear();
            lastOffGroundTime = 0;
            lastMovementTick = 0;
        }
    }
}