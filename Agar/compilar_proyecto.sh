#!/bin/bash

# 🎮 SCRIPT DE COMPILACIÓN OPTIMIZADO - Enhanced Agar Game
# Compilación limpia para Termux

echo "🎮 === ENHANCED AGAR GAME - COMPILACIÓN LIMPIA ==="
echo "📱 Configurado para Termux Android"
echo ""

# Verificar que estamos en el directorio correcto
if [ ! -f "build.gradle" ]; then
    echo "❌ Error: No estás en el directorio del proyecto"
    echo "   Ubícate en el directorio del proyecto EnhancedAgarGame"
    exit 1
fi

# Verificar permisos de ejecución
if [ ! -x "./gradlew" ]; then
    echo "🔧 Configurando permisos de Gradle Wrapper..."
    chmod +x ./gradlew
fi

echo "🧹 PASO 1: Limpiando proyecto..."
./gradlew clean --quiet

if [ $? -eq 0 ]; then
    echo "✅ Limpieza completada"
else
    echo "⚠️  Advertencia: La limpieza tuvo problemas, continuando..."
fi

echo ""
echo "🔧 PASO 2: Verificando configuración..."
./gradlew tasks --quiet > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "✅ Configuración de Gradle verificada"
else
    echo "❌ Error: Problemas con la configuración de Gradle"
    exit 1
fi

echo ""
echo "📦 PASO 3: Compilando APK de Debug..."
echo "⏳ Esto puede tomar varios minutos en la primera ejecución..."

./gradlew assembleDebug --quiet

if [ $? -eq 0 ]; then
    echo ""
    echo "🎉 ¡COMPILACIÓN EXITOSA!"
    echo "📍 APK generado en: app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    echo "📱 Para instalar en tu dispositivo:"
    echo "   adb install app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    echo "🔧 O copia el archivo APK a tu dispositivo Android"
else
    echo ""
    echo "❌ Error de compilación"
    echo "📋 Para más detalles, ejecuta: ./gradlew assembleDebug --info"
    exit 1
fi

echo ""
echo "🏁 === PROCESO COMPLETADO ==="