package com.gaming.enhancedagar.game;

import android.graphics.PointF;
import android.graphics.RectF;
import java.util.List;
import java.util.ArrayList;

/**
 * Sistema de coordenadas avanzado para el juego Enhanced Agar
 * Maneja transformaciones entre coordenadas de pantalla y mundo,
 * sistema de cámara, zoom dinámico y optimizaciones
 */
public class CoordinateSystem {
    
    // === CONFIGURACIÓN DEL MUNDO ===
    public static final double WORLD_WIDTH = 10000.0;
    public static final double WORLD_HEIGHT = 10000.0;
    public static final double WORLD_MIN_X = -WORLD_WIDTH / 2;
    public static final double WORLD_MAX_X = WORLD_WIDTH / 2;
    public static final double WORLD_MIN_Y = -WORLD_HEIGHT / 2;
    public static final double WORLD_MAX_Y = WORLD_HEIGHT / 2;
    
    // === CONFIGURACIÓN DE CÁMARA ===
    private double cameraX = 0.0;
    private double cameraY = 0.0;
    private double targetCameraX = 0.0;
    private double targetCameraY = 0.0;
    
    // === CONFIGURACIÓN DE ZOOM ===
    private double zoom = 1.0;
    private double targetZoom = 1.0;
    private double minZoom = 0.1;
    private double maxZoom = 3.0;
    
    // === CONFIGURACIÓN DE PANTALLA ===
    private int screenWidth = 1920;
    private int screenHeight = 1080;
    private double screenCenterX = screenWidth / 2.0;
    private double screenCenterY = screenHeight / 2.0;
    
    // === CONFIGURACIÓN DE SEGUIMIENTO ===
    private boolean followPlayer = true;
    private double followLerpSpeed = 0.05; // Suavidad del seguimiento
    private double zoomLerpSpeed = 0.1; // Suavidad del zoom
    
    // === CONFIGURACIÓN DE OPTIMIZACIÓN ===
    private RectF viewBounds; // Límites visibles actuales
    private double cullingMargin = 100.0; // Margen para culling
    
    /**
     * Constructor del sistema de coordenadas
     */
    public CoordinateSystem() {
        this.viewBounds = new RectF();
        updateViewBounds();
    }
    
    /**
     * Constructor con dimensiones de pantalla
     */
    public CoordinateSystem(int screenWidth, int screenHeight) {
        this();
        setScreenSize(screenWidth, screenHeight);
    }
    
    // === TRANSFORMACIONES DE COORDENADAS ===
    
    /**
     * Convierte coordenadas del mundo a coordenadas de pantalla
     */
    public PointF worldToScreen(double worldX, double worldY) {
        double screenX = (worldX - cameraX) * zoom + screenCenterX;
        double screenY = (worldY - cameraY) * zoom + screenCenterY;
        return new PointF((float)screenX, (float)screenY);
    }
    
    /**
     * Convierte coordenadas de pantalla a coordenadas del mundo
     */
    public PointF screenToWorld(double screenX, double screenY) {
        double worldX = (screenX - screenCenterX) / zoom + cameraX;
        double worldY = (screenY - screenCenterY) / zoom + cameraY;
        return new PointF((float)worldX, (float)worldY);
    }
    
    /**
     * Convierte un tamaño del mundo a tamaño en pantalla
     */
    public double worldSizeToScreenSize(double worldSize) {
        return worldSize * zoom;
    }
    
    /**
     * Convierte un tamaño de pantalla a tamaño en el mundo
     */
    public double screenSizeToWorldSize(double screenSize) {
        return screenSize / zoom;
    }
    
    // === SISTEMA DE CÁMARA ===
    
