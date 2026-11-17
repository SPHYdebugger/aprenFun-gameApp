# AprenFun Android

## Requisitos para compilar

- **JDK 17 o 21.** El proyecto usa Gradle 8.13 con AGP 8.13.2, y Gradle 8.13 no
  arranca con Java 25 (falla con `Unsupported class file major version 69`).
  Android Studio reciente trae Java 25 por defecto, así que hay que cambiarlo:
  - **Android Studio:** Settings → Build, Execution, Deployment → Build Tools →
    Gradle → *Gradle JDK* → elegir un JDK 17 o 21 (por ejemplo, Temurin 21).
  - **Terminal:** `JAVA_HOME` debe apuntar a ese mismo JDK. Compruébalo con
    `java -version` antes de lanzar `./gradlew`.
- Android SDK con la ruta en `local.properties` (`sdk.dir=...`).

> Subir a Gradle 9.x requiere AGP 9, que es un cambio mayor; de momento se
> mantiene Gradle 8.13 con JDK 17/21.

## Configurar el backend (debug)

La URL del backend en debug se lee de `local.properties` (no se sube a git):

```properties
aprenfun.baseUrl=https://pc-staging.tail9df616.ts.net/api/v1/
```

Debe terminar en `/`. Sin esa línea se usa el host del emulador
(`http://10.0.2.2:8080/api/v1/`). Staging solo es accesible con Tailscale
activado; usa un móvil físico.

## Compilar y probar

```sh
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

En Windows, `gradlew.bat` en lugar de `./gradlew`.
