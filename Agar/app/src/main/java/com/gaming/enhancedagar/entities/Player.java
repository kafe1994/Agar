package com.gaming.enhancedagar.entities;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Clase Player que representa al jugador en Enhanced Agar
 * Extiende Entity e implementa sistema completo de roles, habilidades y crecimiento
 */
public class Player extends Entity {
    private static final String TAG = "Player";
    
    // Tipos de roles disponibles
    public enum PlayerRole {
        EXPLORER("Explorador", 0xFF4CAF50, 1.2f, 0.8f),
        HUNTER("Cazador", 0xFFF44336, 0.8f, 1.3f),
        GUARDIAN("Guardián", 0xFF2196F3, 1.1f, 1.1f),
        SUPPORT("Soporte", 0xFFFF9800, 1.3f, 0.9f),
        BALANCED("Equilibrado", 0xFF9C27B0, 1.0f, 1.0f);
        
        public final String displayName;
        public final int color;
        public final float speedMultiplier;
        public final float powerMultiplier;
        
        PlayerRole(String displayName, int color, float speedMultiplier, float powerMultiplier) {
            this.displayName = displayName;
            this.color = color;
            this.speedMultiplier = speedMultiplier;
            this.powerMultiplier = powerMultiplier;
        }
    }
    
    // Habilidades especiales
    public enum SpecialAbility {
        SPEED_BOOST("Velocidad", 3000, 15000),
        MASS_SPLIT("División", 5000, 20000),
        TELEPORT("Teletransporte", 8000, 30000),
        SHIELD("Escudo", 4000, 12000),
        CAMOUFLAGE("Camuflaje", 6000, 18000);
        
        public final String displayName;
        public final int energyCost;
        public final int cooldown;
        
        SpecialAbility(String displayName, int energyCost, int cooldown) {
            this.displayName = displayName;
            this.energyCost = energyCost;
            this.cooldown = cooldown;
        }
    }
    
    // Estados del jugador
    public enum PlayerState {
        ALIVE("Vivo"),
        DEAD("Muerto"),
        RESPAWNING("Respawnando"),
        SPECTATING("Espectando"),
        POWERED_UP("Potenciado");
        
        public final String displayName;
        
        PlayerState(String displayName) {
            this.displayName = displayName;
        }
    }
    
    // Propiedades del jugador
    private String playerName;
    private PlayerRole role;
    private PlayerState state;
    private int score;
    private int highScore;
    private float mass;
    private float baseMass;
    private int level;
    private int experience;
    private int experienceToNextLevel;
    
    // Sistema de energía y habilidades
    private int currentEnergy;
    private int maxEnergy;
    private Map<SpecialAbility, Long> abilityCooldowns;
    private SpecialAbility activeAbility;
    private long abilityEndTime;
    
    // Controles y movimiento
    private float targetX, targetY;
    private boolean isMoving;
    private Random random;
    
    // Estadísticas de juego
    private int foodEaten;
    private int playersEaten;
    private int deaths;
    private long playTime;
    private long gameStartTime;
    
    // Efectos visuales
    private List<VisualEffect> visualEffects;
    private Path movementPath;
    private int trailLength;
    
    /**
     * Constructor del jugador
     */
    public Player(String name, float startX, float startY) {
        super(startX, startY, 30, 30); // Tamaño inicial
        
        this.playerName = name;
        this.role = PlayerRole.BALANCED;
        this.state = PlayerState.ALIVE;
        this.baseMass = mass = 100;
        this.score = 0;
        this.highScore = 0;
        this.level = 1;
        this.experience = 0;
        this.experienceToNextLevel = 100;
        
        this.maxEnergy = 100;
        this.currentEnergy = maxEnergy;
        this.abilityCooldowns = new HashMap<>();
        this.random = new Random();
        
        this.targetX = startX;
        this.targetY = startY;
        this.isMoving = false;
        
        this.foodEaten = 0;
        this.playersEaten = 0;
        this.deaths = 0;
        this.playTime = 0;
        this.gameStartTime = System.currentTimeMillis();
        
        this.visualEffects = new ArrayList<>();
        this.movementPath = new Path();
        this.trailLength = 0;
        
        // Inicializar cooldowns
        for (SpecialAbility ability : SpecialAbility.values()) {
            abilityCooldowns.put(ability, 0L);
        }
        
        applyRoleModifications();
        updateAppearance();
        
        Log.d(TAG, "Player " + name + " creado con rol " + role.displayName);
    }
    
