package com.gaming.enhancedagar.game;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import android.graphics.Color;
import android.graphics.PointF;

/**
 * Sistema de equipos para el juego Agar mejorado
 * Incluye roles complementarios, buffs de equipo, comunicación visual y estrategias coordinadas
 */
public class TeamSystem {
    
    // Roles disponibles en el juego
    public enum TeamRole {
        SCOUT("Explorador", Color.CYAN, 1.3f, 1.1f),      // Velocidad alta, masa baja
        TANK("Tanque", 0xFF808080, 0.8f, 1.5f),           // Velocidad baja, masa alta (GRAY)
        SUPPORT("Soporte", Color.GREEN, 1.0f, 1.2f),      // Velocidad media, masa media
        ASSASSIN("Asesino", Color.RED, 1.2f, 0.9f),       // Velocidad alta, masa media
        CONTROLLER("Controlador", Color.MAGENTA, 0.9f, 1.1f); // Velocidad media-baja, masa media
        
        private final String name;
        private final int color;
        private final float speedMultiplier;
        private final float massMultiplier;
        
        TeamRole(String name, int color, float speedMultiplier, float massMultiplier) {
            this.name = name;
            this.color = color;
            this.speedMultiplier = speedMultiplier;
            this.massMultiplier = massMultiplier;
        }
        
        public String getName() { return name; }
        public int getColor() { return color; }
        public float getSpeedMultiplier() { return speedMultiplier; }
        public float getMassMultiplier() { return massMultiplier; }
    }
    
    // Estados del equipo
    public enum TeamState {
        FORMING("Formando"),
        ACTIVE("Activo"),
        DEFENSIVE("Defensivo"),
        OFFENSIVE("Ofensivo"),
        RETREATING("Retirándose"),
        VICTORIOUS("Victorioso"),
        DEFEATED("Derrotado");
        
        private final String displayName;
        
        TeamState(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
    }
    
    // Tipos de buffs de equipo
    public enum TeamBuff {
        FORMATION_BONUS("Bonus de Formación", 1.1f, 100),        // Buff por estar cerca
        ROLE_SYNERGY("Sinergia de Roles", 1.15f, 150),           // Buff por roles complementarios
        TEAM_VISION("Visión de Equipo", 1.2f, 200),              // Buff de campo de visión
        COORDINATED_ATTACK("Ataque Coordinado", 1.25f, 300),     // Buff de ataque
        TERRITORY_CONTROL("Control Territorial", 1.1f, 250);     // Buff de control de territorio
        
        private final String name;
        private final float multiplier;
        private final int energyCost;
        
        TeamBuff(String name, float multiplier, int energyCost) {
            this.name = name;
            this.multiplier = multiplier;
            this.energyCost = energyCost;
        }
        
        public String getName() { return name; }
        public float getMultiplier() { return multiplier; }
        public int getEnergyCost() { return energyCost; }
    }
    
    // Objetivos del equipo
    public enum TeamObjective {
        TERRITORY_CONTROL("Control Territorial", 50, 200),
        ELIMINATION("Eliminación", 30, 150),
        SURVIVAL("Supervivencia", 20, 100),
        RESOURCE_COLLECTION("Recolección", 40, 120),
        TIME_CONTROL("Control Temporal", 35, 180);
        
        private final String name;
        private final int basePoints;
        private final int bonusPoints;
        
        TeamObjective(String name, int basePoints, int bonusPoints) {
            this.name = name;
            this.basePoints = basePoints;
            this.bonusPoints = bonusPoints;
        }
        
        public String getName() { return name; }
        public int getBasePoints() { return basePoints; }
        public int getBonusPoints() { return bonusPoints; }
    }
    
    // Mensajes de comunicación visual
    public static class TeamMessage {
        private final String message;
        private final int color;
        private final long timestamp;
        private final PointF position;
        private final int duration;
        
