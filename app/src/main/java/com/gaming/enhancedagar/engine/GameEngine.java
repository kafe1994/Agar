package com.gaming.enhancedagar.engine;

import com.gaming.enhancedagar.entities.Entity;
import com.gaming.enhancedagar.entities.Player;
import com.gaming.enhancedagar.entities.Food;
import com.gaming.enhancedagar.game.GameState;
import com.gaming.enhancedagar.utils.Vector2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Motor principal del juego Enhanced Agar
 * Maneja el bucle de juego, actualización y renderizado de entidades
 * Sistema de timing, FPS y gestión de estados del juego
 */
public class GameEngine {
    private static final String TAG = "GameEngine";
    
    // Game Loop Variables
    private boolean isRunning;
    private boolean isPaused;
    private long lastFrameTime;
    private long currentTime;
    private double deltaTime;
    private double timeScale;
    
    // FPS Control
    private int targetFPS;
    private long frameTime;
    private long minFrameTime;
    private double actualFPS;
    private double fpsUpdateTimer;
    private double fpsUpdateInterval;
    
    // Entities
    private List<Entity> entities;
    private List<Entity> entitiesToAdd;
    private List<Entity> entitiesToRemove;
    private Player player;
    
    // Game State
    private GameState gameState;
    private Random random;
    
    // Spawn System
    private double foodSpawnTimer;
    private double foodSpawnInterval;
    private int maxFoodEntities;
    private Vector2D worldBounds;
    
    // Performance
    private int frameCount;
    private double updateTime;
    private double renderTime;
    
    /**
     * Constructor del GameEngine
     */
    public GameEngine() {
        this.isRunning = false;
        this.isPaused = false;
        this.targetFPS = 60;
        this.frameTime = 1000 / targetFPS;
        this.minFrameTime = frameTime;
        this.timeScale = 1.0;
        this.deltaTime = 0;
        this.actualFPS = targetFPS;
        this.fpsUpdateInterval = 0.5; // Update FPS every 0.5 seconds
        this.fpsUpdateTimer = 0;
        
        // Entity Management
        this.entities = new ArrayList<>();
        this.entitiesToAdd = new ArrayList<>();
        this.entitiesToRemove = new ArrayList<>();
        
        // Random for spawn system
        this.random = new Random();
        
        // Spawn Configuration
        this.foodSpawnInterval = 0.5; // Spawn food every 0.5 seconds
        this.maxFoodEntities = 100;
        this.foodSpawnTimer = 0;
        
        // World bounds (default size)
        this.worldBounds = new Vector2D(1920, 1080);
    }
    
    /**
     * Inicializa el motor de juego
     */
    public void initialize() {
        System.out.println(TAG + ": Inicializando GameEngine...");
        
        // Crear el jugador
        createPlayer();
        
        // Spawn inicial de comida
        spawnInitialFood();
        
        System.out.println(TAG + ": GameEngine inicializado correctamente");
    }
    
    /**
     * Inicia el bucle de juego principal
     */
    public void start() {
        if (isRunning) {
            System.out.println(TAG + ": El juego ya está ejecutándose");
            return;
        }
        
        System.out.println(TAG + ": Iniciando bucle de juego");
        isRunning = true;
        lastFrameTime = System.currentTimeMillis();
        
        gameLoop();
    }
    
    /**
     * Detiene el bucle de juego
     */
    public void stop() {
        System.out.println(TAG + ": Deteniendo GameEngine");
        isRunning = false;
    }
    
    /**
     * Pausa el juego
     */
    public void pause() {
        isPaused = true;
        System.out.println(TAG + ": Juego pausado");
    }
    
    /**
     * Reanuda el juego
     */
    public void resume() {
        isPaused = false;
        System.out.println(TAG + ": Juego reanudado");
    }
    
