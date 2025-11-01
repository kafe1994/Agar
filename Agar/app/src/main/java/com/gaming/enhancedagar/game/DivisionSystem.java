package com.gaming.enhancedagar.game;

import com.gaming.enhancedagar.engine.RoleSystem;
import com.gaming.enhancedagar.engine.VisualEffects;
import com.gaming.enhancedagar.entities.Player;
import com.gaming.enhancedagar.entities.Entity;
import android.graphics.PointF;
import android.graphics.RectF;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Sistema de división inteligente que maneja la fragmentación y fusión de entidades
 * Implementa división basada en masa, roles divididos, fusión automática/manual,
 * efectos visuales y balanceo de fragmentos.
 */
public class DivisionSystem {
    
    // Límites y configuraciones del sistema
    private static final float MIN_DIVISION_MASS = 50.0f; // Masa mínima para dividir
    private static final float DIVISION_RATIO = 0.7f; // 70% de la masa para el fragmento principal
    private static final int MAX_FRAGMENTS_PER_PLAYER = 6; // Máximo fragmentos por jugador
    private static final float FUSION_DISTANCE = 80.0f; // Distancia para fusión automática
    private static final float MASS_TRANSFER_RATE = 5.0f; // Tasa de transferencia de masa
    private static final long FUSION_COOLDOWN = 2000; // 2 segundos cooldown de fusión
    private static final float DIVISION_EFFECT_DURATION = 1500; // Duración efecto división (ms)
    
    // Componentes del sistema
    private final VisualEffects visualEffects;
    private final RoleSystem roleSystem;
    private final Physics physics;
    
    // Mapas para gestionar fragmentos
    private final Map<String, PlayerFragments> playerFragments = new ConcurrentHashMap<>();
    private final Map<String, FragmentData> fragmentMap = new ConcurrentHashMap<>();
    private final List<MassTransfer> activeTransfers = new CopyOnWriteArrayList<>();
    
    // Temporizadores y efectos
    private final Queue<Long> divisionCooldowns = new LinkedList<>();
    
    public DivisionSystem(VisualEffects visualEffects, RoleSystem roleSystem, Physics physics) {
        this.visualEffects = visualEffects;
        this.roleSystem = roleSystem;
        this.physics = physics;
    }
    
    /**
     * Clase que encapsula los fragmentos de un jugador
     */
    private static class PlayerFragments {
        final Player mainPlayer;
        final List<Entity> fragments;
        final Map<Entity, Long> lastFusionTime;
        final Map<Entity, PointF> fragmentVelocities;
        final Set<Entity> dyingFragments;
        
        PlayerFragments(Player mainPlayer) {
            this.mainPlayer = mainPlayer;
            this.fragments = new CopyOnWriteArrayList<>();
            this.lastFusionTime = new ConcurrentHashMap<>();
            this.fragmentVelocities = new ConcurrentHashMap<>();
            this.dyingFragments = new HashSet<>();
            fragments.add(mainPlayer);
        }
    }
    
    /**
     * Clase para datos específicos de fragmentos
     */
    private static class FragmentData {
        final String playerId;
        final Entity fragment;
        final float originalMass;
        final long divisionTime;
        final Set<String> originalRoles;
        
        FragmentData(String playerId, Entity fragment, float originalMass) {
            this.playerId = playerId;
            this.fragment = fragment;
            this.originalMass = originalMass;
            this.divisionTime = System.currentTimeMillis();
            this.originalRoles = new HashSet<>();
        }
    }
    
    /**
     * Clase para transferencia de masa entre fragmentos
     */
    private static class MassTransfer {
        final Entity fromFragment;
        final Entity toFragment;
        float remainingMass;
        final float rate;
        final long startTime;
        long lastUpdate;
        
        MassTransfer(Entity from, Entity to, float mass, float rate) {
            this.fromFragment = from;
            this.toFragment = to;
            this.remainingMass = mass;
            this.rate = rate;
            this.startTime = System.currentTimeMillis();
            this.lastUpdate = startTime;
        }
        
        boolean isComplete() {
            return remainingMass <= 0;
        }
        
