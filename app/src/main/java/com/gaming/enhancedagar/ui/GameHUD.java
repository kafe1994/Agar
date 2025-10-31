package com.gaming.enhancedagar.ui;

import com.gaming.enhancedagar.entities.Player;
import com.gaming.enhancedagar.game.GameState;
import com.gaming.enhancedagar.game.CameraManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * GameHUD - Interfaz de usuario principal durante el juego
 * Proporciona HUD completo con información del jugador, mini-mapa, estadísticas y controles
 */
public class GameHUD extends JPanel implements KeyListener, MouseListener {
    
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
    
    // Componentes UI
    private JPanel playerInfoPanel;
    private JPanel statsPanel;
    private JPanel minimapPanel;
    private JPanel playersListPanel;
    private JPanel abilitiesPanel;
    private JPanel controlsPanel;
    private JScrollPane playersScrollPane;
    
    // Información del jugador
    private JLabel playerNameLabel;
    private JLabel playerSizeLabel;
    private JLabel playerSpeedLabel;
    private JLabel playerRankLabel;
    private JProgressBar healthBar;
    private JProgressBar energyBar;
    private JProgressBar experienceBar;
    
    // Controles
    private JButton pauseButton;
    private JButton settingsButton;
    private JButton[] abilityButtons;
    private JPanel notificationsPanel;
    
    // Sistema de notificaciones
    private final Queue<Notification> notificationQueue = new ConcurrentLinkedQueue<>();
    private final Map<String, Timer> activeTimers = new HashMap<>();
    
    // Estadísticas del juego
    private JLabel killsLabel;
    private JLabel deathsLabel;
    private JLabel timeAliveLabel;
    private JLabel scoreLabel;
    
    // Lista de jugadores
    private DefaultListModel<String> playersListModel;
    private JList<String> playersList;
    
    // Colores del tema
    private static final Color PRIMARY_COLOR = new Color(44, 62, 80);
    private static final Color SECONDARY_COLOR = new Color(52, 152, 219);
    private static final Color SUCCESS_COLOR = new Color(46, 204, 113);
    private static final Color WARNING_COLOR = new Color(241, 196, 15);
    private static final Color DANGER_COLOR = new Color(231, 76, 60);
    private static final Color TEXT_COLOR = new Color(236, 240, 241);
    private static final Color PANEL_COLOR = new Color(52, 73, 94, 220);
    
    // Estado del HUD
    private boolean isPaused = false;
    private boolean isVisible = true;
    private long gameStartTime;
    
    /**
     * Constructor del GameHUD
     */
    public GameHUD(Player player, GameState gameState, CameraManager cameraManager) {
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
        setLayout(new BorderLayout());
        setBackground(PANEL_COLOR);
        setPreferredSize(new Dimension(HUD_WIDTH, PANEL_HEIGHT));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        
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
        playerInfoPanel = new JPanel(new GridBagLayout());
        playerInfoPanel.setBackground(PANEL_COLOR);
        playerInfoPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(SECONDARY_COLOR, 2), "Jugador"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Nombre del jugador
        gbc.gridx = 0; gbc.gridy = 0;
        playerInfoPanel.add(createLabel("Nombre:"), gbc);
        gbc.gridx = 1;
        playerNameLabel = createValueLabel(currentPlayer.getName());
        playerInfoPanel.add(playerNameLabel, gbc);
        
        // Tamaño/Masa
        gbc.gridx = 0; gbc.gridy = 1;
        playerInfoPanel.add(createLabel("Masa:"), gbc);
        gbc.gridx = 1;
        playerSizeLabel = createValueLabel("0");
        playerInfoPanel.add(playerSizeLabel, gbc);
        
        // Velocidad
        gbc.gridx = 0; gbc.gridy = 2;
        playerInfoPanel.add(createLabel("Velocidad:"), gbc);
        gbc.gridx = 1;
        playerSpeedLabel = createValueLabel("0");
        playerInfoPanel.add(playerSpeedLabel, gbc);
        
        // Rango
        gbc.gridx = 0; gbc.gridy = 3;
        playerInfoPanel.add(createLabel("Rango:"), gbc);
        gbc.gridx = 1;
        playerRankLabel = createValueLabel("#1");
        playerInfoPanel.add(playerRankLabel, gbc);
        
        // Barra de vida
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        playerInfoPanel.add(createLabel("Vida:"), gbc);
        gbc.gridy = 5;
        healthBar = createProgressBar();
        healthBar.setBackground(DANGER_COLOR);
        healthBar.setForeground(SUCCESS_COLOR);
        playerInfoPanel.add(healthBar, gbc);
        
        // Barra de energía
        gbc.gridy = 6;
        playerInfoPanel.add(createLabel("Energía:"), gbc);
        gbc.gridy = 7;
        energyBar = createProgressBar();
        energyBar.setBackground(WARNING_COLOR);
        energyBar.setForeground(SECONDARY_COLOR);
        playerInfoPanel.add(energyBar, gbc);
        
        // Barra de experiencia
        gbc.gridy = 8;
        playerInfoPanel.add(createLabel("Experiencia:"), gbc);
        gbc.gridy = 9;
        experienceBar = createProgressBar();
        experienceBar.setBackground(new Color(155, 89, 182));
        experienceBar.setForeground(new Color(142, 68, 173));
        playerInfoPanel.add(experienceBar, gbc);
    }
    
