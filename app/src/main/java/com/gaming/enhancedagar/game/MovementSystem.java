package com.gaming.enhancedagar.game;

import com.gaming.enhancedagar.entities.Entity;
import com.gaming.enhancedagar.entities.Player;
import com.gaming.enhancedagar.utils.Vector2D;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sistema avanzado de movimiento para entidades con pathfinding, steering behaviors
 * y comportamientos por rol. Optimizado para performance.
 */
public class MovementSystem {
    
    // Configuración del sistema
    private static final float DEFAULT_MAX_SPEED = 100.0f;
    private static final float DEFAULT_MAX_FORCE = 50.0f;
    private static final float STEERING_MULTIPLIER = 2.0f;
    private static final int PATHFINDING_MAX_NODES = 1000;
    private static final float OBSTACLE_AVOIDANCE_DISTANCE = 50.0f;
    private static final float NEIGHBOR_RADIUS = 80.0f;
    
    // Navegación por cuadrícula para optimización
    private NavigationGrid navigationGrid;
    
    // Cache para paths (evita recalcular)
    private final Map<String, PathCacheEntry> pathCache;
    
    // Registro de entidades activas
    private final Set<Integer> activeEntities;
    
    // Pool de objetos para evitar GC
    private final ObjectPool objectPool;
    
    /**
     * Tipos de roles de entidades con comportamientos específicos
     */
    public enum EntityRole {
        AGGRESSIVE(1.2f, 0.8f, 0.9f, 1.5f),    // Mayor velocidad, menor evasión
        DEFENSIVE(0.8f, 1.3f, 1.2f, 0.9f),      // Menor velocidad, mayor evasión
        EXPLORER(1.0f, 1.1f, 1.0f, 1.2f),       // Balanceado con mayor cobertura
        PREY(1.3f, 1.5f, 0.7f, 1.8f),           // Alta velocidad y evasión
        HUNTER(1.1f, 0.6f, 1.3f, 1.1f);         // Velocidad moderada, poca evasión
        
        final float speedMultiplier;
        final float avoidanceMultiplier;
        final float pathfindingMultiplier;
        final float aggressionMultiplier;
        
        EntityRole(float speedMult, float avoidanceMult, float pathMult, float aggressionMult) {
            this.speedMultiplier = speedMult;
            this.avoidanceMultiplier = avoidanceMult;
            this.pathfindingMultiplier = pathMult;
            this.aggressionMultiplier = aggressionMult;
        }
    }
    
    /**
     * Cache para paths optimizados
     */
    private static class PathCacheEntry {
        final List<Vector2D> path;
        final long timestamp;
        final float startX, startY, endX, endY;
        
        PathCacheEntry(List<Vector2D> path, float startX, float startY, float endX, float endY) {
            this.path = new ArrayList<>(path);
            this.timestamp = System.currentTimeMillis();
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
        }
        
        boolean isValid(float sx, float sy, float ex, float ey) {
            return Math.abs(startX - sx) < 5 && Math.abs(startY - sy) < 5 &&
                   Math.abs(endX - ex) < 5 && Math.abs(endY - ey) < 5 &&
                   System.currentTimeMillis() - timestamp < 30000; // 30 segundos
        }
    }
    
    /**
     * Pool de objetos para optimizar performance
     */
    private static class ObjectPool {
        private final Queue<Vector2D> vectorPool = new ArrayDeque<>();
        private final Queue<SteeringBehavior> behaviorPool = new ArrayDeque<>();
        
        Vector2D getVector() {
            Vector2D v = vectorPool.poll();
            return v != null ? v : new Vector2D();
        }
        
        void returnVector(Vector2D v) {
            if (vectorPool.size() < 100) { // Límite del pool
                v.set(0, 0);
                vectorPool.offer(v);
            }
        }
        
        SteeringBehavior getBehavior() {
            SteeringBehavior b = behaviorPool.poll();
            return b != null ? b : new SteeringBehavior();
        }
        
        void returnBehavior(SteeringBehavior b) {
            if (behaviorPool.size() < 50) {
                behaviorPool.offer(b);
            }
        }
    }
    
    /**
     * Grid de navegación para pathfinding eficiente
     */
    private static class NavigationGrid {
        private final float cellSize;
        private final int width, height;
        private final boolean[][] obstacles;
        private final float[][] costField;
        
