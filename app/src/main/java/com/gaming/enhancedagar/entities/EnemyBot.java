package com.gaming.enhancedagar.entities;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;

/**
 * EnemyBot - Bot inteligente con IA avanzada para Enhanced Agar
 * Extiende Entity e implementa un sistema completo de IA con roles,
 * comportamientos adaptativos y detección de amenazas
 */
public class EnemyBot extends Entity {
    private static final String TAG = "EnemyBot";
    
    // Tipos de roles del bot con stats específicas
    public enum BotRole {
        TANK("Tanque", 0xFF8D6E63, 1.4f, 0.6f, 1.8f, 0.4f),
        ASSASSIN("Asesino", 0xFF212121, 0.7f, 1.6f, 0.8f, 1.2f),
        GUARDIAN("Guardián", 0xFF3F51B5, 1.1f, 1.0f, 1.3f, 0.8f),
        SCOUT("Explorador", 0xFF4CAF50, 1.5f, 1.4f, 0.6f, 1.0f),
        HYBRID("Híbrido", 0xFFFF5722, 1.0f, 1.1f, 1.0f, 0.9f),
        WANDERER("Vagabundo", 0xFF607D8B, 1.0f, 0.8f, 0.8f, 0.6f);
        
        public final String displayName;
        public final int color;
        public final float sizeMultiplier;      // Factor de tamaño
        public final float speedMultiplier;     // Factor de velocidad
        public final float aggressionMultiplier; // Factor de agresión
        public final float intelligenceMultiplier; // Factor de inteligencia
        
        BotRole(String displayName, int color, float sizeMultiplier, 
                float speedMultiplier, float aggressionMultiplier, float intelligenceMultiplier) {
            this.displayName = displayName;
            this.color = color;
            this.sizeMultiplier = sizeMultiplier;
            this.speedMultiplier = speedMultiplier;
            this.aggressionMultiplier = aggressionMultiplier;
            this.intelligenceMultiplier = intelligenceMultiplier;
        }
    }
    
    // Estados de comportamiento del bot
    public enum BehaviorState {
        WANDERING("Vagabundeando"),
        CHASING("Persiguiendo"),
        FLEEING("Huyendo"),
        HUNTING("Cazando"),
        GUARDING("Vigilando"),
        INVESTIGATING("Investigando"),
        STUNNED("Aturdido");
        
        public final String displayName;
        
        BehaviorState(String displayName) {
            this.displayName = displayName;
        }
    }
    
    // Estados de alerta del bot
    public enum AlertLevel {
        CALM("Calmado", 0.3f),
        ALERT("Alerta", 0.6f),
        AGGRESSIVE("Agresivo", 0.9f),
        PANIC("Pánico", 1.0f);
        
        public final String displayName;
        public final float reactionMultiplier;
        
        AlertLevel(String displayName, float reactionMultiplier) {
            this.displayName = displayName;
            this.reactionMultiplier = reactionMultiplier;
        }
    }
    
    // Propiedades del bot
    private BotRole role;
    private BehaviorState currentBehavior;
    private AlertLevel alertLevel;
    
    // IA y detección
    private List<Entity> detectedEntities;
    private List<Entity> threats;
    private List<Entity> opportunities;
    private Entity primaryTarget;
    private Entity currentThreat;
    
    // Sistema de decisiones
    private float decisionCooldown;
    private float lastDecisionTime;
    private float behaviorTimer;
    private float wanderTargetX, wanderTargetY;
    private boolean hasWanderTarget;
    
    // Memoria del bot
    private Map<Long, EntityMemory> entityMemories;
    private float memoryDecayRate;
    private float lastSeenPlayerTime;
    
    // Estadísticas de IA
    private int decisionsMade;
    private int successfulActions;
    private float averageDecisionTime;
    private long totalDecisionTime;
    
    // Efectos temporales
    private float stunTimeRemaining;
    private float confusionTimeRemaining;
    private float fearTimeRemaining;
    
    // Sistema de prioridades
    private static final float THREAT_PRIORITY = 100.0f;
    private static final float FOOD_PRIORITY = 10.0f;
    private static final float WANDER_PRIORITY = 1.0f;
    
    // Configuración de detección
    private float detectionRadius;
    private float threatDetectionRadius;
    private float aggroRange;
    
    // RNG y tiempo
    private Random random;
    private float lastUpdateTime;
    
    // Memoria de entidades para IA avanzada
    private static class EntityMemory {
        public Entity entity;
        public float lastSeenTime;
        public float lastKnownX, lastKnownY;
        public float threatLevel;
        public int encounterCount;
        
