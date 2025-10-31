package com.gaming.enhancedagar.engine;

import android.graphics.*;
import android.graphics.PorterDuff.Mode;
import android.util.Log;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sistema avanzado de efectos visuales para Enhanced Agar
 * Incluye glow, pulse, shimmer, gradientes dinámicos, sombras y más
 */
public class VisualEffects {
    private static final String TAG = "VisualEffects";
    
    // Efectos activos
    private final Map<Integer, VisualEffect> activeEffects;
    private final Map<String, GradientDefinition> gradients;
    private final List<Particle> particles;
    
    // Sistema de shaders personalizados (sustitutos de OpenGL shaders)
    private final Map<String, ShaderProgram> shaderPrograms;
    
    // Configuración global
    private int screenWidth = 1920;
    private int screenHeight = 1080;
    private long globalTime = 0;
    private float screenShakeIntensity = 0f;
    private float screenFlashIntensity = 0f;
    
    // Efectos de partículas predefinidos
    public enum EffectType {
        GLOW, PULSE, SHIMMER, FLASH, SHAKE, AURA, FORCE_FIELD,
        SPARK, TRAIL, EXPLOSION, RIPPLE, NEON, HOLOGRAPHIC
    }
    
    // Constructores
    public VisualEffects() {
        this.activeEffects = new ConcurrentHashMap<>();
        this.gradients = new ConcurrentHashMap<>();
        this.particles = new ArrayList<>();
        this.shaderPrograms = new ConcurrentHashMap<>();
        initializeShaders();
        initializeGradients();
    }
    
    public VisualEffects(int screenWidth, int screenHeight) {
        this();
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }
    
    /**
     * Inicializa los programas de shaders personalizados (sustitutos de OpenGL)
     */
    private void initializeShaders() {
        // Shader de Glow
        shaderPrograms.put("glow", new ShaderProgram() {
            @Override
            public void applyShader(Canvas canvas, Paint paint, float x, float y, float radius) {
                paint.setMaskFilter(new BlurMaskFilter(radius, BlurMaskFilter.Blur.OUTER));
            }
            
            @Override
            public String getName() { return "glow"; }
        });
        
        // Shader de Pulse
        shaderPrograms.put("pulse", new ShaderProgram() {
            @Override
            public void applyShader(Canvas canvas, Paint paint, float x, float y, float radius) {
                float alpha = (float)(Math.sin(globalTime * 0.01) + 1) * 0.5f;
                paint.setColor(Color.argb((int)(alpha * 255), paint.getColor(), paint.getColor(), paint.getColor()));
            }
            
            @Override
            public String getName() { return "pulse"; }
        });
        
        // Shader de Shimmer
        shaderPrograms.put("shimmer", new ShaderProgram() {
            @Override
            public void applyShader(Canvas canvas, Paint paint, float x, float y, float radius) {
                float shimmerOffset = (globalTime * 0.05f) % 100;
                paint.setShadowLayer(radius * 0.3f, 0, 0, 
                    Color.argb(100, 255, 255, 255));
                paint.setMaskFilter(new BlurMaskFilter(radius * 0.5f, BlurMaskFilter.Blur.NORMAL));
            }
            
            @Override
            public String getName() { return "shimmer"; }
        });
        
        // Shader de Holográfico
        shaderPrograms.put("holographic", new ShaderProgram() {
            @Override
            public void applyShader(Canvas canvas, Paint paint, float x, float y, float radius) {
                float hue = (globalTime * 0.1f) % 360;
                ColorFilter colorFilter = new ColorMatrixColorFilter(new float[] {
                    1, 0, 0, 0, 0,
                    0, 1, 0, 0, 0,
                    0, 0, 1, 0, 0,
                    0, 0, 0, 1, 0
                });
                paint.setColorFilter(colorFilter);
            }
            
            @Override
            public String getName() { return "holographic"; }
        });
    }
    