    /**
     * Aplica las modificaciones del rol al jugador
     */
    private void applyRoleModifications() {
        // Modificar velocidad máxima basada en rol
        this.maxSpeed = 5.0f * role.speedMultiplier;
        
        // Modificar color basado en rol
        this.color = role.color;
        paint.setColor(color);
        
        // Modificar multiplicadores de poder
        float roleMultiplier = role.powerMultiplier;
        
        Log.d(TAG, "Modificaciones de rol aplicadas: velocidad=" + maxSpeed + ", color=" + Integer.toHexString(color));
    }
    
    /**
     * Actualiza la lógica del jugador
     */
    @Override
    public void update(float deltaTime) {
        if (!isActive || state == PlayerState.DEAD) return;
        
        // Actualizar tiempo de juego
        playTime = System.currentTimeMillis() - gameStartTime;
        
        // Actualizar habilidades activas
        updateActiveAbilities();
        
        // Actualizar movimiento hacia el objetivo
        updateMovement(deltaTime);
        
        // Actualizar efectos visuales
        updateVisualEffects(deltaTime);
        
        // Regenerar energía
        regenerateEnergy(deltaTime);
        
        // Verificar nivel y experiencia
        checkLevelUp();
        
        super.update(deltaTime);
        
        // Mantener trayectoria de movimiento
        updateMovementPath();
    }
    
    /**
     * Actualiza las habilidades activas
     */
    private void updateActiveAbilities() {
        if (activeAbility != null && System.currentTimeMillis() >= abilityEndTime) {
            deactivateAbility(activeAbility);
            activeAbility = null;
        }
    }
    
    /**
     * Actualiza el movimiento hacia el objetivo
     */
    private void updateMovement(float deltaTime) {
        if (!isMoving) return;
        
        float dx = targetX - this.x;
        float dy = targetY - this.y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        
        if (distance < 5) {
            isMoving = false;
            velocityX = 0;
            velocityY = 0;
            return;
        }
        
        // Calcular velocidad hacia el objetivo
        float speed = maxSpeed;
        
        // Bonificaciones de velocidad
        if (activeAbility == SpecialAbility.SPEED_BOOST) {
            speed *= 2.0f;
        }
        
        velocityX = (dx / distance) * speed;
        velocityY = (dy / distance) * speed;
        
        normalizeVelocity();
    }
    
    /**
     * Actualiza los efectos visuales
     */
    private void updateVisualEffects(float deltaTime) {
        visualEffects.removeIf(effect -> {
            effect.update(deltaTime);
            return effect.isFinished();
        });
    }
    
    /**
     * Regenera energía con el tiempo
     */
    private void regenerateEnergy(float deltaTime) {
        if (currentEnergy < maxEnergy) {
            currentEnergy = Math.min(maxEnergy, currentEnergy + (int)(deltaTime * 0.5f));
        }
    }
    
    /**
     * Verifica si el jugador sube de nivel
     */
    private void checkLevelUp() {
        if (experience >= experienceToNextLevel) {
            levelUp();
        }
    }
    
    /**
     * El jugador sube de nivel
     */
    private void levelUp() {
        level++;
        experience -= experienceToNextLevel;
        experienceToNextLevel = level * 100; // Incremento exponencial
        
        // Bonificaciones por nivel
        maxEnergy += 10;
        currentEnergy = Math.min(currentEnergy + 20, maxEnergy);
        maxSpeed *= 1.05f; // Incremento sutil de velocidad
        
        // Efectos visuales de subida de nivel
        visualEffects.add(new VisualEffect("level_up", System.currentTimeMillis(), 3000));
        
        notifyListeners("levelUp", level);
        Log.d(TAG, playerName + " subió a nivel " + level);
    }
    