    /**
     * Inicializa el panel de estadísticas del juego
     */
    private void initializeStatsPanel() {
        statsPanel = new JPanel(new GridBagLayout());
        statsPanel.setBackground(PANEL_COLOR);
        statsPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(SECONDARY_COLOR, 2), "Estadísticas"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Matados
        gbc.gridx = 0; gbc.gridy = 0;
        statsPanel.add(createLabel("Matados:"), gbc);
        gbc.gridx = 1;
        killsLabel = createValueLabel("0");
        statsPanel.add(killsLabel, gbc);
        
        // Muertes
        gbc.gridx = 0; gbc.gridy = 1;
        statsPanel.add(createLabel("Muertes:"), gbc);
        gbc.gridx = 1;
        deathsLabel = createValueLabel("0");
        statsPanel.add(deathsLabel, gbc);
        
        // Tiempo vivo
        gbc.gridx = 0; gbc.gridy = 2;
        statsPanel.add(createLabel("Tiempo vivo:"), gbc);
        gbc.gridx = 1;
        timeAliveLabel = createValueLabel("00:00");
        statsPanel.add(timeAliveLabel, gbc);
        
        // Puntuación
        gbc.gridx = 0; gbc.gridy = 3;
        statsPanel.add(createLabel("Puntuación:"), gbc);
        gbc.gridx = 1;
        scoreLabel = createValueLabel("0");
        statsPanel.add(scoreLabel, gbc);
    }
    
