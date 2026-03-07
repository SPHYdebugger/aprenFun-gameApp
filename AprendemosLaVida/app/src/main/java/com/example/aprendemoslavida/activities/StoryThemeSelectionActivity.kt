package com.example.aprendemoslavida.activities

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.example.aprendemoslavida.R
import com.example.aprendemoslavida.databinding.ActivityStoryThemeSelectionBinding
import com.example.aprendemoslavida.story.StoryTopic
import com.example.aprendemoslavida.utils.SettingsManager

class StoryThemeSelectionActivity : BaseActivity() {
    private lateinit var binding: ActivityStoryThemeSelectionBinding
    private lateinit var allSpinners: List<Spinner>
    private val selectableTopics = listOf(
        StoryTopic.MATH_ADD_SUB,
        StoryTopic.MATH_MULTIPLICATION,
        StoryTopic.NATURAL,
        StoryTopic.SOCIAL,
        StoryTopic.ENGLISH
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoryThemeSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val labels = listOf(
            getString(R.string.story_subject_math_add_sub),
            getString(R.string.story_subject_math_multiplication),
            getString(R.string.story_subject_natural),
            getString(R.string.story_subject_social),
            getString(R.string.story_subject_english)
        )

        allSpinners = listOf(
            binding.trophy1Spinner,
            binding.trophy2Spinner,
            binding.trophy3Spinner,
            binding.trophy4Spinner,
            binding.trophy5Spinner,
            binding.trophy6Spinner,
            binding.trophy7Spinner,
            binding.trophy8Spinner,
            binding.trophy9Spinner,
            binding.trophy10Spinner
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        allSpinners.forEach { spinner ->
            spinner.adapter = adapter
        }

        applySelection(SettingsManager.getStoryGateTopics(this))

        allSpinners.forEach { spinner ->
            spinner.setOnItemSelectedListenerSimple {
                SettingsManager.setStoryGateTopics(this, collectSelection())
            }
        }

        binding.restoreRandomButton.setOnClickListener {
            val randomTopics = SettingsManager.restoreRandomStoryGateTopics(this)
            applySelection(randomTopics)
        }
        binding.backButton.setOnClickListener { finish() }
    }

    private fun applySelection(topics: List<StoryTopic>) {
        allSpinners.forEachIndexed { index, spinner ->
            val topic = topics.getOrNull(index) ?: StoryTopic.NATURAL
            val selection = selectableTopics.indexOf(topic).coerceAtLeast(0)
            spinner.setSelection(selection, false)
        }
    }

    private fun collectSelection(): List<StoryTopic> {
        return allSpinners.map { spinner ->
            selectableTopics.getOrElse(spinner.selectedItemPosition) { StoryTopic.NATURAL }
        }
    }
}

private fun Spinner.setOnItemSelectedListenerSimple(onSelected: () -> Unit) {
    onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
        override fun onItemSelected(
            parent: android.widget.AdapterView<*>?,
            view: android.view.View?,
            position: Int,
            id: Long
        ) {
            onSelected()
        }

        override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
    }
}
