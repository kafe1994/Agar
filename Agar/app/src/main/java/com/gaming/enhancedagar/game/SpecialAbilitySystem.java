package com.gaming.enhancedagar.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.media.SoundPool;
import android.util.Log;
import android.view.MotionEvent;

import com.gaming.enhancedagar.engine.RoleSystem;
import com.gaming.enhancedagar.engine.VisualEffects;
import com.gaming.enhancedagar.entities.Player;
import com.gaming.enhancedagar.entities.Entity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sistema completo de habilidades especiales para Enhanced Agar
 * Implementa:
 * - Habilidades únicas por rol
 * - Sistema de energía/mana
 * - Cooldowns y duraciones
 * - Efectos visuales y sonoros
 * - Interacciones entre roles
 * - Escalado por tamaño/nivel
 * - Sistema de combos
 */
public class SpecialAbilitySystem {
    private static final String TAG = "SpecialAbilitySystem";
    
    // Constantes del sistema
    private static final float ENERGY_REGEN_RATE = 0.5f; // Energía por segundo
    private static final float MAX_ENERGY_MULTIPLIER = 1.5f;
    private static final float MIN_ENERGY_MULTIPLIER = 0.7f;
    private static final float COMBO_WINDOW_TIME = 2.5f; // Ventana de tiempo para combos
    private static final float POWER_SCALING_FACTOR = 0.1f; // Escalado por tamaño
    
    // Referencia al contexto de la aplicación
    private final Context context;
    
    // Mapa de habilidades activas por jugador
    private final Map<Player, ActiveAbilities> activeAbilities;
    
    // Efectos visuales del sistema
    private final VisualEffects visualEffects;
    
    // Pool de sonidos para efectos sonoros
    private final SoundPool soundPool;
    private final Map<AbilityType, Integer> soundIds;
    
    // Gestor de combos
    private final ComboManager comboManager;
    
    /**
     * Constructor del sistema de habilidades especiales
     */
    public SpecialAbilitySystem(Context context, VisualEffects visualEffects) {
        this.context = context;
        this.visualEffects = visualEffects;
        this.activeAbilities = new ConcurrentHashMap<>();
        this.soundPool = new SoundPool.Builder().setMaxStreams(10).build();
        this.soundIds = new HashMap<>();
        this.comboManager = new ComboManager();
        
        loadSounds();
        initializeAbilityDefinitions();
    }
    
    /**
     * Tipos de habilidades disponibles
     */
    public enum AbilityType {
        // Habilidades TANK
        SHIELD("Escudo", AbilityRole.TANK, AbilityCategory.DEFENSIVE),
        FORTIFICATION("Fortificación", AbilityRole.TANK, AbilityCategory.BUFF),
        
        // Habilidades ASSASSIN
        STEALTH("Sigilo", AbilityRole.ASSASSIN, AbilityCategory.UTILITY),
        SHADOW_STRIKE("Golpe Sombra", AbilityRole.ASSASSIN, AbilityCategory.OFFENSIVE),
        
        // Habilidades MAGE
        NOVA("Nova Arcana", AbilityRole.MAGE, AbilityCategory.OFFENSIVE),
        SPELL_BOOST("Impulso Mágico", AbilityRole.MAGE, AbilityCategory.BUFF),
        
        // Habilidades SUPPORT
        HEAL("Curación", AbilityRole.SUPPORT, AbilityCategory.HEALING),
        REGENERATION_AURA("Aura Regeneradora", AbilityRole.SUPPORT, AbilityCategory.HEALING),
        
        // Habilidades combinadas
        PROTECTIVE_NOVA("Nova Protectora", AbilityRole.COMBINED, AbilityCategory.DEFENSIVE),
        HEALING_WHIRLWIND("Torbellino Sanador", AbilityRole.COMBINED, AbilityCategory.HEALING),
        SHADOW_SHIELD("Escudo de Sombra", AbilityRole.COMBINED, AbilityCategory.DEFENSIVE);
        
        public final String displayName;
        public final AbilityRole role;
        public final AbilityCategory category;
        
        AbilityType(String displayName, AbilityRole role, AbilityCategory category) {
            this.displayName = displayName;
            this.role = role;
            this.category = category;
        }
    }
    
