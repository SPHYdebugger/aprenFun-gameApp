# Plan correctivo de integración Android con aprenFun_back

Fecha: 2026-10-17

## 1. Encargo recibido

El objetivo indicado fue corregir y completar la integración real de Android con el backend `aprenFun_back`, sin modificar el backend y usando como contrato obligatorio:

- OpenAPI 3.1.0.
- API v0.8.0.
- `applicationId: com.aprenfun.es`.
- Desarrollo en emulador: `http://10.0.2.2:8080/api/v1/`.
- Producción: `https://api.aprenfun.es/api/v1/`.
- Contrato esperado: `src/main/resources/static/api-docs/v1/openapi.yaml`.

El encargo pidió corregir estos puntos:

1. Eliminar el motor local de historia y usar exclusivamente `StorySession API`.
2. Corregir las respuestas JSON de `StorySessionApiService`, eliminando `Response<Void>`.
3. Eliminar puntuaciones y marcadores locales de las partidas online.
4. Centralizar la conversión de códigos de asignatura y modo.
5. Usar temporizadores autoritativos del backend.
6. Persistir las claves `Idempotency-Key` hasta obtener una respuesta definitiva.
7. Crear el flujo real de importación legacy.
8. Corregir ranking e historial usando códigos backend y sin datos privados en el ranking público.
9. Mantener la seguridad de red y el almacenamiento seguro de tokens.
10. Conectar realmente todas las Activities de juego, resultados, ranking, importación, progreso y recompensas.
11. Añadir tests de autenticación, sesiones, historia, castillos, red, idempotencia, importación y ausencia de fallback local.
12. Verificar compilación debug/release, lint y tests instrumentados cuando estén disponibles.

Los criterios de aceptación exigían que todas las partidas y respuestas fueran autoritativas del backend, que no existiera juego offline, que no se calcularan puntos localmente, que historia y castillos usaran sus protocolos remotos, y que los errores HTTP y `application/problem+json` tuvieran tratamiento específico.

## 2. Correcciones aplicadas

### 2.1. Flujo común de partidas remotas

Se creó:

- `app/src/main/java/com/example/aprendemoslavida/activities/RemoteGameActivity.kt`

Esta Activity base implementa:

- Creación remota de sesiones.
- Renderizado de `PresentedQuestion`.
- Envío de respuestas al backend.
- Uso de `PresentedQuestion.deadline` y `GameSession.expiresAt`.
- Presentación de la respuesta correcta enviada por el servidor.
- Consulta final de la sesión antes de mostrar resultados.
- Tratamiento específico de 401, 403, 409, 429 y 5xx.
- Ausencia de generación local de preguntas.
- Ausencia de cálculo local de puntuación.

Las siguientes Activities pasan a usar este flujo:

- `GameActivity`: `natural-sciences`.
- `MathGameActivity`: `mathematics` + `multiplication`.
- `AddSubMathGameActivity`: `mathematics` + `addition-subtraction`.
- `EnglishGameActivity`: `english`.
- `SocialGameActivity`: `social-sciences`.
- `LanguageGameActivity`: `spanish-language`.
- `TorresActivity`: `mathematics` + `castles`.

El antiguo `SumasTorresActivity.kt`, que generaba castillos localmente, fue eliminado.

### 2.2. Historia exclusivamente remota

Se sustituyó el flujo local de `StoryGameActivity`.

La Activity usa:

- `POST /story-sessions`.
- `GET /story-sessions/{sessionId}`.
- `POST /story-sessions/{sessionId}/gates/{gateId}/open`.
- `POST /story-sessions/{sessionId}/answers`.
- `POST /story-sessions/{sessionId}/maps/{mapId}/exit`.
- `POST /story-sessions/{sessionId}/santi`.
- `POST /story-sessions/{sessionId}/santi/answers`.
- `POST /story-sessions/{sessionId}/finish`.
- `POST /story-sessions/{sessionId}/abandon`.

