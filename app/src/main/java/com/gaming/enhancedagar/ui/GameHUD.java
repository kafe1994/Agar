package com.gaming.enhancedagar.ui;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.core.content.ContextCompat;

import com.gaming.enhancedagar.entities.Player;
import com.gaming.enhancedagar.game.GameState;
import com.gaming.enhancedagar.game.CameraManager;

import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * GameHUD - Interfaz de usuario principal durante el juego
 * Proporciona HUD completo con información del jugador, mini-mapa, estadísticas y controles
 */
public class GameHUD extends LinearLayout {
    
    // Configuración del panel
    private static final int HUD_WIDTH = 300;
    private static final int PANEL_HEIGHT = 600;
    private static final int MINIMAP_SIZE = 150;
    private static final int NOTIFICATION_DURATION = 3000;
    private static final int MAX_NOTIFICATIONS = 5;
    
    // Colores del tema (formato Android: 0xAARRGGBB)
    private static final int PRIMARY_COLOR = 0xFF2C3E50;
    private static final int SECONDARY_COLOR = 0xFF3498DB;
    private static final int SUCCESS_COLOR = 0xFF2ECC71;
    private static final int WARNING_COLOR = 0xFFF1C40F;
    private static final int DANGER_COLOR = 0xFFE74C3C;
    private static final int TEXT_COLOR = 0xFFECF0F1;
    private static final int PANEL_COLOR = 0xDD34495E;
    
    // Información del juego
    private Player currentPlayer;
    private GameState gameState;
    private CameraManager cameraManager;
    
    // Componentes UI
    private LinearLayout playerInfoPanel;
    private LinearLayout statsPanel;
    private LinearLayout minimapPanel;
    private LinearLayout playersListPanel;
    private LinearLayout abilitiesPanel;
    private LinearLayout controlsPanel;
    private ScrollView playersScrollView;
    
    // Información del jugador
    private TextView playerNameLabel;
    private TextView playerSizeLabel;
    private TextView playerSpeedLabel;
    private TextView playerRankLabel;
    private ProgressBar healthBar;
    private ProgressBar energyBar;
    private ProgressBar experienceBar;
    
    // Controles
    private Button pauseButton;
    private Button settingsButton;
    private Button[] abilityButtons;
    private LinearLayout notificationsPanel;
    
    // Sistema de notificaciones
    private final Queue<Notification> notificationQueue = new ConcurrentLinkedQueue<>();
    private final Map<String, Timer> activeTimers = new HashMap<>();
    
    // Estadísticas del juego
    private TextView killsLabel;
    private TextView deathsLabel;
    private TextView timeAliveLabel;
    private TextView scoreLabel;
    
    // Lista de jugadores
    private ListView playersList;
    
    // Handler para tareas en UI thread
    private Handler uiHandler;
    
    // Estado del HUD
    private boolean isPaused = false;
    private boolean isVisible = true;
    private long gameStartTime;
    
    /**
     * Constructor del GameHUD
     */
    public GameHUD(Context context) {
        super(context);
        init(context);
    }
    
