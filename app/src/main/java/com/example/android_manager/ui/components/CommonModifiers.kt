package com.example.android_manager.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.android_manager.ui.theme.CyberCyan

/**
 * Renders a thin cyber-cyan scrollbar along the trailing edge of a scrollable component.
 */
fun Modifier.verticalScrollbar(
    scrollProgress: Float,
    viewportRatio: Float,
    thumbColor: Color = CyberCyan.copy(alpha = 0.6f),
    trackWidth: Dp = 3.dp
): Modifier = this.drawWithContent {
    drawContent()
    if (viewportRatio < 1f && viewportRatio > 0f) {
        val barHeight = (size.height * viewportRatio).coerceAtLeast(24.dp.toPx())
        val maxOffset = size.height - barHeight
        val barY = maxOffset * scrollProgress.coerceIn(0f, 1f)
        val barWidthPx = trackWidth.toPx()

        drawRoundRect(
            color = thumbColor,
            topLeft = Offset(size.width - barWidthPx - 2.dp.toPx(), barY),
            size = Size(barWidthPx, barHeight),
            cornerRadius = CornerRadius(barWidthPx / 2f, barWidthPx / 2f)
        )
    }
}

fun Modifier.verticalScrollbar(
    state: androidx.compose.foundation.lazy.LazyListState,
    thumbColor: Color = CyberCyan.copy(alpha = 0.6f),
    trackWidth: Dp = 3.dp
): Modifier = composed {
    val layoutInfo = state.layoutInfo
    val totalItems = layoutInfo.totalItemsCount
    val visibleItems = layoutInfo.visibleItemsInfo.size
    val firstVisible = state.firstVisibleItemIndex
    val progress = if (totalItems > visibleItems && totalItems > 0) {
        firstVisible.toFloat() / (totalItems - visibleItems).toFloat()
    } else 0f
    val ratio = if (totalItems > 0) visibleItems.toFloat() / totalItems.toFloat() else 1f
    verticalScrollbar(progress, ratio, thumbColor, trackWidth)
}

fun Modifier.verticalScrollbar(): Modifier = this


/**
 * Adds an accessible, distinct keyboard focus indicator (Tab / D-pad focus)
 * matching the APVM CyberCyan design language.
 */
fun Modifier.keyboardFocusable(
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    focusBorderColor: Color = CyberCyan,
    interactionSource: MutableInteractionSource? = null
): Modifier = composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isFocused by source.collectIsFocusedAsState()

    if (isFocused) {
        this.border(width = 2.dp, color = focusBorderColor, shape = shape)
    } else {
        this
    }
}

/**
 * Provides smooth hover and press micro-interaction feedback.
 */
fun Modifier.cardInteraction(
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    interactionSource: MutableInteractionSource? = null
): Modifier = composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isHovered by source.collectIsHoveredAsState()
    val isPressed by source.collectIsPressedAsState()
    val isFocused by source.collectIsFocusedAsState()

    val borderColor = when {
        isFocused -> CyberCyan
        isPressed -> CyberCyan.copy(alpha = 0.8f)
        isHovered -> CyberCyan.copy(alpha = 0.45f)
        else -> Color.Transparent
    }

    val borderWidth by animateFloatAsState(
        targetValue = if (isFocused || isPressed || isHovered) 1.5f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "interactionBorder"
    )

    if (borderWidth > 0f) {
        this.border(width = borderWidth.dp, color = borderColor, shape = shape)
    } else {
        this
    }
}
