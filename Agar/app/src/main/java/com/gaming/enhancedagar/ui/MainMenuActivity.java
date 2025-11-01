package com.gaming.enhancedagar.ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.gaming.enhancedagar.MainActivity;
import com.gaming.enhancedagar.engine.RoleSystem;
import com.gaming.enhancedagar.game.GameModeManager;
import com.gaming.enhancedagar.R;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * MainMenuActivity - Menú principal moderno y atractivo para Enhanced Agar
 * 
 * Características:
 * - Selección de modo de juego (Classic, Teams, Survival, Arena, King)
 * - Selección de rol antes del juego (Tank, Assassin, Mage, Support)
 * - Botones con animaciones y efectos visuales
 * - Configuración de nombre de usuario
 * - Integración con sistema de guardado local
 * - Animaciones fluidas y UI moderna
 */
public class MainMenuActivity extends AppCompatActivity {
    
    private static final String TAG = "MainMenuActivity";
    private static final String PREFS_NAME = "EnhancedAgarPrefs";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_LAST_MODE = "last_mode";
    private static final String KEY_LAST_ROLE = "last_role";
    private static final String KEY_SOUND_ENABLED = "sound_enabled";
    private static final String KEY_MUSIC_ENABLED = "music_enabled";
    
    // UI Components
    private ViewGroup menuContainer;
    private TextView titleText;
    private TextView subtitleText;
    private Button playButton;
    private Button settingsButton;
    private Button profileButton;
    private Button scoresButton;
    private Button creditsButton;
    private Button quitButton;
    
    // Mode Selection
    private LinearLayout modeSelectionContainer;
    private List<GameModeManager.GameMode> gameModes;
    private TextView selectedModeText;
    private ImageButton modePrevButton;
    private ImageButton modeNextButton;
    
    // Role Selection
    private LinearLayout roleSelectionContainer;
    private List<RoleSystem.RoleType> availableRoles;
    private TextView selectedRoleText;
    private ImageButton rolePrevButton;
    private ImageButton roleNextButton;
    
    // Username Configuration
    private LinearLayout usernameContainer;
    private EditText usernameEditText;
    private Button saveUsernameButton;
    private Button randomUsernameButton;
    
    // Background Effects
    private View backgroundGradient;
    private ParticleEffectView particleEffectView;
    
    // Animations and Effects
    private ScheduledExecutorService animationExecutor;
    private Handler mainHandler;
    private boolean isAnimating = false;
    private float currentTitleScale = 1.0f;
    
    // Game Configuration
    private GameModeManager.GameMode selectedMode = GameModeManager.GameMode.CLASSIC;
    private RoleSystem.RoleType selectedRole = RoleSystem.RoleType.TANK;
    private String currentUsername = "";
    private boolean soundEnabled = true;
    private boolean musicEnabled = true;
    
    // Shared Preferences
    private SharedPreferences prefs;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        
        // Initialize components
        initializeComponents();
        initializePreferences();
        loadUserSettings();
        setupEventListeners();
        startBackgroundAnimations();
        animateMenuEntry();
        
