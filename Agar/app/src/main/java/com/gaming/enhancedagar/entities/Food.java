package com.gaming.enhancedagar.entities;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.LinearGradient;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Clase Food mejorada para el juego Enhanced Agar
 * Extiende Entity y proporciona múltiples tipos de comida con:
 * - Valores nutricionales y experiencia
 * - Efectos visuales y partículas
 * - Animaciones de spawn y despawn
 * - Sistema de respawn automático
 * - Colores y formas variadas
 */
public class Food extends Entity {
    private static final String TAG = "Food";
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    // Propiedades de animación
    private float spawnProgress = 1.0f;
    private float despawnProgress = 0.0f;
    private boolean isSpawning = false;
    private boolean isDespawning = false;
    private long spawnStartTime = 0;
    private long despawnStartTime = 0;
    private static final long SPAWN_DURATION = 800; // ms
    private static final long DESPAWN_DURATION = 600; // ms
    private static final long LIFETIME = 30000; // 30 segundos
    
    // Sistema de partículas
    private List<Particle> particles = new ArrayList<>();
    private Random random;
    
    // Efectos visuales
    private Paint glowPaint;
    private Paint borderPaint;
    private float glowIntensity = 0.0f;
    private float rotationAngle = 0.0f;
    private float pulseScale = 1.0f;
    
    // Tipos de comida disponibles con formas y colores únicos
    public enum FoodType {
        BASIC("Básica", 0xFF4CAF50, 10, 5, Shape.CIRCLE, 0.8f, 0.6f),
        PREMIUM("Premium", 0xFFFF9800, 25, 15, Shape.DIAMOND, 1.0f, 0.85f),
        RARE("Rara", 0xFF9C27B0, 50, 30, Shape.HEXAGON, 1.2f, 0.9f),
        LEGENDARY("Legendaria", 0xFFFFD700, 100, 60, Shape.STAR, 1.5f, 0.95f),
        POISONOUS("Venenosa", 0xFFF44336, -20, -10, Shape.TRIANGLE, 0.9f, 0.7f),
        MYSTICAL("Mística", 0xFF00BCD4, 75, 45, Shape.PENTAGON, 1.3f, 0.92f),
        COSMIC("Cósmica", 0xFF673AB7, 150, 90, Shape.OCTAGON, 1.8f, 0.98f);
        
        public final String displayName;
        public final int color;
        public final int nutritionValue;
        public final int experienceValue;
        public final Shape shape;
        public final float rarity; // Probabilidad de aparición
        public final float spawnProbability; // Probabilidad específica
        
        FoodType(String displayName, int color, int nutritionValue, int experienceValue, 
                 Shape shape, float rarity, float spawnProbability) {
            this.displayName = displayName;
            this.color = color;
            this.nutritionValue = nutritionValue;
            this.experienceValue = experienceValue;
            this.shape = shape;
            this.rarity = rarity;
            this.spawnProbability = spawnProbability;
        }
    }
    
    // Formas disponibles para la comida
    public enum Shape {
        CIRCLE, DIAMOND, TRIANGLE, HEXAGON, PENTAGON, STAR, OCTAGON
    }
    
    // Propiedades de la comida
    private FoodType foodType;
    private Shape shape;
    private int nutritionValue;
    private int experienceValue;
    private boolean isConsumed = false;
    private long creationTime;
    private long lastRespawnAttempt = 0;
    private static final long RESPAWN_DELAY = 5000; // 5 segundos
    
    /**
     * Constructor de comida con tipo específico
     */
    public Food(float x, float y, FoodType type) {
        super(x, y, 15, 15);
        
        this.foodType = type;
        this.shape = type.shape;
        this.nutritionValue = type.nutritionValue;
        this.experienceValue = type.experienceValue;
        this.random = new Random();
        this.isConsumed = false;
        this.creationTime = System.currentTimeMillis();
        this.spawnStartTime = System.currentTimeMillis();
        
        // Configurar propiedades visuales
        setupVisualProperties();
        
        // Iniciar animación de spawn
        startSpawnAnimation();
        
        Log.d(TAG, "Food " + type.displayName + " creada en (" + x + ", " + y + ")");
    }
    
    /**
     * Configura las propiedades visuales según el tipo
     */
    private void setupVisualProperties() {
        // Configurar color principal
        this.color = foodType.color;
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        
        // Configurar pintura de brillo
        glowPaint = new Paint();
        glowPaint.setColor(foodType.color & 0x20FFFFFF);
        glowPaint.setStyle(Paint.Style.FILL);
        glowPaint.setAntiAlias(true);
        
        // Configurar pintura de borde
        borderPaint = new Paint();
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2);
        borderPaint.setAntiAlias(true);
        
