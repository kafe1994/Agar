package com.gaming.enhancedagar.game;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;

import com.gaming.enhancedagar.entities.Player;
import com.gaming.enhancedagar.entities.Entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Comparator;

/**
 * CameraManager - Gestor avanzado de cámara para Enhanced Agar
 * 
 * Características:
 * - Seguimiento inteligente del jugador con algoritmos predictivos
 * - Zoom dinámico adaptativo basado en tamaño del jugador y entorno
 * - Transiciones suaves con bezier curves y interpolación
 * - Sistema de múltiples cámaras para modo espector
 * - Gestión de límites de mundo con rebote y colisiones
 * - Efectos visuales avanzados (shake, blur, partículas)
 * - Optimización de rendimiento con culling y LOD
 */
public class CameraManager {
    private static final String TAG = "CameraManager";
    
    // Configuración de cámara
    private float screenWidth, screenHeight;
    private float worldWidth, worldHeight;
    
    // Estado de cámara actual
    private float cameraX, cameraY;
    private float targetCameraX, targetCameraY;
    private float zoom = 1.0f;
    private float targetZoom = 1.0f;
    
    // Movimiento y transiciones
    private float moveSpeed = 5.0f;
    private float zoomSpeed = 2.0f;
    private long lastUpdateTime;
    private boolean isTransitioning = false;
    private Transition currentTransition;
    
    // Seguimiento del jugador
    private Player currentPlayer;
    private List<Player> spectatingPlayers = new ArrayList<>();
    private int currentSpectatorIndex = 0;
    private boolean isSpectatorMode = false;
    
    // Sistema de límites y rebote
    private RectF worldBounds;
    private boolean enableBounds = true;
    private float bounceEffect = 0.3f;
    private boolean enableBounce = true;
    
    // Efectos visuales
    private List<CameraEffect> activeEffects = new ArrayList<>();
    private Map<String, Float> shakeOffsets = new HashMap<>();
    
    // Optimización y LOD
    private RectF viewBounds = new RectF();
    private float cullMargin = 100f;
    private boolean enableOptimization = true;
    
    // Interpolación suave
    private float smoothingFactor = 0.1f;
    private boolean useBezierInterpolation = false;
    
    // Historial de posiciones para predicción
    private List<PointF> positionHistory = new ArrayList<>();
    private static final int HISTORY_SIZE = 10;
    private float predictionTime = 500f; // ms
    
    // Zoom dinámico
    private float minZoom = 0.3f;
    private float maxZoom = 3.0f;
    private float basePlayerSize = 30f;
    private boolean adaptiveZoom = true;
    
    // Tipos de transición
    public enum TransitionType {
        LINEAR("Lineal"),
        EASE_IN_OUT("Aceleración"),
        BOUNCE("Rebote"),
        BEZIER("Bezier"),
        INSTANT("Instantáneo");
        
        public final String displayName;
        TransitionType(String displayName) { this.displayName = displayName; }
    }
    
    /**
     * Clase para manejar transiciones suaves
     */
    private static class Transition {
        public TransitionType type;
        public long startTime;
        public long duration;
        public float startX, startY, startZoom;
        public float endX, endY, endZoom;
        
        public Transition(TransitionType type, float startX, float startY, float startZoom,
                         float endX, float endY, float endZoom, long duration) {
            this.type = type;
            this.startX = startX;
            this.startY = startY;
            this.startZoom = startZoom;
            this.endX = endX;
            this.endY = endY;
            this.endZoom = endZoom;
            this.startTime = System.currentTimeMillis();
            this.duration = duration;
        }
        
        public boolean isComplete() {
            return System.currentTimeMillis() - startTime >= duration;
        }
        
        public float interpolate(float currentTime) {
            float progress = (currentTime - startTime) / (float) duration;
            progress = Math.max(0, Math.min(1, progress));
            
            switch (type) {
                case EASE_IN_OUT:
                    return easeInOut(progress);
                case BOUNCE:
                    return bounce(progress);
                case BEZIER:
                    return bezier(progress);
                default:
                    return progress;
            }
        }
        
