package com.gaming.enhancedagar.game;

import android.graphics.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sistema completo de power-ups para EnhancedAgar
 * Implementa power-ups temporales únicos con efectos visuales e interacciones
 */
public class PowerUpSystem {
    
    // Tipos de power-ups disponibles
    public enum PowerUpType {
        SPEED_BOOST("Velocidad", Color.CYAN, 1.0f),
        SHIELD("Escudo", Color.YELLOW, 0.8f),
        MULTIPLIER("Multiplicador", Color.MAGENTA, 0.6f),
        FREE_SPLIT("División Libre", Color.ORANGE, 0.4f),
        INVISIBILITY("Invisibilidad", Color.WHITE, 0.2f);
        
        private final String displayName;
        private final Color color;
        private final float spawnWeight;
        
        PowerUpType(String displayName, Color color, float spawnWeight) {
            this.displayName = displayName;
            this.color = color;
            this.spawnWeight = spawnWeight;
        }
        
        public String getDisplayName() { return displayName; }
        public Color getColor() { return color; }
        public float getSpawnWeight() { return spawnWeight; }
    }
    
    // Rareza de power-ups
    public enum Rarity {
        COMMON(1.0f, Color.GREEN),
        UNCOMMON(0.6f, Color.BLUE),
        RARE(0.3f, Color.PURPLE),
        LEGENDARY(0.1f, Color.RED),
        MYTHIC(0.05f, Color.GOLD);
        
        private final float spawnRate;
        private final Color color;
        
        Rarity(float spawnRate, Color color) {
            this.spawnRate = spawnRate;
            this.color = color;
        }
        
        public float getSpawnRate() { return spawnRate; }
        public Color getColor() { return color; }
    }
    
    // Configuración de power-ups
    public static class PowerUpConfig {
        public final PowerUpType type;
        public final Rarity rarity;
        public final int duration; // en ticks
        public final int cooldown; // en ticks
        public final String effectDescription;
        public final float effectMultiplier;
        public final boolean exclusivePerType; // No puede coexistir con power-ups del mismo tipo
        
        public PowerUpConfig(PowerUpType type, Rarity rarity, int duration, int cooldown, 
                           String effectDescription, float effectMultiplier, boolean exclusivePerType) {
            this.type = type;
            this.rarity = rarity;
            this.duration = duration;
            this.cooldown = cooldown;
            this.effectDescription = effectDescription;
            this.effectMultiplier = effectMultiplier;
            this.exclusivePerType = exclusivePerType;
        }
    }
    
    // Clase para representar un power-up activo
    public static class ActivePowerUp {
        public final PowerUpConfig config;
        public final long startTime;
        public final Player player;
        private int remainingDuration;
        private boolean isActive;
        
        public ActivePowerUp(PowerUpConfig config, Player player, int duration) {
            this.config = config;
            this.player = player;
            this.startTime = System.currentTimeMillis();
            this.remainingDuration = duration;
            this.isActive = true;
        }
        
        public void update() {
            if (remainingDuration > 0) {
                remainingDuration--;
            } else {
                isActive = false;
            }
        }
        
        public void forceDeactivate() {
            isActive = false;
            remainingDuration = 0;
        }
        
        public float getTimeRemaining() {
            return remainingDuration / 60.0f; // Convertir a segundos (60 FPS)
        }
        
        public float getProgress() {
            return 1.0f - (float) remainingDuration / config.duration;
        }
        
        public boolean isExpired() {
            return !isActive || remainingDuration <= 0;
        }
        
        public boolean isActive() {
            return isActive;
        }
    }
    
    // Clase para power-ups en el mapa
    public static class MapPowerUp {
        public final PowerUpConfig config;
        public final Point position;
        private boolean collected;
        private long spawnTime;
        private double rotation;
        
        public MapPowerUp(PowerUpConfig config, Point position) {
            this.config = config;
            this.position = position;
            this.collected = false;
            this.spawnTime = System.currentTimeMillis();
            this.rotation = 0;
        }
        
        public void update() {
            rotation += 0.05;
        }
        
        public void collect() {
            this.collected = true;
        }
        
        public boolean isCollected() {
            return collected;
        }
        
        public double getRotation() {
            return rotation;
        }
    }
    
    // Estado del sistema
    private final List<MapPowerUp> activeMapPowerUps;
    private final Map<Player, List<ActivePowerUp>> playerPowerUps;
    private final Map<PowerUpType, Integer> lastSpawnTime;
    private final Random random;
    private final Rectangle gameBounds;
    
