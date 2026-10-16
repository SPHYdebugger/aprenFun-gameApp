# Plan de Implementación - Segundo PR: Integración Completa de Juego y Contenido Remoto

## 1. Visión General
El objetivo de este **Segundo PR** es conectar completamente el motor de juego de **AprendemosLaVida** con los servicios autoritativos del backend **aprenFun_back**, sustituyendo el cómputo y banco de preguntas local por las sesiones de juego remotas, el catálogo de contenido y las puntuaciones/rankings globales.

---

## 2. Fase 1: Descarga y Gestión de Contenido Curricular
- **Modelos DTOs (`ContentModels.kt`)**:
  - `ContentManifest` (`curriculum`, `release`, `locale`, `hash`, `minimumClientVersion`, `publishedAt`, `downloadUrl`)
  - `ContentRelease` (`curriculum`, `release`, `locale`, `hash`, `questions`)
  - `ContentQuestion` (`id`, `stableKey`, `subject`, `topic`, `kind`, `gradeMin`, `gradeMax`, `prompt`, `generationRule`, `options`)
  - `ContentOption` (`id`, `stableKey`, `text`)
- **Servicio API (`ContentApiService.kt`)**:
  - `GET /api/v1/content/manifest` (soporte de header `If-None-Match` para ETags)
  - `GET /api/v1/content/releases/{version}`
- **Repositorio (`ContentRepository.kt`)**:
  - Gestión de caché local del contenido.
  - Comprobación de versión mínima de cliente exigida por el manifest.

---

## 3. Fase 2: Sesiones de Juego - Modo Preguntas y Castillos (`GameSession`)
- **Modelos DTOs (`GameSessionModels.kt`)**:
  - `CreateGameSession` (`childProfileId`, `subjectCode`, `topicCode`, `questionCount`, `locale`)
  - `GameSession` (`id`, `mode`, `status`, `release`, `startedAt`, `expiresAt`, `score`, `answeredQuestions`, `correctAnswers`, `totalQuestions`, `currentQuestion`)
  - `PresentedQuestion` (`id`, `contentQuestionId`, `position`, `interaction`, `prompt`, `options`, `castle`, `presentedAt`, `deadline`)
  - `CastleProblem` (`top`, `bottom`, `operation`, `columns`, `attemptNumber`, `availablePoints`)
  - `SubmitGameAnswer` (`sessionQuestionId`, `selectedOptionId`, `numericAnswer`)
  - `GameAnswer` (`answerId`, `sessionId`, `sessionQuestionId`, `correct`, `correctOptionId`, `correctNumericAnswer`, `questionResolved`, `points`, `totalScore`, `sessionStatus`, `nextQuestion`)
- **Servicio API (`GameSessionApiService.kt`)**:
  - `POST /api/v1/game-sessions` (`Header: Idempotency-Key`)
  - `GET /api/v1/game-sessions/{sessionId}`
  - `POST /api/v1/game-sessions/{sessionId}/answers` (`Header: Idempotency-Key`)
  - `POST /api/v1/game-sessions/{sessionId}/abandon`
  - `POST /api/v1/game-sessions/{sessionId}/finish`
- **Integración en UI (`GameActivity`, `MathGameActivity`, `EnglishGameActivity`, `SocialGameActivity`, `LanguageGameActivity`, `TorresActivity`)**:
  - Sustitución de los `GameManager` locales por peticiones autoritativas al servidor.
  - La respuesta de si la opción es correcta, los puntos ganados y la siguiente pregunta proceden 100% del servidor.

---

## 4. Fase 3: Sesiones de Juego - Modo Historia (`StorySession`)
- **Modelos DTOs (`StorySessionModels.kt`)**:
  - `CreateStorySession` (`childProfileId`, `mapCount`, `durationSeconds`, `gateTopics`, `locale`)
  - `StorySession` (`id`, `status`, `release`, `locale`, `startedAt`, `remainingTimeMs`, `score`, `mapCount`, `completedMaps`, `finishReason`, `currentMap`, `santi`)
  - `StoryMap` (`id`, `position`, `visualTheme`, `layoutSeed`, `status`, `resolvedGates`, `minimumGatesToExit`, `gates`)
  - `StoryGate` (`id`, `position`, `topic`, `optional`, `status`, `points`, `currentQuestion`)
  - `StorySanti` (`availableOnCurrentMap`, `status`, `rewardPoints`, `currentChallenge`)
  - `SubmitStoryAnswer` (`gateId`, `sessionQuestionId`, `selectedOptionId`, `numericAnswer`)
