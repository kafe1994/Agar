package com.gaming.enhancedagar.game;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import android.graphics.*;

/**
 * Sistema estático de física para el juego Enhanced Agar
 * Maneja colisiones, movimientos, fuerzas y física de fluidos
 */
public final class Physics {
    
    // Constantes físicas
    public static final float GRAVITY = 9.81f;
    public static final float AIR_RESISTANCE = 0.02f;
    public static final float FLUID_DRAG = 0.15f;
    public static final float ELASTICITY = 0.8f;
    public static final float MAX_SPEED = 15.0f;
    public static final float FRICTION_COEF = 0.1f;
    
    // Mapeo optimizado para detección de colisiones múltiples
    private static final Map<String, CollisionResult> collisionCache = new ConcurrentHashMap<>();
    private static final Set<String> activeCollisions = ConcurrentHashMap.newKeySet();
    
    // Pool de objetos para optimización
    private static final Queue<Point2D.Float> pointPool = new ArrayDeque<>();
    private static final Queue<Vector2D> vectorPool = new ArrayDeque<>();
    
    /**
     * Clase interna para vectores 2D optimizados
     */
    public static class Vector2D {
        public float x, y;
        
        public Vector2D() { this(0, 0); }
        public Vector2D(float x, float y) { this.x = x; this.y = y; }
        
        public Vector2D set(float x, float y) { this.x = x; this.y = y; return this; }
        public Vector2D add(Vector2D v) { x += v.x; y += v.y; return this; }
        public Vector2D sub(Vector2D v) { x -= v.x; y -= v.y; return this; }
        public Vector2D mul(float scalar) { x *= scalar; y *= scalar; return this; }
        public Vector2D div(float scalar) { x /= scalar; y /= scalar; return this; }
        
        public float magnitude() { return (float) Math.sqrt(x * x + y * y); }
        public Vector2D normalize() { 
            float mag = magnitude(); 
            if (mag > 0) { x /= mag; y /= mag; } 
            return this; 
        }
        
        public float dot(Vector2D v) { return x * v.x + y * v.y; }
        public Vector2D clone() { return new Vector2D(x, y); }
        
        public void release() {
            vectorPool.offer(this);
        }
    }
    
    /**
     * Clase interna para resultados de colisión
     */
    public static class CollisionResult {
        public boolean collided;
        public float overlap;
        public Vector2D normal;
        public Vector2D velocity;
        public float impulse;
        public Point2D.Float contactPoint;
        
        public CollisionResult() {
            this.collided = false;
            this.overlap = 0;
            this.normal = new Vector2D();
            this.velocity = new Vector2D();
            this.impulse = 0;
            this.contactPoint = new Point2D.Float();
        }
    }
    
    /**
     * Clase interna para entidades con propiedades físicas
     */
    public static class PhysicalEntity {
        public float x, y;
        public float radius;
        public float mass;
        public Vector2D velocity;
        public Vector2D acceleration;
        public float damping;
        public boolean isStatic;
        public float friction;
        public float restitution;
        public Color color;
        
        public PhysicalEntity(float x, float y, float radius, float mass) {
            this.x = x; this.y = y;
            this.radius = radius; this.mass = mass;
            this.velocity = new Vector2D();
            this.acceleration = new Vector2D();
            this.damping = 0.02f;
            this.friction = 0.1f;
            this.restitution = 0.8f;
            this.isStatic = false;
            this.color = Color.WHITE;
        }
        
        public void applyForce(Vector2D force) {
            if (!isStatic && mass > 0) {
                acceleration.x += force.x / mass;
                acceleration.y += force.y / mass;
            }
        }
        
        public void applyImpulse(Vector2D impulse) {
            if (!isStatic && mass > 0) {
                velocity.x += impulse.x / mass;
                velocity.y += impulse.y / mass;
            }
        }
        
        public float getKineticEnergy() {
            return 0.5f * mass * velocity.magnitude() * velocity.magnitude();
        }
    }
    
    // =============================================================================
    // DETECCIÓN DE COLISIONES CIRCULARES
    // =============================================================================
    
