package com.gaming.enhancedagar;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import com.gaming.enhancedagar.engine.GameEngine;

/**
 * MainActivity principal del juego Enhanced Agar
 * Optimizada para funcionar en Android y Termux
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    
    private SurfaceView gameSurfaceView;
    private GameEngine gameEngine;
    private boolean isGameRunning = false;
    private AppCompatImageView debugOverlay;
    
    // Variables para manejo de touch
    private float lastTouchX = 0f;
    private float lastTouchY = 0f;
    private boolean isTouchActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Configuración de la ventana para mejor rendimiento
        configureWindow();
        
        // Inicializar SurfaceView
        initializeSurfaceView();
        
        // Crear instancia del GameEngine
        initializeGameEngine();
        
        // Configurar manejo de errores global
        setupGlobalExceptionHandler();
        
        // Log de información del dispositivo
        logDeviceInfo();
    }
    
    /**
     * Configura la ventana para mejor rendimiento del juego
     */
    private void configureWindow() {
        // Mantener la pantalla encendida durante el juego
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        // Configuración adicional para Termux
        try {
            // Habilitar modo de hardware acelerado si está disponible
            getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            );
        } catch (Exception e) {
            // Hardware acceleration no disponible, continuar sin él
            android.util.Log.w(TAG, "Hardware acceleration no disponible: " + e.getMessage());
        }
    }
    
    /**
     * Inicializa y configura el SurfaceView para el renderizado del juego
     */
    private void initializeSurfaceView() {
        gameSurfaceView = findViewById(R.id.game_surface);
        if (gameSurfaceView == null) {
            android.util.Log.e(TAG, "No se pudo encontrar SurfaceView en el layout");
            return;
        }
        
        // Configurar SurfaceView
        gameSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                android.util.Log.d(TAG, "Surface creada");
                if (gameEngine != null) {
                    gameEngine.setSurface(holder.getSurface());
                    gameEngine.setSurfaceSize(gameSurfaceView.getWidth(), gameSurfaceView.getHeight());
                }
            }
            
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                android.util.Log.d(TAG, "Surface cambiada: " + width + "x" + height);
                if (gameEngine != null) {
                    gameEngine.setSurfaceSize(width, height);
                }
            }
            
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                android.util.Log.d(TAG, "Surface destruida");
                if (gameEngine != null) {
                    gameEngine.onSurfaceDestroyed();
                }
            }
        });
        
        // Configurar listener para touch events
        gameSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return handleTouchEvent(event);
            }
        });
        
        // Optimizaciones específicas para Termux
        gameSurfaceView.setZOrderOnTop(true);
    }
    
    /**
     * Inicializa el GameEngine
     */
    private void initializeGameEngine() {
        try {
            gameEngine = new GameEngine(this);
            android.util.Log.i(TAG, "GameEngine inicializado correctamente");
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error inicializando GameEngine", e);
        }
    }
    
    /**
     * Maneja los eventos de touch para control del juego
     */
    private boolean handleTouchEvent(MotionEvent event) {
        if (gameEngine == null) return false;
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                isTouchActive = true;
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                gameEngine.onTouchEvent(lastTouchX, lastTouchY);
                return true;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isTouchActive = false;
                gameEngine.onTouchRelease();
                return true;
        }
        
        return false;
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        android.util.Log.d(TAG, "onResume llamado");
        
        if (gameEngine != null) {
            gameEngine.onResume();
        }
        
        isGameRunning = true;
        
        // Reiniciar el game loop si es necesario
        if (gameSurfaceView != null && gameSurfaceView.getHolder().isCreating()) {
            gameSurfaceView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (gameEngine != null && gameSurfaceView.getHolder().getSurface().isValid()) {
                        gameEngine.onResume();
                    }
                }
            }, 100);
        }
    }
    
    @Override
    protected void onPause() {
        android.util.Log.d(TAG, "onPause llamado");
        super.onPause();
        
        isGameRunning = false;
        
        if (gameEngine != null) {
            gameEngine.onPause();
        }
    }
    
    @Override
    protected void onDestroy() {
        android.util.Log.d(TAG, "onDestroy llamado");
        super.onDestroy();
        
        if (gameEngine != null) {
            gameEngine.onDestroy();
            gameEngine = null;
        }
        
        // Limpiar referencias
        gameSurfaceView = null;
        debugOverlay = null;
    }
    
    /**
     * Maneja el cambio de orientación de pantalla
     */
    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        android.util.Log.d(TAG, "Configuración cambiada: " + newConfig.orientation);
        
        // Recalcular tamaño de la superficie
        if (gameEngine != null) {
            gameEngine.onConfigurationChanged(newConfig);
        }
    }
    
    /**
     * Configura el manejo global de excepciones para debug
     */
    private void setupGlobalExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                android.util.Log.e(TAG, "Excepción no capturada en thread: " + thread.getName(), ex);
                
                // En producción, podrías querer enviar este error a un servicio de logging
                // Crashlytics.getInstance().recordException(ex);
                
                // Para debug en Termux, imprimir stack trace completo
                if (isRunningInTermux()) {
                    ex.printStackTrace();
                }
                
                // Finalizar la actividad
                finish();
            }
        });
    }
    
    /**
     * Detecta si la aplicación está ejecutándose en Termux
     */
    private boolean isRunningInTermux() {
        return System.getenv("TERMUX_VERSION") != null;
    }
    
    /**
     * Log de información del dispositivo para debugging
     */
    private void logDeviceInfo() {
        android.util.Log.i(TAG, "=== Información del Dispositivo ===");
        android.util.Log.i(TAG, "Modelo: " + android.os.Build.MODEL);
        android.util.Log.i(TAG, "Marca: " + android.os.Build.BRAND);
        android.util.Log.i(TAG, "Android SDK: " + android.os.Build.VERSION.SDK_INT);
        android.util.Log.i(TAG, "Versión Android: " + android.os.Build.VERSION.RELEASE);
        android.util.Log.i(TAG, "Arquitectura: " + System.getProperty("os.arch"));
        android.util.Log.i(TAG, "Ejecutándose en Termux: " + isRunningInTermux());
        
        // Información de memoria
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        
        android.util.Log.i(TAG, "Memoria máx: " + (maxMemory / 1024 / 1024) + "MB");
        android.util.Log.i(TAG, "Memoria total: " + (totalMemory / 1024 / 1024) + "MB");
        android.util.Log.i(TAG, "Memoria libre: " + (freeMemory / 1024 / 1024) + "MB");
        android.util.Log.i(TAG, "=================================");
    }
    
    /**
     * Método público para actualizar información de debug
     */
    public void updateDebugInfo(String info) {
        if (debugOverlay != null) {
            // Aquí podrías actualizar un overlay de debug si existe
            android.util.Log.d(TAG, "Debug: " + info);
        }
    }
    
    /**
     * Método para forzar garbage collection (solo para debug)
     */
    public void forceGarbageCollection() {
        System.gc();
        android.util.Log.i(TAG, "Garbage collection forzado");
    }
    
    /**
     * Método para obtener el estado actual del juego
     */
    public boolean isGameEngineRunning() {
        return isGameRunning && gameEngine != null;
    }
    
    /**
     * Método público para pausar/reanudar el juego manualmente
     */
    public void toggleGamePause() {
        if (gameEngine != null) {
            if (isGameRunning) {
                gameEngine.onPause();
                isGameRunning = false;
            } else {
                gameEngine.onResume();
                isGameRunning = true;
            }
        }
    }
}