        NavigationGrid(float worldWidth, float worldHeight, float cellSize) {
            this.cellSize = cellSize;
            this.width = (int) (worldWidth / cellSize) + 1;
            this.height = (int) (worldHeight / cellSize) + 1;
            this.obstacles = new boolean[width][height];
            this.costField = new float[width][height];
            
            // Inicializar cost field
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    costField[x][y] = 1.0f; // Costo base
                }
            }
        }
        
        int getCellX(float x) { return Math.max(0, Math.min(width - 1, (int) (x / cellSize))); }
        int getCellY(float y) { return Math.max(0, Math.min(height - 1, (int) (y / cellSize))); }
        
        boolean isObstacle(int cellX, int cellY) {
            if (cellX < 0 || cellX >= width || cellY < 0 || cellY >= height) return true;
            return obstacles[cellX][cellY];
        }
        
        void setObstacle(int cellX, int cellY, boolean obstacle) {
            if (cellX >= 0 && cellX < width && cellY >= 0 && cellY < height) {
                obstacles[cellX][cellY] = obstacle;
                costField[cellX][cellY] = obstacle ? Float.MAX_VALUE : 1.0f;
            }
        }
        
        float getCost(int cellX, int cellY) {
            if (cellX < 0 || cellX >= width || cellY < 0 || cellY >= height) return Float.MAX_VALUE;
            return costField[cellX][cellY];
        }
    }
    
    /**
     * Comportamientos de steering básicos
     */
    private static class SteeringBehavior {
        Vector2D seek(Vector2D current, Vector2D target, float maxSpeed, float maxForce) {
            Vector2D desired = Vector2D.subtract(target, current);
            float distance = desired.magnitude();
            
            if (distance < 0.001f) return new Vector2D(0, 0);
            
            desired.normalize();
            desired.multiply(maxSpeed);
            
            Vector2D steer = Vector2D.subtract(desired, current);
            if (steer.magnitude() > maxForce) {
                steer.normalize();
                steer.multiply(maxForce);
            }
            return steer;
        }
        
        Vector2D flee(Vector2D current, Vector2D threat, float maxSpeed, float maxForce) {
            Vector2D desired = Vector2D.subtract(current, threat);
            float distance = desired.magnitude();
            
            if (distance < 0.001f) return new Vector2D(0, 0);
            
            desired.normalize();
            desired.multiply(maxSpeed);
            
            Vector2D steer = Vector2D.subtract(desired, current);
            if (steer.magnitude() > maxForce) {
                steer.normalize();
                steer.multiply(maxForce);
            }
            return steer;
        }
        
        Vector2D arrive(Vector2D current, Vector2D target, float maxSpeed, float maxForce, float slowRadius) {
            Vector2D desired = Vector2D.subtract(target, current);
            float distance = desired.magnitude();
            
            if (distance < 0.001f) return new Vector2D(0, 0);
            
            desired.normalize();
            
            float speed = maxSpeed;
            if (distance < slowRadius) {
                speed = maxSpeed * (distance / slowRadius);
            }
            
            desired.multiply(speed);
            
            Vector2D steer = Vector2D.subtract(desired, current);
            if (steer.magnitude() > maxForce) {
                steer.normalize();
                steer.multiply(maxForce);
            }
            return steer;
        }
    }
    
    public MovementSystem(float worldWidth, float worldHeight) {
        this.navigationGrid = new NavigationGrid(worldWidth, worldHeight, 10.0f);
        this.pathCache = new ConcurrentHashMap<>();
        this.activeEntities = ConcurrentHashMap.newKeySet();
        this.objectPool = new ObjectPool();
    }
    
    /**
     * Actualiza el movimiento de una entidad
     */
    public void updateEntityMovement(Entity entity, float deltaTime) {
        if (!activeEntities.contains(entity.getId())) {
            activeEntities.add(entity.getId());
        }
        
        // Obtener aceleraciones de diferentes comportamientos
        Vector2D totalAcceleration = objectPool.getVector();
        totalAcceleration.set(0, 0);
        
        // Steering behaviors básicos
        SteeringBehavior behavior = objectPool.getBehavior();
        
        // Movimiento hacia objetivo si existe
        if (entity.hasTarget()) {
            Vector2D target = entity.getTargetPosition();
            Vector2D acceleration = behavior.arrive(
                entity.getPosition(), 
                target, 
                getMaxSpeed(entity),
                getMaxForce(entity),
                getSlowRadius(entity)
            );
            totalAcceleration.add(acceleration);
        }
        
        // Evitación de obstáculos
        Vector2D avoidanceForce = calculateObstacleAvoidance(entity);
        totalAcceleration.add(avoidanceForce);
        
        // Separación de otras entidades
        Vector2D separationForce = calculateSeparation(entity);
        totalAcceleration.add(separationForce);
        
        // Aplicar comportamientos por rol
        applyRoleBasedBehavior(entity, totalAcceleration);
        
        // Aplicar aceleración suavizada
        applySmoothedAcceleration(entity, totalAcceleration, deltaTime);
        
        // Limpiar
        objectPool.returnVector(totalAcceleration);
        objectPool.returnBehavior(behavior);
    }
    
    /**
     * Calcula la evitación de obstáculos usando el navigation grid
     */
    private Vector2D calculateObstacleAvoidance(Entity entity) {
        Vector2D avoidance = objectPool.getVector();
        avoidance.set(0, 0);
        
        Vector2D position = entity.getPosition();
        Vector2D velocity = entity.getVelocity();
        float speed = velocity.magnitude();
        
        if (speed < 0.001f) {
            objectPool.returnVector(avoidance);
            return avoidance;
        }
        
        Vector2D forward = velocity.clone();
        forward.normalize();
        
        // Detectar obstáculos en la dirección del movimiento
        Vector2D ahead = Vector2D.add(position, Vector2D.multiply(forward, OBSTACLE_AVOIDANCE_DISTANCE));
        Vector2D ahead2 = Vector2D.add(position, Vector2D.multiply(forward, OBSTACLE_AVOIDANCE_DISTANCE * 0.5f));
        
        int cellX = navigationGrid.getCellX(ahead.x);
        int cellY = navigationGrid.getCellY(ahead.y);
        int cellX2 = navigationGrid.getCellX(ahead2.x);
        int cellY2 = navigationGrid.getCellY(ahead2.y);
        
        if (navigationGrid.isObstacle(cellX, cellY)) {
            Vector2D avoidDir = Vector2D.subtract(ahead, getNearestClearPosition(ahead));
            avoidDir.normalize();
            avoidDir.multiply(getMaxForce(entity) * STEERING_MULTIPLIER);
            avoidance.add(avoidDir);
        } else if (navigationGrid.isObstacle(cellX2, cellY2)) {
            Vector2D avoidDir = Vector2D.subtract(ahead2, getNearestClearPosition(ahead2));
            avoidDir.normalize();
            avoidDir.multiply(getMaxForce(entity) * STEERING_MULTIPLIER * 0.5f);
            avoidance.add(avoidDir);
        }
        
        objectPool.returnVector(avoidance);
        return avoidance;
    }
    
    /**
     * Encuentra la posición clara más cercana
     */
    private Vector2D getNearestClearPosition(Vector2D position) {
        for (int radius = 1; radius <= 5; radius++) {
            for (int angle = 0; angle < 360; angle += 45) {
                float rad = (float) Math.toRadians(angle);
                float x = position.x + (float) Math.cos(rad) * radius * navigationGrid.cellSize;
                float y = position.y + (float) Math.sin(rad) * radius * navigationGrid.cellSize;
                
                int cellX = navigationGrid.getCellX(x);
                int cellY = navigationGrid.getCellY(y);
                
                if (!navigationGrid.isObstacle(cellX, cellY)) {
                    return new Vector2D(x, y);
                }
            }
        }
        return position.clone(); // Fallback
    }
    
    /**
     * Calcula fuerza de separación para evitar colisiones
     */
    private Vector2D calculateSeparation(Entity entity) {
        Vector2D separation = objectPool.getVector();
        separation.set(0, 0);
        
        Vector2D position = entity.getPosition();
        float neighborRadiusSq = NEIGHBOR_RADIUS * NEIGHBOR_RADIUS;
        
        // Aquí se integrarían las otras entidades del juego
        // Por ahora, implementación básica
        
        objectPool.returnVector(separation);
        return separation;
    }
    
    /**
     * Aplica comportamientos específicos por rol
     */
    private void applyRoleBasedBehavior(Entity entity, Vector2D acceleration) {
        EntityRole role = getEntityRole(entity);
        float multiplier = role.speedMultiplier;
        
        // Comportamientos específicos por rol
        switch (role) {
            case AGGRESSIVE:
                // Comportamiento agresivo: moverse directamente hacia objetivos
                if (entity.hasTarget()) {
                    Vector2D target = entity.getTargetPosition();
                    Vector2D toTarget = Vector2D.subtract(target, entity.getPosition());
                    toTarget.normalize();
                    toTarget.multiply(getMaxForce(entity) * 0.5f * multiplier);
                    acceleration.add(toTarget);
                }
                break;
                
            case DEFENSIVE:
                // Comportamiento defensivo: mantener distancia y evadir
                Vector2D dangerZone = objectPool.getVector();
                dangerZone.set(0, 0);
                // Calcular zonas de peligro basado en amenazas cercanas
                if (dangerZone.magnitude() > 0) {
                    dangerZone.normalize();
                    dangerZone.multiply(getMaxForce(entity) * 0.3f);
                    acceleration.add(dangerZone);
                }
                objectPool.returnVector(dangerZone);
                break;
                
            case EXPLORER:
                // Comportamiento exploratorio: cobertura de área
                Vector2D explorationForce = calculateExplorationForce(entity);
                acceleration.add(explorationForce.multiply(role.pathfindingMultiplier));
                break;
                
            case PREY:
                // Comportamiento de presa: máxima evasión
                Vector2D fleeForce = calculateFleeForce(entity);
                acceleration.add(fleeForce.multiply(role.avoidanceMultiplier));
                break;
                
            case HUNTER:
                // Comportamiento de cazador: seguimiento inteligente
                if (entity.hasTarget()) {
                    Vector2D huntForce = calculateHuntForce(entity);
                    acceleration.add(huntForce.multiply(role.aggressionMultiplier));
                }
                break;
        }
    }
    
    /**
     * Calcula fuerza de exploración
     */
    private Vector2D calculateExplorationForce(Entity entity) {
        Vector2D exploration = objectPool.getVector();
        exploration.set(0, 0);
        
        // Generar punto de exploración aleatorio cerca de la posición actual
        Random random = new Random();
        float angle = random.nextFloat() * 2 * (float) Math.PI;
        float distance = 50 + random.nextFloat() * 100;
        
        exploration.x = entity.getPosition().x + (float) Math.cos(angle) * distance;
        exploration.y = entity.getPosition().y + (float) Math.sin(angle) * distance;
        
        // Verificar que sea una posición válida
        int cellX = navigationGrid.getCellX(exploration.x);
        int cellY = navigationGrid.getCellY(exploration.y);
        
        if (navigationGrid.isObstacle(cellX, cellY)) {
            exploration.set(entity.getPosition().x, entity.getPosition().y);
        }
        
        return exploration;
    }
    
    /**
     * Calcula fuerza de huida para presas
     */
    private Vector2D calculateFleeForce(Entity entity) {
        Vector2D flee = objectPool.getVector();
        flee.set(0, 0);
        
        // Detectar amenazas cercanas y calcular dirección opuesta
        // Implementación básica - en un juego real se consultarían las entidades cercanas
        
        Vector2D position = entity.getPosition();
        float fleeRadius = 150.0f;
        
        // Simulación de amenazas (en implementación real serían entidades reales)
        for (int i = 0; i < 3; i++) {
            float threatX = position.x + (float) Math.cos(i * 2 * Math.PI / 3) * fleeRadius;
            float threatY = position.y + (float) Math.sin(i * 2 * Math.PI / 3) * fleeRadius;
            
            Vector2D threat = new Vector2D(threatX, threatY);
            Vector2D toThreat = Vector2D.subtract(threat, position);
            float distance = toThreat.magnitude();
            
            if (distance < fleeRadius && distance > 0.01f) {
                Vector2D fleeDirection = Vector2D.subtract(position, threat);
                fleeDirection.normalize();
                fleeDirection.multiply((fleeRadius - distance) / fleeRadius * getMaxForce(entity));
                flee.add(fleeDirection);
            }
        }
        
        return flee;
    }
    
    /**
     * Calcula fuerza de caza para hunters
     */
    private Vector2D calculateHuntForce(Entity entity) {
        Vector2D hunt = objectPool.getVector();
        hunt.set(0, 0);
        
        if (!entity.hasTarget()) {
            objectPool.returnVector(hunt);
            return hunt;
        }
        
        Vector2D target = entity.getTargetPosition();
        Vector2D position = entity.getPosition();
        Vector2D toTarget = Vector2D.subtract(target, position);
        
        // Cálculo de interceptación simple
        float distance = toTarget.magnitude();
        Vector2D targetVelocity = entity.getTargetVelocity();
        
        if (targetVelocity.magnitude() > 0.01f) {
            // Predecir posición futura del objetivo
            float timeToIntercept = distance / (getMaxSpeed(entity) + targetVelocity.magnitude());
            Vector2D predictedTarget = Vector2D.add(target, Vector2D.multiply(targetVelocity, timeToIntercept));
            
            hunt = Vector2D.subtract(predictedTarget, position);
        } else {
            hunt = toTarget;
        }
        
        hunt.normalize();
        hunt.multiply(getMaxForce(entity) * 0.7f);
        
        return hunt;
    }
    
    /**
     * Aplica aceleración suavizada al movimiento
     */
    private void applySmoothedAcceleration(Entity entity, Vector2D totalAcceleration, float deltaTime) {
        Vector2D currentVelocity = entity.getVelocity();
        
        // Limitar aceleración máxima
        if (totalAcceleration.magnitude() > getMaxForce(entity)) {
            totalAcceleration.normalize();
            totalAcceleration.multiply(getMaxForce(entity));
        }
        
        // Aplicar suavizado (lerp)
        float smoothingFactor = Math.min(deltaTime * 5.0f, 1.0f);
        currentVelocity.add(Vector2D.multiply(totalAcceleration, smoothingFactor * deltaTime));
        
        // Limitar velocidad máxima
        float maxSpeed = getMaxSpeed(entity);
        if (currentVelocity.magnitude() > maxSpeed) {
            currentVelocity.normalize();
            currentVelocity.multiply(maxSpeed);
        }
        
        // Actualizar posición
        Vector2D newPosition = Vector2D.add(entity.getPosition(), 
                                           Vector2D.multiply(currentVelocity, deltaTime));
        
        entity.setPosition(newPosition);
        entity.setVelocity(currentVelocity);
    }
    
    /**
     * Pathfinding usando A* con navegación por grid
     */
    public List<Vector2D> findPath(Vector2D start, Vector2D end) {
        String cacheKey = generateCacheKey(start, end);
        
        // Verificar cache
        PathCacheEntry cached = pathCache.get(cacheKey);
        if (cached != null && cached.isValid(start.x, start.y, end.x, end.y)) {
            return cached.path;
        }
        
        // Algoritmo A*
        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Set<String> closedSet = new HashSet<>();
        Map<String, Node> allNodes = new HashMap<>();
        
        Node startNode = new Node(start, null, 0, heuristic(start, end));
        openSet.offer(startNode);
        allNodes.put(generateNodeKey(start), startNode);
        
        Node endNode = null;
        int nodesProcessed = 0;
        
        while (!openSet.isEmpty() && nodesProcessed < PATHFINDING_MAX_NODES) {
            Node current = openSet.poll();
            nodesProcessed++;
            
            if (current.position.distanceTo(end) < navigationGrid.cellSize) {
                endNode = current;
                break;
            }
            
            closedSet.add(generateNodeKey(current.position));
            
            // Explorar vecinos
            for (Node neighbor : getNeighbors(current.position)) {
                String neighborKey = generateNodeKey(neighbor.position);
                
                if (closedSet.contains(neighborKey) || navigationGrid.isObstacle(
                    navigationGrid.getCellX(neighbor.position.x),
                    navigationGrid.getCellY(neighbor.position.y))) {
                    continue;
                }
                
                float tentativeG = current.gCost + navigationGrid.getCost(
                    navigationGrid.getCellX(neighbor.position.x),
                    navigationGrid.getCellY(neighbor.position.y)
                );
                
                Node existingNode = allNodes.get(neighborKey);
                if (existingNode == null || tentativeG < existingNode.gCost) {
                    float hCost = heuristic(neighbor.position, end);
                    Node newNode = new Node(neighbor.position, current, tentativeG, hCost);
                    
                    openSet.offer(newNode);
                    allNodes.put(neighborKey, newNode);
                }
            }
        }
        
        // Reconstruir path
        List<Vector2D> path = new ArrayList<>();
        if (endNode != null) {
            Node current = endNode;
            while (current != null) {
                path.add(0, current.position.clone());
                current = current.parent;
            }
        }
        
        // Guardar en cache
        if (!path.isEmpty()) {
            pathCache.put(cacheKey, new PathCacheEntry(path, start.x, start.y, end.x, end.y));
        }
        
        return path;
    }
    
    /**
     * Clase auxiliar para el pathfinding A*
     */
    private static class Node implements Comparable<Node> {
        final Vector2D position;
        final Node parent;
        final float gCost;
        final float hCost;
        final float fCost;
        
        Node(Vector2D position, Node parent, float gCost, float hCost) {
            this.position = position;
            this.parent = parent;
            this.gCost = gCost;
            this.hCost = hCost;
            this.fCost = gCost + hCost;
        }
        
        @Override
        public int compareTo(Node other) {
            return Float.compare(this.fCost, other.fCost);
        }
    }
    
    /**
     * Genera vecinos para el pathfinding
     */
    private List<Node> getNeighbors(Vector2D position) {
        List<Node> neighbors = new ArrayList<>();
        
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                
                float newX = position.x + dx * navigationGrid.cellSize;
                float newY = position.y + dy * navigationGrid.cellSize;
                
                int cellX = navigationGrid.getCellX(newX);
                int cellY = navigationGrid.getCellY(newY);
                
                if (!navigationGrid.isObstacle(cellX, cellY)) {
                    neighbors.add(new Node(new Vector2D(newX, newY), null, 0, 0));
                }
            }
        }
        
        return neighbors;
    }
    
    /**
     * Heurística para A* (distancia euclidiana)
     */
    private float heuristic(Vector2D a, Vector2D b) {
        return a.distanceTo(b);
    }
    
    /**
     * Métodos auxiliares y configuraciones
     */
    private EntityRole getEntityRole(Entity entity) {
        if (entity instanceof Player) {
            return EntityRole.EXPLORER; // Default para players
        }
        return EntityRole.DEFENSIVE; // Default para NPCs
    }
    
    private float getMaxSpeed(Entity entity) {
        EntityRole role = getEntityRole(entity);
        return DEFAULT_MAX_SPEED * role.speedMultiplier;
    }
    
    private float getMaxForce(Entity entity) {
        return DEFAULT_MAX_FORCE;
    }
    
    private float getSlowRadius(Entity entity) {
        return 50.0f;
    }
    
    private String generateCacheKey(Vector2D start, Vector2D end) {
        int startX = (int) (start.x / navigationGrid.cellSize);
        int startY = (int) (start.y / navigationGrid.cellSize);
        int endX = (int) (end.x / navigationGrid.cellSize);
        int endY = (int) (end.y / navigationGrid.cellSize);
        return startX + "," + startY + "," + endX + "," + endY;
    }
    
    private String generateNodeKey(Vector2D position) {
        int cellX = navigationGrid.getCellX(position.x);
        int cellY = navigationGrid.getCellY(position.y);
        return cellX + "," + cellY;
    }
    
    /**
     * Gestión del navigation grid
     */
    public void updateObstacle(Vector2D position, boolean isObstacle) {
        int cellX = navigationGrid.getCellX(position.x);
        int cellY = navigationGrid.getCellY(position.y);
        navigationGrid.setObstacle(cellX, cellY, isObstacle);
        
        // Limpiar cache de paths afectados
        pathCache.clear(); // En implementación real, solo limpiar paths afectados
    }
    
    /**
     * Limpieza y mantenimiento
     */
    public void cleanup() {
        // Limpiar cache expirado
        pathCache.entrySet().removeIf(entry -> 
            System.currentTimeMillis() - entry.getValue().timestamp > 60000);
        
        // Limpiar entidades inactivas
        activeEntities.clear();
    }
    
    /**
     * Obtener estadísticas del sistema para debugging
     */
    public String getSystemStats() {
        return String.format("MovementSystem Stats:\n" +
                           "Active Entities: %d\n" +
                           "Cached Paths: %d\n" +
                           "Navigation Grid: %dx%d\n" +
                           "Vector Pool Size: %d\n" +
                           "Behavior Pool Size: %d",
                           activeEntities.size(),
                           pathCache.size(),
                           navigationGrid.width,
                           navigationGrid.height,
                           objectPool.vectorPool.size(),
                           objectPool.behaviorPool.size());
    }
}