        public EntityMemory(Entity entity) {
            this.entity = entity;
            this.lastSeenTime = System.currentTimeMillis();
            this.lastKnownX = entity.getX();
            this.lastKnownY = entity.getY();
            this.threatLevel = 0.0f;
            this.encounterCount = 1;
        }
    }
    
    /**
     * Constructor del EnemyBot
     */
    public EnemyBot(BotRole role, float startX, float startY) {
        super(startX, startY, 30 * role.sizeMultiplier, 30 * role.sizeMultiplier);
        
        this.role = role;
        this.currentBehavior = BehaviorState.WANDERING;
        this.alertLevel = AlertLevel.CALM;
        
        this.detectedEntities = new ArrayList<>();
        this.threats = new ArrayList<>();
        this.opportunities = new ArrayList<>();
        this.primaryTarget = null;
        this.currentThreat = null;
        
        this.decisionCooldown = 1000; // 1 segundo entre decisiones
        this.lastDecisionTime = 0;
        this.behaviorTimer = 0;
        this.hasWanderTarget = false;
        
        this.entityMemories = new HashMap<>();
        this.memoryDecayRate = 30000; // 30 segundos para olvidar
        
        this.decisionsMade = 0;
        this.successfulActions = 0;
        this.averageDecisionTime = 0;
        this.totalDecisionTime = 0;
        
        this.stunTimeRemaining = 0;
        this.confusionTimeRemaining = 0;
        this.fearTimeRemaining = 0;
        
        this.random = new Random();
        this.lastUpdateTime = System.currentTimeMillis();
        
        // Configurar detección según el rol
        configureDetectionByRole();
        
        // Aplicar propiedades del rol
        applyRoleProperties();
        
        Log.d(TAG, "EnemyBot " + role.displayName + " creado en (" + startX + ", " + startY + ")");
    }
    
    /**
     * Configura los parámetros de detección según el rol
     */
    private void configureDetectionByRole() {
        switch (role) {
            case SCOUT:
                detectionRadius = 400;
                threatDetectionRadius = 500;
                aggroRange = 300;
                break;
            case ASSASSIN:
                detectionRadius = 250;
                threatDetectionRadius = 400;
                aggroRange = 200;
                break;
            case GUARDIAN:
                detectionRadius = 350;
                threatDetectionRadius = 450;
                aggroRange = 250;
                break;
            case TANK:
                detectionRadius = 200;
                threatDetectionRadius = 300;
                aggroRange = 150;
                break;
            case HYBRID:
                detectionRadius = 300;
                threatDetectionRadius = 400;
                aggroRange = 225;
                break;
            case WANDERER:
                detectionRadius = 150;
                threatDetectionRadius = 200;
                aggroRange = 100;
                break;
        }
    }
    
    /**
     * Aplica las propiedades específicas del rol
     */
    private void applyRoleProperties() {
        // Velocidad base modificada por rol
        this.maxSpeed = 5.0f * role.speedMultiplier;
        
        // Color específico del rol
        this.color = role.color;
        paint.setColor(color);
        
        // Inteligencia afecta frecuencia de decisiones
        this.decisionCooldown = 1000 / role.intelligenceMultiplier;
        
        // Ajustar sensibilidad de alerta
        float alertSensitivity = 0.5f + (role.intelligenceMultiplier * 0.5f);
        
        Log.d(TAG, "Propiedades aplicadas para " + role.displayName + 
                    ": velocidad=" + maxSpeed + ", color=" + Integer.toHexString(color));
    }
    
    /**
     * Actualiza la lógica del bot
     */
    @Override
    public void update(float deltaTime) {
        if (!isActive) return;
        
        float currentTime = System.currentTimeMillis();
        float dt = currentTime - lastUpdateTime;
        lastUpdateTime = currentTime;
        
        // Actualizar efectos temporales
        updateTemporalEffects(dt);
        
        // Si está aturdido, no puede actuar
        if (stunTimeRemaining > 0) {
            currentBehavior = BehaviorState.STUNNED;
            velocityX *= 0.95f; // Desaceleración natural
            velocityY *= 0.95f;
            return;
        }
        
        // Detectar entidades en el entorno
        detectEntities();
        
        // Evaluar amenazas y oportunidades
        evaluateThreatsAndOpportunities();
        
        // Tomar decisiones periódicamente
        if (currentTime - lastDecisionTime >= decisionCooldown) {
            makeDecision(currentTime);
            lastDecisionTime = currentTime;
        }
        
        // Ejecutar comportamiento actual
        executeCurrentBehavior(deltaTime);
        
        // Actualizar memoria
        updateMemories(currentTime);
        
        // Actualizar posición básica
        super.update(deltaTime);
        
        // Mantener dentro de límites
        maintainBounds();
    }
    
