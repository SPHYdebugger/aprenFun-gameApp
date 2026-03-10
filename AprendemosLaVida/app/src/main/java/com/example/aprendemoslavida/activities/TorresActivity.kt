package com.example.aprendemoslavida.activities

import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.widget.doAfterTextChanged
import com.example.aprendemoslavida.R
import com.example.aprendemoslavida.databinding.ActivitySumasTorresBinding
import com.example.aprendemoslavida.utils.ScoreManager
import kotlin.math.max
import kotlin.random.Random

class TorresActivity : BaseActivity() {
    private lateinit var binding: ActivitySumasTorresBinding

    private val resultInputs = mutableListOf<AppCompatEditText>()
    private val topBorrowInputsByColumn = mutableListOf<AppCompatEditText?>()
    private val bottomBorrowHintsByColumn = mutableListOf<TextView?>()
    private var castlePoints: Int = START_POINTS
    private var totalPoints: Int = 0
    private var castlesSolved: Int = 0
    private lateinit var currentOperation: TowerOperation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySumasTorresBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.checkButton.setOnClickListener { checkResult() }
        binding.backButton.setOnClickListener { goToResults() }

        startNewCastle()
    }

    private fun startNewCastle() {
        castlePoints = START_POINTS
        currentOperation = createOperation()
        renderOperation(currentOperation)
        updateScoreTexts()
    }

    private fun createOperation(): TowerOperation {
        val topDigits = Random.nextInt(1, 5)
        val minBottomDigits = (topDigits - 1).coerceAtLeast(1)
        val bottomDigits = Random.nextInt(minBottomDigits, topDigits + 1)
        val operation = if (Random.nextBoolean()) OperationType.ADD else OperationType.SUB

        return if (operation == OperationType.ADD) {
            // Once length is chosen (equal or one less), generate the lower number fully at random.
            val top = randomNumberWithDigits(topDigits)
            val bottom = randomNumberWithDigits(bottomDigits)
            val result = top + bottom
            val columns = max(max(topDigits, bottomDigits), result.toString().length)
            TowerOperation(top, bottom, result, columns, operation)
        } else {
            val top = randomPositiveNumberWithDigits(topDigits)
            val bottom = randomLowerNumber(top, bottomDigits)
            val result = top - bottom
            val columns = max(top.toString().length, bottom.toString().length)
            TowerOperation(top, bottom, result, columns, operation)
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

    private fun renderOperation(operation: TowerOperation) {
        binding.carryRow.removeAllViews()
        binding.topRow.removeAllViews()
        binding.bottomRow.removeAllViews()
        binding.resultRow.removeAllViews()
        resultInputs.clear()
        topBorrowInputsByColumn.clear()
        bottomBorrowHintsByColumn.clear()

        val topString = operation.top.toString()
        val bottomString = operation.bottom.toString()
        val resultString = operation.result.toString()
        if (operation.type == OperationType.ADD) {
            binding.carryRow.visibility = android.view.View.VISIBLE
            val hasExtraResultDigit = resultString.length > max(topString.length, bottomString.length)
            val leadingCarrySlots = (operation.columns - topString.length).coerceAtLeast(0)
            val extraCarryCircles = if (hasExtraResultDigit && leadingCarrySlots > 0) 1 else 0

            addOperatorSpacer(binding.carryRow)
            addLeadingEmptyCells(binding.carryRow, leadingCarrySlots - extraCarryCircles)
            repeat(topString.length + extraCarryCircles) {
                binding.carryRow.addView(createCarryInput())
            }

            addOperatorSpacer(binding.topRow)
            addAlignedNumberRow(binding.topRow, topString, operation.columns)

            addOperatorCell(binding.bottomRow, "+")
            addAlignedNumberRow(binding.bottomRow, bottomString, operation.columns)

            addOperatorSpacer(binding.resultRow)
            addLeadingEmptyCells(binding.resultRow, operation.columns - resultString.length)
            repeat(resultString.length) {
                val input = createResultInput()
                resultInputs.add(input)
                binding.resultRow.addView(input)
            }
        } else {
            binding.carryRow.visibility = android.view.View.GONE
            addOperatorSpacer(binding.topRow)
            addOperatorCell(binding.bottomRow, "-")
            addOperatorSpacer(binding.resultRow)

            addAlignedSubtractionRows(topString, bottomString, operation.columns)
            addAlignedSubtractionResultRow(resultString, operation.columns)

            bindBorrowHints()
        }
    }

    private fun addAlignedNumberRow(row: LinearLayout, number: String, columns: Int) {
        addLeadingEmptyCells(row, columns - number.length)
        number.forEach { digit ->
            row.addView(createDigitCell(digit.toString()))
        }
    }

    private fun addAlignedSubtractionRows(top: String, bottom: String, columns: Int) {
        val topPadded = top.padStart(columns, ' ')
        val bottomPadded = bottom.padStart(columns, ' ')

        repeat(columns) { col ->
            val topDigit = topPadded[col]
            if (topDigit == ' ') {
                topBorrowInputsByColumn.add(null)
                binding.topRow.addView(createBorrowPlaceholder())
                binding.topRow.addView(createDigitCell(""))
            } else {
                val topBorrow = createCarryInput()
                topBorrowInputsByColumn.add(topBorrow)
                binding.topRow.addView(topBorrow)
                binding.topRow.addView(createDigitCell(topDigit.toString()))
            }

            val bottomDigit = bottomPadded[col]
            if (bottomDigit == ' ') {
                bottomBorrowHintsByColumn.add(null)
                binding.bottomRow.addView(createBorrowPlaceholder())
                binding.bottomRow.addView(createDigitCell(""))
            } else {
                val bottomHint = createBorrowIndicator()
                bottomBorrowHintsByColumn.add(bottomHint)
                binding.bottomRow.addView(bottomHint)
                binding.bottomRow.addView(createDigitCell(bottomDigit.toString()))
            }
        }
    }

    private fun addAlignedSubtractionResultRow(result: String, columns: Int) {
        repeat(columns) {
            binding.resultRow.addView(createBorrowPlaceholder(heightDp = 44))
            val input = createResultInput()
            resultInputs.add(input)
            binding.resultRow.addView(input)
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
        repeat(count.coerceAtLeast(0)) {
            row.addView(createDigitCell(""))
        }
    }

    private fun addOperatorSpacer(row: LinearLayout) {
        row.addView(createOperatorCell(""))
    }

    private fun addOperatorCell(row: LinearLayout, text: String) {
        row.addView(createOperatorCell(text))
    }

    private fun createOperatorCell(text: String): TextView {
        return TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(44))
            gravity = Gravity.CENTER
            this.text = text
            setTextColor(getColor(R.color.text_primary))
            textSize = 24f
        }
    }

    private fun createDigitCell(text: String): TextView {
        return TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_tower_cell)
            this.text = text
            setTextColor(getColor(R.color.text_primary))
            textSize = 24f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
    }

    private fun createBorrowPlaceholder(heightDp: Int = 36): TextView {
        return TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(heightDp))
            text = ""
        }
    }

    private fun createBorrowIndicator(): TextView {
        return TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(36))
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_tower_carry)
            setTextColor(getColor(R.color.answer_wrong))
            textSize = 12f
            text = ""
        }
    }

    private fun createCarryInput(): AppCompatEditText {
        return AppCompatEditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(36))
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_tower_carry)
            inputType = InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(InputFilter.LengthFilter(1))
            setTextColor(getColor(R.color.text_secondary))
            textSize = 16f
            setPadding(0, 0, 0, 0)
        }
    }

    private fun createResultInput(): AppCompatEditText {
        return AppCompatEditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_tower_cell)
            inputType = InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(InputFilter.LengthFilter(1))
            setTextColor(getColor(R.color.text_primary))
            textSize = 24f
            setPadding(0, 0, 0, 0)
        }
    }

    private fun checkResult() {
        val enteredDigits = resultInputs.map { input ->
            input.text
                ?.toString()
                ?.firstOrNull { it.isDigit() }
                ?.toString()
                .orEmpty()
        }

        if (enteredDigits.all { it.isEmpty() }) {
            Toast.makeText(this, getString(R.string.sumas_torres_fill_all), Toast.LENGTH_SHORT).show()
            return
        }

        val expectedPadded = currentOperation.result.toString().padStart(resultInputs.size, '0')
        val hasInvalidEmpty = enteredDigits.withIndex().any { (index, value) ->
            if (value.isNotEmpty()) return@any false
            if (index != 0) return@any true
            val expectedLeft = expectedPadded.getOrNull(index) ?: '0'
            expectedLeft != '0'
        }
        if (hasInvalidEmpty) {
            Toast.makeText(this, getString(R.string.sumas_torres_fill_all), Toast.LENGTH_SHORT).show()
            return
        }

        val normalized = enteredDigits.map { if (it.isEmpty()) "0" else it }
        val entered = normalized.joinToString(separator = "")
        val enteredValue = entered.toIntOrNull()
        if (enteredValue != null && enteredValue == currentOperation.result) {
            onCorrectAnswer()
        } else {
            onWrongAnswer()
        }
    }

    private fun onCorrectAnswer() {
        totalPoints += castlePoints
        castlesSolved += 1
        updateScoreTexts()
        Toast.makeText(
            this,
            getString(R.string.sumas_torres_correct_toast, castlePoints),
            Toast.LENGTH_SHORT
        ).show()
        showContinueDialog(
            title = getString(R.string.sumas_torres_continue_title),
            message = getString(R.string.sumas_torres_continue_message)
        )
    }

    private fun onWrongAnswer() {
        castlePoints = (castlePoints - PENALTY_PER_FAIL).coerceAtLeast(0)
        updateScoreTexts()

        if (castlePoints == 0) {
            showCorrectResult()
            showContinueDialog(
                title = getString(R.string.sumas_torres_no_points_title),
                message = getString(
                    R.string.sumas_torres_no_points_message,
                    currentOperation.result.toString()
                )
            )
            return
        }

        Toast.makeText(
            this,
            getString(R.string.sumas_torres_wrong_toast, castlePoints),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showCorrectResult() {
        val resultText = currentOperation.result.toString()
        val padded = resultText.padStart(resultInputs.size, '0')
        resultInputs.forEachIndexed { index, editText ->
            editText.setText(padded.getOrNull(index)?.toString().orEmpty())
        }
    }

    private fun showContinueDialog(title: String, message: String) {
        val dialog = AlertDialog.Builder(this, R.style.ThemeOverlay_Aprendemos_AlertDialog)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(getString(R.string.sumas_torres_continue_yes)) { _, _ ->
                startNewCastle()
            }
            .setNegativeButton(getString(R.string.sumas_torres_continue_no)) { _, _ ->
                goToResults()
            }
            .setCancelable(false)
            .show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(getColor(R.color.primary))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(getColor(R.color.accent))
    }

    private fun updateScoreTexts() {
        binding.castlePointsText.text = getString(R.string.sumas_torres_castle_points_format, castlePoints)
        binding.totalPointsText.text = getString(R.string.sumas_torres_total_points_format, totalPoints)
    }

    private fun goToResults() {
        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra(ResultActivity.EXTRA_SCORE, totalPoints)
            putExtra(ResultActivity.EXTRA_TOTAL_TIME, 0)
            putExtra(ResultActivity.EXTRA_TOTAL_QUESTIONS, castlesSolved)
            putExtra(ResultActivity.EXTRA_GAME_MODE, ScoreManager.MODE_MATH)
            putExtra(ResultActivity.EXTRA_MATH_TYPE, MathTopicsActivity.TYPE_ADD_SUB_CASTLES)
        }
        startActivity(intent)
        finish()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private data class TowerOperation(
        val top: Int,
        val bottom: Int,
        val result: Int,
        val columns: Int,
        val type: OperationType
    )

    private enum class OperationType {
        ADD, SUB
    }

    companion object {
        private const val START_POINTS = 140
        private const val PENALTY_PER_FAIL = 20
    }
}
