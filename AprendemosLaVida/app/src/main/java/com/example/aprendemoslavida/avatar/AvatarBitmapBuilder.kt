package com.example.aprendemoslavida.avatar

import android.content.Context
import android.graphics.Bitmap
import com.example.aprendemoslavida.model.AvatarProfile

/**
 * Imagen del avatar para el editor (512x512): las mismas capas que el modo historia, en su frame de reposo de frente,
 * para que el personaje que se elige sea exactamente el que luego camina.
 */
object AvatarBitmapBuilder {
    fun buildAvatarBitmap(profileInput: AvatarProfile, context: Context): Bitmap {
        return AvatarLayerCompositor.compose(
            context,
            AvatarLayerCatalog.editorLayers(profileInput),
            rows = 1
        )
    }
}