    /**
     * Roles de habilidades
     */
    public enum AbilityRole {
        TANK, ASSASSIN, MAGE, SUPPORT, COMBINED
    }
    
    /**
     * Categorías de habilidades
     */
    public enum AbilityCategory {
        OFFENSIVE, DEFENSIVE, HEALING, UTILITY, BUFF
    }
    
    /**
     * Estados de habilidades
     */
    public enum AbilityState {
        READY, COOLDOWN, ACTIVE, CASTING
    }
    
    /**
     * Definición completa de una habilidad
     */
    public static class AbilityDefinition {
        public final AbilityType type;
        public final String name;
        public final String description;
        public final float baseEnergyCost;
        public final float baseCooldown;
        public final float baseDuration;
        public final float baseRange;
        public final float baseEffect;
        public final AbilityCategory category;
        public final boolean requiresTarget;
        public final boolean isAOE;
        
        public AbilityDefinition(AbilityType type, String name, String description,
                               float baseEnergyCost, float baseCooldown, float baseDuration,
                               float baseRange, float baseEffect, AbilityCategory category,
                               boolean requiresTarget, boolean isAOE) {
            this.type = type;
            this.name = name;
            this.description = description;
            this.baseEnergyCost = baseEnergyCost;
            this.baseCooldown = baseCooldown;
            this.baseDuration = baseDuration;
            this.baseRange = baseRange;
            this.baseEffect = baseEffect;
            this.category = category;
            this.requiresTarget = requiresTarget;
            this.isAOE = isAOE;
        }
    }
    
    /**
     * Habilidad activa de un jugador
     */
    public static class ActiveAbility {
        public final AbilityType type;
        public final long startTime;
        public final float duration;
        public final float cooldownRemaining;
        public final float energyCost;
        public final float currentEffect;
        
        public ActiveAbility(AbilityType type, long startTime, float duration, 
                           float cooldownRemaining, float energyCost, float currentEffect) {
            this.type = type;
            this.startTime = startTime;
            this.duration = duration;
            this.cooldownRemaining = cooldownRemaining;
            this.energyCost = energyCost;
            this.currentEffect = currentEffect;
        }
        
        public boolean isExpired(long currentTime) {
            return currentTime - startTime >= duration * 1000;
        }
        
        public float getRemainingDuration(long currentTime) {
            float elapsed = (currentTime - startTime) / 1000.0f;
            return Math.max(0, duration - elapsed);
        }
    }
    
    /**
     * Habilidades activas de un jugador
     */
    public static class ActiveAbilities {
        public final Map<AbilityType, ActiveAbility> abilities;
        public float currentEnergy;
        public float maxEnergy;
        
        public ActiveAbilities() {
            this.abilities = new HashMap<>();
            this.currentEnergy = 100.0f;
            this.maxEnergy = 100.0f;
        }
        
        public boolean hasAbility(AbilityType type) {
            return abilities.containsKey(type) && !abilities.get(type).isExpired(System.currentTimeMillis());
        }
        
        public ActiveAbility getAbility(AbilityType type) {
            return abilities.get(type);
        }
    }
    
    /**
     * Gestor de combos de habilidades
     */
    public static class ComboManager {
        private final Map<Player, List<ComboStep>> playerCombos;
        private final Map<String, ComboDefinition> comboDefinitions;
        
        public ComboManager() {
            this.playerCombos = new HashMap<>();
            this.comboDefinitions = new HashMap<>();
            initializeComboDefinitions();
        }
        
        public void addAbilityUse(Player player, AbilityType ability, long timestamp) {
            playerCombos.computeIfAbsent(player, k -> new ArrayList<>())
                       .add(new ComboStep(ability, timestamp));
            
            // Limpiar pasos antiguos
            List<ComboStep> steps = playerCombos.get(player);
            steps.removeIf(step -> (timestamp - step.timestamp) / 1000.0f > COMBO_WINDOW_TIME);
            
            // Verificar si se completó un combo
            checkForCombo(player, steps);
        }
        