    /**
     * Actualiza la posición de la cámara con interpolación suave
     */
    public void updateCamera() {
        if (followPlayer) {
            // Interpolación lineal suave para el seguimiento
            cameraX += (targetCameraX - cameraX) * followLerpSpeed;
            cameraY += (targetCameraY - cameraY) * followLerpSpeed;
        } else {
            cameraX = targetCameraX;
            cameraY = targetCameraY;
        }
        
        // Interpolación suave del zoom
        zoom += (targetZoom - zoom) * zoomLerpSpeed;
        
        // Aplicar límites del mundo
        cameraX = Math.max(WORLD_MIN_X, Math.min(WORLD_MAX_X, cameraX));
        cameraY = Math.max(WORLD_MIN_Y, Math.min(WORLD_MAX_Y, cameraY));
        
        updateViewBounds();
    }
    
    /**
     * Establece la posición objetivo de la cámara
     */
    public void setCameraTarget(double x, double y) {
        this.targetCameraX = x;
        this.targetCameraY = y;
    }
    
    /**
     * Establece la posición de la cámara directamente
     */
    public void setCameraPosition(double x, double y) {
        this.cameraX = x;
        this.cameraY = y;
        this.targetCameraX = x;
        this.targetCameraY = y;
        updateViewBounds();
    }
    
    // === SISTEMA DE ZOOM ===
    
    /**
     * Establece el zoom objetivo
     */
    public void setZoomTarget(double zoom) {
        this.targetZoom = Math.max(minZoom, Math.min(maxZoom, zoom));
    }
    
    /**
     * Establece el zoom directamente
     */
    public void setZoom(double zoom) {
        this.zoom = Math.max(minZoom, Math.min(maxZoom, zoom));
        this.targetZoom = this.zoom;
        updateViewBounds();
    }
    
    /**
     * Ajusta el zoom por un factor
     */
    public void zoomBy(double factor) {
        setZoomTarget(zoom * factor);
    }
    
    /**
     * Establece el zoom basado en el tamaño de un jugador
     */
    public void setZoomForPlayerSize(double playerSize) {
        // Fórmula que hace que jugadores más grandes tengan zoom menor
        double optimalZoom = Math.max(minZoom, Math.min(maxZoom, 150.0 / (playerSize + 50.0)));
        setZoomTarget(optimalZoom);
    }
    
    // === GESTIÓN DE LÍMITES DEL MUNDO ===
    
    /**
     * Verifica si un punto está dentro de los límites del mundo
     */
    public boolean isPointInWorld(double x, double y) {
        return x >= WORLD_MIN_X && x <= WORLD_MAX_X && 
               y >= WORLD_MIN_Y && y <= WORLD_MAX_Y;
    }
    
    /**
     * Limita un punto a los límites del mundo
     */
    public PointF clampToWorld(double x, double y) {
        double clampedX = Math.max(WORLD_MIN_X, Math.min(WORLD_MAX_X, x));
        double clampedY = Math.max(WORLD_MIN_Y, Math.min(WORLD_MAX_Y, y));
        return new PointF((float)clampedX, (float)clampedY);
    }
    
    /**
     * Obtiene los límites visibles actuales del mundo
     */
    public RectF getViewBounds() {
        return new RectF(viewBounds.left, viewBounds.top, 
                        viewBounds.left + (viewBounds.right - viewBounds.left), 
                        viewBounds.top + (viewBounds.bottom - viewBounds.top));
    }
    
    /**
     * Actualiza los límites de la vista
     */
    private void updateViewBounds() {
        double halfWidth = screenWidth / (2.0 * zoom);
        double halfHeight = screenHeight / (2.0 * zoom);
        
        viewBounds.left = (float)(cameraX - halfWidth);
        viewBounds.top = (float)(cameraY - halfHeight);
        viewBounds.right = (float)(cameraX + halfWidth);
        viewBounds.bottom = (float)(cameraY + halfHeight);
    }
    
    // === DETECCIÓN VISUAL ===
    
    /**
     * Verifica si un objeto es visible en la pantalla
     */
    public boolean isVisible(double worldX, double worldY, double radius) {
        RectF expandedBounds = new RectF(
            (float)(viewBounds.left - radius), (float)(viewBounds.top - radius),
            (float)(viewBounds.left + (viewBounds.right - viewBounds.left) + 2 * radius), 
            (float)(viewBounds.top + (viewBounds.bottom - viewBounds.top) + 2 * radius)
        );
        return expandedBounds.contains((float)worldX, (float)worldY);
    }
    
