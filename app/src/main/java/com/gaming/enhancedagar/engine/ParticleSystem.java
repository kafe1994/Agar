package com.gaming.enhancedagar.engine;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import android.graphics.*;

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
        public int color;  // Android int color (ARGB)
        public int endColor;  // Android int color (ARGB)
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
        public final int baseColor;  // Android int color (ARGB)
        public final int patterns;
        public final boolean hasGravity;
        public final boolean hasFriction;
        
        public ParticleConfig(float baseSize, float baseLifetime, float baseSpeed, 
                            int baseColor, int patterns, boolean hasGravity, boolean hasFriction) {
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
            new ParticleConfig(8f, 1.2f, 200f, Color.rgb(255, 165, 0), 3, true, true)); // Orange
        PARTICLE_CONFIGS.put(ParticleType.TRAIL, 
            new ParticleConfig(3f, 0.8f, 50f, Color.CYAN, 2, false, true));
        PARTICLE_CONFIGS.put(ParticleType.SPARK, 
            new ParticleConfig(2f, 1.0f, 300f, Color.YELLOW, 4, true, true));
        PARTICLE_CONFIGS.put(ParticleType.SMOKE, 
            new ParticleConfig(12f, 2.5f, 30f, Color.GRAY, 2, true, false));
        PARTICLE_CONFIGS.put(ParticleType.FIRE, 
            new ParticleConfig(6f, 1.5f, 80f, Color.rgb(255, 100, 0), 5, true, true)); // Red-Orange
        PARTICLE_CONFIGS.put(ParticleType.STAR, 
            new ParticleConfig(4f, 1.8f, 120f, Color.WHITE, 6, false, false));
        PARTICLE_CONFIGS.put(ParticleType.MAGIC, 
            new ParticleConfig(5f, 2.0f, 100f, Color.rgb(128, 0, 255), 8, false, true)); // Purple
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
    private final Map<String, Integer> colorPalette = new ConcurrentHashMap<>();  // Android int colors
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
        colorPalette.put("explosion", Color.rgb(255, 120, 0));
        colorPalette.put("trail", Color.CYAN);
        colorPalette.put("spark", Color.YELLOW);
        colorPalette.put("smoke", Color.rgb(80, 80, 80));
        colorPalette.put("fire", Color.rgb(255, 80, 0));
        colorPalette.put("star", Color.WHITE);
        colorPalette.put("magic", Color.rgb(128, 0, 255));
        colorPalette.put("splash", Color.rgb(0, 150, 255));
        colorPalette.put("shockwave", Color.WHITE);
        colorPalette.put("heal", Color.rgb(0, 255, 100));
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
    public void createExplosion(float x, float y, float power, Integer customColor) {
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
    public void createTrail(float x, float y, float vx, float vy, Integer color) {
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
    public void createSparks(float x, float y, int count, Integer color) {
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
    public void createSmoke(float x, float y, int count, Integer color) {
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
    public void createMagic(float x, float y, int count, Integer color) {
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
     * Genera efectos de salpicadura
     */
    public void createSplash(float x, float y, int count, Integer color) {
        if (!enabled) return;
        
        count = (int)(count * currentProfile.effectIntensity);
        
        for (int i = 0; i < count; i++) {
            float angle = (float)(Math.random() * Math.PI * 2);
            float speed = (float)(Math.random() * PARTICLE_CONFIGS.get(ParticleType.SPLASH).baseSpeed);
            
            createParticle(ParticleType.SPLASH, x, y,
                (float)Math.cos(angle) * speed,
                (float)Math.sin(angle) * speed,
                color != null ? color : PARTICLE_CONFIGS.get(ParticleType.SPLASH).baseColor);
        }
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
    private void createParticle(ParticleType type, float x, float y, float vx, float vy, int color) {
        createParticle(type, x, y, vx, vy, color, 1f);
    }
    
    private void createParticle(ParticleType type, float x, float y, float vx, float vy, 
                              int color, float lifeMultiplier) {
        createParticle(type, x, y, vx, vy, color, lifeMultiplier, 0);
    }
    
    private void createParticle(ParticleType type, float x, float y, float vx, float vy, 
                              int color, float lifeMultiplier, float customSize) {
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
        
        // Configurar color (Android int color)
        particle.color = color;
        if (type == ParticleType.FIRE || type == ParticleType.EXPLOSION) {
            particle.endColor = Color.rgb(255, 50, 0);  // Dark red
        } else {
            // Extraer componentes RGB del color int y crear color con alpha 0
            int r = Color.red(color);
            int g = Color.green(color);
            int b = Color.blue(color);
            particle.endColor = Color.argb(0, r, g, b);
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
        applySpecialEffects(particle, lifeProgress, deltaTime);
        
        return true;
    }
    
    /**
     * Aplica efectos especiales a las partículas
     */
    private void applySpecialEffects(Particle particle, float lifeProgress, float deltaTime) {
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
    public void render(Canvas canvas) {
        if (!enabled || canvas == null) return;
        
        // Agrupar por tipo para renderizado eficiente
        renderParticlesByType(canvas, ParticleType.TRAIL);
        renderParticlesByType(canvas, ParticleType.SMOKE);
        renderParticlesByType(canvas, ParticleType.FIRE);
        renderParticlesByType(canvas, ParticleType.EXPLOSION);
        renderParticlesByType(canvas, ParticleType.SPARK);
        renderParticlesByType(canvas, ParticleType.STAR);
        renderParticlesByType(canvas, ParticleType.MAGIC);
        renderParticlesByType(canvas, ParticleType.SPLASH);
        renderParticlesByType(canvas, ParticleType.SHOCKWAVE);
        renderParticlesByType(canvas, ParticleType.HEAL);
    }
    
    private void renderParticlesByType(Canvas canvas, ParticleType type) {
        boolean hasAdditive = false;
        Paint currentPaint = new Paint();
        
        for (Particle particle : particles) {
            if (particle.type != type || !particle.active) continue;
            
            // Configurar alpha blending
            float finalAlpha = particle.alpha * currentProfile.effectIntensity;
            if (finalAlpha < 0.05f) continue;
            
            // Usar additive blending para efectos luminosos (simplificado para Android)
            if (shouldUseAdditiveBlending(particle.type)) {
                if (!hasAdditive) {
                    currentPaint.setAlpha((int)(finalAlpha * 255));
                    currentPaint.setXfermode(PorterDuff.Mode.SRC_OVER);
                    hasAdditive = true;
                }
            } else {
                if (hasAdditive) {
                    currentPaint.setAlpha(255);
                    currentPaint.setXfermode(null);
                    hasAdditive = false;
                }
            }
            
            renderParticle(canvas, particle, currentPaint);
        }
        
        if (hasAdditive) {
            currentPaint.setAlpha(255);
            currentPaint.setXfermode(null);
        }
    }
    
    private boolean shouldUseAdditiveBlending(ParticleType type) {
        return type == ParticleType.EXPLOSION || type == ParticleType.FIRE || 
               type == ParticleType.MAGIC || type == ParticleType.HEAL || 
               type == ParticleType.STAR || type == ParticleType.SPARK;
    }
    
    private void renderParticle(Canvas canvas, Particle particle, Paint paint) {
        // Usar Matrix para rotación en Android
        Matrix matrix = new Matrix();
        matrix.setRotate((float)Math.toDegrees(particle.rotation), particle.x, particle.y);
        
        float size = particle.size;
        float alpha = Math.min(1f, particle.alpha * currentProfile.effectIntensity);
        
        int renderColor = blendColors(particle.color, particle.endColor, 
                                    particle.lifetime / particle.maxLifetime);
        
        // Aplicar alpha al color
        int r = Color.red(renderColor);
        int g = Color.green(renderColor);
        int b = Color.blue(renderColor);
        renderColor = Color.argb((int)(alpha * 255), r, g, b);
        
        paint.setColor(renderColor);
        
        // Aplicar transformación
        canvas.save();
        canvas.setMatrix(matrix);
        
        switch (particle.type) {
            case TRAIL:
                renderTrail(canvas, particle.x, particle.y, size, paint);
                break;
            case SPARK:
                renderSpark(canvas, particle.x, particle.y, size, particle.pattern, paint);
                break;
            case SMOKE:
                renderSmoke(canvas, particle.x, particle.y, size, paint);
                break;
            case FIRE:
                renderFire(canvas, particle.x, particle.y, size, paint);
                break;
            case STAR:
                renderStar(canvas, particle.x, particle.y, size, particle.pattern, paint);
                break;
            case MAGIC:
                renderMagic(canvas, particle.x, particle.y, size, particle.pattern, paint);
                break;
            case SHOCKWAVE:
                renderShockwave(canvas, particle.x, particle.y, size, paint);
                break;
            default:
                renderCircle(canvas, particle.x, particle.y, size, paint);
                break;
        }
        
        canvas.restore();
    }
    
    private void renderTrail(Canvas canvas, float x, float y, float size, Paint paint) {
        int s = (int)size;
        canvas.drawCircle(x, y, s, paint);
    }
    
    private void renderSpark(Canvas canvas, float x, float y, float size, int pattern, Paint paint) {
        int length = (int)(size * 3);
        int thickness = Math.max(1, (int)(size / 3));
        
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(thickness);
        
        switch (pattern % 4) {
            case 0: // Líneas rectas
                canvas.drawLine(x - length, y, x + length, y, paint);
                break;
            case 1: // Líneas verticales
                canvas.drawLine(x, y - length, x, y + length, paint);
                break;
            case 2: // Líneas diagonales /
                canvas.drawLine(x - length, y + length, x + length, y - length, paint);
                break;
            case 3: // Líneas diagonales \
                canvas.drawLine(x - length, y - length, x + length, y + length, paint);
                break;
        }
    }
    
    private void renderSmoke(Canvas canvas, float x, float y, float size, Paint paint) {
        // Renderizar múltiples círculos para efecto de humo
        int circles = 3;
        Paint smokePaint = new Paint(paint);
        smokePaint.setStyle(Paint.Style.FILL);
        
        for (int i = 0; i < circles; i++) {
            float offset = i * size * 0.3f;
            float alpha = 0.3f / (i + 1);
            smokePaint.setColor(Color.argb((int)(alpha * 255), 80, 80, 80));
            
            canvas.drawCircle(x - offset, y - offset, size + offset, smokePaint);
        }
    }
    
    private void renderFire(Canvas canvas, float x, float y, float size, Paint paint) {
        // Efecto de llama triangular - usando gradiente básico
        int baseSize = (int)(size * 2);
        
        // Crear path triangular para la llama
        android.graphics.Path path = new android.graphics.Path();
        path.moveTo(x, y - baseSize);  // Top
        path.lineTo(x - baseSize, y + baseSize);  // Bottom left
        path.lineTo(x + baseSize, y + baseSize);  // Bottom right
        path.close();
        
        // Simular gradiente creando múltiples capas
        paint.setStyle(Paint.Style.FILL);
        
        // Capa exterior (más oscura)
        paint.setColor(Color.rgb(255, 50, 0));
        canvas.drawPath(path, paint);
        
        // Capa intermedia
        canvas.save();
        canvas.scale(0.7f, 0.7f, x, y);
        paint.setColor(Color.rgb(255, 100, 0));
        canvas.drawPath(path, paint);
        
        // Capa interior (más clara)
        paint.setColor(Color.rgb(255, 255, 100));
        canvas.drawPath(path, paint);
        canvas.restore();
    }
    
    private void renderStar(Canvas canvas, float x, float y, float size, int pattern, Paint paint) {
        int points = 4 + (pattern % 4);
        double radius = size;
        double innerRadius = radius * 0.4;
        
        android.graphics.Path path = new android.graphics.Path();
        
        for (int i = 0; i < points * 2; i++) {
            double angle = i * Math.PI / points;
            double r = (i % 2 == 0) ? radius : innerRadius;
            
            float px = (float)(x + r * Math.cos(angle));
            float py = (float)(y + r * Math.sin(angle));
            
            if (i == 0) {
                path.moveTo(px, py);
            } else {
                path.lineTo(px, py);
            }
        }
        
        path.close();
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPath(path, paint);
    }
    
    private void renderMagic(Canvas canvas, float x, float y, float size, int pattern, Paint paint) {
        // Efecto de cristal/magia con formas geométricas
        int sides = 3 + (pattern % 4);
        float radius = size * 1.5f;
        
        android.graphics.Path path = new android.graphics.Path();
        
        for (int i = 0; i < sides; i++) {
            double angle = i * 2 * Math.PI / sides;
            float px = (float)(x + radius * Math.cos(angle));
            float py = (float)(y + radius * Math.sin(angle));
            
            if (i == 0) {
                path.moveTo(px, py);
            } else {
                path.lineTo(px, py);
            }
        }
        
        path.close();
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPath(path, paint);
    }
    
    private void renderShockwave(Canvas canvas, float x, float y, float size, Paint paint) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        canvas.drawCircle(x, y, size, paint);
    }
    
    private void renderCircle(Canvas canvas, float x, float y, float size, Paint paint) {
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(x, y, size, paint);
    }
    
    private int blendColors(int start, int end, float factor) {
        factor = Math.max(0, Math.min(1, factor));
        
        int sr = Color.red(start);
        int sg = Color.green(start);
        int sb = Color.blue(start);
        int sa = Color.alpha(start);
        
        int er = Color.red(end);
        int eg = Color.green(end);
        int eb = Color.blue(end);
        int ea = Color.alpha(end);
        
        int r = (int)(sr * (1 - factor) + er * factor);
        int g = (int)(sg * (1 - factor) + eg * factor);
        int b = (int)(sb * (1 - factor) + eb * factor);
        int a = (int)(sa * (1 - factor) + ea * factor);
        
        return Color.argb(a, r, g, b);
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