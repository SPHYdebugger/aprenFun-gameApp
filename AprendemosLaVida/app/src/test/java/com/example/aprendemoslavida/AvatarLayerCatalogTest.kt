package com.example.aprendemoslavida

import com.example.aprendemoslavida.avatar.AvatarLayerCatalog
import com.example.aprendemoslavida.model.AvatarProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AvatarLayerCatalogTest {

    private val assetsDir = File("src/main/assets")

    @Test
    fun `hay 324 combinaciones y todas sus capas existen en assets`() {
        val profiles = AvatarLayerCatalog.allProfiles()
        assertEquals(2 * 3 * 3 * 3 * 6, profiles.size)
        val missing = profiles.flatMap { profile ->
            val story = AvatarLayerCatalog.storyLayers(profile)
            val editor = AvatarLayerCatalog.editorLayers(profile)
            listOf(story.body, story.outfit, story.outfitMask, story.hair, editor.body, editor.outfit, editor.outfitMask, editor.hair)
        }.distinct().filterNot { File(assetsDir, it).isFile }
        assertTrue("Faltan capas en assets: $missing", missing.isEmpty())
    }

    @Test
    fun `ropa y pelo van separados por genero y el cuerpo es comun`() {
        for (profile in AvatarLayerCatalog.allProfiles()) {
            val layers = AvatarLayerCatalog.storyLayers(profile)
            val other = if (AvatarLayerCatalog.isGirl(profile)) "boy_" else "girl_"
            assertFalse(layers.outfit.contains(other))
            assertFalse(layers.hair.contains(other))
            assertFalse(layers.body.contains("boy") || layers.body.contains("girl"))
        }
    }

    @Test
    fun `la prenda 5 de chica usa outfit_girl_05b como el editor`() {
        val girl = AvatarProfile(gender = "NINA", outfit = 4)
        assertEquals("avatar/outfit_girl_05b.png", AvatarLayerCatalog.editorLayers(girl).outfit)
        assertEquals("avatar/story/outfit_girl_05b_mask.png", AvatarLayerCatalog.storyLayers(girl).outfitMask)
    }

    @Test
    fun `indices fuera de rango dan la vuelta y un genero desconocido es nino`() {
        val odd = AvatarProfile(gender = "X", hairStyle = 4, hairColor = -1, skinTone = 5, outfit = 7)
        val layers = AvatarLayerCatalog.storyLayers(odd)
        assertEquals("avatar/story/body_skin_05.png", layers.body)
        assertEquals("avatar/story/outfit_boy_02.png", layers.outfit)
        assertEquals("avatar/story/hair_boy_02_c2.png", layers.hair)
    }

    @Test
    fun `el perfil por defecto es el piloto aprobado`() {
        val layers = AvatarLayerCatalog.editorLayers(AvatarProfile())
        assertEquals("avatar/base_body_skin_01.png", layers.body)
        assertEquals("avatar/outfit_boy_01.png", layers.outfit)
        assertEquals("avatar/hair_boy_01_c0.png", layers.hair)
    }
}