        public TeamMessage(String message, int color, PointF position, int duration) {
            this.message = message;
            this.color = color;
            this.position = position;
            this.timestamp = System.currentTimeMillis();
            this.duration = duration;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > duration;
        }
        
        public String getMessage() { return message; }
        public int getColor() { return color; }
        public PointF getPosition() { return position; }
        public int getDuration() { return duration; }
        public long getTimestamp() { return timestamp; }
    }
    
    // Jugador en equipo
    public static class TeamPlayer {
        private final int playerId;
        private final String playerName;
        private TeamRole role;
        private PointF position;
        private float mass;
        private Team team;
        private boolean isActive;
        private int health;
        private float speed;
        private Map<TeamBuff, Integer> buffLevels;
        
        public TeamPlayer(int playerId, String playerName) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.role = TeamRole.SUPPORT; // Rol por defecto
            this.position = new PointF(0, 0);
            this.mass = 100f;
            this.team = null;
            this.isActive = false;
            this.health = 100;
            this.speed = 1.0f;
            this.buffLevels = new ConcurrentHashMap<>();
        }
        
        public void setRole(TeamRole role) { this.role = role; }
        public void setPosition(PointF position) { this.position = position; }
        public void setMass(float mass) { this.mass = mass; }
        public void setTeam(Team team) { this.team = team; }
        public void setActive(boolean active) { isActive = active; }
        public void setHealth(int health) { this.health = health; }
        public void setSpeed(float speed) { this.speed = speed; }
        
        public TeamRole getRole() { return role; }
        public PointF getPosition() { return position; }
        public float getMass() { return mass; }
        public Team getTeam() { return team; }
        public boolean isActive() { return isActive; }
        public int getHealth() { return health; }
        public float getSpeed() { return speed; }
        public int getPlayerId() { return playerId; }
        public String getPlayerName() { return playerName; }
        public Map<TeamBuff, Integer> getBuffLevels() { return buffLevels; }
        
        public void addBuffLevel(TeamBuff buff) {
            buffLevels.put(buff, buffLevels.getOrDefault(buff, 0) + 1);
        }
        
        public void removeBuff(TeamBuff buff) {
            buffLevels.remove(buff);
        }
        
        public float getEffectiveMass() {
            float effectiveMass = mass * role.getMassMultiplier();
            
            // Aplicar buffs
            for (Map.Entry<TeamBuff, Integer> entry : buffLevels.entrySet()) {
                TeamBuff buff = entry.getKey();
                int level = entry.getValue();
                effectiveMass *= Math.pow(buff.getMultiplier(), level);
            }
            
            return effectiveMass;
        }
        
        public float getEffectiveSpeed() {
            float effectiveSpeed = speed * role.getSpeedMultiplier();
            
            // Aplicar buffs
            for (Map.Entry<TeamBuff, Integer> entry : buffLevels.entrySet()) {
                TeamBuff buff = entry.getKey();
                int level = entry.getValue();
                effectiveSpeed *= Math.pow(buff.getMultiplier(), level);
            }
            
            return effectiveSpeed;
        }
    }
    
    // Equipo
    public static class Team {
        private final int teamId;
        private final String teamName;
        private final int teamColor;
        private final List<TeamPlayer> players;
        private TeamState state;
        private int score;
        private int territoryControl;
        private Map<TeamObjective, Integer> objectiveProgress;
        private PointF teamCenter;
        private float teamMass;
        private int teamEnergy;
        private List<TeamMessage> messages;
        private Map<TeamPlayer, PointF> lastKnownPositions;
        
        public Team(int teamId, String teamName, int teamColor) {
            this.teamId = teamId;
            this.teamName = teamName;
            this.teamColor = teamColor;
            this.players = new ArrayList<>();
            this.state = TeamState.FORMING;
            this.score = 0;
            this.territoryControl = 0;
            this.objectiveProgress = new ConcurrentHashMap<>();
            this.teamCenter = new PointF(0, 0);
            this.teamMass = 0f;
            this.teamEnergy = 100;
            this.messages = new ArrayList<>();
            this.lastKnownPositions = new ConcurrentHashMap<>();
        }
        
