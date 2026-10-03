package com.kuniran.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kuniran.R
import com.kuniran.core.model.UserRole

/** Tab Profil: data diri warga + tindakan akun (keluar RT / keluar aplikasi). */
@Composable
internal fun SettingsProfileTab(
    uiState: SettingsUiState,
    hasRt: Boolean,
    viewModel: SettingsViewModel,
    onLoggedOut: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var houseInfo by remember { mutableStateOf("") }

    LaunchedEffect(uiState.currentUser) {
        uiState.currentUser?.let { user ->
            fullName = user.fullName
            phoneNumber = user.phoneNumber ?: ""
            houseInfo = user.houseInfo ?: ""
        }
    }

    val rtGroup = uiState.rtGroup
    if (hasRt && rtGroup != null) {
        SettingsInviteCard(
            group = rtGroup,
            isAdmin = uiState.currentUser?.role == UserRole.ADMIN_RT
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_profile_header),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text(stringResource(R.string.field_profile_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("settings_name_input"),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text(stringResource(R.string.field_profile_phone)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("settings_phone_input"),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = houseInfo,
                onValueChange = { houseInfo = it },
                label = { Text(stringResource(R.string.field_profile_house)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("settings_house_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = {
                    viewModel.updateProfile(
                        fullName,
                        phoneNumber.ifBlank { null },
                        houseInfo.ifBlank { null }
                    ) {
                        // Success
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("btn_save_profile"),
                shape = RoundedCornerShape(12.dp),
                enabled = fullName.isNotBlank() && !uiState.isLoading
            ) {
                Text(stringResource(R.string.btn_save_profile))
            }
        }
    }

    Text(
        text = stringResource(R.string.settings_account_header),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )

    if (hasRt) {
        OutlinedButton(
            onClick = { viewModel.leaveRt { onLoggedOut() } },
            modifier = Modifier.fillMaxWidth().testTag("btn_leave_rt"),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text(stringResource(R.string.settings_btn_leave_rt))
        }
    }

    Button(
        onClick = { viewModel.signOut { onLoggedOut() } },
        modifier = Modifier.fillMaxWidth().testTag("btn_signout"),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    ) {
        Text(stringResource(R.string.settings_btn_signout))
    }
}