        private float easeInOut(float t) {
            return t < 0.5f ? 2 * t * t : -1 + (4 - 2 * t) * t;
        }
        
        private float bounce(float t) {
            return 1 - Math.abs((float)Math.cos(t * Math.PI));
        }
        
        private float bezier(float t) {
            // Bezier cúbica simple
            return t * t * (3 - 2 * t);
        }
    }
    
    /**
     * Efectos visuales de cámara
     */
    public enum CameraEffectType {
        SHAKE("Temblor", 500),
        ZOOM_PULSE("Pulso", 1000),
        FLASH("Destello", 300),
        BLUR("Desenfoque", 2000),
        PARTICLES("Partículas", 1500);
        
        public final String displayName;
        public final long defaultDuration;
        
        CameraEffectType(String displayName, long defaultDuration) {
            this.displayName = displayName;
            this.defaultDuration = defaultDuration;
        }
    }
    
    /**
     * Clase para efectos de cámara
     */
    private static class CameraEffect {
        public CameraEffectType type;
        public long startTime;
        public long duration;
        public float intensity;
        public Map<String, Float> parameters;
        
        public CameraEffect(CameraEffectType type, long startTime, long duration, float intensity) {
            this.type = type;
            this.startTime = startTime;
            this.duration = duration;
            this.intensity = intensity;
            this.parameters = new HashMap<>();
        }
        
        public boolean isActive() {
            return System.currentTimeMillis() - startTime < duration;
        }
        
        public float getProgress() {
            long elapsed = System.currentTimeMillis() - startTime;
            return Math.max(0, Math.min(1, elapsed / (float) duration));
        }
        
        public void setParameter(String key, float value) {
            parameters.put(key, value);
        }
        
        public float getParameter(String key, float defaultValue) {
            return parameters.containsKey(key) ? parameters.get(key) : defaultValue;
        }
    }
    
    /**
     * Constructor del CameraManager
     */
    public CameraManager(float screenWidth, float screenHeight, float worldWidth, float worldHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        
        this.worldBounds = new RectF(0, 0, worldWidth, worldHeight);
        this.lastUpdateTime = System.currentTimeMillis();
        
        // Centrar cámara inicialmente
        cameraX = worldWidth / 2;
        cameraY = worldHeight / 2;
        targetCameraX = cameraX;
        targetCameraY = cameraY;
        
        Log.d(TAG, "CameraManager inicializado - Pantalla: " + screenWidth + "x" + screenHeight + 
                    ", Mundo: " + worldWidth + "x" + worldHeight);
    }
    
    /**
     * Actualiza la lógica de la cámara
     */
    public void update() {
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastUpdateTime) / 1000f;
        lastUpdateTime = currentTime;
        
        // Actualizar transiciones
        if (isTransitioning && currentTransition != null) {
            updateTransition(currentTime);
        } else {
            // Seguimiento normal del jugador
            if (currentPlayer != null) {
                updatePlayerTracking(deltaTime);
            }
        }
        
        // Actualizar zoom dinámico
        if (adaptiveZoom) {
            updateDynamicZoom(deltaTime);
        }
        
        // Aplicar límites de mundo
        if (enableBounds) {
            applyWorldBounds();
        }
        
        // Actualizar efectos
        updateEffects();
        