        public void addPlayer(TeamPlayer player) {
            players.add(player);
            player.setTeam(this);
            calculateTeamStats();
        }
        
        public void removePlayer(TeamPlayer player) {
            players.remove(player);
            player.setTeam(null);
            calculateTeamStats();
        }
        
        public void setState(TeamState state) { this.state = state; }
        public void setScore(int score) { this.score = score; }
        public void setTerritoryControl(int control) { this.territoryControl = control; }
        public void setTeamEnergy(int energy) { this.teamEnergy = Math.max(0, Math.min(100, energy)); }
        
        public int getTeamId() { return teamId; }
        public String getTeamName() { return teamName; }
        public int getTeamColor() { return teamColor; }
        public List<TeamPlayer> getPlayers() { return players; }
        public TeamState getState() { return state; }
        public int getScore() { return score; }
        public int getTerritoryControl() { return territoryControl; }
        public Map<TeamObjective, Integer> getObjectiveProgress() { return objectiveProgress; }
        public PointF getTeamCenter() { return teamCenter; }
        public float getTeamMass() { return teamMass; }
        public int getTeamEnergy() { return teamEnergy; }
        public List<TeamMessage> getMessages() { return messages; }
        public Map<TeamPlayer, PointF> getLastKnownPositions() { return lastKnownPositions; }
        
        private void calculateTeamStats() {
            // Calcular centro del equipo
            if (players.isEmpty()) {
                teamCenter.set(0, 0);
                teamMass = 0;
                return;
            }
            
            double totalX = 0, totalY = 0, totalMass = 0;
            
            for (TeamPlayer player : players) {
                totalX += player.getPosition().x * player.getMass();
                totalY += player.getPosition().y * player.getMass();
                totalMass += player.getMass();
            }
            
            teamCenter.set((float)(totalX / totalMass), (float)(totalY / totalMass));
            teamMass = (float) totalMass;
        }
        
        public void updateObjectiveProgress(TeamObjective objective, int progress) {
            objectiveProgress.put(objective, Math.max(0, progress));
        }
        
        public void addMessage(TeamMessage message) {
            messages.add(message);
            // Limpiar mensajes expirados
            messages.removeIf(TeamMessage::isExpired);
        }
        
        public void updateLastKnownPosition(TeamPlayer player, PointF position) {
            lastKnownPositions.put(player, position);
        }
    }
    
    // Configuración del sistema de equipos
    public static class TeamConfig {
        private final int maxTeams;
        private final int maxPlayersPerTeam;
        private final float formationRadius;
        private final int maxBuffDistance;
        private final int messageDuration;
        private final boolean autoBalance;
        
        public TeamConfig() {
            this.maxTeams = 4;
            this.maxPlayersPerTeam = 6;
            this.formationRadius = 200f;
            this.maxBuffDistance = 150f;
            this.messageDuration = 3000;
            this.autoBalance = true;
        }
        
        public TeamConfig(int maxTeams, int maxPlayersPerTeam, float formationRadius, 
                         int maxBuffDistance, int messageDuration, boolean autoBalance) {
            this.maxTeams = maxTeams;
            this.maxPlayersPerTeam = maxPlayersPerTeam;
            this.formationRadius = formationRadius;
            this.maxBuffDistance = maxBuffDistance;
            this.messageDuration = messageDuration;
            this.autoBalance = autoBalance;
        }
        
        public int getMaxTeams() { return maxTeams; }
        public int getMaxPlayersPerTeam() { return maxPlayersPerTeam; }
        public float getFormationRadius() { return formationRadius; }
        public int getMaxBuffDistance() { return maxBuffDistance; }
        public int getMessageDuration() { return messageDuration; }
        public boolean isAutoBalance() { return autoBalance; }
    }
    