    /**
     * Actualiza efectos temporales como stun, confusión, miedo
     */
    private void updateTemporalEffects(float deltaTime) {
        if (stunTimeRemaining > 0) {
            stunTimeRemaining -= deltaTime;
        }
        if (confusionTimeRemaining > 0) {
            confusionTimeRemaining -= deltaTime;
        }
        if (fearTimeRemaining > 0) {
            fearTimeRemaining -= deltaTime;
        }
    }
    
    /**
     * Detecta entidades en el rango de visión
     */
    private void detectEntities() {
        detectedEntities.clear();
        
        List<Entity> allEntities = Entity.getActiveEntities();
        
        for (Entity entity : allEntities) {
            if (entity == this || !entity.isActive()) continue;
            
            float distance = getDistanceTo(entity);
            
            // Solo detectar dentro del rango de detección
            if (distance <= detectionRadius) {
                detectedEntities.add(entity);
            }
        }
    }
    
    /**
     * Evalúa amenazas y oportunidades en el entorno
     */
    private void evaluateThreatsAndOpportunities() {
        threats.clear();
        opportunities.clear();
        
        for (Entity entity : detectedEntities) {
            float threatLevel = calculateThreatLevel(entity);
            float opportunityLevel = calculateOpportunityLevel(entity);
            
            // Clasificar como amenaza u oportunidad
            if (threatLevel > 20.0f) {
                threats.add(entity);
            } else if (opportunityLevel > 15.0f) {
                opportunities.add(entity);
            }
        }
        
        // Ordenar por prioridad
        sortEntitiesByPriority(threats);
        sortEntitiesByPriority(opportunities);
        
        // Actualizar amenaza principal
        updatePrimaryThreat();
    }
    
    /**
     * Calcula el nivel de amenaza de una entidad
     */
    private float calculateThreatLevel(Entity entity) {
        if (!(entity instanceof Player)) return 0.0f;
        
        Player player = (Player) entity;
        float distance = getDistanceTo(entity);
        float sizeAdvantage = player.getWidth() / this.width;
        
        // Los jugadores grandes son más amenazantes
        float sizeThreat = Math.max(0, (sizeAdvantage - 1.0f) * 50);
        
        // Amenaza por proximidad (más cerca = más amenazante)
        float proximityThreat = Math.max(0, (threatDetectionRadius - distance) / threatDetectionRadius * 30);
        
        // Amenaza por estado del jugador (si está potenciado)
        float stateThreat = player.getActiveAbility() != null ? 20 : 0;
        
        // Memory-based threat (amenazas conocidas son más temibles)
        float memoryThreat = 0.0f;
        EntityMemory memory = entityMemories.get(player.getId());
        if (memory != null && memory.threatLevel > 0) {
            memoryThreat = memory.threatLevel * 0.5f;
        }
        
        // Modificar por rol del bot
        float roleMod = role.aggressionMultiplier * alertLevel.reactionMultiplier;
        
        return (sizeThreat + proximityThreat + stateThreat + memoryThreat) * roleMod;
    }
    
    /**
     * Calcula el nivel de oportunidad de una entidad
     */
    private float calculateOpportunityLevel(Entity entity) {
        if (entity instanceof Food) {
            return 20.0f; // Prioridad fija para comida
        } else if (entity instanceof EnemyBot) {
            // Oportunidad vs otros bots basada en tamaño
            EnemyBot otherBot = (EnemyBot) entity;
            float sizeAdvantage = this.width / otherBot.getWidth();
            if (sizeAdvantage > 1.5f) {
                return 25.0f * sizeAdvantage;
            }
        } else if (entity instanceof Player) {
            // Oportunidad vs jugadores pequeños
            Player player = (Player) entity;
            float sizeAdvantage = this.width / player.getWidth();
            if (sizeAdvantage > 1.3f) {
                return 30.0f * sizeAdvantage;
            }
        }
        
        return 0.0f;
    }
    
    /**
     * Ordena entidades por prioridad de amenaza/oportunidad
     */
    private void sortEntitiesByPriority(List<Entity> entities) {
        entities.sort((e1, e2) -> {
            float p1 = calculatePriority(e1);
            float p2 = calculatePriority(e2);
            return Float.compare(p2, p1); // Descendente
        });
    }
    
