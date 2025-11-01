# MainMenuActivity - Menú Principal Moderno

## Descripción
`MainMenuActivity.java` es una implementación moderna y atractiva del menú principal para el juego Enhanced Agar. Ofrece una experiencia de usuario mejorada con animaciones fluidas, selección de modos de juego, configuración de roles y sistema de guardado local.

## Características Principales

### 🎮 Selección de Modo de Juego
- **Clásico**: Modo tradicional de Agar.io
- **Equipos**: 4v4 con roles específicos (Scout, Tank, Support, Assassin)
- **Supervivencia**: Sobrevive oleadas de enemigos
- **Arena**: Combate directo sin crecimiento
- **Rey**: Ataca al rey para ganar puntos

### ⚔️ Sistema de Roles
- **Tanque**: Alto HP y defensa, velocidad reducida
- **Asesino**: Alta velocidad y daño crítico, HP reducido
- **Mago**: Habilidades mágicas y daño a distancia
- **Soporte**: Habilidades de curación y soporte

### 🎨 Animaciones y Efectos Visuales
- **Animaciones de entrada**: Entrada progresiva de elementos
- **Efectos de pulsación**: Feedback visual al seleccionar opciones
- **Gradiente animado**: Fondo que cambia de color constantemente
- **Partículas flotantes**: Efectos de partículas sutiles
- **Escala del título**: Animación de flotación del título principal

### 👤 Configuración de Usuario
- **Nombre de usuario personalizable**: Hasta 20 caracteres
- **Generación aleatoria**: Botón para generar nombres aleatorios
- **Guardado automático**: Persistencia de configuración

### 💾 Sistema de Guardado Local
- **SharedPreferences**: Almacenamiento eficiente de configuración
- **Persistencia automática**: Guarda automáticamente cada cambio
- **Carga de configuración**: Restaura la configuración al iniciar

## Estructura de Archivos

### Archivos Creados
```
EnhancedAgarGame/
├── src/main/java/com/gaming/enhancedagar/ui/
│   └── MainMenuActivity.java           # Actividad principal del menú
├── src/main/res/layout/
│   └── activity_main_menu.xml          # Layout del menú principal
└── src/main/res/drawable/
    ├── card_background.xml              # Fondo de tarjetas
    ├── button_play.xml                  # Botón principal JUGAR
    ├── button_secondary.xml             # Botones secundarios
    ├── button_minor.xml                 # Botones menores
    ├── button_danger.xml                # Botón de salida
    ├── button_circle.xml                # Botones circulares
    ├── gradient_background.xml          # Fondo con gradiente
    └── edit_text_background.xml         # Fondo de campos de texto
└── src/main/res/animator/
    └── button_press_animator.xml        # Animación de botones
└── AndroidManifest.xml                  # Registro de la actividad
```

## Funcionalidades Detalladas

### 1. Gestión de Estados
- **Ciclo de vida**: Manejo completo de onCreate, onResume, onPause, onDestroy
- **Animaciones**: Inicio y pausa de animaciones según el estado
- **Recursos**: Limpieza automática de recursos

### 2. Sistema de Animaciones
```java
// Animación de entrada del menú
animateMenuEntry();

// Animación de pulsación de botones
animateButtonPress(button, action);

// Efecto de cambio de selección
playButtonEffect();
```

### 3. Configuración de Juego
```java
// Selección de modo
selectedMode = gameModes.get(index);

// Selección de rol
selectedRole = availableRoles.get(index);

// Guardado automático
saveUserSettings();
```

### 4. Efectos de Fondo
- **Gradiente animado**: Cambia de color cada 8 segundos
- **Partículas**: Sistema de partículas flotantes
- **Título flotante**: Escala alternante del título principal

## Integración con GameModeManager

El menú se integra perfectamente con el sistema de modos de juego existente:

```java
GameModeManager.GameMode selectedMode = GameModeManager.GameMode.CLASSIC;
// Se pasa al GameEngine cuando inicia el juego
```

## Integración con RoleSystem

Los roles se muestran con información detallada:

```java
RoleSystem.RoleType selectedRole = RoleSystem.RoleType.TANK;
// Información visual del rol seleccionado
```

## Personalización

### Modificar Colores
Editar los archivos XML en `drawable/` para cambiar la paleta de colores.

### Añadir Nuevos Modos
1. Agregar a `GameModeManager.GameMode`
2. Actualizar `getModeInfo()` en MainMenuActivity
3. Agregar íconos y descripciones

### Añadir Nuevos Roles
1. Agregar a `RoleSystem.RoleType`
2. Actualizar `getRoleInfo()` en MainMenuActivity
3. Configurar estadísticas en RoleSystem

## Rendimiento

### Optimizaciones Implementadas
- **ExecutorService**: Para animaciones en background
- **Handler**: Para actualizaciones en UI thread
- **SharedPreferences**: Almacenamiento eficiente
- **Animaciones pausables**: Se pausan en onPause()

### Memoria
- **Limpieza automática**: Recursos liberados en onDestroy()
- **Gestión de threads**: ExecutorService se cierra correctamente
- **Referencias nulas**: Verificación de referencias antes de usar

## Compatibilidad

### Dispositivos Soportados
- ✅ Android API 21+ (Android 5.0)
- ✅ Teléfonos y tablets
- ✅ Orientación vertical y horizontal
- ✅ Termux (Android)

### Características Hardware
- ✅ Touch screen
- ✅ Multitouch (opcional)
- ✅ Acelerómetro (opcional)
- ✅ Giroscopio (opcional)

## Uso

### Iniciar el Juego
1. Usuario establece nombre de usuario
2. Selecciona modo de juego
3. Selecciona rol de personaje
4. Presiona "JUGAR AHORA"
5. Se inicia MainActivity con configuración

### Navegación
- **Botones principales**: Jugar, Configuración, Perfil, Puntuaciones, Créditos
- **Selección**: Flechas izquierda/derecha para cambiar modo/rol
- **Configuración**: Nombre de usuario personalizable
- **Salida**: Botón de salir con confirmación

## Debugging

### Logs Disponibles
```java
android.util.Log.i(TAG, "MainMenuActivity initialized successfully");
android.util.Log.d(TAG, "Mode changed to: " + selectedMode.name());
android.util.Log.d(TAG, "User settings saved");
```

### Debug Features
- **Información del dispositivo**: Log automático en MainActivity
- **Estado de animaciones**: Tracking de isAnimating
- **Configuración cargada**: Log de settings al iniciar

## Futuras Mejoras

### Funcionalidades Planeadas
- **SettingsActivity**: Pantalla de configuración completa
- **ProfileActivity**: Perfil de jugador con estadísticas
- **ScoresActivity**: Leaderboard y puntuaciones
- **CreditsActivity**: Pantalla de créditos
- **Audio**: Sonidos de botones y música de fondo
- **Temas**: Múltiples temas visuales

### Optimizaciones
- **Base de datos**: Almacenamiento local más robusto
- **Sincronización**: Cloud save para múltiples dispositivos
- **Personalización avanzada**: Más opciones de customización
- **Accesibilidad**: Soporte completo para TalkBack

## Changelog

### v1.0 (Actual)
- ✅ Implementación completa del menú principal
- ✅ Selección de modos de juego
- ✅ Selección de roles
- ✅ Animaciones y efectos visuales
- ✅ Sistema de guardado local
- ✅ Configuración de usuario
- ✅ Integración con GameEngine
- ✅ Compatibilidad con Termux
- ✅ UI moderna y responsiva