    // Variables principales del sistema
    private final Map<Integer, Team> teams;
    private final Map<Integer, TeamPlayer> players;
    private final TeamConfig config;
    private final Random random;
    private int nextTeamId;
    private int nextPlayerId;
    
    // Constructor
    public TeamSystem(TeamConfig config) {
        this.teams = new ConcurrentHashMap<>();
        this.players = new ConcurrentHashMap<>();
        this.config = config;
        this.random = new Random();
        this.nextTeamId = 1;
        this.nextPlayerId = 1;
    }
    
    public TeamSystem() {
        this(new TeamConfig());
    }
    
    // Métodos principales
    
    /**
     * Crear un nuevo equipo
     */
    public Team createTeam(String teamName, int teamColor) {
        if (teams.size() >= config.getMaxTeams()) {
            return null;
        }
        
        Team team = new Team(nextTeamId++, teamName, teamColor);
        teams.put(team.getTeamId(), team);
        return team;
    }
    
    /**
     * Agregar jugador al sistema
     */
    public TeamPlayer addPlayer(String playerName) {
        TeamPlayer player = new TeamPlayer(nextPlayerId++, playerName);
        players.put(player.getPlayerId(), player);
        return player;
    }
    
    /**
     * Asignar jugador a equipo con distribución equitativa de roles
     */
    public boolean assignPlayerToTeam(TeamPlayer player, Team team) {
        if (team.getPlayers().size() >= config.getMaxPlayersPerTeam()) {
            return false;
        }
        
        // Determinar el rol que más necesita el equipo
        TeamRole neededRole = determineNeededRole(team);
        player.setRole(neededRole);
        
        team.addPlayer(player);
        return true;
    }
    
    /**
     * Distribuir equitativamente los roles en un equipo
     */
    private TeamRole determineNeededRole(Team team) {
        Map<TeamRole, Integer> roleCount = new HashMap<>();
        
        // Contar roles actuales
        for (TeamPlayer player : team.getPlayers()) {
            roleCount.put(player.getRole(), roleCount.getOrDefault(player.getRole(), 0) + 1);
        }
        
        // Encontrar el rol con menos jugadores
        TeamRole minRole = null;
        int minCount = Integer.MAX_VALUE;
        
        for (TeamRole role : TeamRole.values()) {
            int count = roleCount.getOrDefault(role, 0);
            if (count < minCount) {
                minCount = count;
                minRole = role;
            }
        }
        
        return minRole != null ? minRole : TeamRole.SUPPORT;
    }
    
    /**
     * Calcular y aplicar buffs de equipo por proximidad
     */
    public void updateTeamBuffs() {
        for (Team team : teams.values()) {
            updateFormationBuffs(team);
            updateRoleSynergyBuffs(team);
        }
    }
    
    /**
     * Actualizar buffs de formación
     */
    private void updateFormationBuffs(Team team) {
        List<TeamPlayer> activePlayers = team.getPlayers().stream()
            .filter(TeamPlayer::isActive)
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        
        // Calcular distancias entre jugadores
        for (int i = 0; i < activePlayers.size(); i++) {
            for (int j = i + 1; j < activePlayers.size(); j++) {
                TeamPlayer player1 = activePlayers.get(i);
                TeamPlayer player2 = activePlayers.get(j);
                
                double distance = player1.getPosition().distance(player2.getPosition());
                
                if (distance <= config.getFormationRadius()) {
                    // Aplicar buff de formación
                    player1.addBuffLevel(TeamBuff.FORMATION_BONUS);
                    player2.addBuffLevel(TeamBuff.FORMATION_BONUS);
                }
            }
        }
    }
    
