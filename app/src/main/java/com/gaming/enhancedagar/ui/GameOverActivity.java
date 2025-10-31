package com.gaming.enhancedagar.ui;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.gaming.enhancedagar.MainActivity;
import com.gaming.enhancedagar.R;
import com.gaming.enhancedagar.game.GameState;
import com.gaming.enhancedagar.game.RoleSystem;
import com.gaming.enhancedagar.engine.RoleSystem;
import com.gaming.enhancedagar.utils.StatisticsManager;
import com.gaming.enhancedagar.utils.AchievementManager;
import com.gaming.enhancedagar.utils.PersonalRecordsManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Actividad que muestra la pantalla de Game Over con estadísticas detalladas,
 * logros, mejores momentos y opciones de interacción.
 */
public class GameOverActivity extends AppCompatActivity {
    
    // Views principales
    private NestedScrollView scrollView;
    private ImageView backgroundGradient;
    private ImageView trophyIcon;
    private TextView titleText;
    private TextView finalPositionText;
    private TextView personalBestText;
    
    // Layouts para estadísticas
    private LinearLayout statsContainer;
    private LinearLayout roleStatsContainer;
    private LinearLayout achievementsContainer;
    private LinearLayout bestMomentsContainer;
    private LinearLayout recordsComparisonContainer;
    
    // Botones
    private Button retryButton;
    private Button menuButton;
    private Button shareButton;
    
    // Gestores de datos
    private StatisticsManager statsManager;
    private AchievementManager achievementManager;
    private PersonalRecordsManager recordsManager;
    
    // Datos de la partida
    private int finalPosition;
    private int finalScore;
    private long gameTime;
    private RoleSystem.RoleType playerRole;
    private Map<String, Integer> roleStats;
    private List<String> bestMoments;
    private List<String> newAchievements;
    
