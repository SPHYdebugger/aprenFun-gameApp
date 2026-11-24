package com.example.aprendemoslavida

import com.example.aprendemoslavida.avatar.AvatarLayerCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.DataInputStream
import java.io.File

/** Comprueba el tamaño de las capas leyendo la cabecera PNG (IHDR), sin depender de android.graphics. */
class AvatarLayerAssetsFormatTest {

    private val avatarDir = File("src/main/assets/avatar")

    private fun pngSize(file: File): Pair<Int, Int> {
        DataInputStream(file.inputStream().buffered()).use { input ->
            val signature = ByteArray(8).also { input.readFully(it) }
            assertTrue("${file.name} no es PNG", signature[1] == 'P'.code.toByte() && signature[2] == 'N'.code.toByte())
            input.readInt() // longitud del bloque IHDR
            val type = ByteArray(4).also { input.readFully(it) }
            assertEquals("IHDR", String(type, Charsets.US_ASCII))
            return input.readInt() to input.readInt()
        }
    }

    @Test
    fun `las hojas del modo historia son 4x4 de 192 px`() {
        val sheets = File(avatarDir, "story").listFiles { f -> f.extension == "png" }.orEmpty()
        // 3 cuerpos + 15 prendas + 15 máscaras + 84 pelos (14 peinados × 6 colores) + 11 calzados
        assertEquals(3 + 15 + 15 + 84 + 11, sheets.size)
        for (sheet in sheets) {
            assertEquals("Tamaño de ${sheet.name}", 768 to 768, pngSize(sheet))
        }
    }

    @Test
    fun `las capas del editor son de 512 px`() {
        val catalogLayers = AvatarLayerCatalog.allProfiles()
            .map { AvatarLayerCatalog.editorLayers(it) }
            .flatMap { listOf(it.body, it.shoes, it.outfit, it.outfitMask, it.hair) }
            .distinct()
        assertEquals(3 + 11 + 15 + 15 + 84, catalogLayers.size)
        val allPngs = avatarDir.listFiles { f -> f.isFile && f.extension == "png" }.orEmpty()
        for (layer in allPngs) {
            assertEquals("Tamaño de ${layer.name}", 512 to 512, pngSize(layer))
        }
    }

    @Test
    fun `las miniaturas del calzado son de 128 px`() {
        val thumbs = File(avatarDir, "thumbs").listFiles { f -> f.extension == "png" }.orEmpty()
        assertEquals(11, thumbs.size)
        for (thumb in thumbs) {
            assertEquals("Tamaño de ${thumb.name}", 128 to 128, pngSize(thumb))
        }
    }
}