        void update() {
            long currentTime = System.currentTimeMillis();
            float deltaTime = (currentTime - lastUpdate) / 1000.0f;
            float transferAmount = Math.min(remainingMass, rate * deltaTime);
            
            if (fromFragment.getMass() > transferAmount) {
                fromFragment.reduceMass(transferAmount);
                toFragment.addMass(transferAmount);
                remainingMass -= transferAmount;
            }
            
            lastUpdate = currentTime;
        }
    }
    
    /**
     * Método principal de división inteligente
     */
    public List<Entity> dividePlayer(Player player, PointF direction) {
        if (!canDivide(player)) {
            return Collections.emptyList();
        }
        
        // Aplicar cooldown
        divisionCooldowns.offer(System.currentTimeMillis());
        pruneCooldowns();
        
        PlayerFragments pf = getOrCreatePlayerFragments(player);
        
        // Determinar fragmento a dividir (el más grande)
        Entity fragmentToSplit = findLargestFragment(pf.fragments);
        
        if (fragmentToSplit == null || fragmentToSplit.getMass() < MIN_DIVISION_MASS) {
            return Collections.emptyList();
        }
        
        // Crear nuevos fragmentos
        List<Entity> newFragments = createFragments(player, fragmentToSplit, direction);
        
        if (newFragments.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Actualizar roles de fragmentos
        distributeRolesAmongFragments(pf, fragmentToSplit, newFragments);
        
        // Aplicar efectos visuales
        applyDivisionEffects(newFragments, fragmentToSplit);
        
        // Aplicar velocidades de separación
        applyFragmentVelocities(pf, newFragments, direction);
        
        return newFragments;
    }
    
    /**
     * Verifica si un jugador puede dividirse
     */
    private boolean canDivide(Player player) {
        PlayerFragments pf = getOrCreatePlayerFragments(player);
        
        // Verificar límite de fragmentos
        if (pf.fragments.size() >= MAX_FRAGMENTS_PER_PLAYER) {
            return false;
        }
        
        // Verificar cooldown
        if (!divisionCooldowns.isEmpty() && 
            System.currentTimeMillis() - divisionCooldowns.peek() < FUSION_COOLDOWN) {
            return false;
        }
        
        // Verificar masa suficiente en al menos un fragmento
        for (Entity fragment : pf.fragments) {
            if (fragment.getMass() >= MIN_DIVISION_MASS) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Crea fragmentos inteligentes basados en masa y roles
     */
    private List<Entity> createFragments(Player player, Entity toSplit, PointF direction) {
        List<Entity> fragments = new ArrayList<>();
        float totalMass = toSplit.getMass();
        
        // Determinar número óptimo de fragmentos basado en masa
        int optimalFragments = calculateOptimalFragments(totalMass);
        
        // Distribuir masa inteligentemente
        float[] massDistribution = calculateMassDistribution(totalMass, optimalFragments);
        
        // Crear fragmentos con distribución balanceada
        for (int i = 0; i < optimalFragments; i++) {
            float fragmentMass = massDistribution[i];
            
            if (fragmentMass < MIN_DIVISION_MASS) {
                continue;
            }
            
            // Crear nuevo fragmento
            Entity fragment = createFragment(player, toSplit, fragmentMass, i, direction);
            if (fragment != null) {
                fragments.add(fragment);
                registerFragment(player.getId(), fragment, fragmentMass);
            }
        }
        
        // Reducir masa del fragmento original
        float totalMassToRemove = 0;
        for (float mass : massDistribution) {
            totalMassToRemove += mass;
        }
        toSplit.reduceMass(totalMassToRemove);
        
        return fragments;
    }
    
    /**
     * Calcula el número óptimo de fragmentos basado en la masa total
     */
    private int calculateOptimalFragments(float totalMass) {
        if (totalMass < 100) return 1;
        if (totalMass < 200) return 2;
        if (totalMass < 400) return 3;
        if (totalMass < 800) return 4;
        return Math.min(5, MAX_FRAGMENTS_PER_PLAYER);
    }
    
    /**
     * Calcula distribución inteligente de masa entre fragmentos
     */
    private float[] calculateMassDistribution(float totalMass, int fragmentCount) {
        float[] distribution = new float[fragmentCount];
        
        if (fragmentCount == 1) {
            distribution[0] = totalMass * DIVISION_RATIO;
            return distribution;
        }
        
        // Distribución no uniforme para mantener fairness
        float remainingMass = totalMass;
        for (int i = 0; i < fragmentCount - 1; i++) {
            if (i == 0) {
                // Primer fragmento obtiene más masa (fragmento principal)
                distribution[i] = remainingMass * 0.4f;
            } else if (i == 1) {
                // Segundo fragmento obtiene una cantidad moderada
                distribution[i] = remainingMass * 0.3f;
            } else {
                // Fragmentos adicionales obtienen cantidades menores
                distribution[i] = remainingMass * 0.15f;
            }
            remainingMass -= distribution[i];
        }
        
        // Último fragmento obtiene el resto
        distribution[fragmentCount - 1] = remainingMass;
        
        return distribution;
    }
    
    /**
     * Crea un fragmento individual con sus propiedades específicas
     */
    private Entity createFragment(Player player, Entity original, float mass, int index, PointF direction) {
        // Crear nueva entidad fragmento
        Entity fragment = new Entity(original.getX(), original.getY(), mass);
        
        // Heredar propiedades del original
        fragment.setColor(original.getColor());
        fragment.setName(original.getName() + "_" + index);
        
        // Posicionar fragmento con separación inteligente
        PointF offset = calculateFragmentOffset(direction, index, fragment.getRadius());
        fragment.setPosition(original.getX() + offset.x, original.getY() + offset.y);
        
        return fragment;
    }
    
    /**
     * Calcula offset de posicionamiento para fragmentos
     */
    private PointF calculateFragmentOffset(PointF direction, int fragmentIndex, float radius) {
        float angle = fragmentIndex * (2 * (float) Math.PI / 3); // Distribución triangular
        
        // Aplicar dirección del jugador
        float baseX = (float) Math.cos(angle) * radius * 1.5f;
        float baseY = (float) Math.sin(angle) * radius * 1.5f;
        
        // Ajustar por dirección de división
        if (direction != null) {
            float dirAngle = (float) Math.atan2(direction.y, direction.x);
            float cos = (float) Math.cos(dirAngle);
            float sin = (float) Math.sin(dirAngle);
            
            float adjX = baseX * cos - baseY * sin;
            float adjY = baseX * sin + baseY * cos;
            
            return new PointF(adjX, adjY);
        }
        
        return new PointF(baseX, baseY);
    }
    
    /**
     * Registra un fragmento en el sistema
     */
    private void registerFragment(String playerId, Entity fragment, float originalMass) {
        fragmentMap.put(fragment.getId(), new FragmentData(playerId, fragment, originalMass));
        
        PlayerFragments pf = playerFragments.get(playerId);
        if (pf != null) {
            pf.fragments.add(fragment);
            pf.lastFusionTime.put(fragment, 0L);
        }
    }
    
    /**
     * Distribuye roles entre fragmentos para especialización
     */
    private void distributeRolesAmongFragments(PlayerFragments pf, Entity original, List<Entity> newFragments) {
        Set<String> availableRoles = roleSystem.getAvailableRoles();
        List<String> roleList = new ArrayList<>(availableRoles);
        
        // El fragmento principal hereda el rol del original
        Entity mainFragment = newFragments.get(0);
        if (newFragments.size() > 1) {
            // Asignar roles especializados a fragmentos adicionales
            for (int i = 1; i < newFragments.size() && i < roleList.size(); i++) {
                Entity fragment = newFragments.get(i);
                String role = roleList.get(i % roleList.size());
                
                roleSystem.assignRole(fragment, role);
                
                // Aplicar modificadores de rol
                applyRoleModifiers(fragment, role);
            }
        }
        
        // El fragmento principal mantiene el rol original
        String originalRole = roleSystem.getRole(original);
        if (originalRole != null) {
            roleSystem.assignRole(mainFragment, originalRole);
        }
    }
    
    /**
     * Aplica modificadores específicos del rol al fragmento
     */
    private void applyRoleModifiers(Entity fragment, String role) {
        switch (role.toLowerCase()) {
            case "speed":
                // Fragmentos veloces son más pequeños pero más ágiles
                float speedMass = Math.max(fragment.getMass() * 0.8f, MIN_DIVISION_MASS);
                fragment.setMass(speedMass);
                break;
                
            case "defense":
                // Fragmentos defensivos mantienen más masa
                float defenseMass = Math.max(fragment.getMass() * 1.2f, MIN_DIVISION_MASS);
                fragment.setMass(defenseMass);
                break;
                
            case "hunter":
                // Fragmentos cazadores tienen forma optimizada para cazar
                // Mantienen masa moderada con ventajas de velocidad
                break;
                
            case "collector":
                // Fragmentos colectores priorizan eficiencia de masa
                // Obtienen bonus de recolección de comida
                break;
        }
    }
    
    /**
     * Aplica efectos visuales de división
     */
    private void applyDivisionEffects(List<Entity> newFragments, Entity original) {
        // Efecto de explosión en el punto de división
        visualEffects.createExplosionEffect(original.getX(), original.getY(), 
                                          original.getRadius() * 1.5f);
        
        // Efectos individuales para cada fragmento
        for (Entity fragment : newFragments) {
            visualEffects.createFragmentationEffect(fragment.getX(), fragment.getY(),
                                                  fragment.getRadius());
        }
        
        // Línea de conexión temporal entre fragmentos
        for (int i = 0; i < newFragments.size() - 1; i++) {
            visualEffects.createConnectionEffect(newFragments.get(i), newFragments.get(i + 1));
        }
    }
    
    /**
     * Aplica velocidades de separación a los fragmentos
     */
    private void applyFragmentVelocities(PlayerFragments pf, List<Entity> fragments, PointF direction) {
        float baseVelocity = 200.0f; // Velocidad base de separación
        
        for (int i = 0; i < fragments.size(); i++) {
            Entity fragment = fragments.get(i);
            
            // Calcular velocidad de separación
            PointF velocity = calculateSeparationVelocity(direction, i, baseVelocity);
            fragment.setVelocity(velocity.x, velocity.y);
            
            // Almacenar velocidad para decay gradual
            pf.fragmentVelocities.put(fragment, new PointF(velocity.x, velocity.y));
        }
        
        // Aplicar velocidad de recoil al fragmento original si existe
        if (pf.fragments.contains(pf.mainPlayer) && !pf.fragments.isEmpty()) {
            PointF recoilVelocity = calculateRecoilVelocity(direction, baseVelocity * 0.5f);
            pf.mainPlayer.setVelocity(recoilVelocity.x, recoilVelocity.y);
        }
    }
    
    /**
     * Calcula velocidad de separación inteligente
     */
    private PointF calculateSeparationVelocity(PointF direction, int fragmentIndex, float baseVelocity) {
        if (direction == null) {
            // Velocidad radial si no hay dirección específica
            float angle = fragmentIndex * (2 * (float) Math.PI / 3);
            return new PointF((float) Math.cos(angle) * baseVelocity,
                            (float) Math.sin(angle) * baseVelocity);
        }
        
        // Velocidad en dirección de división con dispersión
        float dispersion = 0.3f; // 30% de dispersión
        float angle = (float) Math.atan2(direction.y, direction.x);
        angle += (fragmentIndex - 1) * dispersion; // Dispersión angular
        
        return new PointF((float) Math.cos(angle) * baseVelocity,
                        (float) Math.sin(angle) * baseVelocity);
    }
    
    /**
     * Calcula velocidad de recoil para el fragmento principal
     */
    private PointF calculateRecoilVelocity(PointF direction, float magnitude) {
        if (direction == null) {
            return new PointF(0, 0);
        }
        
        // Velocidad opuesta a la dirección de división
        return new PointF(-direction.x * magnitude, -direction.y * magnitude);
    }
    
    /**
     * Encuentra el fragmento más grande de un jugador
     */
    private Entity findLargestFragment(List<Entity> fragments) {
        Entity largest = null;
        float maxMass = 0;
        
        for (Entity fragment : fragments) {
            if (fragment.getMass() > maxMass) {
                maxMass = fragment.getMass();
                largest = fragment;
            }
        }
        
        return largest;
    }
    
    /**
     * Fusiona fragmentos manualmente
     */
    public boolean mergeFragments(String playerId, List<Entity> fragmentsToMerge) {
        PlayerFragments pf = playerFragments.get(playerId);
        if (pf == null || fragmentsToMerge.size() < 2) {
            return false;
        }
        
        // Verificar cooldown de fusión
        for (Entity fragment : fragmentsToMerge) {
            Long lastFusion = pf.lastFusionTime.get(fragment);
            if (lastFusion != null && 
                System.currentTimeMillis() - lastFusion < FUSION_COOLDOWN) {
                return false;
            }
        }
        
        // Realizar fusión
        return performFusion(pf, fragmentsToMerge);
    }
    
    /**
     * Fusiona fragmentos automáticamente cercanos
     */
    public void performAutomaticFusions() {
        for (PlayerFragments pf : playerFragments.values()) {
            if (pf.fragments.size() <= 1) continue;
            
            List<Entity> closeFragments = findCloseFragments(pf.fragments);
            if (closeFragments.size() >= 2) {
                performFusion(pf, closeFragments);
            }
        }
    }
    
    /**
     * Encuentra fragmentos cercanos para fusión automática
     */
    private List<Entity> findCloseFragments(List<Entity> fragments) {
        List<Entity> closeFragments = new ArrayList<>();
        
        for (int i = 0; i < fragments.size(); i++) {
            for (int j = i + 1; j < fragments.size(); j++) {
                Entity frag1 = fragments.get(i);
                Entity frag2 = fragments.get(j);
                
                float distance = physics.calculateDistance(frag1, frag2);
                
                if (distance <= FUSION_DISTANCE) {
                    closeFragments.add(frag1);
                    closeFragments.add(frag2);
                    break; // Solo agregar un par por fragmento
                }
            }
        }
        
        return closeFragments;
    }
    
    /**
     * Realiza la fusión de fragmentos
     */
    private boolean performFusion(PlayerFragments pf, List<Entity> fragmentsToMerge) {
        if (fragmentsToMerge.size() < 2) return false;
        
        // Seleccionar fragmento de destino (el más grande)
        Entity targetFragment = findLargestFragment(fragmentsToMerge);
        
        // Calcular masa total a fusionar
        float totalMass = 0;
        List<Entity> sourceFragments = new ArrayList<>();
        
        for (Entity fragment : fragmentsToMerge) {
            if (fragment != targetFragment) {
                totalMass += fragment.getMass();
                sourceFragments.add(fragment);
            }
        }
        
        // Transferir masa con animación
        if (totalMass > 0) {
            startMassTransfer(targetFragment, sourceFragments, totalMass);
        }
        
        // Aplicar efectos visuales de fusión
        applyFusionEffects(fragmentsToMerge);
        
        // Actualizar roles
        mergeFragmentRoles(pf, targetFragment, sourceFragments);
        
        // Remover fragmentos fusionados
        for (Entity sourceFragment : sourceFragments) {
            pf.fragments.remove(sourceFragment);
            fragmentMap.remove(sourceFragment.getId());
            pf.lastFusionTime.remove(sourceFragment);
            pf.fragmentVelocities.remove(sourceFragment);
        }
        
        // Registrar tiempo de fusión
        pf.lastFusionTime.put(targetFragment, System.currentTimeMillis());
        
        return true;
    }
    
    /**
     * Inicia transferencia de masa con animación
     */
    private void startMassTransfer(Entity target, List<Entity> sources, float totalMass) {
        for (Entity source : sources) {
            if (source.getMass() <= 0) continue;
            
            float transferMass = source.getMass();
            MassTransfer transfer = new MassTransfer(source, target, transferMass, MASS_TRANSFER_RATE);
            activeTransfers.add(transfer);
        }
    }
    
    /**
     * Aplica efectos visuales de fusión
     */
    private void applyFusionEffects(List<Entity> fragments) {
        // Efecto de implosión en el punto de fusión
        Entity center = findLargestFragment(fragments);
        if (center != null) {
            visualEffects.createFusionEffect(center.getX(), center.getY(), center.getRadius() * 2.0f);
        }
        
        // Efectos de conexión entre fragmentos
        for (int i = 0; i < fragments.size() - 1; i++) {
            for (int j = i + 1; j < fragments.size(); j++) {
                visualEffects.createConnectionEffect(fragments.get(i), fragments.get(j));
            }
        }
    }
    
    /**
     * Fusiona roles de fragmentos al fusionarse
     */
    private void mergeFragmentRoles(PlayerFragments pf, Entity target, List<Entity> sources) {
        // Recopilar todos los roles únicos
        Set<String> allRoles = new HashSet<>();
        
        String targetRole = roleSystem.getRole(target);
        if (targetRole != null) {
            allRoles.add(targetRole);
        }
        
        for (Entity source : sources) {
            String sourceRole = roleSystem.getRole(source);
            if (sourceRole != null) {
                allRoles.add(sourceRole);
            }
        }
        
        // Asignar rol combinado al fragmento objetivo
        if (allRoles.size() == 1) {
            // Solo un rol, mantenerlo
            roleSystem.assignRole(target, allRoles.iterator().next());
        } else if (allRoles.size() > 1) {
            // Múltiples roles, crear rol híbrido
            String hybridRole = createHybridRole(allRoles);
            roleSystem.assignRole(target, hybridRole);
        }
    }
    
    /**
     * Crea un rol híbrido basado en múltiples roles
     */
    private String createHybridRole(Set<String> roles) {
        List<String> roleList = new ArrayList<>(roles);
        if (roleList.isEmpty()) return "neutral";
        
        // Combinar características de roles
        StringBuilder hybrid = new StringBuilder();
        for (String role : roleList) {
            if (hybrid.length() > 0) hybrid.append("_");
            hybrid.append(role);
        }
        
        return hybrid.toString().toLowerCase();
    }
    
    /**
     * Actualiza el sistema de división (llamado cada frame)
     */
    public void update(float deltaTime) {
        // Actualizar transferencias de masa activas
        updateMassTransfers();
        
        // Decay de velocidades de fragmentos
        updateFragmentVelocities(deltaTime);
        
        // Realizar fusiones automáticas
        performAutomaticFusions();
        
        // Limpiar fragmentos marcados para muerte
        cleanupDyingFragments();
    }
    
    /**
     * Actualiza transferencias de masa activas
     */
    private void updateMassTransfers() {
        activeTransfers.removeIf(transfer -> {
            transfer.update();
            return transfer.isComplete();
        });
    }
    
    /**
     * Actualiza velocidades de fragmentos con decay gradual
     */
    private void updateFragmentVelocities(float deltaTime) {
        float decayRate = 0.8f; // Factor de decay por segundo
        
        for (PlayerFragments pf : playerFragments.values()) {
            for (Map.Entry<Entity, PointF> entry : pf.fragmentVelocities.entrySet()) {
                Entity fragment = entry.getKey();
                PointF velocity = entry.getValue();
                
                // Aplicar decay
                velocity.x *= Math.pow(decayRate, deltaTime);
                velocity.y *= Math.pow(decayRate, deltaTime);
                
                // Aplicar velocidad al fragmento
                if (Math.abs(velocity.x) > 0.1f || Math.abs(velocity.y) > 0.1f) {
                    fragment.setVelocity(velocity.x, velocity.y);
                } else {
                    // Velocidad muy baja, detener
                    fragment.setVelocity(0, 0);
                    pf.fragmentVelocities.remove(fragment);
                }
            }
        }
    }
    
    /**
     * Limpia fragmentos marcados para muerte
     */
    private void cleanupDyingFragments() {
        for (PlayerFragments pf : playerFragments.values()) {
            pf.fragments.removeAll(pf.dyingFragments);
            for (Entity dyingFragment : pf.dyingFragments) {
                fragmentMap.remove(dyingFragment.getId());
                pf.lastFusionTime.remove(dyingFragment);
                pf.fragmentVelocities.remove(dyingFragment);
            }
            pf.dyingFragments.clear();
        }
    }
    
    /**
     * Obtiene o crea datos de fragmentos para un jugador
     */
    private PlayerFragments getOrCreatePlayerFragments(Player player) {
        return playerFragments.computeIfAbsent(player.getId(), 
                                             k -> new PlayerFragments(player));
    }
    
    /**
     * Limpia cooldowns expirados
     */
    private void pruneCooldowns() {
        long currentTime = System.currentTimeMillis();
        while (!divisionCooldowns.isEmpty() && 
               currentTime - divisionCooldowns.peek() > FUSION_COOLDOWN) {
            divisionCooldowns.poll();
        }
    }
    
    /**
     * Obtiene todos los fragmentos de un jugador
     */
    public List<Entity> getPlayerFragments(String playerId) {
        PlayerFragments pf = playerFragments.get(playerId);
        return pf != null ? new ArrayList<>(pf.fragments) : Collections.emptyList();
    }
    
    /**
     * Verifica si un fragmento pertenece a un jugador específico
     */
    public boolean isFragmentOfPlayer(Entity fragment, String playerId) {
        FragmentData data = fragmentMap.get(fragment.getId());
        return data != null && data.playerId.equals(playerId);
    }
    
    /**
     * Obtiene el fragmento principal de un jugador
     */
    public Entity getMainFragment(String playerId) {
        PlayerFragments pf = playerFragments.get(playerId);
        return pf != null ? pf.mainPlayer : null;
    }
    
    /**
     * Marca un fragmento para eliminación
     */
    public void markFragmentForDeath(Entity fragment) {
        String playerId = getPlayerIdForFragment(fragment);
        if (playerId != null) {
            PlayerFragments pf = playerFragments.get(playerId);
            if (pf != null) {
                pf.dyingFragments.add(fragment);
            }
        }
    }
    
    /**
     * Obtiene el ID del jugador que posee un fragmento
     */
    private String getPlayerIdForFragment(Entity fragment) {
        FragmentData data = fragmentMap.get(fragment.getId());
        return data != null ? data.playerId : null;
    }
    
    /**
     * Obtiene estadísticas del sistema de división
     */
    public DivisionStats getDivisionStats(String playerId) {
        PlayerFragments pf = playerFragments.get(playerId);
        if (pf == null) {
            return new DivisionStats(0, 0, 0, 0);
        }
        
        int fragmentCount = pf.fragments.size();
        int maxFragments = MAX_FRAGMENTS_PER_PLAYER;
        float totalMass = 0;
        int activeTransfers = 0;
        
        for (Entity fragment : pf.fragments) {
            totalMass += fragment.getMass();
        }
        
        for (MassTransfer transfer : activeTransfers) {
            if (transfer.fromFragment.getId().equals(pf.mainPlayer.getId()) ||
                transfer.toFragment.getId().equals(pf.mainPlayer.getId())) {
                activeTransfers++;
            }
        }
        
        return new DivisionStats(fragmentCount, maxFragments, totalMass, activeTransfers);
    }
    
    /**
     * Clase para estadísticas del sistema de división
     */
    public static class DivisionStats {
        public final int fragmentCount;
        public final int maxFragments;
        public final float totalMass;
        public final int activeTransfers;
        
        public DivisionStats(int fragmentCount, int maxFragments, float totalMass, int activeTransfers) {
            this.fragmentCount = fragmentCount;
            this.maxFragments = maxFragments;
            this.totalMass = totalMass;
            this.activeTransfers = activeTransfers;
        }
        
        public float getFragmentationRatio() {
            return maxFragments > 0 ? (float) fragmentCount / maxFragments : 0;
        }
        
        public boolean isFullyFragmented() {
            return fragmentCount >= maxFragments;
        }
        
        public boolean hasActiveTransfers() {
            return activeTransfers > 0;
        }
    }
    
    /**
     * Limpia recursos del sistema
     */
    public void cleanup() {
        playerFragments.clear();
        fragmentMap.clear();
        activeTransfers.clear();
        divisionCooldowns.clear();
    }
    
    /**
     * Resetea el sistema completo
     */
    public void reset() {
        cleanup();
    }
}