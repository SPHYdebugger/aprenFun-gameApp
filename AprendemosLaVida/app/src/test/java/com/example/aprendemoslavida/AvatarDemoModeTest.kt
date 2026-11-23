package com.example.aprendemoslavida

import com.example.aprendemoslavida.avatar.AvatarLayerCatalog
import com.example.aprendemoslavida.model.AvatarProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Al salir del modo demo, lo que no se ha desbloqueado con los puntos reales vuelve a lo básico. */
class AvatarDemoModeTest {

    @Test
    fun `en modo demo todo esta desbloqueado`() {
        val demoPoints = Int.MAX_VALUE
        for (outfit in AvatarLayerCatalog.OUTFIT_BASIC until AvatarLayerCatalog.OUTFIT_COUNT) {
            assertTrue(AvatarLayerCatalog.isOutfitUnlocked(outfit, demoPoints))
        }
        for (shoes in 0 until AvatarLayerCatalog.SHOES_COUNT) {
            assertTrue(AvatarLayerCatalog.isShoesUnlocked(shoes, demoPoints))
        }
    }

    @Test
    fun `al salir de la demo sin puntos vuelve a la ropa y el calzado basicos`() {
        val dressedInDemo = AvatarProfile(gender = "NINA", outfit = 6, shoes = 5, hairStyle = 2, skinTone = 1)
        val after = AvatarLayerCatalog.withinUnlocks(dressedInDemo, globalPoints = 0)
        assertEquals(AvatarLayerCatalog.OUTFIT_BASIC, after.outfit)
        assertEquals(0, after.shoes)
        // el resto del personaje no cambia
        assertEquals("NINA", after.gender)
        assertEquals(2, after.hairStyle)
        assertEquals(1, after.skinTone)
    }

    @Test
    fun `al salir de la demo se conserva lo que ya estaba desbloqueado con puntos`() {
        val profile = AvatarProfile(gender = "NINO", outfit = 1, shoes = 2)
        val after = AvatarLayerCatalog.withinUnlocks(profile, globalPoints = 30000)
        assertEquals(1, after.outfit) // 25.000 puntos
        assertEquals(2, after.shoes) // 15.000 puntos
        val partly = AvatarLayerCatalog.withinUnlocks(AvatarProfile(gender = "NINO", outfit = 5, shoes = 2), globalPoints = 20000)
        assertEquals(AvatarLayerCatalog.OUTFIT_BASIC, partly.outfit) // 150.000 > 20.000
        assertEquals(2, partly.shoes)
    }
}
