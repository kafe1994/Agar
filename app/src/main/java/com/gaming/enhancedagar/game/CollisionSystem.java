package com.gaming.enhancedagar.game;

import android.graphics.RectF;
import android.util.Log;

import com.gaming.enhancedagar.entities.Entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sistema avanzado de colisiones para Enhanced Agar
 * Implementa:
 * - Broad phase collision detection usando Quadtree
 * - Narrow phase collision (círculos y rectángulos)
 * - Sistema de respuesta a colisiones
 * - Resolución de overlaps
 * - Optimización espacial para mejor performance
 * - Eventos de colisión para notificaciones
 */
public class CollisionSystem {
    private static final String TAG = "CollisionSystem";
    
    // Configuración del quadtree
    private static final int MAX_OBJECTS_PER_NODE = 10;
    private static final int MAX_LEVELS = 5;
    
    // Listas para optimización
    private List<Entity> dynamicEntities;
    private List<Entity> staticEntities;
    private List<Entity> allEntities;
    
    // Quadtree para optimización espacial
    private Quadtree quadtree;
    
    // Mapeo de colisiones para evitar duplicados
    private Set<String> activeCollisions;
    
    // Listeners de eventos de colisión
    private List<CollisionListener> collisionListeners;
    
    // Estadísticas para debugging
    private CollisionStats stats;
    
    // Filtros de colisión
    private CollisionFilter collisionFilter;
    
    /**
     * Constructor del sistema de colisiones
     */
    public CollisionSystem() {
        this.dynamicEntities = new ArrayList<>();
        this.staticEntities = new ArrayList<>();
        this.allEntities = new ArrayList<>();
        this.quadtree = new Quadtree(0, new RectF(0, 0, 10000, 10000));
        this.activeCollisions = new HashSet<>();
        this.collisionListeners = new ArrayList<>();
        this.stats = new CollisionStats();
        this.collisionFilter = new CollisionFilter();
    }
    
    /**
     * Actualiza el sistema de colisiones
     * @param deltaTime tiempo transcurrido en milisegundos
     * @param worldBounds límites del mundo
     */
    public void update(float deltaTime, RectF worldBounds) {
        stats.startFrame();
        
        try {
            // Actualizar quadtree con nuevos límites
            quadtree.setBounds(worldBounds);
            quadtree.clear();
            
            // Reconstruir quadtree con todas las entidades activas
            rebuildQuadtree();
            
            // Detectar colisiones
            detectCollisions();
            
            // Resolver colisiones detectadas
            resolveCollisions();
            
            // Limpiar colisiones que ya no existen
            cleanupCollisions();
            
        } catch (Exception e) {
            Log.e(TAG, "Error actualizando sistema de colisiones", e);
        }
        
        stats.endFrame();
    }
    
    /**
     * Reconstruye el quadtree con todas las entidades activas
     */
    private void rebuildQuadtree() {
        for (Entity entity : allEntities) {
            if (entity.isActive() && entity.isAlive()) {
                quadtree.insert(entity);
            }
        }
    }
    
    /**
     * Detecta todas las colisiones en el mundo
     */
    private void detectCollisions() {
        List<Entity> entitiesToCheck = new ArrayList<>();
        
        // Broad phase: obtener entidades potencialmente colisionables
        for (Entity entity : allEntities) {
            if (entity.isActive() && entity.isAlive()) {
                List<Entity> nearbyEntities = quadtree.retrieve(entity);
                entitiesToCheck.addAll(nearbyEntities);
            }
        }
        
        // Narrow phase: verificar colisiones específicas
        Set<String> currentFrameCollisions = new HashSet<>();
        
        for (int i = 0; i < allEntities.size(); i++) {
            Entity entityA = allEntities.get(i);
            if (!entityA.isActive() || !entityA.isAlive()) continue;
            
            for (int j = i + 1; j < allEntities.size(); j++) {
                Entity entityB = allEntities.get(j);
                if (!entityB.isActive() || !entityB.isAlive()) continue;
                
                // Verificar si estas entidades pueden colisionar
                if (!collisionFilter.canCollide(entityA, entityB)) continue;
                
                String collisionKey = getCollisionKey(entityA, entityB);
                if (activeCollisions.contains(collisionKey)) {
                    currentFrameCollisions.add(collisionKey);
                    continue; // Ya procesada en frame anterior
                }
                
                // Detectar colisión
                if (isColliding(entityA, entityB)) {
                    currentFrameCollisions.add(collisionKey);
                    
                    CollisionInfo collisionInfo = new CollisionInfo(entityA, entityB);
                    stats.recordCollision(collisionInfo);
                    
                    // Notificar listeners
                    notifyCollisionListeners("collisionDetected", collisionInfo);
                    
                    // Trigger eventos en entidades
                    entityA.onCollision(entityB);
                    entityB.onCollision(entityA);
                }
            }
        }
        
        // Actualizar set de colisiones activas
        activeCollisions.clear();
        activeCollisions.addAll(currentFrameCollisions);
    }
    
