package com.gaming.enhancedagar.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.gaming.enhancedagar.R;

/**
 * Actividad de configuración completa del juego Enhanced Agar
 * Permite configurar gráficos, audio, controles, red e idioma
 */
public class SettingsActivity extends Activity {
    
    private static final String PREFS_NAME = "EnhancedAgarSettings";
    private static final String TAG = "SettingsActivity";
    
    // Configuraciones gráficas
    private SeekBar fpsSeekBar;
    private SeekBar graphicsQualitySeekBar;
    private SeekBar particleDetailSeekBar;
    private Switch antiAliasingSwitch;
    private Switch shadowsSwitch;
    private TextView fpsValue;
    private TextView graphicsQualityValue;
    private TextView particleDetailValue;
    
    // Configuraciones de audio
    private SeekBar musicVolumeSeekBar;
    private SeekBar sfxVolumeSeekBar;
    private Switch musicEnabledSwitch;
    private Switch sfxEnabledSwitch;
    private TextView musicVolumeValue;
    private TextView sfxVolumeValue;
    
    // Configuraciones de controles
    private SeekBar sensitivitySeekBar;
    private SeekBar zoomSpeedSeekBar;
    private Switch virtualJoystickSwitch;
    private Switch swipeControlsSwitch;
    private TextView sensitivityValue;
    private TextView zoomSpeedValue;
    
    // Configuraciones de red
    private Switch onlineModeSwitch;
    private Switch showPlayersSwitch;
    private SeekBar networkQualitySeekBar;
    private TextView networkQualityValue;
    
    // Configuraciones de idioma
    private Button languageButton;
    private TextView currentLanguageText;
    
    // Botones principales
    private Button saveButton;
    private Button cancelButton;
    private Button applyButton;
    private Button resetButton;
    