        private void checkForCombo(Player player, List<ComboStep> steps) {
            if (steps.size() < 2) return;
            
            // Crear secuencia de habilidades usadas
            List<AbilityType> sequence = steps.stream()
                    .sorted(Comparator.comparingLong(step -> step.timestamp))
                    .map(step -> step.ability)
                    .toList();
            
            // Verificar contra definiciones de combos
            for (ComboDefinition combo : comboDefinitions.values()) {
                if (matchesComboSequence(sequence, combo.sequence)) {
                    Log.d(TAG, "Combo executed: " + combo.name + " by " + player.getPlayerName());
                    executeCombo(player, combo);
                    break;
                }
            }
        }
        
        private boolean matchesComboSequence(List<AbilityType> sequence, List<AbilityType> comboSequence) {
            if (sequence.size() < comboSequence.size()) return false;
            
            int end = sequence.size();
            int start = end - comboSequence.size();
            
            for (int i = 0; i < comboSequence.size(); i++) {
                if (sequence.get(start + i) != comboSequence.get(i)) {
                    return false;
                }
            }
            return true;
        }
        
        private void executeCombo(Player player, ComboDefinition combo) {
            // Implementar ejecución de combo
            combo.onExecute.accept(player);
            
            // Limpiar el combo después de ejecutarlo
            playerCombos.remove(player);
        }
        
        private void initializeComboDefinitions() {
            // Combo: MAGE NOVA + TANK SHIELD = PROTECTIVE_NOVA
            comboDefinitions.put("NOVA_SHIELD", new ComboDefinition(
                "Nova Protectora",
                Arrays.asList(AbilityType.NOVA, AbilityType.SHIELD),
                player -> {
                    // Ejecutar nova protectora
                }
            ));
            
            // Combo: SUPPORT HEAL + MAGE SPELL_BOOST = HEALING_WHIRLWIND
            comboDefinitions.put("HEAL_SPELL", new ComboDefinition(
                "Torbellino Sanador",
                Arrays.asList(AbilityType.HEAL, AbilityType.SPELL_BOOST),
                player -> {
                    // Ejecutar torbellino sanador
                }
            ));
            
            // Combo: ASSASSIN STEALTH + TANK SHIELD = SHADOW_SHIELD
            comboDefinitions.put("STEALTH_SHIELD", new ComboDefinition(
                "Escudo de Sombra",
                Arrays.asList(AbilityType.STEALTH, AbilityType.SHIELD),
                player -> {
                    // Ejecutar escudo de sombra
                }
            ));
        }
        
        private static class ComboStep {
            final AbilityType ability;
            final long timestamp;
            
            ComboStep(AbilityType ability, long timestamp) {
                this.ability = ability;
                this.timestamp = timestamp;
            }
        }
        
        private static class ComboDefinition {
            final String name;
            final List<AbilityType> sequence;
            final java.util.function.Consumer<Player> onExecute;
            
            ComboDefinition(String name, List<AbilityType> sequence, 
                          java.util.function.Consumer<Player> onExecute) {
                this.name = name;
                this.sequence = sequence;
                this.onExecute = onExecute;
            }
        }
    }
    
    /**
     * Mapa de definiciones de habilidades
     */
    private final Map<AbilityType, AbilityDefinition> abilityDefinitions;
    
