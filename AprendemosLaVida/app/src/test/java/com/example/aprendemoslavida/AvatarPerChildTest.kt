package com.example.aprendemoslavida

import com.example.aprendemoslavida.avatar.AvatarProfileStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** Cada niño tiene su propio avatar: la clave de guardado depende del perfil infantil activo. */
class AvatarPerChildTest {

    @Test
    fun `cada nino guarda su avatar con una clave distinta`() {
        val ana = AvatarProfileStore.keyForChild("1111-ana")
        val leo = AvatarProfileStore.keyForChild("2222-leo")
        assertNotEquals(ana, leo)
        assertEquals(ana, AvatarProfileStore.keyForChild("1111-ana"))
    }

    @Test
    fun `sin nino activo se usa la clave del movil (la de antes)`() {
        assertEquals("profile_json", AvatarProfileStore.keyForChild(null))
        assertEquals("profile_json", AvatarProfileStore.keyForChild(""))
        assertNotEquals("profile_json", AvatarProfileStore.keyForChild("1111-ana"))
    }
}
