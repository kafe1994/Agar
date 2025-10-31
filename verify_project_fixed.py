#!/usr/bin/env python3
"""
Script de verificación para Enhanced Agar Game Android
Verifica la estructura correcta del proyecto para Termux
"""

import os
import re
import json
import time
from pathlib import Path

def print_header(title):
    print(f"\n{'='*60}")
    print(f"📱 {title}")
    print('='*60)

def find_java_files(root_dir):
    """Encuentra todos los archivos Java"""
    java_files = []
    for root, dirs, files in os.walk(root_dir):
        for file in files:
            if file.endswith('.java'):
                java_files.append(os.path.join(root, file))
    return java_files

def analyze_android_manifest(manifest_path):
    """Analiza AndroidManifest.xml"""
    print(f"📱 Analizando AndroidManifest.xml...")
    
    if not os.path.exists(manifest_path):
        print("❌ ERROR: AndroidManifest.xml no encontrado")
        return None
    
    with open(manifest_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Extraer package
    package_match = re.search(r'package="([^"]+)"', content)
    package = package_match.group(1) if package_match else "No encontrado"
    
    # Extraer minSdk y targetSdk
    min_sdk_match = re.search(r'minSdk="(\d+)"', content)
    target_sdk_match = re.search(r'targetSdk="(\d+)"', content)
    
    min_sdk = min_sdk_match.group(1) if min_sdk_match else "No especificado"
    target_sdk = target_sdk_match.group(1) if target_sdk_match else "No especificado"
    
    # Encontrar activities
    activities = re.findall(r'<activity[^>]*android:name="([^"]+)"', content)
    
    # Encontrar permissions
    permissions = re.findall(r'<uses-permission[^>]*android:name="([^"]+)"', content)
    
    print(f"✅ Package: {package}")
    print(f"✅ minSdk: {min_sdk}")
    print(f"✅ targetSdk: {target_sdk}")
    print(f"✅ Activities encontradas: {len(activities)}")
    print(f"✅ Permissions encontrados: {len(permissions)}")
    
    return {
        'package': package,
        'minSdk': min_sdk,
        'targetSdk': target_sdk,
        'activities': activities,
        'permissions': permissions,
        'content_length': len(content)
    }

def verify_gradle_structure(project_root):
    """Verifica la estructura de archivos Gradle"""
    print(f"🔧 Verificando configuración Gradle...")
    
    gradle_files = {
        'build.gradle': os.path.join(project_root, 'build.gradle'),
        'settings.gradle': os.path.join(project_root, 'settings.gradle'),
        'gradle.properties': os.path.join(project_root, 'gradle.properties'),
        'app/build.gradle': os.path.join(project_root, 'app', 'build.gradle')
    }
    
    missing_files = []
    existing_files = []
    
    for name, path in gradle_files.items():
        if os.path.exists(path):
            existing_files.append(name)
            print(f"✅ {name} encontrado")
        else:
            missing_files.append(name)
            print(f"❌ {name} faltante")
    
    return {
        'existing': existing_files,
        'missing': missing_files,
        'complete': len(missing_files) == 0
    }

def analyze_imports(java_files):
    """Analiza todos los imports en archivos Java"""
    print(f"📦 Analizando imports...")
    
    imports = set()
    import_stats = {}
    
    for java_file in java_files:
        try:
            with open(java_file, 'r', encoding='utf-8') as f:
                content = f.read()
                file_imports = re.findall(r'import\s+([^;]+);', content)
                imports.update(file_imports)
                
                for imp in file_imports:
                    if imp not in import_stats:
                        import_stats[imp] = 0
                    import_stats[imp] += 1
                    
        except Exception as e:
            print(f"⚠️  Error leyendo {java_file}: {e}")
    
    print(f"✅ Total de imports únicos: {len(imports)}")
    return {
        'total_imports': len(imports),
        'import_details': import_stats
    }

def check_android_compatibility(imports):
    """Verifica compatibilidad con Android"""
    print(f"🔍 Verificando compatibilidad con Android...")
    
    android_imports = [imp for imp in imports if imp.startswith('android.') or imp.startswith('androidx.')]
    java_imports = [imp for imp in imports if imp.startswith('java.')]
    external_imports = [imp for imp in imports if not imp.startswith(('android.', 'androidx.', 'java.'))]
    
    print(f"✅ Imports Android: {len(android_imports)}")
    print(f"✅ Imports Java estándar: {len(java_imports)}")
    print(f"✅ Imports externos: {len(external_imports)}")
    
    # Verificar imports problemáticos
    problematic = []
    
    # Verificar imports de Java Swing/AWT que no existen en Android
    problematic_patterns = [
        'javax.swing',
        'java.awt',
    ]
    
    for pattern in problematic_patterns:
        for imp in external_imports:
            if pattern in imp:
                problematic.append(f"⚠️  {imp} (puede causar problemas en Android)")
    
    if problematic:
        print(f"⚠️  Imports problemáticos encontrados:")
        for p in problematic:
            print(f"    {p}")
    else:
        print(f"✅ No se encontraron imports problemáticos")
    
    return {
        'android_imports': len(android_imports),
        'java_imports': len(java_imports),
        'external_imports': len(external_imports),
        'problematic': problematic
    }

def calculate_file_metrics(java_files):
    """Calcula métricas de los archivos"""
    print(f"📊 Calculando métricas de archivos...")
    
    total_lines = 0
    total_files = len(java_files)
    class_count = 0
    
    class_patterns = [
        r'class\s+\w+',
        r'interface\s+\w+',
        r'enum\s+\w+'
    ]
    
    for java_file in java_files:
        try:
            with open(java_file, 'r', encoding='utf-8') as f:
                content = f.read()
                total_lines += len(content.splitlines())
                
                for pattern in class_patterns:
                    class_count += len(re.findall(pattern, content))
                    
        except Exception as e:
            print(f"⚠️  Error leyendo {java_file}: {e}")
    
    avg_lines = total_lines / total_files if total_files > 0 else 0
    complexity_score = total_lines + (class_count * 100)  # Estimación simple
    
    print(f"✅ Total archivos: {total_files}")
    print(f"✅ Total líneas: {total_lines}")
    print(f"✅ Total clases/interfaces: {class_count}")
    print(f"✅ Promedio líneas por archivo: {avg_lines:.1f}")
    
    return {
        'total_files': total_files,
        'total_lines': total_lines,
        'total_classes': class_count,
        'avg_lines_per_file': avg_lines,
        'complexity_score': complexity_score
    }

def main():
    """Función principal"""
    start_time = time.time()
    
    print_header("VERIFICACIÓN DE PROYECTO ENHANCED AGAR GAME")
    
    project_root = "/workspace/EnhancedAgarGame"
    
    if not os.path.exists(project_root):
        print(f"❌ ERROR: Directorio del proyecto no encontrado: {project_root}")
        return
    
    print(f"📁 Directorio del proyecto: {project_root}")
    
    # 1. Verificar estructura de directorios
    print_header("VERIFICANDO ESTRUCTURA DE DIRECTORIOS")
    
    essential_dirs = [
        'app/src/main/java',
        'app/src/main/res',
        'app/build.gradle'
    ]
    
    dirs_status = {}
    for dir_path in essential_dirs:
        full_path = os.path.join(project_root, dir_path)
        exists = os.path.exists(full_path)
        dirs_status[dir_path] = exists
        status = "✅" if exists else "❌"
        print(f"{status} {dir_path}")
    
    # 2. Verificar archivos Gradle
    print_header("VERIFICANDO CONFIGURACIÓN GRADLE")
    gradle_status = verify_gradle_structure(project_root)
    
    # 3. Encontrar archivos Java
    print_header("ENCONTRANDO ARCHIVOS JAVA")
    java_files = find_java_files(os.path.join(project_root, 'app', 'src', 'main', 'java'))
    print(f"✅ Archivos Java encontrados: {len(java_files)}")
    
    if len(java_files) == 0:
        print("❌ ERROR: No se encontraron archivos Java")
        return
    
    # 4. Analizar AndroidManifest.xml
    print_header("ANALIZANDO ANDROIDMANIFEST.XML")
    manifest_path = os.path.join(project_root, 'app', 'src', 'main', 'AndroidManifest.xml')
    manifest_info = analyze_android_manifest(manifest_path)
    
    # 5. Analizar imports
    print_header("ANALIZANDO IMPORTS")
    imports_analysis = analyze_imports(java_files)
    
    # 6. Verificar compatibilidad Android
    print_header("VERIFICANDO COMPATIBILIDAD ANDROID")
    android_compat = check_android_compatibility(imports_analysis['import_details'].keys())
    
    # 7. Calcular métricas
    print_header("CALCULANDO MÉTRICAS")
    file_metrics = calculate_file_metrics(java_files)
    
    # 8. Evaluación final
    print_header("EVALUACIÓN FINAL")
    
    critical_issues = []
    warnings = []
    
    # Verificar problemas críticos
    if not gradle_status['complete']:
        critical_issues.append("Archivos de configuración Gradle faltantes")
    
    if manifest_info is None:
        critical_issues.append("AndroidManifest.xml no encontrado o inválido")
    
    if len(java_files) == 0:
        critical_issues.append("No se encontraron archivos Java")
    
    # Verificar warnings
    if android_compat['problematic']:
        warnings.extend(android_compat['problematic'])
    
    # Reporte final
    verification_time = time.time() - start_time
    
    report = {
        "timestamp": time.strftime("%Y-%m-%dT%H:%M:%S"),
        "verification_time_seconds": round(verification_time, 2),
        "project_root": project_root,
        "summary": {
            "total_java_files": len(java_files),
            "total_imports": imports_analysis['total_imports'],
            "total_issues": len(critical_issues) + len(warnings),
            "critical_issues": len(critical_issues),
            "warnings": len(warnings)
        },
        "directory_structure": dirs_status,
        "gradle_configuration": gradle_status,
        "android_manifest": manifest_info,
        "imports_analysis": {
            "total_unique_imports": imports_analysis['total_imports'],
            "android_compatibility": android_compat
        },
        "file_metrics": file_metrics,
        "critical_issues": critical_issues,
        "warnings": warnings
    }
    
    # Mostrar resumen
    print(f"\n{'='*60}")
    print(f"📊 RESUMEN FINAL:")
    print(f"{'='*60}")
    print(f"✅ Archivos Java: {len(java_files)}")
    print(f"✅ Importes únicos: {imports_analysis['total_imports']}")
    print(f"✅ Configuración Gradle: {'COMPLETA' if gradle_status['complete'] else 'INCOMPLETA'}")
    print(f"✅ AndroidManifest.xml: {'VÁLIDO' if manifest_info else 'INVÁLIDO'}")
    print(f"❌ Errores críticos: {len(critical_issues)}")
    print(f"⚠️  Warnings: {len(warnings)}")
    
    if len(critical_issues) == 0:
        print(f"\n🎮 ESTADO: PROYECTO LISTO PARA COMPILACIÓN EN TERMUX")
        print(f"⏱️  Tiempo de verificación: {verification_time:.1f} segundos")
        
        print(f"\n📋 PRÓXIMOS PASOS:")
        print(f"1. cd {project_root}")
        print(f"2. ./gradlew clean")
        print(f"3. ./gradlew assembleDebug")
        
    else:
        print(f"\n❌ ESTADO: PROYECTO TIENE PROBLEMAS CRÍTICOS")
        print(f"\n🚨 PROBLEMAS CRÍTICOS:")
        for issue in critical_issues:
            print(f"   • {issue}")
        
        if warnings:
            print(f"\n⚠️  WARNINGS:")
            for warning in warnings:
                print(f"   • {warning}")
    
    # Guardar reporte
    report_path = os.path.join(project_root, 'enhanced_verification_report.json')
    with open(report_path, 'w', encoding='utf-8') as f:
        json.dump(report, f, indent=2, ensure_ascii=False)
    
    print(f"\n📄 Reporte detallado guardado en: {report_path}")
    
    return len(critical_issues) == 0

if __name__ == "__main__":
    success = main()
    exit(0 if success else 1)