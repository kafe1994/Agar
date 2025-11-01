package com.gaming.enhancedagar.ui;

import android.content.Context;
import android.graphics.*;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * UIManager - Gestor unificado para todos los elementos de UI del juego Enhanced Agar
 * Maneja notificaciones, diálogos, tooltips, animaciones y estados de UI
 */
public class UIManager {
    private static final String TAG = "UIManager";
    
    // Instancia única del UIManager
    private static UIManager instance;
    
    // Contexto y recursos
    private Context context;
    private Resources resources;
    private DisplayMetrics displayMetrics;
    
    // Mapas para gestión de elementos UI
    private Map<String, UIElement> uiElements;
    private Map<String, Notification> activeNotifications;
    private Map<String, Dialog> activeDialogs;
    private Map<String, Tooltip> activeTooltips;
    
    // Sistema de animaciones
    private Map<String, Animation> animations;
    private Animation currentTransition;
    
    // Estados de UI
    private UIState currentState;
    private Paint statePaint;
    
    // Sistema de dimensiones responsivas
    private float density;
    private int screenWidth, screenHeight;
    
    // Constructor privado para patrón Singleton
    private UIManager(Context context) {
        this.context = context.getApplicationContext();
        this.resources = context.getResources();
        this.displayMetrics = resources.getDisplayMetrics();
        
        initializeDisplayMetrics();
        initializePaints();
        initializeCollections();
        initializeAnimations();
    }
    
    /**
     * Obtiene la instancia única del UIManager
     */
    public static synchronized UIManager getInstance(Context context) {
        if (instance == null) {
            instance = new UIManager(context);
        }
        return instance;
    }
    
    /**
     * Inicializa las métricas de display
     */
    private void initializeDisplayMetrics() {
        density = displayMetrics.density;
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;
        
        Log.d(TAG, "Display inicializado - Densidad: " + density + 
              ", Ancho: " + screenWidth + ", Alto: " + screenHeight);
    }
    
    /**
     * Inicializa los paints para renderizado
     */
    private void initializePaints() {
        statePaint = new Paint();
        statePaint.setAntiAlias(true);
        statePaint.setStyle(Paint.Style.FILL);
        statePaint.setTextSize(getScaledSize(16)); // Tamaño base escalable
    }
    
    /**
     * Inicializa las colecciones
     */
    private void initializeCollections() {
        uiElements = new ConcurrentHashMap<>();
        activeNotifications = new ConcurrentHashMap<>();
        activeDialogs = new ConcurrentHashMap<>();
        activeTooltips = new ConcurrentHashMap<>();
        animations = new ConcurrentHashMap<>();
    }
    
    /**
     * Inicializa las animaciones
     */
    private void initializeAnimations() {
        // Las animaciones se cargan dinámicamente según la necesidad
        Log.d(TAG, "Animaciones inicializadas");
    }
    
    // ==================== SISTEMA DE NOTIFICACIONES ====================
    
