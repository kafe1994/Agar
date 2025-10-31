package com.gaming.enhancedagar.engine;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.awt.*;
import java.awt.geom.AffineTransform;

/**
 * Sistema avanzado de partículas con pool, múltiples tipos y optimización
 * Soporta explosions, trails, sparks, smoke, fire y efectos especiales
 */
public class ParticleSystem {
    
    // Pool de partículas para optimización
    private static class ParticlePool {
        private final Queue<Particle> pool = new ArrayDeque<>();
        private final int maxPoolSize;
        
        public ParticlePool(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }
        
        public Particle get() {
            return pool.isEmpty() ? new Particle() : pool.poll();
        }
        
        public void release(Particle particle) {
            if (pool.size() < maxPoolSize) {
                pool.offer(particle);
            }
        }
    }
    
    // Partícula individual
    private static class Particle {
        public boolean active = false;
        public float x, y;
        public float vx, vy;
        public float ax, ay;
        public float size;
        public float alpha;
        public float lifetime;
        public float maxLifetime;
        public Color color;
        public Color endColor;
        public float rotation;
        public float rotationSpeed;
        public ParticleType type;
        public int pattern;
        public float[] parameters;
        
        public void reset() {
            active = false;
            x = y = vx = vy = ax = ay = 0;
            size = alpha = lifetime = maxLifetime = 0;
            color = endColor = Color.WHITE;
            rotation = rotationSpeed = 0;
            type = ParticleType.TRAIL;
            pattern = 0;
            parameters = null;
        }
    }
    
    // Tipos de partículas
    public enum ParticleType {
        EXPLOSION, TRAIL, SPARK, SMOKE, FIRE, STAR, MAGIC, SPLASH, SHOCKWAVE, HEAL
    }
    
    // Configuración de perfil de detalle
    public enum DetailProfile {
        LOW(200, 0.3f, false),
        MEDIUM(500, 0.6f, true),
        HIGH(1000, 1.0f, true);
        
        public final int maxParticles;
        public final float effectIntensity;
        public final boolean enableComplexEffects;
        
        DetailProfile(int maxParticles, float effectIntensity, boolean enableComplexEffects) {
            this.maxParticles = maxParticles;
            this.effectIntensity = effectIntensity;
            this.enableComplexEffects = enableComplexEffects;
        }
    }
    
    // Parámetros de configuración por tipo
    private static class ParticleConfig {
        public final float baseSize;
        public final float baseLifetime;
        public final float baseSpeed;
        public final Color baseColor;
        public final int patterns;
        public final boolean hasGravity;
        public final boolean hasFriction;
        
        public ParticleConfig(float baseSize, float baseLifetime, float baseSpeed, 
                            Color baseColor, int patterns, boolean hasGravity, boolean hasFriction) {
            this.baseSize = baseSize;
            this.baseLifetime = baseLifetime;
            this.baseSpeed = baseSpeed;
            this.baseColor = baseColor;
            this.patterns = patterns;
            this.hasGravity = hasGravity;
            this.hasFriction = hasFriction;
        }
    }
    
    // Configuraciones predefinidas
    private static final Map<ParticleType, ParticleConfig> PARTICLE_CONFIGS = new HashMap<>();
    
    static {
        PARTICLE_CONFIGS.put(ParticleType.EXPLOSION, 
            new ParticleConfig(8f, 1.2f, 200f, Color.ORANGE, 3, true, true));
        PARTICLE_CONFIGS.put(ParticleType.TRAIL, 
            new ParticleConfig(3f, 0.8f, 50f, Color.CYAN, 2, false, true));
        PARTICLE_CONFIGS.put(ParticleType.SPARK, 
            new ParticleConfig(2f, 1.0f, 300f, Color.YELLOW, 4, true, true));
        PARTICLE_CONFIGS.put(ParticleType.SMOKE, 
            new ParticleConfig(12f, 2.5f, 30f, Color.GRAY, 2, true, false));
        PARTICLE_CONFIGS.put(ParticleType.FIRE, 
            new ParticleConfig(6f, 1.5f, 80f, new Color(255, 100, 0), 5, true, true));
        PARTICLE_CONFIGS.put(ParticleType.STAR, 
            new ParticleConfig(4f, 1.8f, 120f, Color.WHITE, 6, false, false));
        PARTICLE_CONFIGS.put(ParticleType.MAGIC, 
            new ParticleConfig(5f, 2.0f, 100f, new Color(128, 0, 255), 8, false, true));
        PARTICLE_CONFIGS.put(ParticleType.SPLASH, 
            new ParticleConfig(3f, 1.2f, 150f, Color.BLUE, 3, true, true));
        PARTICLE_CONFIGS.put(ParticleType.SHOCKWAVE, 
            new ParticleConfig(15f, 0.8f, 250f, Color.WHITE, 1, false, false));
        PARTICLE_CONFIGS.put(ParticleType.HEAL, 
            new ParticleConfig(4f, 2.5f, 60f, Color.GREEN, 4, false, true));
    }
    
