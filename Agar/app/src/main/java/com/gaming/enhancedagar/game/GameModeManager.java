package com.gaming.enhancedagar.game;

import com.gaming.enhancedagar.entities.Player;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor de modos de juego con reglas específicas y sistemas de puntuación únicos
 * Soporta Classic, Teams, Survival, Arena y King modes
 */
public class GameModeManager {
    
    public enum GameMode {
        CLASSIC("Clásico", "Modo tradicional de Agar.io"),
        TEAMS("Equipos", "4v4 con roles específicos"),
        SURVIVAL("Supervivencia", "Sobrevive a ondas de enemigos"),
        ARENA("Arena", "Combate directo sin crecimiento"),
        KING("Rey", "Ataca al rey para ganar puntos")
    }
    
    public enum TeamRole {
        SCOUT("Explorador", "Velocidad +30%, Visibilidad +20%", 0),
        TANK("Tanque", "Resistencia +50%, Velocidad -20%", 1),
        SUPPORT("Soporte", "Curación +40%, Área de visión +30%", 2),
        ASSASSIN("Asesino", "Velocidad +60%, Tamaño -20%", 3);
        
        private final String name;
        private final String description;
        private final int roleId;
        
        TeamRole(String name, String description, int roleId) {
            this.name = name;
            this.description = description;
            this.roleId = roleId;
        }
        
        public String getName() { return name; }
        public String getDescription() { return description; }
        public int getRoleId() { return roleId; }
    }
    
    private GameMode currentMode;
    private Map<Player, TeamRole> playerRoles;
    private Map<String, Integer> teamScores;
    private Map<Player, Integer> individualScores;
    private int currentWave = 0;
    private Player kingPlayer = null;
    private long waveStartTime = 0;
    private boolean waveInProgress = false;
    private Random random;
    
    // Configuraciones específicas por modo
    private final Map<GameMode, ModeConfig> modeConfigs;
    
    public GameModeManager() {
        this.currentMode = GameMode.CLASSIC;
        this.playerRoles = new ConcurrentHashMap<>();
        this.teamScores = new ConcurrentHashMap<>();
        this.individualScores = new ConcurrentHashMap<>();
        this.random = new Random();
        this.modeConfigs = initializeModeConfigs();
    }
    
    /**
     * Inicializa las configuraciones específicas de cada modo
     */
    private Map<GameMode, ModeConfig> initializeModeConfigs() {
        Map<GameMode, ModeConfig> configs = new HashMap<>();
        
        // CLASSIC - Modo tradicional
        configs.put(GameMode.CLASSIC, new ModeConfig(
            100, 100, 5, 0, 1000, 30, 50, 2.0f, 1.0f, 10
        ));
        
        // TEAMS - 4v4 con roles
        configs.put(GameMode.TEAMS, new ModeConfig(
            80, 80, 3, 0, 800, 25, 40, 1.5f, 1.2f, 15
        ));
        
        // SURVIVAL - Ondas de enemigos
        configs.put(GameMode.SURVIVAL, new ModeConfig(
            60, 60, 8, 0, 600, 20, 60, 3.0f, 0.8f, 5
        ));
        
        // ARENA - Sin crecimiento
        configs.put(GameMode.ARENA, new ModeConfig(
            50, 50, 0, 0, 1200, 40, 20, 1.0f, 2.0f, 20
        ));
        
        // KING - Rey objetivo
        configs.put(GameMode.KING, new ModeConfig(
            70, 70, 4, 0, 900, 35, 45, 2.5f, 1.5f, 8
        ));
        
        return configs;
    }
    
    /**
     * Configuración específica para cada modo de juego
     */
    public static class ModeConfig {
        public final int spawnRadius;
        public final int foodCount;
        public final int enemyCount;
        public final int respawnDelay;
        public final int gameDuration;
        public final int minSize;
        public final int maxSize;
        public final float speedMultiplier;
        public final float sizeMultiplier;
        public final int pointsPerKill;
        