    /**
     * Inicializa las definiciones de habilidades
     */
    private void initializeAbilityDefinitions() {
        abilityDefinitions = new HashMap<>();
        
        // Habilidades TANK
        abilityDefinitions.put(AbilityType.SHIELD, new AbilityDefinition(
            AbilityType.SHIELD, "Escudo", "Protección temporal que absorbe daño",
            25.0f, 15.0f, 8.0f, 0.0f, 150.0f, AbilityCategory.DEFENSIVE,
            false, false
        ));
        
        abilityDefinitions.put(AbilityType.FORTIFICATION, new AbilityDefinition(
            AbilityType.FORTIFICATION, "Fortificación", "Aumenta defensa y resistencia",
            40.0f, 20.0f, 12.0f, 50.0f, 2.0f, AbilityCategory.BUFF,
            false, true
        ));
        
        // Habilidades ASSASSIN
        abilityDefinitions.put(AbilityType.STEALTH, new AbilityDefinition(
            AbilityType.STEALTH, "Sigilo", "Invisibilidad temporal con aumento de velocidad",
            30.0f, 18.0f, 5.0f, 0.0f, 1.8f, AbilityCategory.UTILITY,
            false, false
        ));
        
        abilityDefinitions.put(AbilityType.SHADOW_STRIKE, new AbilityDefinition(
            AbilityType.SHADOW_STRIKE, "Golpe Sombra", "Ataque crítico que ignora defensa",
            35.0f, 12.0f, 0.0f, 30.0f, 2.5f, AbilityCategory.OFFENSIVE,
            true, false
        ));
        
        // Habilidades MAGE
        abilityDefinitions.put(AbilityType.NOVA, new AbilityDefinition(
            AbilityType.NOVA, "Nova Arcana", "Explosión mágica que daña enemigos cercanos",
            50.0f, 25.0f, 0.0f, 80.0f, 1.8f, AbilityCategory.OFFENSIVE,
            false, true
        ));
        
        abilityDefinitions.put(AbilityType.SPELL_BOOST, new AbilityDefinition(
            AbilityType.SPELL_BOOST, "Impulso Mágico", "Aumenta daño mágico temporalmente",
            20.0f, 30.0f, 10.0f, 0.0f, 1.5f, AbilityCategory.BUFF,
            false, false
        ));
        
        // Habilidades SUPPORT
        abilityDefinitions.put(AbilityType.HEAL, new AbilityDefinition(
            AbilityType.HEAL, "Curación", "Restaura vida a ti y aliados cercanos",
            45.0f, 8.0f, 3.0f, 60.0f, 80.0f, AbilityCategory.HEALING,
            false, true
        ));
        
        abilityDefinitions.put(AbilityType.REGENERATION_AURA, new AbilityDefinition(
            AbilityType.REGENERATION_AURA, "Aura Regeneradora", "Aura que regenera vida continuamente",
            60.0f, 45.0f, 15.0f, 100.0f, 5.0f, AbilityCategory.HEALING,
            false, true
        ));
    }
    
    /**
     * Carga los sonidos de las habilidades
     */
    private void loadSounds() {
        // Cargar sonidos de habilidades (requiere archivos de audio en res/raw/)
        try {
            soundIds.put(AbilityType.SHIELD, soundPool.load(context, R.raw.shield_sound, 1));
            soundIds.put(AbilityType.NOVA, soundPool.load(context, R.raw.nova_sound, 1));
            soundIds.put(AbilityType.HEAL, soundPool.load(context, R.raw.heal_sound, 1));
            soundIds.put(AbilityType.STEALTH, soundPool.load(context, R.raw.stealth_sound, 1));
            soundIds.put(AbilityType.SHADOW_STRIKE, soundPool.load(context, R.raw.shadow_strike_sound, 1));
        } catch (Exception e) {
            Log.w(TAG, "No se pudieron cargar algunos sonidos: " + e.getMessage());
        }
    }
    
    /**
     * Actualiza el sistema de habilidades
     */
    public void update(float deltaTime) {
        long currentTime = System.currentTimeMillis();
        
        // Actualizar energía y remover habilidades expiradas
        for (Player player : activeAbilities.keySet()) {
            ActiveAbilities playerAbilities = activeAbilities.get(player);
            
            // Regenerar energía
            playerAbilities.currentEnergy = Math.min(
                playerAbilities.maxEnergy,
                playerAbilities.currentEnergy + ENERGY_REGEN_RATE * deltaTime
            );
            
            // Remover habilidades expiradas
            playerAbilities.abilities.entrySet().removeIf(entry ->
                entry.getValue().isExpired(currentTime)
            );
        }
        
        // Actualizar efectos visuales
        visualEffects.update(deltaTime);
    }
    
    /**
     * Intenta usar una habilidad
     */
    public boolean useAbility(Player player, AbilityType abilityType, float targetX, float targetY) {
        ActiveAbilities playerAbilities = activeAbilities.computeIfAbsent(player, k -> new ActiveAbilities());
        AbilityDefinition definition = abilityDefinitions.get(abilityType);
        
        if (definition == null) {
            Log.w(TAG, "Habilidad no encontrada: " + abilityType);
            return false;
        }
        
        // Verificar si el jugador tiene suficiente energía
        float energyCost = calculateEnergyCost(player, definition);
        if (playerAbilities.currentEnergy < energyCost) {
            Log.d(TAG, "Energía insuficiente para usar " + abilityType.displayName);
            return false;
        }
        
        // Verificar cooldown
        if (isOnCooldown(player, abilityType)) {
            Log.d(TAG, abilityType.displayName + " en cooldown");
            return false;
        }
        
        // Verificar compatibilidad con el rol del jugador
        if (!isCompatibleWithRole(player, abilityType)) {
            Log.d(TAG, "Habilidad incompatible con el rol del jugador");
            return false;
        }
        
        // Ejecutar la habilidad
        executeAbility(player, abilityType, definition, targetX, targetY, energyCost);
        
        // Registrar uso para combos
        comboManager.addAbilityUse(player, abilityType, System.currentTimeMillis());
        
        return true;
    }
    