    // Configuraciones predefinidas de power-ups
    private static final Map<PowerUpType, PowerUpConfig> BASE_CONFIGS;
    
    static {
        BASE_CONFIGS = new HashMap<>();
        // Speed Boost
        BASE_CONFIGS.put(PowerUpType.SPEED_BOOST, new PowerUpConfig(
            PowerUpType.SPEED_BOOST, Rarity.COMMON, 600, 300, 
            "Aumenta la velocidad de movimiento", 1.5f, true
        ));
        
        // Shield
        BASE_CONFIGS.put(PowerUpType.SHIELD, new PowerUpConfig(
            PowerUpType.SHIELD, Rarity.UNCOMMON, 900, 450,
            "Protege contra división forzada", 1.0f, true
        ));
        
        // Multiplier
        BASE_CONFIGS.put(PowerUpType.MULTIPLIER, new PowerUpConfig(
            PowerUpType.MULTIPLIER, Rarity.RARE, 450, 600,
            "Masa ganada multiplicada temporalmente", 2.0f, true
        ));
        
        // Free Split
        BASE_CONFIGS.put(PowerUpType.FREE_SPLIT, new PowerUpConfig(
            PowerUpType.FREE_SPLIT, Rarity.UNCOMMON, 300, 200,
            "Divisiones sin restricciones", 1.0f, true
        ));
        
        // Invisibility
        BASE_CONFIGS.put(PowerUpType.INVISIBILITY, new PowerUpConfig(
            PowerUpType.INVISIBILITY, Rarity.LEGENDARY, 600, 900,
            "Invisible a otros jugadores temporalmente", 1.0f, true
        ));
    }
    
    // Constructor
    public PowerUpSystem(Rectangle gameBounds) {
        this.activeMapPowerUps = new ArrayList<>();
        this.playerPowerUps = new HashMap<>();
        this.lastSpawnTime = new HashMap<>();
        this.random = new Random();
        this.gameBounds = gameBounds;
        
        // Inicializar cooldowns de spawn
        for (PowerUpType type : PowerUpType.values()) {
            lastSpawnTime.put(type, 0);
        }
    }
    
    /**
     * Actualiza todo el sistema de power-ups
     */
    public void update() {
        // Actualizar power-ups en el mapa
        for (MapPowerUp powerUp : activeMapPowerUps) {
            if (!powerUp.isCollected()) {
                powerUp.update();
            }
        }
        
        // Actualizar power-ups de jugadores
        for (List<ActivePowerUp> powerUps : playerPowerUps.values()) {
            for (ActivePowerUp powerUp : powerUps) {
                powerUp.update();
                if (powerUp.isExpired()) {
                    applyPowerUpEndEffects(powerUp);
                }
            }
            // Remover power-ups expirados
            powerUps.removeIf(ActivePowerUp::isExpired);
        }
        
        // Spawn automático de power-ups
        spawnRandomPowerUps();
    }
    
    /**
     * Genera power-ups aleatorios en el mapa
     */
    private void spawnRandomPowerUps() {
        for (PowerUpType type : PowerUpType.values()) {
            if (shouldSpawnPowerUp(type)) {
                spawnPowerUp(type);
                lastSpawnTime.put(type, (int) System.currentTimeMillis());
            }
        }
    }
    
    /**
     * Determina si debe spawnear un power-up del tipo especificado
     */
    private boolean shouldSpawnPowerUp(PowerUpType type) {
        PowerUpConfig config = BASE_CONFIGS.get(type);
        int lastSpawn = lastSpawnTime.get(type);
        long currentTime = System.currentTimeMillis();
        
        // Verificar cooldown de spawn
        long timeSinceLastSpawn = currentTime - lastSpawn;
        long minSpawnInterval = getMinSpawnInterval(config.rarity);
        
        if (timeSinceLastSpawn < minSpawnInterval) {
            return false;
        }
        
        // Verificar límite de power-ups en mapa
        long maxPowerUpsInMap = getMaxPowerUpsInMap();
        if (activeMapPowerUps.stream().filter(p -> !p.isCollected()).count() >= maxPowerUpsInMap) {
            return false;
        }
        
        // Probabilidad basada en rareza
        return random.nextFloat() < config.rarity.getSpawnRate();
    }
    