        public ModeConfig(int spawnRadius, int foodCount, int enemyCount, int respawnDelay,
                         int gameDuration, int minSize, int maxSize, float speedMultiplier,
                         float sizeMultiplier, int pointsPerKill) {
            this.spawnRadius = spawnRadius;
            this.foodCount = foodCount;
            this.enemyCount = enemyCount;
            this.respawnDelay = respawnDelay;
            this.gameDuration = gameDuration;
            this.minSize = minSize;
            this.maxSize = maxSize;
            this.speedMultiplier = speedMultiplier;
            this.sizeMultiplier = sizeMultiplier;
            this.pointsPerKill = pointsPerKill;
        }
    }
    
    /**
     * Cambia el modo de juego
     */
    public boolean setGameMode(GameMode mode) {
        if (mode == null) return false;
        
        this.currentMode = mode;
        resetModeSpecificData();
        
        // Configuración especial según el modo
        switch (mode) {
            case TEAMS:
                initializeTeams();
                break;
            case SURVIVAL:
                startSurvivalWave(1);
                break;
            case KING:
                selectNewKing();
                break;
        }
        
        return true;
    }
    
    /**
     * Inicializa el modo de equipos con 4 roles específicos
     */
    private void initializeTeams() {
        teamScores.clear();
        teamScores.put("Equipo_A", 0);
        teamScores.put("Equipo_B", 0);
        
        // Reiniciar roles de jugadores
        for (Map.Entry<Player, TeamRole> entry : playerRoles.entrySet()) {
            entry.setValue(null);
        }
    }
    
    /**
     * Asigna un rol específico a un jugador en modo equipos
     */
    public boolean assignRoleToPlayer(Player player, TeamRole role) {
        if (currentMode != GameMode.TEAMS || player == null || role == null) {
            return false;
        }
        
        // Verificar que no haya más de un jugador por rol en cada equipo
        int teamACount = 0, teamBCount = 0;
        for (Map.Entry<Player, TeamRole> entry : playerRoles.entrySet()) {
            if (entry.getValue() == role) {
                String team = determineTeam(entry.getKey());
                if (team.equals("Equipo_A")) teamACount++;
                else if (team.equals("Equipo_B")) teamBCount++;
            }
        }
        
        String playerTeam = determineTeam(player);
        if ((playerTeam.equals("Equipo_A") && teamACount >= 2) || 
            (playerTeam.equals("Equipo_B") && teamBCount >= 2)) {
            return false;
        }
        
        playerRoles.put(player, role);
        applyRoleStats(player, role);
        return true;
    }
    
    /**
     * Aplica las estadísticas específicas del rol al jugador
     */
    private void applyRoleStats(Player player, TeamRole role) {
        if (player == null) return;
        
        switch (role) {
            case SCOUT:
                player.setSpeedMultiplier(1.3f);
                player.setVisionRangeMultiplier(1.2f);
                break;
            case TANK:
                player.setHealthMultiplier(1.5f);
                player.setSpeedMultiplier(0.8f);
                break;
            case SUPPORT:
                player.setHealingMultiplier(1.4f);
                player.setVisionRangeMultiplier(1.3f);
                break;
            case ASSASSIN:
                player.setSpeedMultiplier(1.6f);
                player.setSizeMultiplier(0.8f);
                break;
        }
    }
    
    /**
     * Determina el equipo de un jugador (A o B)
     */
    private String determineTeam(Player player) {
        if (player == null) return "Equipo_A";
        return (player.getId() % 2 == 0) ? "Equipo_A" : "Equipo_B";
    }
    
    /**
     * Modo Survival: Inicia una nueva ola de enemigos
     */
    public void startSurvivalWave(int waveNumber) {
        if (currentMode != GameMode.SURVIVAL) return;
        
        this.currentWave = waveNumber;
        this.waveStartTime = System.currentTimeMillis();
        this.waveInProgress = true;
        
        ModeConfig config = modeConfigs.get(GameMode.SURVIVAL);
        int enemiesThisWave = config.enemyCount + (waveNumber * 2);
        
        // Generar enemigos según la dificultad
        for (int i = 0; i < enemiesThisWave; i++) {
            generateEnemyForWave(waveNumber);
        }
    }
    
