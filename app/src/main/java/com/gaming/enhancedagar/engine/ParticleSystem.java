package com.gaming.enhancedagar.engine;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import android.graphics.*;
import android.graphics.Matrix;

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
        // Esta función se llama desde updateParticle, por lo que necesita el deltaTime
        // Pero no lo tenemos aquí, lo calcularemos basado en el tiempo de vida
        float deltaTime = 0.016f; // DeltaTime por defecto
        
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
        if (!enabled) return;
        
        // Guardar estado original del canvas
        canvas.save();
        
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
        
        // Restaurar estado del canvas
        canvas.restore();
    }
    
    private void renderParticlesByType(Canvas canvas, ParticleType type) {
        Paint paint = new Paint();
        
        for (Particle particle : particles) {
            if (particle.type != type || !particle.active) continue;
            
            // Configurar alpha blending
            float finalAlpha = particle.alpha * currentProfile.effectIntensity;
            if (finalAlpha < 0.05f) continue;
            
            // Configurar el paint con alpha
            paint.setAlpha((int) (finalAlpha * 255));
            paint.setColor(particle.color);
            
            // Usar additive blending para efectos luminosos
            if (shouldUseAdditiveBlending(particle.type)) {
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));
            } else {
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
            }
            
            renderParticle(canvas, particle, paint);
        }
    }
    
    private boolean shouldUseAdditiveBlending(ParticleType type) {
        return type == ParticleType.EXPLOSION || type == ParticleType.FIRE || 
               type == ParticleType.MAGIC || type == ParticleType.HEAL || 
               type == ParticleType.STAR || type == ParticleType.SPARK;
    }
    
    private void renderParticle(Canvas canvas, Particle particle, Paint paint) {
        canvas.save();
        
        // Aplicar rotación usando Matrix de Android
        Matrix matrix = new Matrix();
        matrix.postRotate(particle.rotation, particle.x, particle.y);
        canvas.setMatrix(matrix);
        
        float size = particle.size;
        float alpha = Math.min(1f, particle.alpha * currentProfile.effectIntensity);
        
        android.graphics.Color renderColor = blendColorsAndroid(particle.color, particle.endColor, 
                                      particle.lifetime / particle.maxLifetime);
        
        paint.setColor(renderColor);
        paint.setAlpha((int)(alpha * 255));
        
        switch (particle.type) {
            case TRAIL:
                renderTrail(canvas, paint, particle.x, particle.y, size);
                break;
            case SPARK:
                renderSpark(canvas, paint, particle.x, particle.y, size, particle.pattern);
                break;
            case SMOKE:
                renderSmoke(canvas, paint, particle.x, particle.y, size);
                break;
            case FIRE:
                renderFire(canvas, paint, particle.x, particle.y, size);
                break;
            case STAR:
                renderStar(canvas, paint, particle.x, particle.y, size, particle.pattern);
                break;
            case MAGIC:
                renderMagic(canvas, paint, particle.x, particle.y, size, particle.pattern);
                break;
            case SHOCKWAVE:
                renderShockwave(canvas, paint, particle.x, particle.y, size);
                break;
            default:
                renderCircle(canvas, paint, particle.x, particle.y, size);
                break;
        }
        
        canvas.restore();
    }
    
    private void renderTrail(Canvas canvas, Paint paint, float x, float y, float size) {
        float s = size;
        canvas.drawCircle(x, y, s, paint);
    }
    
    private void renderSpark(Canvas canvas, Paint paint, float x, float y, float size, int pattern) {
        float length = size * 3;
        float thickness = Math.max(1, size / 3);
        
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(thickness);
        paint.setStrokeCap(Paint.Cap.ROUND);
        
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
    
    private void renderSmoke(Canvas canvas, Paint paint, float x, float y, float size) {
        // Renderizar múltiples círculos para efecto de humo
        int circles = 3;
        
        Paint smokePaint = new Paint(paint);
        smokePaint.setStyle(Paint.Style.FILL);
        
        for (int i = 0; i < circles; i++) {
            float offset = i * size * 0.3f;
            float alpha = 0.3f / (i + 1);
            
            smokePaint.setColor(android.graphics.Color.argb((int)(alpha * 255), 80, 80, 80));
            canvas.drawCircle(x - offset, y - offset, size + offset, smokePaint);
        }
    }
    
    private void renderFire(Canvas canvas, Paint paint, float x, float y, float size) {
        // Efecto de llama triangular
        float baseSize = size * 2;
        
        Path path = new Path();
        path.moveTo(x, y - baseSize);
        path.lineTo(x - baseSize, y + baseSize);
        path.lineTo(x + baseSize, y + baseSize);
        path.close();
        
        // Crear gradiente
        Shader gradient = new LinearGradient(
            x, y - baseSize,
            x, y + baseSize,
            new int[]{android.graphics.Color.argb(200, 255, 255, 100), 
                     android.graphics.Color.argb(200, 255, 50, 0)},
            new float[]{0f, 1f},
            Shader.TileMode.CLAMP
        );
        
        Paint firePaint = new Paint(paint);
        firePaint.setStyle(Paint.Style.FILL);
        firePaint.setShader(gradient);
        
        canvas.drawPath(path, firePaint);
    }
    
    private void renderStar(Canvas canvas, Paint paint, float x, float y, float size, int pattern) {
        int points = 4 + (pattern % 4);
        float radius = size;
        float innerRadius = radius * 0.4f;
        
        Path path = new Path();
        
        for (int i = 0; i < points * 2; i++) {
            float angle = (float)(i * Math.PI / points);
            float r = (i % 2 == 0) ? radius : innerRadius;
            
            float px = (float)(x + r * Math.cos(angle));
            float py = (float)(y + r * Math.sin(angle));
            
            if (i == 0) {
                path.moveTo(px, py);
            } else {
                path.lineTo(px, py);
            }
        }
        
        path.close();
        
        Paint starPaint = new Paint(paint);
        starPaint.setStyle(Paint.Style.FILL);
        
        canvas.drawPath(path, starPaint);
    }
    
    private void renderMagic(Canvas canvas, Paint paint, float x, float y, float size, int pattern) {
        // Efecto de cristal/magia con formas geométricas
        int sides = 3 + (pattern % 4);
        float radius = size * 1.5f;
        
        Path path = new Path();
        
        for (int i = 0; i < sides; i++) {
            float angle = (float)(i * 2 * Math.PI / sides);
            float px = (float)(x + radius * Math.cos(angle));
            float py = (float)(y + radius * Math.sin(angle));
            
            if (i == 0) {
                path.moveTo(px, py);
            } else {
                path.lineTo(px, py);
            }
        }
        
        path.close();
        
        Paint magicPaint = new Paint(paint);
        magicPaint.setStyle(Paint.Style.FILL);
        
        canvas.drawPath(path, magicPaint);
    }
    
    private void renderShockwave(Canvas canvas, Paint paint, float x, float y, float size) {
        Paint shockPaint = new Paint(paint);
        shockPaint.setStyle(Paint.Style.STROKE);
        shockPaint.setStrokeWidth(2);
        
        canvas.drawCircle(x, y, size, shockPaint);
    }
    
    private void renderCircle(Canvas canvas, Paint paint, float x, float y, float size) {
        Paint circlePaint = new Paint(paint);
        circlePaint.setStyle(Paint.Style.FILL);
        
        canvas.drawCircle(x, y, size, circlePaint);
    }
    
    private android.graphics.Color blendColorsAndroid(android.graphics.Color start, android.graphics.Color end, float factor) {
        factor = Math.max(0, Math.min(1, factor));
        
        int r = (int)(android.graphics.Color.red(start) * (1 - factor) + android.graphics.Color.red(end) * factor);
        int g = (int)(android.graphics.Color.green(start) * (1 - factor) + android.graphics.Color.green(end) * factor);
        int b = (int)(android.graphics.Color.blue(start) * (1 - factor) + android.graphics.Color.blue(end) * factor);
        int a = (int)(android.graphics.Color.alpha(start) * (1 - factor) + android.graphics.Color.alpha(end) * factor);
        
        return android.graphics.Color.argb(a, r, g, b);
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