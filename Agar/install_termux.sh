#!/bin/bash

# 🎮 Enhanced Agar Game - Script de Instalación Automática para Termux
# Versión: 1.0
# Autor: MiniMax Agent

set -e  # Salir en caso de error

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Función para mostrar mensajes
show_message() {
    local color=$1
    local message=$2
    echo -e "${color}[$(date '+%H:%M:%S')] ${message}${NC}"
}

# Banner de inicio
show_banner() {
    echo -e "${BLUE}"
    cat << "EOF"
╔══════════════════════════════════════════════════════════════╗
║                    🎮 Enhanced Agar Game                     ║
║                  Script de Instalación Termux               ║
╚══════════════════════════════════════════════════════════════╝
EOF
    echo -e "${NC}"
}

# Verificar si estamos en Termux
check_termux() {
    show_message $YELLOW "🔍 Verificando entorno Termux..."
    if [ ! -d "$HOME/.termux" ]; then
        show_message $RED "❌ Error: Este script debe ejecutarse en Termux"
        show_message $YELLOW "📱 Instala Termux desde F-Droid: https://f-droid.org/packages/com.termux/"
        exit 1
    fi
    show_message $GREEN "✅ Entorno Termux verificado"
}

# Actualizar sistema
update_system() {
    show_message $YELLOW "📦 Actualizando sistema Termux..."
    pkg update && pkg upgrade -y
    show_message $GREEN "✅ Sistema actualizado"
}

# Instalar dependencias
install_dependencies() {
    show_message $YELLOW "🔧 Instalando dependencias esenciales..."
    
    # Lista de paquetes necesarios
    local packages=(
        "git"
        "python"
        "curl"
        "wget"
        "openjdk-17"
        "android-tools"
        "unzip"
        "build-essential"
    )
    
    for package in "${packages[@]}"; do
        show_message $BLUE "📦 Instalando $package..."
        pkg install -y "$package" || {
            show_message $RED "❌ Error instalando $package"
            exit 1
        }
    done
    
    show_message $GREEN "✅ Dependencias instaladas"
}