Ya no usa como fuente de verdad:

- `StoryProgressManager`.
- `StoryQuestionProvider`.
- `StoryScoreManager`.
- `StoryMap` local.
- Preguntas locales.
- Puntuación local.
- Temporizador de decisión local.

El servidor decide preguntas, puertas, respuestas, puntos, Santi, mapas, estado y resultado final.

### 2.3. DTOs de historia

Se corrigieron:

- `StorySessionApiService.kt`.
- `StorySessionRepository.kt`.
- `StorySessionModels.kt`.

Se añadieron los modelos:

- `GateOpenResponse`.
- `StoryAnswerResponse`.
- `SantiState`.
- `SantiAnswerResponse`.

Los endpoints de historia ya no usan `Response<Void>` cuando devuelven JSON.

La UI representa ahora:

- Pregunta siguiente.
- Estado de puerta.
- Puntos de la respuesta.
- Puntuación total.
- Tiempo restante.
- Estado de Santi.
- Estado final de la sesión.

### 2.4. Puntuación y ranking

Se corrigió `ResultActivity` para que:

- No llame a `SettingsManager.addGlobalPoints()`.
- No llame a `ScoreManager.saveScore()`.
- No pida nombres libres.
- Muestre la puntuación recibida del servidor.
- Reabra la modalidad y tema remoto correctos.

Se corrigió `ScoresActivity` para que:

- No use `loadLocalFallbackScores()`.
- No lea marcadores locales como fallback.
- Muestre error o estado de reintento si no hay conexión.
- Envíe los códigos de modo del backend.
- Mantenga el historial privado remoto.
- Use únicamente los campos públicos del ranking: pseudonym, score y date.

Los datos `source=LEGACY` pueden aparecer en el historial privado, pero no se usan para generar ranking local.

### 2.5. Conversión centralizada de códigos

Se creó:

- `app/src/main/java/com/example/aprendemoslavida/data/model/SubjectMapper.kt`.

Conversión aplicada:

| Código local | Código backend |
|---|---|
| natural | natural-sciences |
| math | mathematics |
| english | english |
| social | social-sciences |
| language | spanish-language |
| story | story |

Temas matemáticos:

- `multiplication`.
- `addition-subtraction`.
- `castles`.

Se eliminó el uso de `subjectCode = "NATURAL"` en las Activities.

### 2.6. Temporizadores autoritativos

Las partidas estándar usan:

- `PresentedQuestion.deadline`.
- `GameSession.expiresAt`.

Historia usa:

- `StorySession.remainingTimeMs`.

El contador local solo sirve para representar visualmente el tiempo. Al llegar a cero se envía una operación al backend y el servidor decide si la respuesta o la sesión han caducado.

`SettingsManager` ya no decide si una pregunta o partida online ha terminado.

### 2.7. Idempotencia

Se creó:

- `app/src/main/java/com/example/aprendemoslavida/data/storage/IdempotencyStorage.kt`.

Las claves se:

- Generan una sola vez por acción.
- Guardan con `commit()` antes de enviar la petición.
- Conservan durante timeouts y errores de red.
- Eliminan solo después de una respuesta definitiva.

Se aplican a:

- Creación de partidas.
- Respuestas de partidas.
- Creación de historia.
- Respuestas de historia.
- Respuestas de Santi.
- Importación legacy.

Los repositorios ya no generan automáticamente nuevas claves UUID en cada reintento.

### 2.8. Importación legacy

Se creó:

- `app/src/main/java/com/example/aprendemoslavida/activities/LegacyImportActivity.kt`.

El flujo incluye:

- Resumen de datos locales.
- Perfil infantil destino.
- Confirmación adulta mediante `GuardianPasswordDialog`.
- Envío de `POST /children/{childId}/legacy-import`.
- Persistencia de `Idempotency-Key`.
- Tratamiento de `IMPORTED`.
- Tratamiento de `ALREADY_IMPORTED`.
- Tratamiento de 409.
- Reintento con la misma clave.
- Conservación de datos locales hasta confirmar la importación.

