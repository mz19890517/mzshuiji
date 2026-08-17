package com.mz.shunji.ui.notebooks.dialog

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import com.mz.shunji.R
import com.mz.shunji.data.model.Notebook
import com.mz.shunji.databinding.DialogEditNotebookBinding
import com.mz.shunji.ui.common.BaseDialog
import com.mz.shunji.ui.common.setButton
import com.mz.shunji.ui.utils.requestFocusAndKeyboard

class EditNotebookDialog : BaseDialog<DialogEditNotebookBinding>() {
    private val model: NotebookDialogViewModel by activityViewModel()
    private lateinit var notebook: Notebook

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notebook = arguments?.getParcelable(NOTEBOOK) ?: return
    }

    override fun createBinding(inflater: LayoutInflater) = DialogEditNotebookBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        when {
            this::notebook.isInitialized -> {
                // Valid notebook id
                dialog.setTitle(getString(R.string.action_rename_notebook))
                binding.editTextNotebookName.setText(notebook.name)
                dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.action_save), this) {
                    val name = binding.editTextNotebookName.text
                        .toString()
                        .ifEmpty { getString(R.string.indicator_untitled) }

                    lifecycleScope.launch {
                        val exists = model.notebookExistsByName(name, ignoreId = notebook.id)

                        if (!exists) {
                            val notebook = notebook.copy(name = name)
                            model.updateNotebook(notebook)
                            return@launch dismiss()
                        }

                        Toast
                            .makeText(requireContext(), getString(R.string.indicator_notebook_already_exists, name), Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                binding.editTextNotebookName.requestFocusAndKeyboard()
            }
            else -> {
                dialog.setTitle(getString(R.string.action_new_notebook))
                dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.action_save), this) {
                    val name = binding.editTextNotebookName.text
                        .toString()
                        .ifEmpty { getString(R.string.indicator_untitled) }

                    lifecycleScope.launch {
                        val exists = model.notebookExistsByName(name)

                        if (!exists) {
                            val tag = Notebook(name)
                            model.insertNotebook(tag)
                            return@launch dismiss()
                        }

                        Toast
                            .makeText(requireContext(), getString(R.string.indicator_notebook_already_exists, name), Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                binding.editTextNotebookName.requestFocusAndKeyboard()
            }
        }
    }

    companion object {
        private const val NOTEBOOK = "NOTEBOOK"
        fun build(notebook: Notebook?): EditNotebookDialog {
            return EditNotebookDialog().apply {
                arguments = if (notebook == null) bundleOf() else bundleOf(
                    NOTEBOOK to notebook
                )
            }
        }
    }
}
