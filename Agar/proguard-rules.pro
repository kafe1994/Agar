# ================================================================
# PROGUARD RULES PARA ENHANCED AGAR GAME
# ================================================================
# Optimización para dispositivos de gama baja y reducción de APK
# ================================================================

# ================================================================
# 1. PRESERVAR ACTIVITIES Y COMPONENTES ANDROID
# ================================================================
# Todas las Activities deben mantenerse para el AndroidManifest
-keep public class com.gaming.enhancedagar.MainActivity
-keep public class com.gaming.enhancedagar.ui.MainMenuActivity
-keep public class com.gaming.enhancedagar.ui.GameOverActivity  
-keep public class com.gaming.enhancedagar.ui.SettingsActivity

# Preservar componentes de Android
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# ================================================================
# 2. PRESERVAR ENTIDADES PRINCIPALES DEL JUEGO
# ================================================================
# Clases de entidades que no deben ofuscarse
-keep class com.gaming.enhancedagar.entities.Entity {
    protected <fields>;
}
-keep class com.gaming.enhancedagar.entities.Player {
    protected <fields>;
}
-keep class com.gaming.enhancedagar.entities.EnemyBot {
    protected <fields>;
}
-keep class com.gaming.enhancedagar.entities.Food {
    protected <fields>;
}

# Preservar sistemas de juego principales
-keep class com.gaming.enhancedagar.game.GameState
-keep class com.gaming.enhancedagar.game.Physics
-keep class com.gaming.enhancedagar.game.CollisionSystem
-keep class com.gaming.enhancedagar.game.MovementSystem

# ================================================================
# 3. PRESERVAR INTERFACES PÚBLICOS
# ================================================================
# Interfaces para sistemas del juego
-keep interface com.gaming.enhancedagar.engine.**
-keep interface com.gaming.enhancedagar.game.**
-keep interface com.gaming.enhancedagar.entities.**
-keep interface com.gaming.enhancedagar.ui.**

# Preservar callbacks y listeners
-keep public interface * {
    public <methods>;
}

# ================================================================
# 4. OPTIMIZACIONES ESPECÍFICAS PARA CANVAS Y RENDERING
# ================================================================
# Optimizaciones para Canvas y gráficos (crítico para juegos)
-dontwarn android.graphics.**
-dontwarn android.opengl.**

# Preservar métodos de rendering importantes
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Canvas optimizations para dispositivos de gama baja
-keepclassmembers class com.gaming.enhancedagar.engine.Renderer {
    public void render(**);
    public void update(**);
}
-keepclassmembers class com.gaming.enhancedagar.engine.TextRenderer {
    public void drawText(**);
    public void measureText(**);
}

# Preservar optimizaciones de visualización
-keepclassmembers class com.gaming.enhancedagar.engine.VisualEffects {
    public void createEffect(**);
    public void updateEffect(**);
}

# ================================================================
# 5. OPTIMIZACIONES PARA MOTOR DE JUEGO
# ================================================================
# Game Engine optimizaciones
-keepclassmembers class com.gaming.enhancedagar.engine.GameEngine {
    public void startGame();
    public void pauseGame();
    public void resumeGame();
    public void stopGame();
    public void updateGame();
    public void renderFrame();
}

# Camera Manager para optimización de viewport
-keepclassmembers class com.gaming.enhancedagar.game.CameraManager {
    public void updateCamera(**);
    public void zoomTo(**);
    public void focusOn(**);
}

# Input Handler para dispositivos táctiles
-keepclassmembers class com.gaming.enhancedagar.game.InputHandler {
    public void handleTouch(**);
    public void handleGesture(**);
}

# ================================================================
# 6. SISTEMAS AVANZADOS DEL JUEGO
# ================================================================
# Power Up System
-keepclassmembers class com.gaming.enhancedagar.game.PowerUpSystem {
    public void activatePowerUp(**);
    public void deactivatePowerUp(**);
}

# Division System (mecánica única del juego)
-keepclassmembers class com.gaming.enhancedagar.game.DivisionSystem {
    public void splitEntity(**);
    public void mergeEntities(**);
}

# Team System
-keepclassmembers class com.gaming.enhancedagar.game.TeamSystem {
    public void joinTeam(**);
    public void switchTeam(**);
}

# Special Ability System
-keepclassmembers class com.gaming.enhancedagar.game.SpecialAbilitySystem {
    public void activateAbility(**);
    public void useSpecialMove(**);
}

