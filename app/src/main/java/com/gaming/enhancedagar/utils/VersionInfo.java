package com.gaming.enhancedagar.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Clase VersionInfo - Maneja información de versión, build automático y analytics
 * Características:
 * - Información de versión del juego
 * - Build automático con timestamp
 * - Información de compilación
 * - Sistema de actualización automática
 * - Logging de información del dispositivo
 * - Compatibilidad con analytics
 * - Configuración para release/debug builds
 */
public class VersionInfo {
    private static final String TAG = "VersionInfo";
    private static final String VERSION_FILE = "version_info.dat";
    private static final String UPDATE_CHECK_FILE = "last_update_check.dat";
    
    // Información de versión
    private String versionName;
    private int versionCode;
    private String buildNumber;
    private String buildTimestamp;
    private String gitCommit;
    private String branchName;
    private boolean isDebugBuild;
    
    // Información del dispositivo
    private String deviceModel;
    private String androidVersion;
    private String sdkVersion;
    private String manufacturer;
    private String totalMemory;
    private String availableMemory;
    private String diskSpace;
    
    // Analytics y tracking
    private Map<String, Object> analyticsData;
    private String sessionId;
    private long startTime;
    
    private Context context;
    private static VersionInfo instance;
    
    private VersionInfo(Context context) {
        this.context = context.getApplicationContext();
        this.analyticsData = new HashMap<>();
        this.startTime = System.currentTimeMillis();
        this.sessionId = generateSessionId();
        
        initializeVersionInfo();
        initializeDeviceInfo();
        saveVersionInfo();
        
        Log.i(TAG, "VersionInfo inicializada correctamente");
    }
    
    public static synchronized VersionInfo getInstance(Context context) {
        if (instance == null) {
            instance = new VersionInfo(context);
        }
        return instance;
    }
    
