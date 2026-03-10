package com.example.aprendemoslavida.story

import android.app.Dialog
import android.os.Bundle
import android.util.TypedValue
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

// Reusable modal question UI for blocking story checkpoints.
class StoryQuestionDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val gateId = requireArguments().getInt(ARG_GATE_ID)
        val question = requireArguments().getString(ARG_QUESTION).orEmpty()
        val options = requireArguments().getStringArrayList(ARG_OPTIONS)?.toTypedArray() ?: emptyArray()

        isCancelable = false

        val density = requireContext().resources.displayMetrics.density
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((20 * density).toInt(), (8 * density).toInt(), (20 * density).toInt(), (8 * density).toInt())
        }

        val questionView = TextView(requireContext()).apply {
            text = question
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            setTextColor(requireContext().getColor(com.example.aprendemoslavida.R.color.text_primary))
            setPadding(0, 0, 0, (12 * density).toInt())
        }
        container.addView(questionView)

        options.forEachIndexed { index, option ->
            val button = MaterialButton(requireContext()).apply {
                text = option
                setOnClickListener {
                    dismissAllowingStateLoss()
                    (activity as? Listener)?.onStoryQuestionAnswered(gateId, index)
                }
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (8 * density).toInt()
            }
            container.addView(button, params)
        }

        val scrollView = ScrollView(requireContext()).apply {
            isFillViewport = true
            addView(container)
        }

        val titleView = TextView(requireContext()).apply {
            text = getString(com.example.aprendemoslavida.R.string.story_checkpoint_title)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTextColor(requireContext().getColor(com.example.aprendemoslavida.R.color.text_primary))
            setPadding((20 * density).toInt(), (16 * density).toInt(), (20 * density).toInt(), (4 * density).toInt())
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setCustomTitle(titleView)
            .setView(scrollView)
            .create()
    }

    interface Listener {
        fun onStoryQuestionAnswered(gateId: Int, selectedIndex: Int)
    }

    companion object {
        const val TAG = "StoryQuestionDialog"
        private const val ARG_GATE_ID = "arg_gate_id"
        private const val ARG_QUESTION = "arg_question"
        private const val ARG_OPTIONS = "arg_options"

        fun newInstance(gateId: Int, question: StoryQuestion): StoryQuestionDialogFragment {
            return StoryQuestionDialogFragment().apply {
                arguments = bundleOf(
                    ARG_GATE_ID to gateId,
                    ARG_QUESTION to question.text,
                    ARG_OPTIONS to ArrayList(question.options.take(4))
                )
            }
        }
    }
}