    /**
     * Muestra una notificación temporal
     */
    public void showNotification(String id, String message, NotificationType type, int duration) {
        try {
            Notification notification = new Notification(id, message, type, duration);
            activeNotifications.put(id, notification);
            
            // Programar auto-destrucción
            if (duration > 0) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    dismissNotification(id);
                }, duration);
            }
            
            Log.d(TAG, "Notificación mostrada: " + message + " (Tipo: " + type + ")");
        } catch (Exception e) {
            Log.e(TAG, "Error al mostrar notificación: " + e.getMessage());
        }
    }
    
    /**
     * Muestra notificación de éxito
     */
    public void showSuccess(String message, int duration) {
        showNotification("success_" + System.currentTimeMillis(), message, NotificationType.SUCCESS, duration);
    }
    
    /**
     * Muestra notificación de error
     */
    public void showError(String message, int duration) {
        showNotification("error_" + System.currentTimeMillis(), message, NotificationType.ERROR, duration);
    }
    
    /**
     * Muestra notificación de información
     */
    public void showInfo(String message, int duration) {
        showNotification("info_" + System.currentTimeMillis(), message, NotificationType.INFO, duration);
    }
    
    /**
     * Muestra notificación de advertencia
     */
    public void showWarning(String message, int duration) {
        showNotification("warning_" + System.currentTimeMillis(), message, NotificationType.WARNING, duration);
    }
    
    /**
     * Descarta una notificación
     */
    public void dismissNotification(String id) {
        Notification notification = activeNotifications.remove(id);
        if (notification != null) {
            Log.d(TAG, "Notificación descartada: " + id);
        }
    }
    
    /**
     * Descarta todas las notificaciones
     */
    public void dismissAllNotifications() {
        activeNotifications.clear();
        Log.d(TAG, "Todas las notificaciones descartadas");
    }
    
    // ==================== GESTIÓN DE DIÁLOGOS ====================
    
    /**
     * Muestra un diálogo simple
     */
    public void showDialog(String id, String title, String message, DialogType type) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(title)
                   .setMessage(message)
                   .setPositiveButton("Aceptar", (dialog, which) -> {
                       dismissDialog(id);
                   })
                   .setNegativeButton("Cancelar", (dialog, which) -> {
                       dismissDialog(id);
                   });
            
            AlertDialog dialog = builder.create();
            activeDialogs.put(id, new Dialog(dialog, type));
            dialog.show();
            
            Log.d(TAG, "Diálogo mostrado: " + title);
        } catch (Exception e) {
            Log.e(TAG, "Error al mostrar diálogo: " + e.getMessage());
        }
    }
    
    /**
     * Muestra diálogo personalizado
     */
    public void showCustomDialog(String id, String title, View contentView, DialogType type) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(title)
                   .setView(contentView);
            
            AlertDialog dialog = builder.create();
            activeDialogs.put(id, new Dialog(dialog, type));
            dialog.show();
            
            Log.d(TAG, "Diálogo personalizado mostrado: " + title);
        } catch (Exception e) {
            Log.e(TAG, "Error al mostrar diálogo personalizado: " + e.getMessage());
        }
    }
    
    /**
     * Descarta un diálogo
     */
    public void dismissDialog(String id) {
        Dialog dialog = activeDialogs.remove(id);
        if (dialog != null && dialog.alertDialog.isShowing()) {
            dialog.alertDialog.dismiss();
            Log.d(TAG, "Diálogo descartado: " + id);
        }
    }
    
    /**
     * Descarta todos los diálogos
     */
    public void dismissAllDialogs() {
        for (Dialog dialog : activeDialogs.values()) {
            if (dialog.alertDialog.isShowing()) {
                dialog.alertDialog.dismiss();
            }
        }
        activeDialogs.clear();
        Log.d(TAG, "Todos los diálogos descartados");
    }
    
    // ==================== SISTEMA DE TOOLTIPS ====================
    
    /**
     * Muestra un tooltip
     */
    public void showTooltip(String id, String text, float x, float y, TooltipPosition position) {
        try {
            Tooltip tooltip = new Tooltip(id, text, x, y, position);
            activeTooltips.put(id, tooltip);
            
            // Auto-ocultar después de 3 segundos
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                hideTooltip(id);
            }, 3000);
            
            Log.d(TAG, "Tooltip mostrado: " + text);
        } catch (Exception e) {
            Log.e(TAG, "Error al mostrar tooltip: " + e.getMessage());
        }
    }
    
    /**
     * Oculta un tooltip
     */
    public void hideTooltip(String id) {
        activeTooltips.remove(id);
        Log.d(TAG, "Tooltip ocultado: " + id);
    }
    
    /**
     * Oculta todos los tooltips
     */
    public void hideAllTooltips() {
        activeTooltips.clear();
        Log.d(TAG, "Todos los tooltips ocultados");
    }
    
    // ==================== ANIMACIONES Y TRANSICIONES ====================
    
    /**
     * Inicia una transición entre pantallas
     */
    public void startScreenTransition(TransitionType type, Runnable onComplete) {
        try {
            Log.d(TAG, "Iniciando transición: " + type);
            
            // Simulación de transición - en implementación real se cargaría desde recursos
            if (onComplete != null) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    onComplete.run();
                    Log.d(TAG, "Transición completada: " + type);
                }, 500); // Transición de 500ms
            }
        } catch (Exception e) {
            Log.e(TAG, "Error en transición: " + e.getMessage());
        }
    }
    
    /**
     * Anima un elemento UI
     */
    public void animateElement(String elementId, AnimationType type, int duration) {
        try {
            Log.d(TAG, "Animando elemento: " + elementId + " - Tipo: " + type);
            
            // Implementación básica de animación
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Log.d(TAG, "Animación completada para: " + elementId);
            }, duration);
        } catch (Exception e) {
            Log.e(TAG, "Error en animación: " + e.getMessage());
        }
    }
    
    // ==================== GESTIÓN DE ESTADOS ====================
    
    /**
     * Establece el estado actual de la UI
     */
    public void setUIState(UIState state) {
        this.currentState = state;
        Log.d(TAG, "Estado UI cambiado a: " + state);
    }
    
    /**
     * Obtiene el estado actual de la UI
     */
    public UIState getUIState() {
        return currentState;
    }
    
    /**
     * Muestra estado de carga
     */
    public void showLoading(String message) {
        setUIState(UIState.LOADING);
        showNotification("loading", message, NotificationType.INFO, 0); // Sin auto-ocultar
    }
    
    /**
     * Oculta estado de carga
     */
    public void hideLoading() {
        dismissNotification("loading");
        setUIState(UIState.IDLE);
    }
    
    /**
     * Muestra estado de éxito
     */
    public void showSuccessState(String message) {
        setUIState(UIState.SUCCESS);
        showSuccess(message, 2000);
    }
    
    /**
     * Muestra estado de error
     */
    public void showErrorState(String message) {
        setUIState(UIState.ERROR);
        showError(message, 3000);
    }
    
    // ==================== GESTIÓN DE ELEMENTOS UI ====================
    
    /**
     * Registra un elemento UI
     */
    public void registerUIElement(String id, UIElement element) {
        uiElements.put(id, element);
        Log.d(TAG, "Elemento UI registrado: " + id);
    }
    
    /**
     * Desregistra un elemento UI
     */
    public void unregisterUIElement(String id) {
        uiElements.remove(id);
        Log.d(TAG, "Elemento UI desregistrado: " + id);
    }
    
    /**
     * Actualiza un elemento UI
     */
    public void updateUIElement(String id, Object data) {
        UIElement element = uiElements.get(id);
        if (element != null) {
            element.update(data);
            Log.d(TAG, "Elemento UI actualizado: " + id);
        }
    }
    
    /**
     * Obtiene un elemento UI por ID
     */
    public UIElement getUIElement(String id) {
        return uiElements.get(id);
    }
    
    // ==================== COMPATIBILIDAD RESPONSIVA ====================
    
    /**
     * Obtiene tamaño escalado basado en densidad
     */
    public float getScaledSize(float baseSize) {
        return baseSize * density;
    }
    
    /**
     * Obtiene ancho de pantalla escalado
     */
    public float getScaledWidth(float percentage) {
        return (screenWidth * percentage) / 100f;
    }
    
    /**
     * Obtiene alto de pantalla escalado
     */
    public float getScaledHeight(float percentage) {
        return (screenHeight * percentage) / 100f;
    }
    
    /**
     * Verifica si es pantalla pequeña
     */
    public boolean isSmallScreen() {
        return screenWidth <= 600 || screenHeight <= 800;
    }
    
    /**
     * Verifica si es pantalla grande
     */
    public boolean isLargeScreen() {
        return screenWidth >= 1200 || screenHeight >= 1600;
    }
    
    // ==================== RENDERIZADO ====================
    
    /**
     * Renderiza todos los elementos UI
     */
    public void render(Canvas canvas) {
        if (canvas == null) return;
        
        try {
            // Renderizar notificaciones
            renderNotifications(canvas);
            
            // Renderizar tooltips
            renderTooltips(canvas);
            
            // Renderizar estado actual
            renderUIState(canvas);
            
        } catch (Exception e) {
            Log.e(TAG, "Error en renderizado UI: " + e.getMessage());
        }
    }
    
    /**
     * Renderiza las notificaciones
     */
    private void renderNotifications(Canvas canvas) {
        float yOffset = getScaledSize(50);
        
        for (Notification notification : activeNotifications.values()) {
            renderNotification(canvas, notification, yOffset);
            yOffset += getScaledSize(60);
        }
    }
    
    /**
     * Renderiza una notificación individual
     */
    private void renderNotification(Canvas canvas, Notification notification, float yOffset) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        
        // Color según tipo
        int color = getNotificationColor(notification.type);
        paint.setColor(color);
        
        float width = getScaledWidth(80); // 80% del ancho de pantalla
        float height = getScaledSize(50);
        float x = getScaledWidth(10); // Centrado con 10% de margen
        float radius = getScaledSize(8);
        
        // Dibujar fondo redondeado
        canvas.drawRoundRect(x, yOffset, x + width, yOffset + height, radius, radius, paint);
        
        // Dibujar texto
        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(getScaledSize(14));
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.LEFT);
        
        canvas.drawText(notification.message, x + getScaledSize(16), yOffset + height/2 + getScaledSize(5), textPaint);
    }
    
    /**
     * Renderiza los tooltips
     */
    private void renderTooltips(Canvas canvas) {
        for (Tooltip tooltip : activeTooltips.values()) {
            renderTooltip(canvas, tooltip);
        }
    }
    
    /**
     * Renderiza un tooltip individual
     */
    private void renderTooltip(Canvas canvas, Tooltip tooltip) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(220, 0, 0, 0)); // Negro semi-transparente
        
        float textWidth = paint.measureText(tooltip.text);
        float padding = getScaledSize(8);
        float width = textWidth + (padding * 2);
        float height = getScaledSize(30);
        
        float x = tooltip.x;
        float y = tooltip.y;
        
        // Ajustar posición según tipo
        switch (tooltip.position) {
            case TOP:
                y -= height;
                break;
            case RIGHT:
                x += getScaledSize(10);
                break;
            case LEFT:
                x -= width;
                break;
            case BOTTOM:
            default:
                y += getScaledSize(10);
                break;
        }
        
        // Dibujar fondo
        canvas.drawRoundRect(x, y, x + width, y + height, getScaledSize(4), paint);
        
        // Dibujar texto
        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(getScaledSize(12));
        textPaint.setAntiAlias(true);
        
        canvas.drawText(tooltip.text, x + padding, y + height/2 + getScaledSize(4), textPaint);
    }
    
    /**
     * Renderiza el estado actual de la UI
     */
    private void renderUIState(Canvas canvas) {
        if (currentState == null) return;
        
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        
        switch (currentState) {
            case LOADING:
                paint.setColor(Color.argb(100, 0, 0, 0)); // Overlay oscuro
                canvas.drawRect(0, 0, screenWidth, screenHeight, paint);
                
                // Dibujar spinner o indicador de carga
                drawLoadingIndicator(canvas);
                break;
                
            case ERROR:
                // Renderizar indicadores de error
                break;
                
            case SUCCESS:
                // Renderizar indicadores de éxito
                break;
                
            case IDLE:
            default:
                // No renderizado especial
                break;
        }
    }
    
    /**
     * Dibuja indicador de carga
     */
    private void drawLoadingIndicator(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(getScaledSize(4));
        paint.setAntiAlias(true);
        
        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 2f;
        float radius = getScaledSize(30);
        
        // Dibujar círculo de carga
        canvas.drawCircle(centerX, centerY, radius, paint);
        
        // Dibujar punto de progreso (simulado)
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(centerX + radius - getScaledSize(5), centerY, getScaledSize(5), paint);
    }
    
    /**
     * Obtiene el color de notificación según el tipo
     */
    private int getNotificationColor(NotificationType type) {
        switch (type) {
            case SUCCESS: return Color.argb(200, 76, 175, 80);   // Verde
            case ERROR: return Color.argb(200, 244, 67, 54);     // Rojo
            case WARNING: return Color.argb(200, 255, 193, 7);   // Amarillo
            case INFO:
            default: return Color.argb(200, 33, 150, 243);      // Azul
        }
    }
    
    // ==================== GESTIÓN DE MEMORIA ====================
    
    /**
     * Limpia recursos
     */
    public void cleanup() {
        dismissAllNotifications();
        dismissAllDialogs();
        hideAllTooltips();
        uiElements.clear();
        animations.clear();
        
        Log.d(TAG, "UIManager limpiado");
    }
    
    // ==================== CLASES INTERNAS ====================
    
    /**
     * Tipos de notificación
     */
    public enum NotificationType {
        INFO, SUCCESS, WARNING, ERROR
    }
    
    /**
     * Tipos de diálogo
     */
    public enum DialogType {
        CONFIRM, ALERT, CUSTOM, LOADING
    }
    
    /**
     * Tipos de animación
     */
    public enum AnimationType {
        FADE_IN, FADE_OUT, SLIDE_IN, SLIDE_OUT, SCALE_IN, SCALE_OUT, BOUNCE
    }
    
    /**
     * Tipos de transición
     */
    public enum TransitionType {
        FADE, SLIDE_LEFT, SLIDE_RIGHT, SLIDE_UP, SLIDE_DOWN, ZOOM_IN, ZOOM_OUT
    }
    
    /**
     * Posiciones de tooltip
     */
    public enum TooltipPosition {
        TOP, BOTTOM, LEFT, RIGHT, CENTER
    }
    
    /**
     * Estados de UI
     */
    public enum UIState {
        IDLE, LOADING, SUCCESS, ERROR, PAUSED, MENU, GAMEPLAY
    }
    
    /**
     * Clase interna para representar una notificación
     */
    private static class Notification {
        String id;
        String message;
        NotificationType type;
        long duration;
        
        Notification(String id, String message, NotificationType type, int duration) {
            this.id = id;
            this.message = message;
            this.type = type;
            this.duration = duration;
        }
    }
    
    /**
     * Clase interna para representar un diálogo
     */
    private static class Dialog {
        AlertDialog alertDialog;
        DialogType type;
        
        Dialog(AlertDialog alertDialog, DialogType type) {
            this.alertDialog = alertDialog;
            this.type = type;
        }
    }
    
    /**
     * Clase interna para representar un tooltip
     */
    private static class Tooltip {
        String id;
        String text;
        float x, y;
        TooltipPosition position;
        
        Tooltip(String id, String text, float x, float y, TooltipPosition position) {
            this.id = id;
            this.text = text;
            this.x = x;
            this.y = y;
            this.position = position;
        }
    }
    
    /**
     * Interface para elementos UI
     */
    public interface UIElement {
        void update(Object data);
        void render(Canvas canvas);
        void cleanup();
    }
}