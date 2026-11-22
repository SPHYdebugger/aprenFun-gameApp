package com.example.aprendemoslavida

import com.example.aprendemoslavida.story.StoryWalkAnimator
import com.example.aprendemoslavida.story.StoryWalkAnimator.Facing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoryWalkAnimatorTest {

    private val step = StoryWalkAnimator.TILES_PER_FRAME

    @Test
    fun `empieza en reposo mirando abajo`() {
        val a = StoryWalkAnimator()
        assertEquals(Facing.DOWN, a.facing)
        assertFalse(a.moving)
        assertEquals(0, a.frame)
    }

    @Test
    fun `al arrancar muestra el primer paso y avanza un frame por cada tramo recorrido`() {
        val a = StoryWalkAnimator()
        a.update(1f, 0f, 0.01f)
        assertTrue(a.moving)
        assertEquals(1, a.frame)
        a.update(1f, 0f, step)
        assertEquals(2, a.frame)
        a.update(1f, 0f, step / 2)
        assertEquals(2, a.frame)
        a.update(1f, 0f, step / 2)
        assertEquals(3, a.frame)
        a.update(1f, 0f, step)
        assertEquals("el ciclo vuelve al frame 0", 0, a.frame)
    }

    @Test
    fun `al parar vuelve al reposo y conserva la direccion`() {
        val a = StoryWalkAnimator()
        a.update(-1f, 0f, step)
        assertEquals(Facing.LEFT, a.facing)
        a.update(0f, 0f, 0f)
        assertFalse(a.moving)
        assertEquals(0, a.frame)
        assertEquals("ya no se gira hacia abajo al parar", Facing.LEFT, a.facing)
    }

    @Test
    fun `contra una pared mira hacia alli pero no anima`() {
        val a = StoryWalkAnimator()
        a.update(0f, -1f, 0f)
        assertEquals(Facing.UP, a.facing)
        assertFalse(a.moving)
        assertEquals(0, a.frame)
    }

    @Test
    fun `la diagonal exacta mantiene la direccion actual`() {
        val horizontal = StoryWalkAnimator()
        horizontal.update(1f, 0f, step)
        horizontal.update(0.7071f, 0.7071f, step)
        assertEquals(Facing.RIGHT, horizontal.facing)

        val vertical = StoryWalkAnimator()
        vertical.update(0f, 1f, step)
        vertical.update(0.7071f, 0.7071f, step)
        assertEquals(Facing.DOWN, vertical.facing)
    }

    @Test
    fun `histeresis - cerca de la diagonal no cambia, pasada la banda si`() {
        val a = StoryWalkAnimator()
        a.update(1f, 0f, step)
        // 50 grados desde la horizontal: dentro de la banda, sigue a la derecha.
        a.update(0.643f, 0.766f, step)
        assertEquals(Facing.RIGHT, a.facing)
        // 60 grados: pasa a abajo.
        a.update(0.5f, 0.866f, step)
        assertEquals(Facing.DOWN, a.facing)
        // Volver a 40 grados no basta para regresar a la derecha; a 30 grados sí.
        a.update(0.766f, 0.643f, step)
        assertEquals(Facing.DOWN, a.facing)
        a.update(0.866f, 0.5f, step)
        assertEquals(Facing.RIGHT, a.facing)
    }

    @Test
    fun `invertir el sentido en el mismo eje cambia de inmediato`() {
        val a = StoryWalkAnimator()
        a.update(1f, 0f, step)
        a.update(-1f, 0f, step)
        assertEquals(Facing.LEFT, a.facing)
        a.update(0f, -1f, step)
        assertEquals(Facing.UP, a.facing)
    }
}
