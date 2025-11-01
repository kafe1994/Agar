#!/bin/bash

echo "=== VERIFICACIÓN DE ERRORES DE COMPILACIÓN ==="
echo ""

# Buscar imports AWT problemáticos
echo "1. Buscando imports AWT problemáticos..."
awt_imports=$(find . -name "*.java" -exec grep -l "java.awt\|java.swing" {} \;)
if [ -n "$awt_imports" ]; then
    echo "❌ Encontrados imports AWT:"
    echo "$awt_imports"
else
    echo "✅ No se encontraron imports AWT"
fi

# Verificar que las clases utils existan
echo ""
echo "2. Verificando clases del paquete utils..."
utils_classes=(
    "utils/Vector2D.java"
    "utils/StatisticsManager.java" 
    "utils/AchievementManager.java"
    "utils/PersonalRecordsManager.java"
    "utils/VersionInfo.java"
)

for class in "${utils_classes[@]}"; do
    if [ -f "app/src/main/java/com/gaming/enhancedagar/$class" ]; then
        echo "✅ $class existe"
    else
        echo "❌ $class NO existe"
    fi
done

# Verificar métodos duplicados
echo ""
echo "3. Verificando métodos duplicados en Entity.java..."
entity_duplicates=$(grep -n "public.*getDistanceTo" app/src/main/java/com/gaming/enhancedagar/entities/Entity.java | wc -l)
if [ "$entity_duplicates" -eq 1 ]; then
    echo "✅ Entity.java: Sin métodos getDistanceTo duplicados"
else
    echo "❌ Entity.java: $entity_duplicates métodos getDistanceTo encontrados"
fi

# Verificar VersionInfo.java
echo ""
echo "4. Verificando VersionInfo.java..."
if grep -q "private boolean checkDebugBuild()" app/src/main/java/com/gaming/enhancedagar/utils/VersionInfo.java; then
    echo "✅ VersionInfo.java: Método privado renombrado correctamente"
else
    echo "❌ VersionInfo.java: Método privado no renombrado"
fi

# Verificar MovementSystem.java
echo ""
echo "5. Verificando MovementSystem.java..."
if grep -q "import java.util.PriorityQueue;" app/src/main/java/com/gaming/enhancedagar/game/MovementSystem.java; then
    echo "✅ MovementSystem.java: Import correcto de PriorityQueue"
else
    echo "❌ MovementSystem.java: Import incorrecto de PriorityQueue"
fi

# Verificar CoordinateSystem.java
echo ""
echo "6. Verificando CoordinateSystem.java..."
if grep -q "import android.graphics.PointF" app/src/main/java/com/gaming/enhancedagar/game/CoordinateSystem.java; then
    echo "✅ CoordinateSystem.java: Imports Android correctos"
else
    echo "❌ CoordinateSystem.java: Imports Android incorrectos"
fi

echo ""
echo "=== VERIFICACIÓN COMPLETADA ==="