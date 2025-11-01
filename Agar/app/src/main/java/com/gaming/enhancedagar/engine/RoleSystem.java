package com.gaming.enhancedagar.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Sistema de roles para el juego Enhanced Agar
 * Implementa 4 tipos de roles con ventajas, desventajas y balanceo único
 */
public class RoleSystem {
    
    /**
     * Enum que define los 4 tipos de roles disponibles
     */
    public enum RoleType {
        TANK("Tank", "Alto HP y defensa, bajo daño"),
        ASSASSIN("Assassin", "Alta velocidad y daño crítico"),
        MAGE("Mage", "Habilidades mágicas y daño a distancia"),
        SUPPORT("Support", "Habilidades de curación y soporte");
        
        private final String displayName;
        private final String description;
        
        RoleType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Mapa de ventajas entre roles (quem gana contra quien)
     */
    private static final Map<RoleType, Set<RoleType>> ADVANTAGES = new HashMap<>();
    
    /**
     * Mapa de desventajas entre roles
     */
    private static final Map<RoleType, Set<RoleType>> DISADVANTAGES = new HashMap<>();
    
    /**
     * Modificadores de daño base entre roles
     */
    private static final Map<String, Double> DAMAGE_MODIFIERS = new HashMap<>();
    
    /**
     * Estadísticas base por rol
     */
    private static final Map<RoleType, RoleStats> ROLE_STATS = new HashMap<>();
    
    /**
     * Habilidades especiales por rol
     */
    private static final Map<RoleType, SpecialAbility> SPECIAL_ABILITIES = new HashMap<>();
    
    static {
        initializeAdvantages();
        initializeDisadvantages();
        initializeDamageModifiers();
        initializeRoleStats();
        initializeSpecialAbilities();
    }
    
    /**
     * Inicializa las ventajas entre roles
     */
    private static void initializeAdvantages() {
        // TANK tiene ventaja contra ASSASSIN (alta defensa)
        ADVANTAGES.put(RoleType.TANK, Set.of(RoleType.ASSASSIN));
        
        // ASSASSIN tiene ventaja contra MAGE (velocidad vs magia)
        ADVANTAGES.put(RoleType.ASSASSIN, Set.of(RoleType.MAGE));
        
        // MAGE tiene ventaja contra SUPPORT (magia vs curación)
        ADVANTAGES.put(RoleType.MAGE, Set.of(RoleType.SUPPORT));
        
        // SUPPORT tiene ventaja contra TANK (soporte prolongado)
        ADVANTAGES.put(RoleType.SUPPORT, Set.of(RoleType.TANK));
    }
    
    /**
     * Inicializa las desventajas entre roles
     */
    private static void initializeDisadvantages() {
        // TANK es vulnerable a MAGE (magia penetra armadura)
        DISADVANTAGES.put(RoleType.TANK, Set.of(RoleType.MAGE));
        
        // ASSASSIN es vulnerable a TANK (velocidad vs defensa)
        DISADVANTAGES.put(RoleType.ASSASSIN, Set.of(RoleType.TANK));
        
        // MAGE es vulnerable a ASSASSIN (fragilidad vs velocidad)
        DISADVANTAGES.put(RoleType.MAGE, Set.of(RoleType.ASSASSIN));
        
        // SUPPORT es vulnerable a MAGE (soporte vs magia)
        DISADVANTAGES.put(RoleType.SUPPORT, Set.of(RoleType.MAGE));
    }
    