# Game Mode Manager
-keepclassmembers class com.gaming.enhancedagar.game.GameModeManager {
    public void switchMode(**);
    public void loadMode(**);
}

# ================================================================
# 7. OPTIMIZACIONES DE PARTÍCULAS Y EFECTOS
# ================================================================
# Particle System (crítico para performance)
-keepclassmembers class com.gaming.enhancedagar.engine.ParticleSystem {
    public void createParticle(**);
    public void updateParticles(**);
    public void renderParticles(**);
}

# Background Renderer optimizado
-keepclassmembers class com.gaming.enhancedagar.engine.BackgroundRenderer {
    public void renderBackground(**);
    public void updateBackground(**);
}

# ================================================================
# 8. OFUSCAR INFORMACIÓN SENSIBLE
# ================================================================
# Ofuscar clases de configuración interna
-keep class !com.gaming.enhancedagar.ui.**,
        !com.gaming.enhancedagar.game.**,
        !com.gaming.enhancedagar.entities.**,
        !com.gaming.enhancedagar.engine.** {
    *;
}

# Preservar version info para debugging (ofuscar contenido)
-keep class com.gaming.enhancedagar.utils.VersionInfo {
    private <fields>;
    public void getVersionName();
    public void getVersionCode();
}

# ================================================================
# 9. OPTIMIZACIONES PARA DISPOSITIVOS DE GAMA BAJA
# ================================================================
# Configuraciones específicas para memoria limitada
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose

# Optimizaciones de método y constantes
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# Mantener línea de números para stack traces
-keepattributes SourceFile,LineNumberTable

# ================================================================
# 10. MINIMIZAR TAMAÑO DEL APK
# ================================================================
# Remover clases y métodos no utilizados
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Optimizaciones de cadenas
-optimizations !code/simplification/string

# Reducir overhead de clases
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# ================================================================
# 11. CONFIGURACIONES ESPECÍFICAS PARA JUEGO
# ================================================================
# Game Thread optimizado
-keepclassmembers class com.gaming.enhancedagar.game.GameThread {
    public void startGameLoop();
    public void stopGameLoop();
    public boolean isRunning();
}

# Coordinate System para cálculos de juego
-keepclassmembers class com.gaming.enhancedagar.game.CoordinateSystem {
    public void worldToScreen(**);
    public void screenToWorld(**);
    public void normalizeCoordinates(**);
}

# UIManager optimizado
-keepclassmembers class com.gaming.enhancedagar.ui.UIManager {
    public void updateUI(**);
    public void hideUI(**);
    public void showUI(**);
}

# GameHUD optimizado
-keepclassmembers class com.gaming.enhancedagar.ui.GameHUD {
    public void updateHUD(**);
    public void renderHUD(**);
}

# Role System
-keepclassmembers class com.gaming.enhancedagar.engine.RoleSystem {
    public void assignRole(**);
    public void updateRole(**);
}

# ================================================================
# 12. EXCLUSIONES ESPECÍFICAS
# ================================================================
# Excluir clases de testing (si las hay)
-dontwarn org.junit.**
-dontwarn junit.framework.**

# Excluir warnings de librerías comunes
-dontwarn org.apache.**
-dontwarn javax.annotation.**

# Excluir warnings de Firebase (si se usa)
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ================================================================
# 13. OPTIMIZACIONES FINALES PARA GAMA BAJA
# ================================================================
# Habilitar optimización agresiva
-maxproguardwarnings
-allowaccessmodification
-repackageclasses ''
-keepattributes **

# Configuración final para dispositivos con limitaciones
# Estas reglas aseguran que el juego funcione en dispositivos
# con poca memoria y procesadores lentos
-keepclassmembers class ** {
    @android.webkit.JavascriptInterface <methods>;
}

# Preservar constructores de clases principales
-keepclassmembers class * {
    public <init>(**);
    protected <init>(**);
}

# ================================================================
# 14. CONFIGURACIONES DE COMPATIBILIDAD
# ================================================================
# Compatibilidad con diferentes versiones de Android
-dontwarn java.lang.instrument.ClassFileTransformer
-dontwarn sun.misc.SignalHandler
-dontwarn java.lang.instrument.Instrumentation
-dontwarn sun.misc.Signal

# ================================================================
# FIN DE LAS REGLAS DE PROGUARD
# ================================================================