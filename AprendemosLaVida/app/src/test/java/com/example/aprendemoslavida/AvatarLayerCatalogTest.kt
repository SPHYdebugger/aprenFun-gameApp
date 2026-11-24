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
    fun `todas las combinaciones tienen sus capas en assets`() {
        val profiles = AvatarLayerCatalog.allProfiles()
        // 2 géneros × 7 peinados × 6 colores × 3 pieles × (básica + 7 prendas) × 6 calzados
        assertEquals(2 * 7 * 6 * 3 * 8 * 6, profiles.size)
        val missing = profiles.flatMap { profile ->
            val story = AvatarLayerCatalog.storyLayers(profile)
            val editor = AvatarLayerCatalog.editorLayers(profile)
            listOf(
                story.body, story.shoes, story.outfit, story.outfitMask, story.hair,
                editor.body, editor.shoes, editor.outfit, editor.outfitMask, editor.hair
            ) + (0 until AvatarLayerCatalog.SHOES_COUNT).map { AvatarLayerCatalog.shoesThumb(profile, it) } +
                AvatarLayerCatalog.outfitEditorLayer(profile, profile.outfit)
        }.distinct().filterNot { File(assetsDir, it).isFile }
        assertTrue("Faltan capas en assets: $missing", missing.isEmpty())
    }

    @Test
    fun `el personaje inicial es una nina con ropa y calzado basicos`() {
        val defaults = AvatarProfile()
        assertEquals("NINA", defaults.gender)
        assertEquals(AvatarLayerCatalog.OUTFIT_BASIC, defaults.outfit)
        assertEquals(0, defaults.shoes)
        val layers = AvatarLayerCatalog.editorLayers(defaults)
        assertEquals("avatar/base_body_skin_01.png", layers.body)
        assertEquals("avatar/outfit_basic_01.png", layers.outfit)
        assertEquals("avatar/shoes_basic.png", layers.shoes)
        assertEquals("avatar/hair_girl_01_c0.png", layers.hair)
    }

    @Test
    fun `ropa basica y calzado basico son comunes a nino y nina`() {
        val boy = AvatarLayerCatalog.storyLayers(AvatarProfile(gender = "NINO", outfit = -1, shoes = 0))
        val girl = AvatarLayerCatalog.storyLayers(AvatarProfile(gender = "NINA", outfit = -1, shoes = 0))
        assertEquals(boy.outfit, girl.outfit)
        assertEquals(boy.shoes, girl.shoes)
    }

    @Test
    fun `ropa, pelo y calzado no basicos van separados por genero`() {
        for (profile in AvatarLayerCatalog.allProfiles()) {
            val layers = AvatarLayerCatalog.storyLayers(profile)
            val other = if (AvatarLayerCatalog.isGirl(profile)) "boy_" else "girl_"
            assertFalse(layers.outfit.contains(other))
            assertFalse(layers.hair.contains(other))
            assertFalse(layers.shoes.contains(other))
            assertFalse(layers.body.contains("boy") || layers.body.contains("girl"))
        }
    }

    @Test
    fun `la prenda 5 de chica es girl_05b y la 7 es la de gala`() {
        val girl = AvatarProfile(gender = "NINA", outfit = 4)
        assertEquals("avatar/outfit_girl_05b.png", AvatarLayerCatalog.editorLayers(girl).outfit)
        assertEquals("avatar/story/outfit_girl_07.png", AvatarLayerCatalog.storyLayers(girl.copy(outfit = 6)).outfit)
        assertEquals("avatar/story/outfit_boy_07_mask.png", AvatarLayerCatalog.storyLayers(AvatarProfile(gender = "NINO", outfit = 6)).outfitMask)
    }

    @Test
    fun `normalizacion - basica se conserva, indices dan la vuelta, genero desconocido es nina`() {
        val odd = AvatarLayerCatalog.normalize(AvatarProfile(gender = "X", hairStyle = 4, hairColor = -1, skinTone = 5, outfit = 8, shoes = 7))
        assertEquals("NINA", odd.gender)
        assertEquals(4, odd.hairStyle)
        assertEquals(5, odd.hairColor)
        val wrapped = AvatarLayerCatalog.normalize(AvatarProfile(hairStyle = 9, hairColor = 8))
        assertEquals(2, wrapped.hairStyle)
        assertEquals(2, wrapped.hairColor)
        assertEquals(2, odd.skinTone)
        assertEquals(1, odd.outfit)
        assertEquals(1, odd.shoes)
        assertEquals(AvatarLayerCatalog.OUTFIT_BASIC, AvatarLayerCatalog.normalize(AvatarProfile(outfit = -5)).outfit)
    }

    @Test
    fun `los perfiles guardados antes de este cambio siguen igual`() {
        // Un niño con la prenda 0 (sudadera) guardado antes: no pasa a la ropa básica ni cambia de género.
        val old = AvatarLayerCatalog.normalize(AvatarProfile(gender = "NINO", outfit = 0, shoes = 0))
        assertEquals("NINO", old.gender)
        assertEquals(0, old.outfit)
        assertEquals("avatar/story/outfit_boy_01.png", AvatarLayerCatalog.storyLayers(old).outfit)
    }

    @Test
    fun `desbloqueos por puntos`() {
        assertTrue(AvatarLayerCatalog.isOutfitUnlocked(AvatarLayerCatalog.OUTFIT_BASIC, 0))
        assertFalse(AvatarLayerCatalog.isOutfitUnlocked(0, 9999))
        assertTrue(AvatarLayerCatalog.isOutfitUnlocked(0, 10000))
        assertFalse(AvatarLayerCatalog.isOutfitUnlocked(6, 199999))
        assertTrue(AvatarLayerCatalog.isOutfitUnlocked(6, 200000))
        assertTrue(AvatarLayerCatalog.isShoesUnlocked(0, 0))
        assertFalse(AvatarLayerCatalog.isShoesUnlocked(1, 4999))
        assertTrue(AvatarLayerCatalog.isShoesUnlocked(1, 5000))
        assertTrue(AvatarLayerCatalog.isShoesUnlocked(5, 120000))
        assertFalse(AvatarLayerCatalog.isShoesUnlocked(5, 119999))
    }

    @Test
    fun `la importacion al servidor no envia valores que el contrato no conoce`() {
        val basic = AvatarLayerCatalog.toLegacyImport(AvatarProfile(gender = "NINA", outfit = -1, shoes = 4))
        assertEquals(0, basic.outfit)
        assertEquals("NINA", basic.gender)
        val gala = AvatarLayerCatalog.toLegacyImport(AvatarProfile(gender = "NINO", outfit = 6))
        assertTrue(gala.outfit in 0..5)
        val normal = AvatarLayerCatalog.toLegacyImport(AvatarProfile(gender = "NINO", outfit = 3, skinTone = 2, hairStyle = 1, hairColor = 2))
        assertEquals(3, normal.outfit)
        assertEquals(2, normal.skinTone)
        assertEquals(1, normal.hairStyle)
        assertEquals(2, normal.hairColor)
        // peinados y colores nuevos: el servidor solo conoce 0..2
        val nuevo = AvatarLayerCatalog.toLegacyImport(AvatarProfile(gender = "NINA", hairStyle = 6, hairColor = 5))
        assertTrue(nuevo.hairStyle in 0..2)
        assertTrue(nuevo.hairColor in 0..2)
    }

    @Test
    fun `peinados y colores nuevos tienen sus capas`() {
        val girl = AvatarLayerCatalog.storyLayers(AvatarProfile(gender = "NINA", hairStyle = 6, hairColor = 3))
        assertEquals("avatar/story/hair_girl_07_c3.png", girl.hair)
        val boy = AvatarLayerCatalog.editorLayers(AvatarProfile(gender = "NINO", hairStyle = 3, hairColor = 5))
        assertEquals("avatar/hair_boy_04_c5.png", boy.hair)
    }
}