    /**
     * Resuelve todas las colisiones detectadas
     */
    private void resolveCollisions() {
        for (String collisionKey : activeCollisions) {
            String[] entityIds = collisionKey.split("_");
            if (entityIds.length != 2) continue;
            
            Entity entityA = findEntityById(Long.parseLong(entityIds[0]));
            Entity entityB = findEntityById(Long.parseLong(entityIds[1]));
            
            if (entityA != null && entityB && isColliding(entityA, entityB)) {
                resolveCollision(entityA, entityB);
            }
        }
    }
    
    /**
     * Resuelve una colisión específica entre dos entidades
     * @param entityA primera entidad
     * @param entityB segunda entidad
     */
    private void resolveCollision(Entity entityA, Entity entityB) {
        // Determinar método de resolución basado en tipos
        if (isCircleCollision(entityA, entityB)) {
            resolveCircleCollision(entityA, entityB);
        } else if (isRectCollision(entityA, entityB)) {
            resolveRectCollision(entityA, entityB);
        } else {
            resolveMixedCollision(entityA, entityB);
        }
        
        // Notificar resolución
        CollisionInfo collisionInfo = new CollisionInfo(entityA, entityB);
        notifyCollisionListeners("collisionResolved", collisionInfo);
    }
    
    /**
     * Resuelve colisión entre dos círculos
     */
    private void resolveCircleCollision(Entity entityA, Entity entityB) {
        float radiusA = Math.max(entityA.getWidth(), entityA.getHeight()) / 2;
        float radiusB = Math.max(entityB.getWidth(), entityB.getHeight()) / 2;
        
        float dx = entityB.getX() - entityA.getX();
        float dy = entityB.getY() - entityA.getY();
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        
        if (distance == 0) {
            // Evitar división por cero, separación aleatoria
            float angle = (float) (Math.random() * Math.PI * 2);
            dx = (float) Math.cos(angle);
            dy = (float) Math.sin(angle);
            distance = 1;
        }
        
        float overlap = (radiusA + radiusB) - distance;
        if (overlap > 0) {
            // Separar entidades basado en masa
            float totalMass = entityA.getMass() + entityB.getMass();
            float separationA = overlap * (entityB.getMass() / totalMass);
            float separationB = overlap * (entityA.getMass() / totalMass);
            
            float nx = dx / distance;
            float ny = dy / distance;
            
            // Mover entidades
            entityA.setX(entityA.getX() - nx * separationA);
            entityA.setY(entityA.getY() - ny * separationA);
            entityB.setX(entityB.getX() + nx * separationB);
            entityB.setY(entityB.getY() + ny * separationB);
            
            // Aplicar impulso basado en elasticidad
            applyCollisionImpulse(entityA, entityB, nx, ny, overlap);
        }
    }
    
