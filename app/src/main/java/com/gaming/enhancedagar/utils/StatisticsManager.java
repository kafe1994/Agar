package com.gaming.enhancedagar.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manager para estadísticas del jugador
 */
public class StatisticsManager {
    private Context context;
    private SharedPreferences prefs;
    
    // Estadísticas del juego
    private int totalGamesPlayed;
    private int totalWins;
    private int totalDeaths;
    private float totalPlayTime;
    private int totalFoodsEaten;
    private int totalSizeGained;
    private int totalPowerUpsCollected;
    private float averageScore;
    private int bestScore;
    private int longestSurvivalTime;
    
    // Estadísticas por rol
    private Map<String, GameStats> roleStats;
    
    public static class GameStats {
        public int gamesPlayed;
        public int wins;
        public int deaths;
        public float totalPlayTime;
        public int bestScore;
        
        public GameStats() {
            this.gamesPlayed = 0;
            this.wins = 0;
            this.deaths = 0;
            this.totalPlayTime = 0f;
            this.bestScore = 0;
        }
    }
    
    public StatisticsManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences("EnhancedAgar_Stats", Context.MODE_PRIVATE);
        this.roleStats = new HashMap<>();
        loadStatistics();
    }
    
    private void loadStatistics() {
        totalGamesPlayed = prefs.getInt("totalGamesPlayed", 0);
        totalWins = prefs.getInt("totalWins", 0);
        totalDeaths = prefs.getInt("totalDeaths", 0);
        totalPlayTime = prefs.getFloat("totalPlayTime", 0f);
        totalFoodsEaten = prefs.getInt("totalFoodsEaten", 0);
        totalSizeGained = prefs.getInt("totalSizeGained", 0);
        totalPowerUpsCollected = prefs.getInt("totalPowerUpsCollected", 0);
        averageScore = prefs.getFloat("averageScore", 0f);
        bestScore = prefs.getInt("bestScore", 0);
        longestSurvivalTime = prefs.getInt("longestSurvivalTime", 0);
        
        // Cargar estadísticas por rol
        String[] roles = {"TANK", "ASSASSIN", "MAGE", "SUPPORT"};
        for (String role : roles) {
            GameStats stats = new GameStats();
            stats.gamesPlayed = prefs.getInt(role + "_games", 0);
            stats.wins = prefs.getInt(role + "_wins", 0);
            stats.deaths = prefs.getInt(role + "_deaths", 0);
            stats.totalPlayTime = prefs.getFloat(role + "_time", 0f);
            stats.bestScore = prefs.getInt(role + "_best", 0);
            roleStats.put(role, stats);
        }
    }
    
    public void recordGameEnd(boolean won, String role, int score, int survivalTime, 
                             int foodsEaten, int sizeGained, int powerUpsCollected) {
        totalGamesPlayed++;
        
        if (won) totalWins++;
        else totalDeaths++;
        
        totalPlayTime += survivalTime;
        totalFoodsEaten += foodsEaten;
        totalSizeGained += sizeGained;
        totalPowerUpsCollected += powerUpsCollected;
        
        // Calcular promedio
        averageScore = ((averageScore * (totalGamesPlayed - 1)) + score) / totalGamesPlayed;
        
        if (score > bestScore) bestScore = score;
        if (survivalTime > longestSurvivalTime) longestSurvivalTime = survivalTime;
        
        // Actualizar estadísticas por rol
        GameStats roleStat = roleStats.get(role);
        if (roleStat != null) {
            roleStat.gamesPlayed++;
            if (won) roleStat.wins++;
            else roleStat.deaths++;
            roleStat.totalPlayTime += survivalTime;
            if (score > roleStat.bestScore) roleStat.bestScore = score;
        }
        
        saveStatistics();
    }
    
    private void saveStatistics() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("totalGamesPlayed", totalGamesPlayed);
        editor.putInt("totalWins", totalWins);
        editor.putInt("totalDeaths", totalDeaths);
        editor.putFloat("totalPlayTime", totalPlayTime);
        editor.putInt("totalFoodsEaten", totalFoodsEaten);
        editor.putInt("totalSizeGained", totalSizeGained);
        editor.putInt("totalPowerUpsCollected", totalPowerUpsCollected);
        editor.putFloat("averageScore", averageScore);
        editor.putInt("bestScore", bestScore);
        editor.putInt("longestSurvivalTime", longestSurvivalTime);
        
        // Guardar estadísticas por rol
        for (Map.Entry<String, GameStats> entry : roleStats.entrySet()) {
            String role = entry.getKey();
            GameStats stats = entry.getValue();
            editor.putInt(role + "_games", stats.gamesPlayed);
            editor.putInt(role + "_wins", stats.wins);
            editor.putInt(role + "_deaths", stats.deaths);
            editor.putFloat(role + "_time", stats.totalPlayTime);
            editor.putInt(role + "_best", stats.bestScore);
        }
        
        editor.apply();
    }
    
    // Getters
    public int getTotalGamesPlayed() { return totalGamesPlayed; }
    public int getTotalWins() { return totalWins; }
    public int getTotalDeaths() { return totalDeaths; }
    public float getTotalPlayTime() { return totalPlayTime; }
    public int getTotalFoodsEaten() { return totalFoodsEaten; }
    public int getTotalSizeGained() { return totalSizeGained; }
    public int getTotalPowerUpsCollected() { return totalPowerUpsCollected; }
    public float getAverageScore() { return averageScore; }
    public int getBestScore() { return bestScore; }
    public int getLongestSurvivalTime() { return longestSurvivalTime; }
    
    public float getWinRate() {
        return totalGamesPlayed > 0 ? (float) totalWins / totalGamesPlayed * 100f : 0f;
    }
    
    public GameStats getRoleStats(String role) {
        return roleStats.get(role);
    }
    
    public void resetStatistics() {
        totalGamesPlayed = 0;
        totalWins = 0;
        totalDeaths = 0;
        totalPlayTime = 0;
        totalFoodsEaten = 0;
        totalSizeGained = 0;
        totalPowerUpsCollected = 0;
        averageScore = 0;
        bestScore = 0;
        longestSurvivalTime = 0;
        
        roleStats.clear();
        saveStatistics();
    }
}