    /**
     * Detecta colisión entre dos círculos
     */
    public static CollisionResult detectCircularCollision(PhysicalEntity a, PhysicalEntity b) {
        String cacheKey = String.format("%d_%d", System.identityHashCode(a), System.identityHashCode(b));
        
        // Verificar cache para optimización
        CollisionResult cached = collisionCache.get(cacheKey);
        if (cached != null) {
            return cached.clone();
        }
        
        CollisionResult result = getCollisionResult();
        
        float dx = b.x - a.x;
        float dy = b.y - a.y;
        float distanceSq = dx * dx + dy * dy;
        float minDistance = a.radius + b.radius;
        
        if (distanceSq < minDistance * minDistance) {
            result.collided = true;
            result.overlap = minDistance - (float) Math.sqrt(distanceSq);
            
            // Calcular normal de colisión
            if (distanceSq > 0) {
                float distance = (float) Math.sqrt(distanceSq);
                result.normal.set(dx / distance, dy / distance);
                result.contactPoint.set(
                    a.x + result.normal.x * a.radius,
                    a.y + result.normal.y * a.radius
                );
            } else {
                // Colisión directa, normal arbitraria
                result.normal.set(1, 0);
                result.contactPoint.set(a.x, a.y);
            }
            
            // Calcular impulso de colisión
            result.impulse = calculateCollisionImpulse(a, b, result.normal);
        }
        
        // Guardar en cache
        collisionCache.put(cacheKey, result.clone());
        
        return result;
    }
    
    /**
     * Detecta colisiones múltiples usando optimización espacial
     */
    public static List<CollisionResult> detectMultipleCollisions(List<PhysicalEntity> entities) {
        List<CollisionResult> collisions = new ArrayList<>();
        
        for (int i = 0; i < entities.size(); i++) {
            for (int j = i + 1; j < entities.size(); j++) {
                CollisionResult collision = detectCircularCollision(entities.get(i), entities.get(j));
                if (collision.collided) {
                    collisions.add(collision);
                }
            }
        }
        
        return collisions;
    }
    
    // =============================================================================
    // ALGORITMOS DE REBOTE Y DAMPING
    // =============================================================================
    
    /**
     * Aplica rebote elástico entre dos entidades
     */
    public static void applyElasticBounce(PhysicalEntity a, PhysicalEntity b, CollisionResult collision) {
        if (!collision.collided) return;
        
        Vector2D normal = collision.normal;
        
        // Calcular velocidades relativas
        Vector2D relativeVel = a.velocity.clone().sub(b.velocity);
        float velAlongNormal = relativeVel.dot(normal);
        
        if (velAlongNormal > 0) return; // Ya se separando
        
        // Coeficiente de restitución (rebote)
        float e = Math.min(a.restitution, b.restitution) * ELASTICITY;
        
        // Calcular impulso
        float j = -(1 + e) * velAlongNormal;
        j /= (a.isStatic ? 0 : 1/a.mass) + (b.isStatic ? 0 : 1/b.mass);
        
        // Aplicar impulso
        Vector2D impulse = normal.clone().mul(j);
        if (!a.isStatic) a.velocity.sub(impulse.clone().mul(1/a.mass));
        if (!b.isStatic) b.velocity.add(impulse.clone().mul(1/b.mass));
        
        // Aplicar corrección de posición para evitar sinking
        float percent = 0.8f; // Corrección del 80%
        float slop = 0.01f; // Tolerancia
        float correctionMag = Math.max(collision.overlap - slop, 0) / 
                             ((a.isStatic ? 0 : 1/a.mass) + (b.isStatic ? 0 : 1/b.mass)) * percent;
        
        Vector2D correction = normal.clone().mul(correctionMag);
        if (!a.isStatic) {
            a.x -= correction.x / a.mass;
            a.y -= correction.y / a.mass;
        }
        if (!b.isStatic) {
            b.x += correction.x / b.mass;
            b.y += correction.y / b.mass;
        }
    }
    
    /**
     * Aplica damping (amortiguamiento) a la velocidad
     */
    public static void applyDamping(PhysicalEntity entity, float deltaTime) {
        float dampingFactor = (float) Math.exp(-entity.damping * deltaTime * 60);
        entity.velocity.mul(dampingFactor);
        
        // Damping adicional por fricción con el aire
        if (entity.velocity.magnitude() > 0) {
            Vector2D airResistance = entity.velocity.clone();
            airResistance.mul(-AIR_RESISTANCE * entity.velocity.magnitude() * deltaTime);
            entity.velocity.add(airResistance);
        }
    }
    
