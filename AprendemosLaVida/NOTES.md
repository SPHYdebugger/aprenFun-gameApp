# Cambios realizados

## Análisis del flujo existente (Paso 1)
- Entrada actual de `JUGAR`: `MainMenuActivity` lanzaba `GameModeActivity` con `ACTION_PLAY`.
- Menú de selección de asignaturas: `GameModeActivity` (`activity_game_mode.xml`).
- Flujo de preguntas:
  - Naturales: `GameActivity` + `GameManager`.
  - Matemáticas: `MathTopicsActivity` -> `MathGameActivity` / `AddSubMathGameActivity`.
  - Inglés: `EnglishGameActivity` + `EnglishGameManager`.
  - Sociales: `SocialTopicsActivity` -> `SocialGameActivity` + `SocialGameManager`.
- Puntuaciones:
  - Guardado/lectura: `ScoreManager`.
  - Pantalla final: `ResultActivity`.
  - Listado: `ScoresActivity`.

## Archivos tocados y motivo
- `MainMenuActivity.kt`: `JUGAR` ahora abre menú intermedio de tipo de juego.
- `GameTypeMenuActivity.kt` + `activity_game_type_menu.xml`: nuevo menú con `MODO PREGUNTAS` y `MODO HISTORIA`.
- `StoryGameActivity.kt` + `activity_story_game.xml`: host del modo historia y UI con D-pad overlay.
- `story/StoryGameView.kt`: bucle update/draw, movimiento top-down y colisiones.
- `story/StoryMap.kt`: mapa base y salida.
- `story/StoryGate.kt`: definición de checkpoints bloqueantes.
- `story/StoryProgressManager.kt`: estado de checkpoints, preguntas y tiempo por gate.
- `story/StoryQuestionProvider.kt`: reutiliza bancos de preguntas existentes.
- `story/StoryQuestionDialogFragment.kt`: UI reutilizable para pregunta bloqueante.
- `story/StoryScoreManager.kt`: reglas de puntos del modo historia.
- `ScoreManager.kt`: nuevo modo de puntuación `story`.
- `ResultActivity.kt`: soporte de rejugar para `MODE_STORY`.
- `ScoresActivity.kt`: título para puntuaciones de historia.
- `AndroidManifest.xml`: registro de nuevas activities.
- `strings.xml` y `values-gl/strings.xml`: textos de menú tipo de juego y modo historia.
- `model/GameMode.kt`: enum simple de modos de juego compartidos.

## Checklist rápida (Paso 8)
- [ ] `JUGAR -> Tipo de juego`
- [ ] `Modo preguntas -> menú asignaturas -> flujo actual OK`
- [ ] `Modo historia -> mapa -> checkpoints -> pregunta repetida al fallar`
- [ ] `Final -> éxito -> post-juego con botones`