    /**
     * Actualizar buffs de sinergia de roles
     */
    private void updateRoleSynergyBuffs(Team team) {
        Set<TeamRole> presentRoles = team.getPlayers().stream()
            .filter(TeamPlayer::isActive)
            .map(TeamPlayer::getRole)
            .collect(Collectors.toSet());
        
        // Verificar combinaciones sinérgicas
        boolean hasScout = presentRoles.contains(TeamRole.SCOUT);
        boolean hasTank = presentRoles.contains(TeamRole.TANK);
        boolean hasSupport = presentRoles.contains(TeamRole.SUPPORT);
        boolean hasAssassin = presentRoles.contains(TeamRole.ASSASSIN);
        boolean hasController = presentRoles.contains(TeamRole.CONTROLLER);
        
        for (TeamPlayer player : team.getPlayers()) {
            if (!player.isActive()) continue;
            
            // Buff por diversidad de roles
            if (presentRoles.size() >= 3) {
                player.addBuffLevel(TeamBuff.ROLE_SYNERGY);
            }
            
            // Buff específico por combinación de roles
            if (hasTank && hasSupport) {
                player.addBuffLevel(TeamBuff.TERRITORY_CONTROL);
            }
            
            if (hasScout && hasAssassin) {
                player.addBuffLevel(TeamBuff.COORDINATED_ATTACK);
            }
        }
    }
    
    /**
     * Actualizar objetivos del equipo
     */
    public void updateTeamObjectives() {
        for (Team team : teams.values()) {
            updateTerritoryControl(team);
            updateEliminationObjective(team);
        }
    }
    
    /**
     * Actualizar control territorial
     */
    private void updateTerritoryControl(Team team) {
        // Calcular territorio controlado basado en la posición del centro del equipo
        float territoryRadius = 300f;
        int baseControl = calculateTerritorySize(team.getTeamCenter(), territoryRadius);
        
        team.setTerritoryControl(baseControl);
        team.updateObjectiveProgress(TeamObjective.TERRITORY_CONTROL, baseControl);
        
        // Bonificación por formación compacta
        if (isTeamCompact(team)) {
            team.updateObjectiveProgress(TeamObjective.TERRITORY_CONTROL, baseControl / 2);
        }
    }
    
    /**
     * Actualizar objetivo de eliminación
     */
    private void updateEliminationObjective(Team team) {
        int totalOpponents = players.size() - team.getPlayers().size();
        int eliminatedOpponents = 0; // Se calcularía basado en jugadores eliminados
        
        team.updateObjectiveProgress(TeamObjective.ELIMINATION, eliminatedOpponents * 10);
    }
    
    /**
     * Enviar mensaje de comunicación visual
     */
    public void sendTeamMessage(TeamPlayer sender, String message, int color) {
        if (sender.getTeam() == null) return;
        
        TeamMessage teamMessage = new TeamMessage(
            message, 
            color, 
            sender.getPosition(), 
            config.getMessageDuration()
        );
        
        sender.getTeam().addMessage(teamMessage);
    }
    
    /**
     * Actualizar estrategias coordinadas
     */
    public void updateCoordinatedStrategies() {
        for (Team team : teams.values()) {
            TeamState newState = determineTeamState(team);
            if (newState != team.getState()) {
                team.setState(newState);
                updateTeamStrategy(team, newState);
            }
        }
    }
    
    /**
     * Determinar estado del equipo basado en condiciones del juego
     */
    private TeamState determineTeamState(Team team) {
        float avgHealth = team.getPlayers().stream()
            .filter(TeamPlayer::isActive)
            .mapToInt(TeamPlayer::getHealth)
            .average()
            .orElse(0.0);
        
        float teamStrength = calculateTeamStrength(team);
        int opponentCount = teams.values().stream()
            .mapToInt(t -> t.getPlayers().size())
            .sum() - team.getPlayers().size();
        
        if (avgHealth < 30) {
            return TeamState.RETREATING;
        } else if (teamStrength > opponentCount * 1.5) {
            return TeamState.OFFENSIVE;
        } else if (teamStrength < opponentCount * 0.5) {
            return TeamState.DEFENSIVE;
        } else {
            return TeamState.ACTIVE;
        }
    }
    
