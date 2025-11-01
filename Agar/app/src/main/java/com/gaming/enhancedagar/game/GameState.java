package com.gaming.enhancedagar.game;

/**
 * Estados del juego Enhanced Agar
 * Define los diferentes estados posibles del juego
 */
public enum GameState {
    /**
     * Estado inicial del juego, esperando inicio
     */
    MENU("Menú"),
    
    /**
     * Estado de juego activo, ejecutándose
     */
    PLAYING("Jugando"),
    
    /**
     * Juego pausado temporalmente
     */
    PAUSED("Pausado"),
    
    /**
     * Juego terminado o terminado
     */
    GAME_OVER("Game Over"),
    
    /**
     * Estado de configuración/opciones
     */
    SETTINGS("Configuración"),
    
    /**
     * Estado de carga inicializando recursos
     */
    LOADING("Cargando");
    
    private final String displayName;
    
    /**
     * Constructor del enum GameState
     * @param displayName Nombre para mostrar en UI
     */
    GameState(String displayName) {
        this.displayName = displayName;
    }
    
    /**
     * Obtiene el nombre de display del estado
     * @return Nombre para mostrar en UI
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Verifica si el estado es interactivo
     * @return true si el juego acepta input del usuario
     */
    public boolean isInteractive() {
        return this == PLAYING;
    }
    
    /**
     * Verifica si el estado permite renderizado
     * @return true si el estado debe renderizarse
     */
    public boolean isRenderable() {
        return this != LOADING;
    }
    
    /**
     * Verifica si el estado requiere update de lógica
     * @return true si la lógica del juego debe actualizarse
     */
    public boolean isUpdatable() {
        return this == PLAYING;
    }
    
    @Override
    public String toString() {
        return "GameState{" +
                "name=" + name() +
                ", displayName='" + displayName + '\'' +
                '}';
    }
}