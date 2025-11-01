package com.gaming.enhancedagar.ui;

import com.gaming.enhancedagar.entities.Player;
import com.gaming.enhancedagar.game.GameState;
import com.gaming.enhancedagar.game.CameraManager;
import com.gaming.enhancedagar.utils.StatisticsManager;
import com.gaming.enhancedagar.utils.VersionInfo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.util.AttributeSet;
import android.util.Log;

import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * GameHUD - Interfaz de usuario principal durante el juego
 * Proporciona HUD completo con información del jugador, mini-mapa, estadísticas y controles
 * Versión Android - Reemplaza Swing por Android Views
 */
public class GameHUD extends RelativeLayout implements View.OnTouchListener {
    
    private static final String TAG = "GameHUD";
    
    // Referencias del juego
    private Player player;
    private GameState gameState;
    private CameraManager cameraManager;
    private StatisticsManager statisticsManager;
    private VersionInfo versionInfo;
    
    // Componentes de la interfaz
    private LinearLayout topPanel;
    private LinearLayout bottomPanel;
    private LinearLayout leftPanel;
    private LinearLayout rightPanel;
    private TextView scoreText;
    private TextView sizeText;
    private TextView positionText;
    private TextView fpsText;
    private TextView timeText;
    private TextView powerUpsText;
    private TextView statusText;
    private ListView leaderboardList;
    private ArrayAdapter<String> leaderboardAdapter;
    private MinimapView minimapView;
    
    // Datos para UI
    private List<String> leaderboardData = new ArrayList<>();
    private Map<String, Integer> playerStats = new HashMap<>();
    
    // Configuración visual
    private Paint textPaint;
    private Paint backgroundPaint;
    private Paint borderPaint;
    private int uiColor = 0xFF00FF00; // Verde
    private int backgroundColor = 0x80000000; // Negro semi-transparente
    private float uiPadding = 16f;
    private float textSize = 18f;
    
    // Estado de interacción
    private boolean isDragging = false;
    private float lastTouchX, lastTouchY;
    
    // Constructor para Android
    public GameHUD(Context context) {
        super(context);
        init(context);
    }
    
    public GameHUD(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    public GameHUD(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }
    
    /**
     * Inicialización de componentes
     */
    private void init(Context context) {
        // Configurar paints para dibujo
        textPaint = new Paint();
        textPaint.setColor(uiColor);
        textPaint.setTextSize(textSize);
        textPaint.setTypeface(Typeface.MONOSPACE);
        textPaint.setAntiAlias(true);
        
        backgroundPaint = new Paint();
        backgroundPaint.setColor(backgroundColor);
        backgroundPaint.setStyle(Paint.Style.FILL);
        
        borderPaint = new Paint();
        borderPaint.setColor(uiColor);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f);
        
        // Configurar layout
        setOnTouchListener(this);
        setClickable(true);
        
        // Crear paneles de UI
        createUIPanels(context);
        
        // Configurar actualización periódica
        setupPeriodicUpdate();
    }
    
    /**
     * Crear paneles de la interfaz
     */
    private void createUIPanels(Context context) {
        // Panel superior
        topPanel = new LinearLayout(context);
        topPanel.setOrientation(LinearLayout.HORIZONTAL);
        topPanel.setPadding((int)uiPadding, (int)uiPadding, (int)uiPadding, (int)uiPadding);
        
        scoreText = new TextView(context);
        sizeText = new TextView(context);
        positionText = new TextView(context);
        fpsText = new TextView(context);
        
        topPanel.addView(scoreText);
        topPanel.addView(sizeText);
        topPanel.addView(positionText);
        topPanel.addView(fpsText);
        
        // Panel inferior
        bottomPanel = new LinearLayout(context);
        bottomPanel.setOrientation(LinearLayout.HORIZONTAL);
        bottomPanel.setPadding((int)uiPadding, (int)uiPadding, (int)uiPadding, (int)uiPadding);
        
        timeText = new TextView(context);
        powerUpsText = new TextView(context);
        statusText = new TextView(context);
        
        bottomPanel.addView(timeText);
        bottomPanel.addView(powerUpsText);
        bottomPanel.addView(statusText);
        
        // Panel izquierdo - Leaderboard
        leftPanel = new LinearLayout(context);
        leftPanel.setOrientation(LinearLayout.VERTICAL);
        leftPanel.setPadding((int)uiPadding, (int)uiPadding, (int)uiPadding, (int)uiPadding);
        
        leaderboardAdapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, leaderboardData);
        leaderboardList = new ListView(context);
        leaderboardList.setAdapter(leaderboardAdapter);
        
        leftPanel.addView(leaderboardList);
        
        // Mini-mapa
        minimapView = new MinimapView(context);
        
