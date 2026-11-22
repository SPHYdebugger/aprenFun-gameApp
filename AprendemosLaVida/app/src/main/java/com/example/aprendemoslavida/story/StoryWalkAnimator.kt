package com.example.aprendemoslavida.story

import kotlin.math.abs

/**
 * Estado visual del paso del jugador en el modo historia: dirección y frame de la hoja.
 *
 * - La dirección tiene histéresis: solo cambia de eje cuando el nuevo eje domina claramente
 *   (más de ~10° más allá de la diagonal), así la diagonal no hace parpadear el sprite.
 * - Los frames avanzan por distancia recorrida, no por tiempo: los pies no patinan y, si el
 *   jugador choca contra una pared, no anima.
 * - Ciclo de 4 frames: 0 neutro (también reposo), 1 paso, 2 neutro, 3 paso con la otra pierna.
 *   Al arrancar se muestra directamente el frame 1 para que el paso se note desde el primer momento.
 *
 * Kotlin puro: sin dependencias de Android, para probarlo en tests de JVM.
 */
class StoryWalkAnimator(
    private val frameCount: Int = 4,
    private val tilesPerFrame: Float = TILES_PER_FRAME,
    private val axisSwitchRatio: Float = AXIS_SWITCH_RATIO
) {
    enum class Facing { UP, DOWN, LEFT, RIGHT }

    var facing: Facing = Facing.DOWN
        private set
    var moving: Boolean = false
        private set
    var frame: Int = 0
        private set

    private var distanceInFrame = 0f

    /**
     * @param inputX, inputY vector de entrada (joystick o cruceta); (0, 0) = sin entrada.
     * @param movedTiles distancia que el jugador se ha movido de verdad en este tick (0 si una pared lo ha frenado).
     */
    fun update(inputX: Float, inputY: Float, movedTiles: Float) {
        val hasInput = inputX != 0f || inputY != 0f
        if (hasInput) {
            facing = resolveFacing(inputX, inputY)
        }
        if (!hasInput || movedTiles <= 0f) {
            stop()
            return
        }
        if (!moving) {
            moving = true
            frame = 1 % frameCount
            distanceInFrame = 0f
        }
        distanceInFrame += movedTiles
        while (distanceInFrame >= tilesPerFrame) {
            distanceInFrame -= tilesPerFrame
            frame = (frame + 1) % frameCount
        }
    }

    private fun stop() {
        moving = false
        frame = 0
        distanceInFrame = 0f
    }

    private fun resolveFacing(x: Float, y: Float): Facing {
        val ax = abs(x)
        val ay = abs(y)
        val currentlyHorizontal = facing == Facing.LEFT || facing == Facing.RIGHT
        val horizontal = if (currentlyHorizontal) ax * axisSwitchRatio >= ay else ax > ay * axisSwitchRatio
        return if (horizontal) {
            if (x >= 0f) Facing.RIGHT else Facing.LEFT
        } else {
            if (y >= 0f) Facing.DOWN else Facing.UP
        }
    }

    companion object {
        /** Distancia (en tiles) que se recorre por cada frame del ciclo: un ciclo completo cada tile. */
        const val TILES_PER_FRAME = 0.25f

        /** tan(55°): para cambiar de eje hay que pasar unos 10° de la diagonal (20° de banda en total). */
        const val AXIS_SWITCH_RATIO = 1.428f
    }
}