    /**
     * Spawnea un power-up del tipo especificado
     */
    private void spawnPowerUp(PowerUpType type) {
        PowerUpConfig config = BASE_CONFIGS.get(type);
        Point position = findValidSpawnPosition();
        
        if (position != null) {
            activeMapPowerUps.add(new MapPowerUp(config, position));
        }
    }
    
    /**
     * Encuentra una posición válida para spawnear
     */
    private Point findValidSpawnPosition() {
        int maxAttempts = 10;
        int margin = 50;
        
        for (int i = 0; i < maxAttempts; i++) {
            int x = random.nextInt(gameBounds.width - 2 * margin) + margin;
            int y = random.nextInt(gameBounds.height - 2 * margin) + margin;
            Point pos = new Point(x, y);
            
            // Verificar que no esté demasiado cerca de otros power-ups
            if (isValidPosition(pos)) {
                return pos;
            }
        }
        return null;
    }
    
    /**
     * Verifica si una posición es válida para spawnear
     */
    private boolean isValidPosition(Point position) {
        int minDistance = 100;
        
        for (MapPowerUp powerUp : activeMapPowerUps) {
            if (!powerUp.isCollected()) {
                double distance = position.distance(powerUp.position);
                if (distance < minDistance) {
                    return false;
                }
            }
        }
        return true;
    }
    
    /**
     * Obtiene el intervalo mínimo de spawn basado en la rareza
     */
    private long getMinSpawnInterval(Rarity rarity) {
        switch (rarity) {
            case COMMON: return 2000;      // 2 segundos
            case UNCOMMON: return 5000;    // 5 segundos
            case RARE: return 10000;       // 10 segundos
            case LEGENDARY: return 20000;  // 20 segundos
            case MYTHIC: return 60000;     // 60 segundos
            default: return 5000;
        }
    }
    
    /**
     * Obtiene el número máximo de power-ups en el mapa
     */
    private long getMaxPowerUpsInMap() {
        return 15; // Límite fijo por simplicidad
    }
    
    /**
     * Intenta recoger un power-up para un jugador
     */
    public boolean tryCollectPowerUp(Player player, Point pickupPoint) {
        for (MapPowerUp powerUp : activeMapPowerUps) {
            if (!powerUp.isCollected() && isInPickupRange(player, pickupPoint, powerUp)) {
                powerUp.collect();
                return activatePowerUp(player, powerUp.config);
            }
        }
        return false;
    }
    
    /**
     * Verifica si un power-up está en rango de recogida
     */
    private boolean isInPickupRange(Player player, Point pickupPoint, MapPowerUp powerUp) {
        double distance = pickupPoint.distance(powerUp.position);
        return distance < 30; // Rango de recogida
    }
    
    /**
     * Activa un power-up para un jugador
     */
    private boolean activatePowerUp(Player player, PowerUpConfig config) {
        // Verificar exclusividad por tipo
        List<ActivePowerUp> playerPowerUps = getPlayerPowerUps(player);
        
        if (config.exclusivePerType) {
            // Remover power-ups del mismo tipo
            playerPowerUps.removeIf(p -> p.config.type == config.type);
        }
        
        // Verificar cooldowns personales
        if (isOnCooldown(player, config.type)) {
            return false;
        }
        
        // Crear y activar el nuevo power-up
        ActivePowerUp activePowerUp = new ActivePowerUp(config, player, config.duration);
        playerPowerUps.add(activePowerUp);
        
        // Aplicar efectos inmediatamente
        applyPowerUpStartEffects(activePowerUp);
        
        return true;
    }
    
    /**
     * Verifica si un power-up está en cooldown para el jugador
     */
    private boolean isOnCooldown(Player player, PowerUpType type) {
        List<ActivePowerUp> powerUps = getPlayerPowerUps(player);
        
        for (ActivePowerUp powerUp : powerUps) {
            if (powerUp.config.type == type) {
                return false; // Ya tiene este tipo activo
            }
        }
        
        // Aquí se podría implementar lógica de cooldown personal
        // Por simplicidad, no implementamos cooldown personal en esta versión
        return false;
    }
    