    /**
     * Bucle de juego principal
     */
    private void gameLoop() {
        while (isRunning) {
            currentTime = System.currentTimeMillis();
            deltaTime = (currentTime - lastFrameTime) / 1000.0 * timeScale;
            lastFrameTime = currentTime;
            
            if (!isPaused) {
                update(deltaTime);
            }
            
            render();
            
            frameCount++;
            
            // FPS Control
            frameTime = System.currentTimeMillis() - currentTime;
            if (frameTime < minFrameTime) {
                try {
                    Thread.sleep(minFrameTime - frameTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
    
    /**
     * Actualiza el estado del juego
     */
    private void update(double deltaTime) {
        long startTime = System.currentTimeMillis();
        
        // Actualizar entidades
        updateEntities(deltaTime);
        
        // Manejar spawn de objetos
        handleSpawning(deltaTime);
        
        // Procesar cambios de entidades
        processEntityChanges();
        
        // Actualizar estadísticas
        updateGameStats(deltaTime);
        
        updateTime = System.currentTimeMillis() - startTime;
    }
    
    /**
     * Renderiza el juego
     */
    private void render() {
        long startTime = System.currentTimeMillis();
        
        // Limpiar pantalla
        clearScreen();
        
        // Renderizar entidades
        renderEntities();
        
        // Renderizar UI (FPS, etc.)
        renderUI();
        
        renderTime = System.currentTimeMillis() - startTime;
    }
    
    /**
     * Actualiza todas las entidades
     */
    private void updateEntities(double deltaTime) {
        for (Entity entity : entities) {
            if (entity.isActive()) {
                entity.update(deltaTime);
            }
        }
    }
    
    /**
     * Renderiza todas las entidades
     */
    private void renderEntities() {
        for (Entity entity : entities) {
            if (entity.isActive()) {
                entity.render();
            }
        }
    }
    
    /**
     * Maneja el spawn de objetos (comida principalmente)
     */
    private void handleSpawning(double deltaTime) {
        foodSpawnTimer += deltaTime;
        
        if (foodSpawnTimer >= foodSpawnInterval) {
            spawnFood();
            foodSpawnTimer = 0;
        }
    }
    
    /**
     * Crea el jugador inicial
     */
    private void createPlayer() {
        Vector2D playerPos = new Vector2D(worldBounds.x / 2, worldBounds.y / 2);
        player = new Player("Player1", playerPos, 20.0);
        addEntity(player);
    }
    
    /**
     * Crea comida inicial en el mapa
     */
    private void spawnInitialFood() {
        for (int i = 0; i < 20; i++) {
            spawnFood();
        }
    }
    
    /**
     * Spawnea una entidad de comida aleatoria
     */
    private void spawnFood() {
        if (getFoodCount() >= maxFoodEntities) {
            return;
        }
        
        double x = random.nextDouble() * (worldBounds.x - 40) + 20;
        double y = random.nextDouble() * (worldBounds.y - 40) + 20;
        Vector2D position = new Vector2D(x, y);
        
        Food food = new Food(position, 5.0, generateRandomFoodType());
        addEntity(food);
    }
    
    /**
     * Genera un tipo de comida aleatorio
     */
    private Food.FoodType generateRandomFoodType() {
        Food.FoodType[] types = Food.FoodType.values();
        return types[random.nextInt(types.length)];
    }
    
    /**
     * Procesa cambios en las entidades (agregar/remover)
     */
    private void processEntityChanges() {
        if (!entitiesToAdd.isEmpty()) {
            entities.addAll(entitiesToAdd);
            entitiesToAdd.clear();
        }
        
        if (!entitiesToRemove.isEmpty()) {
            entities.removeAll(entitiesToRemove);
            entitiesToRemove.clear();
        }
    }
    
    /**
     * Actualiza estadísticas del juego
     */
    private void updateGameStats(double deltaTime) {
        // Actualizar FPS
        fpsUpdateTimer += deltaTime;
        if (fpsUpdateTimer >= fpsUpdateInterval) {
            actualFPS = frameCount / fpsUpdateTimer;
            frameCount = 0;
            fpsUpdateTimer = 0;
        }
    }
    
    /**
     * Limpia la pantalla
     */
    private void clearScreen() {
        // Implementación específica del renderizador
        System.out.print("\033[H\033[2J"); // Clear console (for demo)
    }
    
    /**
     * Renderiza la interfaz de usuario
     */
    private void renderUI() {
        // Mostrar FPS y estadísticas
        System.out.printf("FPS: %.1f | Entities: %d | Update: %.2fms | Render: %.2fms\r",
                         actualFPS, entities.size(), updateTime, renderTime);
    }
    
    // === PUBLIC API METHODS ===
    
    /**
     * Agrega una entidad al juego
     */
    public void addEntity(Entity entity) {
        entitiesToAdd.add(entity);
        System.out.println(TAG + ": Agregando entidad " + entity.getClass().getSimpleName());
    }
    
    /**
     * Remueve una entidad del juego
     */
    public void removeEntity(Entity entity) {
        entitiesToRemove.add(entity);
    }
    
    /**
     * Obtiene todas las entidades activas
     */
    public List<Entity> getActiveEntities() {
        List<Entity> activeEntities = new ArrayList<>();
        for (Entity entity : entities) {
            if (entity.isActive()) {
                activeEntities.add(entity);
            }
        }
        return activeEntities;
    }
    
    /**
     * Obtiene el jugador
     */
    public Player getPlayer() {
        return player;
    }
    
    /**
     * Obtiene el número de entidades de comida
     */
    public int getFoodCount() {
        int count = 0;
        for (Entity entity : entities) {
            if (entity instanceof Food && entity.isActive()) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Establece el estado del juego
     */
    public void setGameState(GameState gameState) {
        this.gameState = gameState;
        System.out.println(TAG + ": Estado del juego cambiado a " + gameState);
    }
    
    /**
     * Obtiene el estado actual del juego
     */
    public GameState getGameState() {
        return gameState;
    }
    
    /**
     * Establece la escala de tiempo
     */
    public void setTimeScale(double timeScale) {
        this.timeScale = Math.max(0.0, Math.min(timeScale, 2.0));
    }
    
    /**
     * Obtiene la escala de tiempo
     */
    public double getTimeScale() {
        return timeScale;
    }
    
    /**
     * Establece los límites del mundo
     */
    public void setWorldBounds(Vector2D bounds) {
        this.worldBounds = bounds;
    }
    
    /**
     * Obtiene los límites del mundo
     */
    public Vector2D getWorldBounds() {
        return worldBounds;
    }
    
    /**
     * Establece el intervalo de spawn de comida
     */
    public void setFoodSpawnInterval(double interval) {
        this.foodSpawnInterval = Math.max(0.1, interval);
    }
    
    /**
     * Establece el número máximo de entidades de comida
     */
    public void setMaxFoodEntities(int max) {
        this.maxFoodEntities = Math.max(1, max);
    }
    
    // === GETTERS ===
    
    public boolean isRunning() {
        return isRunning;
    }
    
    public boolean isPaused() {
        return isPaused;
    }
    
    public double getDeltaTime() {
        return deltaTime;
    }
    
    public double getActualFPS() {
        return actualFPS;
    }
    
    public int getTargetFPS() {
        return targetFPS;
    }
    
    public int getFrameCount() {
        return frameCount;
    }
    
    public double getUpdateTime() {
        return updateTime;
    }
    
    public double getRenderTime() {
        return renderTime;
    }
    
    /**
     * Limpia recursos del GameEngine
     */
    public void cleanup() {
        System.out.println(TAG + ": Limpiando recursos del GameEngine");
        
        entities.clear();
        entitiesToAdd.clear();
        entitiesToRemove.clear();
        player = null;
        
        isRunning = false;
        isPaused = false;
    }
    
    @Override
    public String toString() {
        return String.format("GameEngine{running=%s, paused=%s, fps=%.1f, entities=%d, player=%s}",
                           isRunning, isPaused, actualFPS, entities.size(), 
                           player != null ? "exists" : "null");
    }
}