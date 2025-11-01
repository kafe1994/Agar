# 🎮 Enhanced Agar Game - Instalación en Termux

## 📋 Descripción
Este es un videojuego innovador inspirado en Agar.io con 4 roles únicos, mecánicas tipo piedra-papel-tijera, múltiples modos de juego y gráficos modernos.

## ✨ Características Únicas
- **4 Roles Especializados**: Tank, Assassin, Mage, Support con ventajas/desventajas específicas
- **5 Modos de Juego**: Classic, Teams, Survival, Arena, King
- **Habilidades Especiales**: Cada rol tiene habilidades únicas
- **Sistema de División Inteligente**: Fragmentos con roles reasignados
- **Gráficos Modernos**: Sistema de partículas, efectos visuales y renderizado optimizado
- **Compatibilidad Total**: Optimizado para Termux y dispositivos móviles

## 🛠️ Requisitos Previos

### En Android:
1. **Termux** instalado desde F-Droid o Play Store
2. **Android SDK Tools** (Build Tools 34.0.0)
3. **Mínimo 2GB de RAM** para compilación
4. **3GB de espacio libre**

### En Termux:
```bash
# Actualizar paquetes
pkg update && pkg upgrade -y

# Instalar dependencias esenciales
pkg install -y git python curl openjdk-17

# Instalar Android SDK
pkg install -y android-tools

# Configurar ANDROID_HOME
mkdir -p $HOME/android-sdk
echo 'export ANDROID_HOME=$HOME/android-sdk' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools' >> ~/.bashrc
source ~/.bashrc

# Descargar Android SDK Command Line Tools
cd $ANDROID_HOME
curl -L -o cmdline-tools.zip https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip
unzip cmdline-tools.zip && mkdir -p cmdline-tools/latest && mv cmdline-tools/* cmdline-tools/latest/

# Instalar platform-tools y build-tools
sdkmanager "platform-tools" "build-tools;34.0.0" "platforms;android-34"
```

## 📦 Instalación del Proyecto

### 1. Descargar el Código
```bash
# Navegar al directorio de trabajo
cd $HOME

# Si tienes el código en un repositorio git:
git clone <URL_DEL_REPOSITORIO> EnhancedAgarGame

# O extraer desde archivo ZIP:
unzip EnhancedAgarGame.zip
cd EnhancedAgarGame
```

### 2. Configurar Gradle
```bash
# Dar permisos de ejecución a gradlew
chmod +x ./gradlew

# Verificar que gradlew funciona
./gradlew --version
```

### 3. Compilar el Proyecto
```bash
# Limpiar build anterior
./gradlew clean

# Compilar APK de debug (más rápido)
./gradlew assembleDebug

# O compilar APK de release (optimizado)
./gradlew assembleRelease
```

## 🎯 Uso del Juego

### Ejecutar en Emulador/Device
```bash
# Verificar que el dispositivo esté conectado
adb devices

# Instalar APK
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Compilar y Ejecutar Directamente
```bash
# Instalar en el dispositivo conectado
./gradlew installDebug
```

## 🎮 Mecánicas del Juego

### Roles y Ventajas:
- **TANK** > **MAGE** (defensa vs magia)
- **ASSASSIN** > **TANK** (velocidad vs resistencia)
- **MAGE** > **ASSASSIN** (distancia vs sigilo)
- **SUPPORT** > **TODOS** (soporte universal)

### Modos de Juego:
1. **Classic**: Modo tradicional de Agar.io
2. **Teams**: Equipos 4v4 con roles específicos
3. **Survival**: Supervivencia contra oleadas de enemigos
4. **Arena**: Combate directo sin crecimiento
5. **King**: Un jugador es el rey, todos deben atacarlo

### Habilidades Especiales:
- **Tank**: Shield (Escudo), Fortification (Fortificación)
- **Assassin**: Stealth (Sigilo), Shadow Strike (Golpe Sombra)
- **Mage**: Nova (Nova Arcana), Spell Boost (Impulso Mágico)
- **Support**: Heal (Curación), Regeneration Aura (Aura Regeneradora)

## 🐛 Solución de Problemas

### Error de Compilación:
```bash
# Limpiar y reconstruir
./gradlew clean
./gradlew assembleDebug --stacktrace --info

# Verificar versiones
./gradlew dependencies
```

### Error de Memoria:
```bash
# Aumentar memoria de Gradle
echo 'org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=512m' >> gradle.properties
```

### Error de SDK:
```bash
# Verificar ANDROID_HOME
echo $ANDROID_HOME

# Reinstalar SDK si es necesario
rm -rf $ANDROID_HOME
# Repetir instalación de SDK desde paso anterior
```

### Error de Permisos:
```bash
# Verificar permisos de archivos
ls -la ./gradlew
chmod +x ./gradlew

# Verificar permisos del directorio
chmod -R 755 .
```

## 📊 Optimización para Dispositivos

### Configuración Automática:
El juego detecta automáticamente el dispositivo y ajusta:
- **FPS objetivo**: 60 (alta gama), 30 (gama media/baja)
- **Calidad de partículas**: Automática según RAM disponible
- **Resolución**: Escalado automático según densidad de pantalla

### Configuración Manual:
```bash
# El juego incluye settings automático en runtime
# pero puedes optimizar manualmente editando:
# src/main/java/com/gaming/enhancedagar/utils/DeviceOptimizer.java
```

## 🔧 Desarrollo y Debugging

### Ejecutar en Modo Debug:
```bash
# Con logging detallado
./gradlew assembleDebug -Dandroid.enableLogging=true

# Con tests unitarios
./gradlew test

# Con tests de instrumentación
./gradlew connectedAndroidTest
```

### Archivos Importantes:
```
EnhancedAgarGame/
├── src/main/java/com/gaming/enhancedagar/
│   ├── MainActivity.java          # Actividad principal
│   ├── engine/                    # Motor de juego
│   ├── entities/                  # Entidades (Player, Food, EnemyBot)
│   ├── game/                      # Sistemas de juego
│   ├── ui/                        # Interfaz de usuario
│   └── utils/                     # Utilidades
├── src/main/res/                  # Recursos Android
├── build.gradle                   # Configuración de build
└── README.md                      # Documentación completa
```

## 📱 Resultado Final

Después de la instalación exitosa tendrás:
- **APK optimizado** (~15-25 MB)
- **Juego completamente funcional** con todas las mecánicas
- **Interfaz moderna** con menús y configuraciones
- **5 modos de juego** diferentes
- **Sistema de roles único** tipo piedra-papel-tijera

## 🤝 Contribución

Para contribuir al proyecto:
1. Fork del repositorio
2. Crear branch para nueva funcionalidad
3. Commit con mensajes descriptivos
4. Pull request con descripción detallada

## 📄 Licencia

Este proyecto está bajo licencia MIT. Ver archivo LICENSE para más detalles.

---

**¡Disfruta jugando Enhanced Agar Game! 🏆**