    /**
     * Obtiene una lista de objetos visibles con culling
     */
    public <T extends GameObject> List<T> getVisibleObjects(List<T> objects) {
        List<T> visible = new ArrayList<>();
        for (T obj : objects) {
            if (isVisible(obj.getX(), obj.getY(), obj.getRadius())) {
                visible.add(obj);
            }
        }
        return visible;
    }
    
    /**
     * Verifica si un rectángulo interseca con la vista visible
     */
    public boolean intersectsView(double x, double y, double width, double height) {
        RectF rect = new RectF((float)x, (float)y, (float)(x + width), (float)(y + height));
        return RectF.intersects(rect, viewBounds);
    }
    
    /**
     * Obtiene la distancia desde la cámara a un punto
     */
    public double getDistanceFromCamera(double worldX, double worldY) {
        double dx = worldX - cameraX;
        double dy = worldY - cameraY;
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    // === OPTIMIZACIÓN MULTI-RESOLUCIÓN ===
    
    /**
     * Establece el tamaño de la pantalla
     */
    public void setScreenSize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        this.screenCenterX = width / 2.0;
        this.screenCenterY = height / 2.0;
        updateViewBounds();
    }
    
    /**
     * Ajusta el zoom para diferentes resoluciones
     */
    public void adjustZoomForResolution(int width, int height) {
        double baseWidth = 1920.0; // Resolución de referencia
        double baseHeight = 1080.0;
        
        double scaleX = width / baseWidth;
        double scaleY = height / baseHeight;
        double scale = (scaleX + scaleY) / 2.0;
        
        setZoom(zoom * scale);
    }
    
    /**
     * Obtiene el nivel de detalle apropiado para la distancia
     */
    public int getDetailLevel(double distance) {
        if (distance < 200) return 3; // Alto detalle
        if (distance < 500) return 2; // Detalle medio
        if (distance < 1000) return 1; // Bajo detalle
        return 0; // Solo representación básica
    }
    
    // === GETTERS Y SETTERS ===
    
    public double getCameraX() { return cameraX; }
    public double getCameraY() { return cameraY; }
    public double getTargetCameraX() { return targetCameraX; }
    public double getTargetCameraY() { return targetCameraY; }
    public double getZoom() { return zoom; }
    public double getTargetZoom() { return targetZoom; }
    public double getMinZoom() { return minZoom; }
    public double getMaxZoom() { return maxZoom; }
    public int getScreenWidth() { return screenWidth; }
    public int getScreenHeight() { return screenHeight; }
    public boolean isFollowPlayer() { return followPlayer; }
    public double getFollowLerpSpeed() { return followLerpSpeed; }
    public double getZoomLerpSpeed() { return zoomLerpSpeed; }
    
    public void setFollowPlayer(boolean followPlayer) { 
        this.followPlayer = followPlayer; 
    }
    
    public void setFollowLerpSpeed(double speed) { 
        this.followLerpSpeed = Math.max(0.0, Math.min(1.0, speed)); 
    }
    
    public void setZoomLerpSpeed(double speed) { 
        this.zoomLerpSpeed = Math.max(0.0, Math.min(1.0, speed)); 
    }
    
    public void setMinZoom(double minZoom) { 
        this.minZoom = Math.max(0.01, minZoom); 
    }
    
    public void setMaxZoom(double maxZoom) { 
        this.maxZoom = Math.max(this.minZoom, maxZoom); 
    }
    
    // === INTERFAZ PARA OBJETOS DEL JUEGO ===
    
    /**
     * Interfaz para objetos que pueden ser procesados por el sistema de coordenadas
     */
    public interface GameObject {
        double getX();
        double getY();
        double getRadius();
    }
}