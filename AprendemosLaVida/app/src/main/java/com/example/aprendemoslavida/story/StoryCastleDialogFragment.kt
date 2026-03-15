package com.example.aprendemoslavida.story

import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputFilter
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import com.example.aprendemoslavida.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.math.max
import kotlin.random.Random

class StoryCastleDialogFragment : DialogFragment() {
    private val resultInputs = mutableListOf<AppCompatEditText>()
    private val topBorrowInputsByColumn = mutableListOf<AppCompatEditText?>()
    private val bottomBorrowHintsByColumn = mutableListOf<TextView?>()
    private val finishHandler = Handler(Looper.getMainLooper())
    private var failedAttempts: Int = 0
    private var resolved: Boolean = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val gateId = requireArguments().getInt(ARG_GATE_ID)
        val dialogTitle = requireArguments().getString(ARG_TITLE).orEmpty()
        val top = requireArguments().getInt(ARG_TOP)
        val bottom = requireArguments().getInt(ARG_BOTTOM)
        val operator = requireArguments().getString(ARG_OPERATOR).orEmpty()
        val result = requireArguments().getInt(ARG_RESULT)
        val columns = requireArguments().getInt(ARG_COLUMNS)

        isCancelable = false
        resultInputs.clear()
        topBorrowInputsByColumn.clear()
        bottomBorrowHintsByColumn.clear()
        failedAttempts = 0
        resolved = false

