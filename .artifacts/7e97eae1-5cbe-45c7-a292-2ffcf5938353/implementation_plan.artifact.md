# Implementación de Inyección de Dependencias (Hilt) y Refactorización a MVVM

Este plan detalla la implementación completa de Hilt en el proyecto DespensaCX, siguiendo las mejores prácticas de Android mediante la introducción de ViewModels para manejar la lógica de datos y separar las responsabilidades de la UI.

## User Review Required

> [!IMPORTANT]
> Se introducirán cambios estructurales en las Activities para delegar la lógica a los ViewModels. Las llamadas directas a `AppDatabase.getInstance(this)` serán eliminadas.

## Proposed Changes

### [Component Name] Infraestructura de Hilt

#### [MODIFY] [MainActivity.kt](file:///C:/Users/jlsj0/Documents/AndroidStudioProjects/DespensaCX/app/src/main/kotlin/com/example/despensacx/ui/MainActivity.kt)
#### [MODIFY] [DetalleListaActivity.kt](file:///C:/Users/jlsj0/Documents/AndroidStudioProjects/DespensaCX/app/src/main/kotlin/com/example/despensacx/ui/DetalleListaActivity.kt)
#### [MODIFY] [GestionTiendasActivity.kt](file:///C:/Users/jlsj0/Documents/AndroidStudioProjects/DespensaCX/app/src/main/kotlin/com/example/despensacx/ui/GestionTiendasActivity.kt)
#### [MODIFY] [ListasArchivadasActivity.kt](file:///C:/Users/jlsj0/Documents/AndroidStudioProjects/DespensaCX/app/src/main/kotlin/com/example/despensacx/ui/ListasArchivadasActivity.kt)
#### [MODIFY] [EstadisticasActivity.kt](file:///C:/Users/jlsj0/Documents/AndroidStudioProjects/DespensaCX/app/src/main/kotlin/com/example/despensacx/ui/EstadisticasActivity.kt)

### [Component Name] ViewModels [NEW]

Se crearán los siguientes archivos en un nuevo paquete `com.example.despensacx.viewmodel`:
- `MainViewModel.kt`
- `DetalleListaViewModel.kt`
- `GestionTiendasViewModel.kt`
- `ListasArchivadasViewModel.kt`
- `EstadisticasViewModel.kt`

Cada ViewModel inyectará los DAOs necesarios (`ListaDao`, `ProductoDao`, `TiendaDao`) a través de su constructor anotado con `@HiltViewModel`.

### [Component Name] Repositorios (Opcional pero recomendado)

Para una implementación más limpia, se podría introducir una capa de Repositorio, pero para este paso inicial inyectaremos los DAOs directamente en los ViewModels para simplificar.

## Verification Plan

### Automated Tests
- Se verificará que la aplicación compile correctamente tras los cambios.
- Se puede ejecutar `./gradlew assembleDebug` para confirmar que el procesamiento de anotaciones de Hilt funciona.

### Manual Verification
- Abrir la aplicación y verificar que la lista principal cargue correctamente.
- Navegar a cada pantalla y comprobar que las operaciones CRUD (crear, editar, eliminar) sigan funcionando.
- Probar el respaldo y restauración para asegurar que la inyección en las actividades no rompió esta funcionalidad.