    /**
     * Inicializa el panel del mini-mapa
     */
    private void initializeMinimapPanel() {
        minimapPanel = new JPanel(new BorderLayout());
        minimapPanel.setBackground(PANEL_COLOR);
        minimapPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(SECONDARY_COLOR, 2), "Mini-Mapa"));
        
        MinimapComponent minimapComponent = new MinimapComponent();
        minimapComponent.setPreferredSize(new Dimension(MINIMAP_SIZE, MINIMAP_SIZE));
        minimapPanel.add(minimapComponent, BorderLayout.CENTER);
    }
    
    /**
     * Inicializa el panel de lista de jugadores
     */
    private void initializePlayersListPanel() {
        playersListPanel = new JPanel(new BorderLayout());
        playersListPanel.setBackground(PANEL_COLOR);
        playersListPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(SECONDARY_COLOR, 2), "Jugadores Vivos"));
        
        playersListModel = new DefaultListModel<>();
        playersList = new JList<>(playersListModel);
        playersList.setBackground(new Color(44, 62, 80));
        playersList.setForeground(TEXT_COLOR);
        playersList.setSelectionBackground(SECONDARY_COLOR);
        playersList.setSelectionForeground(Color.WHITE);
        
        playersScrollPane = new JScrollPane(playersList);
        playersScrollPane.setBackground(PANEL_COLOR);
        playersScrollPane.setBorder(null);
        
        playersListPanel.add(playersScrollPane, BorderLayout.CENTER);
    }
    
    /**
     * Inicializa el panel de habilidades
     */
    private void initializeAbilitiesPanel() {
        abilitiesPanel = new JPanel(new GridLayout(3, 3, 5, 5));
        abilitiesPanel.setBackground(PANEL_COLOR);
        abilitiesPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(SECONDARY_COLOR, 2), "Habilidades"));
        
        String[] abilityNames = {"Velocidad", "División", "Escudo", "Cámara", "Ralentizar", "Atraer", "Empujar", "Explosión", "Radar"};
        abilityButtons = new JButton[abilityNames.length];
        
        for (int i = 0; i < abilityNames.length; i++) {
            abilityButtons[i] = createAbilityButton(abilityNames[i]);
            abilitiesPanel.add(abilityButtons[i]);
        }
    }
    
    /**
     * Inicializa el panel de controles
     */
    private void initializeControlsPanel() {
        controlsPanel = new JPanel(new FlowLayout());
        controlsPanel.setBackground(PANEL_COLOR);
        
        pauseButton = createControlButton("⏸️ Pausa", "Pausar/Reanudar juego");
        settingsButton = createControlButton("⚙️ Opciones", "Configuración del juego");
        
        controlsPanel.add(pauseButton);
        controlsPanel.add(settingsButton);
    }
    
    /**
     * Inicializa el panel de notificaciones
     */
    private void initializeNotificationsPanel() {
        notificationsPanel = new JPanel();
        notificationsPanel.setLayout(new BoxLayout(notificationsPanel, BoxLayout.Y_AXIS));
        notificationsPanel.setBackground(PANEL_COLOR);
    }
    
    /**
     * Configura el layout principal
     */
    private void setupLayout() {
        JScrollPane mainScrollPane = new JScrollPane(this);
        mainScrollPane.setBorder(null);
        mainScrollPane.getViewport().setBackground(PANEL_COLOR);
        
        // Panel principal con información y estadísticas
        JPanel topPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        topPanel.setBackground(PANEL_COLOR);
        topPanel.add(playerInfoPanel);
        topPanel.add(statsPanel);
        
        // Panel central con mini-mapa y lista de jugadores
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        centerPanel.setBackground(PANEL_COLOR);
        centerPanel.add(minimapPanel);
        centerPanel.add(playersListPanel);
        
        // Panel inferior con habilidades y controles
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.setBackground(PANEL_COLOR);
        bottomPanel.add(abilitiesPanel, BorderLayout.CENTER);
        bottomPanel.add(controlsPanel, BorderLayout.SOUTH);
        
        // Layout principal
        setLayout(new BorderLayout(5, 5));
        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
        add(notificationsPanel, BorderLayout.EAST);
    }
    
    /**
     * Configura los event listeners
     */
    private void setupEventListeners() {
        // Listener para pausar/reanudar
        pauseButton.addActionListener(e -> togglePause());
        
        // Listener para configuraciones
        settingsButton.addActionListener(e -> showSettingsDialog());
        
        // Listeners para habilidades
        for (JButton button : abilityButtons) {
            button.addActionListener(e -> handleAbilityClick(button.getText()));
        }
        
        // Listener global para teclas
        addKeyListener(this);
        setFocusable(true);
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
                    SwingUtilities.invokeLater(() -> updateHUD());
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
        
        playersListModel.clear();
        
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
            playersListModel.addElement(playerInfo);
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
        notificationsPanel.removeAll();
        for (Notification notification : notificationQueue) {
            JLabel notificationLabel = createNotificationLabel(notification);
            notificationsPanel.add(notificationLabel);
            notificationsPanel.add(Box.createVerticalStrut(5));
        }
        
        notificationsPanel.revalidate();
        notificationsPanel.repaint();
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
                SwingUtilities.invokeLater(() -> updateNotifications());
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
        JDialog settingsDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Configuración");
        settingsDialog.setSize(400, 300);
        settingsDialog.setLocationRelativeTo(this);
        
        JPanel settingsPanel = new JPanel(new GridLayout(0, 1));
        settingsPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Configuraciones básicas
        settingsPanel.add(new JLabel("Configuraciones del Juego"));
        settingsPanel.add(new JLabel("- Activar/desactivar sonido"));
        settingsPanel.add(new JLabel("- Ajustar calidad gráfica"));
        settingsPanel.add(new JLabel("- Configurar controles"));
        settingsPanel.add(new JLabel("- Opciones de red"));
        
        settingsDialog.add(settingsPanel);
        settingsDialog.setVisible(true);
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
                    SwingUtilities.invokeLater(() -> addNotification("Velocidad desactivada", NotificationType.WARNING));
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
                    SwingUtilities.invokeLater(() -> addNotification("Escudo desactivado", NotificationType.WARNING));
                }
            }, 5000);
        } else {
            addNotification("¡No tienes suficiente energía!", NotificationType.ERROR);
        }
    }
    
    // Métodos para crear componentes UI
    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_COLOR);
        label.setFont(new Font("Arial", Font.BOLD, 12));
        return label;
    }
    
    private JLabel createValueLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(SECONDARY_COLOR);
        label.setFont(new Font("Arial", Font.PLAIN, 12));
        return label;
    }
    
    private JProgressBar createProgressBar() {
        JProgressBar progressBar = new JProgressBar();
        progressBar.setBackground(new Color(52, 73, 94));
        progressBar.setForeground(SECONDARY_COLOR);
        progressBar.setBorder(BorderFactory.createLineBorder(SECONDARY_COLOR));
        return progressBar;
    }
    
    private JButton createAbilityButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(new Color(44, 62, 80));
        button.setForeground(TEXT_COLOR);
        button.setBorder(BorderFactory.createLineBorder(SECONDARY_COLOR));
        button.setFont(new Font("Arial", Font.PLAIN, 10));
        button.setFocusPainted(false);
        
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(SECONDARY_COLOR);
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(44, 62, 80));
            }
        });
        
        return button;
    }
    
    private JButton createControlButton(String text, String tooltip) {
        JButton button = new JButton(text);
        button.setBackground(SECONDARY_COLOR);
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createRaisedBevelBorder());
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setToolTipText(tooltip);
        
        return button;
    }
    
    private JLabel createNotificationLabel(Notification notification) {
        JLabel label = new JLabel(notification.getMessage());
        label.setOpaque(true);
        label.setBorder(new EmptyBorder(5, 10, 5, 10));
        
        switch (notification.getType()) {
            case SUCCESS:
                label.setBackground(new Color(46, 204, 113, 200));
                label.setForeground(Color.WHITE);
                break;
            case ERROR:
                label.setBackground(new Color(231, 76, 60, 200));
                label.setForeground(Color.WHITE);
                break;
            case WARNING:
                label.setBackground(new Color(241, 196, 15, 200));
                label.setForeground(Color.BLACK);
                break;
            case INFO:
                label.setBackground(new Color(52, 152, 219, 200));
                label.setForeground(Color.WHITE);
                break;
        }
        
        return label;
    }
    
    // Componente personalizado para el mini-mapa
    private class MinimapComponent extends JComponent {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.ANTIALIASING, true);
            
            // Dibujar fondo del mini-mapa
            g2d.setColor(new Color(20, 20, 20));
            g2d.fillRect(0, 0, getWidth(), getHeight());
            
            // Dibujar bordes
            g2d.setColor(SECONDARY_COLOR);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            
            if (gameState != null && cameraManager != null) {
                drawMiniMapContent(g2d);
            }
        }
        
        private void drawMiniMapContent(Graphics2D g2d) {
            // Dibujar jugadores en el mini-mapa
            if (gameState.getActivePlayers() != null) {
                for (Player player : gameState.getActivePlayers()) {
                    Point playerPos = worldToMinimap(player.getX(), player.getY());
                    
                    if (player.equals(currentPlayer)) {
                        g2d.setColor(SUCCESS_COLOR);
                        g2d.fillRect(playerPos.x - 3, playerPos.y - 3, 6, 6);
                    } else {
                        g2d.setColor(DANGER_COLOR);
                        g2d.fillRect(playerPos.x - 2, playerPos.y - 2, 4, 4);
                    }
                }
            }
            
            // Dibujar comida en el mini-mapa (solo algunos puntos)
            if (gameState.getFood() != null && gameState.getFood().size() > 0) {
                g2d.setColor(WARNING_COLOR);
                for (int i = 0; i < Math.min(50, gameState.getFood().size()); i += 10) {
                    // Dibujar algunos puntos de comida representativos
                }
            }
            
            // Dibujar viewport del jugador
            if (cameraManager != null) {
                g2d.setColor(SECONDARY_COLOR);
                g2d.setStroke(new BasicStroke(1));
                Rectangle viewport = cameraManager.getViewportBounds();
                Point topLeft = worldToMinimap(viewport.getX(), viewport.getY());
                Point bottomRight = worldToMinimap(viewport.getX() + viewport.getWidth(), 
                        viewport.getY() + viewport.getHeight());
                
                g2d.drawRect(topLeft.x, topLeft.y, 
                        bottomRight.x - topLeft.x, bottomRight.y - topLeft.y);
            }
        }
        
        private Point worldToMinimap(double worldX, double worldY) {
            if (gameState == null || cameraManager == null) {
                return new Point(0, 0);
            }
            
            // Convertir coordenadas del mundo a coordenadas del mini-mapa
            int mapX = (int) ((worldX / gameState.getWorldWidth()) * (MINIMAP_SIZE - 20)) + 10;
            int mapY = (int) ((worldY / gameState.getWorldHeight()) * (MINIMAP_SIZE - 20)) + 10;
            
            return new Point(mapX, mapY);
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
    
    // Métodos de interfaz KeyListener
    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_P:
                togglePause();
                break;
            case KeyEvent.VK_ESCAPE:
                showSettingsDialog();
                break;
            case KeyEvent.VK_F1:
                isVisible = !isVisible;
                setVisible(isVisible);
                break;
        }
    }
    
    @Override
    public void keyReleased(KeyEvent e) {}
    
    @Override
    public void keyTyped(KeyEvent e) {}
    
    @Override
    public void mouseClicked(MouseEvent e) {
        requestFocus();
    }
    
    @Override
    public void mousePressed(MouseEvent e) {}
    
    @Override
    public void mouseReleased(MouseEvent e) {}
    
    @Override
    public void mouseEntered(MouseEvent e) {}
    
    @Override
    public void mouseExited(MouseEvent e) {}
    
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