# Configurar Android SDK
setup_android_sdk() {
    show_message $YELLOW "📱 Configurando Android SDK..."
    
    local android_home="$HOME/android-sdk"
    mkdir -p "$android_home"
    
    # Configurar variables de entorno
    if ! grep -q "ANDROID_HOME" ~/.bashrc; then
        cat >> ~/.bashrc << EOF

# Android SDK Configuration
export ANDROID_HOME=\$HOME/android-sdk
export PATH=\$PATH:\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools
export PATH=\$PATH:\$ANDROID_HOME/build-tools/34.0.0
EOF
    fi
    
    export ANDROID_HOME="$android_home"
    export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/build-tools/34.0.0"
    
    # Descargar Command Line Tools
    cd "$android_home"
    show_message $BLUE "⬇️ Descargando Android Command Line Tools..."
    
    if [ ! -f "cmdline-tools.zip" ]; then
        wget -q --show-progress https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip -O cmdline-tools.zip
    fi
    
    # Extraer y configurar
    if [ ! -d "cmdline-tools/latest" ]; then
        unzip -q cmdline-tools.zip
        mkdir -p cmdline-tools/latest
        mv cmdline-tools/* cmdline-tools/latest/ 2>/dev/null || true
        rmdir cmdline-tools 2>/dev/null || true
    fi
    
    # Aceptar licencias e instalar herramientas
    show_message $BLUE "📦 Instalando Android SDK tools..."
    yes | sdkmanager --sdk_root="$android_home" --licenses || true
    sdkmanager --sdk_root="$android_home" "platform-tools" "build-tools;34.0.0" "platforms;android-34"
    
    show_message $GREEN "✅ Android SDK configurado"
}

# Optimizar Gradle
optimize_gradle() {
    show_message $YELLOW "⚡ Optimizando configuración de Gradle..."
    
    if [ ! -f "gradle.properties" ]; then
        cat > gradle.properties << EOF
# Gradle optimizations for Termux
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=512m
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.daemon=true
org.gradle.configureondemand=true
android.useAndroidX=true
android.enableJetifier=true
android.buildCacheDir=.gradle-build-cache
EOF
    fi
    
    show_message $GREEN "✅ Gradle optimizado"
}

# Compilar el proyecto
compile_project() {
    show_message $YELLOW "🔨 Compilando Enhanced Agar Game..."
    
    # Dar permisos a gradlew
    if [ -f "./gradlew" ]; then
        chmod +x ./gradlew
    else
        show_message $RED "❌ Error: gradlew no encontrado. ¿Estás en el directorio correcto?"
        exit 1
    fi
    
    # Limpiar build anterior
    show_message $BLUE "🧹 Limpiando build anterior..."
    ./gradlew clean
    
    # Compilar APK
    show_message $BLUE "🏗️ Compilando APK..."
    ./gradlew assembleDebug
    
    if [ $? -eq 0 ]; then
        show_message $GREEN "✅ Compilación exitosa!"
        
        # Verificar si el APK se generó
        local apk_path="app/build/outputs/apk/debug/app-debug.apk"
        if [ -f "$apk_path" ]; then
            local size=$(du -h "$apk_path" | cut -f1)
            show_message $GREEN "📱 APK generado: $apk_path ($size)"
        else
            show_message $YELLOW "⚠️ APK no encontrado en la ruta esperada"
        fi
    else
        show_message $RED "❌ Error en la compilación"
        show_message $YELLOW "🔍 Ejecuta './gradlew assembleDebug --stacktrace --info' para más detalles"
        exit 1
    fi
}

# Verificar requisitos del sistema
check_system_requirements() {
    show_message $YELLOW "🔍 Verificando requisitos del sistema..."
    
    # Verificar RAM
    local total_ram=$(free -m | awk 'NR==2{printf "%.0f", $2}')
    if [ "$total_ram" -lt 1500 ]; then
        show_message $YELLOW "⚠️ Advertencia: RAM detectada: ${total_ram}MB (recomendado: 2GB+)"
    else
        show_message $GREEN "✅ RAM suficiente: ${total_ram}MB"
    fi
    
    # Verificar espacio en disco
    local available_space=$(df "$HOME" | awk 'NR==2 {print $4}')
    if [ "$available_space" -lt 3000000 ]; then  # 3GB en KB
        show_message $YELLOW "⚠️ Advertencia: Espacio limitado disponible"
    else
        show_message $GREEN "✅ Espacio suficiente disponible"
    fi
    
    # Verificar versión de Android
    if command -v getprop &> /dev/null; then
        local android_version=$(getprop ro.build.version.release 2>/dev/null || echo "Unknown")
        show_message $BLUE "ℹ️ Versión Android: $android_version"
    fi
}

# Función principal
main() {
    show_banner
    
    # Verificar si se ejecuta como script
    if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
        show_message $GREEN "🚀 Iniciando instalación de Enhanced Agar Game"
    fi
    
    # Verificaciones iniciales
    check_termux
    check_system_requirements
    
    # Instalación paso a paso
    update_system
    install_dependencies
    setup_android_sdk
    optimize_gradle
    
    # Compilar proyecto (solo si estamos en el directorio correcto)
    if [ -f "build.gradle" ] && [ -d "src" ]; then
        compile_project
    else
        show_message $YELLOW "📁 Directorio del proyecto no detectado"
        show_message $BLUE "💡 Asegúrate de ejecutar este script desde el directorio EnhancedAgarGame/"
        show_message $BLUE "💡 Comando: cd EnhancedAgarGame && bash install_termux.sh"
    fi
    
    # Mensaje final
    show_message $GREEN "🎉 ¡Instalación completada!"
    show_message $BLUE "📖 Lee INSTALACION_TERMUX.md para más información"
    show_message $BLUE "🎮 ¡Disfruta jugando Enhanced Agar Game!"
}

# Ejecutar función principal
main "$@"