    /**
     * Actualizar estrategia basada en el estado
     */
    private void updateTeamStrategy(Team team, TeamState state) {
        switch (state) {
            case OFFENSIVE:
                updateOffensiveStrategy(team);
                break;
            case DEFENSIVE:
                updateDefensiveStrategy(team);
                break;
            case RETREATING:
                updateRetreatStrategy(team);
                break;
            case ACTIVE:
                updateActiveStrategy(team);
                break;
        }
    }
    
    private void updateOffensiveStrategy(Team team) {
        for (TeamPlayer player : team.getPlayers()) {
            if (!player.isActive()) continue;
            
            switch (player.getRole()) {
                case SCOUT:
                    // Los exploradores buscan objetivos
                    sendTeamMessage(player, "¡Buscando objetivos!", Color.CYAN);
                    break;
                case ASSASSIN:
                    // Los asesinos se posicionan para ataques
                    sendTeamMessage(player, "¡Posicionándome!", Color.RED);
                    break;
                case TANK:
                    // Los tanques avanzan como vanguardia
                    sendTeamMessage(player, "¡Avanzando!", Color.GRAY);
                    break;
            }
        }
    }
    
    private void updateDefensiveStrategy(Team team) {
        for (TeamPlayer player : team.getPlayers()) {
            if (!player.isActive()) continue;
            
            switch (player.getRole()) {
                case SUPPORT:
                    // Los soportes se enfocan en curación y soporte
                    sendTeamMessage(player, "¡Dando soporte!", Color.GREEN);
                    break;
                case CONTROLLER:
                    // Los controladores manejan el área
                    sendTeamMessage(player, "¡Controlando área!", Color.MAGENTA);
                    break;
                case TANK:
                    // Los tanques protegen al equipo
                    sendTeamMessage(player, "¡Defendiendo!", Color.GRAY);
                    break;
            }
        }
    }
    
    private void updateRetreatStrategy(Team team) {
        for (TeamPlayer player : team.getPlayers()) {
            if (!player.isActive()) continue;
            
            sendTeamMessage(player, "¡Retirándose!", Color.ORANGE);
        }
    }
    
    private void updateActiveStrategy(Team team) {
        for (TeamPlayer player : team.getPlayers()) {
            if (!player.isActive()) continue;
            
            sendTeamMessage(player, "¡Activo!", player.getRole().getColor());
        }
    }
    
    /**
     * Balancear equipos para competitividad
     */
    public void balanceTeams() {
        if (!config.isAutoBalance()) return;
        
        List<TeamPlayer> allPlayers = new ArrayList<>(players.values());
        
        // Remover jugadores de equipos actuales
        for (Team team : teams.values()) {
            team.getPlayers().clear();
        }
        
        // Reasignar jugadores equilibradamente
        Collections.shuffle(allPlayers, random);
        
        for (int i = 0; i < allPlayers.size(); i++) {
            TeamPlayer player = allPlayers.get(i);
            Team team = getBalancedTeam();
            
            if (team != null && team.getPlayers().size() < config.getMaxPlayersPerTeam()) {
                assignPlayerToTeam(player, team);
            }
        }
    }
    
    /**
     * Obtener el equipo que necesita más jugadores para balancear
     */
    private Team getBalancedTeam() {
        return teams.values().stream()
            .min(Comparator.comparingInt(team -> {
                int baseSize = team.getPlayers().size();
                float avgMass = team.getPlayers().stream()
                    .mapToDouble(TeamPlayer::getMass)
                    .average()
                    .orElse(0.0);
                return baseSize + (int)(avgMass / 100); // Balancear también por masa total
            }))
            .orElse(null);
    }
    
    // Métodos auxiliares
    