        // Log initialization
        android.util.Log.i(TAG, "MainMenuActivity initialized successfully");
    }
    
    /**
     * Inicializa todos los componentes de la UI
     */
    private void initializeComponents() {
        // Main container
        menuContainer = findViewById(R.id.menu_container);
        titleText = findViewById(R.id.title_text);
        subtitleText = findViewById(R.id.subtitle_text);
        
        // Main buttons
        playButton = findViewById(R.id.play_button);
        settingsButton = findViewById(R.id.settings_button);
        profileButton = findViewById(R.id.profile_button);
        scoresButton = findViewById(R.id.scores_button);
        creditsButton = findViewById(R.id.credits_button);
        quitButton = findViewById(R.id.quit_button);
        
        // Mode selection
        modeSelectionContainer = findViewById(R.id.mode_selection_container);
        selectedModeText = findViewById(R.id.selected_mode_text);
        modePrevButton = findViewById(R.id.mode_prev_button);
        modeNextButton = findViewById(R.id.mode_next_button);
        
        // Role selection
        roleSelectionContainer = findViewById(R.id.role_selection_container);
        selectedRoleText = findViewById(R.id.selected_role_text);
        rolePrevButton = findViewById(R.id.role_prev_button);
        roleNextButton = findViewById(R.id.role_next_button);
        
        // Username configuration
        usernameContainer = findViewById(R.id.username_container);
        usernameEditText = findViewById(R.id.username_edit_text);
        saveUsernameButton = findViewById(R.id.save_username_button);
        randomUsernameButton = findViewById(R.id.random_username_button);
        
        // Background effects
        backgroundGradient = findViewById(R.id.background_gradient);
        particleEffectView = findViewById(R.id.particle_effect_view);
        
        // Initialize game modes and roles
        gameModes = Arrays.asList(GameModeManager.GameMode.values());
        availableRoles = Arrays.asList(RoleSystem.RoleType.values());
        
        // Initialize handlers
        mainHandler = new Handler(Looper.getMainLooper());
        animationExecutor = Executors.newSingleThreadScheduledExecutor();
        
        // Set initial selections
        updateModeDisplay();
        updateRoleDisplay();
    }
    
    /**
     * Inicializa las preferencias compartidas
     */
    private void initializePreferences() {
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }
    
    /**
     * Carga la configuración guardada del usuario
     */
    private void loadUserSettings() {
        currentUsername = prefs.getString(KEY_USERNAME, "Jugador_" + (int)(Math.random() * 1000));
        selectedMode = GameModeManager.GameMode.valueOf(
            prefs.getString(KEY_LAST_MODE, GameModeManager.GameMode.CLASSIC.name())
        );
        selectedRole = RoleSystem.RoleType.valueOf(
            prefs.getString(KEY_LAST_ROLE, RoleSystem.RoleType.TANK.name())
        );
        soundEnabled = prefs.getBoolean(KEY_SOUND_ENABLED, true);
        musicEnabled = prefs.getBoolean(KEY_MUSIC_ENABLED, true);
        
        usernameEditText.setText(currentUsername);
        updateModeDisplay();
        updateRoleDisplay();
        
        android.util.Log.d(TAG, "User settings loaded - Username: " + currentUsername + 
                          ", Mode: " + selectedMode.name() + ", Role: " + selectedRole.name());
    }
    
    /**
     * Guarda la configuración del usuario
     */
    private void saveUserSettings() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_USERNAME, currentUsername);
        editor.putString(KEY_LAST_MODE, selectedMode.name());
        editor.putString(KEY_LAST_ROLE, selectedRole.name());
        editor.putBoolean(KEY_SOUND_ENABLED, soundEnabled);
        editor.putBoolean(KEY_MUSIC_ENABLED, musicEnabled);
        editor.apply();
        
        android.util.Log.d(TAG, "User settings saved");
    }
    
    /**
     * Configura todos los event listeners
     */
    private void setupEventListeners() {
        // Main menu buttons with animations
        playButton.setOnClickListener(v -> animateButtonPress(playButton, this::startGame));
        settingsButton.setOnClickListener(v -> animateButtonPress(settingsButton, this::openSettings));
        profileButton.setOnClickListener(v -> animateButtonPress(profileButton, this::openProfile));
        scoresButton.setOnClickListener(v -> animateButtonPress(scoresButton, this::openScores));
        creditsButton.setOnClickListener(v -> animateButtonPress(creditsButton, this::openCredits));
        quitButton.setOnClickListener(v -> animateButtonPress(quitButton, this::quitGame));
        
        // Mode selection
        modePrevButton.setOnClickListener(v -> {
            changeMode(-1);
            animateButtonPress(modePrevButton, null);
        });
        modeNextButton.setOnClickListener(v -> {
            changeMode(1);
            animateButtonPress(modeNextButton, null);
        });
        
        // Role selection
        rolePrevButton.setOnClickListener(v -> {
            changeRole(-1);
            animateButtonPress(rolePrevButton, null);
        });
        roleNextButton.setOnClickListener(v -> {
            changeRole(1);
            animateButtonPress(roleNextButton, null);
        });
        
        // Username configuration
        saveUsernameButton.setOnClickListener(v -> {
            animateButtonPress(saveUsernameButton, this::saveUsername);
        });
        
        randomUsernameButton.setOnClickListener(v -> {
            animateButtonPress(randomUsernameButton, this::generateRandomUsername);
        });
        
        // Username edit text focus listener
        usernameEditText.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                showUsernameContainer();
            }
        });
    }
    
    /**
     * Cambia el modo de juego seleccionado
     */
    private void changeMode(int direction) {
        int currentIndex = gameModes.indexOf(selectedMode);
        int newIndex = (currentIndex + direction + gameModes.size()) % gameModes.size();
        selectedMode = gameModes.get(newIndex);
        updateModeDisplay();
        playButtonEffect();
        saveUserSettings();
        
        android.util.Log.d(TAG, "Mode changed to: " + selectedMode.name());
    }
    
    /**
     * Cambia el rol seleccionado
     */
    private void changeRole(int direction) {
        int currentIndex = availableRoles.indexOf(selectedRole);
        int newIndex = (currentIndex + direction + availableRoles.size()) % availableRoles.size();
        selectedRole = availableRoles.get(newIndex);
        updateRoleDisplay();
        playButtonEffect();
        saveUserSettings();
        
        android.util.Log.d(TAG, "Role changed to: " + selectedRole.name());
    }
    
    /**
     * Actualiza la visualización del modo seleccionado
     */
    private void updateModeDisplay() {
        String modeInfo = getModeInfo(selectedMode);
        selectedModeText.setText(modeInfo);
        
        // Add animation to the text change
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(selectedModeText, "alpha", 1f, 0f);
        fadeOut.setDuration(150);
        
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(selectedModeText, "alpha", 0f, 1f);
        fadeIn.setDuration(150);
        
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playSequentially(fadeOut, fadeIn);
        animatorSet.start();
    }
    
    /**
     * Actualiza la visualización del rol seleccionado
     */
    private void updateRoleDisplay() {
        String roleInfo = getRoleInfo(selectedRole);
        selectedRoleText.setText(roleInfo);
        
        // Add animation to the text change
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(selectedRoleText, "alpha", 1f, 0f);
        fadeOut.setDuration(150);
        
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(selectedRoleText, "alpha", 0f, 1f);
        fadeIn.setDuration(150);
        
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playSequentially(fadeOut, fadeIn);
        animatorSet.start();
    }
    
    /**
     * Obtiene información detallada del modo de juego
     */
    private String getModeInfo(GameModeManager.GameMode mode) {
        switch (mode) {
            case CLASSIC:
                return "🏆 Clásico\nTradicional Agar.io\nCrece y domina";
            case TEAMS:
                return "👥 Equipos\n4v4 con roles específicos\nTrabajo en equipo";
            case SURVIVAL:
                return "⚔️ Supervivencia\nSobrevive oleadas de enemigos\nPrueba de resistencia";
            case ARENA:
                return "⚡ Arena\nCombate directo sin crecimiento\nHabilidad pura";
            case KING:
                return "👑 Rey\nAtaca al rey para ganar puntos\nEstrategia y liderazgo";
            default:
                return "Modo Desconocido";
        }
    }
    
    /**
     * Obtiene información detallada del rol
     */
    private String getRoleInfo(RoleSystem.RoleType role) {
        switch (role) {
            case TANK:
                return "🛡️ Tanque\nAlto HP y defensa\nIdeal para protección";
            case ASSASSIN:
                return "🗡️ Asesino\nAlta velocidad y daño crítico\nAtaque sigiloso";
            case MAGE:
                return "🔮 Mago\nHabilidades mágicas\nDaño a distancia";
            case SUPPORT:
                return "✨ Soporte\nCuración y soporte\nAyuda al equipo";
            default:
                return "Rol Desconocido";
        }
    }
    
    /**
     * Anima la entrada del menú
     */
    private void animateMenuEntry() {
        // Reset views
        menuContainer.setAlpha(0f);
        menuContainer.setScaleX(0.8f);
        menuContainer.setScaleY(0.8f);
        
        // Title animation
        titleText.setAlpha(0f);
        titleText.setTranslationY(-100f);
        
        // Subtitle animation
        subtitleText.setAlpha(0f);
        subtitleText.setTranslationY(-50f);
        
        // Animate title
        ObjectAnimator titleAlpha = ObjectAnimator.ofFloat(titleText, "alpha", 0f, 1f);
        titleAlpha.setDuration(800);
        
        ObjectAnimator titleTranslation = ObjectAnimator.ofFloat(titleText, "translationY", -100f, 0f);
        titleTranslation.setDuration(800);
        
        // Animate subtitle
        ObjectAnimator subtitleAlpha = ObjectAnimator.ofFloat(subtitleText, "alpha", 0f, 1f);
        subtitleAlpha.setDuration(800);
        subtitleAlpha.setStartDelay(200);
        
        ObjectAnimator subtitleTranslation = ObjectAnimator.ofFloat(subtitleText, "translationY", -50f, 0f);
        subtitleTranslation.setDuration(800);
        subtitleTranslation.setStartDelay(200);
        
        // Animate main container
        ObjectAnimator containerAlpha = ObjectAnimator.ofFloat(menuContainer, "alpha", 0f, 1f);
        containerAlpha.setDuration(600);
        containerAlpha.setStartDelay(400);
        
        ObjectAnimator containerScaleX = ObjectAnimator.ofFloat(menuContainer, "scaleX", 0.8f, 1f);
        containerScaleX.setDuration(600);
        containerScaleX.setStartDelay(400);
        
        ObjectAnimator containerScaleY = ObjectAnimator.ofFloat(menuContainer, "scaleY", 0.8f, 1f);
        containerScaleY.setDuration(600);
        containerScaleY.setStartDelay(400);
        
        // Start animations
        AnimatorSet titleSet = new AnimatorSet();
        titleSet.playTogether(titleAlpha, titleTranslation);
        
        AnimatorSet subtitleSet = new AnimatorSet();
        subtitleSet.playTogether(subtitleAlpha, subtitleTranslation);
        
        AnimatorSet containerSet = new AnimatorSet();
        containerSet.playTogether(containerAlpha, containerScaleX, containerScaleY);
        
        AnimatorSet mainSet = new AnimatorSet();
        mainSet.playSequentially(titleSet, subtitleSet, containerSet);
        mainSet.start();
    }
    
    /**
     * Anima la pulsación de un botón
     */
    private void animateButtonPress(View button, Runnable action) {
        if (isAnimating) return;
        
        isAnimating = true;
        
        // Scale down
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(button, "scaleX", 1f, 0.95f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(button, "scaleY", 1f, 0.95f);
        
        // Scale up
        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(button, "scaleX", 0.95f, 1f);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(button, "scaleY", 0.95f, 1f);
        
        // Color flash
        ObjectAnimator colorFlash = ObjectAnimator.ofInt(button, "alpha", 1f, 0.8f, 1f);
        colorFlash.setDuration(200);
        
        AnimatorSet pressAnimation = new AnimatorSet();
        pressAnimation.playTogether(scaleDownX, scaleDownY, colorFlash);
        
        AnimatorSet releaseAnimation = new AnimatorSet();
        releaseAnimation.playTogether(scaleUpX, scaleUpY);
        
        AnimatorSet fullAnimation = new AnimatorSet();
        fullAnimation.playSequentially(pressAnimation, releaseAnimation);
        fullAnimation.addListener(new android.animation.Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(android.animation.Animator animation) {
                // Play button click sound
                if (soundEnabled) {
                    playButtonClickSound();
                }
            }
            
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                isAnimating = false;
                if (action != null) {
                    action.run();
                }
            }
            
            @Override
            public void onAnimationCancel(android.animation.Animator animation) {
                isAnimating = false;
            }
            
            @Override
            public void onAnimationRepeat(android.animation.Animator animation) {}
        });
        
        fullAnimation.start();
    }
    
    /**
     * Efecto visual al cambiar selección
     */
    private void playButtonEffect() {
        if (soundEnabled) {
            playButtonClickSound();
        }
        
        // Pulse animation on selection containers
        pulseView(modeSelectionContainer);
        pulseView(roleSelectionContainer);
    }
    
    /**
     * Efecto de pulso en una vista
     */
    private void pulseView(View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.05f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.05f, 1f);
        
        AnimatorSet pulse = new AnimatorSet();
        pulse.playTogether(scaleX, scaleY);
        pulse.setDuration(300);
        pulse.start();
    }
    
    /**
     * Inicia las animaciones de fondo
     */
    private void startBackgroundAnimations() {
        // Gradient animation
        startGradientAnimation();
        
        // Particle effects
        if (particleEffectView != null) {
            particleEffectView.startParticles();
        }
        
        // Title float animation
        startTitleFloatingAnimation();
    }
    
    /**
     * Animación del gradiente de fondo
     */
    private void startGradientAnimation() {
        ValueAnimator colorAnimator = ValueAnimator.ofArgb(
            Color.rgb(25, 25, 112),    // Midnight blue
            Color.rgb(72, 61, 139),    // Dark slate blue
            Color.rgb(138, 43, 226),   // Blue violet
            Color.rgb(25, 25, 112)     // Back to midnight blue
        );
        
        colorAnimator.setDuration(8000);
        colorAnimator.setRepeatCount(ValueAnimator.INFINITE);
        colorAnimator.addUpdateListener(animator -> {
            int color = (int) animator.getAnimatedValue();
            backgroundGradient.setBackgroundColor(color);
        });
        
        colorAnimator.start();
    }
    
    /**
     * Animación flotante del título
     */
    private void startTitleFloatingAnimation() {
        animationExecutor.scheduleWithFixedDelay(() -> {
            mainHandler.post(() -> {
                if (!isAnimating) {
                    float targetScale = (currentTitleScale == 1.0f) ? 1.05f : 1.0f;
                    
                    ObjectAnimator scaleAnimation = ObjectAnimator.ofFloat(titleText, "scaleX", 
                        currentTitleScale, targetScale);
                    scaleAnimation.setDuration(2000);
                    
                    ObjectAnimator scaleYAnimation = ObjectAnimator.ofFloat(titleText, "scaleY", 
                        currentTitleScale, targetScale);
                    scaleYAnimation.setDuration(2000);
                    
                    AnimatorSet titleAnimation = new AnimatorSet();
                    titleAnimation.playTogether(scaleAnimation, scaleYAnimation);
                    titleAnimation.addListener(new android.animation.Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(android.animation.Animator animation) {}
                        
                        @Override
                        public void onAnimationEnd(android.animation.Animator animation) {
                            currentTitleScale = targetScale;
                        }
                        
                        @Override
                        public void onAnimationCancel(android.animation.Animator animation) {}
                        
                        @Override
                        public void onAnimationRepeat(android.animation.Animator animation) {}
                    });
                    
                    titleAnimation.start();
                }
            });
        }, 0, 4, TimeUnit.SECONDS);
    }
    
    /**
     * Muestra el contenedor de nombre de usuario
     */
    private void showUsernameContainer() {
        usernameContainer.setVisibility(View.VISIBLE);
        ObjectAnimator slideDown = ObjectAnimator.ofFloat(usernameContainer, "translationY", -100f, 0f);
        slideDown.setDuration(300);
        slideDown.start();
    }
    
    /**
     * Guarda el nombre de usuario
     */
    private void saveUsername() {
        String username = usernameEditText.getText().toString().trim();
        if (username.isEmpty()) {
            Toast.makeText(this, "El nombre de usuario no puede estar vacío", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (username.length() > 20) {
            Toast.makeText(this, "El nombre de usuario es demasiado largo (máx. 20 caracteres)", Toast.LENGTH_SHORT).show();
            return;
        }
        
        currentUsername = username;
        saveUserSettings();
        
        // Animate the save action
        ObjectAnimator checkAnimation = ObjectAnimator.ofFloat(saveUsernameButton, "rotationY", 0f, 360f);
        checkAnimation.setDuration(500);
        checkAnimation.start();
        
        Toast.makeText(this, "Nombre de usuario guardado: " + currentUsername, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Genera un nombre de usuario aleatorio
     */
    private void generateRandomUsername() {
        String[] prefixes = {"Jugador", "Estratega", "Guerrero", "Leyenda", "Maestro", "Héroe", "Vikingo", "Dragón"};
        String[] suffixes = {"Invencible", "Veloz", "Sigiloso", "Inteligente", "Brillante", "Astuto", "Diestro", "Experto"};
        
        String randomName = prefixes[(int)(Math.random() * prefixes.length)] + 
                          suffixes[(int)(Math.random() * suffixes.length)] + 
                          (int)(Math.random() * 999);
        
        usernameEditText.setText(randomName);
        currentUsername = randomName;
        saveUserSettings();
        
        // Animate the button
        animateButtonPress(randomUsernameButton, null);
    }
    
    /**
     * Inicia el juego con la configuración actual
     */
    private void startGame() {
        if (currentUsername.isEmpty()) {
            Toast.makeText(this, "Por favor establece un nombre de usuario primero", Toast.LENGTH_LONG).show();
            return;
        }
        
        saveUserSettings();
        
        // Animate transition to game
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(menuContainer, "alpha", 1f, 0f);
        fadeOut.setDuration(500);
        
        fadeOut.addListener(new android.animation.Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(android.animation.Animator animation) {
                Toast.makeText(MainMenuActivity.this, 
                    "Iniciando " + selectedMode.getName() + " - " + selectedRole.getDisplayName(), 
                    Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                // Start the game activity
                Intent gameIntent = new Intent(MainMenuActivity.this, MainActivity.class);
                gameIntent.putExtra("selected_mode", selectedMode.name());
                gameIntent.putExtra("selected_role", selectedRole.name());
                gameIntent.putExtra("username", currentUsername);
                
                startActivity(gameIntent);
                finish();
            }
            
            @Override
            public void onAnimationCancel(android.animation.Animator animation) {}
            
            @Override
            public void onAnimationRepeat(android.animation.Animator animation) {}
        });
        
        fadeOut.start();
    }
    
    /**
     * Abre la configuración
     */
    private void openSettings() {
        Toast.makeText(this, "Configuración - Próximamente", Toast.LENGTH_SHORT).show();
        // TODO: Implement SettingsActivity
    }
    
    /**
     * Abre el perfil del jugador
     */
    private void openProfile() {
        Toast.makeText(this, "Perfil de " + currentUsername, Toast.LENGTH_SHORT).show();
        // TODO: Implement ProfileActivity
    }
    
    /**
     * Abre las puntuaciones
     */
    private void openScores() {
        Toast.makeText(this, "Puntuaciones - Próximamente", Toast.LENGTH_SHORT).show();
        // TODO: Implement ScoresActivity
    }
    
    /**
     * Abre los créditos
     */
    private void openCredits() {
        Toast.makeText(this, "Créditos - Próximamente", Toast.LENGTH_SHORT).show();
        // TODO: Implement CreditsActivity
    }
    
    /**
     * Sale del juego
     */
    private void quitGame() {
        // Animate quit
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(menuContainer, "alpha", 1f, 0f);
        fadeOut.setDuration(300);
        
        fadeOut.addListener(new android.animation.Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(android.animation.Animator animation) {
                Toast.makeText(MainMenuActivity.this, "¡Hasta la vista!", Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                finish();
                System.exit(0);
            }
            
            @Override
            public void onAnimationCancel(android.animation.Animator animation) {}
            
            @Override
            public void onAnimationRepeat(android.animation.Animator animation) {}
        });
        
        fadeOut.start();
    }
    
    /**
     * Simula el sonido de click de botón
     */
    private void playButtonClickSound() {
        // TODO: Implement actual sound playback
        android.util.Log.d(TAG, "Playing button click sound");
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Resume background animations
        startBackgroundAnimations();
        
        android.util.Log.d(TAG, "MainMenuActivity resumed");
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Pause animations to save resources
        if (animationExecutor != null && !animationExecutor.isShutdown()) {
            animationExecutor.shutdown();
            animationExecutor = Executors.newSingleThreadScheduledExecutor();
        }
        
        android.util.Log.d(TAG, "MainMenuActivity paused");
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources
        if (animationExecutor != null && !animationExecutor.isShutdown()) {
            animationExecutor.shutdown();
        }
        
        if (particleEffectView != null) {
            particleEffectView.stopParticles();
        }
        
        android.util.Log.i(TAG, "MainMenuActivity destroyed");
    }
    
    @Override
    public void onBackPressed() {
        // Override back button behavior
        quitGame();
    }
    
    /**
     * Vista personalizada para efectos de partículas
     */
    private static class ParticleEffectView extends View {
        private List<Particle> particles;
        private Paint particlePaint;
        private boolean isAnimating = false;
        private Handler particleHandler;
        private Random random;
        
        public ParticleEffectView(Activity context) {
            super(context);
            init();
        }
        
        private void init() {
            particles = new ArrayList<>();
            particlePaint = new Paint();
            particlePaint.setColor(Color.WHITE);
            particlePaint.setAlpha(128);
            particleHandler = new Handler(Looper.getMainLooper());
            random = new Random();
        }
        
        public void startParticles() {
            if (isAnimating) return;
            
            isAnimating = true;
            
            // Add new particles periodically
            particleHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (isAnimating) {
                        // Add new particle
                        Particle particle = new Particle(
                            random.nextInt(getWidth()),
                            random.nextInt(getHeight()),
                            random.nextFloat() * 2 + 1,
                            random.nextFloat() * 360
                        );
                        particles.add(particle);
                        
                        // Remove old particles
                        particles.removeIf(p -> p.life <= 0);
                        
                        // Update all particles
                        for (Particle particle : particles) {
                            particle.update();
                        }
                        
                        invalidate();
                        particleHandler.postDelayed(this, 50);
                    }
                }
            });
        }
        
        public void stopParticles() {
            isAnimating = false;
            if (particleHandler != null) {
                particleHandler.removeCallbacksAndMessages(null);
            }
        }
        
        @Override
        protected void onDraw(android.graphics.Canvas canvas) {
            super.onDraw(canvas);
            
            for (Particle particle : particles) {
                particle.draw(canvas, particlePaint);
            }
        }
        
        private static class Particle {
            float x, y;
            float size;
            float angle;
            int life;
            
            Particle(float x, float y, float size, float angle) {
                this.x = x;
                this.y = y;
                this.size = size;
                this.angle = angle;
                this.life = 255;
            }
            
            void update() {
                x += Math.cos(Math.toRadians(angle)) * 0.5f;
                y += Math.sin(Math.toRadians(angle)) * 0.5f;
                life -= 2;
            }
            
            void draw(android.graphics.Canvas canvas, Paint paint) {
                if (life <= 0) return;
                
                paint.setAlpha(life);
                canvas.drawCircle(x, y, size, paint);
            }
        }
    }
}