        val density = requireContext().resources.displayMetrics.density
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((20 * density).toInt(), (8 * density).toInt(), (20 * density).toInt(), (8 * density).toInt())
        }

        val prompt = TextView(requireContext()).apply {
            text = getString(R.string.story_castle_prompt)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            setTextColor(requireContext().getColor(R.color.text_primary))
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, 0, 0, (12 * density).toInt())
        }
        root.addView(prompt)

        val board = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((8 * density).toInt(), (6 * density).toInt(), (8 * density).toInt(), (8 * density).toInt())
        }

        val carryRow = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
        val topRow = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
        val bottomRow = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
        val resultRow = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }

        if (operator == "+") {
            val topString = top.toString()
            val bottomString = bottom.toString()
            val resultString = result.toString()
            val hasExtraResultDigit = resultString.length > max(topString.length, bottomString.length)
            val leadingCarrySlots = (columns - topString.length).coerceAtLeast(0)
            val extraCarryCircles = if (hasExtraResultDigit && leadingCarrySlots > 0) 1 else 0

            addOperatorSpacer(carryRow)
            addLeadingEmptyCells(carryRow, leadingCarrySlots - extraCarryCircles)
            repeat(topString.length + extraCarryCircles) {
                carryRow.addView(createCarryInput())
            }

            addOperatorSpacer(topRow)
            addAlignedNumber(topRow, topString, columns)

            addOperatorCell(bottomRow, "+")
            addAlignedNumber(bottomRow, bottomString, columns)

            addOperatorSpacer(resultRow)
            addLeadingEmptyCells(resultRow, columns - resultString.length)
            repeat(resultString.length) {
                val input = createResultInput()
                resultInputs.add(input)
                resultRow.addView(input)
            }
        } else {
            carryRow.visibility = android.view.View.GONE
            addOperatorSpacer(topRow)
            addOperatorCell(bottomRow, "-")
            addOperatorSpacer(resultRow)

            addAlignedSubtractionRows(topRow, bottomRow, top.toString(), bottom.toString(), columns)
            addAlignedSubtractionResultRow(resultRow, columns)
            bindBorrowHints()
        }

        val separator = android.view.View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (2 * density).toInt().coerceAtLeast(2)
            )
            setBackgroundColor(requireContext().getColor(R.color.text_primary))
        }

        board.addView(carryRow)
        board.addView(topRow)
        board.addView(bottomRow)
        board.addView(separator)
        board.addView(resultRow)

        val horizontalScroll = HorizontalScrollView(requireContext()).apply {
            isHorizontalScrollBarEnabled = true
            isFillViewport = false
            addView(board)
        }
        root.addView(horizontalScroll)

        val checkButton = MaterialButton(requireContext()).apply {
            text = getString(R.string.sumas_torres_check_button)
            setOnClickListener {
                if (resolved) return@setOnClickListener
                onCheck(gateId, result)
            }
        }
        val buttonParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = (12 * density).toInt() }
        root.addView(checkButton, buttonParams)

        val scroll = ScrollView(requireContext()).apply {
            isFillViewport = true
            addView(root)
        }

        val titleView = TextView(requireContext()).apply {
            text = if (dialogTitle.isNotBlank()) dialogTitle else getString(R.string.story_checkpoint_title)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTextColor(requireContext().getColor(R.color.text_primary))
            setPadding((20 * density).toInt(), (16 * density).toInt(), (20 * density).toInt(), (4 * density).toInt())
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setCustomTitle(titleView)
            .setView(scroll)
            .create()
    }

    override fun onDestroyView() {
        finishHandler.removeCallbacksAndMessages(null)
        super.onDestroyView()
    }

    private fun onCheck(gateId: Int, expectedResult: Int) {
        val enteredDigits = resultInputs.map { input ->
            input.text?.toString()?.firstOrNull { it.isDigit() }?.toString().orEmpty()
        }
        if (enteredDigits.all { it.isEmpty() }) {
            Toast.makeText(requireContext(), getString(R.string.sumas_torres_fill_all), Toast.LENGTH_SHORT).show()
            return
        }

        val expectedPadded = expectedResult.toString().padStart(resultInputs.size, '0')
        val hasInvalidEmpty = enteredDigits.withIndex().any { (index, value) ->
            if (value.isNotEmpty()) return@any false
            if (index != 0) return@any true
            val expectedLeft = expectedPadded.getOrNull(index) ?: '0'
            expectedLeft != '0'
        }
        if (hasInvalidEmpty) {
            Toast.makeText(requireContext(), getString(R.string.sumas_torres_fill_all), Toast.LENGTH_SHORT).show()
            return
        }

        val normalized = enteredDigits.map { if (it.isEmpty()) "0" else it }
        val enteredValue = normalized.joinToString("").toIntOrNull()
        if (enteredValue == expectedResult) {
            resolved = true
            val points = (120 - (failedAttempts * 40)).coerceAtLeast(0)
            dismissAllowingStateLoss()
            (activity as? Listener)?.onStoryCastleResolved(gateId, points)
            return
        }

        failedAttempts += 1
        if (failedAttempts < 3) {
            Toast.makeText(
                requireContext(),
                getString(R.string.story_castle_try_again_format, failedAttempts),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        resolved = true
        val padded = expectedResult.toString().padStart(resultInputs.size, '0')
        resultInputs.forEachIndexed { index, input ->
            input.setText(padded.getOrNull(index)?.toString().orEmpty())
            input.setTextColor(requireContext().getColor(R.color.answer_wrong))
            input.isEnabled = false
        }
        Toast.makeText(
            requireContext(),
            getString(R.string.story_castle_failed_no_points),
            Toast.LENGTH_SHORT
        ).show()
        finishHandler.postDelayed({
            dismissAllowingStateLoss()
            (activity as? Listener)?.onStoryCastleFailed(gateId)
        }, 3000L)
    }

    private fun addAlignedNumber(row: LinearLayout, text: String, columns: Int) {
        repeat((columns - text.length).coerceAtLeast(0)) { row.addView(createDigitCell("")) }
        text.forEach { row.addView(createDigitCell(it.toString())) }
    }

    private fun addAlignedSubtractionRows(
        topRow: LinearLayout,
        bottomRow: LinearLayout,
        top: String,
        bottom: String,
        columns: Int
    ) {
        val topPadded = top.padStart(columns, ' ')
        val bottomPadded = bottom.padStart(columns, ' ')

        repeat(columns) { col ->
            val topDigit = topPadded[col]
            if (topDigit == ' ') {
                topBorrowInputsByColumn.add(null)
                topRow.addView(createBorrowPlaceholder())
                topRow.addView(createDigitCell(""))
            } else {
                val topBorrow = createCarryInput()
                topBorrowInputsByColumn.add(topBorrow)
                topRow.addView(topBorrow)
                topRow.addView(createDigitCell(topDigit.toString()))
            }

            val bottomDigit = bottomPadded[col]
            if (bottomDigit == ' ') {
                bottomBorrowHintsByColumn.add(null)
                bottomRow.addView(createBorrowPlaceholder())
                bottomRow.addView(createDigitCell(""))
            } else {
                val bottomHint = createBorrowIndicator()
                bottomBorrowHintsByColumn.add(bottomHint)
                bottomRow.addView(bottomHint)
                bottomRow.addView(createDigitCell(bottomDigit.toString()))
            }
        }
    }

    private fun addAlignedSubtractionResultRow(resultRow: LinearLayout, columns: Int) {
        repeat(columns) {
            resultRow.addView(createBorrowPlaceholder(heightDp = 44))
            val input = createResultInput()
            resultInputs.add(input)
            resultRow.addView(input)
        }
    }

    private fun bindBorrowHints() {
        topBorrowInputsByColumn.forEachIndexed { index, editText ->
            editText?.doAfterTextChanged { editable ->
                val hint = bottomBorrowHintsByColumn.getOrNull(index) ?: return@doAfterTextChanged
                hint.text = if (editable?.toString()?.trim() == "1") "+1" else ""
            }
        }
    }

    private fun addLeadingEmptyCells(row: LinearLayout, count: Int) {
        repeat(count.coerceAtLeast(0)) { row.addView(createDigitCell("")) }
    }

    private fun addOperatorSpacer(row: LinearLayout) {
        row.addView(createOperatorCell(""))
    }

    private fun addOperatorCell(row: LinearLayout, text: String) {
        row.addView(createOperatorCell(text))
    }

    private fun createOperatorCell(text: String): TextView {
        return TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(44))
            gravity = Gravity.CENTER
            this.text = text
            setTextColor(requireContext().getColor(R.color.text_primary))
            textSize = 24f
        }
    }

    private fun createDigitCell(text: String): TextView {
        return TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_tower_cell)
            this.text = text
            setTextColor(requireContext().getColor(R.color.text_primary))
            textSize = 24f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
    }

    private fun createBorrowPlaceholder(heightDp: Int = 36): TextView {
        return TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(heightDp))
            text = ""
        }
    }

    private fun createBorrowIndicator(): TextView {
        return TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(36))
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_tower_carry)
            setTextColor(requireContext().getColor(R.color.answer_wrong))
            textSize = 12f
            text = ""
        }
    }

    private fun createCarryInput(): AppCompatEditText {
        return AppCompatEditText(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(36))
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_tower_carry)
            inputType = InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(InputFilter.LengthFilter(1))
            setTextColor(requireContext().getColor(R.color.text_secondary))
            textSize = 16f
            setPadding(0, 0, 0, 0)
        }
    }

    private fun createResultInput(): AppCompatEditText {
        return AppCompatEditText(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_tower_cell)
            inputType = InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(InputFilter.LengthFilter(1))
            setTextColor(requireContext().getColor(R.color.text_primary))
            textSize = 24f
            setPadding(0, 0, 0, 0)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    interface Listener {
        fun onStoryCastleResolved(gateId: Int, points: Int)
        fun onStoryCastleFailed(gateId: Int)
    }

    companion object {
        const val TAG = "StoryCastleDialog"
        private const val ARG_GATE_ID = "arg_gate_id"
        private const val ARG_TOP = "arg_top"
        private const val ARG_BOTTOM = "arg_bottom"
        private const val ARG_OPERATOR = "arg_operator"
        private const val ARG_RESULT = "arg_result"
        private const val ARG_COLUMNS = "arg_columns"
        private const val ARG_TITLE = "arg_title"

        fun newInstance(gateId: Int, title: String? = null): StoryCastleDialogFragment {
            val op = createRandomOperation()
            return StoryCastleDialogFragment().apply {
                arguments = bundleOf(
                    ARG_GATE_ID to gateId,
                    ARG_TOP to op.top,
                    ARG_BOTTOM to op.bottom,
                    ARG_OPERATOR to op.operator,
                    ARG_RESULT to op.result,
                    ARG_COLUMNS to op.columns,
                    ARG_TITLE to title.orEmpty()
                )
            }
        }

        private fun createRandomOperation(): CastleOperation {
            val topDigits = Random.nextInt(1, 4)
            val bottomDigits = Random.nextInt((topDigits - 1).coerceAtLeast(1), topDigits + 1)
            val isAdd = Random.nextBoolean()
            return if (isAdd) {
                val top = randomNumberWithDigits(topDigits)
                val bottom = randomNumberWithDigits(bottomDigits)
                val result = top + bottom
                CastleOperation(
                    top = top,
                    bottom = bottom,
                    operator = "+",
                    result = result,
                    columns = max(max(top.toString().length, bottom.toString().length), result.toString().length)
                )
            } else {
                val top = randomPositiveNumberWithDigits(topDigits)
                val bottom = randomLowerNumber(top, bottomDigits)
                val result = top - bottom
                CastleOperation(
                    top = top,
                    bottom = bottom,
                    operator = "-",
                    result = result,
                    columns = max(top.toString().length, bottom.toString().length)
                )
            }
        }

        private fun randomNumberWithDigits(digits: Int): Int {
            if (digits <= 1) return Random.nextInt(0, 10)
            val min = pow10(digits - 1)
            val max = pow10(digits) - 1
            return Random.nextInt(min, max + 1)
        }

        private fun randomPositiveNumberWithDigits(digits: Int): Int {
            if (digits <= 1) return Random.nextInt(1, 10)
            val min = pow10(digits - 1)
            val max = pow10(digits) - 1
            return Random.nextInt(min, max + 1)
        }

        private fun randomLowerNumber(top: Int, desiredDigits: Int): Int {
            val maxForDigits = if (desiredDigits <= 1) 9 else pow10(desiredDigits) - 1
            val minForDigits = if (desiredDigits <= 1) 0 else pow10(desiredDigits - 1)
            val maxAllowed = minOf(maxForDigits, top - 1)
            if (maxAllowed >= minForDigits) {
                return Random.nextInt(minForDigits, maxAllowed + 1)
            }
            return Random.nextInt(0, top)
        }

        private fun pow10(exp: Int): Int {
            var value = 1
            repeat(exp) { value *= 10 }
            return value
        }
    }

    private data class CastleOperation(
        val top: Int,
        val bottom: Int,
        val operator: String,
        val result: Int,
        val columns: Int
    )
}