        addView(topPanel);
        addView(bottomPanel);
        addView(leftPanel);
        addView(minimapView);
    }
    
    /**
     * Establecer referencias del juego
     */
    public void setGameReferences(Player player, GameState gameState, CameraManager cameraManager,
                                  StatisticsManager statisticsManager, VersionInfo versionInfo) {
        this.player = player;
        this.gameState = gameState;
        this.cameraManager = cameraManager;
        this.statisticsManager = statisticsManager;
        this.versionInfo = versionInfo;
        
        minimapView.setReferences(player, gameState, cameraManager);
    }
    
    /**
     * Actualizar información mostrada
     */
    public void updateHUD() {
        if (player == null || gameState == null) return;
        
        try {
            // Actualizar texto de puntuación
            float score = player.getScore();
            scoreText.setText("Puntuación: " + Math.round(score));
            
            // Actualizar tamaño
            float size = player.getRadius();
            sizeText.setText("Tamaño: " + Math.round(size));
            
            // Actualizar posición
            float x = (float)player.getX();
            float y = (float)player.getY();
            positionText.setText("Pos: (" + Math.round(x) + ", " + Math.round(y) + ")");
            
            // Actualizar FPS estimado
            long currentTime = System.currentTimeMillis();
            staticFPSUpdate(currentTime);
            
            // Actualizar tiempo de juego
            if (gameState.getStartTime() > 0) {
                long gameTime = (currentTime - gameState.getStartTime()) / 1000;
                timeText.setText("Tiempo: " + gameTime + "s");
            }
            
            // Actualizar power-ups activos
            int activePowerUps = player.getActivePowerUps().size();
            powerUpsText.setText("Power-ups: " + activePowerUps);
            
            // Actualizar estado del juego
            String status = getGameStatus();
            statusText.setText(status);
            
            // Actualizar leaderboard
            updateLeaderboard();
            
        } catch (Exception e) {
            Log.e(TAG, "Error actualizando HUD", e);
        }
    }
    
    /**
     * Obtener estado actual del juego
     */
    private String getGameStatus() {
        if (gameState == null) return "Inicializando...";
        
        switch (gameState.getCurrentState()) {
            case RUNNING:
                return "Jugando";
            case PAUSED:
                return "Pausado";
            case GAME_OVER:
                return "Game Over";
            case MENU:
                return "Menú";
            default:
                return "Desconocido";
        }
    }
    
    /**
     * Actualizar leaderboard
     */
    private void updateLeaderboard() {
        // Implementación básica del leaderboard
        leaderboardData.clear();
        leaderboardData.add("Jugador: " + Math.round(player.getScore()));
        leaderboardData.add("Tamaño: " + Math.round(player.getRadius()));
        
        if (leaderboardAdapter != null) {
            leaderboardAdapter.notifyDataSetChanged();
        }
    }
    
    /**
     * Actualización estática de FPS
     */
    private void long lastFPSUpdate = 0;
    private int frameCount = 0;
    private float currentFPS = 0;
    
    private void staticFPSUpdate(long currentTime) {
        frameCount++;
        if (currentTime - lastFPSUpdate >= 1000) {
            currentFPS = (frameCount * 1000.0f) / (currentTime - lastFPSUpdate);
            fpsText.setText("FPS: " + Math.round(currentFPS));
            frameCount = 0;
            lastFPSUpdate = currentTime;
        }
    }
    
    /**
     * Configurar actualización periódica
     */
    private void setupPeriodicUpdate() {
        // En una implementación real, esto se haría con un Handler o Timer
        // Por ahora, la actualización se maneja desde el hilo principal del juego
    }
    
    // === MANEJO DE TOUCH ===
    
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isDragging = true;
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                return true;
                
            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    float deltaX = event.getX() - lastTouchX;
                    float deltaY = event.getY() - lastTouchY;
                    
                    // Procesar gestos de cámara o zoom
                    if (cameraManager != null) {
                        cameraManager.handleDrag(deltaX, deltaY);
                    }
                    
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                }
                return true;
                
            case MotionEvent.ACTION_UP:
                isDragging = false;
                return true;
        }
        return false;
    }
    
    // === CLASE INTERNA: MINIMAP ===
    
    /**
     * Vista del mini-mapa
     */
    private static class MinimapView extends View {
        private Player player;
        private GameState gameState;
        private CameraManager cameraManager;
        private Paint minimapPaint;
        private Paint playerPaint;
        private Paint borderPaint;
        private RectF minimapRect;
        
        public MinimapView(Context context) {
            super(context);
            init();
        }
        
        public MinimapView(Context context, AttributeSet attrs) {
            super(context, attrs);
            init();
        }
        
        private void init() {
            minimapPaint = new Paint();
            minimapPaint.setColor(0x4000FF00);
            minimapPaint.setStyle(Paint.Style.FILL);
            
            playerPaint = new Paint();
            playerPaint.setColor(0xFF00FF00);
            playerPaint.setStyle(Paint.Style.FILL);
            
            borderPaint = new Paint();
            borderPaint.setColor(0xFF00FF00);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(2f);
            
            minimapRect = new RectF(10, 10, 110, 110); // 100x100 minimapa
        }
        
        public void setReferences(Player player, GameState gameState, CameraManager cameraManager) {
            this.player = player;
            this.gameState = gameState;
            this.cameraManager = cameraManager;
        }
        
        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            
            if (player == null || gameState == null) return;
            
            // Dibujar fondo del minimapa
            canvas.drawRect(minimapRect, minimapPaint);
            canvas.drawRect(minimapRect, borderPaint);
            
            // Dibujar jugador en el minimapa
            float playerX = minimapRect.left + 50 + (float)(player.getX() / 100.0);
            float playerY = minimapRect.top + 50 + (float)(player.getY() / 100.0);
            
            canvas.drawCircle(playerX, playerY, 3f, playerPaint);
        }
        
        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(120, 120); // Tamaño fijo para el minimapa
        }
    }
}