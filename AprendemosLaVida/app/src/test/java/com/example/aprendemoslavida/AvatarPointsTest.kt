package com.example.aprendemoslavida

import com.example.aprendemoslavida.avatar.AvatarLayerCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Los desbloqueos del avatar usan los puntos acumulados en el servidor (preguntas + historia + rachas). */
class AvatarPointsTest {

    @Test
    fun `se usan los puntos del servidor aunque el contador local antiguo este a cero`() {
        // El fallo: tras jugar en modo preguntas el editor mostraba 0 porque solo miraba el contador local.
        assertEquals(12000, AvatarLayerCatalog.accumulatedPoints(serverBalance = 12000, legacyLocalPoints = 0))
        assertTrue(AvatarLayerCatalog.isOutfitUnlocked(0, AvatarLayerCatalog.accumulatedPoints(12000, 0)))
    }

    @Test
    fun `los puntos antiguos sin importar cuentan, pero no se suman dos veces`() {
        // Antes de importarlos al servidor, cuentan los puntos locales antiguos.
        assertEquals(8000, AvatarLayerCatalog.accumulatedPoints(serverBalance = 0, legacyLocalPoints = 8000))
        // Una vez importados, el servidor ya los incluye: no se duplican.
        assertEquals(8500, AvatarLayerCatalog.accumulatedPoints(serverBalance = 8500, legacyLocalPoints = 8000))
    }

    @Test
    fun `valores negativos no restan`() {
        assertEquals(0, AvatarLayerCatalog.accumulatedPoints(-5, -3))
    }
}