    /**
     * Inicializa la información de versión del juego
     */
    private void initializeVersionInfo() {
        try {
            PackageManager pm = context.getPackageManager();
            String packageName = context.getPackageName();
            
            // Información de versión desde PackageManager
            versionName = pm.getPackageInfo(packageName, 0).versionName;
            versionCode = pm.getPackageInfo(packageName, 0).versionCode;
            
            // Build automático con timestamp
            buildTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());
            
            // Información de compilación
            buildNumber = generateBuildNumber();
            gitCommit = getGitCommitHash();
            branchName = getGitBranchName();
            
            // Determinar tipo de build
            isDebugBuild = checkDebugBuild();
            
        } catch (Exception e) {
            Log.e(TAG, "Error inicializando información de versión", e);
            setDefaultVersionInfo();
        }
    }
    
    /**
     * Inicializa información del dispositivo
     */
    private void initializeDeviceInfo() {
        deviceModel = Build.MODEL;
        androidVersion = Build.VERSION.RELEASE;
        sdkVersion = String.valueOf(Build.VERSION.SDK_INT);
        manufacturer = Build.MANUFACTURER;
        
        // Información de memoria
        totalMemory = formatBytes(Runtime.getRuntime().totalMemory());
        availableMemory = formatBytes(Runtime.getRuntime().freeMemory());
        
        // Información de almacenamiento
        diskSpace = getDiskSpaceInfo();
        
        // Agregar al analytics data
        analyticsData.put("device_model", deviceModel);
        analyticsData.put("android_version", androidVersion);
        analyticsData.put("sdk_version", sdkVersion);
        analyticsData.put("manufacturer", manufacturer);
        analyticsData.put("total_memory", totalMemory);
        analyticsData.put("available_memory", availableMemory);
        analyticsData.put("disk_space", diskSpace);
    }
    
    /**
     * Genera número de build automático
     */
    private String generateBuildNumber() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmm", Locale.getDefault());
        return "BUILD_" + dateFormat.format(new Date());
    }
    
    /**
     * Obtiene hash del commit de Git
     */
    private String getGitCommitHash() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{
                "git", "rev-parse", "--short", "HEAD"
            });
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String commitHash = reader.readLine();
            process.waitFor();
            return commitHash != null ? commitHash : "unknown";
        } catch (Exception e) {
            Log.w(TAG, "No se pudo obtener hash de Git", e);
            return "unknown";
        }
    }
    
    /**
     * Obtiene nombre de la rama de Git
     */
    private String getGitBranchName() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{
                "git", "rev-parse", "--abbrev-ref", "HEAD"
            });
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String branch = reader.readLine();
            process.waitFor();
            return branch != null ? branch : "unknown";
        } catch (Exception e) {
            Log.w(TAG, "No se pudo obtener rama de Git", e);
            return "unknown";
        }
    }
    
    /**
     * Determina si es build de debug
     */
    private boolean checkDebugBuild() {
        return (context.getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }
    
    /**
     * Formatea bytes a formato legible
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.getDefault(), "%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format(Locale.getDefault(), "%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format(Locale.getDefault(), "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    /**
     * Obtiene información de espacio en disco
     */
    private String getDiskSpaceInfo() {
        try {
            File internalDir = context.getFilesDir();
            long totalSpace = internalDir.getTotalSpace();
            long freeSpace = internalDir.getFreeSpace();
            return String.format(Locale.getDefault(), 
                "Total: %s, Free: %s", 
                formatBytes(totalSpace), 
                formatBytes(freeSpace));
        } catch (Exception e) {
            Log.w(TAG, "Error obteniendo información de disco", e);
            return "Unknown";
        }
    }
    
    /**
     * Genera ID de sesión único
     */
    private String generateSessionId() {
        return "SESSION_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
    }
    
    /**
     * Establece valores por defecto en caso de error
     */
    private void setDefaultVersionInfo() {
        versionName = "1.0.0";
        versionCode = 1;
        buildNumber = "BUILD_DEFAULT";
        buildTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(new Date());
        gitCommit = "unknown";
        branchName = "unknown";
        isDebugBuild = true;
    }
    
    /**
     * Guarda información de versión en archivo local
     */
    private void saveVersionInfo() {
        try {
            File file = new File(context.getFilesDir(), VERSION_FILE);
            FileOutputStream fos = new FileOutputStream(file);
            
            StringBuilder data = new StringBuilder();
            data.append("version_name=").append(versionName).append("\n");
            data.append("version_code=").append(versionCode).append("\n");
            data.append("build_number=").append(buildNumber).append("\n");
            data.append("build_timestamp=").append(buildTimestamp).append("\n");
            data.append("git_commit=").append(gitCommit).append("\n");
            data.append("branch_name=").append(branchName).append("\n");
            data.append("is_debug=").append(isDebugBuild).append("\n");
            data.append("device_model=").append(deviceModel).append("\n");
            data.append("android_version=").append(androidVersion).append("\n");
            data.append("sdk_version=").append(sdkVersion).append("\n");
            data.append("manufacturer=").append(manufacturer).append("\n");
            
            fos.write(data.toString().getBytes());
            fos.close();
            
            Log.i(TAG, "Información de versión guardada exitosamente");
        } catch (IOException e) {
            Log.e(TAG, "Error guardando información de versión", e);
        }
    }
    
    /**
     * Carga información de versión desde archivo local
     */
    public void loadVersionInfo() {
        try {
            File file = new File(context.getFilesDir(), VERSION_FILE);
            if (!file.exists()) return;
            
            FileInputStream fis = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            
            String line;
            Map<String, String> data = new HashMap<>();
            
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    data.put(parts[0], parts[1]);
                }
            }
            
            // Cargar datos si están disponibles
            if (data.containsKey("version_name")) versionName = data.get("version_name");
            if (data.containsKey("version_code")) versionCode = Integer.parseInt(data.get("version_code"));
            if (data.containsKey("build_number")) buildNumber = data.get("build_number");
            if (data.containsKey("git_commit")) gitCommit = data.get("git_commit");
            if (data.containsKey("branch_name")) branchName = data.get("branch_name");
            if (data.containsKey("is_debug")) isDebugBuild = Boolean.parseBoolean(data.get("is_debug"));
            
            fis.close();
            
            Log.i(TAG, "Información de versión cargada exitosamente");
        } catch (Exception e) {
            Log.e(TAG, "Error cargando información de versión", e);
        }
    }
    
    /**
     * Verifica actualizaciones disponibles
     */
    public void checkForUpdates(UpdateCheckCallback callback) {
        new Thread(() -> {
            try {
                // Simular verificación de actualización
                // En implementación real, aquí iría la llamada al servidor
                
                long lastCheck = getLastUpdateCheck();
                long currentTime = System.currentTimeMillis();
                
                // Verificar cada 4 horas
                if (currentTime - lastCheck > 4 * 60 * 60 * 1000) {
                    saveLastUpdateCheck(currentTime);
                    
                    // En un caso real, aquí verificaríamos en el servidor
                    boolean updateAvailable = false; // placeholder
                    String newVersion = null;
                    
                    callback.onUpdateCheckComplete(updateAvailable, newVersion);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error verificando actualizaciones", e);
                callback.onUpdateCheckComplete(false, null);
            }
        }).start();
    }
    
    /**
     * Obtiene timestamp de última verificación de actualización
     */
    private long getLastUpdateCheck() {
        try {
            File file = new File(context.getFilesDir(), UPDATE_CHECK_FILE);
            if (!file.exists()) return 0;
            
            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            fis.close();
            
            return Long.parseLong(new String(data));
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Guarda timestamp de verificación de actualización
     */
    private void saveLastUpdateCheck(long timestamp) {
        try {
            File file = new File(context.getFilesDir(), UPDATE_CHECK_FILE);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(String.valueOf(timestamp).getBytes());
            fos.close();
        } catch (IOException e) {
            Log.e(TAG, "Error guardando timestamp de verificación", e);
        }
    }
    
    /**
     * Registra información del dispositivo para analytics
     */
    public void logDeviceInfo() {
        StringBuilder logInfo = new StringBuilder();
        logInfo.append("=== INFORMACIÓN DEL DISPOSITIVO ===\n");
        logInfo.append("Modelo: ").append(deviceModel).append("\n");
        logInfo.append("Android: ").append(androidVersion).append("\n");
        logInfo.append("SDK: ").append(sdkVersion).append("\n");
        logInfo.append("Fabricante: ").append(manufacturer).append("\n");
        logInfo.append("Memoria Total: ").append(totalMemory).append("\n");
        logInfo.append("Memoria Disponible: ").append(availableMemory).append("\n");
        logInfo.append("Espacio Disco: ").append(diskSpace).append("\n");
        logInfo.append("===================================");
        
        Log.i(TAG, logInfo.toString());
    }
    
    /**
     * Agrega evento de analytics
     */
    public void addAnalyticsEvent(String eventName, Map<String, Object> eventData) {
        analyticsData.put("event_" + eventName, eventData);
        analyticsData.put("event_timestamp", System.currentTimeMillis());
        
        Log.d(TAG, "Evento de analytics agregado: " + eventName);
    }
    
    /**
     * Obtiene datos completos para analytics
     */
    public Map<String, Object> getAnalyticsData() {
        Map<String, Object> fullData = new HashMap<>(analyticsData);
        fullData.put("version_name", versionName);
        fullData.put("version_code", versionCode);
        fullData.put("build_number", buildNumber);
        fullData.put("build_timestamp", buildTimestamp);
        fullData.put("git_commit", gitCommit);
        fullData.put("branch_name", branchName);
        fullData.put("is_debug_build", isDebugBuild);
        fullData.put("session_id", sessionId);
        fullData.put("session_duration", System.currentTimeMillis() - startTime);
        
        return fullData;
    }
    
    /**
     * Verifica compatibilidad con versión mínima requerida
     */
    public boolean isCompatibleWith(int minVersionCode) {
        return versionCode >= minVersionCode;
    }
    
    /**
     * Obtiene información completa de la versión
     */
    public String getFullVersionInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Versión: ").append(versionName).append(" (").append(versionCode).append(")\n");
        info.append("Build: ").append(buildNumber).append("\n");
        info.append("Timestamp: ").append(buildTimestamp).append("\n");
        info.append("Git: ").append(gitCommit).append("\n");
        info.append("Rama: ").append(branchName).append("\n");
        info.append("Tipo: ").append(isDebugBuild ? "Debug" : "Release");
        return info.toString();
    }
    
    // Getters para información de versión
    public String getVersionName() { return versionName; }
    public int getVersionCode() { return versionCode; }
    public String getBuildNumber() { return buildNumber; }
    public String getBuildTimestamp() { return buildTimestamp; }
    public String getGitCommit() { return gitCommit; }
    public String getBranchName() { return branchName; }
    public boolean isDebugBuild() { return isDebugBuild; }
    public String getSessionId() { return sessionId; }
    
    // Getters para información del dispositivo
    public String getDeviceModel() { return deviceModel; }
    public String getAndroidVersion() { return androidVersion; }
    public String getSdkVersion() { return sdkVersion; }
    public String getManufacturer() { return manufacturer; }
    public String getTotalMemory() { return totalMemory; }
    public String getAvailableMemory() { return availableMemory; }
    public String getDiskSpace() { return diskSpace; }
    
    /**
     * Interface para callback de verificación de actualización
     */
    public interface UpdateCheckCallback {
        void onUpdateCheckComplete(boolean updateAvailable, String newVersion);
    }
}