    /**
     * Actualiza la apariencia del jugador
     */
    private void updateAppearance() {
        // El tamaño aumenta con la masa
        float sizeMultiplier = (float) Math.sqrt(mass / baseMass);
        this.width = 30 * sizeMultiplier;
        this.height = 30 * sizeMultiplier;
        
        // Limitar tamaño máximo
        float maxSize = 200;
        this.width = Math.min(this.width, maxSize);
        this.height = Math.min(this.height, maxSize);
        
        updateBounds();
    }
    
    /**
     * Renderiza al jugador con todos sus efectos
     */
    @Override
    public void render(Canvas canvas) {
        if (!isActive) return;
        
        // Guardar estado del canvas
        canvas.save();
        
        // Aplicar efectos de camuflaje
        if (activeAbility == SpecialAbility.CAMOUFLAGE) {
            float alpha = 0.3f + 0.7f * (float)Math.sin(System.currentTimeMillis() * 0.01);
            paint.setAlpha((int)(255 * alpha));
        }
        
        // Renderizar trayectoria
        renderTrail(canvas);
        
        // Renderizar cuerpo principal
        super.render(canvas);
        
        // Renderizar borde según el rol
        renderRoleBorder(canvas);
        
        // Renderizar efectos activos
        renderActiveEffects(canvas);
        
        // Renderizar información del jugador
        renderPlayerInfo(canvas);
        
        // Restaurar estado del canvas
        canvas.restore();
    }
    
    /**
     * Renderiza la trayectoria de movimiento
     */
    private void renderTrail(Canvas canvas) {
        if (trailLength > 2 && movementPath != null) {
            Paint trailPaint = new Paint();
            trailPaint.setColor(color & 0x30FFFFFF); // Color semitransparente
            trailPaint.setStyle(Paint.Style.STROKE);
            trailPaint.setStrokeWidth(2);
            
            canvas.drawPath(movementPath, trailPaint);
        }
    }
    
    /**
     * Renderiza el borde según el rol
     */
    private void renderRoleBorder(Canvas canvas) {
        Paint borderPaint = new Paint();
        borderPaint.setColor(role.color);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3);
        
        canvas.drawCircle(x, y, width/2 + 2, borderPaint);
        