    /**
     * Calcula la prioridad general de una entidad
     */
    private float calculatePriority(Entity entity) {
        float threat = calculateThreatLevel(entity);
        float opportunity = calculateOpportunityLevel(entity);
        
        // Las amenazas tienen mayor prioridad que las oportunidades
        return Math.max(threat, opportunity);
    }
    
    /**
     * Actualiza la amenaza principal del bot
     */
    private void updatePrimaryThreat() {
        if (threats.isEmpty()) {
            currentThreat = null;
            return;
        }
        
        // La amenaza con mayor prioridad se convierte en amenaza principal
        float maxThreat = 0;
        for (Entity threat : threats) {
            float threatLevel = calculateThreatLevel(threat);
            if (threatLevel > maxThreat) {
                maxThreat = threatLevel;
                currentThreat = threat;
            }
        }
    }
    
    /**
     * Toma una decisión basada en el estado actual
     */
    private void makeDecision(float currentTime) {
        decisionsMade++;
        
        long decisionStart = System.currentTimeMillis();
        
        // Evaluar situación actual
        BehaviorState previousBehavior = currentBehavior;
        
        // Decision tree de la IA
        if (!threats.isEmpty() && shouldFlee()) {
            // Fuga inmediata
            setBehavior(BehaviorState.FLEEING);
            primaryTarget = null;
        } else if (!opportunities.isEmpty() && shouldChase()) {
            // Perseguir objetivo
            primaryTarget = opportunities.get(0);
            setBehavior(BehaviorState.CHASING);
        } else if (isGuardRole() && hasTerritory()) {
            // Comportamiento de guardia
            setBehavior(BehaviorState.GUARDING);
        } else if (currentBehavior == BehaviorState.WANDERING && 
                   behaviorTimer < getMinWanderTime()) {
            // Continuar vagabundeando
            // No cambiar comportamiento
        } else {
            // Cambiar a vagabundeo o investigar
            if (shouldInvestigate()) {
                setBehavior(BehaviorState.INVESTIGATING);
            } else {
                setBehavior(BehaviorState.WANDERING);
            }
        }
        
        // Actualizar memoria de decisiones
        long decisionTime = System.currentTimeMillis() - decisionStart;
        totalDecisionTime += decisionTime;
        averageDecisionTime = totalDecisionTime / (float)decisionsMade;
        
        // Estadísticas de éxito
        if (previousBehavior != currentBehavior) {
            behaviorTimer = 0;
        }
        
        Log.d(TAG, role.displayName + " decisió: " + currentBehavior.displayName + 
                    " (objetivo: " + (primaryTarget != null ? primaryTarget.getClass().getSimpleName() : "none") + ")");
    }
    
    /**
     * Determina si el bot debe huir
     */
    private boolean shouldFlee() {
        if (currentThreat == null) return false;
        
        float threatLevel = calculateThreatLevel(currentThreat);
        float fleeThreshold = 30.0f * role.intelligenceMultiplier;
        
        // Factores adicionales para huir
        if (stunTimeRemaining > 0) return true;
        if (fearTimeRemaining > 0) return true;
        if (this.width < currentThreat.getWidth() * 0.8f) return true;
        
        return threatLevel > fleeThreshold;
    }
    
    /**
     * Determina si el bot debe perseguir
     */
    private boolean shouldChase() {
        if (primaryTarget == null) return false;
        
        float opportunityLevel = calculateOpportunityLevel(primaryTarget);
        float chaseThreshold = 20.0f / role.aggressionMultiplier;
        
        // Solo perseguir si es ventajoso
        return opportunityLevel > chaseThreshold;
    }
    
    /**
     * Verifica si el bot es de rol guardián
     */
    private boolean isGuardRole() {
        return role == BotRole.GUARDIAN || role == BotRole.TANK;
    }
    
    /**
     * Verifica si el bot tiene territorio que vigilar
     */
    private boolean hasTerritory() {
        // Por simplicidad, siempre tiene territorio
        // En una implementación más compleja, sería configurable
        return true;
    }
    
    /**
     * Determina si debe investigar algo
     */
    private boolean shouldInvestigate() {
        // Investigar si hay actividad reciente o si es rol scout
        return role == BotRole.SCOUT || lastSeenPlayerTime > 0;
    }
    
    /**
     * Obtiene el tiempo mínimo de vagabundeo
     */
    private float getMinWanderTime() {
        return 3000 * role.intelligenceMultiplier;
    }
    
