package com.mz.shunji.ui.editor.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.text.isDigitsOnly
import androidx.fragment.app.setFragmentResult
import com.mz.shunji.R
import com.mz.shunji.databinding.DialogInsertTableBinding
import com.mz.shunji.ui.common.BaseDialog
import com.mz.shunji.ui.common.setButton
import com.mz.shunji.ui.editor.EditorFragment
import com.mz.shunji.ui.editor.markdown.tableMarkdown
import com.mz.shunji.ui.utils.requestFocusAndKeyboard

class InsertTableDialog : BaseDialog<DialogInsertTableBinding>() {
    override fun createBinding(inflater: LayoutInflater) = DialogInsertTableBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        dialog.apply {
            setTitle(getString(R.string.action_insert_table))
            setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.action_cancel)) { _, _ -> }
            setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.action_insert), this@InsertTableDialog) {
                val rows = binding.editTextRows.text.toString()
                val columns = binding.editTextColumns.text.toString()

                if (rows.isBlank() || columns.isBlank() || !rows.isDigitsOnly() || !columns.isDigitsOnly()) {
                    Toast.makeText(requireContext(), getString(R.string.message_invalid_number_rows_columns), Toast.LENGTH_SHORT).show()
                    return@setButton
                }

                val markdown = tableMarkdown(
                    rows = rows.toInt(),
                    columns = columns.toInt(),
                )
                setFragmentResult(
                    EditorFragment.MARKDOWN_DIALOG_RESULT,
                    bundleOf(
                        EditorFragment.MARKDOWN_DIALOG_RESULT to markdown
                    )
                )
                dismiss()
            }
        }

        if (binding.editTextColumns.text?.isEmpty() == true) {
            binding.editTextColumns.requestFocusAndKeyboard()
        } else {
            binding.editTextRows.requestFocusAndKeyboard()
        }
    }
}