    /**
     * Inicializa los modificadores de daño
     */
    private static void initializeDamageModifiers() {
        // Modificadores cuando attacker tiene ventaja
        DAMAGE_MODIFIERS.put("TANK_ASSASSIN", 1.5); // Tank golpea fuerte a Assassin
        DAMAGE_MODIFIERS.put("ASSASSIN_MAGE", 1.4); // Assassin es rápido contra Mage
        DAMAGE_MODIFIERS.put("MAGE_SUPPORT", 1.3); // Mage domina al Support
        DAMAGE_MODIFIERS.put("SUPPORT_TANK", 1.2); // Support agota al Tank
        
        // Modificadores cuando attacker tiene desventaja
        DAMAGE_MODIFIERS.put("TANK_MAGE", 0.7); // Tank es vulnerable a Mage
        DAMAGE_MODIFIERS.put("ASSASSIN_TANK", 0.8); // Assassin no puede penetrar Tank
        DAMAGE_MODIFIERS.put("MAGE_ASSASSIN", 0.75); // Mage es lento contra Assassin
        DAMAGE_MODIFIERS.put("SUPPORT_MAGE", 0.85); // Support es débil contra Mage
        
        // Modificadores neutros (mismo rol)
        DAMAGE_MODIFIERS.put("TANK_TANK", 1.0);
        DAMAGE_MODIFIERS.put("ASSASSIN_ASSASSIN", 1.0);
        DAMAGE_MODIFIERS.put("MAGE_MAGE", 1.0);
        DAMAGE_MODIFIERS.put("SUPPORT_SUPPORT", 1.0);
        
        // Modificadores entre roles sin ventaja/desventaja específica
        DAMAGE_MODIFIERS.put("TANK_SUPPORT", 1.0);
        DAMAGE_MODIFIERS.put("ASSASSIN_SUPPORT", 1.0);
        DAMAGE_MODIFIERS.put("MAGE_TANK", 1.1);
        DAMAGE_MODIFIERS.put("SUPPORT_ASSASSIN", 1.0);
    }
    
    /**
     * Inicializa las estadísticas base por rol
     */
    private static void initializeRoleStats() {
        ROLE_STATS.put(RoleType.TANK, new RoleStats(
            150,  // maxHP
            8.0,  // attackDamage
            15.0, // defense
            3.0,  // speed
            0.8,  // attackSpeed
            5.0   // energy
        ));
        
        ROLE_STATS.put(RoleType.ASSASSIN, new RoleStats(
            80,   // maxHP
            12.0, // attackDamage
            5.0,  // defense
            8.0,  // speed
            1.5,  // attackSpeed
            4.0   // energy
        ));
        
        ROLE_STATS.put(RoleType.MAGE, new RoleStats(
            100,  // maxHP
            15.0, // attackDamage (daño mágico)
            6.0,  // defense
            4.0,  // speed
            1.2,  // attackSpeed
            6.0   // energy
        ));
        
        ROLE_STATS.put(RoleType.SUPPORT, new RoleStats(
            120,  // maxHP
            6.0,  // attackDamage
            8.0,  // defense
            5.0,  // speed
            0.9,  // attackSpeed
            8.0   // energy (mucha energía para habilidades)
        ));
    }
    
    /**
     * Inicializa las habilidades especiales por rol
     */
    private static void initializeSpecialAbilities() {
        SPECIAL_ABILITIES.put(RoleType.TANK, new SpecialAbility(
            "Fortificación",
            "Aumenta la defensa en 50% por 5 segundos",
            15.0, // cooldown
            5.0,  // duración
            1.5   // multiplicador
        ));
        
        SPECIAL_ABILITIES.put(RoleType.ASSASSIN, new SpecialAbility(
            "Golpe Sombra",
            "Ataque crítico que ignora 40% de la defensa",
            12.0, // cooldown
            0.0,  // duración instantánea
            2.0   // multiplicador de daño
        ));
        
        SPECIAL_ABILITIES.put(RoleType.MAGE, new SpecialAbility(
            "Bola de Fuego",
            "Proyectil mágico que causa daño en área",
            10.0, // cooldown
            0.0,  // duración instantánea
            1.8   // multiplicador de daño
        ));
        
        SPECIAL_ABILITIES.put(RoleType.SUPPORT, new SpecialAbility(
            "Regeneración",
            "Cura HP a sí mismo y aliados cercanos",
            8.0,  // cooldown
            3.0,  // duración
            0.3   // multiplicador de curación (por segundo)
        ));
    }
    
    /**
     * Verifica si un rol tiene ventaja contra otro
     */
    public static boolean hasAdvantage(RoleType attacker, RoleType defender) {
        Set<RoleType> advantages = ADVANTAGES.get(attacker);
        return advantages != null && advantages.contains(defender);
    }
    
    /**
     * Verifica si un rol tiene desventaja contra otro
     */
    public static boolean hasDisadvantage(RoleType attacker, RoleType defender) {
        Set<RoleType> disadvantages = DISADVANTAGES.get(attacker);
        return disadvantages != null && disadvantages.contains(defender);
    }
    
    /**
     * Calcula el modificador de daño basado en los roles del atacante y defensor
     */
    public static double getDamageModifier(RoleType attacker, RoleType defender) {
        String key = attacker.name() + "_" + defender.name();
        return DAMAGE_MODIFIERS.getOrDefault(key, 1.0);
    }
    