        // Ajustar tamaño según el tipo
        adjustSizeByType();
    }
    
    /**
     * Ajusta el tamaño según el tipo de comida
     */
    private void adjustSizeByType() {
        float baseSize = 15;
        float multiplier = 1.0f;
        
        switch (foodType) {
            case BASIC: multiplier = 0.8f; break;
            case PREMIUM: multiplier = 1.0f; break;
            case RARE: multiplier = 1.2f; break;
            case LEGENDARY: multiplier = 1.5f; break;
            case POISONOUS: multiplier = 0.9f; break;
            case MYSTICAL: multiplier = 1.3f; break;
            case COSMIC: multiplier = 1.8f; break;
        }
        
        this.width = baseSize * multiplier;
        this.height = baseSize * multiplier;
        updateBounds();
    }
    
    /**
     * Inicia la animación de spawn
     */
    private void startSpawnAnimation() {
        isSpawning = true;
        spawnProgress = 0.0f;
        spawnStartTime = System.currentTimeMillis();
        
        // Generar partículas de spawn
        spawnParticles();
    }
    
    /**
     * Inicia la animación de despawn
     */
    private void startDespawnAnimation() {
        isDespawning = true;
        despawnProgress = 0.0f;
        despawnStartTime = System.currentTimeMillis();
        
        // Generar partículas de despawn
        despawnParticles();
    }
    
    /**
     * Genera partículas de spawn
     */
    private void spawnParticles() {
        int particleCount = foodType == FoodType.LEGENDARY || foodType == FoodType.COSMIC ? 12 : 6;
        
        for (int i = 0; i < particleCount; i++) {
            float angle = (float) (2 * Math.PI * i / particleCount);
            float distance = width;
            
            Particle particle = new Particle(
                x + (float)Math.cos(angle) * distance,
                y + (float)Math.sin(angle) * distance,
                (float)Math.cos(angle) * 2,
                (float)Math.sin(angle) * 2,
                foodType.color,
                1000 + random.nextInt(500)
            );
            
            particles.add(particle);
        }
    }
    
    /**
     * Genera partículas de despawn
     */
    private void despawnParticles() {
        int particleCount = 8;
        
        for (int i = 0; i < particleCount; i++) {
            float angle = (float) (2 * Math.PI * i / particleCount);
            
            Particle particle = new Particle(
                x, y,
                (float)Math.cos(angle) * 3,
                (float)Math.sin(angle) * 3,
                foodType.color,
                800 + random.nextInt(400)
            );
            
            particles.add(particle);
        }
    }
    
    /**
     * Actualiza la lógica de la comida
     */
    @Override
    public void update(float deltaTime) {
        if (!isActive) {
            attemptRespawn();
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        
        // Actualizar animaciones
        updateAnimations(currentTime);
        
        // Actualizar partículas
        updateParticles(deltaTime);
        
        // Verificar tiempo de vida
        if (currentTime - creationTime > LIFETIME && !isDespawning) {
            startDespawnAnimation();
        }
        
        super.update(deltaTime);
    }
    
    /**
     * Actualiza las animaciones de spawn/despawn
     */
    private void updateAnimations(long currentTime) {
        // Animación de spawn
        if (isSpawning) {
            long elapsed = currentTime - spawnStartTime;
            spawnProgress = Math.min(1.0f, (float) elapsed / SPAWN_DURATION);
            
            if (elapsed >= SPAWN_DURATION) {
                isSpawning = false;
                spawnProgress = 1.0f;
            }
        }
        
        // Animación de despawn
        if (isDespawning) {
            long elapsed = currentTime - despawnStartTime;
            despawnProgress = Math.min(1.0f, (float) elapsed / DESPAWN_DURATION);
            
            if (elapsed >= DESPAWN_DURATION) {
                isDespawning = false;
                despawnProgress = 0.0f;
                isActive = false;
                isConsumed = true;
            }
        }
        
        // Actualizar efectos visuales
        updateVisualEffects(currentTime);
    }
    
    /**
     * Actualiza los efectos visuales
     */
    private void updateVisualEffects(long currentTime) {
        // Rotación para formas especiales
        if (shape != Shape.CIRCLE) {
            rotationAngle += 0.02f;
        }
        
        // Pulso para comida especial
        if (foodType == FoodType.LEGENDARY || foodType == FoodType.COSMIC || foodType == FoodType.MYSTICAL) {
            pulseScale = 1.0f + 0.1f * (float)Math.sin(currentTime * 0.005);
        }
        
        // Intensidad de brillo
        glowIntensity = (float)Math.abs(Math.sin(currentTime * 0.01));
        
        // Actualizar colores con gradiente
        updateGradientColors();
    }
    
    /**
     * Actualiza los colores con gradiente
     */
    private void updateGradientColors() {
        // Crear gradiente dinámico
        int alpha = (int)(255 * spawnProgress * (1 - despawnProgress));
        int glowAlpha = (int)(50 * glowIntensity * spawnProgress * (1 - despawnProgress));
        
        paint.setAlpha(alpha);
        glowPaint.setAlpha(glowAlpha);
        
        // Color del borde según el tipo
        switch (foodType) {
            case LEGENDARY:
                borderPaint.setColor(0xFFFFD700);
                break;
            case RARE:
                borderPaint.setColor(0xFFFFFFFF);
                break;
            case COSMIC:
                borderPaint.setColor(0xFF00FFFF);
                break;
            default:
                borderPaint.setColor(0xFF333333);
        }
        borderPaint.setAlpha(alpha);
    }
    
    /**
     * Actualiza las partículas
     */
    private void updateParticles(float deltaTime) {
        particles.removeIf(particle -> {
            particle.update(deltaTime);
            return !particle.isAlive();
        });
    }
    
    /**
     * Intenta hacer respawn de la comida
     */
    private void attemptRespawn() {
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastRespawnAttempt < RESPAWN_DELAY) {
            return;
        }
        
        // Verificar probabilidad de respawn basada en rareza
        if (random.nextFloat() < foodType.rarity) {
            respawn();
        }
        
        lastRespawnAttempt = currentTime;
    }
    
    /**
     * Hace respawn de la comida en una nueva posición
     */
    private void respawn() {
        // Nueva posición aleatoria (sería mejor tener acceso al GameEngine)
        float newX = random.nextInt(800) + 50;
        float newY = random.nextInt(1200) + 50;
        
        setPosition(newX, newY);
        isActive = true;
        isConsumed = false;
        creationTime = System.currentTimeMillis();
        
        startSpawnAnimation();
        
        Log.d(TAG, "Food " + foodType.displayName + " ha hecho respawn en (" + newX + ", " + newY + ")");
    }
    
    /**
     * Renderiza la comida con efectos visuales completos
     */
    @Override
    public void draw(Canvas canvas) {
        if (!isVisible || (spawnProgress <= 0 && !isSpawning)) return;
        
        canvas.save();
        
        // Aplicar transformaciones
        float currentScale = spawnProgress * (1 - despawnProgress) * pulseScale;
        canvas.scale(currentScale, currentScale, x, y);
        
        // Renderizar partículas
        for (Particle particle : particles) {
            particle.draw(canvas);
        }
        
        // Renderizar brillo
        if (glowIntensity > 0 && (foodType == FoodType.PREMIUM || foodType == FoodType.LEGENDARY || 
                                  foodType == FoodType.COSMIC || foodType == FoodType.MYSTICAL)) {
            float glowSize = (Math.max(width, height) / 2) + 5 + 3 * glowIntensity;
            canvas.drawCircle(x, y, glowSize, glowPaint);
        }
        
        // Renderizar forma principal
        drawShape(canvas);
        
        // Renderizar borde
        drawBorder(canvas);
        
        // Renderizar efectos especiales
        drawSpecialEffects(canvas);
        
        canvas.restore();
    }
    
    /**
     * Dibuja la forma específica de la comida
     */
    private void drawShape(Canvas canvas) {
        Path path = new Path();
        
        switch (shape) {
            case CIRCLE:
                float radius = Math.max(width, height) / 2;
                canvas.drawCircle(x, y, radius * spawnProgress, paint);
                break;
                
            case DIAMOND:
                drawDiamond(path, x, y, width/2);
                canvas.drawPath(path, paint);
                break;
                
            case TRIANGLE:
                drawTriangle(path, x, y, width/2);
                canvas.drawPath(path, paint);
                break;
                
            case HEXAGON:
                drawPolygon(path, x, y, width/2, 6);
                canvas.drawPath(path, paint);
                break;
                
            case PENTAGON:
                drawPolygon(path, x, y, width/2, 5);
                canvas.drawPath(path, paint);
                break;
                
            case OCTAGON:
                drawPolygon(path, x, y, width/2, 8);
                canvas.drawPath(path, paint);
                break;
                
            case STAR:
                drawStar(path, x, y, width/2);
                canvas.drawPath(path, paint);
                break;
        }
    }
    
    /**
     * Dibuja el borde de la comida
     */
    private void drawBorder(Canvas canvas) {
        if (foodType == FoodType.LEGENDARY || foodType == FoodType.RARE || foodType == FoodType.COSMIC) {
            canvas.drawPath(getShapePath(), borderPaint);
        }
    }
    
    /**
     * Dibuja efectos especiales
     */
    private void drawSpecialEffects(Canvas canvas) {
        switch (foodType) {
            case LEGENDARY:
                drawGlowEffect(canvas, 0xFFFFD700);
                break;
            case COSMIC:
                drawCosmicEffect(canvas);
                break;
            case MYSTICAL:
                drawMysticalEffect(canvas);
                break;
            case POISONOUS:
                drawPoisonEffect(canvas);
                break;
        }
    }
    
    /**
     * Dibuja forma de diamante
     */
    private void drawDiamond(Path path, float centerX, float centerY, float radius) {
        path.moveTo(centerX, centerY - radius);
        path.lineTo(centerX + radius, centerY);
        path.lineTo(centerX, centerY + radius);
        path.lineTo(centerX - radius, centerY);
        path.close();
    }
    
    /**
     * Dibuja forma de triángulo
     */
    private void drawTriangle(Path path, float centerX, float centerY, float radius) {
        for (int i = 0; i < 3; i++) {
            float angle = (float) (-Math.PI / 2 + i * 2 * Math.PI / 3);
            float x = centerX + radius * (float)Math.cos(angle);
            float y = centerY + radius * (float)Math.sin(angle);
            
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.close();
    }
    
    /**
     * Dibuja polígono regular
     */
    private void drawPolygon(Path path, float centerX, float centerY, float radius, int sides) {
        for (int i = 0; i < sides; i++) {
            float angle = (float) (2 * Math.PI * i / sides - Math.PI / 2);
            float x = centerX + radius * (float)Math.cos(angle);
            float y = centerY + radius * (float)Math.sin(angle);
            
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.close();
    }
    
    /**
     * Dibuja forma de estrella
     */
    private void drawStar(Path path, float centerX, float centerY, float radius) {
        int points = 5;
        float innerRadius = radius * 0.4f;
        
        for (int i = 0; i < points * 2; i++) {
            float r = (i % 2 == 0) ? radius : innerRadius;
            float angle = (float) (Math.PI * i / points - Math.PI / 2);
            float x = centerX + r * (float)Math.cos(angle);
            float y = centerY + r * (float)Math.sin(angle);
            
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.close();
    }
    
    /**
     * Obtiene el Path de la forma actual
     */
    private Path getShapePath() {
        Path path = new Path();
        
        switch (shape) {
            case CIRCLE:
                path.addCircle(x, y, Math.max(width, height) / 2, Path.Direction.CW);
                break;
            case DIAMOND:
                drawDiamond(path, x, y, width/2);
                break;
            case TRIANGLE:
                drawTriangle(path, x, y, width/2);
                break;
            case HEXAGON:
                drawPolygon(path, x, y, width/2, 6);
                break;
            case PENTAGON:
                drawPolygon(path, x, y, width/2, 5);
                break;
            case OCTAGON:
                drawPolygon(path, x, y, width/2, 8);
                break;
            case STAR:
                drawStar(path, x, y, width/2);
                break;
        }
        
        return path;
    }
    
    /**
     * Dibuja efecto de brillo dorado (Legendary)
     */
    private void drawGlowEffect(Canvas canvas, int glowColor) {
        Paint glowPaint = new Paint();
        glowPaint.setColor(glowColor & 0x20FFFFFF);
        glowPaint.setStyle(Paint.Style.FILL);
        
        float glowSize = Math.max(width, height) / 2 + 8 + 4 * glowIntensity;
        canvas.drawCircle(x, y, glowSize, glowPaint);
    }
    
    /**
     * Dibuja efecto cósmico (Cosmic)
     */
    private void drawCosmicEffect(Canvas canvas) {
        // Múltiples anillos concéntricos
        for (int i = 0; i < 3; i++) {
            Paint ringPaint = new Paint();
            ringPaint.setColor(0xFF00FFFF);
            ringPaint.setStyle(Paint.Style.STROKE);
            ringPaint.setStrokeWidth(1);
            ringPaint.setAlpha(100 - i * 30);
            
            float radius = Math.max(width, height) / 2 + i * 4 + glowIntensity * 2;
            canvas.drawCircle(x, y, radius, ringPaint);
        }
    }
    
    /**
     * Dibuja efecto místico (Mystical)
     */
    private void drawMysticalEffect(Canvas canvas) {
        Paint mysticalPaint = new Paint();
        mysticalPaint.setColor(0xFF00BCD4);
        mysticalPaint.setStyle(Paint.Style.STROKE);
        mysticalPaint.setStrokeWidth(3);
        mysticalPaint.setAlpha(150);
        
        // Líneas radiales
        int rays = 8;
        for (int i = 0; i < rays; i++) {
            float angle = (float) (2 * Math.PI * i / rays + rotationAngle);
            float endX = x + (float)Math.cos(angle) * (width/2 + 10);
            float endY = y + (float)Math.sin(angle) * (width/2 + 10);
            
            canvas.drawLine(x, y, endX, endY, mysticalPaint);
        }
    }
    
    /**
     * Dibuja efecto venenoso (Poisonous)
     */
    private void drawPoisonEffect(Canvas canvas) {
        // Efecto de pulsación roja
        Paint poisonPaint = new Paint();
        poisonPaint.setColor(0xFFF44336);
        poisonPaint.setStyle(Paint.Style.STROKE);
        poisonPaint.setStrokeWidth(2);
        poisonPaint.setAlpha(100 + (int)(100 * glowIntensity));
        
        canvas.drawCircle(x, y, width/2 + 2, poisonPaint);
    }
    
    /**
     * Consume la comida y inicia animación de despawn
     */
    public void consume() {
        if (isConsumed || isDespawning) return;
        
        startDespawnAnimation();
        
        Log.d(TAG, "Food " + foodType.displayName + " consumida");
    }
    
    /**
     * Genera comida aleatoria con distribución de probabilidad
     */
    public static Food generateRandom(float x, float y) {
        Random random = new Random();
        double randomValue = random.nextDouble();
        
        FoodType type;
        
        if (randomValue < 0.50) {
            type = FoodType.BASIC; // 50%
        } else if (randomValue < 0.75) {
            type = FoodType.PREMIUM; // 25%
        } else if (randomValue < 0.90) {
            type = FoodType.RARE; // 15%
        } else if (randomValue < 0.97) {
            type = FoodType.MYSTICAL; // 7%
        } else if (randomValue < 0.995) {
            type = FoodType.LEGENDARY; // 2.5%
        } else if (randomValue < 0.999) {
            type = FoodType.COSMIC; // 0.4%
        } else {
            type = FoodType.POISONOUS; // 0.1%
        }
        
        return new Food(x, y, type);
    }
    
    /**
     * Genera comida aleatoria en el área de juego
     */
    public static Food generateRandom(int gameWidth, int gameHeight) {
        Random random = new Random();
        float x = random.nextInt(gameWidth - 60) + 30;
        float y = random.nextInt(gameHeight - 60) + 30;
        
        return generateRandom(x, y);
    }
    
    // Getters
    public int getNutritionValue() { return nutritionValue; }
    public int getExperienceValue() { return experienceValue; }
    public FoodType getFoodType() { return foodType; }
    public Shape getShape() { return shape; }
    public boolean isPoisonous() { return foodType == FoodType.POISONOUS; }
    public boolean isAvailable() { return isActive && !isConsumed && spawnProgress >= 1.0f; }
    public float getSpawnProgress() { return spawnProgress; }
    public float getDespawnProgress() { return despawnProgress; }
    
    /**
     * Clase interna para manejar partículas
     */
    private class Particle {
        private float x, y;
        private float velX, velY;
        private int color;
        private long startTime;
        private long lifetime;
        private Paint paint;
        private float alpha;
        
        public Particle(float x, float y, float velX, float velY, int color, long lifetime) {
            this.x = x;
            this.y = y;
            this.velX = velX;
            this.velY = velY;
            this.color = color;
            this.startTime = System.currentTimeMillis();
            this.lifetime = lifetime;
            this.paint = new Paint();
            this.paint.setColor(color);
            this.paint.setStyle(Paint.Style.FILL);
            this.alpha = 1.0f;
        }
        
        public void update(float deltaTime) {
            x += velX * deltaTime;
            y += velY * deltaTime;
            
            // Actualizar alpha basado en tiempo de vida
            long elapsed = System.currentTimeMillis() - startTime;
            alpha = Math.max(0, 1.0f - (float)elapsed / lifetime);
            paint.setAlpha((int)(255 * alpha));
            
            // Reducir velocidad con fricción
            velX *= 0.98f;
            velY *= 0.98f;
        }
        
        public void draw(Canvas canvas) {
            if (alpha > 0) {
                canvas.drawCircle(x, y, 3 * alpha, paint);
            }
        }
        
        public boolean isAlive() {
            return System.currentTimeMillis() - startTime < lifetime;
        }
    }
}