    /**
     * Aplica los efectos de inicio de un power-up
     */
    private void applyPowerUpStartEffects(ActivePowerUp powerUp) {
        Player player = powerUp.player;
        PowerUpConfig config = powerUp.config;
        
        switch (config.type) {
            case SPEED_BOOST:
                player.setSpeedMultiplier(player.getSpeedMultiplier() * config.effectMultiplier);
                break;
            case SHIELD:
                player.setShielded(true);
                break;
            case MULTIPLIER:
                player.setMassMultiplier(player.getMassMultiplier() * config.effectMultiplier);
                break;
            case FREE_SPLIT:
                player.setFreeSplitEnabled(true);
                break;
            case INVISIBILITY:
                player.setInvisible(true);
                break;
        }
    }
    
    /**
     * Aplica los efectos de fin de un power-up
     */
    private void applyPowerUpEndEffects(ActivePowerUp powerUp) {
        Player player = powerUp.player;
        PowerUpConfig config = powerUp.config;
        
        switch (config.type) {
            case SPEED_BOOST:
                player.setSpeedMultiplier(player.getSpeedMultiplier() / config.effectMultiplier);
                break;
            case SHIELD:
                player.setShielded(false);
                break;
            case MULTIPLIER:
                player.setMassMultiplier(player.getMassMultiplier() / config.effectMultiplier);
                break;
            case FREE_SPLIT:
                player.setFreeSplitEnabled(false);
                break;
            case INVISIBILITY:
                player.setInvisible(false);
                break;
        }
    }
    
    /**
     * Obtiene los power-ups activos de un jugador
     */
    private List<ActivePowerUp> getPlayerPowerUps(Player player) {
        return playerPowerUps.computeIfAbsent(player, k -> new ArrayList<>());
    }
    
    /**
     * Obtiene la lista de power-ups en el mapa
     */
    public List<MapPowerUp> getMapPowerUps() {
        return new ArrayList<>(activeMapPowerUps);
    }
    
    /**
     * Limpia power-ups recogidos del mapa
     */
    public void cleanCollectedPowerUps() {
        activeMapPowerUps.removeIf(MapPowerUp::isCollected);
    }
    
    /**
     * Obtiene información de power-ups para UI
     */
    public PowerUpInfo getPlayerPowerUpInfo(Player player) {
        List<ActivePowerUp> powerUps = getPlayerPowerUps(player);
        List<PowerUpStatus> statuses = new ArrayList<>();
        
        for (ActivePowerUp powerUp : powerUps) {
            statuses.add(new PowerUpStatus(
                powerUp.config.type,
                powerUp.config.rarity,
                powerUp.getTimeRemaining(),
                powerUp.getProgress()
            ));
        }
        
        return new PowerUpInfo(statuses);
    }
    
    /**
     * Clase para información de UI de power-ups
     */
    public static class PowerUpInfo {
        public final List<PowerUpStatus> activePowerUps;
        
        public PowerUpInfo(List<PowerUpStatus> activePowerUps) {
            this.activePowerUps = activePowerUps;
        }
        
        public boolean hasActivePowerUps() {
            return !activePowerUps.isEmpty();
        }
    }
    
    /**
     * Clase para estado individual de power-up
     */
    public static class PowerUpStatus {
        public final PowerUpType type;
        public final Rarity rarity;
        public final float timeRemaining;
        public final float progress;
        
        public PowerUpStatus(PowerUpType type, Rarity rarity, float timeRemaining, float progress) {
            this.type = type;
            this.rarity = rarity;
            this.timeRemaining = timeRemaining;
            this.progress = progress;
        }
    }
    
    /**
     * Renderiza todos los power-ups del mapa
     */
    public void render(Graphics2D g) {
        for (MapPowerUp powerUp : activeMapPowerUps) {
            if (!powerUp.isCollected()) {
                renderMapPowerUp(g, powerUp);
            }
        }
    }
    
    /**
     * Renderiza un power-up individual en el mapa
     */
    private void renderMapPowerUp(Graphics2D g, MapPowerUp powerUp) {
        Point pos = powerUp.position;
        PowerUpConfig config = powerUp.config;
        
        // Guardar transformaciones
        AffineTransform oldTransform = g.getTransform();
        
        // Mover al centro del power-up
        g.translate(pos.x, pos.y);
        g.rotate(powerUp.getRotation());
        
        // Renderizar según rareza
        switch (config.rarity) {
            case COMMON:
                renderCommonPowerUp(g, config);
                break;
            case UNCOMMON:
                renderUncommonPowerUp(g, config);
                break;
            case RARE:
                renderRarePowerUp(g, config);
                break;
            case LEGENDARY:
                renderLegendaryPowerUp(g, config);
                break;
            case MYTHIC:
                renderMythicPowerUp(g, config);
                break;
        }
        
        // Restaurar transformaciones
        g.setTransform(oldTransform);
    }
    