    /**
     * Calcula el daño final aplicando modificadores de rol
     */
    public static double calculateFinalDamage(RoleType attacker, RoleType defender, double baseDamage) {
        double modifier = getDamageModifier(attacker, defender);
        return baseDamage * modifier;
    }
    
    /**
     * Obtiene las estadísticas base de un rol
     */
    public static RoleStats getRoleStats(RoleType role) {
        return ROLE_STATS.get(role);
    }
    
    /**
     * Obtiene la habilidad especial de un rol
     */
    public static SpecialAbility getSpecialAbility(RoleType role) {
        return SPECIAL_ABILITIES.get(role);
    }
    
    /**
     * Clase que encapsula las estadísticas de un rol
     */
    public static class RoleStats {
        private final double maxHP;
        private final double attackDamage;
        private final double defense;
        private final double speed;
        private final double attackSpeed;
        private final double energy;
        
        public RoleStats(double maxHP, double attackDamage, double defense, 
                        double speed, double attackSpeed, double energy) {
            this.maxHP = maxHP;
            this.attackDamage = attackDamage;
            this.defense = defense;
            this.speed = speed;
            this.attackSpeed = attackSpeed;
            this.energy = energy;
        }
        
        // Getters
        public double getMaxHP() { return maxHP; }
        public double getAttackDamage() { return attackDamage; }
        public double getDefense() { return defense; }
        public double getSpeed() { return speed; }
        public double getAttackSpeed() { return attackSpeed; }
        public double getEnergy() { return energy; }
        
        /**
         * Aplica un multiplicador a todas las estadísticas
         */
        public RoleStats multiplyAll(double multiplier) {
            return new RoleStats(
                maxHP * multiplier,
                attackDamage * multiplier,
                defense * multiplier,
                speed * multiplier,
                attackSpeed * multiplier,
                energy * multiplier
            );
        }
        
        @Override
        public String toString() {
            return String.format(
                "HP: %.0f, ATK: %.1f, DEF: %.1f, SPD: %.1f, AS: %.1f, EN: %.1f",
                maxHP, attackDamage, defense, speed, attackSpeed, energy
            );
        }
    }
    
    /**
     * Clase que encapsula una habilidad especial
     */
    public static class SpecialAbility {
        private final String name;
        private final String description;
        private final double cooldown;
        private final double duration;
        private final double effectMultiplier;
        
        public SpecialAbility(String name, String description, double cooldown, 
                            double duration, double effectMultiplier) {
            this.name = name;
            this.description = description;
            this.cooldown = cooldown;
            this.duration = duration;
            this.effectMultiplier = effectMultiplier;
        }
        
        // Getters
        public String getName() { return name; }
        public String getDescription() { return description; }
        public double getCooldown() { return cooldown; }
        public double getDuration() { return duration; }
        public double getEffectMultiplier() { return effectMultiplier; }
        
        /**
         * Verifica si la habilidad es instantánea
         */
        public boolean isInstant() {
            return duration <= 0.0;
        }
        
        /**
         * Verifica si la habilidad tiene duración
         */
        public boolean hasDuration() {
            return duration > 0.0;
        }
        
        @Override
        public String toString() {
            return String.format(
                "%s: %s (CD: %.1fs, Dur: %.1fs, Mult: %.1f)",
                name, description, cooldown, duration, effectMultiplier
            );
        }
    }
    
    /**
     * Obtiene información detallada sobre las ventajas y desventajas de un rol
     */
    public static String getRoleInfo(RoleType role) {
        StringBuilder info = new StringBuilder();
        info.append(role.getDisplayName()).append(" - ").append(role.getDescription()).append("\n");
        
        Set<RoleType> advantages = ADVANTAGES.get(role);
        Set<RoleType> disadvantages = DISADVANTAGES.get(role);
        
        if (advantages != null && !advantages.isEmpty()) {
            info.append("Ventajas contra: ");
            advantages.forEach(r -> info.append(r.getDisplayName()).append(" "));
            info.append("\n");
        }
        
        if (disadvantages != null && !disadvantages.isEmpty()) {
            info.append("Desventajas contra: ");
            disadvantages.forEach(r -> info.append(r.getDisplayName()).append(" "));
            info.append("\n");
        }
        
        RoleStats stats = getRoleStats(role);
        info.append("Estadísticas: ").append(stats.toString()).append("\n");
        
        SpecialAbility ability = getSpecialAbility(role);
        info.append("Habilidad especial: ").append(ability.toString());
        
        return info.toString();
    }
}