    /**
     * Resuelve colisión entre dos rectángulos
     */
    private void resolveRectCollision(Entity entityA, Entity entityB) {
        RectF boundsA = entityA.getBounds();
        RectF boundsB = entityB.getBounds();
        
        float centerXA = (boundsA.left + boundsA.right) / 2;
        float centerYA = (boundsA.top + boundsA.bottom) / 2;
        float centerXB = (boundsB.left + boundsB.right) / 2;
        float centerYB = (boundsB.top + boundsB.bottom) / 2;
        
        float overlapX = Math.min(boundsA.right, boundsB.right) - Math.max(boundsA.left, boundsB.left);
        float overlapY = Math.min(boundsA.bottom, boundsB.bottom) - Math.max(boundsA.top, boundsB.top);
        
        if (overlapX > 0 && overlapY > 0) {
            // Determinar eje de mínima penetración
            if (overlapX < overlapY) {
                // Separar en X
                float direction = centerXA < centerXB ? -1 : 1;
                float separation = overlapX / 2;
                
                entityA.setX(entityA.getX() + direction * separation);
                entityB.setX(entityB.getX() - direction * separation);
                
                // Intercambiar velocidades en X
                float tempVelX = entityA.getVelocityX();
                entityA.setVelocity(entityB.getVelocityX(), entityA.getVelocityY());
                entityB.setVelocity(tempVelX, entityB.getVelocityY());
                
            } else {
                // Separar en Y
                float direction = centerYA < centerYB ? -1 : 1;
                float separation = overlapY / 2;
                
                entityA.setY(entityA.getY() + direction * separation);
                entityB.setY(entityB.getY() - direction * separation);
                
                // Intercambiar velocidades en Y
                float tempVelY = entityA.getVelocityY();
                entityA.setVelocity(entityA.getVelocityX(), entityB.getVelocityY());
                entityB.setVelocity(entityB.getVelocityX(), tempVelY);
            }
        }
    }
    
    /**
     * Resuelve colisión mixta (círculo-rectángulo)
     */
    private void resolveMixedCollision(Entity entityA, Entity entityB) {
        // Implementar detección y resolución específica para círculo-rectángulo
        if (isCircleAABBCollision(entityA, entityB)) {
            resolveCircleAABBCollision(entityA, entityB);
        } else {
            // Fallback a resolución rectangular
            resolveRectCollision(entityA, entityB);
        }
    }
    
    /**
     * Resuelve colisión círculo-AABB
     */
    private void resolveCircleAABBCollision(Entity circleEntity, Entity rectEntity) {
        RectF rectBounds = rectEntity.getBounds();
        float circleRadius = Math.max(circleEntity.getWidth(), circleEntity.getHeight()) / 2;
        
        // Encontrar punto más cercano del rectángulo al círculo
        float closestX = Math.max(rectBounds.left, Math.min(circleEntity.getX(), rectBounds.right));
        float closestY = Math.max(rectBounds.top, Math.min(circleEntity.getY(), rectBounds.bottom));
        
        float dx = circleEntity.getX() - closestX;
        float dy = circleEntity.getY() - closestY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        
        if (distance < circleRadius) {
            float overlap = circleRadius - distance;
            
            if (distance == 0) {
                // Círculo está exactamente en una esquina o borde
                // Separar en la dirección de menor penetración
                float centerX = rectEntity.getX();
                float centerY = rectEntity.getY();
                float dxCenter = circleEntity.getX() - centerX;
                float dyCenter = circleEntity.getY() - centerY;
                
                if (Math.abs(dxCenter) > Math.abs(dyCenter)) {
                    dx = dxCenter > 0 ? 1 : -1;
                    dy = 0;
                } else {
                    dx = 0;
                    dy = dyCenter > 0 ? 1 : -1;
                }
                distance = 1;
            }
            
            float nx = dx / distance;
            float ny = dy / distance;
            
            circleEntity.setX(circleEntity.getX() + nx * overlap);
            circleEntity.setY(circleEntity.getY() + ny * overlap);
        }
    }
    
    /**
     * Aplica impulso de colisión basado en elasticidad
     */
    private void applyCollisionImpulse(Entity entityA, Entity entityB, float nx, float ny, float overlap) {
        float elasticity = 0.8f; // Coeficiente de restitución
        
        // Calcular velocidades relativas
        float rvx = entityB.getVelocityX() - entityA.getVelocityX();
        float rvy = entityB.getVelocityY() - entityA.getVelocityY();
        float velAlongNormal = rvx * nx + rvy * ny;
        
        if (velAlongNormal > 0) return; // Las entidades se están separando
        
        // Calcular impulso
        float invMassA = 1 / entityA.getMass();
        float invMassB = 1 / entityB.getMass();
        float impulseScalar = -(1 + elasticity) * velAlongNormal / (invMassA + invMassB);
        
        float impulseX = impulseScalar * nx;
        float impulseY = impulseScalar * ny;
        
        // Aplicar impulso
        entityA.setVelocity(entityA.getVelocityX() - impulseX * invMassA, 
                          entityA.getVelocityY() - impulseY * invMassA);
        entityB.setVelocity(entityB.getVelocityX() + impulseX * invMassB, 
                          entityB.getVelocityY() + impulseY * invMassB);
    }
    
