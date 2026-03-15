package com.example.aprendemoslavida.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import com.example.aprendemoslavida.R
import com.example.aprendemoslavida.databinding.ActivityStoryStreakBinding
import com.example.aprendemoslavida.utils.SettingsManager

class StoryStreakActivity : BaseActivity() {
    private lateinit var binding: ActivityStoryStreakBinding
    private val handler = Handler(Looper.getMainLooper())
    private var typewriterRunnable: Runnable? = null
    private var fullText: String = ""
    private var onTypewriterFinished: (() -> Unit)? = null
    private var canContinue: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoryStreakBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val state = SettingsManager.consumeStoryStreakLaunch(this)

        binding.confettiView.start()
        binding.titleText.startAnimation(AnimationUtils.loadAnimation(this, R.anim.streak_pulse))
        binding.streakIcon.startAnimation(AnimationUtils.loadAnimation(this, R.anim.streak_icon))
        binding.iconGlow.startAnimation(AnimationUtils.loadAnimation(this, R.anim.streak_blink))
        binding.sparkleLeft.startAnimation(AnimationUtils.loadAnimation(this, R.anim.streak_blink))
        binding.sparkleCenter.startAnimation(AnimationUtils.loadAnimation(this, R.anim.streak_float))
        binding.sparkleRight.startAnimation(AnimationUtils.loadAnimation(this, R.anim.streak_blink))
        binding.tapToContinueText.visibility = View.INVISIBLE
        binding.tapToContinueText.text = getString(R.string.story_streak_tap_continue)

        val message = buildMessage(state)
        startTypewriter(message) {
            canContinue = true
            binding.tapToContinueText.visibility = View.VISIBLE
            binding.tapToContinueText.startAnimation(AnimationUtils.loadAnimation(this, R.anim.streak_blink))
        }

        binding.root.setOnClickListener {
            if (!canContinue) {
                completeTypewriterNow()
                return@setOnClickListener
            }
            openStoryGame()
        }
    }

    override fun onDestroy() {
        cancelTypewriter()
        binding.confettiView.stop()
        super.onDestroy()
    }

    private fun buildMessage(state: SettingsManager.StoryStreakState): String {
        val rewardPart = getString(R.string.story_streak_rewards_info)
        if (state.isFirstTime) {
            return getString(R.string.story_streak_first_time_format, rewardPart)
        }

        val progressLine = if (state.nextRewardDays != null) {
            getString(
                R.string.story_streak_progress_format,
                state.streakDays,
                state.daysToNextReward,
                state.nextRewardDays
            )
        } else {
            getString(R.string.story_streak_progress_max_format, state.streakDays)
        }

        val bonusLine = if (state.bonusForToday > 0) {
            getString(R.string.story_streak_today_bonus_format, state.bonusForToday)
        } else {
            getString(R.string.story_streak_today_no_bonus)
        }

        return getString(
            R.string.story_streak_daily_format,
            progressLine,
            bonusLine,
            rewardPart
        )
    }

    private fun openStoryGame() {
        val intent = Intent(this, StoryGameActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun startTypewriter(text: String, onFinished: (() -> Unit)? = null) {
        cancelTypewriter()
        fullText = text
        onTypewriterFinished = onFinished
        binding.bodyText.text = ""
        var index = 0
        val runnable = object : Runnable {
            override fun run() {
                if (index >= text.length) {
                    typewriterRunnable = null
                    onTypewriterFinished?.invoke()
                    onTypewriterFinished = null
                    return
                }
                index += 1
                binding.bodyText.text = text.substring(0, index)
                handler.postDelayed(this, 28L)
            }
        }
        typewriterRunnable = runnable
        handler.post(runnable)
    }

    private fun completeTypewriterNow() {
        cancelTypewriter()
        binding.bodyText.text = fullText
        canContinue = true
        if (binding.tapToContinueText.visibility != View.VISIBLE) {
            binding.tapToContinueText.visibility = View.VISIBLE
            binding.tapToContinueText.startAnimation(AnimationUtils.loadAnimation(this, R.anim.streak_blink))
        }
    }

    private fun cancelTypewriter() {
        typewriterRunnable?.let { handler.removeCallbacks(it) }
        typewriterRunnable = null
        onTypewriterFinished = null
    }
}
