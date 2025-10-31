package com.gaming.enhancedagar.game;

import com.gaming.enhancedagar.engine.GameEngine;
import com.gaming.enhancedagar.utils.Vector2D;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.List;

/**
 * GameView - Vista principal del juego Enhanced Agar
 * 
 * Características principales:
 * - Extiende SurfaceView e implementa SurfaceHolder.Callback
 * - Manejo completo del ciclo de vida de la superficie
 * - Integración con GameEngine para lógica del juego
 * - Sistema de callbacks para eventos
 * - Optimizado para Termux y Android
 * - Renderizado con Canvas optimizado
 * - Control de FPS y performance
 * 
 * @author Enhanced Agar Team
 * @version 1.0
 */
public class GameView extends SurfaceView implements SurfaceHolder.Callback {
    
    private static final String TAG = "GameView";
    
    // Surface Management
    private SurfaceHolder surfaceHolder;
    private boolean surfaceCreated = false;
    private boolean surfaceDestroyed = false;
    private Object surfaceLock = new Object();
    
    // Game Engine
    private GameEngine gameEngine;
    private boolean engineInitialized = false;
    
    // Rendering Thread
    private RenderThread renderThread;
    private boolean isRendering = false;
    
    // Performance Monitoring
    private long lastFrameTime = 0;
    private float currentFPS = 0.0f;
    private int frameCount = 0;
    private long fpsTimer = 0;
    private static final long FPS_UPDATE_INTERVAL = 1000; // 1 segundo
    
    // Optimization for Termux
    private boolean lowMemoryMode = false;
    private int targetFPS = 60;
    private boolean adaptiveFPS = true;
    
    // Callbacks System
    private List<GameViewCallback> callbacks = new ArrayList<>();
    
    // Drawing Resources
    private Paint backgroundPaint;
    private Paint fpsPaint;
    private Paint textPaint;
    
    // Dimensions
    private int viewWidth = 0;
    private int viewHeight = 0;
    
    // Touch Handling
    private float lastTouchX = 0;
    private float lastTouchY = 0;
    private boolean isTouching = false;
    
    /**
     * Constructor para contexto
     */
    public GameView(Context context) {
        super(context);
        initialize(context);
    }
    
