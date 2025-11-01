package com.gaming.enhancedagar.engine;

import android.graphics.*;
import android.util.Log;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sistema avanzado de renderizado de fondo para Enhanced Agar
 * Características:
 * - Fondo dinámico con patrón de grid o hexágonos
 * - Gradientes adaptativos según posición del jugador
 * - Nebulosas y elementos ambientales
 * - Sistema de parallax para profundidad
 * - Adaptación de color según tema del juego
 * - Optimización de renderizado (solo lo visible)
 * - Efectos de transición entre regiones
 */
public class BackgroundRenderer {
    private static final String TAG = "BackgroundRenderer";
    
    // Configuración del grid/hexágonos
    private static final float GRID_SIZE = 200f;
    private static final float HEX_SIZE = 100f;
    private static final float HEX_SIDE = HEX_SIZE / 3f;
    private static final float PARALLAX_LAYERS = 4f;
    
    // Elementos ambientales
    private List<Nebula> nebulas;
    private List<Star> stars;
    private List<ParticleEffect> environmentalEffects;
    
    // Sistema de temas
    private ThemeSystem themeSystem;
    private Map<String, Paint> themePaints;
    
    // Optimización
    private RectF visibleBounds;
    private Set<String> visibleGridCells;
    private Set<String> visibleHexCells;
    private boolean[] activeParallaxLayers;
    
    // Transiciones
    private TransitionController transitionController;
    private float currentTransitionProgress;
    
    // Patrones de fondo
    private enum PatternType {
        GRID, HEXAGONS, DOTS, LINES, WAVES
    }
    private PatternType currentPattern = PatternType.GRID;
    
    // Sistema de colores adaptativos
    private ColorAdaptiveSystem colorSystem;
    private Random colorShiftRandom;
    
    // Clase interna para las nebulosas
    private static class Nebula {
        float x, y;
        float radius;
        float opacity;
        float rotation;
        float rotationSpeed;
        Paint paint;
        int color;
        boolean active;
        
        public Nebula(float x, float y, float radius, int color) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.color = color;
            this.opacity = 0.3f;
            this.rotation = 0f;
            this.rotationSpeed = 0.5f;
            this.active = true;
            
            this.paint = new Paint();
            this.paint.setColor(color);
            this.paint.setStyle(Paint.Style.FILL);
            this.paint.setAlpha((int)(opacity * 255));
            this.paint.setMaskFilter(new BlurMaskFilter(radius * 0.3f, BlurMaskFilter.Blur.NORMAL));
        }
        
        public void update(float deltaTime) {
            rotation += rotationSpeed * deltaTime;
            opacity = 0.2f + 0.1f * (float)Math.sin(rotation * 0.02);
            paint.setAlpha((int)(opacity * 255));
        }
        