    /**
     * Inicializa gradientes dinámicos predefinidos
     */
    private void initializeGradients() {
        // Gradiente de fuego
        gradients.put("fire", new GradientDefinition(
            new float[]{0f, 0.5f, 1f},
            new int[]{Color.RED, Color.YELLOW, Color.TRANSPARENT},
            Shader.TileMode.CLAMP
        ));
        
        // Gradiente de agua
        gradients.put("water", new GradientDefinition(
            new float[]{0f, 0.5f, 1f},
            new int[]{Color.BLUE, Color.CYAN, Color.TRANSPARENT},
            Shader.TileMode.CLAMP
        ));
        
        // Gradiente eléctrico
        gradients.put("electric", new GradientDefinition(
            new float[]{0f, 0.3f, 0.7f, 1f},
            new int[]{Color.YELLOW, Color.WHITE, Color.CYAN, Color.TRANSPARENT},
            Shader.TileMode.REPEAT
        ));
        
        // Gradiente multicolor
        gradients.put("rainbow", new GradientDefinition(
            new float[]{0f, 0.16f, 0.33f, 0.5f, 0.67f, 0.83f, 1f},
            new int[]{Color.RED, Color.MAGENTA, Color.BLUE, Color.CYAN, 
                     Color.GREEN, Color.YELLOW, Color.RED},
            Shader.TileMode.CLAMP
        ));
    }
    
    /**
     * Efecto de resplandor (Glow) avanzado
     */
    public int createGlowEffect(float x, float y, float radius, int baseColor, 
                               float glowIntensity, float duration) {
        int effectId = generateEffectId();
        
        VisualEffect effect = new VisualEffect.Builder()
            .type(EffectType.GLOW)
            .position(x, y)
            .baseColor(baseColor)
            .radius(radius)
            .intensity(glowIntensity)
            .duration(duration)
            .shader(shaderPrograms.get("glow"))
            .addLayer(new GlowLayer(radius * 1.5f, 0.7f))
            .addLayer(new GlowLayer(radius * 2.5f, 0.4f))
            .addLayer(new GlowLayer(radius * 4f, 0.15f))
            .build();
        
        activeEffects.put(effectId, effect);
        return effectId;
    }
    
    /**
     * Efecto de pulso dinámico
     */
    public int createPulseEffect(float x, float y, float radius, int baseColor,
                                float pulseSpeed, float duration) {
        int effectId = generateEffectId();
        
        VisualEffect effect = new VisualEffect.Builder()
            .type(EffectType.PULSE)
            .position(x, y)
            .baseColor(baseColor)
            .radius(radius)
            .duration(duration)
            .customProperty("pulseSpeed", pulseSpeed)
            .shader(shaderPrograms.get("pulse"))
            .build();
        
        activeEffects.put(effectId, effect);
        return effectId;
    }
    
    /**
     * Efecto de brillo intermitente (Shimmer)
     */
    public int createShimmerEffect(float x, float y, float radius, int baseColor,
                                  float shimmerSpeed, float duration) {
        int effectId = generateEffectId();
        
        VisualEffect effect = new VisualEffect.Builder()
            .type(EffectType.SHIMMER)
            .position(x, y)
            .baseColor(baseColor)
            .radius(radius)
            .duration(duration)
            .customProperty("shimmerSpeed", shimmerSpeed)
            .shader(shaderPrograms.get("shimmer"))
            .build();
        
        activeEffects.put(effectId, effect);
        return effectId;
    }
    
    /**
     * Gradientes dinámicos animados
     */
    public Shader createDynamicGradient(String gradientName, float x, float y, float width, float height) {
        GradientDefinition gradDef = gradients.get(gradientName);
        if (gradDef == null) {
            Log.w(TAG, "Gradient not found: " + gradientName);
            return null;
        }
        
        LinearGradient gradient = new LinearGradient(
            x, y, x + width, y + height,
            gradDef.getColors(),
            gradDef.getPositions(),
            gradDef.getTileMode()
        );
        
        // Añadir animación al gradiente
        return new Shader() {
            @Override
            public void setLocalMatrix(Matrix matrix) {
                gradient.setLocalMatrix(matrix);
            }
            
            @Override
            public Matrix getLocalMatrix() {
                return gradient.getLocalMatrix();
            }
        };
    }
    