    /**
     * Aplica damping de fluidos
     */
    public static void applyFluidDamping(PhysicalEntity entity, float fluidDensity, float deltaTime) {
        float speed = entity.velocity.magnitude();
        if (speed > 0) {
            // Resistencia de fluidos proporcional al cuadrado de la velocidad
            float dragForce = 0.5f * fluidDensity * speed * speed * entity.radius * FLUID_DRAG;
            Vector2D drag = entity.velocity.clone().normalize().mul(-dragForce);
            entity.applyForce(drag);
        }
    }
    
    // =============================================================================
    // CÁLCULOS DE VELOCIDAD Y ACELERACIÓN
    // =============================================================================
    
    /**
     * Integra movimiento con aceleración constante
     */
    public static void integrateMotion(PhysicalEntity entity, float deltaTime) {
        if (entity.isStatic) return;
        
        // Aplicar gravedad si no está en fluido
        entity.acceleration.y += GRAVITY;
        
        // Actualizar velocidad
        entity.velocity.x += entity.acceleration.x * deltaTime;
        entity.velocity.y += entity.acceleration.y * deltaTime;
        
        // Limitar velocidad máxima
        float speed = entity.velocity.magnitude();
        if (speed > MAX_SPEED) {
            entity.velocity.normalize().mul(MAX_SPEED);
        }
        
        // Actualizar posición
        entity.x += entity.velocity.x * deltaTime;
        entity.y += entity.velocity.y * deltaTime;
        
        // Resetear aceleración
        entity.acceleration.set(0, 0);
    }
    
    /**
     * Calcula velocidad orbital alrededor de un punto
     */
    public static Vector2D calculateOrbitalVelocity(float centerX, float centerY, float distance, float mass) {
        Vector2D orbitalVel = getVector2D();
        
        if (mass > 0 && distance > 0) {
            // v = sqrt(G*M/r)
            float orbitalSpeed = (float) Math.sqrt(GRAVITY * mass / distance);
            orbitalVel.set(-orbitalSpeed, 0); // Velocidad tangencial inicial
        }
        
        return orbitalVel;
    }
    
    /**
     * Calcula aceleración centrípeta
     */
    public static Vector2D calculateCentripetalAcceleration(Vector2D velocity, float radius) {
        Vector2D centripetalAcc = velocity.clone();
        float speed = velocity.magnitude();
        
        if (radius > 0 && speed > 0) {
            float acceleration = speed * speed / radius;
            centripetalAcc.normalize().mul(acceleration);
        }
        
        return centripetalAcc;
    }
    
    // =============================================================================
    // SISTEMA DE IMPULSO Y FUERZA
    // =============================================================================
    
    /**
     * Aplica fuerza gravitacional entre dos entidades
     */
    public static void applyGravitationalForce(PhysicalEntity a, PhysicalEntity b) {
        float dx = b.x - a.x;
        float dy = b.y - a.y;
        float distanceSq = dx * dx + dy * dy;
        
        if (distanceSq < 1) return; // Evitar división por cero
        
        float distance = (float) Math.sqrt(distanceSq);
        float force = GRAVITY * a.mass * b.mass / distanceSq;
        
        Vector2D forceVector = new Vector2D(dx / distance * force, dy / distance * force);
        
        a.applyForce(forceVector);
        b.applyForce(forceVector.clone().mul(-1));
    }
    
    /**
     * Aplica fuerza de resorte
     */
    public static void applySpringForce(PhysicalEntity entity, Point2D.Float anchor, float stiffness, float restLength) {
        float dx = entity.x - anchor.x;
        float dy = entity.y - anchor.y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        
        if (distance != 0) {
            float displacement = distance - restLength;
            Vector2D force = new Vector2D(dx / distance, dy / distance).mul(-stiffness * displacement);
            entity.applyForce(force);
        }
    }
    
    /**
     * Aplica fuerza magnética entre entidades
     */
    public static void applyMagneticForce(PhysicalEntity a, PhysicalEntity b, float magneticForce) {
        Vector2D direction = new Vector2D(b.x - a.x, b.y - a.y);
        float distance = direction.magnitude();
        
        if (distance > 0) {
            direction.normalize();
            Vector2D force = direction.mul(magneticForce);
            a.applyForce(force);
            b.applyForce(force.clone().mul(-1));
        }
    }
    
    // =============================================================================
    // DETECCIÓN DE OVERLAPS
    // =============================================================================
    
    /**
     * Detecta overlap completo entre entidades
     */
    public static boolean detectOverlap(PhysicalEntity a, PhysicalEntity b) {
        CollisionResult collision = detectCircularCollision(a, b);
        return collision.collided && collision.overlap > a.radius * 0.5f;
    }
    
