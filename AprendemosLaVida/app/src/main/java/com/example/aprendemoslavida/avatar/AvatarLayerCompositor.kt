package com.example.aprendemoslavida.avatar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint

/**
 * Apila las capas del avatar: cuerpo -> recorte con la máscara de la ropa -> sin piel por encima del pelo
 * -> calzado -> ropa -> pelo. El calzado va debajo de la ropa para que el bajo del pantalón caiga encima.
 *
 * La lógica de píxeles está en [LayerPixels] (Kotlin puro, probado en JVM); aquí solo se cargan y dibujan Bitmaps.
 */
object AvatarLayerCompositor {
    private const val CACHE_SIZE = 10
    private val bitmapCache = object : LinkedHashMap<String, Bitmap>(CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean = size > CACHE_SIZE
    }

    /**
     * Monta las capas en un Bitmap nuevo del tamaño de la capa de cuerpo.
     * @param rows filas de frames de la imagen (4 en las hojas del modo historia, 1 en las capas del editor):
     *   el recorte de la piel por encima del pelo se hace por tile para no mezclar frames.
     */
    fun compose(context: Context, layers: AvatarLayerCatalog.Layers, rows: Int): Bitmap {
        val body = load(context, layers.body)
        val mask = load(context, layers.outfitMask)
        val shoes = load(context, layers.shoes)
        val outfit = load(context, layers.outfit)
        val hair = load(context, layers.hair)
        val width = body.width
        val height = body.height
        for (layer in listOf(mask, shoes, outfit, hair)) {
            require(layer.width == width && layer.height == height) {
                "Las capas del avatar deben tener el mismo tamaño: $layers"
            }
        }

        val bodyPixels = IntArray(width * height).also { body.getPixels(it, 0, width, 0, 0, width, height) }
        val maskPixels = IntArray(width * height).also { mask.getPixels(it, 0, width, 0, 0, width, height) }
        val hairPixels = IntArray(width * height).also { hair.getPixels(it, 0, width, 0, 0, width, height) }
        LayerPixels.applyMask(bodyPixels, maskPixels)
        LayerPixels.hideAboveHair(bodyPixels, hairPixels, width, height, rows)

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(bodyPixels, 0, width, 0, 0, width, height)
        val canvas = Canvas(result)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(shoes, 0f, 0f, paint)
        canvas.drawBitmap(outfit, 0f, 0f, paint)
        canvas.drawBitmap(hair, 0f, 0f, paint)
        return result
    }

    /** Carga (con caché) un PNG de assets. */
    fun loadAsset(context: Context, assetPath: String): Bitmap = load(context, assetPath)

    @Synchronized
    private fun load(context: Context, assetPath: String): Bitmap {
        bitmapCache[assetPath]?.let { return it }
        val options = BitmapFactory.Options().apply {
            inScaled = false
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = context.assets.open(assetPath).use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
                ?: throw IllegalStateException("No se pudo decodificar $assetPath")
        }
        bitmapCache[assetPath] = bitmap
        return bitmap
    }
}

/** Operaciones de píxel sobre ARGB sin premultiplicar (como devuelve Bitmap.getPixels). Kotlin puro. */
object LayerPixels {
    /** Alfa mínimo para considerar que un píxel es pelo. */
    const val HAIR_ALPHA_MIN = 96

    /** Margen (px) que se respeta por debajo del borde superior del pelo. */
    const val HAIR_MARGIN = 2

    /** pixels.alpha *= mask.alpha / 255 (equivale a PorterDuff DST_IN). */
    fun applyMask(pixels: IntArray, mask: IntArray) {
        require(pixels.size == mask.size)
        for (i in pixels.indices) {
            val a = (pixels[i] ushr 24) and 0xFF
            if (a == 0) continue
            val m = (mask[i] ushr 24) and 0xFF
            val na = a * m / 255
            pixels[i] = (na shl 24) or (pixels[i] and 0x00FFFFFF)
        }
    }

    /**
     * En cada columna con pelo, borra la piel que queda por encima del pelo (evita que asome la calva).
     * La imagen se trata como [rows] filas de tiles apiladas; cada fila se procesa por separado.
     */
    fun hideAboveHair(body: IntArray, hair: IntArray, width: Int, height: Int, rows: Int = 1, margin: Int = HAIR_MARGIN) {
        require(body.size == width * height && hair.size == body.size && rows >= 1)
        val tileHeight = height / rows
        for (row in 0 until rows) {
            val yStart = row * tileHeight
            val yEnd = yStart + tileHeight
            for (x in 0 until width) {
                var top = -1
                for (y in yStart until yEnd) {
                    if (((hair[y * width + x] ushr 24) and 0xFF) >= HAIR_ALPHA_MIN) {
                        top = y
                        break
                    }
                }
                if (top < 0) continue
                val clearUntil = minOf(top + margin, yEnd)
                for (y in yStart until clearUntil) {
                    body[y * width + x] = 0
                }
            }
        }
    }
}
