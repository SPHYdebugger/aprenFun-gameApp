package com.example.aprendemoslavida.activities

import android.os.Bundle
import android.view.View
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import android.widget.TextView
import com.example.aprendemoslavida.R
import com.example.aprendemoslavida.avatar.AvatarBitmapBuilder
import com.example.aprendemoslavida.avatar.AvatarProfileStore
import com.example.aprendemoslavida.databinding.ActivityAvatarCustomizationBinding
import com.example.aprendemoslavida.model.AvatarProfile
import com.example.aprendemoslavida.utils.SettingsManager

class AvatarCustomizationActivity : BaseActivity() {

    private lateinit var binding: ActivityAvatarCustomizationBinding
    private lateinit var profile: AvatarProfile
    private var isUpdatingGenderSwitch = false
    private val outfitThumbCache = HashMap<String, BitmapDrawable>()
    private var globalPoints: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAvatarCustomizationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        profile = normalizeProfile(AvatarProfileStore.load(this))

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
            val selectedGender = if (isChecked) GENDERS[1] else GENDERS[0]
            if (profile.gender != selectedGender) {
                profile.gender = selectedGender
                profile.outfit = 0
                refreshUi()
            }
        }

        binding.hairStylePrevButton.setOnClickListener {
            profile.hairStyle = wrapIndex(profile.hairStyle - 1, 3)
            refreshUi()
        }

        binding.hairStyleNextButton.setOnClickListener {
            profile.hairStyle = wrapIndex(profile.hairStyle + 1, 3)
            refreshUi()
        }

        binding.hairColorPrevButton.setOnClickListener {
            profile.hairColor = wrapIndex(profile.hairColor - 1, HAIR_COLOR_NAMES.size)
            refreshUi()
        }

        binding.hairColorNextButton.setOnClickListener {
            profile.hairColor = wrapIndex(profile.hairColor + 1, HAIR_COLOR_NAMES.size)
            refreshUi()
        }

        binding.skinTonePrevButton.setOnClickListener {
            profile.skinTone = wrapIndex(profile.skinTone - 1, SKIN_NAMES.size)
            refreshUi()
        }

        binding.skinToneNextButton.setOnClickListener {
            profile.skinTone = wrapIndex(profile.skinTone + 1, SKIN_NAMES.size)
            refreshUi()
        }

        outfitButtons().forEachIndexed { index, button ->
            button.setOnClickListener {
                if (!isOutfitUnlocked(index)) {
                    val required = OUTFIT_UNLOCK_POINTS[index]
                    val missing = (required - globalPoints).coerceAtLeast(0)
                    Toast.makeText(
                        this,
                        getString(
                            R.string.avatar_outfit_locked_points_toast,
                            globalPoints,
                            missing
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                profile.outfit = index
                refreshUi()
            }
        }

        binding.saveButton.setOnClickListener {
            AvatarProfileStore.save(this, profile)
            Toast.makeText(this, getString(R.string.avatar_saved_status), Toast.LENGTH_SHORT).show()
        }

        binding.backButton.setOnClickListener { finish() }
    }

    private fun refreshUi() {
        profile = normalizeProfile(profile)
        globalPoints = SettingsManager.getGlobalPoints(this)

        val hairStyles = if (isGirl(profile)) HAIR_STYLE_GIRL else HAIR_STYLE_BOY

        binding.hairStyleValueText.text = hairStyles[profile.hairStyle]
        binding.hairColorValueText.text = HAIR_COLOR_NAMES[profile.hairColor]
        binding.skinToneValueText.text = SKIN_NAMES[profile.skinTone]

        setOutfitThumbnails()

        isUpdatingGenderSwitch = true
        binding.genderSwitch.isChecked = isGirl(profile)
        binding.genderSwitch.text = if (isGirl(profile)) {
            getString(R.string.avatar_gender_girl)
        } else {
            getString(R.string.avatar_gender_boy)
        }
        isUpdatingGenderSwitch = false
        renderPreview()
    }

    private fun setOutfitThumbnails() {
        val paths = if (isGirl(profile)) OUTFIT_GIRL_PATHS else OUTFIT_BOY_PATHS
        val buttons = outfitButtons()
        val lockLabels = outfitLockLabels()
        for (index in paths.indices) {
            setOutfitThumb(
                button = buttons[index],
                lockLabel = lockLabels[index],
                assetPath = paths[index],
                selected = profile.outfit == index,
                unlocked = isOutfitUnlocked(index),
                requiredPoints = OUTFIT_UNLOCK_POINTS[index]
            )
        }
    }

    private fun setOutfitThumb(
        button: AppCompatImageButton,
        lockLabel: TextView,
        assetPath: String,
        selected: Boolean,
        unlocked: Boolean,
        requiredPoints: Int
    ) {
        button.setImageDrawable(loadOutfitThumb(assetPath))
        button.isEnabled = true
        button.alpha = if (unlocked) 1f else 0.45f
        button.background = GradientDrawable().apply {
            cornerRadius = dp(10).toFloat()
            setColor(
                when {
                    !unlocked -> 0xFFE8E8E8.toInt()
                    selected -> 0xFFFFE8C4.toInt()
                    else -> 0xFFF6F1E8.toInt()
                }
            )
            setStroke(
                if (selected) dp(2) else dp(1),
                if (selected && unlocked) ContextCompat.getColor(this@AvatarCustomizationActivity, R.color.primary) else 0xFFD0C3AF.toInt()
            )
        }

        if (unlocked) {
            lockLabel.visibility = View.INVISIBLE
        } else {
            lockLabel.visibility = View.VISIBLE
            lockLabel.text = getString(R.string.avatar_outfit_lock_format, requiredPoints)
        }
    }

    private fun loadOutfitThumb(assetPath: String): BitmapDrawable {
        return outfitThumbCache.getOrPut(assetPath) {
            val bitmap = assets.open(assetPath).use { stream ->
                BitmapFactory.decodeStream(stream)
                    ?: throw IllegalStateException("No se pudo decodificar $assetPath")
            }
            val thumbnail = android.graphics.Bitmap.createScaledBitmap(bitmap, dp(36), dp(36), true)
            BitmapDrawable(resources, thumbnail)
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun outfitButtons(): List<AppCompatImageButton> = listOf(
        binding.outfitButton0,
        binding.outfitButton1,
        binding.outfitButton2,
        binding.outfitButton3,
        binding.outfitButton4,
        binding.outfitButton5
    )

    private fun outfitLockLabels(): List<TextView> = listOf(
        binding.outfitLockText0,
        binding.outfitLockText1,
        binding.outfitLockText2,
        binding.outfitLockText3,
        binding.outfitLockText4,
        binding.outfitLockText5
    )

    private fun isOutfitUnlocked(index: Int): Boolean {
        val required = OUTFIT_UNLOCK_POINTS.getOrElse(index) { Int.MAX_VALUE }
        return globalPoints >= required
    }

    private fun renderPreview() {
        val bitmap = AvatarBitmapBuilder.buildAvatarBitmap(profile, this)
        binding.avatarPreviewImage.setImageBitmap(bitmap)
    }

    private fun normalizeProfile(input: AvatarProfile): AvatarProfile {
        val normalized = input.copy()
        if (normalized.gender != GENDERS[0] && normalized.gender != GENDERS[1]) {
            normalized.gender = GENDERS[0]
        }
        normalized.hairStyle = wrapIndex(normalized.hairStyle, 3)
        normalized.hairColor = wrapIndex(normalized.hairColor, HAIR_COLOR_NAMES.size)
        normalized.eyeShape = wrapIndex(normalized.eyeShape, 3)
        normalized.eyeColor = wrapIndex(normalized.eyeColor, 4)
        normalized.mouthShape = wrapIndex(normalized.mouthShape, 3)
        normalized.mouthColor = wrapIndex(normalized.mouthColor, 3)
        normalized.skinTone = wrapIndex(normalized.skinTone, SKIN_NAMES.size)
        normalized.outfit = wrapIndex(normalized.outfit, OUTFIT_BOY_PATHS.size)
        return normalized
    }

    private fun isGirl(profile: AvatarProfile): Boolean = profile.gender == GENDERS[1]

    private fun wrapIndex(value: Int, length: Int): Int {
        val m = value % length
        return if (m < 0) m + length else m
    }

    companion object {
        private val GENDERS = arrayOf("NINO", "NINA")

        private val HAIR_STYLE_BOY = arrayOf("Corto", "Desordenado", "Flequillo")
        private val HAIR_STYLE_GIRL = arrayOf("Largo", "Coletas", "Bob")

        private val HAIR_COLOR_NAMES = arrayOf("Negro", "Castano", "Rubio")
        private val SKIN_NAMES = arrayOf("Tono 1", "Tono 2", "Tono 3", "Tono 4", "Tono 5")
        private val OUTFIT_UNLOCK_POINTS = intArrayOf(10000, 25000, 50000, 80000, 100000, 150000)

        private val OUTFIT_BOY_PATHS = arrayOf(
            "avatar/outfit_boy_01.png",
            "avatar/outfit_boy_02.png",
            "avatar/outfit_boy_03.png",
            "avatar/outfit_boy_04.png",
            "avatar/outfit_boy_05.png",
            "avatar/outfit_boy_06.png"
        )
        private val OUTFIT_GIRL_PATHS = arrayOf(
            "avatar/outfit_girl_01.png",
            "avatar/outfit_girl_02.png",
            "avatar/outfit_girl_03.png",
            "avatar/outfit_girl_04.png",
            "avatar/outfit_girl_05b.png",
            "avatar/outfit_girl_06.png"
        )
    }
}
