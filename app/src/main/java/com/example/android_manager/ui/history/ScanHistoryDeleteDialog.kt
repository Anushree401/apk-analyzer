package com.example.android_manager.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.android_manager.ui.theme.Border
import com.example.android_manager.ui.theme.PrimaryText
import com.example.android_manager.ui.theme.RiskCritical
import com.example.android_manager.ui.theme.SecondaryText
import com.example.android_manager.ui.theme.Surface

/**
 * Confirmation dialog for deleting an individual record or wiping the full history log.
 */
@Composable
fun ScanHistoryDeleteDialog(
    isClearAll: Boolean,
    targetName: String? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Surface,
            border = BorderStroke(1.dp, Border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = if (isClearAll) "CLEAR ALL SCAN HISTORY" else "DELETE SCAN RECORD",
                    color = RiskCritical,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isClearAll) {
                        "Are you sure you want to permanently delete all scan records? This action cannot be undone."
                    } else {
                        "Are you sure you want to delete the scan log for \"${targetName ?: "this item"}\"?"
                    },
                    color = PrimaryText,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Border),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SecondaryText
                        )
                    ) {
                        Text(
                            text = "CANCEL",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RiskCritical,
                            contentColor = PrimaryText
                        ),
                        modifier = Modifier.padding(start = 12.dp)
                    ) {
                        Text(
                            text = if (isClearAll) "CLEAR ALL" else "DELETE",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
