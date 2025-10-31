#!/bin/bash

# ==========================================
# SCRIPT DE COMPILACIÓN PARA TERMUX
# Enhanced Agar Game - Versión Reconstruida
# ==========================================

echo "🚀 CONFIGURACIÓN Y COMPILACIÓN PARA TERMUX"
echo "Enhanced Agar Game - Reconstruido para Termux"
echo "=========================================="

# VARIABLES DEL PROYECTO
PROJECT_NAME="EnhancedAgarGame_Reconstruido"
PACKAGE_NAME="com.gaming.enhancedagar"
PROJECT_PATH="/storage/emulated/0/my_projects/agar"

# 1. VERIFICAR ENTORNO JAVA
echo "☕ Verificando Java..."
if [ -z "$JAVA_HOME" ]; then
    echo "export JAVA_HOME=$HOME/lib/jvm/java-21-openjdk" >> ~/.zshrc
    echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.zshrc
    echo "✅ Java configurado en ~/.zshrc"
    export JAVA_HOME=$HOME/lib/jvm/java-21-openjdk
    export PATH=$JAVA_HOME/bin:$PATH
else
    echo "✅ Java ya está configurado: $JAVA_HOME"
fi

# Verificar que Java funciona
if ! java -version &> /dev/null; then
    echo "❌ Error: Java no funciona correctamente"
    echo "💡 Solución: pkg install openjdk-17"
    exit 1
fi

# 2. VERIFICAR ANDROID SDK
echo "📱 Verificando Android SDK..."
if [ -z "$ANDROID_HOME" ]; then
    echo "export ANDROID_HOME=$HOME/android-sdk" >> ~/.zshrc
    echo 'export PATH=$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools:$PATH' >> ~/.zshrc
    echo "✅ Android SDK configurado en ~/.zshrc"
    export ANDROID_HOME=$HOME/android-sdk
    export PATH=$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools:$PATH
else
    echo "✅ Android SDK ya está configurado: $ANDROID_HOME"
fi

# Verificar herramientas Android
if [ ! -d "$ANDROID_HOME/platform-tools" ]; then
    echo "❌ Error: Android SDK no encontrado"
    echo "💡 Solución: pkg install android-tools"
    exit 1
fi

# 3. VERIFICAR PERMISOS
echo "🔧 Configurando permisos..."
chmod +x gradlew
chmod +x gradlew.bat
echo "✅ Permisos de gradlew configurados"

# 4. LIMPIAR BUILD ANTERIOR
echo "🧹 Limpiando build anterior..."
./gradlew clean --stacktrace

echo ""
echo "=========================================="
echo "🎯 INICIANDO COMPILACIÓN CON AAPT2 DISABLED"
echo "=========================================="

# 5. COMPILAR APK CON CONFIGURACIÓN AAPT2
echo "🔨 Compilando APK (esto puede tomar varios minutos)..."
echo "⚠️  IMPORTANTE: La configuración usa legacy resources para evitar AAPT2"

./gradlew assembleDebug --stacktrace --info

echo ""
echo "=========================================="
echo "📋 VERIFICANDO RESULTADOS"
echo "=========================================="

# 6. VERIFICAR SI EL APK SE GENERÓ
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    echo "🎉 ¡ÉXITO! APK generado correctamente"
    echo "📍 Ubicación: $APK_PATH"
    echo "📊 Información del APK:"
    ls -la "$APK_PATH"
    echo ""
    echo "📱 Para instalar en tu dispositivo Android:"
    echo "   adb install $APK_PATH"
    echo ""
    echo "📋 O transferir manualmente a:"
    echo "   $PROJECT_PATH/$APK_PATH"
else
    echo "❌ Error: No se pudo generar el APK"
    echo "📋 Revisa los logs anteriores para más detalles"
    echo ""
    echo "🔍 Diagnóstico común:"
    echo "   - Verifica que Java 17+ esté instalado"
    echo "   - Verifica que Android SDK esté instalado"
    echo "   - Verifica permisos de archivos"
    echo "   - Reinicia la terminal para cargar nuevas variables de entorno"
fi

echo ""
echo "=========================================="
echo "📋 INFORMACIÓN DEL PROYECTO"
echo "=========================================="
echo "📦 Paquete: $PACKAGE_NAME"
echo "📁 Proyecto: $PROJECT_NAME"
echo "🎯 Versión: 1.0 (code 1)"
echo "📱 Target SDK: 34"
echo "🔧 Mínimo SDK: 21"
echo "⚙️  AAPT2: DISABLED (legacy resources)"
echo ""
echo "✅ Proceso completado"