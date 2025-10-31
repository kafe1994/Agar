package com.gaming.enhancedagar.engine;

import android.content.Context;
import android.graphics.*;
import android.os.Build;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import androidx.annotation.RequiresApi;
import com.gaming.enhancedagar.game.CameraManager;
import com.gaming.enhancedagar.game.CoordinateSystem;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Sistema de renderizado principal optimizado para Enhanced Agar
 * Maneja múltiples capas, batching, escalado y optimización de performance
 */
public class Renderer {
    private static final String TAG = "Renderer";
    
    // Canvas principal y contexto
    private Canvas canvas;
    private SurfaceHolder surfaceHolder;
    private Context context;
    private Paint paint;
    private Matrix identityMatrix;
    
    // Sistema de capas
    public enum Layer {
        BACKGROUND(0),
        ENTITIES(1),
        UI(2),
        EFFECTS(3);
        
        public final int priority;
        
        Layer(int priority) {
            this.priority = priority;
        }
    }
    
    // Estructuras para renderizado por lotes
    private static class RenderBatch {
        Paint paint;
        Path path;
        RectF bounds;
        Layer layer;
        float zOrder;
        
        RenderBatch(Paint paint, Path path, RectF bounds, Layer layer, float zOrder) {
            this.paint = paint;
            this.path = path;
            this.bounds = bounds;
            this.layer = layer;
            this.zOrder = zOrder;
        }
    }
    
    // Cache de bitmaps para optimización
    private final Map<String, Bitmap> bitmapCache = new ConcurrentHashMap<>();
    private final LruCache<String, Bitmap> dynamicCache;
    
    // Colas de renderizado por capa
    private final Map<Layer, List<RenderBatch>> layerQueues = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock queueLock = new ReentrantReadWriteLock();
    
    // Sistema de transformaciones
    private CameraManager cameraManager;
    private CoordinateSystem coordinateSystem;
    private float worldScale = 1.0f;
    private float deviceScale = 1.0f;
    private float density;
    
    // Estadísticas y debug
    private final DebugStats debugStats = new DebugStats();
    private boolean debugMode = false;
    
    // Optimización
    private boolean hardwareAcceleration = true;
    private Paint debugPaint;
    private Rect debugRect;
    
    /**
     * Constructor del renderer
     */
    public Renderer(Context context, SurfaceHolder surfaceHolder) {
        this.context = context;
        this.surfaceHolder = surfaceHolder;
        this.paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.identityMatrix = new Matrix();
        this.dynamicCache = new LruCache<String, Bitmap>(50) {
            @Override
            protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
                if (evicted && !oldValue.isRecycled()) {
                    oldValue.recycle();
                }
            }
        };
        
        initializeDebugTools();
        initializeLayerQueues();
        setupHardwareAcceleration();
        
        // Obtener densidad de pantalla
        density = context.getResources().getDisplayMetrics().density;
        