    /**
     * Ejecuta una habilidad
     */
    private void executeAbility(Player player, AbilityType abilityType, AbilityDefinition definition,
                              float targetX, float targetY, float energyCost) {
        ActiveAbilities playerAbilities = activeAbilities.get(player);
        
        // Reducir energía
        playerAbilities.currentEnergy -= energyCost;
        
        // Calcular efectos escalados
        float scaledEffect = calculateScaledEffect(player, definition.baseEffect);
        float scaledDuration = definition.baseDuration * getPowerMultiplier(player);
        float scaledCooldown = definition.baseCooldown * getCooldownMultiplier(player);
        
        // Crear habilidad activa
        ActiveAbility activeAbility = new ActiveAbility(
            abilityType,
            System.currentTimeMillis(),
            scaledDuration,
            scaledCooldown,
            energyCost,
            scaledEffect
        );
        
        // Agregar a habilidades activas
        playerAbilities.abilities.put(abilityType, activeAbility);
        
        // Aplicar efectos específicos según el tipo de habilidad
        applyAbilityEffects(player, abilityType, targetX, targetY, scaledEffect);
        
        // Reproducir efectos visuales y sonoros
        playAbilityEffects(player, abilityType);
        
        Log.d(TAG, "Habilidad ejecutada: " + abilityType.displayName + 
                  " por " + player.getPlayerName());
    }
    
    /**
     * Aplica los efectos específicos de cada habilidad
     */
    private void applyAbilityEffects(Player player, AbilityType abilityType, 
                                   float targetX, float targetY, float effect) {
        switch (abilityType) {
            case SHIELD:
                // Escudo: absorber daño por duración
                applyShieldEffect(player, effect);
                break;
                
            case STEALTH:
                // Sigilo: invisibilidad y aumento de velocidad
                applyStealthEffect(player, effect);
                break;
                
            case NOVA:
                // Nova: daño en área
                applyNovaEffect(player, targetX, targetY, effect);
                break;
                
            case HEAL:
                // Curación: restaurar vida
                applyHealEffect(player, targetX, targetY, effect);
                break;
                
            case FORTIFICATION:
                // Fortificación: aumentar defensa
                applyFortificationEffect(player, effect);
                break;
                
            case SHADOW_STRIKE:
                // Golpe sombra: ataque crítico
                applyShadowStrikeEffect(player, targetX, targetY, effect);
                break;
                
            case SPELL_BOOST:
                // Impulso mágico: aumentar daño mágico
                applySpellBoostEffect(player, effect);
                break;
                
            case REGENERATION_AURA:
                // Aura regeneradora: curación continua
                applyRegenerationAuraEffect(player, effect);
                break;
        }
    }
    
    /**
     * Verifica si una habilidad está en cooldown
     */
    public boolean isOnCooldown(Player player, AbilityType abilityType) {
        ActiveAbilities playerAbilities = activeAbilities.get(player);
        if (playerAbilities == null) return false;
        
        ActiveAbility ability = playerAbilities.getAbility(abilityType);
        return ability != null && ability.cooldownRemaining > 0;
    }
    
    /**
     * Obtiene el cooldown restante de una habilidad
     */
    public float getCooldownRemaining(Player player, AbilityType abilityType) {
        ActiveAbilities playerAbilities = activeAbilities.get(player);
        if (playerAbilities == null) return 0;
        
        ActiveAbility ability = playerAbilities.getAbility(abilityType);
        if (ability == null) return 0;
        
        return ability.cooldownRemaining;
    }
    
