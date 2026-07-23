# Tecnologías — Inventory Industry

## Stack Tecnológico

| Categoría | Tecnología | Versión | Propósito |
|-----------|-----------|---------|-----------|
| **Lenguaje** | Kotlin | 2.0.21 | Lenguaje principal (JVM) |
| **UI Framework** | Jetpack Compose Desktop | 1.7.3 | UI declarativa multiplataforma |
| **UI** | Compose Material 3 | (bundled) | Sistema de diseño Material Design 3 |
| **UI** | Compose Material Icons Extended | (bundled) | Iconografía extendida |
| **ORM** | JetBrains Exposed (core) | 0.55.0 | Mapeo objeto-relacional |
| **ORM** | JetBrains Exposed (jdbc) | 0.55.0 | Capa JDBC para Exposed |
| **Database** | SQLite JDBC | 3.47.1.0 | Base de datos embebida |
| **Concurrency** | Kotlin Coroutines (core) | 1.9.0 | Concurrencia y estructuración de código |
| **Concurrency** | Kotlin Coroutines (swing) | 1.9.0 | Integración coroutines-Swing |
| **PDF** | Apache PDFBox | 2.0.31 | Generación de reportes PDF |
| **Build** | Gradle | 8.14 | Sistema de construcción |
| **Toolchain** | JVM (OpenJDK) | 17 | Target de compilación |

## Detalles por Tecnología

### Kotlin 2.0.21

- **Toolchain**: JVM 17 (configurado en `build.gradle.kts`)
- **Compiler args**: `-opt-in=kotlin.RequiresOptIn` para APIs experimentales de Compose
- **Paradigma**: Funcional con null safety, data classes, sealed classes, extension functions

### Jetpack Compose Desktop 1.7.3

- **Plugin**: `org.jetbrains.compose` v1.7.3
- **Plugin Kotlin**: `org.jetbrains.kotlin.plugin.compose` v2.0.21
- **Patrón UI**: State hoisting, CompositionLocal, recomposición reactiva
- **Componentes custom**: 50+ componentes reutilizables en `ui/components/`
- **Gráficos**: Implementación custom con Canvas (`EnterpriseCharts.kt`)
- **Navegación**: Manual vía sealed class `ScreenRoute` + estado mutable

### JetBrains Exposed 0.55.0

- **Patrón**: Table objects como DSL tipado para queries
- **Auto-migración**: `SchemaUtils.createMissingTablesAndColumns()` crea/actualiza tablas al iniciar
- **Transacciones**: `transaction { }` blocks para atomicidad
- **15 tablas** definidas en `Tables.kt`
- **2200+ líneas** de queries y lógica en `InventoryRepository.kt`

### SQLite 3.47.1.0

- **Ubicación**: `~/.inventory-industry/inventory.db`
- **Foreign keys**: Habilitadas vía `?foreign_keys=ON` en la URL JDBC
- **Ventajas**: Zero-config, portátil (un solo archivo), suficiente para single-user
- **Limitaciones**: Sin concurrencia de escritura (aceptable para uso desktop)

### Apache PDFBox 2.0.31

- **Uso**: Generación de reportes PDF (ventas, inventario, etapas)
- **Generadores**: 3 clases de generación PDF dedicadas
- **Carácter**: Opcional — la app funciona sin generar PDFs

### Coroutines 1.9.0

- **Patrón**: `Dispatchers.Swing` para operaciones de UI, `Dispatchers.IO` para base de datos
- **Uso**: Carga de datos en segundo plano, operaciones de red (futuro)

## Build y Distribución

### Gradle 8.14

- **Wrapper**: Incluido (`gradlew` / `gradlew.bat`)
- **Plugin resolution**: `gradlePluginPortal()`, `mavenCentral()`, `google()`
- **Toolchain resolver**: `foojay-resolver-convention` v0.8.0

### Comandos Principales

| Comando | Propósito |
|---------|-----------|
| `./gradlew run` | Ejecutar la aplicación (también via `./start.sh`) |
| `./gradlew build` | Compilar y generar JAR |
| `./gradlew packageDmg` | Empaquetar para macOS |
| `./gradlew packageMsi` | Empaquetar para Windows |
| `./gradlew packageDeb` | Empaquetar para Linux |

### Formatos de Distribución

| Formato | Plataforma |
|---------|-----------|
| `.dmg` | macOS |
| `.msi` | Windows |
| `.deb` | Linux (Debian/Ubuntu) |

## Dependencias de Desarrollo (docs/)

| Herramienta | Propósito |
|------------|-----------|
| Node.js + Puppeteer | Generación de PDFs de documentación |
| Python | Scripts de generación de video |
| ffmpeg | Combinación de frames para video |

## Estructura de Archivos de Build

```
build.gradle.kts          # Build script principal
settings.gradle.kts       # Nombre del proyecto, plugin management
gradle.properties          # JVM args, caching habilitado
gradle/wrapper/            # Gradle wrapper
start.sh                  # Shortcut: ./gradlew run
```