    /**
     * Establece un nuevo comportamiento
     */
    private void setBehavior(BehaviorState newBehavior) {
        if (currentBehavior != newBehavior) {
            BehaviorState oldBehavior = this.currentBehavior;
            this.currentBehavior = newBehavior;
            this.behaviorTimer = 0;
            
            notifyListeners("behaviorChanged", new Object[]{oldBehavior, newBehavior});
            Log.d(TAG, role.displayName + " cambió de " + oldBehavior.displayName + 
                        " a " + newBehavior.displayName);
        }
    }
    
    /**
     * Ejecuta el comportamiento actual
     */
    private void executeCurrentBehavior(float deltaTime) {
        behaviorTimer += deltaTime;
        
        switch (currentBehavior) {
            case WANDERING:
                executeWanderingBehavior();
                break;
            case CHASING:
                executeChasingBehavior();
                break;
            case FLEEING:
                executeFleeingBehavior();
                break;
            case HUNTING:
                executeHuntingBehavior();
                break;
            case GUARDING:
                executeGuardingBehavior();
                break;
            case INVESTIGATING:
                executeInvestigatingBehavior();
                break;
            case STUNNED:
                executeStunnedBehavior();
                break;
        }
        
        // Actualizar alerta basada en comportamiento
        updateAlertLevel();
    }
    
    /**
     * Comportamiento de vagabundeo
     */
    private void executeWanderingBehavior() {
        if (!hasWanderTarget || behaviorTimer > getWanderDuration()) {
            generateNewWanderTarget();
        }
        
        if (hasWanderTarget) {
            moveTowards(wanderTargetX, wanderTargetY, maxSpeed * 0.5f);
            
            // Si llegó al objetivo, generar uno nuevo
            float distance = (float) Math.sqrt(
                Math.pow(wanderTargetX - x, 2) + Math.pow(wanderTargetY - y, 2)
            );
            
            if (distance < 50) {
                hasWanderTarget = false;
            }
        }
    }
    
    /**
     * Comportamiento de persecución
     */
    private void executeChasingBehavior() {
        if (primaryTarget == null || !primaryTarget.isActive()) {
            setBehavior(BehaviorState.WANDERING);
            return;
        }
        
        float targetX = primaryTarget.getX();
        float targetY = primaryTarget.getY();
        
        // Movimiento predictivo básico
        float predictionFactor = 0.3f;
        float predictedX = targetX + primaryTarget.getVelocityX() * predictionFactor * 1000;
        float predictedY = targetY + primaryTarget.getVelocityY() * predictionFactor * 1000;
        
        moveTowards(predictedX, predictedY, maxSpeed);
        
        // Si el objetivo está muy lejos, reconsiderar
        float distance = getDistanceTo(primaryTarget);
        if (distance > detectionRadius * 1.5f) {
            primaryTarget = null;
            setBehavior(BehaviorState.WANDERING);
        }
    }
    
    /**
     * Comportamiento de huida
     */
    private void executeFleeingBehavior() {
        if (currentThreat == null || !currentThreat.isActive()) {
            setBehavior(BehaviorState.WANDERING);
            return;
        }
        
        // Calcular dirección opuesta a la amenaza
        float awayX = x - currentThreat.getX();
        float awayY = y - currentThreat.getY();
        float distance = (float) Math.sqrt(awayX * awayX + awayY * awayY);
        
        if (distance > 0) {
            // Mover en dirección opuesta con velocidad máxima
            awayX /= distance;
            awayY /= distance;
            
            float fleeDistance = aggroRange * 2;
            float targetX = x + awayX * fleeDistance;
            float targetY = y + awayY * fleeDistance;
            
            moveTowards(targetX, targetY, maxSpeed * 1.2f);
        }
        
        // Si la amenaza está lejos, reducir alerta
        float threatDistance = getDistanceTo(currentThreat);
        if (threatDistance > threatDetectionRadius * 1.5f) {
            setBehavior(BehaviorState.WANDERING);
            currentThreat = null;
        }
    }
    
    /**
     * Comportamiento de caza (más agresivo que persecución)
     */
    private void executeHuntingBehavior() {
        // Similar a chase pero más agresivo
        executeChasingBehavior();
        
        // Comportamiento adicional de caza
        if (primaryTarget != null) {
            // Posicionamiento táctico
            float distance = getDistanceTo(primaryTarget);
            if (distance < 100) {
                // Ajustar velocidad para no overshoot
                velocityX *= 0.9f;
                velocityY *= 0.9f;
            }
        }
    }
    