    /**
     * Verifica si una habilidad es compatible con el rol del jugador
     */
    private boolean isCompatibleWithRole(Player player, AbilityType abilityType) {
        if (abilityType.role == AbilityRole.COMBINED) {
            // Los combos pueden ser usados por cualquier rol
            return true;
        }
        
        RoleSystem.RoleType playerRole = getPlayerRole(player);
        
        return (abilityType.role == AbilityRole.TANK && playerRole == RoleSystem.RoleType.TANK) ||
               (abilityType.role == AbilityRole.ASSASSIN && playerRole == RoleSystem.RoleType.ASSASSIN) ||
               (abilityType.role == AbilityRole.MAGE && playerRole == RoleSystem.RoleType.MAGE) ||
               (abilityType.role == AbilityRole.SUPPORT && playerRole == RoleSystem.RoleType.SUPPORT);
    }
    
    /**
     * Obtiene el rol del jugador
     */
    private RoleSystem.RoleType getPlayerRole(Player player) {
        // Implementar lógica para obtener el rol del jugador
        // Por ahora retornamos un rol por defecto
        return RoleSystem.RoleType.TANK;
    }
    
    /**
     * Calcula el costo de energía escalado
     */
    private float calculateEnergyCost(Player player, AbilityDefinition definition) {
        float sizeMultiplier = 1.0f + (getPlayerSize(player) * POWER_SCALING_FACTOR);
        return definition.baseEnergyCost * sizeMultiplier;
    }
    
    /**
     * Calcula el efecto escalado
     */
    private float calculateScaledEffect(Player player, float baseEffect) {
        float sizeMultiplier = getPowerMultiplier(player);
        return baseEffect * sizeMultiplier;
    }
    
    /**
     * Obtiene el multiplicador de poder basado en el tamaño del jugador
     */
    private float getPowerMultiplier(Player player) {
        float size = getPlayerSize(player);
        return 1.0f + (size * POWER_SCALING_FACTOR);
    }
    
    /**
     * Obtiene el multiplicador de cooldown
     */
    private float getCooldownMultiplier(Player player) {
        // Los jugadores más grandes tienen cooldowns ligeramente más largos
        float size = getPlayerSize(player);
        return 1.0f + (size * 0.05f);
    }
    
    /**
     * Obtiene el tamaño del jugador
     */
    private float getPlayerSize(Player player) {
        // Calcular tamaño basado en el radio o masa del jugador
        return player.getRadius() / 50.0f; // Normalizar tamaño
    }
    