    public GameHUD(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    public GameHUD(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    
    private void init(Context context) {
        uiHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Inicializa el GameHUD con los parámetros del juego
     */
    public void initializeGame(Player player, GameState gameState, CameraManager cameraManager) {
        this.currentPlayer = player;
        this.gameState = gameState;
        this.cameraManager = cameraManager;
        this.gameStartTime = System.currentTimeMillis();
        
        initializeComponents();
        setupLayout();
        setupEventListeners();
        startUpdateTimer();
    }
    
    /**
     * Inicializa todos los componentes de la UI
     */
    private void initializeComponents() {
        setOrientation(VERTICAL);
        setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10));
        setBackgroundColor(PANEL_COLOR);
        
        // Panel de información del jugador
        initializePlayerInfoPanel();
        
        // Panel de estadísticas
        initializeStatsPanel();
        
        // Panel de mini-mapa
        initializeMinimapPanel();
        
        // Panel de lista de jugadores
        initializePlayersListPanel();
        
        // Panel de habilidades
        initializeAbilitiesPanel();
        
        // Panel de controles
        initializeControlsPanel();
        
        // Panel de notificaciones
        initializeNotificationsPanel();
    }
    
    /**
     * Inicializa el panel de información del jugador
     */
    private void initializePlayerInfoPanel() {
        playerInfoPanel = createPanel("Jugador");
        
        LinearLayout nameLayout = new LinearLayout(getContext());
        nameLayout.setOrientation(LinearLayout.HORIZONTAL);
        TextView nameLabel = createLabel("Nombre:");
        playerNameLabel = createValueLabel(currentPlayer.getName());
        nameLayout.addView(nameLabel);
        nameLayout.addView(playerNameLabel);
        playerInfoPanel.addView(nameLayout);
        
        LinearLayout sizeLayout = new LinearLayout(getContext());
        sizeLayout.setOrientation(LinearLayout.HORIZONTAL);
        TextView sizeLabel = createLabel("Masa:");
        playerSizeLabel = createValueLabel("0");
        sizeLayout.addView(sizeLabel);
        sizeLayout.addView(playerSizeLabel);
        playerInfoPanel.addView(sizeLayout);
        
        LinearLayout speedLayout = new LinearLayout(getContext());
        speedLayout.setOrientation(LinearLayout.HORIZONTAL);
        TextView speedLabel = createLabel("Velocidad:");
        playerSpeedLabel = createValueLabel("0");
        speedLayout.addView(speedLabel);
        speedLayout.addView(playerSpeedLabel);
        playerInfoPanel.addView(speedLayout);
        
        LinearLayout rankLayout = new LinearLayout(getContext());
        rankLayout.setOrientation(LinearLayout.HORIZONTAL);
        TextView rankLabel = createLabel("Rango:");
        playerRankLabel = createValueLabel("#1");
        rankLayout.addView(rankLabel);
        rankLayout.addView(playerRankLabel);
        playerInfoPanel.addView(rankLayout);
        
        TextView healthLabel = createLabel("Vida:");
        playerInfoPanel.addView(healthLabel);
        healthBar = createProgressBar();
        playerInfoPanel.addView(healthBar);
        
        TextView energyLabel = createLabel("Energía:");
        playerInfoPanel.addView(energyLabel);
        energyBar = createProgressBar();
        playerInfoPanel.addView(energyBar);
        
        TextView expLabel = createLabel("Experiencia:");
        playerInfoPanel.addView(expLabel);
        experienceBar = createProgressBar();
        playerInfoPanel.addView(experienceBar);
    }
    
    /**
     * Inicializa el panel de estadísticas del juego
     */
    private void initializeStatsPanel() {
        statsPanel = createPanel("Estadísticas");
        
        LinearLayout killsLayout = new LinearLayout(getContext());
        killsLayout.setOrientation(LinearLayout.HORIZONTAL);
        killsLayout.addView(createLabel("Matados:"));
        killsLabel = createValueLabel("0");
        killsLayout.addView(killsLabel);
        statsPanel.addView(killsLayout);
        
        LinearLayout deathsLayout = new LinearLayout(getContext());
        deathsLayout.setOrientation(LinearLayout.HORIZONTAL);
        deathsLayout.addView(createLabel("Muertes:"));
        deathsLabel = createValueLabel("0");
        deathsLayout.addView(deathsLabel);
        statsPanel.addView(deathsLayout);
        
        LinearLayout timeLayout = new LinearLayout(getContext());
        timeLayout.setOrientation(LinearLayout.HORIZONTAL);
        timeLayout.addView(createLabel("Tiempo vivo:"));
        timeAliveLabel = createValueLabel("00:00");
        timeLayout.addView(timeAliveLabel);
        statsPanel.addView(timeLayout);
        
        LinearLayout scoreLayout = new LinearLayout(getContext());
        scoreLayout.setOrientation(LinearLayout.HORIZONTAL);
        scoreLayout.addView(createLabel("Puntuación:"));
        scoreLabel = createValueLabel("0");
        scoreLayout.addView(scoreLabel);
        statsPanel.addView(scoreLayout);
    }
    
    /**
     * Inicializa el panel del mini-mapa
     */
    private void initializeMinimapPanel() {
        minimapPanel = createPanel("Mini-Mapa");
        
        MinimapView minimapView = new MinimapView(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            dpToPx(MINIMAP_SIZE), dpToPx(MINIMAP_SIZE));
        minimapView.setLayoutParams(params);
        minimapPanel.addView(minimapView);
    }
    
    /**
     * Inicializa el panel de lista de jugadores
     */
    private void initializePlayersListPanel() {
        playersListPanel = createPanel("Jugadores Vivos");
        
        playersList = new ListView(getContext());
        playersScrollView = new ScrollView(getContext());
        playersScrollView.addView(playersList);
        playersListPanel.addView(playersScrollView);
    }
    
    /**
     * Inicializa el panel de habilidades
     */
    private void initializeAbilitiesPanel() {
        abilitiesPanel = createPanel("Habilidades");
        GridLayout gridLayout = new GridLayout(3, 3, dpToPx(5), dpToPx(5));
        abilitiesPanel.setLayout(gridLayout);
        
        String[] abilityNames = {"Velocidad", "División", "Escudo", "Cámara", "Ralentizar", "Atraer", "Empujar", "Explosión", "Radar"};
        abilityButtons = new Button[abilityNames.length];
        
        for (int i = 0; i < abilityNames.length; i++) {
            abilityButtons[i] = createAbilityButton(abilityNames[i]);
            abilitiesPanel.addView(abilityButtons[i]);
        }
    }
    
    /**
     * Inicializa el panel de controles
     */
    private void initializeControlsPanel() {
        controlsPanel = createPanel("");
        controlsPanel.setOrientation(LinearLayout.HORIZONTAL);
        
        pauseButton = createControlButton("⏸️ Pausa", "Pausar/Reanudar juego");
        settingsButton = createControlButton("⚙️ Opciones", "Configuración del juego");
        
        controlsPanel.addView(pauseButton);
        controlsPanel.addView(settingsButton);
    }
    
    /**
     * Inicializa el panel de notificaciones
     */
    private void initializeNotificationsPanel() {
        notificationsPanel = new LinearLayout(getContext());
        notificationsPanel.setOrientation(LinearLayout.VERTICAL);
        notificationsPanel.setBackgroundColor(PANEL_COLOR);
    }
    
    /**
     * Configura el layout principal
     */
    private void setupLayout() {
        // Panel principal con información y estadísticas
        LinearLayout topPanel = new LinearLayout(getContext());
        topPanel.setOrientation(LinearLayout.VERTICAL);
        topPanel.setBackgroundColor(PANEL_COLOR);
        topPanel.addView(playerInfoPanel);
        topPanel.addView(statsPanel);
        
        // Panel central con mini-mapa y lista de jugadores
        LinearLayout centerPanel = new LinearLayout(getContext());
        centerPanel.setOrientation(LinearLayout.HORIZONTAL);
        centerPanel.setBackgroundColor(PANEL_COLOR);
        centerPanel.addView(minimapPanel);
        centerPanel.addView(playersListPanel);
        
        // Panel inferior con habilidades y controles
        LinearLayout bottomPanel = new LinearLayout(getContext());
        bottomPanel.setOrientation(LinearLayout.VERTICAL);
        bottomPanel.setBackgroundColor(PANEL_COLOR);
        bottomPanel.addView(abilitiesPanel);
        bottomPanel.addView(controlsPanel);
        
        // Layout principal
        addView(topPanel);
        addView(centerPanel);
        addView(bottomPanel);
        addView(notificationsPanel);
    }
    
    /**
     * Configura los event listeners
     */
    private void setupEventListeners() {
        // Listener para pausar/reanudar
        pauseButton.setOnClickListener(v -> togglePause());
        
        // Listener para configuraciones
        settingsButton.setOnClickListener(v -> showSettingsDialog());
        
        // Listeners para habilidades
        for (Button button : abilityButtons) {
            if (button != null) {
                button.setOnClickListener(v -> handleAbilityClick(button.getText().toString()));
            }
        }
    }
    
    /**
     * Inicia el timer de actualización del HUD
     */
    private void startUpdateTimer() {
        Runnable updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isVisible && !isPaused) {
                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            updateHUD();
                        }
                    });
                }
                // Programar la siguiente ejecución en 100ms
                uiHandler.postDelayed(this, 100);
            }
        };
        // Iniciar la actualización cada 100ms
        uiHandler.post(updateRunnable);
    }
    
    /**
     * Actualiza toda la información del HUD
     */
    private void updateHUD() {
        if (currentPlayer == null) return;
        
        updatePlayerInfo();
        updateStats();
        updatePlayersList();
        updateNotifications();
    }
    
    /**
     * Actualiza la información del jugador
     */
    private void updatePlayerInfo() {
        // Actualizar valores básicos
        playerNameLabel.setText(currentPlayer.getName());
        playerSizeLabel.setText(String.format("%.1f", currentPlayer.getMass()));
        playerSpeedLabel.setText(String.format("%.1f", currentPlayer.getSpeed()));
        
        // Actualizar barras de progreso
        healthBar.setValue((int) currentPlayer.getHealth());
        healthBar.setMaximum((int) currentPlayer.getMaxHealth());
        
        energyBar.setValue((int) currentPlayer.getEnergy());
        energyBar.setMaximum((int) currentPlayer.getMaxEnergy());
        
        experienceBar.setValue((int) currentPlayer.getExperience());
        experienceBar.setMaximum((int) currentPlayer.getMaxExperience());
        
        // Actualizar rango
        int rank = calculatePlayerRank();
        playerRankLabel.setText("#" + rank);
    }
    
    /**
     * Actualiza las estadísticas del juego
     */
    private void updateStats() {
        if (gameState == null) return;
        
        killsLabel.setText(String.valueOf(gameState.getKillCount()));
        deathsLabel.setText(String.valueOf(gameState.getDeathCount()));
        scoreLabel.setText(String.valueOf(gameState.getScore()));
        
        // Actualizar tiempo vivo
        long timeAlive = (System.currentTimeMillis() - gameStartTime) / 1000;
        timeAliveLabel.setText(formatTime(timeAlive));
    }
    
    /**
     * Actualiza la lista de jugadores
     */
    private void updatePlayersList() {
        if (gameState == null || gameState.getActivePlayers() == null) return;
        
        List<String> playerNames = new ArrayList<>();
        
        // Ordenar jugadores por masa
        List<Player> sortedPlayers = new ArrayList<>(gameState.getActivePlayers());
        sortedPlayers.sort((p1, p2) -> Double.compare(p2.getMass(), p1.getMass()));
        
        // Agregar a la lista
        for (Player player : sortedPlayers) {
            String playerInfo = String.format("%s - %.1f", 
                    player.getName(), player.getMass());
            if (player.equals(currentPlayer)) {
                playerInfo += " (Tú)";
            }
            playerNames.add(playerInfo);
        }
        
        // Usar ArrayAdapter para Android
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), 
            android.R.layout.simple_list_item_1, playerNames);
        playersList.setAdapter(adapter);
    }
    
    /**
     * Actualiza las notificaciones
     */
    private void updateNotifications() {
        // Remover notificaciones expiradas
        notificationQueue.removeIf(notification -> 
                System.currentTimeMillis() > notification.getEndTime());
        
        // Actualizar panel de notificaciones
        notificationsPanel.removeAllViews();
        for (Notification notification : notificationQueue) {
            TextView notificationLabel = createNotificationLabel(notification);
            notificationsPanel.addView(notificationLabel);
            
            // Agregar espacio entre notificaciones
            View spacer = new View(getContext());
            LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(5));
            spacer.setLayoutParams(spacerParams);
            notificationsPanel.addView(spacer);
        }
    }
    
    /**
     * Calcula el rango del jugador actual
     */
    private int calculatePlayerRank() {
        if (gameState == null || gameState.getActivePlayers() == null) return 1;
        
        int rank = 1;
        for (Player player : gameState.getActivePlayers()) {
            if (player.getMass() > currentPlayer.getMass()) {
                rank++;
            }
        }
        return rank;
    }
    
    /**
     * Formatea el tiempo en formato MM:SS
     */
    private String formatTime(long seconds) {
        long minutes = seconds / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }
    
    /**
     * Agrega una nueva notificación
     */
    public void addNotification(String message, NotificationType type) {
        if (notificationQueue.size() >= MAX_NOTIFICATIONS) {
            notificationQueue.poll(); // Remover la más antigua
        }
        
        Notification notification = new Notification(message, type, 
                System.currentTimeMillis() + NOTIFICATION_DURATION);
        notificationQueue.add(notification);
        
        // Handler para remover notificación automáticamente
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                notificationQueue.remove(notification);
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        updateNotifications();
                    }
                });
            }
        }, NOTIFICATION_DURATION);
        
        activeTimers.put(message, timer);
    }
    
    /**
     * Pausa o reanuda el juego
     */
    private void togglePause() {
        isPaused = !isPaused;
        pauseButton.setText(isPaused ? "▶️ Reanudar" : "⏸️ Pausa");
        
        addNotification(isPaused ? "Juego pausado" : "Juego reanudado", 
                isPaused ? NotificationType.WARNING : NotificationType.SUCCESS);
    }
    
    /**
     * Muestra el diálogo de configuración
     */
    private void showSettingsDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setTitle("Configuración");
        
        String[] settings = {
            "Configuraciones del Juego",
            "- Activar/desactivar sonido",
            "- Ajustar calidad gráfica", 
            "- Configurar controles",
            "- Opciones de red"
        };
        
        builder.setItems(settings, (dialog, which) -> {
            // Manejar selección
        });
        
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
    
    /**
     * Maneja el click en una habilidad
     */
    private void handleAbilityClick(String abilityName) {
        if (currentPlayer == null) return;
        
        switch (abilityName) {
            case "Velocidad":
                useSpeedAbility();
                break;
            case "División":
                useDivisionAbility();
                break;
            case "Escudo":
                useShieldAbility();
                break;
            default:
                addNotification("Habilidad " + abilityName + " activada", NotificationType.INFO);
        }
    }
    
    /**
     * Usa la habilidad de velocidad
     */
    private void useSpeedAbility() {
        if (currentPlayer.getEnergy() >= 20) {
            currentPlayer.consumeEnergy(20);
            currentPlayer.setSpeedMultiplier(2.0f);
            addNotification("¡Velocidad activada!", NotificationType.SUCCESS);
            
            // Volver a velocidad normal después de 3 segundos
            uiHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    currentPlayer.setSpeedMultiplier(1.0f);
                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            addNotification("Velocidad desactivada", NotificationType.WARNING);
                        }
                    });
                }
            }, 3000);
        } else {
            addNotification("¡No tienes suficiente energía!", NotificationType.ERROR);
        }
    }
    
    /**
     * Usa la habilidad de división
     */
    private void useDivisionAbility() {
        if (currentPlayer.getMass() >= 50 && currentPlayer.getEnergy() >= 30) {
            currentPlayer.consumeEnergy(30);
            addNotification("¡División activada!", NotificationType.SUCCESS);
            // Lógica de división se maneja en el game engine
        } else {
            addNotification("¡No puedes dividirte!", NotificationType.ERROR);
        }
    }
    
    /**
     * Usa la habilidad de escudo
     */
    private void useShieldAbility() {
        if (currentPlayer.getEnergy() >= 25) {
            currentPlayer.consumeEnergy(25);
            currentPlayer.setShielded(true);
            addNotification("¡Escudo activado!", NotificationType.SUCCESS);
            
            // Desactivar escudo después de 5 segundos
            uiHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    currentPlayer.setShielded(false);
                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            addNotification("Escudo desactivado", NotificationType.WARNING);
                        }
                    });
                }
            }, 5000);
        } else {
            addNotification("¡No tienes suficiente energía!", NotificationType.ERROR);
        }
    }
    
    // Métodos para crear componentes UI
    private TextView createLabel(String text) {
        TextView textView = new TextView(getContext());
        textView.setText(text);
        textView.setTextColor(TEXT_COLOR);
        textView.setTextSize(12);
        textView.setTypeface(null, Typeface.BOLD);
        return textView;
    }
    
    private TextView createValueLabel(String text) {
        TextView textView = new TextView(getContext());
        textView.setText(text);
        textView.setTextColor(SECONDARY_COLOR);
        textView.setTextSize(12);
        return textView;
    }
    
    private ProgressBar createProgressBar() {
        ProgressBar progressBar = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        return progressBar;
    }
    
    private Button createAbilityButton(String text) {
        Button button = new Button(getContext());
        button.setText(text);
        button.setBackgroundColor(PRIMARY_COLOR);
        button.setTextColor(TEXT_COLOR);
        button.setTextSize(10);
        
        button.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                button.setBackgroundColor(SECONDARY_COLOR);
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                button.setBackgroundColor(PRIMARY_COLOR);
            }
            return false;
        });
        
        return button;
    }
    
    private Button createControlButton(String text, String tooltip) {
        Button button = new Button(getContext());
        button.setText(text);
        button.setBackgroundColor(SECONDARY_COLOR);
        button.setTextColor(Color.WHITE);
        button.setTextSize(12);
        button.setTypeface(null, Typeface.BOLD);
        button.setContentDescription(tooltip);
        
        return button;
    }
    
    private TextView createNotificationLabel(Notification notification) {
        TextView label = new TextView(getContext());
        label.setText(notification.getMessage());
        label.setPadding(dpToPx(10), dpToPx(5), dpToPx(10), dpToPx(5));
        
        switch (notification.getType()) {
            case SUCCESS:
                label.setBackgroundColor(SUCCESS_COLOR);
                label.setTextColor(Color.WHITE);
                break;
            case ERROR:
                label.setBackgroundColor(DANGER_COLOR);
                label.setTextColor(Color.WHITE);
                break;
            case WARNING:
                label.setBackgroundColor(WARNING_COLOR);
                label.setTextColor(Color.BLACK);
                break;
            case INFO:
                label.setBackgroundColor(SECONDARY_COLOR);
                label.setTextColor(Color.WHITE);
                break;
        }
        
        return label;
    }
    
    private LinearLayout createPanel(String title) {
        LinearLayout panel = new LinearLayout(getContext());
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundColor(PANEL_COLOR);
        panel.setPadding(dpToPx(10), dpToPx(5), dpToPx(10), dpToPx(5));
        
        if (!title.isEmpty()) {
            TextView titleView = createLabel(title);
            titleView.setTypeface(null, Typeface.BOLD);
            panel.addView(titleView);
        }
        
        return panel;
    }
    
    // Método para convertir dp a píxeles
    private int dpToPx(int dp) {
        return (int) (dp * getContext().getResources().getDisplayMetrics().density);
    }
    
    
    // Clase personalizada para el mini-mapa
    private class MinimapView extends View {
        private Paint paint;
        private Paint borderPaint;
        
        public MinimapView(Context context) {
            super(context);
            init();
        }
        
        private void init() {
            paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(2);
        }
        
        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            
            // Dibujar fondo del mini-mapa
            paint.setColor(0xFF141414);
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            
            // Dibujar bordes
            borderPaint.setColor(SECONDARY_COLOR);
            canvas.drawRect(0, 0, getWidth() - 1, getHeight() - 1, borderPaint);
            
            if (gameState != null && cameraManager != null) {
                drawMiniMapContent(canvas);
            }
        }
        
        private void drawMiniMapContent(Canvas canvas) {
            // Dibujar jugadores en el mini-mapa
            if (gameState.getActivePlayers() != null) {
                for (Player player : gameState.getActivePlayers()) {
                    PointF playerPos = worldToMinimap(player.getX(), player.getY());
                    
                    if (player.equals(currentPlayer)) {
                        paint.setColor(SUCCESS_COLOR);
                        canvas.drawRect(playerPos.x - 3, playerPos.y - 3, 6, 6, paint);
                    } else {
                        paint.setColor(DANGER_COLOR);
                        canvas.drawRect(playerPos.x - 2, playerPos.y - 2, 4, 4, paint);
                    }
                }
            }
            
            // Dibujar comida en el mini-mapa (solo algunos puntos)
            if (gameState.getFood() != null && gameState.getFood().size() > 0) {
                paint.setColor(WARNING_COLOR);
                for (int i = 0; i < Math.min(50, gameState.getFood().size()); i += 10) {
                    // Dibujar algunos puntos de comida representativos
                }
            }
            
            // Dibujar viewport del jugador
            if (cameraManager != null) {
                paint.setColor(SECONDARY_COLOR);
                Paint viewportPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                viewportPaint.setStyle(Paint.Style.STROKE);
                viewportPaint.setStrokeWidth(1);
                viewportPaint.setColor(SECONDARY_COLOR);
                
                RectF viewport = cameraManager.getViewportBounds();
                PointF topLeft = worldToMinimap(viewport.left, viewport.top);
                PointF bottomRight = worldToMinimap(viewport.right, viewport.bottom);
                
                canvas.drawRect(topLeft.x, topLeft.y, 
                        bottomRight.x, bottomRight.y, viewportPaint);
            }
        }
        
        private PointF worldToMinimap(double worldX, double worldY) {
            if (gameState == null || cameraManager == null) {
                return new PointF(0, 0);
            }
            
            // Convertir coordenadas del mundo a coordenadas del mini-mapa
            float mapX = (float) ((worldX / gameState.getWorldWidth()) * (MINIMAP_SIZE - 20)) + 10;
            float mapY = (float) ((worldY / gameState.getWorldHeight()) * (MINIMAP_SIZE - 20)) + 10;
            
            return new PointF(mapX, mapY);
        }
    }
    
    // Clase para representar notificaciones
    private static class Notification {
        private final String message;
        private final NotificationType type;
        private final long endTime;
        
        public Notification(String message, NotificationType type, long endTime) {
            this.message = message;
            this.type = type;
            this.endTime = endTime;
        }
        
        public String getMessage() { return message; }
        public NotificationType getType() { return type; }
        public long getEndTime() { return endTime; }
    }
    
    // Enum para tipos de notificaciones
    public enum NotificationType {
        SUCCESS, ERROR, WARNING, INFO
    }
    
    // Getters para acceso desde otras clases
    public boolean isPaused() { return isPaused; }
    public void setPaused(boolean paused) { isPaused = paused; }
    public boolean isVisible() { return isVisible; }
    public void setVisible(boolean visible) { isVisible = visible; }
    
    // Método para limpiar recursos
    public void cleanup() {
        for (Timer timer : activeTimers.values()) {
            timer.cancel();
        }
        activeTimers.clear();
        notificationQueue.clear();
        
        // Limpiar handler
        if (uiHandler != null) {
            uiHandler.removeCallbacksAndMessages(null);
        }
    }
}