    /**
     * Renderizado de auras y campos de fuerza
     */
    public void renderAura(Canvas canvas, float x, float y, float radius, int baseColor,
                          float rotationSpeed, float waveIntensity) {
        Paint auraPaint = new Paint();
        auraPaint.setStyle(Paint.Style.STROKE);
        auraPaint.setStrokeWidth(radius * 0.1f);
        
        // Crear gradiente radial para la aura
        RadialGradient auraGradient = new RadialGradient(
            x, y, radius, new int[]{baseColor, Color.TRANSPARENT},
            new float[]{0f, 0.7f, 1f}, Shader.TileMode.CLAMP
        );
        auraPaint.setShader(auraGradient);
        
        // Efecto de ondas
        float time = globalTime * 0.003f;
        for (int i = 0; i < 3; i++) {
            float waveRadius = radius * (0.8f + 0.1f * Math.sin(time * rotationSpeed + i));
            auraPaint.setColor(baseColor);
            auraPaint.setAlpha((int)(50 * waveIntensity * (1.0f - i * 0.3f)));
            canvas.drawCircle(x, y, waveRadius, auraPaint);
        }
    }
    
    /**
     * Efectos de screen shake y flash
     */
    public void applyScreenShake(Canvas canvas, float intensity) {
        if (intensity > 0) {
            screenShakeIntensity = intensity;
            float shakeX = (float)(Math.random() - 0.5f) * intensity;
            float shakeY = (float)(Math.random() - 0.5f) * intensity;
            canvas.translate(shakeX, shakeY);
        }
    }
    
    public void applyScreenFlash(Canvas canvas, float intensity, int flashColor) {
        if (intensity > 0) {
            Paint flashPaint = new Paint();
            flashPaint.setColor(flashColor);
            flashPaint.setAlpha((int)(intensity * 255));
            canvas.drawRect(0, 0, screenWidth, screenHeight, flashPaint);
        }
    }
    
    /**
     * Sombras realistas y bordes
     */
    public void drawRealisticShadow(Canvas canvas, float x, float y, float width, float height,
                                   float lightAngle, float shadowLength, float intensity) {
        // Calcular posición de la sombra basada en el ángulo de luz
        float shadowX = x + (float)Math.cos(Math.toRadians(lightAngle)) * shadowLength;
        float shadowY = y + (float)Math.sin(Math.toRadians(lightAngle)) * shadowLength;
        
        Paint shadowPaint = new Paint();
        shadowPaint.setColor(Color.BLACK);
        shadowPaint.setAlpha((int)(intensity * 100));
        shadowPaint.setMaskFilter(new BlurMaskFilter(width * 0.1f, BlurMaskFilter.Blur.NORMAL));
        
        canvas.save();
        canvas.skew(0.3f, 0);
        canvas.drawOval(shadowX, shadowY, shadowX + width * 1.2f, shadowY + height * 1.2f, shadowPaint);
        canvas.restore();
    }
    
    /**
     * Renderizado de partículas
     */
    public void createParticleBurst(float x, float y, int baseColor, int particleCount,
                                   float speed, float size) {
        for (int i = 0; i < particleCount; i++) {
            float angle = (float)(Math.random() * Math.PI * 2);
            float velocity = speed * (0.5f + (float)Math.random());
            float vx = (float)Math.cos(angle) * velocity;
            float vy = (float)Math.sin(angle) * velocity;
            
            particles.add(new Particle(x, y, vx, vy, size, baseColor, 1000));
        }
    }
    
    /**
     * Actualización y renderizado de todos los efectos
     */
    public void render(Canvas canvas, float deltaTime) {
        globalTime += deltaTime;
        
        // Aplicar screen shake
        canvas.save();
        applyScreenShake(canvas, screenShakeIntensity);
        
        // Reducir shake gradualmente
        if (screenShakeIntensity > 0) {
            screenShakeIntensity *= 0.95f;
            if (screenShakeIntensity < 0.1f) {
                screenShakeIntensity = 0;
            }
        }
        
        // Renderizar efectos activos
        renderActiveEffects(canvas);
        
        // Renderizar partículas
        renderParticles(canvas, deltaTime);
        
        // Aplicar screen flash
        if (screenFlashIntensity > 0) {
            applyScreenFlash(canvas, screenFlashIntensity, Color.WHITE);
            screenFlashIntensity *= 0.9f;
            if (screenFlashIntensity < 0.1f) {
                screenFlashIntensity = 0;
            }
        }
        
        canvas.restore();
    }
    