    // Animaciones
    private Animation fadeInAnimation;
    private Animation slideUpAnimation;
    private Animation pulseAnimation;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_over);
        
        initializeViews();
        initializeManagers();
        loadGameData();
        setupAnimations();
        displayGameOverStats();
        animateEntrance();
    }
    
    private void initializeViews() {
        scrollView = findViewById(R.id.scrollView);
        backgroundGradient = findViewById(R.id.backgroundGradient);
        trophyIcon = findViewById(R.id.trophyIcon);
        titleText = findViewById(R.id.titleText);
        finalPositionText = findViewById(R.id.finalPositionText);
        personalBestText = findViewById(R.id.personalBestText);
        
        statsContainer = findViewById(R.id.statsContainer);
        roleStatsContainer = findViewById(R.id.roleStatsContainer);
        achievementsContainer = findViewById(R.id.achievementsContainer);
        bestMomentsContainer = findViewById(R.id.bestMomentsContainer);
        recordsComparisonContainer = findViewById(R.id.recordsComparisonContainer);
        
        retryButton = findViewById(R.id.retryButton);
        menuButton = findViewById(R.id.menuButton);
        shareButton = findViewById(R.id.shareButton);
        
        setupButtonClickListeners();
    }
    
    private void initializeManagers() {
        statsManager = new StatisticsManager(this);
        achievementManager = new AchievementManager(this);
        recordsManager = new PersonalRecordsManager(this);
    }
    
    private void loadGameData() {
        Intent intent = getIntent();
        finalPosition = intent.getIntExtra("final_position", 0);
        finalScore = intent.getIntExtra("final_score", 0);
        gameTime = intent.getLongExtra("game_time", 0);
        playerRole = (RoleSystem.RoleType) intent.getSerializableExtra("player_role");
        roleStats = (Map<String, Integer>) intent.getSerializableExtra("role_stats");
        bestMoments = intent.getStringArrayListExtra("best_moments");
        newAchievements = intent.getStringArrayListExtra("new_achievements");
        
        // Si algún dato viene nulo, usar valores por defecto
        if (roleStats == null) roleStats = new HashMap<>();
        if (bestMoments == null) bestMoments = new ArrayList<>();
        if (newAchievements == null) newAchievements = new ArrayList<>();
    }
    
    private void setupAnimations() {
        fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        slideUpAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse);
    }
    
    private void setupButtonClickListeners() {
        retryButton.setOnClickListener(v -> {
            playButtonSound();
            animateButtonClick(v);
            startNewGame();
        });
        
        menuButton.setOnClickListener(v -> {
            playButtonSound();
            animateButtonClick(v);
            returnToMainMenu();
        });
        
        shareButton.setOnClickListener(v -> {
            playButtonSound();
            animateButtonClick(v);
            shareResults();
        });
    }
    
    private void displayGameOverStats() {
        displayHeader();
        displayFinalResults();
        displayPlayerRole();
        displayGeneralStats();
        displayRoleSpecificStats();
        displayBestMoments();
        displayNewAchievements();
        displayRecordsComparison();
    }
    
    private void displayHeader() {
        titleText.setText("¡Partida Finalizada!");
        titleText.setTypeface(null, Typeface.BOLD);
        titleText.setTextSize(32);
        
        // Animación de pulso para el título
        titleText.startAnimation(pulseAnimation);
        
        // Configurar posición final
        String positionText = finalPosition == 1 ? "🥇 1er Lugar" :
                             finalPosition == 2 ? "🥈 2do Lugar" :
                             finalPosition == 3 ? "🥉 3er Lugar" :
                             finalPosition + "° Lugar";
        
        finalPositionText.setText(positionText);
        finalPositionText.setTextSize(24);
        finalPositionText.setTypeface(null, Typeface.BOLD);
        
        // Verificar si es récord personal
        boolean isPersonalBest = recordsManager.isPersonalBest(finalScore, finalPosition);
        if (isPersonalBest) {
            personalBestText.setText("🎉 ¡NUEVO RÉCORD PERSONAL! 🎉");
            personalBestText.setVisibility(View.VISIBLE);
            personalBestText.setTextSize(16);
            personalBestText.setTextColor(ContextCompat.getColor(this, R.color.gold));
        } else {
            personalBestText.setVisibility(View.GONE);
        }
        
        // Configurar icono del trofeo
        if (finalPosition <= 3) {
            trophyIcon.setImageResource(R.drawable.ic_trophy_gold);
            trophyIcon.startAnimation(pulseAnimation);
        } else {
            trophyIcon.setImageResource(R.drawable.ic_trophy_silver);
        }
    }
    
    private void displayFinalResults() {
        TextView scoreText = createStatRow("Puntuación Final", formatNumber(finalScore));
        scoreText.setTypeface(null, Typeface.BOLD);
        scoreText.setTextSize(20);
        scoreText.setTextColor(ContextCompat.getColor(this, R.color.primary));
        statsContainer.addView(scoreText);
        
        TextView timeText = createStatRow("Tiempo de Juego", formatGameTime(gameTime));
        statsContainer.addView(timeText);
        
        TextView positionDetailText = createStatRow("Posición Final", 
            finalPosition + " de " + getIntent().getIntExtra("total_players", 100));
        statsContainer.addView(positionDetailText);
    }
    
    private void displayPlayerRole() {
        TextView roleTitle = new TextView(this);
        roleTitle.setText("ROL JUGADO");
        roleTitle.setTypeface(null, Typeface.BOLD);
        roleTitle.setTextSize(16);
        roleTitle.setPadding(16, 24, 16, 8);
        roleStatsContainer.addView(roleTitle);
        
        TextView roleText = new TextView(this);
        roleText.setText(getRoleDisplayName(playerRole));
        roleText.setTypeface(null, Typeface.BOLD);
        roleText.setTextSize(18);
        roleText.setTextColor(ContextCompat.getColor(this, R.color.accent));
        roleText.setPadding(16, 0, 16, 16);
        roleStatsContainer.addView(roleText);
    }
    
    private void displayGeneralStats() {
        addSectionTitle(statsContainer, "Estadísticas Generales");
        
        for (Map.Entry<String, Integer> stat : roleStats.entrySet()) {
            if (!isRoleSpecificStat(stat.getKey())) {
                TextView statRow = createStatRow(getStatDisplayName(stat.getKey()), 
                    formatNumber(stat.getValue()));
                statsContainer.addView(statRow);
            }
        }
    }
    
    private void displayRoleSpecificStats() {
        addSectionTitle(roleStatsContainer, "Estadísticas por Rol");
        
        for (Map.Entry<String, Integer> stat : roleStats.entrySet()) {
            if (isRoleSpecificStat(stat.getKey())) {
                TextView statRow = createStatRow(getStatDisplayName(stat.getKey()), 
                    formatNumber(stat.getValue()));
                roleStatsContainer.addView(statRow);
            }
        }
    }
    
    private void displayBestMoments() {
        if (bestMoments.isEmpty()) return;
        
        addSectionTitle(bestMomentsContainer, "Mejores Momentos");
        
        for (int i = 0; i < bestMoments.size(); i++) {
            TextView momentText = new TextView(this);
            momentText.setText("⭐ " + bestMoments.get(i));
            momentText.setTextSize(16);
            momentText.setPadding(16, 8, 16, 8);
            momentText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            bestMomentsContainer.addView(momentText);
            
            if (i < bestMoments.size() - 1) {
                View divider = createDivider();
                bestMomentsContainer.addView(divider);
            }
        }
    }
    
    private void displayNewAchievements() {
        if (newAchievements.isEmpty()) return;
        
        addSectionTitle(achievementsContainer, "🏆 Logros Desbloqueados");
        
        for (String achievement : newAchievements) {
            LinearLayout achievementRow = new LinearLayout(this);
            achievementRow.setOrientation(LinearLayout.HORIZONTAL);
            achievementRow.setPadding(16, 12, 16, 12);
            achievementRow.setBackground(ContextCompat.getDrawable(this, R.drawable.achievement_background));
            
            TextView achievementIcon = new TextView(this);
            achievementIcon.setText("🏅");
            achievementIcon.setTextSize(24);
            achievementIcon.setPadding(0, 0, 16, 0);
            
            TextView achievementText = new TextView(this);
            achievementText.setText(achievement);
            achievementText.setTextSize(16);
            achievementText.setTypeface(null, Typeface.BOLD);
            achievementText.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            
            achievementRow.addView(achievementIcon);
            achievementRow.addView(achievementText);
            
            achievementsContainer.addView(achievementRow);
        }
    }
    
    private void displayRecordsComparison() {
        addSectionTitle(recordsComparisonContainer, "Comparación con Récords");
        
        // Récord de puntuación
        int bestScore = recordsManager.getBestScore();
        TextView scoreComparison = createComparisonRow(
            "Mejor Puntuación", 
            finalScore, 
            bestScore,
            finalScore > bestScore
        );
        recordsComparisonContainer.addView(scoreComparison);
        
        // Mejor posición
        int bestPosition = recordsManager.getBestPosition();
        TextView positionComparison = createComparisonRow(
            "Mejor Posición", 
            finalPosition, 
            bestPosition,
            finalPosition < bestPosition
        );
        recordsComparisonContainer.addView(positionComparison);
        
        // Tiempo más largo
        long bestTime = recordsManager.getLongestGameTime();
        TextView timeComparison = createComparisonRow(
            "Juego Más Largo", 
            (int)(gameTime / 1000), 
            (int)(bestTime / 1000),
            gameTime > bestTime
        );
        recordsComparisonContainer.addView(timeComparison);
    }
    
    private void addSectionTitle(LinearLayout container, String title) {
        TextView titleText = new TextView(this);
        titleText.setText(title);
        titleText.setTypeface(null, Typeface.BOLD);
        titleText.setTextSize(18);
        titleText.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        titleText.setPadding(16, 24, 16, 12);
        container.addView(titleText);
    }
    
    private TextView createStatRow(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(16, 8, 16, 8);
        row.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView labelText = new TextView(this);
        labelText.setText(label);
        labelText.setTextSize(16);
        labelText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        labelText.setLayoutParams(new LinearLayout.LayoutParams(0, 
            LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        
        TextView valueText = new TextView(this);
        valueText.setText(value);
        valueText.setTextSize(16);
        valueText.setTypeface(null, Typeface.BOLD);
        valueText.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        
        row.addView(labelText);
        row.addView(valueText);
        
        return (TextView) row.getChildAt(1); // Retorna el TextView del valor
    }
    
    private TextView createComparisonRow(String label, int current, int best, boolean isBetter) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(16, 12, 16, 12);
        row.setBackground(ContextCompat.getDrawable(this, R.drawable.comparison_background));
        
        TextView iconText = new TextView(this);
        iconText.setText(isBetter ? "📈" : "📊");
        iconText.setTextSize(20);
        iconText.setPadding(0, 0, 16, 0);
        
        TextView labelText = new TextView(this);
        labelText.setText(label);
        labelText.setTextSize(14);
        labelText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        labelText.setLayoutParams(new LinearLayout.LayoutParams(0, 
            LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        
        TextView valueText = new TextView(this);
        if (label.contains("Tiempo")) {
            valueText.setText(formatGameTime(current * 1000L) + " / " + formatGameTime(best * 1000L));
        } else {
            valueText.setText(current + " / " + best);
        }
        valueText.setTextSize(16);
        valueText.setTypeface(null, Typeface.BOLD);
        valueText.setTextColor(isBetter ? 
            ContextCompat.getColor(this, R.color.success) : 
            ContextCompat.getColor(this, R.color.text_primary));
        
        row.addView(iconText);
        row.addView(labelText);
        row.addView(valueText);
        
        return (TextView) row.getChildAt(2); // Retorna el TextView del valor
    }
    
    private View createDivider() {
        View divider = new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1);
        params.leftMargin = 16;
        params.rightMargin = 16;
        divider.setLayoutParams(params);
        divider.setBackgroundColor(ContextCompat.getColor(this, R.color.divider));
        return divider;
    }
    
    private void animateEntrance() {
        // Animar entrada secuencial
        titleText.startAnimation(fadeInAnimation);
        
        new android.os.Handler().postDelayed(() -> {
            finalPositionText.startAnimation(slideUpAnimation);
        }, 200);
        
        new android.os.Handler().postDelayed(() -> {
            statsContainer.startAnimation(fadeInAnimation);
        }, 400);
        
        new android.os.Handler().postDelayed(() -> {
            retryButton.startAnimation(slideUpAnimation);
            menuButton.startAnimation(slideUpAnimation);
            shareButton.startAnimation(slideUpAnimation);
        }, 600);
    }
    
    private void animateButtonClick(View view) {
        view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.button_click));
    }
    
    private void startNewGame() {
        Intent intent = new Intent(this, GameView.class);
        intent.putExtra("new_game", true);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        finish();
    }
    
    private void returnToMainMenu() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        finish();
    }
    
    private void shareResults() {
        String shareText = generateShareText();
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "¡Mira mi resultado en Enhanced Agar!");
        
        startActivity(Intent.createChooser(shareIntent, "Compartir resultados"));
    }
    
    private String generateShareText() {
        StringBuilder sb = new StringBuilder();
        sb.append("🎮 ¡Acabo de terminar una partida en Enhanced Agar!\n\n");
        sb.append("🏆 Posición: ").append(finalPosition).append("° lugar\n");
        sb.append("⭐ Puntuación: ").append(formatNumber(finalScore)).append(" puntos\n");
        sb.append("⏱️ Tiempo: ").append(formatGameTime(gameTime)).append("\n");
        sb.append("🎭 Rol: ").append(getRoleDisplayName(playerRole)).append("\n\n");
        
        if (!newAchievements.isEmpty()) {
            sb.append("🏅 Logros desbloqueados:\n");
            for (String achievement : newAchievements) {
                sb.append("• ").append(achievement).append("\n");
            }
            sb.append("\n");
        }
        
        if (finalPosition <= 3) {
            sb.append("🔥 ¡Consiguí podium! ¿Puedes hacerme frente?\n");
        } else {
            sb.append("💪 ¡La próxima será mejor! ¿Te atreves a desafiarme?\n");
        }
        
        sb.append("\n#EnhancedAgar #JuegoMovil #AgarIO #Competencia");
        
        return sb.toString();
    }
    
    private void playButtonSound() {
        // Aquí se reproduciría un sonido de botón
        // SoundManager.getInstance(this).playButtonSound();
    }
    
    // Métodos de utilidad
    private String formatNumber(int number) {
        if (number >= 1000000) {
            return String.format(Locale.getDefault(), "%.1fM", number / 1000000.0f);
        } else if (number >= 1000) {
            return String.format(Locale.getDefault(), "%.1fK", number / 1000.0f);
        }
        return String.valueOf(number);
    }
    
    private String formatGameTime(long timeInMillis) {
        long seconds = timeInMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes % 60, seconds % 60);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds % 60);
        }
    }
    
    private String getRoleDisplayName(RoleSystem.RoleType role) {
        switch (role) {
            case SEEKER: return "Buscador";
            case HUNTER: return "Cazador";
            case GUARDIAN: return "Guardián";
            case SPLITTER: return "Divisor";
            case VIRUS: return "Virus";
            case LEADER: return "Líder";
            default: return "Superviviente";
        }
    }
    
    private String getStatDisplayName(String statKey) {
        Map<String, String> statNames = new HashMap<>();
        statNames.put("food_eaten", "Comida Consumida");
        statNames.put("cells_absorbed", "Células Absorbidas");
        statNames.put("divisions_made", "Divisiones Realizadas");
        statNames.put("mergers_survived", "Fusiones Sobrevividas");
        statNames.put("time_alive", "Tiempo Vivo");
        statNames.put("territory_control", "Territorio Controlado");
        statNames.put("speed_boosts", "Impulsos de Velocidad");
        statNames.put("enemies_defeated", "Enemigos Derrotados");
        statNames.put("assists_given", "Asistencias Dadas");
        statNames.put("team_score", "Puntuación de Equipo");
        statNames.put("mass_gained", "Masa Ganada");
        statNames.put("distance_traveled", "Distancia Recorrida");
        
        return statNames.getOrDefault(statKey, statKey.replace("_", " ").toUpperCase());
    }
    
    private boolean isRoleSpecificStat(String statKey) {
        return statKey.equals("divisions_made") || 
               statKey.equals("mergers_survived") || 
               statKey.equals("territory_control") ||
               statKey.equals("team_score");
    }
    
    @Override
    public void onBackPressed() {
        // Deshabilitar botón de atrás durante la pantalla de game over
        // returnToMainMenu();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Restart animations if needed
    }
}