    // Configuraciones temporales para preview
    private SharedPreferences tempPrefs;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        initializeViews();
        setupListeners();
        loadSettings();
        updateUI();
    }
    
    private void initializeViews() {
        // Configuraciones gráficas
        fpsSeekBar = findViewById(R.id.seekBar_fps);
        graphicsQualitySeekBar = findViewById(R.id.seekBar_graphics_quality);
        particleDetailSeekBar = findViewById(R.id.seekBar_particle_detail);
        antiAliasingSwitch = findViewById(R.id.switch_anti_aliasing);
        shadowsSwitch = findViewById(R.id.switch_shadows);
        fpsValue = findViewById(R.id.text_fps_value);
        graphicsQualityValue = findViewById(R.id.text_graphics_quality_value);
        particleDetailValue = findViewById(R.id.text_particle_detail_value);
        
        // Configuraciones de audio
        musicVolumeSeekBar = findViewById(R.id.seekBar_music_volume);
        sfxVolumeSeekBar = findViewById(R.id.seekBar_sfx_volume);
        musicEnabledSwitch = findViewById(R.id.switch_music_enabled);
        sfxEnabledSwitch = findViewById(R.id.switch_sfx_enabled);
        musicVolumeValue = findViewById(R.id.text_music_volume_value);
        sfxVolumeValue = findViewById(R.id.text_sfx_volume_value);
        
        // Configuraciones de controles
        sensitivitySeekBar = findViewById(R.id.seekBar_sensitivity);
        zoomSpeedSeekBar = findViewById(R.id.seekBar_zoom_speed);
        virtualJoystickSwitch = findViewById(R.id.switch_virtual_joystick);
        swipeControlsSwitch = findViewById(R.id.switch_swipe_controls);
        sensitivityValue = findViewById(R.id.text_sensitivity_value);
        zoomSpeedValue = findViewById(R.id.text_zoom_speed_value);
        
        // Configuraciones de red
        onlineModeSwitch = findViewById(R.id.switch_online_mode);
        showPlayersSwitch = findViewById(R.id.switch_show_players);
        networkQualitySeekBar = findViewById(R.id.seekBar_network_quality);
        networkQualityValue = findViewById(R.id.text_network_quality_value);
        
        // Configuraciones de idioma
        languageButton = findViewById(R.id.button_language);
        currentLanguageText = findViewById(R.id.text_current_language);
        
        // Botones principales
        saveButton = findViewById(R.id.button_save);
        cancelButton = findViewById(R.id.button_cancel);
        applyButton = findViewById(R.id.button_apply);
        resetButton = findViewById(R.id.button_reset);
    }
    
    private void setupListeners() {
        // SeekBars para valores numéricos
        fpsSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                fpsValue.setText(progress + " FPS");
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        graphicsQualitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                String[] quality = {"Bajo", "Medio", "Alto", "Ultra", "Máximo"};
                graphicsQualityValue.setText(quality[Math.min(progress, quality.length-1)]);
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        particleDetailSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                particleDetailValue.setText(progress + "%");
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        musicVolumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                musicVolumeValue.setText(progress + "%");
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        sfxVolumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sfxVolumeValue.setText(progress + "%");
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        sensitivitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sensitivityValue.setText(progress + "%");
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        zoomSpeedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                zoomSpeedValue.setText(progress + "%");
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        networkQualitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                String[] quality = {"Baja", "Estándar", "Alta", "Óptima"};
                networkQualityValue.setText(quality[Math.min(progress, quality.length-1)]);
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // Botones principales
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
                Toast.makeText(SettingsActivity.this, "Configuraciones guardadas", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
        
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tempPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        
        applyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
                Toast.makeText(SettingsActivity.this, "Configuraciones aplicadas", Toast.LENGTH_SHORT).show();
            }
        });
        
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetToDefaults();
                Toast.makeText(SettingsActivity.this, "Configuraciones restablecidas", Toast.LENGTH_SHORT).show();
            }
        });
        
        languageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLanguageSelectionDialog();
            }
        });
    }
    
    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        // Configuraciones gráficas
        fpsSeekBar.setProgress(prefs.getInt("fps", 60));
        graphicsQualitySeekBar.setProgress(prefs.getInt("graphics_quality", 2));
        particleDetailSeekBar.setProgress(prefs.getInt("particle_detail", 75));
        antiAliasingSwitch.setChecked(prefs.getBoolean("anti_aliasing", true));
        shadowsSwitch.setChecked(prefs.getBoolean("shadows", true));
        
        // Configuraciones de audio
        musicVolumeSeekBar.setProgress(prefs.getInt("music_volume", 80));
        sfxVolumeSeekBar.setProgress(prefs.getInt("sfx_volume", 90));
        musicEnabledSwitch.setChecked(prefs.getBoolean("music_enabled", true));
        sfxEnabledSwitch.setChecked(prefs.getBoolean("sfx_enabled", true));
        
        // Configuraciones de controles
        sensitivitySeekBar.setProgress(prefs.getInt("sensitivity", 50));
        zoomSpeedSeekBar.setProgress(prefs.getInt("zoom_speed", 60));
        virtualJoystickSwitch.setChecked(prefs.getBoolean("virtual_joystick", false));
        swipeControlsSwitch.setChecked(prefs.getBoolean("swipe_controls", true));
        
        // Configuraciones de red
        onlineModeSwitch.setChecked(prefs.getBoolean("online_mode", false));
        showPlayersSwitch.setChecked(prefs.getBoolean("show_players", true));
        networkQualitySeekBar.setProgress(prefs.getInt("network_quality", 2));
        
        // Configuración de idioma
        String currentLanguage = prefs.getString("language", "es");
        updateLanguageDisplay(currentLanguage);
    }
    
    private void updateUI() {
        // Actualizar valores de texto para sliders
        fpsValue.setText(fpsSeekBar.getProgress() + " FPS");
        
        String[] quality = {"Bajo", "Medio", "Alto", "Ultra", "Máximo"};
        graphicsQualityValue.setText(quality[Math.min(graphicsQualitySeekBar.getProgress(), quality.length-1)]);
        
        particleDetailValue.setText(particleDetailSeekBar.getProgress() + "%");
        musicVolumeValue.setText(musicVolumeSeekBar.getProgress() + "%");
        sfxVolumeValue.setText(sfxVolumeSeekBar.getProgress() + "%");
        sensitivityValue.setText(sensitivitySeekBar.getProgress() + "%");
        zoomSpeedValue.setText(zoomSpeedSeekBar.getProgress() + "%");
        
        String[] netQuality = {"Baja", "Estándar", "Alta", "Óptima"};
        networkQualityValue.setText(netQuality[Math.min(networkQualitySeekBar.getProgress(), netQuality.length-1)]);
    }
    
    private void saveSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        // Configuraciones gráficas
        editor.putInt("fps", fpsSeekBar.getProgress());
        editor.putInt("graphics_quality", graphicsQualitySeekBar.getProgress());
        editor.putInt("particle_detail", particleDetailSeekBar.getProgress());
        editor.putBoolean("anti_aliasing", antiAliasingSwitch.isChecked());
        editor.putBoolean("shadows", shadowsSwitch.isChecked());
        
        // Configuraciones de audio
        editor.putInt("music_volume", musicVolumeSeekBar.getProgress());
        editor.putInt("sfx_volume", sfxVolumeSeekBar.getProgress());
        editor.putBoolean("music_enabled", musicEnabledSwitch.isChecked());
        editor.putBoolean("sfx_enabled", sfxEnabledSwitch.isChecked());
        
        // Configuraciones de controles
        editor.putInt("sensitivity", sensitivitySeekBar.getProgress());
        editor.putInt("zoom_speed", zoomSpeedSeekBar.getProgress());
        editor.putBoolean("virtual_joystick", virtualJoystickSwitch.isChecked());
        editor.putBoolean("swipe_controls", swipeControlsSwitch.isChecked());
        
        // Configuraciones de red
        editor.putBoolean("online_mode", onlineModeSwitch.isChecked());
        editor.putBoolean("show_players", showPlayersSwitch.isChecked());
        editor.putInt("network_quality", networkQualitySeekBar.getProgress());
        
        editor.apply();
        
        // Notificar a la actividad principal sobre los cambios
        notifySettingsChanged();
    }
    
    private void resetToDefaults() {
        // Configuraciones gráficas por defecto
        fpsSeekBar.setProgress(60);
        graphicsQualitySeekBar.setProgress(2);
        particleDetailSeekBar.setProgress(75);
        antiAliasingSwitch.setChecked(true);
        shadowsSwitch.setChecked(true);
        
        // Configuraciones de audio por defecto
        musicVolumeSeekBar.setProgress(80);
        sfxVolumeSeekBar.setProgress(90);
        musicEnabledSwitch.setChecked(true);
        sfxEnabledSwitch.setChecked(true);
        
        // Configuraciones de controles por defecto
        sensitivitySeekBar.setProgress(50);
        zoomSpeedSeekBar.setProgress(60);
        virtualJoystickSwitch.setChecked(false);
        swipeControlsSwitch.setChecked(true);
        
        // Configuraciones de red por defecto
        onlineModeSwitch.setChecked(false);
        showPlayersSwitch.setChecked(true);
        networkQualitySeekBar.setProgress(2);
        
        // Idioma por defecto
        updateLanguageDisplay("es");
        
        updateUI();
    }
    
    private void showLanguageSelectionDialog() {
        String[] languages = {"Español", "English", "Français", "Deutsch", "Italiano", "Português"};
        String[] languageCodes = {"es", "en", "fr", "de", "it", "pt"};
        
        final SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String currentLanguage = prefs.getString("language", "es");
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Seleccionar Idioma");
        
        int selectedIndex = 0;
        for (int i = 0; i < languageCodes.length; i++) {
            if (languageCodes[i].equals(currentLanguage)) {
                selectedIndex = i;
                break;
            }
        }
        
        builder.setSingleChoiceItems(languages, selectedIndex, new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                String selectedCode = languageCodes[which];
                updateLanguageDisplay(selectedCode);
                dialog.dismiss();
            }
        });
        
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }
    
    private void updateLanguageDisplay(String languageCode) {
        String languageName;
        switch (languageCode) {
            case "es": languageName = "Español"; break;
            case "en": languageName = "English"; break;
            case "fr": languageName = "Français"; break;
            case "de": languageName = "Deutsch"; break;
            case "it": languageName = "Italiano"; break;
            case "pt": languageName = "Português"; break;
            default: languageName = "Español"; break;
        }
        
        currentLanguageText.setText(languageName);
        
        // Guardar idioma seleccionado
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("language", languageCode);
        editor.apply();
    }
    
    private void notifySettingsChanged() {
        // Enviar broadcast para notificar cambios de configuración
        Intent intent = new Intent("com.gaming.enhancedagar.SETTINGS_CHANGED");
        sendBroadcast(intent);
    }
    
    @Override
    public void onBackPressed() {
        // Guardar automáticamente antes de salir
        saveSettings();
        super.onBackPressed();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Guardar configuraciones automáticamente al pausar
        saveSettings();
    }
    
    /**
     * Método para obtener todas las configuraciones
     * @return SharedPreferences con todas las configuraciones guardadas
     */
    public static SharedPreferences getSettings(Activity activity) {
        return activity.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }
    
    /**
     * Método para aplicar configuraciones específicas desde código
     */
    public static void applySettings(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        // Aquí se aplicarían las configuraciones al motor del juego
        // Por ejemplo: establecer FPS, calidad gráfica, volumen, etc.
        
        Intent intent = new Intent("com.gaming.enhancedagar.APPLY_SETTINGS");
        activity.sendBroadcast(intent);
    }
}