    /**
     * Comportamiento de guardia
     */
    private void executeGuardingBehavior() {
        // Patrullar un área específica
        if (!hasWanderTarget || behaviorTimer > 5000) {
            generatePatrolTarget();
        }
        
        if (hasWanderTarget) {
            moveTowards(wanderTargetX, wanderTargetY, maxSpeed * 0.7f);
        }
        
        // Vigilancia activa - revisar amenazas frecuentemente
        if (behaviorTimer > 2000) {
            evaluateThreatsAndOpportunities();
        }
    }
    
    /**
     * Comportamiento de investigación
     */
    private void executeInvestigatingBehavior() {
        // Buscar signos de actividad
        if (lastSeenPlayerTime > 0) {
            long timeSinceLastSeen = System.currentTimeMillis() - lastSeenPlayerTime;
            if (timeSinceLastSeen < 10000) { // 10 segundos de memoria
                // Moverse hacia la última posición conocida del jugador
                // Esto requeriría memoria de posiciones pasadas
            }
        }
        
        // Alternar entre investigar y vagabundear
        if (behaviorTimer > 4000) {
            setBehavior(BehaviorState.WANDERING);
        }
    }
    
    /**
     * Comportamiento cuando está aturdido
     */
    private void executeStunnedBehavior() {
        // Solo moverse por inercia, sin control activo
        velocityX *= 0.98f;
        velocityY *= 0.98f;
    }
    
    /**
     * Genera un nuevo objetivo de vagabundeo
     */
    private void generateNewWanderTarget() {
        float wanderRange = 200;
        wanderTargetX = x + (random.nextFloat() - 0.5f) * wanderRange * 2;
        wanderTargetY = y + (random.nextFloat() - 0.5f) * wanderRange * 2;
        
        // Asegurar que esté dentro de límites razonables
        wanderTargetX = Math.max(50, Math.min(wanderTargetX, 750));
        wanderTargetY = Math.max(50, Math.min(wanderTargetY, 1350));
        
        hasWanderTarget = true;
    }
    
    /**
     * Genera un objetivo de patrulla para guardianes
     */
    private void generatePatrolTarget() {
        // Patrullar en un patrón circular alrededor de un punto central
        float patrolRadius = 150;
        float angle = random.nextFloat() * 2 * (float) Math.PI;
        
        wanderTargetX = x + (float) Math.cos(angle) * patrolRadius;
        wanderTargetY = y + (float) Math.sin(angle) * patrolRadius;
        
        hasWanderTarget = true;
    }
    
    /**
     * Obtiene la duración del vagabundeo
     */
    private float getWanderDuration() {
        return 2000 + random.nextInt(4000); // 2-6 segundos
    }
    
    /**
     * Actualiza el nivel de alerta basado en la situación
     */
    private void updateAlertLevel() {
        AlertLevel newAlertLevel = AlertLevel.CALM;
        
        if (!threats.isEmpty()) {
            float maxThreat = 0;
            for (Entity threat : threats) {
                maxThreat = Math.max(maxThreat, calculateThreatLevel(threat));
            }
            
            if (maxThreat > 80) {
                newAlertLevel = AlertLevel.PANIC;
            } else if (maxThreat > 50) {
                newAlertLevel = AlertLevel.AGGRESSIVE;
            } else if (maxThreat > 20) {
                newAlertLevel = AlertLevel.ALERT;
            }
        } else if (!opportunities.isEmpty()) {
            newAlertLevel = AlertLevel.ALERT;
        }
        
        if (this.alertLevel != newAlertLevel) {
            this.alertLevel = newAlertLevel;
            notifyListeners("alertLevelChanged", newAlertLevel);
        }
    }
    
    /**
     * Actualiza las memorias de entidades
     */
    private void updateMemories(long currentTime) {
        // Decay de memorias
        entityMemories.entrySet().removeIf(entry -> {
            EntityMemory memory = entry.getValue();
            return currentTime - memory.lastSeenTime > memoryDecayRate;
        });
        
        // Actualizar memorias de entidades detectadas
        for (Entity entity : detectedEntities) {
            EntityMemory memory = entityMemories.get(entity.getId());
            if (memory == null) {
                memory = new EntityMemory(entity);
                entityMemories.put(entity.getId(), memory);
            } else {
                memory.lastSeenTime = currentTime;
                memory.lastKnownX = entity.getX();
                memory.lastKnownY = entity.getY();
                memory.encounterCount++;
                
                // Actualizar nivel de amenaza basado en encuentros
                if (entity instanceof Player) {
                    float threatIncrease = calculateThreatLevel(entity) * 0.1f;
                    memory.threatLevel = Math.min(100, memory.threatLevel + threatIncrease);
                }
            }
        }
        
        // Registrar última vez que vio un jugador
        for (Entity entity : detectedEntities) {
            if (entity instanceof Player) {
                lastSeenPlayerTime = currentTime;
                break;
            }
        }
    }
    
