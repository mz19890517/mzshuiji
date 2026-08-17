package com.mz.shunji.ui.sync.nextcloud

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import com.mz.shunji.R
import com.mz.shunji.data.sync.nextcloud.BackendValidationResult
import com.mz.shunji.data.sync.nextcloud.BackendValidationResult.CertificateError
import com.mz.shunji.data.sync.nextcloud.BackendValidationResult.Incompatible
import com.mz.shunji.data.sync.nextcloud.BackendValidationResult.InvalidConfig
import com.mz.shunji.data.sync.nextcloud.BackendValidationResult.Success
import com.mz.shunji.databinding.DialogNextcloudAccountBinding
import com.mz.shunji.ui.common.BaseDialog
import com.mz.shunji.ui.common.setButton
import com.mz.shunji.ui.utils.requestFocusAndKeyboard

class NextcloudAccountDialog : BaseDialog<DialogNextcloudAccountBinding>() {
    private val model: NextcloudViewModel by activityViewModel()

    private var username = ""
    private var password = ""

    override fun createBinding(inflater: LayoutInflater) = DialogNextcloudAccountBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        dialog.setTitle(getString(R.string.preferences_nextcloud_account))

        lifecycleScope.launch {
            username = model.username.first()
            password = model.password.first()

            if (username.isNotBlank() && password.isNotBlank()) {
                binding.editTextUsername.setText(username)
                binding.editTextPassword.setText(password)
            }
        }

        dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.action_save), this@NextcloudAccountDialog) {
            username = binding.editTextUsername.text.toString()
            password = binding.editTextPassword.text.toString()

            if (username.isBlank() or password.isBlank()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.message_credentials_cannot_be_blank),
                    Toast.LENGTH_SHORT
                ).show()
                return@setButton
            }

            Toast.makeText(requireContext(), getString(R.string.indicator_connecting), Toast.LENGTH_LONG).show()

            lifecycleScope.launch {
                val result = model.authenticate(username, password)
                val messageResId = when (result) {
                    Incompatible -> R.string.message_server_not_compatible
                    Success -> R.string.message_logged_in_successfully
                    InvalidConfig -> R.string.message_invalid_credentials
                    CertificateError -> R.string.message_certificates_invalid
                    BackendValidationResult.NotesNotInstalled -> R.string.message_notes_not_installed
                }
                Toast.makeText(requireContext(), getString(messageResId), Toast.LENGTH_LONG).show()
                if (result == Success) dismiss()
            }
        }

        binding.editTextUsername.requestFocusAndKeyboard()
    }
}
