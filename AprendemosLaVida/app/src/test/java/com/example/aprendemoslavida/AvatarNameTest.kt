package com.example.aprendemoslavida

import com.example.aprendemoslavida.avatar.AvatarLayerCatalog
import com.example.aprendemoslavida.model.AvatarProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AvatarNameTest {

    @Test
    fun `el avatar empieza sin nombre`() {
        assertEquals("", AvatarProfile().name)
    }

    @Test
    fun `el nombre se limpia de espacios y saltos de linea y se recorta`() {
        assertEquals("Lucía", AvatarLayerCatalog.cleanName("  Lucía  "))
        assertEquals("Ana María", AvatarLayerCatalog.cleanName("Ana \n  María"))
        assertEquals("", AvatarLayerCatalog.cleanName("   "))
        val long = AvatarLayerCatalog.cleanName("Maximiliano Alejandro")
        assertTrue(long.length <= AvatarLayerCatalog.NAME_MAX_LENGTH)
        assertEquals(long.trim(), long)
    }

    @Test
    fun `normalizar el perfil limpia el nombre y no toca el resto`() {
        val p = AvatarLayerCatalog.normalize(AvatarProfile(gender = "NINO", outfit = 2, name = "  Leo "))
        assertEquals("Leo", p.name)
        assertEquals(2, p.outfit)
    }

    @Test
    fun `el saludo de Sofia lleva el nombre y existe una version sin nombre en cada idioma`() {
        for (folder in listOf("values", "values-gl")) {
            val xml = File("src/main/res/$folder/strings.xml").readText()
            val withName = Regex("<string name=\"story_intro_text\">(.*?)</string>").find(xml)?.groupValues?.get(1)
            val noName = Regex("<string name=\"story_intro_text_no_name\">(.*?)</string>").find(xml)?.groupValues?.get(1)
            assertTrue("$folder: falta story_intro_text", withName != null)
            assertTrue("$folder: el saludo debe llevar el nombre (%1\$s)", withName!!.contains("%1\$s"))
            assertTrue("$folder: falta story_intro_text_no_name", noName != null && !noName.contains("%1\$s"))
        }
    }

    @Test
    fun `la locucion de Sofia es un unico recurso sofia1`() {
        val raw = File("src/main/res/raw").listFiles().orEmpty().map { it.name }.filter { it.startsWith("sofia1.") }
        assertEquals(listOf("sofia1.wav"), raw)
    }
}
