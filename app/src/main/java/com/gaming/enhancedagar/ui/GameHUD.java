package com.gaming.enhancedagar.ui;

import com.gaming.enhancedagar.entities.Player;
import com.gaming.enhancedagar.game.GameState;
import com.gaming.enhancedagar.game.CameraManager;

import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.graphics.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.ArrayList;

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
    
    // Información del juego
    private Player currentPlayer;
    private GameState gameState;
    private CameraManager cameraManager;
    
    // Componentes UI (versión Android)
    private LinearLayout playerInfoPanel;
    private LinearLayout statsPanel;
    private LinearLayout minimapPanel;
    private LinearLayout playersListPanel;
    private LinearLayout abilitiesPanel;
    private LinearLayout controlsPanel;
    private ScrollView playersScrollPane;
    
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
    private ListView<String> playersList;
    private ArrayList<String> playersListData;
    
    // Colores del tema
    private static final int PRIMARY_COLOR = android.graphics.Color.rgb(44, 62, 80);
    private static final int SECONDARY_COLOR = android.graphics.Color.rgb(52, 152, 219);
    private static final int SUCCESS_COLOR = android.graphics.Color.rgb(46, 204, 113);
    private static final int WARNING_COLOR = android.graphics.Color.rgb(241, 196, 15);
    private static final int DANGER_COLOR = android.graphics.Color.rgb(231, 76, 60);
    private static final int TEXT_COLOR = android.graphics.Color.rgb(236, 240, 241);
    private static final int PANEL_COLOR = android.graphics.Color.argb(220, 52, 73, 94);
    
    // Estado del HUD
    private boolean isPaused = false;
    private boolean isVisible = true;
    private long gameStartTime;
    
    /**
     * Constructor del GameHUD
     */
    public GameHUD(Context context, Player player, GameState gameState, CameraManager cameraManager) {
        super(context);
        this.currentPlayer = player;
        this.gameState = gameState;
        this.cameraManager = cameraManager;
        this.gameStartTime = System.currentTimeMillis();
        this.playersListData = new ArrayList<>();
        
        setOrientation(VERTICAL);
        setPadding(16, 16, 16, 16);
        setBackgroundColor(PANEL_COLOR);
        initializeComponents();
        setupEventListeners();
        startUpdateTimer();
    }
    
    /**
     * Constructor XML para Android
     */
    public GameHUD(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        setPadding(16, 16, 16, 16);
        setBackgroundColor(PANEL_COLOR);
        initializeComponents();
        setupEventListeners();
    }
    
    /**
     * Inicializa todos los componentes de la UI
     */
    private void initializeComponents() {
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
        playerInfoPanel = new LinearLayout(getContext());
        playerInfoPanel.setOrientation(LinearLayout.VERTICAL);
        playerInfoPanel.setPadding(16, 16, 16, 16);
        playerInfoPanel.setBackgroundColor(PANEL_COLOR);
        
        // Título del panel
        TextView titleLabel = new TextView(getContext());
        titleLabel.setText("Jugador");
        titleLabel.setTextColor(SECONDARY_COLOR);
        titleLabel.setTextSize(18);
        titleLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        playerInfoPanel.addView(titleLabel);
        
        // Nombre del jugador
        addLabelValuePair(playerInfoPanel, "Nombre:", currentPlayer.getName());
        
        // Tamaño/Masa
        addLabelValuePair(playerInfoPanel, "Masa:", "0");
        
        // Velocidad
        addLabelValuePair(playerInfoPanel, "Velocidad:", "0");
        
        // Rango
        addLabelValuePair(playerInfoPanel, "Rango:", "#1");
        
        // Barra de vida
        TextView healthLabel = new TextView(getContext());
        healthLabel.setText("Vida:");
        healthLabel.setTextColor(TEXT_COLOR);
        healthLabel.setTextSize(14);
        playerInfoPanel.addView(healthLabel);
        
        healthBar = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleHorizontal);
        healthBar.setMax(100);
        healthBar.setProgress(100);
        playerInfoPanel.addView(healthBar);
        
        // Barra de energía
        TextView energyLabel = new TextView(getContext());
        energyLabel.setText("Energía:");
        energyLabel.setTextColor(TEXT_COLOR);
        energyLabel.setTextSize(14);
        playerInfoPanel.addView(energyLabel);
        
        energyBar = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleHorizontal);
        energyBar.setMax(100);
        energyBar.setProgress(100);
        playerInfoPanel.addView(energyBar);
        
        // Barra de experiencia
        TextView expLabel = new TextView(getContext());
        expLabel.setText("Experiencia:");
        expLabel.setTextColor(TEXT_COLOR);
        expLabel.setTextSize(14);
        playerInfoPanel.addView(expLabel);
        
        experienceBar = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleHorizontal);
        experienceBar.setMax(100);
        experienceBar.setProgress(0);
        playerInfoPanel.addView(experienceBar);
    }
    
    /**
     * Inicializa el panel de estadísticas del juego
     */
    private void initializeStatsPanel() {
        statsPanel = new LinearLayout(getContext());
        statsPanel.setOrientation(LinearLayout.VERTICAL);
        statsPanel.setPadding(16, 16, 16, 16);
        statsPanel.setBackgroundColor(PANEL_COLOR);
        
        // Título del panel
        TextView titleLabel = new TextView(getContext());
        titleLabel.setText("Estadísticas");
        titleLabel.setTextColor(SECONDARY_COLOR);
        titleLabel.setTextSize(18);
        titleLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        statsPanel.addView(titleLabel);
        
        // Matados
        addLabelValuePair(statsPanel, "Matados:", "0");
        
        // Muertes
        addLabelValuePair(statsPanel, "Muertes:", "0");
        
        // Tiempo vivo
        addLabelValuePair(statsPanel, "Tiempo vivo:", "00:00");
        
        // Puntuación
        addLabelValuePair(statsPanel, "Puntuación:", "0");
    }
    
    /**
     * Inicializa el panel del mini-mapa
     */
    private void initializeMinimapPanel() {
        minimapPanel = new LinearLayout(getContext());
        minimapPanel.setOrientation(LinearLayout.VERTICAL);
        minimapPanel.setPadding(16, 16, 16, 16);
        minimapPanel.setBackgroundColor(PANEL_COLOR);
        
        // Título del panel
        TextView titleLabel = new TextView(getContext());
        titleLabel.setText("Mini-Mapa");
        titleLabel.setTextColor(SECONDARY_COLOR);
        titleLabel.setTextSize(18);
        titleLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        minimapPanel.addView(titleLabel);
        
        // Mini-mapa simplificado usando ImageView
        ImageView minimapView = new ImageView(getContext());
        minimapView.setLayoutParams(new LinearLayout.LayoutParams(MINIMAP_SIZE, MINIMAP_SIZE));
        minimapView.setBackgroundColor(android.graphics.Color.rgb(20, 20, 20));
        minimapView.setScaleType(ImageView.ScaleType.CENTER);
        minimapPanel.addView(minimapView);
    }
    
    /**
     * Inicializa el panel de lista de jugadores
     */
    private void initializePlayersListPanel() {
        playersListPanel = new LinearLayout(getContext());
        playersListPanel.setOrientation(LinearLayout.VERTICAL);
        playersListPanel.setPadding(16, 16, 16, 16);
        playersListPanel.setBackgroundColor(PANEL_COLOR);
        
        // Título del panel
        TextView titleLabel = new TextView(getContext());
        titleLabel.setText("Jugadores Vivos");
        titleLabel.setTextColor(SECONDARY_COLOR);
        titleLabel.setTextSize(18);
        titleLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        playersListPanel.addView(titleLabel);
        
        // Lista de jugadores
        playersListData = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), 
                android.R.layout.simple_list_item_1, playersListData);
        playersList = new ListView<>(getContext());
        playersList.setAdapter(adapter);
        playersList.setBackgroundColor(android.graphics.Color.rgb(44, 62, 80));
        playersListPanel.addView(playersList);
    }
    
    /**
     * Inicializa el panel de habilidades
     */
    private void initializeAbilitiesPanel() {
        abilitiesPanel = new LinearLayout(getContext());
        abilitiesPanel.setOrientation(LinearLayout.VERTICAL);
        abilitiesPanel.setPadding(16, 16, 16, 16);
        abilitiesPanel.setBackgroundColor(PANEL_COLOR);
        
        // Título del panel
        TextView titleLabel = new TextView(getContext());
        titleLabel.setText("Habilidades");
        titleLabel.setTextColor(SECONDARY_COLOR);
        titleLabel.setTextSize(18);
        titleLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        abilitiesPanel.addView(titleLabel);
        
        // Grid de habilidades
        GridLayout abilitiesGrid = new GridLayout(getContext());
        abilitiesGrid.setRowCount(3);
        abilitiesGrid.setColumnCount(3);
        abilitiesGrid.setPadding(8, 8, 8, 8);
        
        String[] abilityNames = {"Velocidad", "División", "Escudo", "Cámara", "Ralentizar", "Atraer", "Empujar", "Explosión", "Radar"};
        abilityButtons = new Button[abilityNames.length];
        
        for (int i = 0; i < abilityNames.length; i++) {
            abilityButtons[i] = new Button(getContext());
            abilityButtons[i].setText(abilityNames[i]);
            abilityButtons[i].setBackgroundColor(android.graphics.Color.rgb(44, 62, 80));
            abilityButtons[i].setTextColor(TEXT_COLOR);
            abilityButtons[i].setTextSize(12);
            
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(i % 3, 1f);
            params.rowSpec = GridLayout.spec(i / 3, 1f);
            params.setMargins(2, 2, 2, 2);
            abilityButtons[i].setLayoutParams(params);
            
            abilitiesGrid.addView(abilityButtons[i]);
        }
        
        abilitiesPanel.addView(abilitiesGrid);
    }
    
    /**
     * Inicializa el panel de controles
     */
    private void initializeControlsPanel() {
        controlsPanel = new LinearLayout(getContext());
        controlsPanel.setOrientation(LinearLayout.HORIZONTAL);
        controlsPanel.setPadding(16, 16, 16, 16);
        controlsPanel.setBackgroundColor(PANEL_COLOR);
        controlsPanel.setGravity(android.view.Gravity.CENTER);
        
        pauseButton = new Button(getContext());
        pauseButton.setText("⏸️ Pausa");
        pauseButton.setBackgroundColor(SECONDARY_COLOR);
        pauseButton.setTextColor(android.graphics.Color.WHITE);
        pauseButton.setPadding(16, 8, 16, 8);
        pauseButton.setTextSize(14);
        
        settingsButton = new Button(getContext());
        settingsButton.setText("⚙️ Opciones");
        settingsButton.setBackgroundColor(SECONDARY_COLOR);
        settingsButton.setTextColor(android.graphics.Color.WHITE);
        settingsButton.setPadding(16, 8, 16, 8);
        settingsButton.setTextSize(14);
        
        controlsPanel.addView(pauseButton);
        controlsPanel.addView(settingsButton);
    }
    
    /**
     * Inicializa el panel de notificaciones
     */
    private void initializeNotificationsPanel() {
        notificationsPanel = new LinearLayout(getContext());
        notificationsPanel.setOrientation(LinearLayout.VERTICAL);
        notificationsPanel.setPadding(16, 16, 16, 16);
        notificationsPanel.setBackgroundColor(PANEL_COLOR);
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
            button.setOnClickListener(v -> handleAbilityClick(button.getText().toString()));
        }
    }
    
    /**
     * Inicia el timer de actualización del HUD
     */
    private void startUpdateTimer() {
        Timer updateTimer = new Timer();
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (isVisible && !isPaused) {
                    // En Android, usamos post() para ejecutar en el thread UI
                    post(() -> updateHUD());
                }
            }
        }, 0, 100); // Actualizar cada 100ms
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
        
        playersListData.clear();
        
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
            playersListData.add(playerInfo);
        }
        
        // Notificar al adapter que los datos cambiaron
        if (playersList.getAdapter() != null) {
            ((ArrayAdapter<String>) playersList.getAdapter()).notifyDataSetChanged();
        }
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
            TextView notificationText = createNotificationTextView(notification);
            notificationsPanel.addView(notificationText);
            
            // Agregar espacio entre notificaciones
            TextView spacer = new TextView(getContext());
            spacer.setText(" ");
            spacer.setTextSize(4);
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
        
        // Timer para remover notificación automáticamente
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                notificationQueue.remove(notification);
                // En Android, usamos post() para ejecutar en el thread UI
                post(() -> updateNotifications());
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
        // Crear diálogo simple para Android
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Configuración");
        
        String[] options = {
            "Configuraciones del Juego",
            "- Activar/desactivar sonido",
            "- Ajustar calidad gráfica", 
            "- Configurar controles",
            "- Opciones de red"
        };
        
        builder.setItems(options, (dialog, which) -> {
            // Manejar selección de opciones
            addNotification("Configuración: " + options[which], NotificationType.INFO);
        });
        
        builder.setPositiveButton("Cerrar", null);
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
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    currentPlayer.setSpeedMultiplier(1.0f);
                    // En Android, usamos post() para ejecutar en el thread UI
                    post(() -> addNotification("Velocidad desactivada", NotificationType.WARNING));
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
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    currentPlayer.setShielded(false);
                    // En Android, usamos post() para ejecutar en el thread UI
                    post(() -> addNotification("Escudo desactivado", NotificationType.WARNING));
                }
            }, 5000);
        } else {
            addNotification("¡No tienes suficiente energía!", NotificationType.ERROR);
        }
    }
    
    /**
     * Crea un TextView para una notificación
     */
    private TextView createNotificationTextView(Notification notification) {
        TextView textView = new TextView(getContext());
        textView.setText(notification.getMessage());
        textView.setPadding(8, 4, 8, 4);
        textView.setTextSize(12);
        
        switch (notification.getType()) {
            case SUCCESS:
                textView.setBackgroundColor(android.graphics.Color.argb(200, 46, 204, 113));
                textView.setTextColor(android.graphics.Color.WHITE);
                break;
            case ERROR:
                textView.setBackgroundColor(android.graphics.Color.argb(200, 231, 76, 60));
                textView.setTextColor(android.graphics.Color.WHITE);
                break;
            case WARNING:
                textView.setBackgroundColor(android.graphics.Color.argb(200, 241, 196, 15));
                textView.setTextColor(android.graphics.Color.BLACK);
                break;
            case INFO:
                textView.setBackgroundColor(android.graphics.Color.argb(200, 52, 152, 219));
                textView.setTextColor(android.graphics.Color.WHITE);
                break;
        }
        
        return textView;
    }
    
    /**
     * Método helper para agregar un par label-valor a un panel
     */
    private void addLabelValuePair(LinearLayout panel, String label, String value) {
        LinearLayout rowLayout = new LinearLayout(getContext());
        rowLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        TextView labelView = new TextView(getContext());
        labelView.setText(label);
        labelView.setTextColor(TEXT_COLOR);
        labelView.setTextSize(14);
        labelView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        
        TextView valueView = new TextView(getContext());
        valueView.setText(value);
        valueView.setTextColor(SECONDARY_COLOR);
        valueView.setTextSize(14);
        valueView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        
        rowLayout.addView(labelView);
        rowLayout.addView(valueView);
        panel.addView(rowLayout);
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
    }
}