        // Optimización de vista
        if (enableOptimization) {
            updateViewBounds();
        }
    }
    
    /**
     * Actualiza las transiciones de cámara
     */
    private void updateTransition(long currentTime) {
        if (currentTransition.isComplete()) {
            isTransitioning = false;
            currentTransition = null;
            return;
        }
        
        float progress = currentTransition.interpolate(currentTime);
        
        // Interpolar posición y zoom
        cameraX = lerp(currentTransition.startX, currentTransition.endX, progress);
        cameraY = lerp(currentTransition.startY, currentTransition.endY, progress);
        zoom = lerp(currentTransition.startZoom, currentTransition.endZoom, progress);
    }
    
    /**
     * Actualiza el seguimiento del jugador
     */
    private void updatePlayerTracking(float deltaTime) {
        // Predecir posición del jugador
        PointF predictedPosition = predictPlayerPosition();
        
        // Ajustar objetivo de cámara
        targetCameraX = predictedPosition.x;
        targetCameraY = predictedPosition.y;
        
        // Movimiento suave hacia el objetivo
        float lerpFactor = 1 - (float)Math.pow(1 - smoothingFactor, deltaTime * 60);
        
        cameraX = lerp(cameraX, targetCameraX, lerpFactor);
        cameraY = lerp(cameraY, targetCameraY, lerpFactor);
        
        // Aplicar shake de movimiento si existe
        if (shakeOffsets.containsKey("movement")) {
            float shakeX = shakeOffsets.get("movement");
            float shakeY = shakeOffsets.get("movement");
            cameraX += shakeX;
            cameraY += shakeY;
        }
    }
    
    /**
     * Predice la posición futura del jugador
     */
    private PointF predictPlayerPosition() {
        if (positionHistory.size() < 2) {
            return new PointF(currentPlayer.getX(), currentPlayer.getY());
        }
        
        PointF last = positionHistory.get(positionHistory.size() - 1);
        PointF secondLast = positionHistory.get(positionHistory.size() - 2);
        
        // Calcular velocidad
        float velocityX = last.x - secondLast.x;
        float velocityY = last.y - secondLast.y;
        
        // Predicción simple basada en velocidad
        float predictionFactor = predictionTime / 16f; // Asumiendo 60 FPS
        float predictedX = last.x + velocityX * predictionFactor;
        float predictedY = last.y + velocityY * predictionFactor;
        
        return new PointF(predictedX, predictedY);
    }
    
    /**
     * Actualiza el zoom dinámico basado en el tamaño del jugador
     */
    private void updateDynamicZoom(float deltaTime) {
        if (currentPlayer == null) return;
        
        float playerSize = Math.max(currentPlayer.getWidth(), currentPlayer.getHeight());
        float sizeRatio = playerSize / basePlayerSize;
        
        // Calcular zoom objetivo basado en tamaño
        float zoomMultiplier = 1.0f;
        
        if (sizeRatio < 1.0f) {
            // Jugador pequeño - hacer zoom out para ver más área
            zoomMultiplier = 1.0f + (1.0f - sizeRatio) * 0.5f;
        } else if (sizeRatio > 2.0f) {
            // Jugador grande - hacer zoom in para mejor control
            zoomMultiplier = 1.0f - Math.min((sizeRatio - 2.0f) * 0.1f, 0.3f);
        }
        
        // Considerar densidad de entidades cercanas
        float densityMultiplier = calculateDensityMultiplier();
        zoomMultiplier *= densityMultiplier;
        
        // Ajustar zoom objetivo
        targetZoom = Math.max(minZoom, Math.min(maxZoom, zoomMultiplier));
        
        // Transición suave del zoom
        float zoomLerpFactor = 1 - (float)Math.pow(1 - 0.05f, deltaTime * 60);
        zoom = lerp(zoom, targetZoom, zoomLerpFactor);
    }
    
    /**
     * Calcula multiplicador de zoom basado en densidad de entidades
     */
    private float calculateDensityMultiplier() {
        // TODO: Implementar detección de densidad de entidades
        // Por ahora, retorno 1.0f (sin modificación)
        return 1.0f;
    }
    
    /**
     * Aplica límites del mundo con efecto de rebote
     */
    private void applyWorldBounds() {
        float halfWidth = screenWidth / (2 * zoom);
        float halfHeight = screenHeight / (2 * zoom);
        
        boolean bounced = false;
        
        // Límite izquierdo
        if (cameraX - halfWidth < worldBounds.left) {
            cameraX = worldBounds.left + halfWidth;
            if (enableBounce) {
                bounced = true;
                addEffect(CameraEffectType.SHAKE, 0.1f, 100);
            }
        }
        
        // Límite derecho
        if (cameraX + halfWidth > worldBounds.right) {
            cameraX = worldBounds.right - halfWidth;
            if (enableBounce) {
                bounced = true;
                addEffect(CameraEffectType.SHAKE, 0.1f, 100);
            }
        }
        
        // Límite superior
        if (cameraY - halfHeight < worldBounds.top) {
            cameraY = worldBounds.top + halfHeight;
            if (enableBounce) {
                bounced = true;
                addEffect(CameraEffectType.SHAKE, 0.1f, 100);
            }
        }
        
        // Límite inferior
        if (cameraY + halfHeight > worldBounds.bottom) {
            cameraY = worldBounds.bottom - halfHeight;
            if (enableBounce) {
                bounced = true;
                addEffect(CameraEffectType.SHAKE, 0.1f, 100);
            }
        }
        
        // Si rebotó, agregar efecto visual
        if (bounced) {
            triggerBoundsBounce();
        }
    }
    
    /**
     * Actualiza los efectos activos
     */
    private void updateEffects() {
        // Remover efectos completados
        activeEffects.removeIf(effect -> !effect.isActive());
        
        // Procesar cada efecto activo
        for (CameraEffect effect : activeEffects) {
            processEffect(effect);
        }
    }
    
    /**
     * Procesa un efecto específico
     */
    private void processEffect(CameraEffect effect) {
        float progress = effect.getProgress();
        
        switch (effect.type) {
            case SHAKE:
                updateShakeEffect(effect);
                break;
            case ZOOM_PULSE:
                updateZoomPulseEffect(effect);
                break;
            case FLASH:
                updateFlashEffect(effect);
                break;
            case BLUR:
                updateBlurEffect(effect);
                break;
            case PARTICLES:
                updateParticlesEffect(effect);
                break;
        }
    }
    
    /**
     * Actualiza el efecto de temblor
     */
    private void updateShakeEffect(CameraEffect effect) {
        float intensity = effect.intensity * (1.0f - effect.getProgress());
        float shakeX = (float)(Math.random() - 0.5) * intensity * 10;
        float shakeY = (float)(Math.random() - 0.5) * intensity * 10;
        
        shakeOffsets.put("effect", shakeX);
        shakeOffsets.put("effect", shakeY);
    }
    
    /**
     * Actualiza el efecto de pulso de zoom
     */
    private void updateZoomPulseEffect(CameraEffect effect) {
        float pulseIntensity = effect.intensity * (float)Math.sin(effect.getProgress() * Math.PI * 4);
        zoom += pulseIntensity * 0.1f;
    }
    
    /**
     * Actualiza el efecto de destello
     */
    private void updateFlashEffect(CameraEffect effect) {
        // El efecto de flash se renderiza en render()
    }
    
    /**
     * Actualiza el efecto de desenfoque
     */
    private void updateBlurEffect(CameraEffect effect) {
        // El efecto de blur se aplica en render()
    }
    
    /**
     * Actualiza el efecto de partículas
     */
    private void updateParticlesEffect(CameraEffect effect) {
        // El efecto de partículas se renderiza en render()
    }
    
    /**
     * Actualiza los límites de vista para optimización
     */
    private void updateViewBounds() {
        float halfWidth = screenWidth / (2 * zoom);
        float halfHeight = screenHeight / (2 * zoom);
        
        viewBounds.left = cameraX - halfWidth - cullMargin;
        viewBounds.top = cameraY - halfHeight - cullMargin;
        viewBounds.right = cameraX + halfWidth + cullMargin;
        viewBounds.bottom = cameraY + halfHeight + cullMargin;
    }
    
    /**
     * Renderiza los efectos de cámara
     */
    public void render(Canvas canvas) {
        canvas.save();
        
        // Aplicar transformaciones de cámara
        applyCameraTransforms(canvas);
        
        // Renderizar efectos de post-procesado
        renderPostProcessEffects(canvas);
        
        canvas.restore();
        
        // Renderizar overlay effects (efectos que no requieren transformaciones)
        renderOverlayEffects(canvas);
    }
    
    /**
     * Aplica las transformaciones de cámara al canvas
     */
    private void applyCameraTransforms(Canvas canvas) {
        // Calcular offsets de efectos activos
        float totalShakeX = 0f, totalShakeY = 0f;
        
        for (Float value : shakeOffsets.values()) {
            totalShakeX += value;
            totalShakeY += value;
        }
        
        // Calcular centro de la pantalla
        float centerX = screenWidth / 2;
        float centerY = screenHeight / 2;
        
        // Transformar coordinate system
        canvas.scale(zoom, zoom, centerX, centerY);
        canvas.translate(centerX - cameraX + totalShakeX, centerY - cameraY + totalShakeY);
    }
    
    /**
     * Renderiza efectos de post-procesado
     */
    private void renderPostProcessEffects(Canvas canvas) {
        for (CameraEffect effect : activeEffects) {
            switch (effect.type) {
                case FLASH:
                    renderFlashEffect(canvas, effect);
                    break;
                case BLUR:
                    renderBlurEffect(canvas, effect);
                    break;
                case PARTICLES:
                    renderParticlesEffect(canvas, effect);
                    break;
            }
        }
    }
    
    /**
     * Renderiza el efecto de destello
     */
    private void renderFlashEffect(Canvas canvas, CameraEffect effect) {
        float progress = effect.getProgress();
        float alpha = effect.intensity * (1.0f - progress);
        
        Paint flashPaint = new Paint();
        flashPaint.setColor(0xFFFFFFFF);
        flashPaint.setAlpha((int)(255 * alpha));
        
        canvas.drawRect(0, 0, screenWidth, screenHeight, flashPaint);
    }
    
    /**
     * Renderiza el efecto de desenfoque
     */
    private void renderBlurEffect(Canvas canvas, CameraEffect effect) {
        float progress = effect.getProgress();
        float blurIntensity = effect.intensity * (1.0f - progress) * 10f;
        
        // El blur real requiere shaders, aquí simulación básica
        Paint blurPaint = new Paint();
        blurPaint.setColor(0x40000000);
        canvas.drawRect(0, 0, screenWidth, screenHeight, blurPaint);
    }
    
    /**
     * Renderiza el efecto de partículas
     */
    private void renderParticlesEffect(Canvas canvas, CameraEffect effect) {
        // TODO: Implementar sistema de partículas
    }
    
    /**
     * Renderiza efectos de overlay
     */
    private void renderOverlayEffects(Canvas canvas) {
        // Renderizar información de cámara en modo debug
        if (isDebugMode()) {
            renderDebugInfo(canvas);
        }
    }
    
    /**
     * Renderiza información de debug
     */
    private void renderDebugInfo(Canvas canvas) {
        Paint debugPaint = new Paint();
        debugPaint.setColor(0xFF00FF00);
        debugPaint.setTextSize(20);
        
        String info = String.format("Cam: (%.1f, %.1f) Zoom: %.2f\n" +
                                   "Player: %s\n" +
                                   "Mode: %s",
                                   cameraX, cameraY, zoom,
                                   currentPlayer != null ? currentPlayer.getPlayerName() : "None",
                                   isSpectatorMode ? "Spectator" : "Player");
        
        canvas.drawText(info, 10, 30, debugPaint);
    }
    
    /**
     * Gestiona eventos de touch para controlar la cámara
     */
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                handleTouchDown(event);
                break;
            case MotionEvent.ACTION_MOVE:
                handleTouchMove(event);
                break;
            case MotionEvent.ACTION_UP:
                handleTouchUp(event);
                break;
        }
        return true;
    }
    
    /**
     * Maneja el toque hacia abajo
     */
    private void handleTouchDown(MotionEvent event) {
        if (event.getPointerCount() == 2) {
            // Inicio de pinch zoom
            startPinchZoom(event);
        }
    }
    
    /**
     * Maneja el movimiento de toque
     */
    private void handleTouchMove(MotionEvent event) {
        if (event.getPointerCount() == 2) {
            // Continuar pinch zoom
            updatePinchZoom(event);
        } else if (isSpectatorMode) {
            // En modo espector, permitir pan manual
            handleSpectatorPan(event);
        }
    }
    
    /**
     * Maneja el fin del toque
     */
    private void handleTouchUp(MotionEvent event) {
        if (event.getPointerCount() < 2) {
            // Fin de pinch zoom
            endPinchZoom();
        }
    }
    
    /**
     * Inicia el zoom por pellizco
     */
    private void startPinchZoom(MotionEvent event) {
        // TODO: Implementar pinch zoom
    }
    
    /**
     * Actualiza el zoom por pellizco
     */
    private void updatePinchZoom(MotionEvent event) {
        // TODO: Implementar pinch zoom
    }
    
    /**
     * Termina el zoom por pellizco
     */
    private void endPinchZoom() {
        // TODO: Implementar pinch zoom
    }
    
    /**
     * Maneja el pan manual en modo espector
     */
    private void handleSpectatorPan(MotionEvent event) {
        // TODO: Implementar pan manual
    }
    
    /**
     * Establece el jugador actual a seguir
     */
    public void setCurrentPlayer(Player player) {
        this.currentPlayer = player;
        this.isSpectatorMode = false;
        
        // Limpiar historial
        positionHistory.clear();
        addPositionToHistory(player.getX(), player.getY());
        
        Log.d(TAG, "Cámara siguiendo jugador: " + player.getPlayerName());
    }
    
    /**
     * Añade posición al historial para predicción
     */
    private void addPositionToHistory(float x, float y) {
        positionHistory.add(new PointF(x, y));
        
        if (positionHistory.size() > HISTORY_SIZE) {
            positionHistory.remove(0);
        }
    }
    
    /**
     * Añade un jugador a la lista de espector
     */
    public void addSpectatingPlayer(Player player) {
        if (!spectatingPlayers.contains(player)) {
            spectatingPlayers.add(player);
            Log.d(TAG, "Jugador añadido a modo espector: " + player.getPlayerName());
        }
    }
    
    /**
     * Remueve un jugador de la lista de espector
     */
    public void removeSpectatingPlayer(Player player) {
        spectatingPlayers.remove(player);
        Log.d(TAG, "Jugador removido de modo espector: " + player.getPlayerName());
    }
    
    /**
     * Cambia al siguiente jugador en modo espector
     */
    public void switchToNextSpectatingPlayer() {
        if (spectatingPlayers.isEmpty()) return;
        
        currentSpectatorIndex = (currentSpectatorIndex + 1) % spectatingPlayers.size();
        Player nextPlayer = spectatingPlayers.get(currentSpectatorIndex);
        
        // Transición suave al nuevo jugador
        transitionToPlayer(nextPlayer, TransitionType.BEZIER, 2000);
        
        Log.d(TAG, "Cambiando a jugador espector: " + nextPlayer.getPlayerName());
    }
    
    /**
     * Transición suave a un jugador
     */
    public void transitionToPlayer(Player player, TransitionType type, long duration) {
        isSpectatorMode = true;
        this.currentPlayer = player;
        
        // Calcular zoom apropiado para el jugador
        float playerSize = Math.max(player.getWidth(), player.getHeight());
        float optimalZoom = Math.max(minZoom, Math.min(maxZoom, basePlayerSize / playerSize));
        
        // Crear transición
        currentTransition = new Transition(
            type, cameraX, cameraY, zoom,
            player.getX(), player.getY(), optimalZoom,
            duration
        );
        isTransitioning = true;
    }
    
    /**
     * Añade un efecto a la cámara
     */
    public void addEffect(CameraEffectType type, float intensity, long duration) {
        CameraEffect effect = new CameraEffect(
            type, System.currentTimeMillis(), 
            duration > 0 ? duration : type.defaultDuration,
            intensity
        );
        
        activeEffects.add(effect);
        Log.d(TAG, "Añadido efecto: " + type.displayName + " (intensity: " + intensity + ")");
    }
    
    /**
     * Remueve efectos de un tipo específico
     */
    public void removeEffects(CameraEffectType type) {
        activeEffects.removeIf(effect -> effect.type == type);
    }
    
    /**
     * Trigger para rebote en límites
     */
    private void triggerBoundsBounce() {
        addEffect(CameraEffectType.SHAKE, bounceEffect, 200);
        
        // Efecto visual adicional
        addEffect(CameraEffectType.FLASH, 0.2f, 100);
    }
    
    /**
     * Función de interpolación lineal
     */
    private float lerp(float start, float end, float factor) {
        return start + (end - start) * factor;
    }
    
    /**
     * Verifica si una entidad está dentro del campo de vista
     */
    public boolean isInView(Entity entity) {
        if (!enableOptimization) return true;
        
        float entityLeft = entity.getX() - entity.getWidth() / 2;
        float entityTop = entity.getY() - entity.getHeight() / 2;
        float entityRight = entity.getX() + entity.getWidth() / 2;
        float entityBottom = entity.getY() + entity.getHeight() / 2;
        
        return RectF.intersects(viewBounds, 
                new RectF(entityLeft, entityTop, entityRight, entityBottom));
    }
    
    /**
     * Convierte coordenadas de pantalla a coordenadas del mundo
     */
    public PointF screenToWorld(float screenX, float screenY) {
        float centerX = screenWidth / 2;
        float centerY = screenHeight / 2;
        
        float worldX = cameraX + (screenX - centerX) / zoom;
        float worldY = cameraY + (screenY - centerY) / zoom;
        
        return new PointF(worldX, worldY);
    }
    
    /**
     * Convierte coordenadas del mundo a coordenadas de pantalla
     */
    public PointF worldToScreen(float worldX, float worldY) {
        float centerX = screenWidth / 2;
        float centerY = screenHeight / 2;
        
        float screenX = centerX + (worldX - cameraX) * zoom;
        float screenY = centerY + (worldY - cameraY) * zoom;
        
        return new PointF(screenX, screenY);
    }
    
    /**
     * Establece nuevos límites del mundo
     */
    public void setWorldBounds(float left, float top, float right, float bottom) {
        worldBounds.set(left, top, right, bottom);
        worldWidth = right - left;
        worldHeight = bottom - top;
    }
    
    /**
     * Fuerza un zoom específico
     */
    public void setZoom(float zoom, TransitionType type, long duration) {
        if (duration <= 0) {
            this.zoom = zoom;
            targetZoom = zoom;
        } else {
            currentTransition = new Transition(
                type, cameraX, cameraY, this.zoom,
                cameraX, cameraY, zoom, duration
            );
            isTransitioning = true;
        }
    }
    
    /**
     * Establece la posición de la cámara inmediatamente
     */
    public void setCameraPosition(float x, float y) {
        cameraX = targetCameraX = x;
        cameraY = targetCameraY = y;
        isTransitioning = false;
        currentTransition = null;
    }
    
    // Getters
    public float getCameraX() { return cameraX; }
    public float getCameraY() { return cameraY; }
    public float getZoom() { return zoom; }
    public float getTargetZoom() { return targetZoom; }
    public RectF getViewBounds() { return viewBounds; }
    public RectF getWorldBounds() { return worldBounds; }
    public boolean isInSpectatorMode() { return isSpectatorMode; }
    public Player getCurrentPlayer() { return currentPlayer; }
    public List<Player> getSpectatingPlayers() { return new ArrayList<>(spectatingPlayers); }
    
    // Setters y configuración
    public void setMoveSpeed(float speed) { this.moveSpeed = speed; }
    public void setZoomSpeed(float speed) { this.zoomSpeed = speed; }
    public void setSmoothingFactor(float factor) { this.smoothingFactor = factor; }
    public void setMinZoom(float minZoom) { this.minZoom = minZoom; }
    public void setMaxZoom(float maxZoom) { this.maxZoom = maxZoom; }
    public void setAdaptiveZoom(boolean adaptive) { this.adaptiveZoom = adaptive; }
    public void setEnableBounds(boolean enable) { this.enableBounds = enable; }
    public void setEnableBounce(boolean enable) { this.enableBounce = enable; }
    public void setEnableOptimization(boolean enable) { this.enableOptimization = enable; }
    public void setBounceEffect(float effect) { this.bounceEffect = effect; }
    
    // Debug
    private boolean debugMode = false;
    public void setDebugMode(boolean debug) { this.debugMode = debug; }
    public boolean isDebugMode() { return debugMode; }
}