Se conservan:

- `sourceSchemaVersion`.
- `sourceAppVersion`.
- `globalPoints`.
- `preferences`.
- `avatar`.
- `streak`.
- `scores`.

Los nombres locales de los marcadores no se incluyen en el DTO enviado.

También se añadió el acceso desde el menú principal.

### 2.9. Progreso y recompensas

Se creó:

- `app/src/main/java/com/example/aprendemoslavida/activities/ProgressActivity.kt`.

La Activity consulta remotamente:

- Progreso del perfil.
- Puntos.
- Racha.
- Temas.
- Logros.
- Cosméticos desbloqueados.

No usa puntos locales para representar el progreso online.

### 2.10. Seguridad de red y tokens

Se mantuvo:

- Permiso `INTERNET`.
- Base URL debug configurable en `BuildConfig`.
- Base URL release HTTPS.
- HTTP solo en `app/src/debug/AndroidManifest.xml`.
- `usesCleartextTraffic=false` en el manifest principal/release.
- Tokens en `EncryptedSharedPreferences` y Android Keystore.
- No persistencia de contraseñas.
- No persistencia de guardian tokens.

Se eliminó el fallback de tokens a preferencias normales en producción.

### 2.11. Errores RFC 7807

Se mantuvo el uso de `ProblemJsonParser` para respuestas `application/problem+json`.

Las pantallas de juego, historia e importación ofrecen mensajes específicos para:

- 401.
- 403.
- 409.
- 429.
- 5xx.
- Timeout o pérdida de conexión.

### 2.12. Documentación corregida

Se actualizaron:

- `docs/plan_implementacion_pr1.md`.
- `docs/plan_implementacion_pr2.md`.
- `NOTES.md`.

Ahora indican explícitamente que la existencia de DTOs, repositorios o tests no certifica que el flujo de usuario esté conectado.

## 3. Tests añadidos o actualizados

Se añadieron o actualizaron:

- `GameContractTests.kt`.
- `ConcurrentRefreshTest.kt`.
- `RemoteIntegrationCorrectionTests.kt`.
- `AnswerFeedback.kt`.

Se verifican:

- Login y refresh.
- Renovación concurrente.
- Creación de partidas.
- Respuestas correctas e incorrectas.
- Colores de respuesta seleccionada y correcta.
- Castillos y respuestas numéricas.
- Historia.
- Santi.
- Reintento con la misma clave.
- Importación legacy.
- `ALREADY_IMPORTED`.
- 409.
- Ranking.
- Mapeo de códigos backend.

## 4. Verificaciones ejecutadas

Correcto:

- `:app:testDebugUnitTest`: 17 tests.
- `:app:compileDebugKotlin`.
- `:app:assembleDebug`.
- `:app:assembleRelease`.
- `:app:lint`.
- `git diff --check`.

No hay tests instrumentados en el checkout.

## 5. Pendientes externos

No se ejecutó la prueba manual contra el backend local porque:

- El daemon de Docker no estaba activo.
- El archivo indicado `src/main/resources/static/api-docs/v1/openapi.yaml` no está presente en este checkout.

Por tanto, la validación final contra PostgreSQL/Docker y la comprobación exacta contra el YAML OpenAPI deben ejecutarse cuando el backend y su contrato estén disponibles.

## 6. Confirmación de integración

La auditoría de las Activities confirma que:

- Ningún modo de juego activo genera preguntas localmente.
- Ningún modo de juego activo calcula puntuaciones localmente.
- Ningún modo de juego activo usa fallback offline.
- Las respuestas y los estados finales proceden del backend.
- Historia usa exclusivamente `StorySession API`.
- Castillos usa exclusivamente `game-sessions` con `topicCode=castles`.
- `ResultActivity` no escribe puntuaciones locales.
- `ScoresActivity` solo muestra datos remotos.
