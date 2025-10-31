#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script de Verificación de Proyecto Enhanced Agar Game
Verifica imports, dependencias, compatibilidad Android y errores potenciales
"""

import os
import re
import json
import ast
import sys
from pathlib import Path
from datetime import datetime
from typing import Dict, List, Set, Tuple, Any
import xml.etree.ElementTree as ET

class ProjectVerifier:
    def __init__(self, project_root: str):
        self.project_root = Path(project_root)
        self.java_files = []
        self.dependencies = set()
        self.issues = []
        self.reports = []
        
        # Android APIs estándar
        self.android_apis = {
            'android.app': ['Activity', 'Application', 'Service', 'BroadcastReceiver'],
            'android.os': ['Bundle', 'Handler', 'Looper', 'AsyncTask'],
            'android.view': ['View', 'SurfaceView', 'MotionEvent', 'WindowManager'],
            'android.graphics': ['Canvas', 'Paint', 'Bitmap', 'Matrix'],
            'android.widget': ['TextView', 'Button', 'RelativeLayout'],
            'android.content': ['Context', 'Intent', 'SharedPreferences'],
            'android.media': ['AudioManager', 'MediaPlayer', 'SoundPool'],
            'android.location': ['LocationManager', 'LocationListener'],
            'android.hardware': ['SensorManager', 'Sensor', 'Camera'],
            'android.util': ['Log', 'SparseArray', 'DisplayMetrics'],
            'androidx': ['AppCompatActivity', 'Fragment', 'RecyclerView'],
            'android.opengl': ['GLSurfaceView', 'GL10', 'GLRenderer'],
        }
        
        # Librerías comunes en Android
        self.common_libraries = {
            'junit': 'test',
            'androidx.test': 'test',
            'mockito': 'test',
            'robolectric': 'test',
            'kotlin': 'implementation',
            'gson': 'implementation',
            'glide': 'implementation',
            'retrofit': 'implementation',
            'okhttp': 'implementation',
            'firebase': 'implementation',
            'play-services': 'implementation',
        }

    def scan_java_files(self) -> List[Path]:
        """Escanea recursivamente todos los archivos .java"""
        java_files = []
        java_pattern = re.compile(r'\.java$', re.IGNORECASE)
        
        for root, dirs, files in os.walk(self.project_root):
            # Evitar directorios de build
            if 'build' in root or 'gradle' in root:
                continue
                
            for file in files:
                if java_pattern.search(file):
                    java_files.append(Path(root) / file)
        
        self.java_files = java_files
        return java_files

    def extract_imports(self, file_path: Path) -> List[str]:
        """Extrae todos los imports de un archivo Java"""
        imports = []
        try:
            with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
                content = f.read()
                
            # Buscar líneas de import
            import_pattern = re.compile(r'^\s*import\s+([\w\.]+);', re.MULTILINE)
            matches = import_pattern.findall(content)
            imports = matches
            
        except Exception as e:
            self.issues.append(f"Error leyendo {file_path}: {e}")
            
        return imports

    def parse_gradle_dependencies(self) -> Dict[str, str]:
        """Parsea las dependencias del build.gradle"""
        dependencies = {}
        
        gradle_files = list(self.project_root.glob("**/build.gradle"))
        
        for gradle_file in gradle_files:
            try:
                with open(gradle_file, 'r', encoding='utf-8') as f:
                    content = f.read()
                    
                # Buscar líneas de dependencias
                dep_patterns = [
                    r"implementation\s+['\"]([^'\"]+)['\"]",
                    r"compileOnly\s+['\"]([^'\"]+)['\"]",
                    r"testImplementation\s+['\"]([^'\"]+)['\"]",
                    r"androidTestImplementation\s+['\"]([^'\"]+)['\"]",
                    r"api\s+['\"]([^'\"]+)['\"]",
                ]
                
                for pattern in dep_patterns:
                    matches = re.findall(pattern, content)
                    for match in matches:
                        # Extraer group y name
                        parts = match.split(':')
                        if len(parts) >= 2:
                            group = parts[0]
                            name = parts[1]
                            dependencies[f"{group}.{name}"] = "gradle"
                            
            except Exception as e:
                self.issues.append(f"Error leyendo gradle {gradle_file}: {e}")
                
        return dependencies

    def verify_import_compatibility(self, imports: List[str], file_path: Path) -> List[str]:
        """Verifica la compatibilidad de los imports"""
        issues = []
        gradle_deps = self.parse_gradle_dependencies()
        
        for import_line in imports:
            # Verificar si es un import de Android estándar
            is_android_api = any(import_line.startswith(api) for api in self.android_apis.keys())
            
            if is_android_api:
                # Verificar APIs específicas de Android
                if import_line.startswith('android.'):
                    base_pkg = '.'.join(import_line.split('.')[:3])
                    if base_pkg not in self.android_apis:
                        continue  # API no estándar pero válida
                        
                elif import_line.startswith('androidx.'):
                    # AndroidX es estándar para apps modernas
                    continue
                    
            # Verificar imports locales (del mismo proyecto)
            if import_line.startswith('com.gaming.enhancedagar'):
                continue
                
            # Verificar librerías conocidas
            is_known_lib = any(import_line.startswith(lib) for lib in self.common_libraries.keys())
            
            if not is_known_lib and not is_android_api:
                # Verificar si está en dependencias de Gradle
                import_base = '.'.join(import_line.split('.')[:-1])
                found_in_gradle = False
                
                for dep_key in gradle_deps.keys():
                    if import_base.startswith(dep_key) or dep_key.startswith(import_base):
                        found_in_gradle = True
                        break
                        
                if not found_in_gradle:
                    issues.append(f"Import no verificado: {import_line} en {file_path.name}")
                    
        return issues

    def analyze_method_signatures(self, file_path: Path) -> Dict[str, Any]:
        """Analiza las firmas de métodos en un archivo Java"""
        analysis = {
            'classes': [],
            'methods': [],
            'issues': []
        }
        
        try:
            with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
                content = f.read()
                
            # Buscar definiciones de clase
            class_pattern = re.compile(r'class\s+(\w+)[^{]*{', re.MULTILINE)
            classes = class_pattern.findall(content)
            analysis['classes'] = classes
            
            # Buscar métodos
            method_pattern = re.compile(r'(public|private|protected)?\s*(static)?\s*(final)?\s*[\w<>[\]\s]+\s+(\w+)\s*\([^)]*\)\s*[^{]*', re.MULTILINE)
            methods = method_pattern.findall(content)
            analysis['methods'] = [{'modifiers': m[0:3], 'name': m[3]} for m in methods]
            
            # Verificaciones específicas
            for class_name in classes:
                if not any(char.isupper() for char in class_name):
                    analysis['issues'].append(f"Clase con nombre no PascalCase: {class_name}")
                    
        except Exception as e:
            analysis['issues'].append(f"Error analizando métodos en {file_path.name}: {e}")
            
        return analysis

    def check_android_manifest(self) -> Dict[str, Any]:
        """Verifica el AndroidManifest.xml"""
        manifest_issues = []
        manifest_data = {}
        
        manifest_path = self.project_root / "src/main/AndroidManifest.xml"
        if not manifest_path.exists():
            manifest_issues.append("AndroidManifest.xml no encontrado")
            return {'issues': manifest_issues, 'data': {}}
            
        try:
            tree = ET.parse(manifest_path)
            root = tree.getroot()
            
            # Verificar configuración básica
            manifest_data['package'] = root.get('package', '')
            manifest_data['minSdk'] = root.find(".//uses-sdk").get('android:minSdkVersion', '') if root.find(".//uses-sdk") is not None else ''
            manifest_data['targetSdk'] = root.find(".//uses-sdk").get('android:targetSdkVersion', '') if root.find(".//uses-sdk") is not None else ''
            
            # Verificar permisos
            permissions = root.findall(".//uses-permission")
            manifest_data['permissions'] = [p.get('android:name') for p in permissions]
            
            # Verificar actividades
            activities = root.findall(".//activity")
            manifest_data['activities'] = [a.find('intent-filter/action').get('android:name') if a.find('intent-filter/action') is not None else 'Sin intent-filter' for a in activities]
            
            # Validaciones
            if not manifest_data['package']:
                manifest_issues.append("Package no definido en manifest")
                
            if manifest_data['minSdk'] and int(manifest_data['minSdk']) < 21:
                manifest_issues.append(f"minSdk muy bajo: {manifest_data['minSdk']} (recomendado: 21+)")
                
            if not activities:
                manifest_issues.append("No se encontraron actividades en el manifest")
                
        except ET.ParseError as e:
            manifest_issues.append(f"Error parseando AndroidManifest.xml: {e}")
        except Exception as e:
            manifest_issues.append(f"Error verificando manifest: {e}")
            
        return {'issues': manifest_issues, 'data': manifest_data}

    def check_file_consistency(self) -> List[str]:
        """Verifica consistencia entre archivos y manifest"""
        issues = []
        
        # Obtener actividades del manifest
        manifest_info = self.check_android_manifest()
        manifest_activities = manifest_info['data'].get('activities', [])
        
        # Buscar clases Activity en el código
        activity_classes = set()
        activity_pattern = re.compile(r'extends\s+(AppCompatActivity|Activity|FragmentActivity)')
        
        for java_file in self.java_files:
            try:
                with open(java_file, 'r', encoding='utf-8', errors='ignore') as f:
                    content = f.read()
                    
                if activity_pattern.search(content):
                    # Extraer nombre de clase
                    class_match = re.search(r'class\s+(\w+)\s+extends', content)
                    if class_match:
                        activity_classes.add(class_match.group(1))
            except:
                continue
                
        # Verificar que las actividades estén registradas en el manifest
        for activity in activity_classes:
            activity_found = any(
                manifest_activity and activity in manifest_activity 
                for manifest_activity in manifest_activities
            )
            if not activity_found and manifest_activities:  # Solo si hay actividades en el manifest
                issues.append(f"Actividad {activity} no registrada en AndroidManifest.xml")
                
        return issues

    def generate_performance_report(self) -> Dict[str, Any]:
        """Genera reporte de rendimiento y optimizaciones"""
        performance_data = {
            'total_files': len(self.java_files),
            'total_lines': 0,
            'total_classes': 0,
            'complexity_score': 0,
            'recommendations': []
        }
        
        for java_file in self.java_files:
            try:
                with open(java_file, 'r', encoding='utf-8', errors='ignore') as f:
                    content = f.read()
                    lines = content.splitlines()
                    
                performance_data['total_lines'] += len(lines)
                
                # Contar clases
                class_matches = re.findall(r'^\s*(public|private|protected)?\s*class\s+\w+', content, re.MULTILINE)
                performance_data['total_classes'] += len(class_matches)
                
                # Calcular complejidad simple (número de métodos)
                method_matches = re.findall(r'(public|private|protected)\s+[\w<>[\]\s]+\s+\w+\s*\([^)]*\)', content)
                performance_data['complexity_score'] += len(method_matches)
                
            except:
                continue
                
        # Generar recomendaciones
        if performance_data['total_lines'] > 10000:
            performance_data['recommendations'].append("Considerar dividir archivos muy grandes (>10000 líneas)")
            
        if performance_data['complexity_score'] > 500:
            performance_data['recommendations'].append("Alta complejidad: considerar refactoring para mejorar mantenibilidad")
            
        if performance_data['total_files'] > 100:
            performance_data['recommendations'].append("Many files: considerar reorganizar en módulos")
            
        return performance_data

    def run_full_verification(self) -> Dict[str, Any]:
        """Ejecuta la verificación completa del proyecto"""
        print("🔍 Iniciando verificación del proyecto Enhanced Agar Game...")
        print("=" * 60)
        
        start_time = datetime.now()
        
        # 1. Escanear archivos Java
        print("📁 Escaneando archivos Java...")
        java_files = self.scan_java_files()
        print(f"   Encontrados {len(java_files)} archivos Java")
        
        # 2. Verificar imports
        print("\n📦 Verificando imports...")
        all_imports = set()
        import_issues = []
        
        for java_file in java_files:
            imports = self.extract_imports(java_file)
            all_imports.update(imports)
            file_issues = self.verify_import_compatibility(imports, java_file)
            import_issues.extend(file_issues)
            
        print(f"   Total de imports únicos: {len(all_imports)}")
        
        # 3. Verificar Android Manifest
        print("\n📱 Verificando AndroidManifest.xml...")
        manifest_info = self.check_android_manifest()
        
        # 4. Verificar consistencia
        print("\n🔄 Verificando consistencia...")
        consistency_issues = self.check_file_consistency()
        
        # 5. Analizar métodos
        print("\n🔧 Analizando estructura de código...")
        method_analysis_issues = []
        for java_file in java_files:
            analysis = self.analyze_method_signatures(java_file)
            method_analysis_issues.extend(analysis['issues'])
            
        # 6. Generar reporte de rendimiento
        print("\n📊 Generando reporte de rendimiento...")
        performance_data = self.generate_performance_report()
        
        # 7. Compilar todos los issues
        all_issues = []
        all_issues.extend(import_issues)
        all_issues.extend(manifest_info['issues'])
        all_issues.extend(consistency_issues)
        all_issues.extend(method_analysis_issues)
        
        end_time = datetime.now()
        verification_time = (end_time - start_time).total_seconds()
        
        # Generar reporte final
        report = {
            'timestamp': datetime.now().isoformat(),
            'verification_time_seconds': verification_time,
            'project_root': str(self.project_root),
            'summary': {
                'total_java_files': len(java_files),
                'total_imports': len(all_imports),
                'total_issues': len(all_issues),
                'critical_issues': len([i for i in all_issues if 'error' in i.lower()]),
                'warnings': len([i for i in all_issues if 'warning' in i.lower() or 'warn' in i.lower()])
            },
            'files_analyzed': [str(f) for f in java_files],
            'imports': list(all_imports),
            'issues': all_issues,
            'android_manifest': manifest_info['data'],
            'performance': performance_data,
            'recommendations': self.generate_recommendations(all_issues, performance_data)
        }
        
        return report

    def generate_recommendations(self, issues: List[str], performance_data: Dict) -> List[str]:
        """Genera recomendaciones basadas en el análisis"""
        recommendations = []
        
        # Recomendaciones basadas en issues
        if any('import' in issue.lower() for issue in issues):
            recommendations.append("Revisar imports no estándar - verificar dependencias en build.gradle")
            
        if any('manifest' in issue.lower() for issue in issues):
            recommendations.append("Corregir configuración en AndroidManifest.xml")
            
        if any('activity' in issue.lower() for issue in issues):
            recommendations.append("Verificar registro de actividades en el manifest")
            
        # Recomendaciones de rendimiento
        recommendations.extend(performance_data.get('recommendations', []))
        
        # Recomendaciones generales
        recommendations.append("Ejecutar lint de Android para verificación adicional")
        recommendations.append("Probar compilación con './gradlew assembleDebug'")
        recommendations.append("Ejecutar tests unitarios con './gradlew test'")
        
        return recommendations

    def save_report(self, report: Dict[str, Any], output_file: str = None):
        """Guarda el reporte en formato JSON y texto"""
        if output_file is None:
            output_file = self.project_root / "verification_report.json"
            
        # Guardar JSON
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(report, f, indent=2, ensure_ascii=False)
            
        # Guardar reporte legible
        text_file = output_file.with_suffix('.txt')
        with open(text_file, 'w', encoding='utf-8') as f:
            f.write("REPORTE DE VERIFICACIÓN - ENHANCED AGAR GAME\n")
            f.write("=" * 50 + "\n\n")
            f.write(f"Fecha: {report['timestamp']}\n")
            f.write(f"Tiempo de verificación: {report['verification_time_seconds']:.2f} segundos\n")
            f.write(f"Directorio: {report['project_root']}\n\n")
            
            f.write("RESUMEN:\n")
            f.write("-" * 20 + "\n")
            for key, value in report['summary'].items():
                f.write(f"{key.replace('_', ' ').title()}: {value}\n")
            f.write("\n")
            
            f.write("ARCHIVOS ANALIZADOS:\n")
            f.write("-" * 30 + "\n")
            for file_path in report['files_analyzed']:
                f.write(f"- {file_path}\n")
            f.write("\n")
            
            f.write("IMPORTS ENCONTRADOS:\n")
            f.write("-" * 30 + "\n")
            for import_line in sorted(report['imports']):
                f.write(f"- {import_line}\n")
            f.write("\n")
            
            if report['issues']:
                f.write("ISSUES ENCONTRADOS:\n")
                f.write("-" * 30 + "\n")
                for i, issue in enumerate(report['issues'], 1):
                    f.write(f"{i:3d}. {issue}\n")
                f.write("\n")
            else:
                f.write("✅ NO SE ENCONTRARON ISSUES\n\n")
                
            if report['recommendations']:
                f.write("RECOMENDACIONES:\n")
                f.write("-" * 30 + "\n")
                for i, rec in enumerate(report['recommendations'], 1):
                    f.write(f"{i:2d}. {rec}\n")
                f.write("\n")
                
            f.write("VERIFICACIÓN COMPLETADA ✅\n")
            
        print(f"\n📄 Reporte guardado en:")
        print(f"   JSON: {output_file}")
        print(f"   Texto: {text_file}")

    def print_summary(self, report: Dict[str, Any]):
        """Imprime resumen de la verificación"""
        print("\n" + "=" * 60)
        print("📋 RESUMEN DE VERIFICACIÓN")
        print("=" * 60)
        
        summary = report['summary']
        print(f"📁 Archivos Java analizados: {summary['total_java_files']}")
        print(f"📦 Imports únicos encontrados: {summary['total_imports']}")
        print(f"⚠️  Total de issues: {summary['total_issues']}")
        print(f"🔴 Issues críticos: {summary['critical_issues']}")
        print(f"🟡 Warnings: {summary['warnings']}")
        
        if summary['total_issues'] == 0:
            print("\n🎉 ¡PROYECTO VERIFICADO SIN ERRORES!")
        elif summary['critical_issues'] == 0:
            print("\n✅ Solo warnings encontrados - proyecto listo para compilar")
        else:
            print("\n❌ Se encontraron issues críticos que deben ser corregidos")
            
        print(f"\n⏱️  Tiempo de verificación: {report['verification_time_seconds']:.2f} segundos")

def main():
    """Función principal"""
    # Determinar directorio del proyecto
    script_dir = Path(__file__).parent
    project_root = script_dir
    
    print("🚀 Verificador de Proyecto Enhanced Agar Game")
    print("   Versión: 1.0")
    print(f"   Directorio: {project_root}")
    
    # Crear verificador
    verifier = ProjectVerifier(project_root)
    
    # Ejecutar verificación
    report = verifier.run_full_verification()
    
    # Mostrar resumen
    verifier.print_summary(report)
    
    # Guardar reportes
    verifier.save_report(report)
    
    # Exit code basado en issues críticos
    if report['summary']['critical_issues'] > 0:
        sys.exit(1)  # Error si hay issues críticos
    else:
        sys.exit(0)  # Éxito si no hay issues críticos

if __name__ == "__main__":
    main()