    /**
     * Genera un enemigo específico para la ola actual
     */
    private void generateEnemyForWave(int waveNumber) {
        // Lógica de generación de enemigos con dificultad escalable
        double difficultyMultiplier = 1.0 + (waveNumber * 0.2);
        // Aquí se integraría con el sistema de entidades para crear enemigos
    }
    
    /**
     * Modo King: Selecciona un nuevo rey
     */
    public void selectNewKing() {
        if (currentMode != GameMode.KING) return;
        
        // Obtener jugador con más puntos o el más grande
        Player newKing = getBestPlayerForKing();
        if (newKing != null) {
            kingPlayer = newKing;
            // Aplicar efectos especiales al rey
            applyKingEffects(newKing);
        }
    }
    
    /**
     * Selecciona el mejor candidato para ser rey
     */
    private Player getBestPlayerForKing() {
        Player bestPlayer = null;
        int bestScore = -1;
        
        for (Map.Entry<Player, Integer> entry : individualScores.entrySet()) {
            if (entry.getValue() > bestScore) {
                bestScore = entry.getValue();
                bestPlayer = entry.getKey();
            }
        }
        
        return bestPlayer;
    }
    
    /**
     * Aplica efectos especiales al rey
     */
    private void applyKingEffects(Player king) {
        if (king == null) return;
        
        king.setSpeedMultiplier(0.7f); // Rey más lento pero más grande
        king.setSizeMultiplier(2.0f);
        king.setHealthMultiplier(3.0f);
        // Aquí se aplicarían más efectos visuales para identificar al rey
    }
    
    /**
     * Actualiza el sistema de puntuación según el modo activo
     */
    public void updateScore(Player player, String event, int value) {
        if (player == null) return;
        
        switch (currentMode) {
            case CLASSIC:
                updateClassicScore(player, event, value);
                break;
            case TEAMS:
                updateTeamsScore(player, event, value);
                break;
            case SURVIVAL:
                updateSurvivalScore(player, event, value);
                break;
            case ARENA:
                updateArenaScore(player, event, value);
                break;
            case KING:
                updateKingScore(player, event, value);
                break;
        }
    }
    
    /**
     * Sistema de puntuación para modo Classic
     */
    private void updateClassicScore(Player player, String event, int value) {
        individualScores.put(player, individualScores.getOrDefault(player, 0) + value);
    }
    
    /**
     * Sistema de puntuación para modo Teams
     */
    private void updateTeamsScore(Player player, String event, int value) {
        String team = determineTeam(player);
        teamScores.put(team, teamScores.getOrDefault(team, 0) + value);
        
        // Bonus por trabajo en equipo basado en roles
        TeamRole role = playerRoles.get(player);
        if (role != null) {
            int bonus = calculateRoleBonus(role, event);
            teamScores.put(team, teamScores.getOrDefault(team, 0) + bonus);
        }
    }
    
    /**
     * Sistema de puntuación para modo Survival
     */
    private void updateSurvivalScore(Player player, String event, int value) {
        // En Survival los puntos son acumulativos y determinan la supervivencia
        individualScores.put(player, individualScores.getOrDefault(player, 0) + value);
        
        // Bonificación por supervivencia de ola
        if (event.equals("wave_survived")) {
            individualScores.put(player, individualScores.getOrDefault(player, 0) + (currentWave * 10));
        }
    }
    
    /**
     * Sistema de puntuación para modo Arena
     */
    private void updateArenaScore(Player player, String event, int value) {
        // Arena se centra en eliminación rápida
        individualScores.put(player, individualScores.getOrDefault(player, 0) + value);
        
        if (event.equals("kill")) {
            individualScores.put(player, individualScores.getOrDefault(player, 0) + 20);
        }
    }
    
