package com.gaming.enhancedagar.game;

import android.graphics.Canvas;
import android.view.SurfaceHolder;
import android.os.SystemClock;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import com.gaming.enhancedagar.engine.GameEngine;

/**
 * GameThread - Maneja el bucle de juego principal con control preciso de FPS
 * y sincronización con SurfaceView para el juego Enhanced Agar
 */
public class GameThread extends Thread {
    
    // Estados del juego
    public enum GameState {
        RUNNING,
        PAUSED,
        STOPPED,
        WAITING
    }
    
    // Configuración del thread
    private static final int MAX_FRAME_SKIP = 5;
    private static final double TARGET_FPS = 60.0;
    private static final double FRAME_TIME = 1000.0 / TARGET_FPS; // ~16.67ms por frame
    
    // Referencias del sistema
    private final SurfaceHolder surfaceHolder;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile GameState currentState = GameState.WAITING;
    
    // Control de FPS y timing
    private volatile int fps = 0;
    private int frameCount = 0;
    private long lastFpsTime = 0;
    private long lastUpdateTime = 0;
    private long deltaTime = 0;
    
    // Sincronización y locks
    private final ReentrantLock gameLock = new ReentrantLock();
    private final Object pauseLock = new Object();
    
    // Lógica del juego
    private GameEngine gameEngine;
    
    // Threading
    private volatile boolean isInterrupted = false;
    private volatile boolean isPaused = false;
    
    // Estadísticas de performance
    private long totalGameTime = 0;
    private long lastRenderTime = 0;
    private long minFrameTime = Long.MAX_VALUE;
    private long maxFrameTime = 0;
    
    /**
     * Constructor del GameThread
     * @param surfaceHolder SurfaceHolder para el rendering
     * @param gameEngine Referencia al motor del juego
     */
    public GameThread(SurfaceHolder surfaceHolder, GameEngine gameEngine) {
        super("GameThread");
        this.surfaceHolder = surfaceHolder;
        this.gameEngine = gameEngine;
        this.lastFpsTime = System.currentTimeMillis();
        this.lastUpdateTime = SystemClock.uptimeMillis();
    }
    
    @Override
    public void run() {
        Canvas canvas = null;
        long frameStartTime;
        long frameEndTime;
        long frameTime;
        int skipCount = 0;
        
        while (running.get()) {
            frameStartTime = SystemClock.uptimeMillis();
            
            try {
                // Verificar estado del juego
                if (currentState == GameState.PAUSED) {
                    handlePausedState();
                    continue;
                }
                
                if (currentState == GameState.STOPPED) {
                    break;
                }
                
                // Obtener canvas para renderizado
                canvas = surfaceHolder.lockCanvas();
                if (canvas == null) {
                    // Si no se puede obtener el canvas, esperar un poco
                    SystemClock.sleep(1);
                    continue;
                }
                
                // Sincronizar acceso al motor del juego
                gameLock.lock();
                try {
                    // Actualizar lógica del juego
                    updateGameLogic();
                    
                    // Renderizar frame
                    renderFrame(canvas);
                    
                } finally {
                    gameLock.unlock();
                }
                
            } catch (Exception e) {
                handleGameException(e);
            } finally {
                if (canvas != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    } catch (Exception e) {
                        handleCanvasException(e);
                    }
                }
            }
            
            frameEndTime = SystemClock.uptimeMillis();
            frameTime = frameEndTime - frameStartTime;
            
            // Actualizar estadísticas de timing
            updateFrameStatistics(frameTime);
            
            // Control de FPS
            controlFrameRate(frameStartTime, frameEndTime);
            
            // Lógica de salto de frames para mantener FPS objetivo
            handleFrameSkipping(frameStartTime, frameEndTime, skipCount);
        }
        
