package com.example.android_manager.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.android_manager.ui.theme.Border
import com.example.android_manager.ui.theme.PrimaryText

@Composable
fun FindingCard(
    severity:String,
    title:String,
    packageName:String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Border,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = severity,
            color = PrimaryText
        )
        Text(
            text = title,
            color = PrimaryText,
            modifier =  Modifier.padding(top = 6.dp)
        )
        Text(
            text = packageName,
            color = PrimaryText,
            modifier =  Modifier.padding(top = 4.dp)
        )
    }
}