    /**
     * Encuentra todas las entidades que se superponen con una dada
     */
    public static List<PhysicalEntity> findOverlappingEntities(PhysicalEntity entity, List<PhysicalEntity> allEntities) {
        List<PhysicalEntity> overlaps = new ArrayList<>();
        
        for (PhysicalEntity other : allEntities) {
            if (other != entity && detectOverlap(entity, other)) {
                overlaps.add(other);
            }
        }
        
        return overlaps;
    }
    
    /**
     * Calcula área de superposición entre círculos
     */
    public static float calculateOverlapArea(PhysicalEntity a, PhysicalEntity b) {
        float r0 = a.radius;
        float r1 = b.radius;
        float d = distanceBetween(a, b);
        
        if (d >= r0 + r1) return 0; // Sin superposición
        if (d <= Math.abs(r0 - r1)) {
            // Un círculo está completamente dentro del otro
            return (float) Math.PI * Math.min(r0, r1) * Math.min(r0, r1);
        }
        
        // Calcular área de superposición parcial
        float alpha = (float) Math.acos((d*d + r0*r0 - r1*r1) / (2 * d * r0));
        float beta = (float) Math.acos((d*d + r1*r1 - r0*r0) / (2 * d * r1));
        
        float area0 = r0*r0 * alpha;
        float area1 = r1*r1 * beta;
        float area2 = 0.5f * (float) Math.sqrt(
            (-d + r0 + r1) * (d + r0 - r1) * (d - r0 + r1) * (d + r0 + r1)
        );
        
        return area0 + area1 - area2;
    }
    
    // =============================================================================
    // FÍSICA DE FLUIDOS BÁSICA
    // =============================================================================
    
    /**
     * Simula flotabilidad en fluido
     */
    public static void applyBuoyancy(PhysicalEntity entity, float fluidDensity, float fluidLevel) {
        if (entity.y < fluidLevel) {
            // Calcular volumen sumergido
            float submersionDepth = fluidLevel - entity.y;
            float submersionRatio = Math.min(submersionDepth / (entity.radius * 2), 1.0f);
            
            // Fuerza de flotabilidad: F = ρ * V * g
            float volume = (float) Math.PI * entity.radius * entity.radius * entity.radius * 4/3;
            float buoyantForce = fluidDensity * volume * submersionRatio * GRAVITY;
            
            Vector2D buoyancy = new Vector2D(0, -buoyantForce);
            entity.applyForce(buoyancy);
        }
    }
    
    /**
     * Simula tensión superficial
     */
    public static void applySurfaceTension(List<PhysicalEntity> entities, float tensionCoefficient) {
        for (int i = 0; i < entities.size(); i++) {
            for (int j = i + 1; j < entities.size(); j++) {
                PhysicalEntity a = entities.get(i);
                PhysicalEntity b = entities.get(j);
                
                float distance = distanceBetween(a, b);
                float contactDistance = a.radius + b.radius;
                
                if (distance < contactDistance * 2 && distance > 0) {
                    // Fuerza de tensión superficial proporcional al área de contacto
                    float tension = tensionCoefficient * (contactDistance * 2 - distance);
                    Vector2D direction = new Vector2D(b.x - a.x, b.y - a.y).normalize();
                    
                    Vector2D force = direction.mul(tension);
                    a.applyForce(force);
                    b.applyForce(force.clone().mul(-1));
                }
            }
        }
    }
    
    /**
     * Simula viscosidad de fluido
     */
    public static void applyViscosity(List<PhysicalEntity> entities, float viscosity) {
        for (int i = 0; i < entities.size(); i++) {
            for (int j = i + 1; j < entities.size(); j++) {
                PhysicalEntity a = entities.get(i);
                PhysicalEntity b = entities.get(j);
                
                float distance = distanceBetween(a, b);
                float influenceRadius = Math.max(a.radius, b.radius) * 3;
                
                if (distance < influenceRadius && distance > 0) {
                    // Fuerza viscosa proporcional a la diferencia de velocidades
                    Vector2D velocityDiff = a.velocity.clone().sub(b.velocity);
                    Vector2D direction = new Vector2D(b.x - a.x, b.y - a.y).normalize();
                    
                    float viscosityForce = viscosity * velocityDiff.dot(direction) / (distance * distance);
                    Vector2D force = direction.mul(-viscosityForce);
                    
                    a.applyForce(force);
                    b.applyForce(force.clone().mul(-1));
                }
            }
        }
    }
    