    /**
     * Reproduce efectos visuales y sonoros de una habilidad
     */
    private void playAbilityEffects(Player player, AbilityType abilityType) {
        // Efectos visuales
        visualEffects.playAbilityEffect(abilityType, player.getX(), player.getY());
        
        // Efectos sonoros
        Integer soundId = soundIds.get(abilityType);
        if (soundId != null) {
            soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f);
        }
    }
    
    // Implementaciones específicas de efectos de habilidades
    
    private void applyShieldEffect(Player player, float effect) {
        // Implementar escudo que absorbe daño
        Log.d(TAG, "Aplicando escudo a " + player.getPlayerName() + " con efecto " + effect);
    }
    
    private void applyStealthEffect(Player player, float effect) {
        // Implementar invisibilidad
        Log.d(TAG, "Aplicando sigilo a " + player.getPlayerName() + " con efecto " + effect);
    }
    
    private void applyNovaEffect(Player player, float x, float y, float effect) {
        // Implementar explosión mágica
        Log.d(TAG, "Aplicando nova a " + player.getPlayerName() + " en (" + x + ", " + y + ") con efecto " + effect);
    }
    
    private void applyHealEffect(Player player, float x, float y, float effect) {
        // Implementar curación
        Log.d(TAG, "Aplicando curación a " + player.getPlayerName() + " con efecto " + effect);
    }
    
    private void applyFortificationEffect(Player player, float effect) {
        // Implementar aumento de defensa
        Log.d(TAG, "Aplicando fortificación a " + player.getPlayerName() + " con efecto " + effect);
    }
    
    private void applyShadowStrikeEffect(Player player, float x, float y, float effect) {
        // Implementar golpe sombra
        Log.d(TAG, "Aplicando golpe sombra desde " + player.getPlayerName() + " hacia (" + x + ", " + y + ") con efecto " + effect);
    }
    
    private void applySpellBoostEffect(Player player, float effect) {
        // Implementar aumento de daño mágico
        Log.d(TAG, "Aplicando impulso mágico a " + player.getPlayerName() + " con efecto " + effect);
    }
    
    private void applyRegenerationAuraEffect(Player player, float effect) {
        // Implementar aura regeneradora
        Log.d(TAG, "Aplicando aura regeneradora a " + player.getPlayerName() + " con efecto " + effect);
    }
    
    /**
     * Renderiza las habilidades activas en pantalla
     */
    public void render(Canvas canvas, Paint paint) {
        for (Player player : activeAbilities.keySet()) {
            ActiveAbilities playerAbilities = activeAbilities.get(player);
            
            // Renderizar efectos de habilidades activas
            for (ActiveAbility ability : playerAbilities.abilities.values()) {
                if (!ability.isExpired(System.currentTimeMillis())) {
                    renderAbilityEffect(canvas, paint, player, ability);
                }
            }
        }
    }
    
    /**
     * Renderiza el efecto de una habilidad específica
     */
    private void renderAbilityEffect(Canvas canvas, Paint paint, Player player, ActiveAbility ability) {
        switch (ability.type) {
            case SHIELD:
                renderShieldEffect(canvas, paint, player, ability);
                break;
            case STEALTH:
                renderStealthEffect(canvas, paint, player, ability);
                break;
            case NOVA:
                renderNovaEffect(canvas, paint, player, ability);
                break;
            case HEAL:
                renderHealEffect(canvas, paint, player, ability);
                break;
            // Agregar más efectos de renderizado...
        }
    }
    
    // Métodos de renderizado específicos
    
    private void renderShieldEffect(Canvas canvas, Paint paint, Player player, ActiveAbility ability) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(0x804CAF50); // Verde translúcido
        paint.setStrokeWidth(5.0f);
        
        float radius = player.getRadius() + 20;
        canvas.drawCircle(player.getX(), player.getY(), radius, paint);
    }
    
    private void renderStealthEffect(Canvas canvas, Paint paint, Player player, ActiveAbility ability) {
        // Efecto de invisibilidad: renderizar con transparencia
        paint.setAlpha(50); // Semi-transparente
    }
    
    private void renderNovaEffect(Canvas canvas, Paint paint, Player player, ActiveAbility ability) {
        // Efecto de explosión mágica
        float elapsed = (System.currentTimeMillis() - ability.startTime) / 1000.0f;
        float progress = elapsed / ability.duration;
        
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x60FF4500); // Naranja translúcido
        paint.setAlpha((int)(255 * (1.0f - progress)));
        
        float radius = player.getRadius() + (progress * 100);
        canvas.drawCircle(player.getX(), player.getY(), radius, paint);
    }
    
    private void renderHealEffect(Canvas canvas, Paint paint, Player player, ActiveAbility ability) {
        // Efecto de curación: partículas verdes
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x804CAF50); // Verde translúcido
        
        float radius = player.getRadius() + 30;
        canvas.drawCircle(player.getX(), player.getY(), radius, paint);
    }
    
    /**
     * Obtiene información de habilidades para UI
     */
    public String getAbilityInfo(Player player, AbilityType abilityType) {
        AbilityDefinition definition = abilityDefinitions.get(abilityType);
        if (definition == null) return "Habilidad no encontrada";
        
        ActiveAbilities playerAbilities = activeAbilities.get(player);
        boolean onCooldown = isOnCooldown(player, abilityType);
        float cooldownRemaining = getCooldownRemaining(player, abilityType);
        float energyCost = calculateEnergyCost(player, definition);
        
        StringBuilder info = new StringBuilder();
        info.append(definition.name).append("\n");
        info.append(definition.description).append("\n");
        info.append("Costo de energía: ").append(energyCost).append("\n");
        info.append("Cooldown: ").append(definition.baseCooldown).append("s");
        
        if (onCooldown) {
            info.append(" (Restante: ").append(cooldownRemaining).append("s)");
        }
        
        return info.toString();
    }
    
    /**
     * Limpia recursos del sistema
     */
    public void cleanup() {
        soundPool.release();
        activeAbilities.clear();
        comboManager.playerCombos.clear();
    }
}