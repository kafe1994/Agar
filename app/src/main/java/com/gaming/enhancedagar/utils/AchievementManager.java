package com.gaming.enhancedagar.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manager para logros del jugador
 */
public class AchievementManager {
    private Context context;
    private SharedPreferences prefs;
    
    // Lista de logros disponibles
    public enum Achievement {
        // Logros básicos
        FIRST_GAME("first_game", "Primera Partida", "Juega tu primera partida", 0),
        FIRST_WIN("first_win", "Primera Victoria", "Gana tu primera partida", 50),
        FIRST_DEATH("first_death", "Vida y Muerte", "Muere por primera vez", 10),
        
        // Logros de supervivencia
        SURVIVE_30("survive_30", "Sobreviviente Novato", "Sobrevive 30 segundos", 25),
        SURVIVE_60("survive_60", "Sobreviviente", "Sobrevive 1 minuto", 50),
        SURVIVE_300("survive_300", "Superviviente Veteran", "Sobrevive 5 minutos", 100),
        SURVIVE_600("survive_600", "Leyenda del Sobrevivencia", "Sobrevive 10 minutos", 250),
        
        // Logros de puntos
        SCORE_100("score_100", "Demostrador", "Obtén 100 puntos", 25),
        SCORE_500("score_500", "Aspirante", "Obtén 500 puntos", 75),
        SCORE_1000("score_1000", "Maestro", "Obtén 1000 puntos", 150),
        SCORE_5000("score_5000", "Leyenda", "Obtén 5000 puntos", 300),
        
        // Logros de comida
        FOOD_50("food_50", "Hambre Leve", "Come 50 unidades de comida", 20),
        FOOD_200("food_200", "Comedor Ávido", "Come 200 unidades de comida", 50),
        FOOD_500("food_500", "Depredador", "Come 500 unidades de comida", 100),
        FOOD_1000("food_1000", "Higante Devourer", "Come 1000 unidades de comida", 200),
        
        // Logros de power-ups
        POWER_UP_5("powerup_5", "Entusiasta", "Usa 5 power-ups", 30),
        POWER_UP_20("powerup_20", "Adicto", "Usa 20 power-ups", 75),
        POWER_UP_50("powerup_50", "Maestro", "Usa 50 power-ups", 150),
        
        // Logros de roles
        ROLE_MASTER_TANK("role_tank", "Maestro Tanque", "Juega 10 partidas como Tanque", 75),
        ROLE_MASTER_ASSASSIN("role_assassin", "Maestro Asesina", "Juega 10 partidas como Asesino", 75),
        ROLE_MASTER_MAGE("role_mage", "Maestro Mago", "Juega 10 partidas como Mago", 75),
        ROLE_MASTER_SUPPORT("role_support", "Maestro Soporte", "Juega 10 partidas como Soporte", 75),
        
        // Logros especiales
        PERFECT_GAME("perfect_game", "Juego Perfecto", "Gana sin morir con puntuación máxima", 500),
        QUICK_WIN("quick_win", "Victoria Rápida", "Gana en menos de 60 segundos", 100),
        COMBO_MEAL("combo_meal", "Combo Perfecto", "Come 10 entidades seguidas", 75),
        MULTI_SPLIT("multi_split", "Fragmentación Maestra", "Divídete 5 veces en una partida", 100),
        
        // Logros de juegos totales
        GAMES_10("games_10", "Novato", "Juega 10 partidas", 25),
        GAMES_50("games_50", "Aficionado", "Juega 50 partidas", 75),
        GAMES_100("games_100", "Veterano", "Juega 100 partidas", 150),
        GAMES_500("games_500", "Leyenda", "Juega 500 partidas", 300)
    }
    
    private Map<String, AchievementData> achievements;
    private List<AchievementListener> listeners;
    
    public static class AchievementData {
        public boolean unlocked;
        public long unlockTime;
        public int progress;
        public int targetProgress;
        
        public AchievementData() {
            this.unlocked = false;
            this.unlockTime = 0;
            this.progress = 0;
            this.targetProgress = 1;
        }
        
        public AchievementData(int targetProgress) {
            this.unlocked = false;
            this.unlockTime = 0;
            this.progress = 0;
            this.targetProgress = targetProgress;
        }
        
        public boolean isUnlocked() {
            return unlocked;
        }
        
        public int getProgress() {
            return progress;
        }
        
        public int getTargetProgress() {
            return targetProgress;
        }
        
        public float getProgressPercentage() {
            return targetProgress > 0 ? (float) progress / targetProgress : 0f;
        }
    }
    
    public interface AchievementListener {
        void onAchievementUnlocked(Achievement achievement);
        void onProgressUpdated(Achievement achievement, int progress);
    }
    