    // Estado del sistema
    private final List<Particle> particles = new ArrayList<>();
    private final ParticlePool pool;
    private DetailProfile currentProfile = DetailProfile.MEDIUM;
    private final Map<String, Color> colorPalette = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private boolean enabled = true;
    
    // Estadísticas
    private int totalSpawned = 0;
    private int totalKilled = 0;
    private long lastUpdateTime = 0;
    
    public ParticleSystem(DetailProfile profile) {
        this.pool = new ParticlePool(profile.maxParticles * 2);
        setProfile(profile);
        initializeColorPalette();
    }
    
    public ParticleSystem() {
        this(DetailProfile.MEDIUM);
    }
    
    private void initializeColorPalette() {
        colorPalette.put("explosion", new Color(255, 120, 0));
        colorPalette.put("trail", new Color(0, 255, 255));
        colorPalette.put("spark", new Color(255, 255, 0));
        colorPalette.put("smoke", new Color(80, 80, 80));
        colorPalette.put("fire", new Color(255, 80, 0));
        colorPalette.put("star", Color.WHITE);
        colorPalette.put("magic", new Color(128, 0, 255));
        colorPalette.put("splash", new Color(0, 150, 255));
        colorPalette.put("shockwave", Color.WHITE);
        colorPalette.put("heal", new Color(0, 255, 100));
    }
    
    /**
     * Establece el perfil de detalle del sistema
     */
    public void setProfile(DetailProfile profile) {
        this.currentProfile = profile;
        trimParticlesToLimit();
    }
    
    private void trimParticlesToLimit() {
        while (particles.size() > currentProfile.maxParticles) {
            Particle particle = particles.remove(particles.size() - 1);
            pool.release(particle);
        }
    }
    
    /**
     * Genera partículas de explosión
     */
    public void createExplosion(float x, float y, float power, Color customColor) {
        if (!enabled) return;
        
        ParticleConfig config = PARTICLE_CONFIGS.get(ParticleType.EXPLOSION);
        int particleCount = (int)(power * 15 * currentProfile.effectIntensity);
        particleCount = Math.min(particleCount, 50);
        
        for (int i = 0; i < particleCount; i++) {
            float angle = (float)(Math.random() * Math.PI * 2);
            float speed = (float)(Math.random() * config.baseSpeed * power);
            
            createParticle(ParticleType.EXPLOSION, x, y,
                (float)Math.cos(angle) * speed,
                (float)Math.sin(angle) * speed,
                customColor != null ? customColor : config.baseColor);
        }
        
        // Ondas de choque para altos detalles
        if (currentProfile.enableComplexEffects) {
            createShockwave(x, y, power * 30);
        }
    }
    
    /**
     * Genera partículas de rastro (trail)
     */
    public void createTrail(float x, float y, float vx, float vy, Color color) {
        if (!enabled) return;
        
        // Reducir frecuencia según el perfil
        if (random.nextFloat() > currentProfile.effectIntensity) return;
        
        float speed = (float)Math.sqrt(vx * vx + vy * vy);
        float lifeMultiplier = Math.min(speed / 200f, 2f);
        
        createParticle(ParticleType.TRAIL, x, y,
            vx * -0.1f + (random.nextFloat() - 0.5f) * 20,
            vy * -0.1f + (random.nextFloat() - 0.5f) * 20,
            color != null ? color : PARTICLE_CONFIGS.get(ParticleType.TRAIL).baseColor,
            lifeMultiplier);
    }
    
    /**
     * Genera chispas (sparks)
     */
    public void createSparks(float x, float y, int count, Color color) {
        if (!enabled) return;
        
        count = (int)(count * currentProfile.effectIntensity);
        count = Math.min(count, 20);
        
        for (int i = 0; i < count; i++) {
            float angle = (float)(Math.random() * Math.PI * 2);
            float speed = (float)(Math.random() * PARTICLE_CONFIGS.get(ParticleType.SPARK).baseSpeed);
            
            createParticle(ParticleType.SPARK, x, y,
                (float)Math.cos(angle) * speed,
                (float)Math.sin(angle) * speed,
                color != null ? color : PARTICLE_CONFIGS.get(ParticleType.SPARK).baseColor);
        }
    }
    