    // =============================================================================
    // MÉTODOS AUXILIARES Y UTILIDADES
    // =============================================================================
    
    /**
     * Calcula distancia entre dos entidades
     */
    public static float distanceBetween(PhysicalEntity a, PhysicalEntity b) {
        float dx = b.x - a.x;
        float dy = b.y - a.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Calcula ángulo entre dos entidades
     */
    public static float calculateAngle(PhysicalEntity from, PhysicalEntity to) {
        return (float) Math.atan2(to.y - from.y, to.x - from.x);
    }
    
    /**
     * Convierte velocidad polar a cartesiana
     */
    public static Vector2D polarToCartesian(float speed, float angle) {
        return getVector2D().set(
            speed * (float) Math.cos(angle),
            speed * (float) Math.sin(angle)
        );
    }
    
    /**
     * Convierte velocidad cartesiana a polar
     */
    public static Vector2D cartesianToPolar(Vector2D velocity) {
        float speed = velocity.magnitude();
        float angle = (float) Math.atan2(velocity.y, velocity.x);
        return polarToCartesian(speed, angle);
    }
    
    /**
     * Resuelve colisión con paredes
     */
    public static void resolveWallCollision(PhysicalEntity entity, Rectangle bounds) {
        // Colisión con pared izquierda
        if (entity.x - entity.radius < bounds.x) {
            entity.x = bounds.x + entity.radius;
            entity.velocity.x = Math.abs(entity.velocity.x) * entity.restitution;
        }
        
        // Colisión con pared derecha
        if (entity.x + entity.radius > bounds.x + bounds.width) {
            entity.x = bounds.x + bounds.width - entity.radius;
            entity.velocity.x = -Math.abs(entity.velocity.x) * entity.restitution;
        }
        
        // Colisión con pared superior
        if (entity.y - entity.radius < bounds.y) {
            entity.y = bounds.y + entity.radius;
            entity.velocity.y = Math.abs(entity.velocity.y) * entity.restitution;
        }
        
        // Colisión con pared inferior
        if (entity.y + entity.radius > bounds.y + bounds.height) {
            entity.y = bounds.y + bounds.height - entity.radius;
            entity.velocity.y = -Math.abs(entity.velocity.y) * entity.restitution;
        }
    }
    
    /**
     * Calcula impulso de colisión
     */
    private static float calculateCollisionImpulse(PhysicalEntity a, PhysicalEntity b, Vector2D normal) {
        Vector2D relativeVel = a.velocity.clone().sub(b.velocity);
        float velAlongNormal = relativeVel.dot(normal);
        
        if (velAlongNormal > 0) return 0;
        
        float e = Math.min(a.restitution, b.restitution);
        float j = -(1 + e) * velAlongNormal;
        j /= (a.isStatic ? 0 : 1/a.mass) + (b.isStatic ? 0 : 1/b.mass);
        
        return j;
    }
    
    // =============================================================================
    // SISTEMA DE POOL PARA OPTIMIZACIÓN
    // =============================================================================
    
    /**
     * Obtiene un vector del pool
     */
    private static Vector2D getVector2D() {
        return vectorPool.poll() != null ? vectorPool.poll() : new Vector2D();
    }
    
    /**
     * Obtiene un resultado de colisión del pool
     */
    private static CollisionResult getCollisionResult() {
        return new CollisionResult();
    }
    
    /**
     * Limpia el cache de colisiones
     */
    public static void clearCollisionCache() {
        collisionCache.clear();
        activeCollisions.clear();
    }
    
    /**
     * Actualiza el sistema de física
     */
    public static void updatePhysics(List<PhysicalEntity> entities, float deltaTime, Rectangle bounds) {
        // Aplicar fuerzas y integrar movimiento
        for (PhysicalEntity entity : entities) {
            applyDamping(entity, deltaTime);
            integrateMotion(entity, deltaTime);
            resolveWallCollision(entity, bounds);
        }
        
        // Detectar y resolver colisiones
        List<CollisionResult> collisions = detectMultipleCollisions(entities);
        for (CollisionResult collision : collisions) {
            // Encontrar las entidades involucradas en esta colisión
            // Nota: En una implementación real, necesitaríamos mapear la colisión a las entidades
        }
        
        // Limpiar cache periódicamente
        if (System.currentTimeMillis() % 1000 < 16) { // Cada ~1 segundo
            clearCollisionCache();
        }
    }
    
    /**
     * Constructor privado para clase estática
     */
    private Physics() {}
}