        Log.i(TAG, "Renderer inicializado con aceleración de hardware: " + hardwareAcceleration);
    }
    
    /**
     * Inicializa las herramientas de debug
     */
    private void initializeDebugTools() {
        debugPaint = new Paint();
        debugPaint.setColor(Color.RED);
        debugPaint.setStyle(Paint.Style.STROKE);
        debugPaint.setStrokeWidth(2);
        debugPaint.setTextSize(30);
        
        debugRect = new Rect();
    }
    
    /**
     * Inicializa las colas de capas
     */
    private void initializeLayerQueues() {
        for (Layer layer : Layer.values()) {
            layerQueues.put(layer, new ArrayList<>());
        }
    }
    
    /**
     * Configura la aceleración de hardware
     */
    private void setupHardwareAcceleration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            try {
                paint.setLayerType(Paint.LAYER_TYPE_HARDWARE, null);
                hardwareAcceleration = true;
            } catch (Exception e) {
                Log.w(TAG, "Error configurando aceleración de hardware: " + e.getMessage());
                hardwareAcceleration = false;
            }
        }
    }
    
    /**
     * Configura la resolución y escalado del dispositivo
     */
    public void setupResolution(int screenWidth, int screenHeight) {
        deviceScale = Math.min(screenWidth, screenHeight) / 1080f; // Normalizar a 1080p base
        
        // Configurar matriz de transformación
        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(deviceScale, deviceScale);
        
        Log.i(TAG, "Resolución configurada: " + screenWidth + "x" + screenHeight + 
                  ", Escala del dispositivo: " + deviceScale);
    }
    
    /**
     * Renderizado principal del frame
     */
    public void renderFrame() {
        if (canvas == null) {
            return;
        }
        
        long frameStart = System.nanoTime();
        
        try {
            // Limpiar canvas
            canvas.drawColor(Color.BLACK);
            
            // Aplicar transformaciones de cámara
            applyCameraTransformations();
            
            // Renderizar por lotes organizados por capas
            renderByLayers();
            
            // Renderizar UI si está habilitada
            renderUI();
            
            // Renderizar efectos
            renderEffects();
            
            // Renderizar debug si está habilitado
            if (debugMode) {
                renderDebug();
            }
            
            // Actualizar estadísticas
            updateFrameStats(frameStart);
            
        } catch (Exception e) {
            Log.e(TAG, "Error durante renderizado: " + e.getMessage(), e);
        }
    }
    
    /**
     * Aplica las transformaciones de la cámara
     */
    private void applyCameraTransformations() {
        if (cameraManager != null) {
            // Obtener transformaciones de la cámara
            float[] cameraMatrix = cameraManager.getCameraMatrix();
            
            // Aplicar escalado mundial
            Matrix worldMatrix = new Matrix();
            worldMatrix.postScale(worldScale, worldScale);
            
            // Combinar matrices
            canvas.concat(worldMatrix);
        }
    }
    
    /**
     * Renderiza las capas por lotes
     */
    private void renderByLayers() {
        queueLock.readLock().lock();
        try {
            // Ordenar capas por prioridad
            Arrays.stream(Layer.values())
                  .sorted(Comparator.comparingInt(layer -> layer.priority))
                  .forEach(this::renderLayer);
        } finally {
            queueLock.readLock().unlock();
        }
    }
    
    /**
     * Renderiza una capa específica
     */
    private void renderLayer(Layer layer) {
        List<RenderBatch> batchList = layerQueues.get(layer);
        if (batchList.isEmpty()) {
            return;
        }
        
        // Ordenar por z-order dentro de la capa
        batchList.sort(Comparator.comparingDouble(batch -> batch.zOrder));
        
        for (RenderBatch batch : batchList) {
            canvas.drawPath(batch.path, batch.paint);
        }
        
        // Limpiar la cola después del renderizado
        batchList.clear();
    }
    
    /**
     * Renderiza la UI
     */
    private void renderUI() {
        queueLock.readLock().lock();
        try {
            List<RenderBatch> uiBatch = layerQueues.get(Layer.UI);
            if (!uiBatch.isEmpty()) {
                // Restaurar matriz identidad para UI
                canvas.setMatrix(identityMatrix);
                
                for (RenderBatch batch : uiBatch) {
                    canvas.drawPath(batch.path, batch.paint);
                }
                
                uiBatch.clear();
            }
        } finally {
            queueLock.readLock().unlock();
        }
    }
    
    /**
     * Renderiza efectos
     */
    private void renderEffects() {
        queueLock.writeLock().lock();
        try {
            List<RenderBatch> effectsBatch = layerQueues.get(Layer.EFFECTS);
            
            // Renderizar efectos con alpha blending
            Paint effectPaint = new Paint(paint);
            effectPaint.setAlpha((int)(255 * 0.7f)); // 70% de opacidad
            
            for (RenderBatch batch : effectsBatch) {
                batch.paint.setAlpha(effectPaint.getAlpha());
                canvas.drawPath(batch.path, batch.paint);
            }
            
            effectsBatch.clear();
        } finally {
            queueLock.writeLock().unlock();
        }
    }
    
    /**
     * Añade un elemento al lote de renderizado
     */
    public void addToBatch(Path path, Paint paint, RectF bounds, Layer layer, float zOrder) {
        queueLock.writeLock().lock();
        try {
            RenderBatch batch = new RenderBatch(paint, path, bounds, layer, zOrder);
            layerQueues.get(layer).add(batch);
        } finally {
            queueLock.writeLock().unlock();
        }
    }
    
    /**
     * Añade un bitmap al cache
     */
    public void cacheBitmap(String key, Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            dynamicCache.put(key, bitmap);
            bitmapCache.put(key, bitmap);
        }
    }
    
    /**
     * Obtiene un bitmap del cache
     */
    public Bitmap getCachedBitmap(String key) {
        Bitmap bitmap = dynamicCache.get(key);
        if (bitmap == null) {
            bitmap = bitmapCache.get(key);
        }
        return bitmap;
    }
    
    /**
     * Dibuja un bitmap con escalado optimizado
     */
    public void drawBitmapOptimized(String key, Bitmap bitmap, Rect src, RectF dst) {
        if (bitmap != null && !bitmap.isRecycled()) {
            canvas.drawBitmap(bitmap, src, dst, paint);
        }
    }
    
    /**
     * Configura la escala mundial
     */
    public void setWorldScale(float scale) {
        this.worldScale = scale;
    }
    
    /**
     * Configura el administrador de cámara
     */
    public void setCameraManager(CameraManager cameraManager) {
        this.cameraManager = cameraManager;
    }
    
    /**
     * Configura el sistema de coordenadas
     */
    public void setCoordinateSystem(CoordinateSystem coordinateSystem) {
        this.coordinateSystem = coordinateSystem;
    }
    
    /**
     * Activa/desactiva el modo debug
     */
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }
    
    /**
     * Renderiza información de debug
     */
    private void renderDebug() {
        canvas.setMatrix(identityMatrix);
        
        String[] debugInfo = {
            "FPS: " + debugStats.getFPS(),
            "Batches: " + debugStats.getBatchCount(),
            "Memoria: " + (Runtime.getRuntime().totalMemory() / 1024 / 1024) + "MB",
            "Cache: " + dynamicCache.size() + " bitmaps"
        };
        
        float y = 50;
        for (String info : debugInfo) {
            canvas.drawText(info, 20, y, debugPaint);
            y += 35;
        }
        
        // Dibujar rectángulo de rendimiento
        debugRect.set(20, y + 20, 200, y + 60);
        canvas.drawRect(debugRect, debugPaint);
    }
    
    /**
     * Renderiza información de debug en el mundo
     */
    public void renderDebugInWorld(String text, float x, float y, Paint textPaint) {
        addToBatch(createTextPath(text, x, y), textPaint, new RectF(x-50, y-20, x+50, y+20), 
                  Layer.UI, 1000f);
    }
    
    /**
     * Crea un Path para texto
     */
    private Path createTextPath(String text, float x, float y) {
        Path path = new Path();
        path.moveTo(x, y);
        return path;
    }
    
    /**
     * Actualiza las estadísticas del frame
     */
    private void updateFrameStats(long frameStart) {
        long frameTime = System.nanoTime() - frameStart;
        debugStats.recordFrame(frameTime);
    }
    
    /**
     * Configura el canvas
     */
    public void setCanvas(Canvas canvas) {
        this.canvas = canvas;
        if (hardwareAcceleration && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                this.canvas.setInsertPreserved(CANVAS_INSERT_PRESERVED_NONE);
            }
        }
    }
    
    /**
     * Libera recursos
     */
    public void destroy() {
        // Limpiar caches
        for (Bitmap bitmap : bitmapCache.values()) {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
        bitmapCache.clear();
        
        // Limpiar colas de renderizado
        for (List<RenderBatch> queue : layerQueues.values()) {
            queue.clear();
        }
        
        // Liberar pintura de debug
        if (debugPaint != null) {
            debugPaint.reset();
        }
        
        Log.i(TAG, "Renderer destruido y recursos liberados");
    }
    
    /**
     * Clase para estadísticas de debug
     */
    private static class DebugStats {
        private final Queue<Long> frameTimes = new ArrayDeque<>();
        private final int maxSamples = 60;
        private int batchCount = 0;
        
        public void recordFrame(long frameTime) {
            frameTimes.offer(frameTime);
            if (frameTimes.size() > maxSamples) {
                frameTimes.poll();
            }
        }
        
        public float getFPS() {
            if (frameTimes.isEmpty()) return 0;
            
            long totalTime = 0;
            for (long time : frameTimes) {
                totalTime += time;
            }
            
            double avgFrameTime = totalTime / (double) frameTimes.size();
            return (float) (1000000000.0 / avgFrameTime);
        }
        
        public void setBatchCount(int count) {
            this.batchCount = count;
        }
        
        public int getBatchCount() {
            return batchCount;
        }
    }
    
    // Constantes
    private static final int CANVAS_INSERT_PRESERVED_NONE = 0;
    
    /**
     * Obtiene el paint principal
     */
    public Paint getPaint() {
        return paint;
    }
    
    /**
     * Obtiene el contexto
     */
    public Context getContext() {
        return context;
    }
    
    /**
     * Obtiene la densidad del dispositivo
     */
    public float getDensity() {
        return density;
    }
}