    /**
     * Constructor para contexto con atributos
     */
    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }
    
    /**
     * Constructor completo con atributos y estilo
     */
    public GameView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }
    
    /**
     * Inicialización común para todos los constructores
     */
    private void initialize(Context context) {
        Log.d(TAG, "Inicializando GameView...");
        
        // Configurar SurfaceHolder
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        
        // Inicializar GameEngine
        gameEngine = new GameEngine();
        
        // Configurar optimizaciones para Termux
        setupTermuxOptimizations();
        
        // Inicializar recursos de dibujo
        initializeDrawingResources();
        
        // Configurar callbacks por defecto
        setupDefaultCallbacks();
        
        Log.d(TAG, "GameView inicializado correctamente");
    }
    
    /**
     * Configura optimizaciones específicas para Termux
     */
    private void setupTermuxOptimizations() {
        // Detectar si estamos en Termux
        String termuxProperty = System.getProperty("user.home");
        boolean isTermux = termuxProperty != null && termuxProperty.contains("com.termux");
        
        if (isTermux) {
            Log.i(TAG, "Detectado entorno Termux, aplicando optimizaciones...");
            
            // Reducir FPS en Termux para mejor performance
            targetFPS = 30;
            adaptiveFPS = true;
            
            // Activar modo de baja memoria
            lowMemoryMode = true;
            
            // Optimizar recursos gráficos
            setWillNotDraw(false);
        } else {
            Log.i(TAG, "Entorno Android estándar detectado");
            targetFPS = 60;
            adaptiveFPS = true;
        }
    }
    
    /**
     * Inicializa los recursos de dibujo
     */
    private void initializeDrawingResources() {
        // Paint para el fondo
        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.BLACK);
        
        // Paint para FPS
        fpsPaint = new Paint();
        fpsPaint.setColor(Color.GREEN);
        fpsPaint.setTextSize(24);
        fpsPaint.setAntiAlias(true);
        
        // Paint para texto general
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(32);
        textPaint.setAntiAlias(true);
    }
    
    /**
     * Configura callbacks por defecto del sistema
     */
    private void setupDefaultCallbacks() {
        // Callback para cambios de estado del juego
        addCallback(new GameViewCallback() {
            @Override
            public void onGameStateChanged(GameState newState) {
                Log.d(TAG, "Estado del juego cambiado: " + newState);
            }
            
            @Override
            public void onFPSChanged(float fps) {
                // FPS actualizado automáticamente
            }
            
            @Override
            public void onError(Exception error) {
                Log.e(TAG, "Error en GameView", error);
            }
            
            @Override
            public void onPerformanceWarning(String message) {
                Log.w(TAG, "Advertencia de performance: " + message);
            }
        });
    }
    
    // === SURFACE HOLDER CALLBACKS ===
    
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "Superficie creada");
        
        synchronized (surfaceLock) {
            surfaceCreated = true;
            surfaceDestroyed = false;
        }
        
        // Notificar callbacks
        notifyCallbacks(callback -> callback.onSurfaceCreated());
        
        // Inicializar GameEngine si no está inicializado
        if (!engineInitialized) {
            initializeGameEngine();
        }
        
        // Iniciar renderizado
        startRendering();
    }
    
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "Superficie cambiada: " + width + "x" + height);
        
        viewWidth = width;
        viewHeight = height;
        
        // Actualizar límites del mundo en GameEngine
        if (gameEngine != null) {
            Vector2D worldBounds = new Vector2D(width, height);
            gameEngine.setWorldBounds(worldBounds);
        }
        
        // Notificar callbacks
        notifyCallbacks(callback -> callback.onSurfaceChanged(width, height));
    }
    
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "Superficie destruida");
        
        synchronized (surfaceLock) {
            surfaceDestroyed = true;
            surfaceCreated = false;
        }
        
        // Detener renderizado
        stopRendering();
        
        // Notificar callbacks
        notifyCallbacks(callback -> callback.onSurfaceDestroyed());
    }
    
    // === GAME ENGINE INTEGRATION ===
    
    /**
     * Inicializa el GameEngine
     */
    private void initializeGameEngine() {
        if (gameEngine != null) {
            gameEngine.initialize();
            engineInitialized = true;
            Log.d(TAG, "GameEngine inicializado");
        }
    }
    
    /**
     * Inicia el juego
     */
    public void startGame() {
        if (gameEngine != null && engineInitialized) {
            gameEngine.setGameState(GameState.PLAYING);
            gameEngine.start();
            
            notifyCallbacks(callback -> callback.onGameStateChanged(GameState.PLAYING));
            Log.i(TAG, "Juego iniciado");
        }
    }
    
    /**
     * Pausa el juego
     */
    public void pauseGame() {
        if (gameEngine != null) {
            gameEngine.pause();
            gameEngine.setGameState(GameState.PAUSED);
            
            notifyCallbacks(callback -> callback.onGameStateChanged(GameState.PAUSED));
            Log.i(TAG, "Juego pausado");
        }
    }
    
    /**
     * Reanuda el juego
     */
    public void resumeGame() {
        if (gameEngine != null) {
            gameEngine.resume();
            gameEngine.setGameState(GameState.PLAYING);
            
            notifyCallbacks(callback -> callback.onGameStateChanged(GameState.PLAYING));
            Log.i(TAG, "Juego reanudado");
        }
    }
    
    /**
     * Detiene el juego
     */
    public void stopGame() {
        if (gameEngine != null) {
            gameEngine.stop();
            gameEngine.cleanup();
            engineInitialized = false;
            
            notifyCallbacks(callback -> callback.onGameStateChanged(GameState.MENU));
            Log.i(TAG, "Juego detenido");
        }
    }
    
    // === RENDERING SYSTEM ===
    
    /**
     * Inicia el hilo de renderizado
     */
    private void startRendering() {
        if (!isRendering) {
            renderThread = new RenderThread();
            isRendering = true;
            renderThread.start();
            Log.d(TAG, "Hilo de renderizado iniciado");
        }
    }
    
    /**
     * Detiene el hilo de renderizado
     */
    private void stopRendering() {
        if (isRendering && renderThread != null) {
            isRendering = false;
            
            try {
                renderThread.join(1000); // Esperar máximo 1 segundo
                if (renderThread.isAlive()) {
                    Log.w(TAG, "Render thread no se detuvo correctamente");
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrumpido mientras esperaba que terminara render thread", e);
            }
            
            renderThread = null;
            Log.d(TAG, "Hilo de renderizado detenido");
        }
    }
    
    /**
     * Bucle principal de renderizado
     */
    private void renderLoop() {
        Log.d(TAG, "Iniciando bucle de renderizado");
        
        long lastTime = System.currentTimeMillis();
        long targetTime = 1000 / targetFPS;
        
        while (isRendering) {
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - lastTime;
            
            // Control de FPS adaptativo
            if (adaptiveFPS && elapsedTime < targetTime) {
                try {
                    Thread.sleep(targetTime - elapsedTime);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Render thread interrumpido", e);
                    break;
                }
            }
            
            lastTime = System.currentTimeMillis();
            
            // Renderizar frame
            renderFrame();
            
            // Actualizar estadísticas de FPS
            updateFPSStats();
        }
        
        Log.d(TAG, "Bucle de renderizado terminado");
    }
    
    /**
     * Renderiza un frame individual
     */
    private void renderFrame() {
        if (surfaceHolder == null || !surfaceHolder.getSurface().isValid()) {
            return;
        }
        
        Canvas canvas = null;
        try {
            // Obtener canvas bloqueando la superficie
            canvas = surfaceHolder.lockCanvas();
            
            if (canvas != null) {
                // Limpiar canvas
                canvas.drawColor(Color.BLACK);
                
                // Sincronizar con GameEngine
                if (gameEngine != null && gameEngine.isRunning()) {
                    // El GameEngine maneja su propia lógica de update/render
                    // Aquí solo renderizamos lo que GameEngine nos proporciona
                    renderGameContent(canvas);
                } else {
                    // Renderizar pantalla de carga o menú
                    renderIdleScreen(canvas);
                }
                
                // Renderizar overlay de FPS si está en modo debug
                if (isDebugMode()) {
                    renderFPSOverlay(canvas);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error durante renderizado", e);
            notifyCallbacks(callback -> callback.onError(e));
        } finally {
            if (canvas != null) {
                try {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                } catch (Exception e) {
                    Log.e(TAG, "Error al liberar canvas", e);
                }
            }
        }
    }
    
    /**
     * Renderiza el contenido del juego
     */
    private void renderGameContent(Canvas canvas) {
        try {
            // Renderizar el background del juego
            renderGameBackground(canvas);
            
            // Aquí podríamos llamar a métodos del GameEngine para obtener
            // las entidades y renderizarlas, pero por ahora usamos placeholders
            
            // Placeholder: Renderizar player si existe
            if (gameEngine.getPlayer() != null) {
                renderPlayer(canvas, gameEngine.getPlayer());
            }
            
            // Placeholder: Renderizar entidades de comida
            renderFoodEntities(canvas);
            
            // Placeholder: Renderizar UI del juego
            renderGameUI(canvas);
            
        } catch (Exception e) {
            Log.e(TAG, "Error renderizando contenido del juego", e);
            notifyCallbacks(callback -> callback.onError(e));
        }
    }
    
    /**
     * Renderiza el fondo del juego
     */
    private void renderGameBackground(Canvas canvas) {
        // Gradiente de fondo o patrón de grid
        Paint gridPaint = new Paint();
        gridPaint.setColor(Color.DARK_GRAY);
        gridPaint.setStrokeWidth(1);
        
        int gridSize = 50;
        for (int x = 0; x < viewWidth; x += gridSize) {
            canvas.drawLine(x, 0, x, viewHeight, gridPaint);
        }
        for (int y = 0; y < viewHeight; y += gridSize) {
            canvas.drawLine(0, y, viewWidth, y, gridPaint);
        }
    }
    
    /**
     * Renderiza el jugador
     */
    private void renderPlayer(Canvas canvas, com.gaming.enhancedagar.entities.Player player) {
        Paint playerPaint = new Paint();
        playerPaint.setColor(Color.BLUE);
        playerPaint.setAntiAlias(true);
        
        // Obtener posición del jugador
        Vector2D pos = player.getPosition();
        double radius = player.getRadius();
        
        if (pos != null && radius > 0) {
            canvas.drawCircle((float) pos.x, (float) pos.y, (float) radius, playerPaint);
            
            // Renderizar nombre del jugador
            Paint namePaint = new Paint();
            namePaint.setColor(Color.WHITE);
            namePaint.setTextSize(16);
            namePaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(player.getName(), (float) pos.x, (float) pos.y - (float) radius - 10, namePaint);
        }
    }
    
    /**
     * Renderiza entidades de comida
     */
    private void renderFoodEntities(Canvas canvas) {
        Paint foodPaint = new Paint();
        foodPaint.setAntiAlias(true);
        
        // Placeholder: Renderizar algunos círculos de comida
        int foodCount = Math.min(gameEngine.getFoodCount(), 20); // Limitar para performance
        
        for (int i = 0; i < foodCount; i++) {
            float x = (float) (Math.random() * viewWidth);
            float y = (float) (Math.random() * viewHeight);
            float radius = 5 + (float) (Math.random() * 5);
            
            // Color aleatorio para la comida
            foodPaint.setColor(Color.HSVToColor(new float[]{
                (float) (Math.random() * 360), 0.7f, 0.9f
            }));
            
            canvas.drawCircle(x, y, radius, foodPaint);
        }
    }
    
    /**
     * Renderiza UI del juego
     */
    private void renderGameUI(Canvas canvas) {
        // Información básica del juego
        Paint uiPaint = new Paint();
        uiPaint.setColor(Color.WHITE);
        uiPaint.setTextSize(18);
        uiPaint.setAntiAlias(true);
        
        String info = String.format("Entities: %d", gameEngine.getActiveEntities().size());
        canvas.drawText(info, 10, 30, uiPaint);
        
        // Estado del juego
        if (gameEngine.getGameState() != null) {
            String stateText = "State: " + gameEngine.getGameState().getDisplayName();
            canvas.drawText(stateText, 10, 55, uiPaint);
        }
    }
    
    /**
     * Renderiza pantalla en estado idle (carga/menú)
     */
    private void renderIdleScreen(Canvas canvas) {
        // Fondo
        canvas.drawColor(Color.BLACK);
        
        // Texto de carga
        Paint loadingPaint = new Paint();
        loadingPaint.setColor(Color.WHITE);
        loadingPaint.setTextSize(32);
        loadingPaint.setTextAlign(Paint.Align.CENTER);
        loadingPaint.setAntiAlias(true);
        
        String text = "Enhanced Agar";
        float centerX = viewWidth / 2f;
        float centerY = viewHeight / 2f;
        
        canvas.drawText(text, centerX, centerY, loadingPaint);
        
        if (!engineInitialized) {
            String subText = "Inicializando...";
            Paint subPaint = new Paint();
            subPaint.setColor(Color.GRAY);
            subPaint.setTextSize(20);
            subPaint.setTextAlign(Paint.Align.CENTER);
            subPaint.setAntiAlias(true);
            
            canvas.drawText(subText, centerX, centerY + 40, subPaint);
        }
    }
    
    /**
     * Renderiza overlay de FPS para debugging
     */
    private void renderFPSOverlay(Canvas canvas) {
        String fpsText = String.format("FPS: %.1f", currentFPS);
        canvas.drawText(fpsText, viewWidth - 150, 30, fpsPaint);
    }
    
    /**
     * Actualiza estadísticas de FPS
     */
    private void updateFPSStats() {
        frameCount++;
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - fpsTimer >= FPS_UPDATE_INTERVAL) {
            currentFPS = (frameCount * 1000.0f) / (currentTime - fpsTimer);
            fpsTimer = currentTime;
            frameCount = 0;
            
            // Notificar cambio de FPS
            notifyCallbacks(callback -> callback.onFPSChanged(currentFPS));
            
            // Advertencias de performance
            if (currentFPS < 20) {
                notifyCallbacks(callback -> callback.onPerformanceWarning("FPS bajo: " + currentFPS));
            }
        }
    }
    
    /**
     * Verifica si está en modo debug
     */
    private boolean isDebugMode() {
        return Log.isLoggable(TAG, Log.DEBUG);
    }
    
    // === TOUCH EVENT HANDLING ===
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = x;
                lastTouchY = y;
                isTouching = true;
                
                // Notificar callbacks
                notifyCallbacks(callback -> callback.onTouchDown(x, y));
                break;
                
            case MotionEvent.ACTION_MOVE:
                if (isTouching) {
                    float deltaX = x - lastTouchX;
                    float deltaY = y - lastTouchY;
                    
                    // Notificar callbacks
                    notifyCallbacks(callback -> callback.onTouchMove(x, y, deltaX, deltaY));
                    
                    lastTouchX = x;
                    lastTouchY = y;
                }
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isTouching = false;
                
                // Notificar callbacks
                notifyCallbacks(callback -> callback.onTouchUp(x, y));
                break;
        }
        
        return true; // Evento procesado
    }
    
    // === CALLBACK SYSTEM ===
    
    /**
     * Interfaz para callbacks del GameView
     */
    public interface GameViewCallback {
        default void onSurfaceCreated() {}
        default void onSurfaceChanged(int width, int height) {}
        default void onSurfaceDestroyed() {}
        default void onGameStateChanged(GameState newState) {}
        default void onFPSChanged(float fps) {}
        default void onError(Exception error) {}
        default void onPerformanceWarning(String message) {}
        default void onTouchDown(float x, float y) {}
        default void onTouchMove(float x, float y, float deltaX, float deltaY) {}
        default void onTouchUp(float x, float y) {}
    }
    
    /**
     * Agrega un callback
     */
    public void addCallback(GameViewCallback callback) {
        if (callback != null) {
            callbacks.add(callback);
        }
    }
    
    /**
     * Remueve un callback
     */
    public void removeCallback(GameViewCallback callback) {
        callbacks.remove(callback);
    }
    
    /**
     * Notifica todos los callbacks
     */
    private <T> void notifyCallbacks(java.util.function.Consumer<GameViewCallback> action) {
        for (GameViewCallback callback : callbacks) {
            try {
                action.accept(callback);
            } catch (Exception e) {
                Log.e(TAG, "Error notificando callback", e);
            }
        }
    }
    
    // === RENDER THREAD CLASS ===
    
    /**
     * Hilo dedicado para renderizado
     */
    private class RenderThread extends Thread {
        public RenderThread() {
            super("GameView-RenderThread");
        }
        
        @Override
        public void run() {
            Log.d(TAG, "RenderThread iniciado");
            renderLoop();
        }
    }
    
    // === GETTERS AND UTILITY METHODS ===
    
    /**
     * Obtiene el GameEngine
     */
    public GameEngine getGameEngine() {
        return gameEngine;
    }
    
    /**
     * Obtiene el FPS actual
     */
    public float getCurrentFPS() {
        return currentFPS;
    }
    
    /**
     * Obtiene el estado del renderizado
     */
    public boolean isRendering() {
        return isRendering;
    }
    
    /**
     * Establece el FPS objetivo
     */
    public void setTargetFPS(int fps) {
        this.targetFPS = Math.max(10, Math.min(fps, 120));
    }
    
    /**
     * Activa/desactiva FPS adaptativo
     */
    public void setAdaptiveFPS(boolean adaptive) {
        this.adaptiveFPS = adaptive;
    }
    
    /**
     * Establece modo de baja memoria
     */
    public void setLowMemoryMode(boolean lowMemory) {
        this.lowMemoryMode = lowMemory;
        if (lowMemory) {
            setTargetFPS(30);
        }
    }
    
    /**
     * Obtiene dimensiones de la vista
     */
    public Vector2D getViewDimensions() {
        return new Vector2D(viewWidth, viewHeight);
    }
    
    /**
     * Verifica si la superficie está lista
     */
    public boolean isSurfaceReady() {
        synchronized (surfaceLock) {
            return surfaceCreated && !surfaceDestroyed;
        }
    }
    
    /**
     * Limpia recursos del GameView
     */
    public void cleanup() {
        Log.d(TAG, "Limpiando recursos del GameView");
        
        // Detener juego
        stopGame();
        
        // Detener renderizado
        stopRendering();
        
        // Limpiar callbacks
        callbacks.clear();
        
        // Limpiar GameEngine
        if (gameEngine != null) {
            gameEngine.cleanup();
        }
        
        // Liberar recursos de dibujo
        backgroundPaint = null;
        fpsPaint = null;
        textPaint = null;
    }
    
    /**
     * Obtiene información de debug del GameView
     */
    public String getDebugInfo() {
        return String.format(
            "GameView{engine=%s, rendering=%s, fps=%.1f, surface=%s, callbacks=%d}",
            gameEngine != null ? "initialized" : "null",
            isRendering,
            currentFPS,
            isSurfaceReady() ? "ready" : "not ready",
            callbacks.size()
        );
    }
    
    @Override
    public String toString() {
        return "GameView{" +
                "width=" + viewWidth +
                ", height=" + viewHeight +
                ", fps=" + String.format("%.1f", currentFPS) +
                ", rendering=" + isRendering +
                ", surface=" + (surfaceCreated ? "created" : "not created") +
                '}';
    }
}