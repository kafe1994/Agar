#!/bin/bash

# Script de verificación de recursos para Enhanced Agar Game
echo "🔍 VERIFICANDO RECURSOS DEL PROYECTO..."
echo ""

# Verificar archivos drawable requeridos
echo "📱 Verificando archivos drawable..."
drawable_files=(
    "menu_background"
    "ic_pause"
    "ic_settings"
    "ic_center"
    "ic_split"
    "ic_boost"
    "ic_trophy_gold"
    "ic_trophy_silver"
    "achievement_background"
    "comparison_background"
    "button_primary"
    "gradient_background"
    "card_background"
    "edit_text_background"
    "button_play"
    "button_secondary"
    "button_minor"
    "button_danger"
    "button_circle"
)

missing_drawables=0
for drawable in "${drawable_files[@]}"; do
    if [ ! -f "/workspace/EnhancedAgarGame_Reconstruido/app/src/main/res/drawable/${drawable}.xml" ]; then
        echo "❌ Falta: drawable/${drawable}.xml"
        ((missing_drawables++))
    fi
done

if [ $missing_drawables -eq 0 ]; then
    echo "✅ Todos los drawables requeridos están presentes"
else
    echo "⚠️  Faltan $missing_drawables archivos drawable"
fi

# Verificar layouts
echo ""
echo "📄 Verificando layouts..."
layout_files=(
    "activity_main"
    "activity_main_menu"
    "activity_settings"
    "activity_game_over"
)

missing_layouts=0
for layout in "${layout_files[@]}"; do
    if [ ! -f "/workspace/EnhancedAgarGame_Reconstruido/app/src/main/res/layout/${layout}.xml" ]; then
        echo "❌ Falta: layout/${layout}.xml"
        ((missing_layouts++))
    fi
done

if [ $missing_layouts -eq 0 ]; then
    echo "✅ Todos los layouts requeridos están presentes"
else
    echo "⚠️  Faltan $missing_layouts archivos layout"
fi

# Verificar archivos de recursos
echo ""
echo "🎨 Verificando archivos de recursos..."
resource_files=(
    "colors.xml"
    "dimens.xml"
    "strings.xml"
    "styles.xml"
)

missing_resources=0
for resource in "${resource_files[@]}"; do
    if [ ! -f "/workspace/EnhancedAgarGame_Reconstruido/app/src/main/res/values/${resource}" ]; then
        echo "❌ Falta: values/${resource}"
        ((missing_resources++))
    fi
done

if [ $missing_resources -eq 0 ]; then
    echo "✅ Todos los archivos de recursos están presentes"
else
    echo "⚠️  Faltan $missing_resources archivos de recursos"
fi

# Verificar animaciones
echo ""
echo "🎬 Verificando animaciones..."
anim_files=(
    "fade_in"
    "fade_out"
    "slide_up"
    "slide_down"
)

missing_anims=0
for anim in "${anim_files[@]}"; do
    if [ ! -f "/workspace/EnhancedAgarGame_Reconstruido/app/src/main/res/anim/${anim}.xml" ]; then
        echo "❌ Falta: anim/${anim}.xml"
        ((missing_anims++))
    fi
done

if [ $missing_anims -eq 0 ]; then
    echo "✅ Todas las animaciones están presentes"
else
    echo "⚠️  Faltan $missing_anims archivos de animación"
fi

# Verificar sonidos
echo ""
echo "🔊 Verificando archivos de sonido..."
sound_files=(
    "shield_sound"
    "nova_sound"
    "heal_sound"
    "stealth_sound"
    "shadow_strike_sound"
)

missing_sounds=0
for sound in "${sound_files[@]}"; do
    if [ ! -f "/workspace/EnhancedAgarGame_Reconstruido/app/src/main/res/raw/${sound}.xml" ]; then
        echo "❌ Falta: raw/${sound}.xml"
        ((missing_sounds++))
    fi
done

if [ $missing_sounds -eq 0 ]; then
    echo "✅ Todos los archivos de sonido están presentes"
else
    echo "⚠️  Faltan $missing_sounds archivos de sonido"
fi

# Verificar archivos de configuración Gradle
echo ""
echo "⚙️ Verificando archivos de configuración..."
config_files=(
    "build.gradle"
    "app/build.gradle"
    "settings.gradle"
    "gradle.properties"
)

missing_configs=0
for config in "${config_files[@]}"; do
    if [ ! -f "/workspace/EnhancedAgarGame_Reconstruido/${config}" ]; then
        echo "❌ Falta: ${config}"
        ((missing_configs++))
    fi
done

if [ $missing_configs -eq 0 ]; then
    echo "✅ Todos los archivos de configuración están presentes"
else
    echo "⚠️  Faltan $missing_configs archivos de configuración"
fi

# Verificar AndroidManifest
echo ""
echo "📋 Verificando AndroidManifest.xml..."
if [ -f "/workspace/EnhancedAgarGame_Reconstruido/app/src/main/AndroidManifest.xml" ]; then
    echo "✅ AndroidManifest.xml está presente"
else
    echo "❌ Falta AndroidManifest.xml"
    ((missing_configs++))
fi

# Resumen final
echo ""
echo "📊 RESUMEN DE VERIFICACIÓN:"
echo "=================================="

total_missing=$((missing_drawables + missing_layouts + missing_resources + missing_anims + missing_sounds + missing_configs))

if [ $total_missing -eq 0 ]; then
    echo "🎉 ¡PERFECTO! Todos los archivos requeridos están presentes"
    echo "✅ El proyecto está listo para compilar"
else
    echo "⚠️  Total de archivos faltantes: $total_missing"
    echo "🔧 Se recomienda revisar y crear los archivos faltantes"
fi

echo ""
echo "📂 Estructura del proyecto:"
echo "EnhancedAgarGame_Reconstruido/"
find /workspace/EnhancedAgarGame_Reconstruido -type f | head -20 | sed 's|/workspace/EnhancedAgarGame_Reconstruido/|  |' | sort
echo "  ... y más archivos"

exit 0