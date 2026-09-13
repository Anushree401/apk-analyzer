package com.example.android_manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.android_manager.ui.theme.Background
import com.example.android_manager.ui.theme.PrimaryText
import com.example.android_manager.ui.theme.SecondaryText

@Composable
fun APVMTopBar(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Start
    ) {

        if (onBack != null) {

            IconButton(
                onClick = onBack
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = PrimaryText
                )
            }
        }

        Text(
            text = "APVM",
            color = PrimaryText,
            modifier = Modifier.padding(
                vertical = 8.dp
            )
        )

        Text(
            text = "Android Security Manager",
            color = SecondaryText,
            modifier = Modifier.padding(
                start = 16.dp,
                top = 8.dp,
                bottom = 8.dp
            )
        )
    }
}