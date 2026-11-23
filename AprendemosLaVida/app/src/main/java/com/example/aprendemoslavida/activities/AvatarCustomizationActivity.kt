package com.example.aprendemoslavida.activities

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import com.example.aprendemoslavida.R
import com.example.aprendemoslavida.avatar.AvatarBitmapBuilder
import com.example.aprendemoslavida.avatar.AvatarLayerCatalog
import com.example.aprendemoslavida.avatar.AvatarLayerCompositor
import com.example.aprendemoslavida.avatar.AvatarProfileStore
import com.example.aprendemoslavida.databinding.ActivityAvatarCustomizationBinding
import com.example.aprendemoslavida.model.AvatarProfile
import com.example.aprendemoslavida.utils.SettingsManager

class AvatarCustomizationActivity : BaseActivity() {

    private lateinit var binding: ActivityAvatarCustomizationBinding
    private lateinit var profile: AvatarProfile
    private var isUpdatingGenderSwitch = false
    private val thumbCache = HashMap<String, BitmapDrawable>()
    private var globalPoints: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAvatarCustomizationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        profile = AvatarLayerCatalog.normalize(AvatarProfileStore.load(this))
        binding.avatarNameInput.setText(profile.name)

        setupListeners()
        refreshUi()
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    private fun setupListeners() {
        binding.genderSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingGenderSwitch) return@setOnCheckedChangeListener
            val selectedGender = if (isChecked) AvatarLayerCatalog.GENDER_GIRL else AvatarLayerCatalog.GENDER_BOY
            if (profile.gender != selectedGender) {
                profile.gender = selectedGender
                // La ropa y el calzado de cada género son distintos: se vuelve a los básicos, comunes a los dos.
                profile.outfit = AvatarLayerCatalog.OUTFIT_BASIC
                profile.shoes = 0
                refreshUi()
            }
        }

        binding.hairStylePrevButton.setOnClickListener {
            profile.hairStyle -= 1
            refreshUi()
        }

        binding.hairStyleNextButton.setOnClickListener {
            profile.hairStyle += 1
            refreshUi()
        }

        binding.hairColorPrevButton.setOnClickListener {
            profile.hairColor -= 1
            refreshUi()
        }

        binding.hairColorNextButton.setOnClickListener {
            profile.hairColor += 1
            refreshUi()
        }

        binding.skinTonePrevButton.setOnClickListener {
            profile.skinTone -= 1
            refreshUi()
        }

        binding.skinToneNextButton.setOnClickListener {
            profile.skinTone += 1
            refreshUi()
        }

        binding.saveButton.setOnClickListener {
            profile.name = AvatarLayerCatalog.cleanName(binding.avatarNameInput.text?.toString().orEmpty())
            binding.avatarNameInput.setText(profile.name)
            AvatarProfileStore.save(this, profile)
            Toast.makeText(this, getString(R.string.avatar_saved_status), Toast.LENGTH_SHORT).show()
        }

        binding.backButton.setOnClickListener { finish() }
    }

    private fun refreshUi() {
        profile = AvatarLayerCatalog.normalize(profile)
        // En modo demo todo aparece desbloqueado; los puntos guardados del usuario no cambian.
        globalPoints = SettingsManager.getUnlockPoints(this)

        val hairStyles = if (AvatarLayerCatalog.isGirl(profile)) HAIR_STYLE_GIRL else HAIR_STYLE_BOY

        binding.hairStyleValueText.text = hairStyles[profile.hairStyle]
        binding.hairColorValueText.text = HAIR_COLOR_NAMES[profile.hairColor]
        binding.skinToneValueText.text = SKIN_NAMES[profile.skinTone]

        buildOutfitRow()
        buildShoesRow()

        isUpdatingGenderSwitch = true
        binding.genderSwitch.isChecked = AvatarLayerCatalog.isGirl(profile)
        binding.genderSwitch.text = if (AvatarLayerCatalog.isGirl(profile)) {
            getString(R.string.avatar_gender_girl)
        } else {
            getString(R.string.avatar_gender_boy)
        }
        isUpdatingGenderSwitch = false
        renderPreview()
    }

    /** Ropa: «Básica» (siempre disponible) + las prendas del género, la última de gala. */
    private fun buildOutfitRow() {
        val row = binding.outfitRow
        row.removeAllViews()
        val outfits = listOf(AvatarLayerCatalog.OUTFIT_BASIC) + (0 until AvatarLayerCatalog.OUTFIT_COUNT).toList()
        for (outfit in outfits) {
            val required = if (outfit == AvatarLayerCatalog.OUTFIT_BASIC) 0 else AvatarLayerCatalog.OUTFIT_UNLOCK_POINTS[outfit]
            row.addView(
                itemView(
                    thumb = loadThumb(AvatarLayerCatalog.outfitEditorLayer(profile, outfit), cropToContent = true),
                    description = outfitDescription(outfit),
                    selected = profile.outfit == outfit,
                    unlocked = AvatarLayerCatalog.isOutfitUnlocked(outfit, globalPoints),
                    requiredPoints = required
                ) {
                    if (!AvatarLayerCatalog.isOutfitUnlocked(outfit, globalPoints)) {
                        showLockedToast(R.string.avatar_outfit_locked_points_toast, required)
                    } else {
                        profile.outfit = outfit
                        refreshUi()
                    }
                }
            )
        }
    }

    /** Calzado: el básico (siempre disponible) + los del género, con candado por puntos. */
    private fun buildShoesRow() {
        val row = binding.shoesRow
        row.removeAllViews()
        for (shoes in 0 until AvatarLayerCatalog.SHOES_COUNT) {
            val required = AvatarLayerCatalog.SHOES_UNLOCK_POINTS[shoes]
            row.addView(
                itemView(
                    thumb = loadThumb(AvatarLayerCatalog.shoesThumb(profile, shoes), cropToContent = false),
                    description = if (shoes == 0) getString(R.string.avatar_shoes_basic_cd) else getString(R.string.avatar_shoes_cd_format, shoes),
                    selected = profile.shoes == shoes,
                    unlocked = AvatarLayerCatalog.isShoesUnlocked(shoes, globalPoints),
                    requiredPoints = required
                ) {
                    if (!AvatarLayerCatalog.isShoesUnlocked(shoes, globalPoints)) {
                        showLockedToast(R.string.avatar_shoes_locked_points_toast, required)
                    } else {
                        profile.shoes = shoes
                        refreshUi()
                    }
                }
            )
        }
    }

    private fun outfitDescription(outfit: Int): String = when (outfit) {
        AvatarLayerCatalog.OUTFIT_BASIC -> getString(R.string.avatar_outfit_basic_cd)
        AvatarLayerCatalog.OUTFIT_COUNT - 1 -> getString(R.string.avatar_outfit_gala_cd)
        else -> getString(OUTFIT_DESCRIPTIONS[outfit])
    }

    private fun showLockedToast(messageRes: Int, required: Int) {
        val missing = (required - globalPoints).coerceAtLeast(0)
        Toast.makeText(this, getString(messageRes, globalPoints, missing), Toast.LENGTH_SHORT).show()
    }

    /** Botón de prenda o calzado: candado con los puntos necesarios encima y miniatura debajo. */
    private fun itemView(
        thumb: BitmapDrawable,
        description: String,
        selected: Boolean,
        unlocked: Boolean,
        requiredPoints: Int,
        onClick: () -> Unit
    ): View {
        // Lo que se lleva puesto se ve como seleccionado aunque aún no esté desbloqueado (perfiles antiguos).
        val looksUnlocked = unlocked || selected
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(dp(64), LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginEnd = dp(6)
            }
        }
        val lockLabel = TextView(this).apply {
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(this@AvatarCustomizationActivity, R.color.text_primary))
            textSize = 11f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            text = if (unlocked) "" else getString(R.string.avatar_outfit_lock_format, requiredPoints)
            visibility = if (unlocked) View.INVISIBLE else View.VISIBLE
        }
        val button = AppCompatImageButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56))
            setImageDrawable(thumb)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(dp(6), dp(6), dp(6), dp(6))
            contentDescription = description
            alpha = if (looksUnlocked) 1f else 0.45f
            background = GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(
                    when {
                        selected -> 0xFFFFE8C4.toInt()
                        !unlocked -> 0xFFE8E8E8.toInt()
                        else -> 0xFFF6F1E8.toInt()
                    }
                )
                setStroke(
                    if (selected) dp(2) else dp(1),
                    if (selected) ContextCompat.getColor(this@AvatarCustomizationActivity, R.color.primary) else 0xFFD0C3AF.toInt()
                )
            }
            setOnClickListener { onClick() }
        }
        container.addView(lockLabel)
        container.addView(button)
        return container
    }

    /** Miniatura de 40 dp. Las capas de ropa (512) se recortan a su contenido para que la prenda se vea grande. */
    private fun loadThumb(assetPath: String, cropToContent: Boolean): BitmapDrawable {
        return thumbCache.getOrPut("$assetPath|$cropToContent") {
            val source = AvatarLayerCompositor.loadAsset(this, assetPath)
            val cropped = if (cropToContent) cropToOpaque(source) else source
            val size = dp(40)
            BitmapDrawable(resources, Bitmap.createScaledBitmap(cropped, size, size, true))
        }
    }

    private fun cropToOpaque(source: Bitmap): Bitmap {
        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h).also { source.getPixels(it, 0, w, 0, 0, w, h) }
        var minX = w
        var minY = h
        var maxX = -1
        var maxY = -1
        for (y in 0 until h) {
            for (x in 0 until w) {
                if (((pixels[y * w + x] ushr 24) and 0xFF) < 64) continue
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
        }
        if (maxX < minX) return source
        val side = maxOf(maxX - minX, maxY - minY) + 1
        val left = ((minX + maxX) / 2 - side / 2).coerceIn(0, maxOf(0, w - side))
        val top = ((minY + maxY) / 2 - side / 2).coerceIn(0, maxOf(0, h - side))
        return Bitmap.createBitmap(source, left, top, minOf(side, w - left), minOf(side, h - top))
    }

    private fun renderPreview() {
        val bitmap = AvatarBitmapBuilder.buildAvatarBitmap(profile, this)
        binding.avatarPreviewImage.setImageBitmap(bitmap)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        private val HAIR_STYLE_BOY = arrayOf("Corto", "Desordenado", "Flequillo")
        private val HAIR_STYLE_GIRL = arrayOf("Largo", "Coletas", "Bob")

        private val HAIR_COLOR_NAMES = arrayOf("Negro", "Castano", "Rubio")
        private val SKIN_NAMES = arrayOf("Tono 1", "Tono 2", "Tono 3")

        private val OUTFIT_DESCRIPTIONS = intArrayOf(
            R.string.avatar_outfit_1_cd,
            R.string.avatar_outfit_2_cd,
            R.string.avatar_outfit_3_cd,
            R.string.avatar_outfit_4_cd,
            R.string.avatar_outfit_5_cd,
            R.string.avatar_outfit_6_cd
        )
    }
}