    private float calculateTeamStrength(Team team) {
        return team.getPlayers().stream()
            .filter(TeamPlayer::isActive)
            .mapToDouble(player -> player.getMass() * player.getRole().getMassMultiplier())
            .sum();
    }
    
    private boolean isTeamCompact(Team team) {
        List<TeamPlayer> activePlayers = team.getPlayers().stream()
            .filter(TeamPlayer::isActive)
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        
        if (activePlayers.size() < 2) return false;
        
        PointF center = team.getTeamCenter();
        float maxDistance = 0;
        
        for (TeamPlayer player : activePlayers) {
            float distance = (float) center.distance(player.getPosition());
            maxDistance = Math.max(maxDistance, distance);
        }
        
        return maxDistance <= config.getFormationRadius() / 2;
    }
    
    private int calculateTerritorySize(PointF center, float radius) {
        // Lógica simplificada para calcular territorio controlado
        // En un juego real, esto sería más complejo
        return (int)(radius * radius / 100);
    }
    
    // Métodos públicos de consulta
    
    public Map<Integer, Team> getTeams() { return teams; }
    public Map<Integer, TeamPlayer> getPlayers() { return players; }
    public TeamConfig getConfig() { return config; }
    
    /**
     * Obtener jugadores de un equipo por rol
     */
    public Map<TeamRole, List<TeamPlayer>> getPlayersByRole(Team team) {
        Map<TeamRole, List<TeamPlayer>> playersByRole = new HashMap<>();
        
        for (TeamRole role : TeamRole.values()) {
            playersByRole.put(role, new ArrayList<>());
        }
        
        for (TeamPlayer player : team.getPlayers()) {
            playersByRole.get(player.getRole()).add(player);
        }
        
        return playersByRole;
    }
    
    /**
     * Calcular efectividad del equipo
     */
    public float calculateTeamEffectiveness(Team team) {
        float baseEffectiveness = calculateTeamStrength(team);
        float roleSynergy = calculateRoleSynergy(team);
        float formationBonus = calculateFormationBonus(team);
        
        return baseEffectiveness * roleSynergy * formationBonus;
    }
    
    private float calculateRoleSynergy(Team team) {
        Set<TeamRole> presentRoles = team.getPlayers().stream()
            .filter(TeamPlayer::isActive)
            .map(TeamPlayer::getRole)
            .collect(Collectors.toSet());
        
        // Bonus por diversidad de roles
        float diversityBonus = 1.0f + (presentRoles.size() * 0.1f);
        return Math.min(diversityBonus, 1.5f); // Máximo 50% de bonus
    }
    
    private float calculateFormationBonus(Team team) {
        if (isTeamCompact(team)) {
            return 1.2f; // 20% bonus por formación compacta
        }
        return 1.0f;
    }
    
    /**
     * Obtener estadísticas del equipo
     */
    public Map<String, Object> getTeamStatistics(Team team) {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("team_id", team.getTeamId());
        stats.put("team_name", team.getTeamName());
        stats.put("player_count", team.getPlayers().size());
        stats.put("score", team.getScore());
        stats.put("territory_control", team.getTerritoryControl());
        stats.put("team_mass", team.getTeamMass());
        stats.put("team_energy", team.getTeamEnergy());
        stats.put("state", team.getState().getDisplayName());
        stats.put("effectiveness", calculateTeamEffectiveness(team));
        
        // Distribución de roles
        Map<TeamRole, Long> roleDistribution = team.getPlayers().stream()
            .collect(Collectors.groupingBy(
                TeamPlayer::getRole,
                Collectors.counting()
            ));
        stats.put("role_distribution", roleDistribution);
        
        // Progreso de objetivos
        stats.put("objectives", team.getObjectiveProgress());
        
        return stats;
    }
    
    /**
     * Limpiar mensajes expirados
     */
    public void cleanupExpiredMessages() {
        for (Team team : teams.values()) {
            team.getMessages().removeIf(TeamMessage::isExpired);
        }
    }
}