        public void draw(Canvas canvas, float offsetX, float offsetY, float parallaxFactor) {
            if (!active) return;
            
            canvas.save();
            canvas.translate(x + offsetX * parallaxFactor, y + offsetY * parallaxFactor);
            canvas.rotate(rotation * 0.1f);
            
            // Crear gradiente radial para la nebulosa
            RadialGradient gradient = new RadialGradient(
                0, 0, radius,
                color,
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            );
            
            paint.setShader(gradient);
            canvas.drawCircle(0, 0, radius, paint);
            
            canvas.restore();
        }
    }
    
    // Clase interna para las estrellas
    private static class Star {
        float x, y;
        float size;
        float brightness;
        float twinkleSpeed;
        float twinklePhase;
        boolean active;
        
        public Star(float x, float y, float size) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.brightness = 0.7f;
            this.twinkleSpeed = 2f + (float)(Math.random() * 3);
            this.twinklePhase = (float)(Math.random() * Math.PI * 2);
            this.active = true;
        }
        
        public void update(float deltaTime) {
            twinklePhase += twinkleSpeed * deltaTime;
            brightness = 0.5f + 0.5f * (float)Math.sin(twinklePhase);
        }
        
        public void draw(Canvas canvas, float offsetX, float offsetY, float parallaxFactor) {
            if (!active) return;
            
            float drawX = x + offsetX * parallaxFactor;
            float drawY = y + offsetY * parallaxFactor;
            
            Paint starPaint = new Paint();
            starPaint.setColor(Color.WHITE);
            starPaint.setAlpha((int)(brightness * 255));
            starPaint.setStyle(Paint.Style.FILL);
            
            // Efecto de brillo suave
            starPaint.setMaskFilter(new BlurMaskFilter(size * 0.5f, BlurMaskFilter.Blur.NORMAL));
            canvas.drawCircle(drawX, drawY, size, starPaint);
            
            // Punto central más brillante
            starPaint.setMaskFilter(null);
            starPaint.setAlpha(255);
            canvas.drawCircle(drawX, drawY, size * 0.3f, starPaint);
        }
    }
    
    // Clase interna para efectos de partículas ambientales
    private static class ParticleEffect {
        float x, y;
        float vx, vy;
        float life;
        float maxLife;
        float size;
        Paint paint;
        boolean active;
        
        public ParticleEffect(float x, float y, float vx, float vy, float life, int color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.life = life;
            this.maxLife = life;
            this.size = 2f + (float)(Math.random() * 3);
            this.active = true;
            
            this.paint = new Paint();
            this.paint.setColor(color);
            this.paint.setStyle(Paint.Style.FILL);
        }
        
        public boolean update(float deltaTime) {
            x += vx * deltaTime;
            y += vy * deltaTime;
            life -= deltaTime;
            
            if (life <= 0) {
                active = false;
                return false;
            }
            
            paint.setAlpha((int)((life / maxLife) * 255));
            return true;
        }
        
        public void draw(Canvas canvas) {
            if (!active) return;
            canvas.drawCircle(x, y, size, paint);
        }
    }
    
    // Sistema de colores adaptativos
    private class ColorAdaptiveSystem {
        private float playerX, playerY;
        private RectF playerBounds;
        private int[] baseColors;
        private float colorShiftIntensity;
        
        public ColorAdaptiveSystem() {
            this.baseColors = new int[]{
                Color.parseColor("#2C3E50"), // Azul oscuro
                Color.parseColor("#3498DB"), // Azul medio
                Color.parseColor("#E74C3C"), // Rojo
                Color.parseColor("#F39C12"), // Naranja
                Color.parseColor("#9B59B6"), // Púrpura
                Color.parseColor("#1ABC9C")  // Turquesa
            };
            this.colorShiftIntensity = 1.0f;
        }
        
        public void updatePlayerPosition(float x, float y, RectF bounds) {
            this.playerX = x;
            this.playerY = y;
            this.playerBounds = bounds;
        }
        
        public int getAdaptiveColor(int baseColor, float gridX, float gridY) {
            if (playerBounds == null) return baseColor;
            
            // Calcular distancia desde el jugador
            float dx = gridX - playerX;
            float dy = gridY - playerY;
            float distance = (float)Math.sqrt(dx * dx + dy * dy);
            
            // Calcular factor de influencia basado en la proximidad
            float influence = Math.max(0, 1.0f - (distance / 1000f));
            if (influence <= 0) return baseColor;
            
            // Modificar saturación y brillo basado en la distancia
            float[] hsv = new float[3];
            Color.colorToHSV(baseColor, hsv);
            
            // Intensificar colores cerca del jugador
            hsv[1] *= (1.0f + influence * 0.3f); // Aumentar saturación
            hsv[2] *= (1.0f + influence * 0.2f); // Aumentar valor/brillo
            
            // Efecto de pulso cerca del jugador
            float pulse = 1.0f + 0.1f * (float)Math.sin(System.currentTimeMillis() * 0.003);
            hsv[2] *= pulse;
            
            return Color.HSVToColor(hsv);
        }
        
        public void setColorShiftIntensity(float intensity) {
            this.colorShiftIntensity = intensity;
        }
    }
    
    // Controlador de transiciones
    private class TransitionController {
        private boolean isTransitioning;
        private float transitionProgress;
        private String currentTheme;
        private String targetTheme;
        private float transitionSpeed;
        
        public TransitionController() {
            this.isTransitioning = false;
            this.transitionProgress = 0f;
            this.currentTheme = "default";
            this.targetTheme = "default";
            this.transitionSpeed = 1.0f;
        }
        
        public void startTransition(String newTheme, float speed) {
            this.targetTheme = newTheme;
            this.transitionSpeed = speed;
            this.isTransitioning = true;
            this.transitionProgress = 0f;
        }
        
        public void update(float deltaTime) {
            if (!isTransitioning) return;
            
            transitionProgress += deltaTime * transitionSpeed;
            currentTransitionProgress = Math.min(1.0f, transitionProgress);
            
            if (transitionProgress >= 1.0f) {
                isTransitioning = false;
                currentTheme = targetTheme;
            }
        }
        
        public float getProgress() {
            return currentTransitionProgress;
        }
        
        public boolean isTransitioning() {
            return isTransitioning;
        }
        
        public String getCurrentTheme() {
            return currentTheme;
        }
    }
    
    // Sistema de temas
    private class ThemeSystem {
        private Map<String, Theme> themes;
        private String currentTheme;
        
        private static class Theme {
            int primaryColor;
            int secondaryColor;
            int accentColor;
            PatternType pattern;
            boolean hasStars;
            boolean hasNebulas;
            
            Theme(int primary, int secondary, int accent, PatternType pattern, boolean stars, boolean nebulas) {
                this.primaryColor = primary;
                this.secondaryColor = secondary;
                this.accentColor = accent;
                this.pattern = pattern;
                this.hasStars = stars;
                this.hasNebulas = nebulas;
            }
        }
        
        public ThemeSystem() {
            initializeThemes();
        }
        
        private void initializeThemes() {
            themes = new HashMap<>();
            
            // Tema espacial
            themes.put("space", new Theme(
                Color.parseColor("#0B1426"), // Azul muy oscuro
                Color.parseColor("#1E2A3A"), // Gris azulado
                Color.parseColor("#4A90E2"), // Azul brillante
                PatternType.HEXAGONS,
                true, true
            ));
            
            // Tema orgánico
            themes.put("organic", new Theme(
                Color.parseColor("#2D5016"), // Verde oscuro
                Color.parseColor("#7CB342"), // Verde medio
                Color.parseColor("#9CCC65"), // Verde claro
                PatternType.GRID,
                false, true
            ));
            
            // Tema cyberpunk
            themes.put("cyberpunk", new Theme(
                Color.parseColor("#1A0A2E"), // Púrpura muy oscuro
                Color.parseColor("#6A0572"), // Púrpura oscuro
                Color.parseColor("#FF006E"), // Rosa brillante
                PatternType.LINES,
                false, true
            ));
            
            currentTheme = "space";
        }
        
        public void setTheme(String themeName) {
            if (themes.containsKey(themeName)) {
                currentTheme = themeName;
                updateThemePaints();
            }
        }
        
        public Theme getCurrentTheme() {
            return themes.get(currentTheme);
        }
        
        public String getCurrentThemeName() {
            return currentTheme;
        }
        
        private void updateThemePaints() {
            Theme theme = themes.get(currentTheme);
            if (theme != null) {
                // Actualizar brushes y paints según el tema
                // Esto se llamaría para actualizar los colores base
            }
        }
    }
    
    // Constructor
    public BackgroundRenderer() {
        initializeComponents();
        generateEnvironmentalElements();
    }
    
    private void initializeComponents() {
        themeSystem = new ThemeSystem();
        colorSystem = new ColorAdaptiveSystem();
        transitionController = new TransitionController();
        
        nebulas = new ArrayList<>();
        stars = new ArrayList<>();
        environmentalEffects = new ArrayList<>();
        
        themePaints = new ConcurrentHashMap<>();
        visibleBounds = new RectF();
        visibleGridCells = new HashSet<>();
        visibleHexCells = new HashSet<>();
        activeParallaxLayers = new boolean[(int)PARALLAX_LAYERS];
        
        colorShiftRandom = new Random();
        
        // Inicializar capas activas
        for (int i = 0; i < PARALLAX_LAYERS; i++) {
            activeParallaxLayers[i] = true;
        }
    }
    
    private void generateEnvironmentalElements() {
        // Generar nebulosas
        for (int i = 0; i < 5; i++) {
            float x = (float)(Math.random() * 5000) - 2500;
            float y = (float)(Math.random() * 5000) - 2500;
            float radius = 300 + (float)(Math.random() * 200);
            int[] nebulaColors = {
                Color.parseColor("#FF6B6B"),
                Color.parseColor("#4ECDC4"),
                Color.parseColor("#45B7D1"),
                Color.parseColor("#96CEB4"),
                Color.parseColor("#FFEAA7"),
                Color.parseColor("#DDA0DD")
            };
            int color = nebulaColors[colorShiftRandom.nextInt(nebulaColors.length)];
            
            nebulas.add(new Nebula(x, y, radius, color));
        }
        
        // Generar estrellas
        for (int i = 0; i < 200; i++) {
            float x = (float)(Math.random() * 8000) - 4000;
            float y = (float)(Math.random() * 8000) - 4000;
            float size = 1f + (float)(Math.random() * 2f);
            stars.add(new Star(x, y, size));
        }
    }
    
    /**
     * Actualiza el estado del renderer
     */
    public void update(float deltaTime, float playerX, float playerY, RectF playerBounds) {
        // Actualizar sistema de colores adaptativos
        colorSystem.updatePlayerPosition(playerX, playerY, playerBounds);
        
        // Actualizar elementos ambientales
        for (Nebula nebula : nebulas) {
            nebula.update(deltaTime);
        }
        
        for (Star star : stars) {
            star.update(deltaTime);
        }
        
        // Actualizar efectos de partículas
        environmentalEffects.removeIf(effect -> !effect.update(deltaTime));
        
        // Actualizar controlador de transiciones
        transitionController.update(deltaTime);
        
        // Generar nuevos efectos ambientales ocasionalmente
        if (colorShiftRandom.nextFloat() < 0.01f) {
            generateEnvironmentalEffect();
        }
    }
    
    /**
     * Renderiza el fondo completo
     */
    public void render(Canvas canvas, float cameraX, float cameraY, RectF viewBounds) {
        visibleBounds.set(viewBounds);
        
        // Optimizar elementos visibles
        updateVisibleElements(cameraX, cameraY);
        
        // Calcular offset para parallax
        float parallaxOffsetX = cameraX;
        float parallaxOffsetY = cameraY;
        
        // Renderizar en capas parallax
        for (int layer = 0; layer < PARALLAX_LAYERS; layer++) {
            if (!activeParallaxLayers[layer]) continue;
            
            float parallaxFactor = 1.0f - (layer * 0.2f);
            float layerOffsetX = parallaxOffsetX * parallaxFactor;
            float layerOffsetY = parallaxOffsetY * parallaxFactor;
            
            renderParallaxLayer(canvas, layer, layerOffsetX, layerOffsetY, viewBounds);
        }
        
        // Renderizar patrón de fondo
        renderBackgroundPattern(canvas, cameraX, cameraY, viewBounds);
        
        // Renderizar gradientes adaptativos
        renderAdaptiveGradients(canvas, cameraX, cameraY, viewBounds);
    }
    
    private void updateVisibleElements(float cameraX, float cameraY) {
        // Calcular qué elementos están visibles
        float viewLeft = visibleBounds.left - 500;
        float viewTop = visibleBounds.top - 500;
        float viewRight = visibleBounds.right + 500;
        float viewBottom = visibleBounds.bottom + 500;
        
        // Actualizar nebulosas visibles
        for (Nebula nebula : nebulas) {
            nebula.active = !(nebula.x < viewLeft || nebula.x > viewRight || 
                             nebula.y < viewTop || nebula.y > viewBottom);
        }
        
        // Actualizar estrellas visibles
        for (Star star : stars) {
            star.active = !(star.x < viewLeft || star.x > viewRight || 
                           star.y < viewTop || star.y > viewBottom);
        }
        
        // Actualizar celdas de grid visibles
        visibleGridCells.clear();
        updateVisibleGridCells(viewLeft, viewTop, viewRight, viewBottom);
        
        visibleHexCells.clear();
        updateVisibleHexCells(viewLeft, viewTop, viewRight, viewBottom);
    }
    
    private void updateVisibleGridCells(float left, float top, float right, float bottom) {
        int startX = (int)Math.floor(left / GRID_SIZE) - 1;
        int endX = (int)Math.ceil(right / GRID_SIZE) + 1;
        int startY = (int)Math.floor(top / GRID_SIZE) - 1;
        int endY = (int)Math.ceil(bottom / GRID_SIZE) + 1;
        
        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                visibleGridCells.add(x + "," + y);
            }
        }
    }
    
    private void updateVisibleHexCells(float left, float top, float right, float bottom) {
        // Calcular hexágonos visibles usando sistema de coordenadas axial
        float hexWidth = HEX_SIDE * 2;
        float hexHeight = (float)(Math.sqrt(3) * HEX_SIDE);
        
        int startX = (int)Math.floor(left / hexWidth) - 2;
        int endX = (int)Math.ceil(right / hexWidth) + 2;
        int startY = (int)Math.floor(top / hexHeight) - 2;
        int endY = (int)Math.ceil(bottom / hexHeight) + 2;
        
        for (int q = startX; q <= endX; q++) {
            for (int r = startY; r <= endY; r++) {
                float[] hexCenter = hexToPixel(q, r);
                if (hexCenter[0] >= left && hexCenter[0] <= right && 
                    hexCenter[1] >= top && hexCenter[1] <= bottom) {
                    visibleHexCells.add(q + "," + r);
                }
            }
        }
    }
    
    private float[] hexToPixel(int q, int r) {
        float x = HEX_SIDE * (1.5f * q);
        float y = HEX_SIDE * ((float)Math.sqrt(3) * (r + q/2f));
        return new float[]{x, y};
    }
    
    private void renderParallaxLayer(Canvas canvas, int layer, float offsetX, float offsetY, RectF viewBounds) {
        Theme theme = themeSystem.getCurrentTheme();
        
        switch (layer) {
            case 0: // Capa de fondo básica
                renderBasicBackground(canvas, theme);
                break;
            case 1: // Capa de estrellas/nebulosas
                if (theme.hasStars) {
                    renderStars(canvas, offsetX, offsetY);
                }
                break;
            case 2: // Capa de nebulosas
                if (theme.hasNebulas) {
                    renderNebulas(canvas, offsetX, offsetY);
                }
                break;
            case 3: // Capa de efectos ambientales
                renderEnvironmentalEffects(canvas);
                break;
        }
    }
    
    private void renderBasicBackground(Canvas canvas, Theme theme) {
        // Gradiente de fondo base
        LinearGradient gradient = new LinearGradient(
            0, 0, canvas.getWidth(), canvas.getHeight(),
            theme.primaryColor,
            theme.secondaryColor,
            Shader.TileMode.CLAMP
        );
        
        Paint backgroundPaint = new Paint();
        backgroundPaint.setShader(gradient);
        backgroundPaint.setStyle(Paint.Style.FILL);
        
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), backgroundPaint);
    }
    
    private void renderStars(Canvas canvas, float offsetX, float offsetY) {
        for (Star star : stars) {
            if (star.active) {
                star.draw(canvas, offsetX, offsetY, 0.1f);
            }
        }
    }
    
    private void renderNebulas(Canvas canvas, float offsetX, float offsetY) {
        for (Nebula nebula : nebulas) {
            if (nebula.active) {
                nebula.draw(canvas, offsetX, offsetY, 0.05f);
            }
        }
    }
    
    private void renderEnvironmentalEffects(Canvas canvas) {
        for (ParticleEffect effect : environmentalEffects) {
            if (effect.active) {
                effect.draw(canvas);
            }
        }
    }
    
    private void renderBackgroundPattern(Canvas canvas, float cameraX, float cameraY, RectF viewBounds) {
        Theme theme = themeSystem.getCurrentTheme();
        
        switch (theme.pattern) {
            case GRID:
                renderGridPattern(canvas, cameraX, cameraY, theme);
                break;
            case HEXAGONS:
                renderHexPattern(canvas, cameraX, cameraY, theme);
                break;
            case DOTS:
                renderDotsPattern(canvas, cameraX, cameraY, theme);
                break;
            case LINES:
                renderLinesPattern(canvas, cameraX, cameraY, theme);
                break;
            case WAVES:
                renderWavesPattern(canvas, cameraX, cameraY, theme);
                break;
        }
    }
    
    private void renderGridPattern(Canvas canvas, float cameraX, float cameraY, Theme theme) {
        Paint gridPaint = new Paint();
        gridPaint.setColor(theme.accentColor);
        gridPaint.setAlpha(50);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f);
        
        float offsetX = -cameraX % GRID_SIZE;
        float offsetY = -cameraY % GRID_SIZE;
        
        for (String cellKey : visibleGridCells) {
            String[] coords = cellKey.split(",");
            int gridX = Integer.parseInt(coords[0]);
            int gridY = Integer.parseInt(coords[1]);
            
            float worldX = gridX * GRID_SIZE + offsetX;
            float worldY = gridY * GRID_SIZE + offsetY;
            
            // Color adaptativo basado en posición del jugador
            int adaptiveColor = colorSystem.getAdaptiveColor(theme.accentColor, 
                gridX * GRID_SIZE, gridY * GRID_SIZE);
            gridPaint.setColor(adaptiveColor);
            gridPaint.setAlpha((int)(30 + 20 * Math.sin(gridX * 0.1 + gridY * 0.1)));
            
            // Dibujar líneas del grid
            canvas.drawRect(worldX, worldY, worldX + GRID_SIZE, worldY + GRID_SIZE, gridPaint);
        }
    }
    
    private void renderHexPattern(Canvas canvas, float cameraX, float cameraY, Theme theme) {
        Paint hexPaint = new Paint();
        hexPaint.setStyle(Paint.Style.STROKE);
        hexPaint.setStrokeWidth(1f);
        
        float offsetX = -cameraX % (HEX_SIDE * 3);
        float offsetY = -cameraY % ((float)Math.sqrt(3) * HEX_SIDE);
        
        for (String cellKey : visibleHexCells) {
            String[] coords = cellKey.split(",");
            int q = Integer.parseInt(coords[0]);
            int r = Integer.parseInt(coords[1]);
            
            float[] center = hexToPixel(q, r);
            float worldX = center[0] + offsetX;
            float worldY = center[1] + offsetY;
            
            // Color adaptativo
            int adaptiveColor = colorSystem.getAdaptiveColor(theme.accentColor, center[0], center[1]);
            hexPaint.setColor(adaptiveColor);
            hexPaint.setAlpha(60);
            
            // Dibujar hexágono
            drawHexagon(canvas, worldX, worldY, HEX_SIDE, hexPaint);
        }
    }
    
    private void drawHexagon(Canvas canvas, float centerX, float centerY, float size, Paint paint) {
        Path hexPath = new Path();
        
        for (int i = 0; i < 6; i++) {
            float angle = (float)(Math.PI / 3 * i);
            float x = centerX + size * (float)Math.cos(angle);
            float y = centerY + size * (float)Math.sin(angle);
            
            if (i == 0) {
                hexPath.moveTo(x, y);
            } else {
                hexPath.lineTo(x, y);
            }
        }
        
        hexPath.close();
        canvas.drawPath(hexPath, paint);
    }
    
    private void renderDotsPattern(Canvas canvas, float cameraX, float cameraY, Theme theme) {
        Paint dotPaint = new Paint();
        dotPaint.setColor(theme.accentColor);
        dotPaint.setStyle(Paint.Style.FILL);
        
        float spacing = 50f;
        float offsetX = -cameraX % spacing;
        float offsetY = -cameraY % spacing;
        
        for (float x = offsetX; x < canvas.getWidth() + spacing; x += spacing) {
            for (float y = offsetY; y < canvas.getHeight() + spacing; y += spacing) {
                int alpha = (int)(100 + 50 * Math.sin(x * 0.01 + y * 0.01));
                dotPaint.setAlpha(alpha);
                canvas.drawCircle(x, y, 2f, dotPaint);
            }
        }
    }
    
    private void renderLinesPattern(Canvas canvas, float cameraX, float cameraY, Theme theme) {
        Paint linePaint = new Paint();
        linePaint.setColor(theme.accentColor);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2f);
        
        float spacing = 100f;
        float offsetX = -cameraX % spacing;
        float offsetY = -cameraY % spacing;
        
        // Líneas verticales
        for (float x = offsetX; x < canvas.getWidth() + spacing; x += spacing) {
            int alpha = (int)(80 + 40 * Math.sin(x * 0.02));
            linePaint.setAlpha(alpha);
            canvas.drawLine(x, 0, x, canvas.getHeight(), linePaint);
        }
        
        // Líneas horizontales
        for (float y = offsetY; y < canvas.getHeight() + spacing; y += spacing) {
            int alpha = (int)(80 + 40 * Math.sin(y * 0.02));
            linePaint.setAlpha(alpha);
            canvas.drawLine(0, y, canvas.getWidth(), y, linePaint);
        }
    }
    
    private void renderWavesPattern(Canvas canvas, float cameraX, float cameraY, Theme theme) {
        Paint wavePaint = new Paint();
        wavePaint.setColor(theme.accentColor);
        wavePaint.setStyle(Paint.Style.STROKE);
        wavePaint.setStrokeWidth(3f);
        wavePaint.setAlpha(100);
        
        float time = System.currentTimeMillis() * 0.001f;
        float amplitude = 30f;
        float frequency = 0.02f;
        
        Path wavePath = new Path();
        
        for (int i = 0; i < canvas.getWidth(); i += 10) {
            float worldX = i + cameraX;
            float waveY = canvas.getHeight() / 2 + 
                         (float)Math.sin(worldX * frequency + time) * amplitude +
                         (float)Math.sin(worldX * frequency * 2 + time * 1.5) * amplitude * 0.5f;
            
            if (i == 0) {
                wavePath.moveTo(i, waveY);
            } else {
                wavePath.lineTo(i, waveY);
            }
        }
        
        canvas.drawPath(wavePath, wavePaint);
    }
    
    private void renderAdaptiveGradients(Canvas canvas, float cameraX, float cameraY, RectF viewBounds) {
        // Gradiente radial desde el centro de la pantalla
        float centerX = canvas.getWidth() / 2f;
        float centerY = canvas.getHeight() / 2f;
        float maxRadius = (float)Math.sqrt(centerX * centerX + centerY * centerY);
        
        Theme theme = themeSystem.getCurrentTheme();
        
        RadialGradient centerGradient = new RadialGradient(
            centerX, centerY, maxRadius,
            theme.accentColor,
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        );
        
        Paint gradientPaint = new Paint();
        gradientPaint.setShader(centerGradient);
        gradientPaint.setAlpha(100);
        
        canvas.drawCircle(centerX, centerY, maxRadius, gradientPaint);
    }
    
    private void generateEnvironmentalEffect() {
        float x = visibleBounds.left + (float)(Math.random() * visibleBounds.width());
        float y = visibleBounds.top + (float)(Math.random() * visibleBounds.height());
        float vx = (float)(Math.random() - 0.5f) * 50;
        float vy = (float)(Math.random() - 0.5f) * 50;
        float life = 2f + (float)(Math.random() * 3f);
        
        int[] effectColors = {
            Color.parseColor("#FFD700"),
            Color.parseColor("#FF6347"),
            Color.parseColor("#00CED1"),
            Color.parseColor("#9370DB")
        };
        
        int color = effectColors[colorShiftRandom.nextInt(effectColors.length)];
        
        environmentalEffects.add(new ParticleEffect(x, y, vx, vy, life, color));
    }
    
    // Métodos públicos de control
    
    /**
     * Cambia el tema del fondo
     */
    public void setTheme(String themeName) {
        themeSystem.setTheme(themeName);
        transitionController.startTransition(themeName, 2.0f);
        generateEnvironmentalElements();
    }
    
    /**
     * Cambia el patrón de fondo
     */
    public void setPatternType(PatternType pattern) {
        this.currentPattern = pattern;
        Theme theme = themeSystem.getCurrentTheme();
        if (theme != null) {
            // Aquí podrías actualizar el tema para usar el nuevo patrón
        }
    }
    
    /**
     * Activa o desactiva capas parallax
     */
    public void setParallaxLayerActive(int layer, boolean active) {
        if (layer >= 0 && layer < PARALLAX_LAYERS) {
            activeParallaxLayers[layer] = active;
        }
    }
    
    /**
     * Fuerza la regeneración de elementos ambientales
     */
    public void regenerateEnvironmentalElements() {
        generateEnvironmentalElements();
    }
    
    /**
     * Obtiene el tema actual
     */
    public String getCurrentTheme() {
        return themeSystem.getCurrentThemeName();
    }
    
    /**
     * Obtiene el progreso de la transición actual
     */
    public float getTransitionProgress() {
        return transitionController.getProgress();
    }
    
    /**
     * Verifica si hay una transición en progreso
     */
    public boolean isTransitioning() {
        return transitionController.isTransitioning();
    }
    
    /**
     * Limpia recursos
     */
    public void cleanup() {
        nebulas.clear();
        stars.clear();
        environmentalEffects.clear();
        visibleGridCells.clear();
        visibleHexCells.clear();
        
        if (themePaints != null) {
            themePaints.clear();
        }
    }
}