    /**
     * Mantiene al bot dentro de los límites de pantalla
     */
    private void maintainBounds() {
        float screenWidth = 800; // TODO: Obtener dimensiones reales de pantalla
        float screenHeight = 1400;
        
        float margin = 50;
        
        if (x < margin) {
            x = margin;
            velocityX = Math.abs(velocityX);
        } else if (x > screenWidth - margin) {
            x = screenWidth - margin;
            velocityX = -Math.abs(velocityX);
        }
        
        if (y < margin) {
            y = margin;
            velocityY = Math.abs(velocityY);
        } else if (y > screenHeight - margin) {
            y = screenHeight - margin;
            velocityY = -Math.abs(velocityY);
        }
    }
    
    /**
     * Renderiza el bot con efectos visuales según su estado
     */
    @Override
    public void draw(Canvas canvas) {
        if (!isVisible) return;
        
        // Guardar estado del canvas
        canvas.save();
        
        // Efectos visuales según comportamiento
        renderBehaviorEffects(canvas);
        
        // Renderizar cuerpo principal
        drawCircle(canvas);
        
        // Renderizar indicadores de estado
        renderStateIndicators(canvas);
        
        // Renderizar información de debug (opcional)
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            renderDebugInfo(canvas);
        }
        
        // Restaurar estado del canvas
        canvas.restore();
    }
    
    /**
     * Renderiza efectos visuales del comportamiento
     */
    private void renderBehaviorEffects(Canvas canvas) {
        Paint effectPaint = new Paint();
        
        switch (currentBehavior) {
            case FLEEING:
                effectPaint.setColor(0x40FF0000); // Rojo semitransparente
                effectPaint.setStyle(Paint.Style.STROKE);
                effectPaint.setStrokeWidth(5);
                float fleeRadius = width/2 + 8 + 3 * (float)Math.sin(System.currentTimeMillis() * 0.02);
                canvas.drawCircle(x, y, fleeRadius, effectPaint);
                break;
                
            case CHASING:
                effectPaint.setColor(0x4000FF00); // Verde semitransparente
                effectPaint.setStyle(Paint.Style.STROKE);
                effectPaint.setStrokeWidth(3);
                float chaseRadius = width/2 + 5;
                canvas.drawCircle(x, y, chaseRadius, effectPaint);
                break;
                
            case INVESTIGATING:
                effectPaint.setColor(0x40FFFF00); // Amarillo semitransparente
                effectPaint.setStyle(Paint.Style.STROKE);
                effectPaint.setStrokeWidth(2);
                canvas.drawCircle(x, y, width/2 + 3, effectPaint);
                break;
        }
        
        // Efectos de estado temporal
        if (stunTimeRemaining > 0) {
            effectPaint.setColor(0x40808080); // Gris
            effectPaint.setStyle(Paint.Style.STROKE);
            effectPaint.setStrokeWidth(4);
            canvas.drawCircle(x, y, width/2 + 10, effectPaint);
        }
    }
    
    /**
     * Renderiza indicadores de estado
     */
    private void renderStateIndicators(Canvas canvas) {
        Paint indicatorPaint = new Paint();
        indicatorPaint.setStyle(Paint.Style.FILL);
        
        float indicatorY = y - width/2 - 8;
        
        // Indicador de rol
        indicatorPaint.setColor(role.color);
        canvas.drawCircle(x - 8, indicatorY, 4, indicatorPaint);
        
        // Indicador de alerta
        float alertOffset = 15 * alertLevel.ordinal();
        indicatorPaint.setColor(getAlertColor());
        canvas.drawCircle(x + alertOffset - 7, indicatorY, 3, indicatorPaint);
    }
    
    /**
     * Obtiene el color según el nivel de alerta
     */
    private int getAlertColor() {
        switch (alertLevel) {
            case CALM: return 0xFF4CAF50;    // Verde
            case ALERT: return 0xFFFF9800;   // Naranja
            case AGGRESSIVE: return 0xFFF44336; // Rojo
            case PANIC: return 0xFF9C27B0;   // Púrpura
            default: return 0xFF808080;
        }
    }
    
    /**
     * Renderiza información de debug
     */
    private void renderDebugInfo(Canvas canvas) {
        Paint debugPaint = new Paint();
        debugPaint.setColor(0xFF000000);
        debugPaint.setTextSize(10);
        debugPaint.setAntiAlias(true);
        
        String debugText = String.format("%s\n%s\n%d ents", 
            role.displayName,
            currentBehavior.displayName,
            detectedEntities.size()
        );
        
        // TODO: Implementar renderizado de texto
        // Esto requeriría un sistema de fuentes
    }
    
    /**
     * El bot es comido por otra entidad
     */
    @Override
    public void die() {
        if (!isAlive) return;
        
        isAlive = false;
        isActive = false;
        
        // Estadísticas finales
        notifyListeners("death", new BotStatistics(this));
        
        Log.d(TAG, "EnemyBot " + role.displayName + " murió. Decisiones: " + decisionsMade + 
                    ", Éxito: " + (decisionsMade > 0 ? (successfulActions * 100 / decisionsMade) : 0) + "%");
    }
    
    /**
     * Aplica stun al bot
     */
    public void applyStun(float duration) {
        this.stunTimeRemaining = duration;
        setBehavior(BehaviorState.STUNNED);
        Log.d(TAG, role.displayName + " aturdido por " + duration + "ms");
    }
    
    /**
     * Aplica confusión al bot
     */
    public void applyConfusion(float duration) {
        this.confusionTimeRemaining = duration;
        // La confusión afecta las decisiones pero no el movimiento
        Log.d(TAG, role.displayName + " confundido por " + duration + "ms");
    }
    
    /**
     * Aplica miedo al bot
     */
    public void applyFear(float duration) {
        this.fearTimeRemaining = duration;
        alertLevel = AlertLevel.PANIC;
        Log.d(TAG, role.displayName + " tiene miedo por " + duration + "ms");
    }
    
    /**
     * Actualiza la posición con IA mejorada
     */
    @Override
    protected void updateBasicPosition(float deltaTime) {
        // Aplicar efectos de confusión al movimiento
        if (confusionTimeRemaining > 0) {
            velocityX += (random.nextFloat() - 0.5f) * 0.5f;
            velocityY += (random.nextFloat() - 0.5f) * 0.5f;
        }
        
        super.updateBasicPosition(deltaTime);
    }
    
    /**
     * Obtiene la efectividad del bot
     */
    public float getEffectiveness() {
        if (decisionsMade == 0) return 0.0f;
        return (successfulActions * 100.0f) / decisionsMade;
    }
    
    /**
     * Registra una acción exitosa
     */
    public void recordSuccessfulAction() {
        successfulActions++;
    }
    
    // Getters para estado y estadísticas
    public BotRole getRole() { return role; }
    public BehaviorState getCurrentBehavior() { return currentBehavior; }
    public AlertLevel getAlertLevel() { return alertLevel; }
    public Entity getPrimaryTarget() { return primaryTarget; }
    public Entity getCurrentThreat() { return currentThreat; }
    public int getDetectedEntitiesCount() { return detectedEntities.size(); }
    public int getThreatsCount() { return threats.size(); }
    public int getOpportunitiesCount() { return opportunities.size(); }
    public int getDecisionsMade() { return decisionsMade; }
    public float getEffectivenessPercentage() { return getEffectiveness(); }
    public float getAverageDecisionTime() { return averageDecisionTime; }
    public boolean isStunned() { return stunTimeRemaining > 0; }
    public boolean isConfused() { return confusionTimeRemaining > 0; }
    public boolean isScared() { return fearTimeRemaining > 0; }
    public float getStunTimeRemaining() { return stunTimeRemaining; }
    public float getConfusionTimeRemaining() { return confusionTimeRemaining; }
    public float getFearTimeRemaining() { return fearTimeRemaining; }
    
    /**
     * Clase para estadísticas del bot
     */
    public static class BotStatistics {
        public final BotRole role;
        public final BehaviorState finalBehavior;
        public final int decisionsMade;
        public final float effectiveness;
        public final float survivalTime;
        public final int entitiesDetected;
        public final int threatsEncountered;
        
        public BotStatistics(EnemyBot bot) {
            this.role = bot.role;
            this.finalBehavior = bot.currentBehavior;
            this.decisionsMade = bot.decisionsMade;
            this.effectiveness = bot.getEffectiveness();
            this.survivalTime = System.currentTimeMillis() - bot.getCreationTime();
            this.entitiesDetected = bot.detectedEntities.size();
            this.threatsEncountered = bot.threats.size();
        }
    }
    
    // Tiempo de creación para estadísticas
    private long creationTime;
    
    public long getCreationTime() { return creationTime; }
    
    {
        creationTime = System.currentTimeMillis();
    }
}