    /**
     * Renderizado para power-up común
     */
    private void renderCommonPowerUp(Graphics2D g, PowerUpConfig config) {
        g.setColor(config.type.getColor());
        g.fillOval(-15, -15, 30, 30);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.drawString(config.type.name().substring(0, 1), -5, 4);
    }
    
    /**
     * Renderizado para power-up poco común
     */
    private void renderUncommonPowerUp(Graphics2D g, PowerUpConfig config) {
        g.setColor(config.type.getColor());
        g.fillOval(-15, -15, 30, 30);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2));
        g.drawOval(-15, -15, 30, 30);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.drawString(config.type.name().substring(0, 1), -5, 4);
    }
    
    /**
     * Renderizado para power-up raro
     */
    private void renderRarePowerUp(Graphics2D g, PowerUpConfig config) {
        g.setColor(config.type.getColor());
        g.fillOval(-15, -15, 30, 30);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(3));
        g.drawOval(-15, -15, 30, 30);
        
        // Efecto de brillo
        g.setColor(Color.WHITE);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
        g.fillOval(-20, -20, 40, 40);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.drawString(config.type.name().substring(0, 1), -5, 4);
    }
    
    /**
     * Renderizado para power-up legendario
     */
    private void renderLegendaryPowerUp(Graphics2D g, PowerUpConfig config) {
        // Gradiente de rareza
        GradientPaint gradient = new GradientPaint(-15, -15, config.rarity.getColor(), 
                                                  15, 15, config.type.getColor());
        g.setPaint(gradient);
        g.fillOval(-15, -15, 30, 30);
        
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(4));
        g.drawOval(-15, -15, 30, 30);
        
        // Efectos de partículas (simplificado)
        long time = System.currentTimeMillis();
        g.setColor(Color.WHITE);
        for (int i = 0; i < 6; i++) {
            double angle = (time * 0.001 + i * Math.PI / 3) % (2 * Math.PI);
            int x = (int) (Math.cos(angle) * 25);
            int y = (int) (Math.sin(angle) * 25);
            g.fillOval(x - 2, y - 2, 4, 4);
        }
        
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.drawString(config.type.name().substring(0, 1), -5, 4);
    }
    
    /**
     * Renderizado para power-up mítico
     */
    private void renderMythicPowerUp(Graphics2D g, PowerUpConfig config) {
        // Efecto de fuego animado
        long time = System.currentTimeMillis();
        int pulse = (int) (Math.sin(time * 0.01) * 5 + 20);
        
        GradientPaint gradient = new GradientPaint(-pulse, -pulse, Color.RED, 
                                                  pulse, pulse, Color.GOLD);
        g.setPaint(gradient);
        g.fillOval(-pulse, -pulse, pulse * 2, pulse * 2);
        
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(5));
        g.drawOval(-pulse, -pulse, pulse * 2, pulse * 2);
        
        // Partículas mágicas
        g.setColor(Color.CYAN);
        for (int i = 0; i < 8; i++) {
            double angle = (time * 0.002 + i * Math.PI / 4) % (2 * Math.PI);
            int x = (int) (Math.cos(angle) * 30);
            int y = (int) (Math.sin(angle) * 30);
            g.fillOval(x - 3, y - 3, 6, 6);
        }
        
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.drawString(config.type.name().substring(0, 1), -5, 4);
    }
    
    /**
     * Renderiza indicadores de power-ups activos para un jugador
     */
    public void renderPlayerPowerUpIndicators(Graphics2D g, Player player, Point screenPos) {
        List<ActivePowerUp> powerUps = getPlayerPowerUps(player);
        
        if (powerUps.isEmpty()) {
            return;
        }
        
        int yOffset = 0;
        for (ActivePowerUp powerUp : powerUps) {
            renderPowerUpIndicator(g, screenPos.x + 10, screenPos.y + yOffset, powerUp);
            yOffset += 25;
        }
    }
    
    /**
     * Renderiza un indicador individual de power-up
     */
    private void renderPowerUpIndicator(Graphics2D g, int x, int y, ActivePowerUp powerUp) {
        PowerUpConfig config = powerUp.config;
        
        // Fondo del indicador
        g.setColor(config.rarity.getColor());
        g.fillRoundRect(x, y, 120, 20, 5, 5);
        
        // Barra de progreso
        float progress = powerUp.getProgress();
        int progressWidth = (int) (progress * 116);
        g.setColor(Color.GREEN);
        g.fillRoundRect(x + 2, y + 2, progressWidth, 16, 3, 3);
        
        // Texto
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 10));
        String text = String.format("%s (%.1fs)", config.type.getDisplayName(), powerUp.getTimeRemaining());
        g.drawString(text, x + 5, y + 14);
    }
    
    /**
     * Maneja interacciones especiales entre power-ups
     */
    public void handlePowerUpInteractions(Player player) {
        List<ActivePowerUp> powerUps = getPlayerPowerUps(player);
        
        // Verificar interacciones específicas
        checkSpecialInteractions(player, powerUps);
    }
    
    /**
     * Verifica interacciones especiales entre power-ups
     */
    private void checkSpecialInteractions(Player player, List<ActivePowerUp> powerUps) {
        // Ejemplo: Invisibility + Shield = Protección temporal invisible
        boolean hasInvisibility = powerUps.stream().anyMatch(p -> p.config.type == PowerUpType.INVISIBILITY);
        boolean hasShield = powerUps.stream().anyMatch(p -> p.config.type == PowerUpType.SHIELD);
        
        if (hasInvisibility && hasShield) {
            // Aplicar efecto especial: escudo durante invisibilidad
            player.setSuperProtected(true);
        } else {
            player.setSuperProtected(false);
        }
        
        // Ejemplo: Speed + Multiplier = Masa negativa (velocidad extrema)
        boolean hasSpeed = powerUps.stream().anyMatch(p -> p.config.type == PowerUpType.SPEED_BOOST);
        boolean hasMultiplier = powerUps.stream().anyMatch(p -> p.config.type == PowerUpType.MULTIPLIER);
        
        if (hasSpeed && hasMultiplier) {
            // Efecto especial: velocidad ultra alta
            player.setUltraFast(true);
        } else {
            player.setUltraFast(false);
        }
    }
    
    /**
     * Remueve todos los power-ups de un jugador (útil para respawn)
     */
    public void removeAllPlayerPowerUps(Player player) {
        List<ActivePowerUp> powerUps = getPlayerPowerUps(player);
        
        // Aplicar efectos inversos
        for (ActivePowerUp powerUp : powerUps) {
            applyPowerUpEndEffects(powerUp);
        }
        
        playerPowerUps.remove(player);
    }
    
    /**
     * Fuerza el spawn de un power-up específico (debug/admin)
     */
    public void forceSpawnPowerUp(PowerUpType type, Point position) {
        PowerUpConfig config = BASE_CONFIGS.get(type);
        if (config != null) {
            activeMapPowerUps.add(new MapPowerUp(config, position));
        }
    }
    
    /**
     * Obtiene estadísticas del sistema
     */
    public PowerUpSystemStats getStats() {
        return new PowerUpSystemStats(
            activeMapPowerUps.size(),
            playerPowerUps.size(),
            getTotalActivePlayerPowerUps(),
            getPowerUpDistribution()
        );
    }
    
    private int getTotalActivePlayerPowerUps() {
        return playerPowerUps.values().stream()
                .mapToInt(List::size)
                .sum();
    }
    
    private Map<PowerUpType, Integer> getPowerUpDistribution() {
        Map<PowerUpType, Integer> distribution = new HashMap<>();
        
        for (MapPowerUp powerUp : activeMapPowerUps) {
            if (!powerUp.isCollected()) {
                distribution.put(powerUp.config.type, 
                    distribution.getOrDefault(powerUp.config.type, 0) + 1);
            }
        }
        
        return distribution;
    }
    
    /**
     * Clase para estadísticas del sistema
     */
    public static class PowerUpSystemStats {
        public final int totalMapPowerUps;
        public final int totalPlayersWithPowerUps;
        public final int totalActivePowerUps;
        public final Map<PowerUpType, Integer> powerUpDistribution;
        
        public PowerUpSystemStats(int totalMapPowerUps, int totalPlayersWithPowerUps, 
                                int totalActivePowerUps, Map<PowerUpType, Integer> powerUpDistribution) {
            this.totalMapPowerUps = totalMapPowerUps;
            this.totalPlayersWithPowerUps = totalPlayersWithPowerUps;
            this.totalActivePowerUps = totalActivePowerUps;
            this.powerUpDistribution = powerUpDistribution;
        }
    }
}