    /**
     * Verifica si dos entidades colisionan
     */
    private boolean isColliding(Entity entityA, Entity entityB) {
        return isCircleCollision(entityA, entityB) || 
               isRectCollision(entityA, entityB) || 
               isCircleAABBCollision(entityA, entityB);
    }
    
    /**
     * Verifica colisión circular
     */
    private boolean isCircleCollision(Entity entityA, Entity entityB) {
        float radiusA = Math.max(entityA.getWidth(), entityA.getHeight()) / 2;
        float radiusB = Math.max(entityB.getWidth(), entityB.getHeight()) / 2;
        
        float dx = entityA.getX() - entityB.getX();
        float dy = entityA.getY() - entityB.getY();
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        
        return distance < (radiusA + radiusB);
    }
    
    /**
     * Verifica colisión rectangular
     */
    private boolean isRectCollision(Entity entityA, Entity entityB) {
        return RectF.intersects(entityA.getBounds(), entityB.getBounds());
    }
    
    /**
     * Verifica colisión círculo-AABB
     */
    private boolean isCircleAABBCollision(Entity circleEntity, Entity rectEntity) {
        RectF rectBounds = rectEntity.getBounds();
        float circleRadius = Math.max(circleEntity.getWidth(), circleEntity.getHeight()) / 2;
        
        // Encontrar punto más cercano del rectángulo al círculo
        float closestX = Math.max(rectBounds.left, Math.min(circleEntity.getX(), rectBounds.right));
        float closestY = Math.max(rectBounds.top, Math.min(circleEntity.getY(), rectBounds.bottom));
        
        float dx = circleEntity.getX() - closestX;
        float dy = circleEntity.getY() - closestY;
        
        return (dx * dx + dy * dy) < (circleRadius * circleRadius);
    }
    
    /**
     * Limpia colisiones que ya no existen
     */
    private void cleanupCollisions() {
        Set<String> validCollisions = new HashSet<>();
        
        for (String collisionKey : activeCollisions) {
            String[] entityIds = collisionKey.split("_");
            if (entityIds.length != 2) continue;
            
            Entity entityA = findEntityById(Long.parseLong(entityIds[0]));
            Entity entityB = findEntityById(Long.parseLong(entityIds[1]));
            
            if (entityA != null && entityB != null && 
                entityA.isActive() && entityB.isActive() && 
                isColliding(entityA, entityB)) {
                validCollisions.add(collisionKey);
            }
        }
        
        activeCollisions.clear();
        activeCollisions.addAll(validCollisions);
    }
    
    /**
     * Obtiene clave única para par de entidades
     */
    private String getCollisionKey(Entity entityA, Entity entityB) {
        long id1 = Math.min(entityA.getId(), entityB.getId());
        long id2 = Math.max(entityA.getId(), entityB.getId());
        return id1 + "_" + id2;
    }
    
    /**
     * Encuentra entidad por ID
     */
    private Entity findEntityById(long id) {
        for (Entity entity : allEntities) {
            if (entity.getId() == id) {
                return entity;
            }
        }
        return null;
    }
    
    /**
     * Notifica listeners de colisión
     */
    private void notifyCollisionListeners(String eventType, CollisionInfo collisionInfo) {
        for (CollisionListener listener : collisionListeners) {
            try {
                listener.onCollisionEvent(eventType, collisionInfo);
            } catch (Exception e) {
                Log.e(TAG, "Error notificando listener de colisión", e);
            }
        }
    }
    
    /**
     * Añade entidad al sistema
     */
    public void addEntity(Entity entity, boolean isStatic) {
        if (entity == null) return;
        
        if (isStatic) {
            staticEntities.add(entity);
        } else {
            dynamicEntities.add(entity);
        }
        allEntities.add(entity);
    }
    
    /**
     * Remueve entidad del sistema
     */
    public void removeEntity(Entity entity) {
        if (entity == null) return;
        
        staticEntities.remove(entity);
        dynamicEntities.remove(entity);
        allEntities.remove(entity);
        
        // Limpiar colisiones relacionadas
        activeCollisions.removeIf(key -> 
            key.contains("_" + entity.getId()) || key.startsWith(entity.getId() + "_")
        );
    }
    
    /**
     * Añade listener de colisiones
     */
    public void addCollisionListener(CollisionListener listener) {
        if (!collisionListeners.contains(listener)) {
            collisionListeners.add(listener);
        }
    }
    
