package com.gaming.enhancedagar.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manager para récords personales del jugador
 */
public class PersonalRecordsManager {
    private Context context;
    private SharedPreferences prefs;
    
    // Récords globales
    private int bestScore;
    private int longestSurvival;
    private int maxFoodEaten;
    private int maxSizeReached;
    private int maxPowerUpsCollected;
    private int mostSplits;
    private int biggestWinMargin;
    private float fastestWin;
    private int highestCombo;
    private int maxPlayersKilled;
    
    // Récords por modo de juego
    private Map<String, ModeRecords> modeRecords;
    
    public static class ModeRecords {
        public int bestScore;
        public int longestSurvival;
        public int maxFoodEaten;
        public float fastestWin;
        
        public ModeRecords() {
            this.bestScore = 0;
            this.longestSurvival = 0;
            this.maxFoodEaten = 0;
            this.fastestWin = 0;
        }
    }
    
    // Récords por rol
    private Map<String, RoleRecords> roleRecords;
    
    public static class RoleRecords {
        public int bestScore;
        public int longestSurvival;
        public int gamesPlayed;
        public int totalWins;
        
        public RoleRecords() {
            this.bestScore = 0;
            this.longestSurvival = 0;
            this.gamesPlayed = 0;
            this.totalWins = 0;
        }
        
        public float getWinRate() {
            return gamesPlayed > 0 ? (float) totalWins / gamesPlayed * 100f : 0f;
        }
    }
    
    // Récords especiales
    private SpecialRecords specialRecords;
    
    public static class SpecialRecords {
        public float bestScorePerMinute;
        public int highestScoreStreak;
        public int perfectGames;
        public int quickestFirstKill;
        public float longestPerfectStreak;
        
        public SpecialRecords() {
            this.bestScorePerMinute = 0f;
            this.highestScoreStreak = 0;
            this.perfectGames = 0;
            this.quickestFirstKill = 0;
            this.longestPerfectStreak = 0f;
        }
    }
    