- **Servicio API (`StorySessionApiService.kt`)**:
  - `POST /api/v1/story-sessions` (`Header: Idempotency-Key`)
  - `GET /api/v1/story-sessions/{sessionId}`
  - `POST /api/v1/story-sessions/{sessionId}/gates/{gateId}/open`
  - `POST /api/v1/story-sessions/{sessionId}/answers` (`Header: Idempotency-Key`)
  - `POST /api/v1/story-sessions/{sessionId}/maps/{mapId}/exit`
  - `POST /api/v1/story-sessions/{sessionId}/santi` (`action`: `OFFER`, `ACCEPT`, `DECLINE`)
  - `POST /api/v1/story-sessions/{sessionId}/santi/answers` (`Header: Idempotency-Key`)
  - `POST /api/v1/story-sessions/{sessionId}/finish`
  - `POST /api/v1/story-sessions/{sessionId}/abandon`
- **Integración en UI (`StoryGameActivity`, `StoryGameView`)**:
  - Mapeo de la semilla de mapa (`layoutSeed`), mundos y puertas desde el backend.
  - Apertura de puertas y envío de respuestas al servidor.
  - Reto opcional de Santi autoritativo (+500 puntos otorgados por el backend).

---

## 5. Fase 4: Progreso, Recompensas y Rankings Moderados
- **Modelos DTOs (`ProgressLeaderboardModels.kt`)**:
  - `PlayerProgress` (`pointsBalance`, `currentStreak`, `longestStreak`, `topics`)
  - `PlayerRewards` (`achievements`, `cosmetics`)
  - `PlayerScore` (`sessionId`, `mode`, `score`, `correctAnswers`, `totalQuestions`, `playedAt`, `source`)
  - `PublicLeaderboard` (`entries`, `page`, `size`, `totalElements`, `totalPages`)
- **Servicios API (`ProgressApiService.kt` & `LeaderboardApiService.kt`)**:
  - `GET /api/v1/children/{childId}/progress`
  - `GET /api/v1/children/{childId}/rewards`
  - `GET /api/v1/children/{childId}/scores`
  - `GET /api/v1/leaderboards` (parámetros: `mode`, `period`, `page`, `size`)
- **Integración en UI (`ScoresActivity`, `ResultActivity`, `StoryStreakActivity`)**:
  - Visualización del balance real de puntos y rachas calculadas por el servidor.
  - Carga de la tabla pública de clasificación moderada.

---

## 6. Fase 5: Importación de Datos Legacy (`LegacyImport`)
- **Modelos DTOs (`LegacyImportModels.kt`)**:
  - `LegacyImport` (`sourceSchemaVersion`, `globalPoints`, `preferences`, `avatar`, `streak`, `scores`)
  - `LegacyImportResult` (`childProfileId`, `status`, `importedAt`, `pointsImported`, `scoresImported`)
- **Servicio y Repositorio (`LegacyImportApiService.kt`)**:
  - `POST /api/v1/children/{childId}/legacy-import` (`Header: Idempotency-Key`)
  - Migración única y segura del estado local previo del usuario hacia su perfil en la API REST.

---

## 7. Fase 6: Estrategia de Tests y Verificación
- **Tests de Integración y Contrato (`GameContractTests.kt`)**:
  - MockWebServer para verificar la creación y actualización de sesiones de juego y modo historia.
  - Verificación de la presencia y formato del cabecero `Idempotency-Key` (UUID v4) en peticiones de mutación.
- **Tests de Manejo de Red en Juego**:
  - Validación de que la pérdida de conexión interrumpe la partida mostrando un aviso interactivo sin intentar resolver o puntuar localmente.

---

## 8. Reglas e Imponderables
- 🛑 **Prohibido el juego offline**: Si no hay conexión a Internet, la app informará al usuario y no permitirá iniciar o responder preguntas localmente.
- 🛑 **Sin cálculo local de resultados**: Ningún temporizador autoritativo, racha, bonificación o puntuación se calculará en la app.
- 🛑 **Idempotencia fija**: Las claves de idempotencia se generan una sola vez por acción del jugador y jamás se reintentan con un UUID distinto.