    /**
     * Remueve listener de colisiones
     */
    public void removeCollisionListener(CollisionListener listener) {
        collisionListeners.remove(listener);
    }
    
    /**
     * Obtiene estadísticas del sistema
     */
    public CollisionStats getStats() {
        return stats;
    }
    
    /**
     * Limpia el sistema
     */
    public void clear() {
        dynamicEntities.clear();
        staticEntities.clear();
        allEntities.clear();
        quadtree.clear();
        activeCollisions.clear();
        collisionListeners.clear();
        stats.reset();
    }
    
    // Getters
    public List<Entity> getDynamicEntities() { return new ArrayList<>(dynamicEntities); }
    public List<Entity> getStaticEntities() { return new ArrayList<>(staticEntities); }
    public List<Entity> getAllEntities() { return new ArrayList<>(allEntities); }
    
    /**
     * Interface para listeners de eventos de colisión
     */
    public interface CollisionListener {
        void onCollisionEvent(String eventType, CollisionInfo collisionInfo);
    }
    
    /**
     * Información de colisión
     */
    public static class CollisionInfo {
        public final Entity entityA;
        public final Entity entityB;
        public final long timestamp;
        public final String collisionType;
        
        public CollisionInfo(Entity entityA, Entity entityB) {
            this.entityA = entityA;
            this.entityB = entityB;
            this.timestamp = System.currentTimeMillis();
            this.collisionType = determineCollisionType(entityA, entityB);
        }
        
        private String determineCollisionType(Entity a, Entity b) {
            if (isCircleCircle(a, b)) return "circle_circle";
            if (isRectRect(a, b)) return "rect_rect";
            if (isCircleRect(a, b)) return "circle_rect";
            if (isRectCircle(a, b)) return "rect_circle";
            return "unknown";
        }
        
        private boolean isCircleCircle(Entity a, Entity b) {
            float ratioA = Math.abs(a.getWidth() - a.getHeight()) / Math.max(a.getWidth(), a.getHeight());
            float ratioB = Math.abs(b.getWidth() - b.getHeight()) / Math.max(b.getWidth(), b.getHeight());
            return ratioA < 0.1f && ratioB < 0.1f;
        }
        
        private boolean isRectRect(Entity a, Entity b) {
            return true; // Por defecto asumir rectángulos
        }
        
        private boolean isCircleRect(Entity a, Entity b) {
            float ratioA = Math.abs(a.getWidth() - a.getHeight()) / Math.max(a.getWidth(), a.getHeight());
            return ratioA < 0.1f;
        }
        
        private boolean isRectCircle(Entity a, Entity b) {
            float ratioB = Math.abs(b.getWidth() - b.getHeight()) / Math.max(b.getWidth(), b.getHeight());
            return ratioB < 0.1f;
        }
    }
    
    /**
     * Estadísticas del sistema de colisiones
     */
    public static class CollisionStats {
        private int totalCollisions;
        private int activeCollisions;
        private long frameTime;
        private float avgFrameTime;
        private int framesProcessed;
        
        public void startFrame() {
            frameTime = System.nanoTime();
        }
        
        public void endFrame() {
            long endTime = System.nanoTime();
            long frameDuration = endTime - frameTime;
            
            frameTime = frameDuration / 1_000_000; // Convertir a milisegundos
            avgFrameTime = (avgFrameTime * framesProcessed + frameTime) / (framesProcessed + 1);
            framesProcessed++;
        }
        
        public void recordCollision(CollisionInfo collisionInfo) {
            totalCollisions++;
            activeCollisions++;
        }
        
        public void reset() {
            totalCollisions = 0;
            activeCollisions = 0;
            frameTime = 0;
            avgFrameTime = 0;
            framesProcessed = 0;
        }
        
        public void updateActiveCollisions(int count) {
            this.activeCollisions = count;
        }
        
        // Getters
        public int getTotalCollisions() { return totalCollisions; }
        public int getActiveCollisions() { return activeCollisions; }
        public float getFrameTime() { return frameTime; }
        public float getAvgFrameTime() { return avgFrameTime; }
        public int getFramesProcessed() { return framesProcessed; }
    }
    
    /**
     * Filtro de colisiones personalizable
     */
    public static class CollisionFilter {
        private Set<String> collisionGroups;
        private Map<String, Set<String>> allowedCollisions;
        