    public PersonalRecordsManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences("EnhancedAgar_Records", Context.MODE_PRIVATE);
        this.modeRecords = new HashMap<>();
        this.roleRecords = new HashMap<>();
        this.specialRecords = new SpecialRecords();
        loadRecords();
    }
    
    private void loadRecords() {
        // Récords globales
        bestScore = prefs.getInt("bestScore", 0);
        longestSurvival = prefs.getInt("longestSurvival", 0);
        maxFoodEaten = prefs.getInt("maxFoodEaten", 0);
        maxSizeReached = prefs.getInt("maxSizeReached", 0);
        maxPowerUpsCollected = prefs.getInt("maxPowerUpsCollected", 0);
        mostSplits = prefs.getInt("mostSplits", 0);
        biggestWinMargin = prefs.getInt("biggestWinMargin", 0);
        fastestWin = prefs.getFloat("fastestWin", 0f);
        highestCombo = prefs.getInt("highestCombo", 0);
        maxPlayersKilled = prefs.getInt("maxPlayersKilled", 0);
        
        // Récords por modo
        String[] modes = {"CLASSIC", "TEAMS", "SURVIVAL", "ARENA", "KING"};
        for (String mode : modes) {
            ModeRecords records = new ModeRecords();
            records.bestScore = prefs.getInt(mode + "_bestScore", 0);
            records.longestSurvival = prefs.getInt(mode + "_longestSurvival", 0);
            records.maxFoodEaten = prefs.getInt(mode + "_maxFoodEaten", 0);
            records.fastestWin = prefs.getFloat(mode + "_fastestWin", 0f);
            modeRecords.put(mode, records);
        }
        
        // Récords por rol
        String[] roles = {"TANK", "ASSASSIN", "MAGE", "SUPPORT"};
        for (String role : roles) {
            RoleRecords records = new RoleRecords();
            records.bestScore = prefs.getInt(role + "_bestScore", 0);
            records.longestSurvival = prefs.getInt(role + "_longestSurvival", 0);
            records.gamesPlayed = prefs.getInt(role + "_gamesPlayed", 0);
            records.totalWins = prefs.getInt(role + "_totalWins", 0);
            roleRecords.put(role, records);
        }
        
        // Récords especiales
        specialRecords.bestScorePerMinute = prefs.getFloat("bestScorePerMinute", 0f);
        specialRecords.highestScoreStreak = prefs.getInt("highestScoreStreak", 0);
        specialRecords.perfectGames = prefs.getInt("perfectGames", 0);
        specialRecords.quickestFirstKill = prefs.getInt("quickestFirstKill", 0);
        specialRecords.longestPerfectStreak = prefs.getFloat("longestPerfectStreak", 0f);
    }
    
    public void updateGlobalRecords(GameEndData data) {
        boolean globalUpdated = false;
        
        // Actualizar récord de puntuación
        if (data.score > bestScore) {
            bestScore = data.score;
            globalUpdated = true;
        }
        
        // Actualizar récord de supervivencia
        if (data.survivalTime > longestSurvival) {
            longestSurvival = data.survivalTime;
            globalUpdated = true;
        }
        
        // Actualizar récord de comida
        if (data.foodsEaten > maxFoodEaten) {
            maxFoodEaten = data.foodsEaten;
            globalUpdated = true;
        }
        
        // Actualizar récord de tamaño
        if (data.maxSize > maxSizeReached) {
            maxSizeReached = data.maxSize;
            globalUpdated = true;
        }
        
        // Actualizar récord de power-ups
        if (data.powerUpsCollected > maxPowerUpsCollected) {
            maxPowerUpsCollected = data.powerUpsCollected;
            globalUpdated = true;
        }
        
        // Actualizar récord de splits
        if (data.timesSplit > mostSplits) {
            mostSplits = data.timesSplit;
            globalUpdated = true;
        }
        
        // Actualizar récord de margen de victoria
        if (data.won && data.marginOfVictory > biggestWinMargin) {
            biggestWinMargin = data.marginOfVictory;
            globalUpdated = true;
        }
        
        // Actualizar récord de victoria rápida
        if (data.won && (fastestWin == 0 || data.survivalTime < fastestWin)) {
            fastestWin = data.survivalTime;
            globalUpdated = true;
        }
        
        // Actualizar récord de combo
        if (data.highestCombo > highestCombo) {
            highestCombo = data.highestCombo;
            globalUpdated = true;
        }
        
        // Actualizar récord de jugadores eliminados
        if (data.playersKilled > maxPlayersKilled) {
            maxPlayersKilled = data.playersKilled;
            globalUpdated = true;
        }
        
        if (globalUpdated) {
            saveGlobalRecords();
        }
    }
    
    public void updateModeRecords(String mode, GameEndData data) {
        ModeRecords records = modeRecords.get(mode);
        if (records == null) {
            records = new ModeRecords();
            modeRecords.put(mode, records);
        }
        
        boolean updated = false;
        
        if (data.score > records.bestScore) {
            records.bestScore = data.score;
            updated = true;
        }
        
        if (data.survivalTime > records.longestSurvival) {
            records.longestSurvival = data.survivalTime;
            updated = true;
        }
        
        if (data.foodsEaten > records.maxFoodEaten) {
            records.maxFoodEaten = data.foodsEaten;
            updated = true;
        }
        
        if (data.won && (records.fastestWin == 0 || data.survivalTime < records.fastestWin)) {
            records.fastestWin = data.survivalTime;
            updated = true;
        }
        
        if (updated) {
            saveModeRecords(mode, records);
        }
    }
    
    public void updateRoleRecords(String role, GameEndData data) {
        RoleRecords records = roleRecords.get(role);
        if (records == null) {
            records = new RoleRecords();
            roleRecords.put(role, records);
        }
        
        records.gamesPlayed++;
        if (data.won) {
            records.totalWins++;
        }
        
        if (data.score > records.bestScore) {
            records.bestScore = data.score;
        }
        
        if (data.survivalTime > records.longestSurvival) {
            records.longestSurvival = data.survivalTime;
        }
        
        saveRoleRecords(role, records);
    }
    
    public void updateSpecialRecords(GameEndData data) {
        boolean updated = false;
        
        // Score per minute
        float scorePerMinute = data.survivalTime > 0 ? data.score / (data.survivalTime / 60f) : 0f;
        if (scorePerMinute > specialRecords.bestScorePerMinute) {
            specialRecords.bestScorePerMinute = scorePerMinute;
            updated = true;
        }
        
        // Score streak
        if (data.scoreStreak > specialRecords.highestScoreStreak) {
            specialRecords.highestScoreStreak = data.scoreStreak;
            updated = true;
        }
        
        // Perfect games
        if (data.perfectGame) {
            specialRecords.perfectGames++;
            updated = true;
        }
        
        // Quickest first kill
        if (data.firstKillTime > 0 && (specialRecords.quickestFirstKill == 0 || 
            data.firstKillTime < specialRecords.quickestFirstKill)) {
            specialRecords.quickestFirstKill = data.firstKillTime;
            updated = true;
        }
        
        // Longest perfect streak
        if (data.perfectStreakTime > specialRecords.longestPerfectStreak) {
            specialRecords.longestPerfectStreak = data.perfectStreakTime;
            updated = true;
        }
        
        if (updated) {
            saveSpecialRecords();
        }
    }
    
    private void saveGlobalRecords() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("bestScore", bestScore);
        editor.putInt("longestSurvival", longestSurvival);
        editor.putInt("maxFoodEaten", maxFoodEaten);
        editor.putInt("maxSizeReached", maxSizeReached);
        editor.putInt("maxPowerUpsCollected", maxPowerUpsCollected);
        editor.putInt("mostSplits", mostSplits);
        editor.putInt("biggestWinMargin", biggestWinMargin);
        editor.putFloat("fastestWin", fastestWin);
        editor.putInt("highestCombo", highestCombo);
        editor.putInt("maxPlayersKilled", maxPlayersKilled);
        editor.apply();
    }
    
    private void saveModeRecords(String mode, ModeRecords records) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(mode + "_bestScore", records.bestScore);
        editor.putInt(mode + "_longestSurvival", records.longestSurvival);
        editor.putInt(mode + "_maxFoodEaten", records.maxFoodEaten);
        editor.putFloat(mode + "_fastestWin", records.fastestWin);
        editor.apply();
    }
    
    private void saveRoleRecords(String role, RoleRecords records) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(role + "_bestScore", records.bestScore);
        editor.putInt(role + "_longestSurvival", records.longestSurvival);
        editor.putInt(role + "_gamesPlayed", records.gamesPlayed);
        editor.putInt(role + "_totalWins", records.totalWins);
        editor.apply();
    }
    
    private void saveSpecialRecords() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat("bestScorePerMinute", specialRecords.bestScorePerMinute);
        editor.putInt("highestScoreStreak", specialRecords.highestScoreStreak);
        editor.putInt("perfectGames", specialRecords.perfectGames);
        editor.putInt("quickestFirstKill", specialRecords.quickestFirstKill);
        editor.putFloat("longestPerfectStreak", specialRecords.longestPerfectStreak);
        editor.apply();
    }
    
    // Getters para récords globales
    public int getBestScore() { return bestScore; }
    public int getLongestSurvival() { return longestSurvival; }
    public int getMaxFoodEaten() { return maxFoodEaten; }
    public int getMaxSizeReached() { return maxSizeReached; }
    public int getMaxPowerUpsCollected() { return maxPowerUpsCollected; }
    public int getMostSplits() { return mostSplits; }
    public int getBiggestWinMargin() { return biggestWinMargin; }
    public float getFastestWin() { return fastestWin; }
    public int getHighestCombo() { return highestCombo; }
    public int getMaxPlayersKilled() { return maxPlayersKilled; }
    
    // Getters para récords por modo
    public ModeRecords getModeRecords(String mode) {
        return modeRecords.get(mode);
    }
    
    // Getters para récords por rol
    public RoleRecords getRoleRecords(String role) {
        return roleRecords.get(role);
    }
    
    // Getters para récords especiales
    public SpecialRecords getSpecialRecords() {
        return specialRecords;
    }
    
    // Método para resetear récords
    public void resetRecords() {
        bestScore = 0;
        longestSurvival = 0;
        maxFoodEaten = 0;
        maxSizeReached = 0;
        maxPowerUpsCollected = 0;
        mostSplits = 0;
        biggestWinMargin = 0;
        fastestWin = 0;
        highestCombo = 0;
        maxPlayersKilled = 0;
        
        modeRecords.clear();
        roleRecords.clear();
        specialRecords = new SpecialRecords();
        
        saveGlobalRecords();
        saveSpecialRecords();
    }
    
    // Clase para datos de fin de juego
    public static class GameEndData {
        public int score;
        public int survivalTime;
        public int foodsEaten;
        public int maxSize;
        public int powerUpsCollected;
        public int timesSplit;
        public int playersKilled;
        public boolean won;
        public int marginOfVictory;
        public int highestCombo;
        public float scorePerMinute;
        public int scoreStreak;
        public boolean perfectGame;
        public int firstKillTime;
        public float perfectStreakTime;
        
        public GameEndData(int score, int survivalTime, int foodsEaten, int maxSize,
                          int powerUpsCollected, int timesSplit, int playersKilled,
                          boolean won, int marginOfVictory, int highestCombo) {
            this.score = score;
            this.survivalTime = survivalTime;
            this.foodsEaten = foodsEaten;
            this.maxSize = maxSize;
            this.powerUpsCollected = powerUpsCollected;
            this.timesSplit = timesSplit;
            this.playersKilled = playersKilled;
            this.won = won;
            this.marginOfVictory = marginOfVictory;
            this.highestCombo = highestCombo;
            this.scorePerMinute = survivalTime > 0 ? score / (survivalTime / 60f) : 0f;
            this.scoreStreak = 0;
            this.perfectGame = false;
            this.firstKillTime = 0;
            this.perfectStreakTime = 0f;
        }
    }
}