        // Indicador de rol
        float indicatorY = y - width/2 - 10;
        canvas.drawCircle(x, indicatorY, 5, borderPaint);
    }
    
    /**
     * Renderiza efectos activos
     */
    private void renderActiveEffects(Canvas canvas) {
        for (VisualEffect effect : visualEffects) {
            effect.render(canvas, x, y, width);
        }
        
        // Efectos especiales
        if (activeAbility == SpecialAbility.SHIELD) {
            Paint shieldPaint = new Paint();
            shieldPaint.setColor(0x404CAF50);
            shieldPaint.setStyle(Paint.Style.STROKE);
            shieldPaint.setStrokeWidth(5);
            
            float radius = width/2 + 10 + 5 * (float)Math.sin(System.currentTimeMillis() * 0.01);
            canvas.drawCircle(x, y, radius, shieldPaint);
        }
    }
    
    /**
     * Renderiza información del jugador
     */
    private void renderPlayerInfo(Canvas canvas) {
        // TODO: Implementar renderizado de UI (nombre, nivel, energía, etc.)
        // Esto requeriría un sistema de renderizado de texto
    }
    
    /**
     * Actualiza la trayectoria de movimiento
     */
    private void updateMovementPath() {
        if (isMoving) {
            if (trailLength == 0) {
                movementPath.moveTo(x, y);
            } else {
                movementPath.lineTo(x, y);
            }
            
            trailLength++;
            
            // Limitar longitud de la trayectoria
            if (trailLength > 50) {
                resetMovementPath();
            }
        }
    }
    
    /**
     * Reinicia la trayectoria de movimiento
     */
    private void resetMovementPath() {
        movementPath.reset();
        movementPath.moveTo(x, y);
        trailLength = 1;
    }
    
    /**
     * El jugador come comida u otra entidad
     */
    public void eat(Entity food) {
        if (state != PlayerState.ALIVE) return;
        
        float foodMass = food.getWidth() * food.getHeight() * 0.01f; // Masa estimada
        
        // Ganar experiencia y puntuación
        int expGained = (int)(foodMass * 0.5f);
        int scoreGained = (int)(foodMass * 10);
        
        gainExperience(expGained);
        addScore(scoreGained);
        
        // Aumentar masa
        mass += foodMass * 0.8f; // Eficiencia de conversión
        updateAppearance();
        
        // Estadísticas
        if (food instanceof Food) {
            foodEaten++;
        } else if (food instanceof Player) {
            playersEaten++;
        }
        
        notifyListeners("eat", food);
        Log.d(TAG, playerName + " comió " + food.getClass().getSimpleName() + 
                    " +" + expGained + " exp, +" + scoreGained + " puntos");
    }
    
    /**
     * Gana experiencia
     */
    public void gainExperience(int amount) {
        experience += amount;
        notifyListeners("experienceGained", amount);
    }
    
    /**
     * Añade puntuación
     */
    public void addScore(int points) {
        score += points;
        if (score > highScore) {
            highScore = score;
            notifyListeners("newHighScore", highScore);
        }
    }
    
    /**
     * Establece el objetivo de movimiento
     */
    public void setTarget(float targetX, float targetY) {
        this.targetX = targetX;
        this.targetY = targetY;
        isMoving = true;
        resetMovementPath();
    }
    
    /**
     * Activa una habilidad especial
     */
    public boolean activateAbility(SpecialAbility ability) {
        if (state != PlayerState.ALIVE) return false;
        if (activeAbility != null) return false;
        if (currentEnergy < ability.energyCost) return false;
        
        long currentTime = System.currentTimeMillis();
        Long lastUsed = abilityCooldowns.get(ability);
        
        if (lastUsed != null && currentTime - lastUsed < ability.cooldown) {
            Log.d(TAG, "Habilidad " + ability.displayName + " en cooldown");
            return false;
        }
        
        // Usar energía
        currentEnergy -= ability.energyCost;
        
        // Activar habilidad
        activateSpecificAbility(ability);
        activeAbility = ability;
        abilityEndTime = currentTime + 3000; // Duración base de 3 segundos
        abilityCooldowns.put(ability, currentTime);
        
        notifyListeners("abilityActivated", ability);
        Log.d(TAG, playerName + " activó habilidad " + ability.displayName);
        
        return true;
    }
    
    /**
     * Activa una habilidad específica
     */
    private void activateSpecificAbility(SpecialAbility ability) {
        switch (ability) {
            case SPEED_BOOST:
                // Ya manejado en updateMovement
                break;
            case MASS_SPLIT:
                // TODO: Implementar división de masa
                break;
            case TELEPORT:
                // TODO: Implementar teletransporte
                break;
            case SHIELD:
                // Ya manejado en renderActiveEffects
                break;
            case CAMOUFLAGE:
                // Ya manejado en render
                break;
        }
    }
    
    /**
     * Desactiva una habilidad
     */
    private void deactivateAbility(SpecialAbility ability) {
        // Efectos visuales
        visualEffects.add(new VisualEffect("ability_end", System.currentTimeMillis(), 1000));
        
        notifyListeners("abilityDeactivated", ability);
        Log.d(TAG, playerName + " desactivó habilidad " + ability.displayName);
    }
    
    /**
     * Cambia el rol del jugador
     */
    public void changeRole(PlayerRole newRole) {
        if (state != PlayerState.ALIVE) return;
        
        PlayerRole oldRole = this.role;
        this.role = newRole;
        applyRoleModifications();
        
        // Efectos visuales del cambio de rol
        visualEffects.add(new VisualEffect("role_change", System.currentTimeMillis(), 2000));
        
        notifyListeners("roleChanged", new Object[]{oldRole, newRole});
        Log.d(TAG, playerName + " cambió de rol " + oldRole.displayName + 
                    " a " + newRole.displayName);
    }
    
    /**
     * El jugador muere
     */
    @Override
    public void die() {
        if (state == PlayerState.DEAD) return;
        
        state = PlayerState.DEAD;
        deaths++;
        isActive = false;
        
        // Efectos visuales de muerte
        visualEffects.add(new VisualEffect("death", System.currentTimeMillis(), 3000));
        
        // Resetear algunas estadísticas
        score = 0;
        mass = baseMass;
        updateAppearance();
        
        notifyListeners("death", null);
        Log.d(TAG, playerName + " murió (muerte #" + deaths + ")");
    }
    
    /**
     * Respawn del jugador
     */
    public void respawn(float x, float y) {
        this.x = x;
        this.y = y;
        this.targetX = x;
        this.targetY = y;
        
        state = PlayerState.ALIVE;
        isAlive = true;
        isActive = true;
        isMoving = false;
        
        // Resetear estadísticas de vida
        mass = baseMass;
        updateAppearance();
        
        // Efectos visuales de respawn
        visualEffects.add(new VisualEffect("respawn", System.currentTimeMillis(), 2000));
        
        notifyListeners("respawn", new float[]{x, y});
        Log.d(TAG, playerName + " respawnó en (" + x + ", " + y + ")");
    }
    
    /**
     * Obtiene la energía disponible para una habilidad
     */
    public int getAvailableEnergy() {
        return currentEnergy;
    }
    
    /**
     * Verifica si una habilidad está disponible
     */
    public boolean isAbilityAvailable(SpecialAbility ability) {
        if (currentEnergy < ability.energyCost) return false;
        
        long currentTime = System.currentTimeMillis();
        Long lastUsed = abilityCooldowns.get(ability);
        
        return lastUsed == null || currentTime - lastUsed >= ability.cooldown;
    }
    
    /**
     * Obtiene el tiempo restante de cooldown para una habilidad
     */
    public long getAbilityCooldownRemaining(SpecialAbility ability) {
        Long lastUsed = abilityCooldowns.get(ability);
        if (lastUsed == null) return 0;
        
        long elapsed = System.currentTimeMillis() - lastUsed;
        long remaining = ability.cooldown - elapsed;
        
        return Math.max(0, remaining);
    }
    
    // Efecto visual simple
    private static class VisualEffect {
        private String type;
        private long startTime;
        private long duration;
        
        public VisualEffect(String type, long startTime, long duration) {
            this.type = type;
            this.startTime = startTime;
            this.duration = duration;
        }
        
        public void update(float deltaTime) {
            // Actualización lógica del efecto
        }
        
        public boolean isFinished() {
            return System.currentTimeMillis() - startTime >= duration;
        }
        
        public void render(Canvas canvas, float x, float y, float size) {
            // Renderizado básico del efecto
            if (isFinished()) return;
            
            float progress = (System.currentTimeMillis() - startTime) / (float)duration;
            
            Paint effectPaint = new Paint();
            effectPaint.setColor(0xFFFFD700); // Dorado
            
            if (type.equals("level_up")) {
                float alpha = 1.0f - progress;
                effectPaint.setAlpha((int)(255 * alpha));
                float scale = 1.0f + progress * 0.5f;
                canvas.drawCircle(x, y, size * scale, effectPaint);
            }
        }
    }
    
    // Getters para estadísticas y estado
    public String getPlayerName() { return playerName; }
    public PlayerRole getRole() { return role; }
    public PlayerState getState() { return state; }
    public int getScore() { return score; }
    public int getHighScore() { return highScore; }
    public float getMass() { return mass; }
    public int getLevel() { return level; }
    public int getExperience() { return experience; }
    public int getExperienceToNextLevel() { return experienceToNextLevel; }
    public int getCurrentEnergy() { return currentEnergy; }
    public int getMaxEnergy() { return maxEnergy; }
    public int getFoodEaten() { return foodEaten; }
    public int getPlayersEaten() { return playersEaten; }
    public int getDeaths() { return deaths; }
    public long getPlayTime() { return playTime; }
    public SpecialAbility getActiveAbility() { return activeAbility; }
    public boolean isMoving() { return isMoving; }
    public float getTargetX() { return targetX; }
    public float getTargetY() { return targetY; }
    
    // Setters
    public void setPlayerName(String playerName) { this.playerName = playerName; }
}