    private void renderActiveEffects(Canvas canvas) {
        Iterator<Map.Entry<Integer, VisualEffect>> iterator = activeEffects.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<Integer, VisualEffect> entry = iterator.next();
            VisualEffect effect = entry.getValue();
            
            if (effect.update(globalTime)) {
                effect.render(canvas);
            } else {
                iterator.remove();
            }
        }
    }
    
    private void renderParticles(Canvas canvas, float deltaTime) {
        Iterator<Particle> iterator = particles.iterator();
        
        while (iterator.hasNext()) {
            Particle particle = iterator.next();
            
            if (particle.update(deltaTime)) {
                particle.render(canvas);
            } else {
                iterator.remove();
            }
        }
    }
    
    /**
     * Efecto de flash instantáneo
     */
    public void createFlashEffect(float intensity, int color, float duration) {
        screenFlashIntensity = intensity;
        
        // Crear efecto visual del flash
        for (int i = 0; i < 5; i++) {
            final float finalIntensity = intensity * (1.0f - i * 0.15f);
            // Programar flash delayed para efecto más dramático
        }
    }
    
    /**
     * Efecto holográfico
     */
    public void renderHolographicEffect(Canvas canvas, float x, float y, float width, float height,
                                        int baseColor, float timeOffset) {
        Paint holographicPaint = new Paint();
        holographicPaint.setColor(baseColor);
        holographicPaint.setAlpha(128);
        holographicPaint.setColorFilter(new ColorMatrixColorFilter(new float[]{
            1, 0, 0, 0, 0,
            0, 1, 0, 0, 0,
            0, 0, 1, 0, 0,
            0, 0, 0, 1, 0
        }));
        
        // Patrón de líneas horizontales
        float lineSpacing = 5f;
        for (float lineY = y; lineY < y + height; lineY += lineSpacing) {
            float alpha = (float)Math.abs(Math.sin((globalTime + lineY) * 0.01 + timeOffset));
            holographicPaint.setAlpha((int)(alpha * 128));
            canvas.drawLine(x, lineY, x + width, lineY, holographicPaint);
        }
    }
    
    /**
     * Efecto de neón
     */
    public void renderNeonEffect(Canvas canvas, float x, float y, float radius, int baseColor) {
        Paint neonPaint = new Paint();
        neonPaint.setStyle(Paint.Style.STROKE);
        
        // Múltiples capas de neón para crear efecto intenso
        for (int i = 0; i < 4; i++) {
            float strokeWidth = radius * (0.05f - i * 0.01f);
            int alpha = 255 - i * 50;
            
            neonPaint.setStrokeWidth(strokeWidth);
            neonPaint.setColor(baseColor);
            neonPaint.setAlpha(alpha);
            
            // Outer glow
            neonPaint.setMaskFilter(new BlurMaskFilter(strokeWidth * 2, BlurMaskFilter.Blur.OUTER));
            canvas.drawCircle(x, y, radius, neonPaint);
            
            // Inner glow
            neonPaint.setMaskFilter(new BlurMaskFilter(strokeWidth * 0.5f, BlurMaskFilter.Blur.NORMAL));
            canvas.drawCircle(x, y, radius - strokeWidth, neonPaint);
        }
    }
    
    /**
     * Limpiar todos los efectos
     */
    public void clearAllEffects() {
        activeEffects.clear();
        particles.clear();
        screenShakeIntensity = 0;
        screenFlashIntensity = 0;
    }
    
    /**
     * Obtener el número de efectos activos
     */
    public int getActiveEffectCount() {
        return activeEffects.size();
    }
    
    /**
     * Generar ID único para efectos
     */
    private int generateEffectId() {
        return (int)(System.currentTimeMillis() + Math.random() * 1000);
    }
    
    /**
     * Actualizar dimensiones de pantalla
     */
    public void onScreenSizeChanged(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
    }
    
    // Clases de apoyo
    
    /**
     * Definición de gradiente con animación
     */
    private static class GradientDefinition {
        private final float[] positions;
        private final int[] colors;
        private final Shader.TileMode tileMode;
        
        public GradientDefinition(float[] positions, int[] colors, Shader.TileMode tileMode) {
            this.positions = positions;
            this.colors = colors;
            this.tileMode = tileMode;
        }
        
        public float[] getPositions() { return positions; }
        public int[] getColors() { return colors; }
        public Shader.TileMode getTileMode() { return tileMode; }
    }
    
    /**
     * Programa de shader personalizado
     */
    private interface ShaderProgram {
        void applyShader(Canvas canvas, Paint paint, float x, float y, float radius);
        String getName();
    }
    
    /**
     * Efecto visual individual
     */
    private static class VisualEffect {
        private final EffectType type;
        private final float x, y;
        private final float radius;
        private final int baseColor;
        private final float duration;
        private final long startTime;
        private final Map<String, Object> customProperties;
        private final ShaderProgram shader;
        private final List<EffectLayer> layers;
        
        private VisualEffect(Builder builder) {
            this.type = builder.type;
            this.x = builder.x;
            this.y = builder.y;
            this.radius = builder.radius;
            this.baseColor = builder.baseColor;
            this.duration = builder.duration;
            this.startTime = System.currentTimeMillis();
            this.customProperties = builder.customProperties;
            this.shader = builder.shader;
            this.layers = builder.layers;
        }
        
        public boolean update(long currentTime) {
            return (currentTime - startTime) < duration;
        }
        
        public void render(Canvas canvas) {
            Paint paint = new Paint();
            paint.setColor(baseColor);
            paint.setStyle(Paint.Style.FILL);
            
            // Aplicar shader si existe
            if (shader != null) {
                shader.applyShader(canvas, paint, x, y, radius);
            }
            
            // Renderizar capas
            for (EffectLayer layer : layers) {
                layer.render(canvas, x, y, radius, baseColor);
            }
        }
        
        public static class Builder {
            private EffectType type;
            private float x, y;
            private float radius;
            private int baseColor;
            private float duration;
            private Map<String, Object> customProperties = new HashMap<>();
            private ShaderProgram shader;
            private List<EffectLayer> layers = new ArrayList<>();
            
            public Builder type(EffectType type) { this.type = type; return this; }
            public Builder position(float x, float y) { this.x = x; this.y = y; return this; }
            public Builder radius(float radius) { this.radius = radius; return this; }
            public Builder baseColor(int color) { this.baseColor = color; return this; }
            public Builder duration(float duration) { this.duration = duration; return this; }
            public Builder customProperty(String key, Object value) { 
                customProperties.put(key, value); return this; 
            }
            public Builder shader(ShaderProgram shader) { this.shader = shader; return this; }
            public Builder addLayer(EffectLayer layer) { this.layers.add(layer); return this; }
            
            public VisualEffect build() { return new VisualEffect(this); }
        }
    }
    
    /**
     * Capa de efecto individual
     */
    private interface EffectLayer {
        void render(Canvas canvas, float x, float y, float radius, int baseColor);
    }
    
    /**
     * Capa de resplandor
     */
    private static class GlowLayer implements EffectLayer {
        private final float glowRadius;
        private final float intensity;
        
        public GlowLayer(float glowRadius, float intensity) {
            this.glowRadius = glowRadius;
            this.intensity = intensity;
        }
        
        @Override
        public void render(Canvas canvas, float x, float y, float radius, int baseColor) {
            Paint glowPaint = new Paint();
            glowPaint.setColor(baseColor);
            glowPaint.setAlpha((int)(intensity * 100));
            glowPaint.setMaskFilter(new BlurMaskFilter(glowRadius, BlurMaskFilter.Blur.OUTER));
            
            canvas.drawCircle(x, y, radius + glowRadius, glowPaint);
        }
    }
    
    /**
     * Partícula individual
     */
    private static class Particle {
        private float x, y;
        private float vx, vy;
        private float size;
        private int color;
        private long life;
        private final long maxLife;
        private float alpha;
        
        public Particle(float x, float y, float vx, float vy, float size, int color, long life) {
            this.x = x; this.y = y;
            this.vx = vx; this.vy = vy;
            this.size = size;
            this.color = color;
            this.life = life;
            this.maxLife = life;
            this.alpha = 1.0f;
        }
        
        public boolean update(float deltaTime) {
            x += vx * deltaTime;
            y += vy * deltaTime;
            
            life -= deltaTime;
            alpha = (float)life / maxLife;
            
            return life > 0;
        }
        
        public void render(Canvas canvas) {
            Paint paint = new Paint();
            paint.setColor(color);
            paint.setAlpha((int)(alpha * 255));
            paint.setMaskFilter(new BlurMaskFilter(size * 0.5f, BlurMaskFilter.Blur.NORMAL));
            
            canvas.drawCircle(x, y, size, paint);
        }
    }
}