    /**
     * Sistema de puntuación para modo King
     */
    private void updateKingScore(Player player, String event, int value) {
        if (player == kingPlayer) {
            // El rey gana puntos por sobrevivir
            individualScores.put(player, individualScores.getOrDefault(player, 0) + value);
        } else {
            // Otros jugadores ganan más puntos atacando al rey
            if (event.equals("king_damage")) {
                individualScores.put(player, individualScores.getOrDefault(player, 0) + (value * 3));
            }
        }
    }
    
    /**
     * Calcula bonus de puntos basado en el rol y la acción
     */
    private int calculateRoleBonus(TeamRole role, String event) {
        switch (role) {
            case SCOUT:
                if (event.equals("food_collected")) return 2;
                break;
            case TANK:
                if (event.equals("player_protected")) return 5;
                break;
            case SUPPORT:
                if (event.equals("player_healed")) return 3;
                break;
            case ASSASSIN:
                if (event.equals("kill")) return 4;
                break;
        }
        return 0;
    }
    
    /**
     * Verifica condiciones de victoria según el modo
     */
    public boolean checkWinCondition() {
        switch (currentMode) {
            case CLASSIC:
                return individualScores.values().stream().anyMatch(score -> score >= 1000);
            case TEAMS:
                return teamScores.values().stream().anyMatch(score -> score >= 500);
            case SURVIVAL:
                return currentWave >= 10; // 10 olas completadas
            case ARENA:
                return individualScores.values().stream().anyMatch(score -> score >= 50);
            case KING:
                return kingPlayer != null && individualScores.getOrDefault(kingPlayer, 0) >= 200;
        }
        return false;
    }
    
    /**
     * Obtiene el ganador actual
     */
    public String getCurrentWinner() {
        switch (currentMode) {
            case CLASSIC, ARENA, SURVIVAL:
                return individualScores.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(entry -> entry.getKey().getName())
                    .orElse("Nadie");
            case TEAMS:
                return teamScores.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("Empate");
            case KING:
                return kingPlayer != null ? kingPlayer.getName() : "Sin rey";
        }
        return "Jugador Desconocido";
    }
    
    /**
     * Reinicia datos específicos del modo actual
     */
    private void resetModeSpecificData() {
        individualScores.clear();
        teamScores.clear();
        playerRoles.clear();
        currentWave = 0;
        kingPlayer = null;
        waveStartTime = 0;
        waveInProgress = false;
    }
    
    /**
     * Obtiene la configuración activa para el modo actual
     */
    public ModeConfig getCurrentModeConfig() {
        return modeConfigs.getOrDefault(currentMode, modeConfigs.get(GameMode.CLASSIC));
    }
    
    /**
     * Actualiza la lógica del modo de juego
     */
    public void updateGameModeLogic() {
        switch (currentMode) {
            case SURVIVAL:
                updateSurvivalLogic();
                break;
            case KING:
                updateKingLogic();
                break;
        }
    }
    
    /**
     * Actualiza la lógica específica del modo Survival
     */
    private void updateSurvivalLogic() {
        if (!waveInProgress) return;
        
        // Verificar si la ola terminó
        long waveDuration = System.currentTimeMillis() - waveStartTime;
        if (waveDuration >= 60000) { // 60 segundos por ola
            startSurvivalWave(currentWave + 1);
        }
    }
    
    /**
     * Actualiza la lógica específica del modo King
     */
    private void updateKingLogic() {
        if (kingPlayer == null || kingPlayer.getSize() < 10) {
            selectNewKing();
        }
    }
    
    // Getters para información del modo
    public GameMode getCurrentMode() { return currentMode; }
    public int getCurrentWave() { return currentWave; }
    public Player getKingPlayer() { return kingPlayer; }
    public Map<String, Integer> getTeamScores() { return new HashMap<>(teamScores); }
    public Map<Player, Integer> getIndividualScores() { return new HashMap<>(individualScores); }
    public Map<Player, TeamRole> getPlayerRoles() { return new HashMap<>(playerRoles); }
    public boolean isWaveInProgress() { return waveInProgress; }
}