        public CollisionFilter() {
            this.collisionGroups = new HashSet<>();
            this.allowedCollisions = new HashMap<>();
        }
        
        public void addCollisionGroup(String groupName) {
            collisionGroups.add(groupName);
        }
        
        public void allowCollisionBetween(String groupA, String groupB) {
            allowedCollisions.computeIfAbsent(groupA, k -> new HashSet<>()).add(groupB);
            allowedCollisions.computeIfAbsent(groupB, k -> new HashSet<>()).add(groupA);
        }
        
        public boolean canCollide(Entity entityA, Entity entityB) {
            // Por defecto, todas las entidades pueden colisionar
            return true;
        }
        
        public void clear() {
            collisionGroups.clear();
            allowedCollisions.clear();
        }
    }
    
    /**
     * Quadtree para optimización espacial
     */
    private static class Quadtree {
        private int level;
        private RectF bounds;
        private List<Entity> objects;
        private Quadtree[] nodes;
        
        public Quadtree(int level, RectF bounds) {
            this.level = level;
            this.bounds = bounds;
            this.objects = new ArrayList<>();
            this.nodes = new Quadtree[4];
        }
        
        public void clear() {
            objects.clear();
            for (int i = 0; i < nodes.length; i++) {
                if (nodes[i] != null) {
                    nodes[i].clear();
                    nodes[i] = null;
                }
            }
        }
        
        public void setBounds(RectF newBounds) {
            this.bounds = newBounds;
        }
        
        public void split() {
            float subWidth = bounds.width() / 2;
            float subHeight = bounds.height() / 2;
            float x = bounds.left;
            float y = bounds.top;
            
            nodes[0] = new Quadtree(level + 1, new RectF(x + subWidth, y, x + subWidth + subWidth, y + subHeight)); // NE
            nodes[1] = new Quadtree(level + 1, new RectF(x, y, x + subWidth, y + subHeight)); // NW
            nodes[2] = new Quadtree(level + 1, new RectF(x, y + subHeight, x + subWidth, y + subHeight + subHeight)); // SW
            nodes[3] = new Quadtree(level + 1, new RectF(x + subWidth, y + subHeight, x + subWidth + subWidth, y + subHeight + subHeight)); // SE
        }
        
        public int getIndex(Entity entity) {
            int index = -1;
            double verticalMidpoint = bounds.left + (bounds.width() / 2);
            double horizontalMidpoint = bounds.top + (bounds.height() / 2);
            
            boolean topQuadrant = entity.getY() < horizontalMidpoint && entity.getY() + entity.getHeight() < horizontalMidpoint;
            boolean bottomQuadrant = entity.getY() > horizontalMidpoint;
            
            if (entity.getX() < verticalMidpoint && entity.getX() + entity.getWidth() < verticalMidpoint) {
                if (topQuadrant) {
                    index = 1; // NW
                } else if (bottomQuadrant) {
                    index = 2; // SW
                }
            } else if (entity.getX() > verticalMidpoint) {
                if (topQuadrant) {
                    index = 0; // NE
                } else if (bottomQuadrant) {
                    index = 3; // SE
                }
            }
            
            return index;
        }
        
        public void insert(Entity entity) {
            if (nodes[0] != null) {
                int index = getIndex(entity);
                if (index != -1) {
                    nodes[index].insert(entity);
                    return;
                }
            }
            
            objects.add(entity);
            
            if (objects.size() > MAX_OBJECTS_PER_NODE && level < MAX_LEVELS) {
                if (nodes[0] == null) {
                    split();
                }
                
                int i = 0;
                while (i < objects.size()) {
                    int index = getIndex(objects.get(i));
                    if (index != -1) {
                        nodes[index].insert(objects.remove(i));
                    } else {
                        i++;
                    }
                }
            }
        }
        
        public List<Entity> retrieve(Entity entity) {
            List<Entity> returnObjects = objects;
            
            if (nodes[0] != null) {
                int index = getIndex(entity);
                if (index != -1) {
                    returnObjects = nodes[index].retrieve(entity);
                } else {
                    // La entidad se extiende por múltiples cuadrantes
                    returnObjects = new ArrayList<>();
                    for (int i = 0; i < nodes.length; i++) {
                        if (nodes[i] != null) {
                            returnObjects.addAll(nodes[i].retrieve(entity));
                        }
                    }
                    returnObjects.addAll(objects);
                }
            }
            
            return returnObjects;
        }
    }
}