    public AchievementManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences("EnhancedAgar_Achievements", Context.MODE_PRIVATE);
        this.achievements = new HashMap<>();
        this.listeners = new ArrayList<>();
        loadAchievements();
    }
    
    private void loadAchievements() {
        for (Achievement achievement : Achievement.values()) {
            AchievementData data = new AchievementData(getTargetForAchievement(achievement));
            data.unlocked = prefs.getBoolean(achievement.name() + "_unlocked", false);
            data.unlockTime = prefs.getLong(achievement.name() + "_time", 0);
            data.progress = prefs.getInt(achievement.name() + "_progress", 0);
            achievements.put(achievement.name(), data);
        }
    }
    
    private int getTargetForAchievement(Achievement achievement) {
        switch (achievement) {
            case FOOD_50: return 50;
            case FOOD_200: return 200;
            case FOOD_500: return 500;
            case FOOD_1000: return 1000;
            case POWER_UP_5: return 5;
            case POWER_UP_20: return 20;
            case POWER_UP_50: return 50;
            case SURVIVE_30: return 30;
            case SURVIVE_60: return 60;
            case SURVIVE_300: return 300;
            case SURVIVE_600: return 600;
            case SCORE_100: return 100;
            case SCORE_500: return 500;
            case SCORE_1000: return 1000;
            case SCORE_5000: return 5000;
            case ROLE_MASTER_TANK:
            case ROLE_MASTER_ASSASSIN:
            case ROLE_MASTER_MAGE:
            case ROLE_MASTER_SUPPORT:
                return 10;
            case COMBO_MEAL: return 10;
            case MULTI_SPLIT: return 5;
            case GAMES_10: return 10;
            case GAMES_50: return 50;
            case GAMES_100: return 100;
            case GAMES_500: return 500;
            default: return 1;
        }
    }
    
    public void addProgress(Achievement achievement, int amount) {
        AchievementData data = achievements.get(achievement.name());
        if (data == null || data.unlocked) return;
        
        data.progress += amount;
        if (data.progress >= data.targetProgress) {
            unlockAchievement(achievement);
        } else {
            notifyProgressUpdate(achievement, data.progress);
        }
        
        saveAchievement(achievement, data);
    }
    
    public void setProgress(Achievement achievement, int progress) {
        AchievementData data = achievements.get(achievement.name());
        if (data == null || data.unlocked) return;
        
        if (progress >= data.targetProgress) {
            unlockAchievement(achievement);
        } else if (progress != data.progress) {
            data.progress = progress;
            notifyProgressUpdate(achievement, progress);
            saveAchievement(achievement, data);
        }
    }
    
    public void unlockAchievement(Achievement achievement) {
        AchievementData data = achievements.get(achievement.name());
        if (data == null || data.unlocked) return;
        
        data.unlocked = true;
        data.unlockTime = System.currentTimeMillis();
        data.progress = data.targetProgress;
        
        notifyAchievementUnlocked(achievement);
        saveAchievement(achievement, data);
    }
    
    private void saveAchievement(Achievement achievement, AchievementData data) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(achievement.name() + "_unlocked", data.unlocked);
        editor.putLong(achievement.name() + "_time", data.unlockTime);
        editor.putInt(achievement.name() + "_progress", data.progress);
        editor.apply();
    }
    
    private void notifyAchievementUnlocked(Achievement achievement) {
        for (AchievementListener listener : listeners) {
            listener.onAchievementUnlocked(achievement);
        }
    }
    
    private void notifyProgressUpdate(Achievement achievement, int progress) {
        for (AchievementListener listener : listeners) {
            listener.onProgressUpdated(achievement, progress);
        }
    }
    
    public void addListener(AchievementListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(AchievementListener listener) {
        listeners.remove(listener);
    }
    
    public boolean isUnlocked(Achievement achievement) {
        AchievementData data = achievements.get(achievement.name());
        return data != null && data.unlocked;
    }
    
    public int getProgress(Achievement achievement) {
        AchievementData data = achievements.get(achievement.name());
        return data != null ? data.progress : 0;
    }
    
    public int getTargetProgress(Achievement achievement) {
        AchievementData data = achievements.get(achievement.name());
        return data != null ? data.targetProgress : 1;
    }
    
    public float getProgressPercentage(Achievement achievement) {
        AchievementData data = achievements.get(achievement.name());
        return data != null ? data.getProgressPercentage() : 0f;
    }
    
    public long getUnlockTime(Achievement achievement) {
        AchievementData data = achievements.get(achievement.name());
        return data != null ? data.unlockTime : 0;
    }
    
    public List<Achievement> getUnlockedAchievements() {
        List<Achievement> unlocked = new ArrayList<>();
        for (Achievement achievement : Achievement.values()) {
            if (isUnlocked(achievement)) {
                unlocked.add(achievement);
            }
        }
        return unlocked;
    }
    
    public List<Achievement> getLockedAchievements() {
        List<Achievement> locked = new ArrayList<>();
        for (Achievement achievement : Achievement.values()) {
            if (!isUnlocked(achievement)) {
                locked.add(achievement);
            }
        }
        return locked;
    }
    
    public int getTotalAchievements() {
        return Achievement.values().length;
    }
    
    public int getUnlockedCount() {
        return getUnlockedAchievements().size();
    }
    
    public float getCompletionPercentage() {
        return (float) getUnlockedCount() / getTotalAchievements() * 100f;
    }
    
    public void resetProgress() {
        for (Achievement achievement : Achievement.values()) {
            AchievementData data = achievements.get(achievement.name());
            if (data != null) {
                data.unlocked = false;
                data.unlockTime = 0;
                data.progress = 0;
                saveAchievement(achievement, data);
            }
        }
    }
}