    /**
     * Genera humo
     */
    public void createSmoke(float x, float y, int count, Color color) {
        if (!enabled) return;
        
        count = (int)(count * currentProfile.effectIntensity);
        
        for (int i = 0; i < count; i++) {
            float angle = (float)(Math.random() * Math.PI * 2);
            float speed = (float)(Math.random() * PARTICLE_CONFIGS.get(ParticleType.SMOKE).baseSpeed);
            
            createParticle(ParticleType.SMOKE, x, y,
                (float)Math.cos(angle) * speed,
                (float)Math.sin(angle) * speed,
                color != null ? color : PARTICLE_CONFIGS.get(ParticleType.SMOKE).baseColor);
        }
    }
    
    /**
     * Genera fuego
     */
    public void createFire(float x, float y, int count) {
        if (!enabled) return;
        
        count = (int)(count * currentProfile.effectIntensity);
        
        for (int i = 0; i < count; i++) {
            createParticle(ParticleType.FIRE, x + (random.nextFloat() - 0.5f) * 20,
                y + (random.nextFloat() - 0.5f) * 20,
                (random.nextFloat() - 0.5f) * 40,
                (random.nextFloat() - 0.5f) * 40,
                PARTICLE_CONFIGS.get(ParticleType.FIRE).baseColor);
        }
    }
    
    /**
     * Genera efectos mágicos
     */
    public void createMagic(float x, float y, int count, Color color) {
        if (!enabled) return;
        
        count = (int)(count * currentProfile.effectIntensity);
        
        for (int i = 0; i < count; i++) {
            float angle = (i / (float)count) * (float)(Math.PI * 2);
            float speed = 60f + random.nextFloat() * 40f;
            
            createParticle(ParticleType.MAGIC, x, y,
                (float)Math.cos(angle) * speed,
                (float)Math.sin(angle) * speed,
                color != null ? color : PARTICLE_CONFIGS.get(ParticleType.MAGIC).baseColor);
        }
    }
    
    /**
     * Genera ondas de choque
     */
    public void createShockwave(float x, float y, float radius) {
        if (!enabled || !currentProfile.enableComplexEffects) return;
        
        createParticle(ParticleType.SHOCKWAVE, x, y, 0, 0, Color.WHITE, 1f, radius * 2);
    }
    
    /**
     * Crea partículas de sanación
     */
    public void createHeal(float x, float y, int count) {
        if (!enabled) return;
        
        count = (int)(count * currentProfile.effectIntensity);
        
        for (int i = 0; i < count; i++) {
            float angle = (float)(Math.random() * Math.PI * 2);
            float speed = (float)(Math.random() * 60);
            
            createParticle(ParticleType.HEAL, x, y,
                (float)Math.cos(angle) * speed,
                (float)Math.sin(angle) * speed,
                Color.GREEN);
        }
    }
    
    /**
     * Método auxiliar para crear partículas
     */
    private void createParticle(ParticleType type, float x, float y, float vx, float vy, Color color) {
        createParticle(type, x, y, vx, vy, color, 1f);
    }
    
    private void createParticle(ParticleType type, float x, float y, float vx, float vy, 
                              Color color, float lifeMultiplier) {
        createParticle(type, x, y, vx, vy, color, lifeMultiplier, 0);
    }
    
    private void createParticle(ParticleType type, float x, float y, float vx, float vy, 
                              Color color, float lifeMultiplier, float customSize) {
        if (particles.size() >= currentProfile.maxParticles) return;
        
        Particle particle = pool.get();
        particle.reset();
        
        particle.active = true;
        particle.x = x;
        particle.y = y;
        particle.vx = vx;
        particle.vy = vy;
        particle.type = type;
        particle.pattern = random.nextInt(PARTICLE_CONFIGS.get(type).patterns);
        
        // Configurar tamaño
        float baseSize = customSize > 0 ? customSize : PARTICLE_CONFIGS.get(type).baseSize;
        particle.size = baseSize * (0.5f + random.nextFloat() * 0.5f);
        
        // Configurar vida
        float baseLifetime = PARTICLE_CONFIGS.get(type).baseLifetime;
        particle.lifetime = 0;
        particle.maxLifetime = baseLifetime * lifeMultiplier * (0.8f + random.nextFloat() * 0.4f);
        
        // Configurar color
        particle.color = color;
        if (type == ParticleType.FIRE || type == ParticleType.EXPLOSION) {
            particle.endColor = new Color(255, 50, 0);
        } else {
            particle.endColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 0);
        }
        
