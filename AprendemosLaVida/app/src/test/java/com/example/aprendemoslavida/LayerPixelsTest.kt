package com.example.aprendemoslavida

import com.example.aprendemoslavida.avatar.LayerPixels
import org.junit.Assert.assertEquals
import org.junit.Test

class LayerPixelsTest {

    private fun argb(a: Int, r: Int, g: Int, b: Int) = (a shl 24) or (r shl 16) or (g shl 8) or b
    private fun alpha(pixel: Int) = (pixel ushr 24) and 0xFF

    @Test
    fun `la mascara multiplica el alfa y conserva el color`() {
        val pixels = intArrayOf(argb(255, 10, 20, 30), argb(200, 1, 2, 3), argb(0, 9, 9, 9))
        val mask = intArrayOf(argb(255, 255, 255, 255), argb(0, 255, 255, 255), argb(255, 0, 0, 0))
        LayerPixels.applyMask(pixels, mask)
        assertEquals(argb(255, 10, 20, 30), pixels[0])
        assertEquals(0, alpha(pixels[1]))
        assertEquals(0, alpha(pixels[2]))
    }

    @Test
    fun `mascara a medias deja el alfa a medias`() {
        val pixels = intArrayOf(argb(255, 100, 100, 100))
        LayerPixels.applyMask(pixels, intArrayOf(argb(128, 255, 255, 255)))
        assertEquals(128, alpha(pixels[0]))
    }

    @Test
    fun `borra la piel por encima del pelo en cada columna con margen`() {
        // 1 columna x 8 filas: piel en todas; pelo empieza en y=3.
        val w = 1
        val h = 8
        val body = IntArray(h) { argb(255, 200, 120, 80) }
        val hair = IntArray(h) { y -> if (y in 3..5) argb(255, 0, 0, 0) else 0 }
        LayerPixels.hideAboveHair(body, hair, w, h, rows = 1, margin = 2)
        // Se borran y = 0..4 (hasta el pelo + 2 px de margen); el resto se conserva.
        for (y in 0..4) assertEquals("y=$y", 0, alpha(body[y]))
        for (y in 5..7) assertEquals("y=$y", 255, alpha(body[y]))
    }

    @Test
    fun `columnas sin pelo no se tocan`() {
        val w = 2
        val h = 4
        val body = IntArray(w * h) { argb(255, 1, 1, 1) }
        val hair = IntArray(w * h) { i -> if (i % w == 0 && i / w == 2) argb(255, 0, 0, 0) else 0 }
        LayerPixels.hideAboveHair(body, hair, w, h, rows = 1, margin = 0)
        assertEquals(0, alpha(body[0 * w + 0]))
        assertEquals(0, alpha(body[1 * w + 0]))
        for (y in 0 until h) assertEquals(255, alpha(body[y * w + 1]))
    }

    @Test
    fun `en una hoja cada fila de tiles se procesa por separado`() {
        // 1 columna, 2 filas de tiles de 4 px: el pelo solo está en el segundo tile (y=6).
        val body = IntArray(8) { argb(255, 1, 1, 1) }
        val hair = IntArray(8) { y -> if (y == 6) argb(255, 0, 0, 0) else 0 }
        LayerPixels.hideAboveHair(body, hair, 1, 8, rows = 2, margin = 0)
        for (y in 0..3) assertEquals("el primer tile no se toca, y=$y", 255, alpha(body[y]))
        assertEquals(0, alpha(body[4]))
        assertEquals(0, alpha(body[5]))
        assertEquals(255, alpha(body[6]))
    }
}