        // Limpieza final
        cleanup();
    }
    
    /**
     * Actualiza la lógica principal del juego
     */
    private void updateGameLogic() {
        long currentTime = SystemClock.uptimeMillis();
        deltaTime = currentTime - lastUpdateTime;
        
        // Evitar actualizaciones extremadamente lentas
        if (deltaTime > 100) { // Si pasan más de 100ms
            deltaTime = 100; // Limitar a 100ms máximo
        }
        
        lastUpdateTime = currentTime;
        totalGameTime += deltaTime;
        
        // Actualizar motor del juego
        if (gameEngine != null) {
            gameEngine.update(deltaTime);
        }
    }
    
    /**
     * Renderiza un frame en el canvas
     */
    private void renderFrame(Canvas canvas) {
        if (gameEngine != null) {
            gameEngine.render(canvas);
        }
        
        // Opcional: mostrar estadísticas en el canvas para debug
        renderDebugInfo(canvas);
    }
    
    /**
     * Renderiza información de debug (FPS, timing, etc.)
     */
    private void renderDebugInfo(Canvas canvas) {
        // Aquí se podría renderizar un overlay con estadísticas
        // canvas.drawText("FPS: " + fps, 10, 20, debugPaint);
    }
    
    /**
     * Controla el frame rate para mantener FPS objetivo
     */
    private void controlFrameRate(long frameStartTime, long frameEndTime) {
        long frameDuration = frameEndTime - frameStartTime;
        long timeToSleep = (long) (FRAME_TIME - frameDuration);
        
        if (timeToSleep > 0) {
            try {
                sleep(timeToSleep);
            } catch (InterruptedException e) {
                handleThreadInterruption();
            }
        } else {
            // Frame took too long, we're behind
            frameCount++;
        }
        
        // Actualizar FPS cada segundo
        updateFpsCounter();
    }
    
    /**
     * Maneja el salto de frames para mantener fluidez
     */
    private void handleFrameSkipping(long frameStartTime, long frameEndTime, int skipCount) {
        long frameDuration = frameEndTime - frameStartTime;
        
        if (frameDuration < FRAME_TIME && skipCount < MAX_FRAME_SKIP) {
            // Frame fue rápido, pero estamos satisfechos con el FPS actual
            skipCount++;
        }
    }
    
    /**
     * Actualiza el contador de FPS
     */
    private void updateFpsCounter() {
        frameCount++;
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastFpsTime >= 1000) {
            fps = frameCount;
            frameCount = 0;
            lastFpsTime = currentTime;
        }
    }
    
    /**
     * Actualiza estadísticas de rendimiento
     */
    private void updateFrameStatistics(long frameTime) {
        lastRenderTime = frameTime;
        
        if (frameTime < minFrameTime) {
            minFrameTime = frameTime;
        }
        
        if (frameTime > maxFrameTime) {
            maxFrameTime = frameTime;
        }
    }
    
    /**
     * Maneja el estado pausado del juego
     */
    private void handlePausedState() {
        synchronized (pauseLock) {
            while (isPaused && running.get()) {
                try {
                    pauseLock.wait();
                } catch (InterruptedException e) {
                    handleThreadInterruption();
                }
            }
        }
    }
    
    /**
     * Maneja excepciones durante el juego
     */
    private void handleGameException(Exception e) {
        // Log de la excepción para debugging
        System.err.println("Error en GameThread: " + e.getMessage());
        e.printStackTrace();
    }
    
    /**
     * Maneja excepciones relacionadas con el canvas
     */
    private void handleCanvasException(Exception e) {
        // Excepción al liberar canvas, continuar normalmente
        System.err.println("Error al liberar canvas: " + e.getMessage());
    }
    
    /**
     * Maneja interrupciones del thread
     */
    private void handleThreadInterruption() {
        isInterrupted = true;
        interrupt();
    }
    
    /**
     * Limpieza final del thread
     */
    private void cleanup() {
        running.set(false);
        currentState = GameState.STOPPED;
        
        // Limpiar recursos del motor del juego
        if (gameEngine != null) {
            gameEngine.onGameThreadStop();
        }
    }
    
    /**
     * Inicia el bucle de juego
     */
    public void startGame() {
        if (currentState == GameState.WAITING || currentState == GameState.STOPPED) {
            running.set(true);
            currentState = GameState.RUNNING;
            isInterrupted = false;
            
            if (!isAlive()) {
                start();
            }
        }
    }
    
    /**
     * Pausa el juego
     */
    public void pauseGame() {
        if (currentState == GameState.RUNNING) {
            isPaused = true;
            currentState = GameState.PAUSED;
            
            synchronized (pauseLock) {
                pauseLock.notifyAll();
            }
        }
    }
    
    /**
     * Reanuda el juego desde pausa
     */
    public void resumeGame() {
        if (currentState == GameState.PAUSED) {
            isPaused = false;
            currentState = GameState.RUNNING;
            lastUpdateTime = SystemClock.uptimeMillis();
            
            synchronized (pauseLock) {
                pauseLock.notifyAll();
            }
        }
    }
    
    /**
     * Detiene el juego completamente
     */
    public void stopGame() {
        running.set(false);
        currentState = GameState.STOPPED;
        
        // Notificar para salir del estado de pausa
        synchronized (pauseLock) {
            pauseLock.notifyAll();
        }
        
        // Interrumpir el thread si está dormido
        if (isAlive()) {
            interrupt();
        }
    }
    
    /**
     * Verifica si el juego está corriendo
     */
    public boolean isRunning() {
        return running.get() && currentState == GameState.RUNNING && !isPaused;
    }
    
    /**
     * Verifica si el juego está pausado
     */
    public boolean isPaused() {
        return currentState == GameState.PAUSED;
    }
    
    /**
     * Obtiene el estado actual del juego
     */
    public GameState getGameState() {
        return currentState;
    }
    
    /**
     * Obtiene el FPS actual
     */
    public int getFps() {
        return fps;
    }
    
    /**
     * Obtiene el tiempo delta en milisegundos
     */
    public long getDeltaTime() {
        return deltaTime;
    }
    
    /**
     * Obtiene el tiempo total de juego en milisegundos
     */
    public long getTotalGameTime() {
        return totalGameTime;
    }
    
    /**
     * Obtiene estadísticas de rendimiento
     */
    public PerformanceStats getPerformanceStats() {
        return new PerformanceStats(
            fps,
            minFrameTime,
            maxFrameTime,
            lastRenderTime,
            totalGameTime
        );
    }
    
    /**
     * Obtiene el tiempo del último renderizado
     */
    public long getLastRenderTime() {
        return lastRenderTime;
    }
    
    /**
     * Establece el motor del juego
     */
    public void setGameEngine(GameEngine gameEngine) {
        this.gameEngine = gameEngine;
    }
    
    /**
     * Verifica si el thread está activo
     */
    public boolean isGameThreadActive() {
        return isAlive() && !isInterrupted;
    }
    
    /**
     * Obtiene el lock del juego para sincronización externa
     */
    public ReentrantLock getGameLock() {
        return gameLock;
    }
    
    /**
     * Clase para estadísticas de rendimiento
     */
    public static class PerformanceStats {
        public final int fps;
        public final long minFrameTime;
        public final long maxFrameTime;
        public final long lastFrameTime;
        public final long totalGameTime;
        
        public PerformanceStats(int fps, long minFrameTime, long maxFrameTime, 
                              long lastFrameTime, long totalGameTime) {
            this.fps = fps;
            this.minFrameTime = minFrameTime;
            this.maxFrameTime = maxFrameTime;
            this.lastFrameTime = lastFrameTime;
            this.totalGameTime = totalGameTime;
        }
        
        @Override
        public String toString() {
            return String.format("FPS: %d, Min: %dms, Max: %dms, Last: %dms, Total: %dms",
                fps, minFrameTime, maxFrameTime, lastFrameTime, totalGameTime);
        }
    }
}