        // Configurar efectos especiales
        if (type == ParticleType.STAR || type == ParticleType.MAGIC) {
            particle.rotationSpeed = (random.nextFloat() - 0.5f) * 8f;
        } else {
            particle.rotationSpeed = (random.nextFloat() - 0.5f) * 4f;
        }
        
        particles.add(particle);
        totalSpawned++;
    }
    
    /**
     * Actualiza todas las partículas
     */
    public void update(float deltaTime) {
        if (!enabled) return;
        
        long startTime = System.nanoTime();
        
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle particle = particles.get(i);
            
            if (!updateParticle(particle, deltaTime)) {
                particles.remove(i);
                pool.release(particle);
                totalKilled++;
            }
        }
        
        lastUpdateTime = System.nanoTime() - startTime;
    }
    
    /**
     * Actualiza una partícula individual
     */
    private boolean updateParticle(Particle particle, float deltaTime) {
        particle.lifetime += deltaTime;
        
        if (particle.lifetime >= particle.maxLifetime) {
            return false;
        }
        
        float lifeProgress = particle.lifetime / particle.maxLifetime;
        
        // Aplicar gravedad y fricción según el tipo
        ParticleConfig config = PARTICLE_CONFIGS.get(particle.type);
        
        if (config.hasGravity) {
            particle.vy += 50f * deltaTime; // Gravedad
        }
        
        if (config.hasFriction) {
            particle.vx *= (1f - 0.1f * deltaTime);
            particle.vy *= (1f - 0.1f * deltaTime);
        }
        
        // Actualizar velocidad con aceleración
        particle.vx += particle.ax * deltaTime;
        particle.vy += particle.ay * deltaTime;
        
        // Actualizar posición
        particle.x += particle.vx * deltaTime;
        particle.y += particle.vy * deltaTime;
        
        // Actualizar rotación
        particle.rotation += particle.rotationSpeed * deltaTime;
        
        // Actualizar alpha basado en la vida
        particle.alpha = 1f - lifeProgress;
        
        // Efectos especiales según el tipo
        applySpecialEffects(particle, lifeProgress);
        
        return true;
    }
    
    /**
     * Aplica efectos especiales a las partículas
     */
    private void applySpecialEffects(Particle particle, float lifeProgress) {
        switch (particle.type) {
            case FIRE:
                particle.size *= (1f + 0.5f * deltaTime);
                float fireAlpha = (float)Math.sin(lifeProgress * Math.PI) * particle.alpha;
                particle.alpha = Math.max(fireAlpha, particle.alpha * 0.3f);
                break;
                
            case SMOKE:
                particle.vy -= 20f * deltaTime; // Humo sube
                particle.size *= (1f + 0.3f * deltaTime);
                break;
                
            case SHOCKWAVE:
                float expandRate = 200f * deltaTime;
                particle.size += expandRate;
                particle.alpha *= (1f - 0.5f * deltaTime);
                break;
                
            case STAR:
            case MAGIC:
                particle.vx *= (1f - 0.2f * deltaTime);
                particle.vy *= (1f - 0.2f * deltaTime);
                break;
                
            case HEAL:
                particle.vy -= 30f * deltaTime; // Partículas de sanación suben
                break;
        }
    }
    
    /**
     * Renderiza todas las partículas
     */
    public void render(Graphics2D g2d) {
        if (!enabled) return;
        
        // Configurar blending para efectos
        Composite originalComposite = g2d.getComposite();
        
        // Agrupar por tipo para renderizado eficiente
        renderParticlesByType(g2d, ParticleType.TRAIL);
        renderParticlesByType(g2d, ParticleType.SMOKE);
        renderParticlesByType(g2d, ParticleType.FIRE);
        renderParticlesByType(g2d, ParticleType.EXPLOSION);
        renderParticlesByType(g2d, ParticleType.SPARK);
        renderParticlesByType(g2d, ParticleType.STAR);
        renderParticlesByType(g2d, ParticleType.MAGIC);
        renderParticlesByType(g2d, ParticleType.SPLASH);
        renderParticlesByType(g2d, ParticleType.SHOCKWAVE);
        renderParticlesByType(g2d, ParticleType.HEAL);
        
        // Restaurar composite original
        g2d.setComposite(originalComposite);
    }
    
    private void renderParticlesByType(Graphics2D g2d, ParticleType type) {
        boolean hasAdditive = false;
        
        for (Particle particle : particles) {
            if (particle.type != type || !particle.active) continue;
            
            // Configurar alpha blending
            float finalAlpha = particle.alpha * currentProfile.effectIntensity;
            if (finalAlpha < 0.05f) continue;
            
            Composite composite = g2d.getComposite();
            
            // Usar additive blending para efectos luminosos
            if (shouldUseAdditiveBlending(particle.type)) {
                if (!hasAdditive) {
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, finalAlpha));
                    hasAdditive = true;
                }
            } else {
                if (hasAdditive) {
                    g2d.setComposite(composite);
                    hasAdditive = false;
                }
            }
            
            renderParticle(g2d, particle);
        }
        
        if (hasAdditive) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }
    }
    
    private boolean shouldUseAdditiveBlending(ParticleType type) {
        return type == ParticleType.EXPLOSION || type == ParticleType.FIRE || 
               type == ParticleType.MAGIC || type == ParticleType.HEAL || 
               type == ParticleType.STAR || type == ParticleType.SPARK;
    }
    
    private void renderParticle(Graphics2D g2d, Particle particle) {
        g2d.setTransform(AffineTransform.getRotateInstance(particle.rotation, particle.x, particle.y));
        
        float size = particle.size;
        float alpha = Math.min(1f, particle.alpha * currentProfile.effectIntensity);
        
        Color renderColor = blendColors(particle.color, particle.endColor, 
                                      particle.lifetime / particle.maxLifetime);
        
        renderColor = new Color(renderColor.getRed(), renderColor.getGreen(), 
                              renderColor.getBlue(), (int)(alpha * 255));
        
        g2d.setColor(renderColor);
        
        switch (particle.type) {
            case TRAIL:
                renderTrail(g2d, particle.x, particle.y, size);
                break;
            case SPARK:
                renderSpark(g2d, particle.x, particle.y, size, particle.pattern);
                break;
            case SMOKE:
                renderSmoke(g2d, particle.x, particle.y, size);
                break;
            case FIRE:
                renderFire(g2d, particle.x, particle.y, size);
                break;
            case STAR:
                renderStar(g2d, particle.x, particle.y, size, particle.pattern);
                break;
            case MAGIC:
                renderMagic(g2d, particle.x, particle.y, size, particle.pattern);
                break;
            case SHOCKWAVE:
                renderShockwave(g2d, particle.x, particle.y, size);
                break;
            default:
                renderCircle(g2d, particle.x, particle.y, size);
                break;
        }
        
        g2d.setTransform(AffineTransform.getRotateInstance(0, 0, 0));
    }
    
    private void renderTrail(Graphics2D g2d, float x, float y, float size) {
        int s = (int)size;
        g2d.fillOval((int)(x - s), (int)(y - s), s * 2, s * 2);
    }
    
    private void renderSpark(Graphics2D g2d, float x, float y, float size, int pattern) {
        int length = (int)(size * 3);
        int thickness = Math.max(1, (int)(size / 3));
        
        g2d.setStroke(new BasicStroke(thickness));
        
        switch (pattern % 4) {
            case 0: // Líneas rectas
                g2d.drawLine((int)(x - length), (int)y, (int)(x + length), (int)y);
                break;
            case 1: // Líneas verticales
                g2d.drawLine((int)x, (int)(y - length), (int)x, (int)(y + length));
                break;
            case 2: // Líneas diagonales /
                g2d.drawLine((int)(x - length), (int)(y + length), 
                           (int)(x + length), (int)(y - length));
                break;
            case 3: // Líneas diagonales \
                g2d.drawLine((int)(x - length), (int)(y - length), 
                           (int)(x + length), (int)(y + length));
                break;
        }
    }
    
    private void renderSmoke(Graphics2D g2d, float x, float y, float size) {
        // Renderizar múltiples círculos para efecto de humo
        int circles = 3;
        for (int i = 0; i < circles; i++) {
            float offset = i * size * 0.3f;
            float alpha = 0.3f / (i + 1);
            g2d.setColor(new Color(80, 80, 80, (int)(alpha * 255)));
            g2d.fillOval((int)(x - offset), (int)(y - offset), 
                        (int)(size * 2 + offset * 2), (int)(size * 2 + offset * 2));
        }
    }
    
    private void renderFire(Graphics2D g2d, float x, float y, float size) {
        // Efecto de llama triangular
        int baseSize = (int)(size * 2);
        int[] xPoints = {(int)x, (int)(x - baseSize), (int)(x + baseSize)};
        int[] yPoints = {(int)(y - baseSize), (int)(y + baseSize), (int)(y + baseSize)};
        
        GradientPaint gradient = new GradientPaint(x, y - baseSize, 
                                                 new Color(255, 255, 100, 200),
                                                 x, y + baseSize, 
                                                 new Color(255, 50, 0, 200));
        
        g2d.setPaint(gradient);
        g2d.fillPolygon(xPoints, yPoints, 3);
    }
    
    private void renderStar(Graphics2D g2d, float x, float y, float size, int pattern) {
        int points = 4 + (pattern % 4);
        double radius = size;
        double innerRadius = radius * 0.4;
        
        int[] xPoints = new int[points * 2];
        int[] yPoints = new int[points * 2];
        
        for (int i = 0; i < points * 2; i++) {
            double angle = i * Math.PI / points;
            double r = (i % 2 == 0) ? radius : innerRadius;
            
            xPoints[i] = (int)(x + r * Math.cos(angle));
            yPoints[i] = (int)(y + r * Math.sin(angle));
        }
        
        g2d.fillPolygon(xPoints, yPoints, points * 2);
    }
    
    private void renderMagic(Graphics2D g2d, float x, float y, float size, int pattern) {
        // Efecto de cristal/magia con formas geométricas
        int sides = 3 + (pattern % 4);
        int radius = (int)(size * 1.5);
        
        int[] xPoints = new int[sides];
        int[] yPoints = new int[sides];
        
        for (int i = 0; i < sides; i++) {
            double angle = i * 2 * Math.PI / sides;
            xPoints[i] = (int)(x + radius * Math.cos(angle));
            yPoints[i] = (int)(y + radius * Math.sin(angle));
        }
        
        g2d.fillPolygon(xPoints, yPoints, sides);
    }
    
    private void renderShockwave(Graphics2D g2d, float x, float y, float size) {
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval((int)(x - size), (int)(y - size), 
                    (int)(size * 2), (int)(size * 2));
    }
    
    private void renderCircle(Graphics2D g2d, float x, float y, float size) {
        g2d.fillOval((int)(x - size), (int)(y - size), (int)(size * 2), (int)(size * 2));
    }
    
    private Color blendColors(Color start, Color end, float factor) {
        factor = Math.max(0, Math.min(1, factor));
        
        int r = (int)(start.getRed() * (1 - factor) + end.getRed() * factor);
        int g = (int)(start.getGreen() * (1 - factor) + end.getGreen() * factor);
        int b = (int)(start.getBlue() * (1 - factor) + end.getBlue() * factor);
        int a = (int)(start.getAlpha() * (1 - factor) + end.getAlpha() * factor);
        
        return new Color(r, g, b, a);
    }
    
    /**
     * Limpia todas las partículas
     */
    public void clear() {
        for (Particle particle : particles) {
            pool.release(particle);
        }
        particles.clear();
    }
    
    /**
     * Habilita/deshabilita el sistema de partículas
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Obtiene el número de partículas activas
     */
    public int getActiveParticleCount() {
        return particles.size();
    }
    
    /**
     * Obtiene las estadísticas del sistema
     */
    public ParticleStats getStats() {
        return new ParticleStats(
            particles.size(),
            totalSpawned,
            totalKilled,
            lastUpdateTime / 1_000_000.0, // Convertir a milisegundos
            currentProfile
        );
    }
    
    /**
     * Clase para estadísticas del sistema
     */
    public static class ParticleStats {
        public final int activeParticles;
        public final int totalSpawned;
        public final int totalKilled;
        public final double lastUpdateTime;
        public final DetailProfile profile;
        
        public ParticleStats(int activeParticles, int totalSpawned, int totalKilled, 
                           double lastUpdateTime, DetailProfile profile) {
            this.activeParticles = activeParticles;
            this.totalSpawned = totalSpawned;
            this.totalKilled = totalKilled;
            this.lastUpdateTime = lastUpdateTime;
            this.profile = profile;
        }
        
        @Override
        public String toString() {
            return String.format("Partículas activas: %d | Generadas: %d | Eliminadas: %d | " +
                               "Última actualización: %.2fms | Perfil: %s",
                               activeParticles, totalSpawned, totalKilled, 
                               lastUpdateTime, profile.name());
        }
    }
}