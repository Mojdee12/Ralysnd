package com.kunk.singbox.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * RALYSND MOCKUP LAYOUT 2026 — shared dark-glass design system.
 *
 * Approved dark mockup: deep charcoal background, dark glass cards with a
 * thin outline, emerald primary and violet secondary, and a glowing
 * emerald-to-violet gradient for the connect control. Presentation only:
 * no navigation, ViewModel, protocol, database, or permission logic lives
 * here, and no sample data is produced here — every card renders the
 * real values its caller passes in.
 */

/** Emerald -> violet gradient used for the connect control and accents. */
fun ralysndGradientBrush(): Brush = Brush.linearGradient(
    colors = listOf(RalysndEmerald, RalysndViolet)
)

/** Dark glass card: charcoal surface, thin outline, 24dp corners. */
@Composable
fun RalysndGlassCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    val borderColor = if (selected) RalysndEmeraldBright else RalysndOutline
    val clickModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .clip(shape)
            .background(RalysndSurface.copy(alpha = 0.94f), shape)
            .border(1.dp, borderColor, shape)
            .then(clickModifier)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

/**
 * Soft ambient glows for the home screen background: violet at the top
 * right, emerald at the bottom left. [glowScale] lets the caller pulse
 * them gently with the connection animation; values near 1f are static.
 */
@Composable
fun RalysndGlowBackground(
    modifier: Modifier = Modifier,
    glowScale: Float = 1f
) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size((300 * glowScale).dp)
                .offset(x = 180.dp, y = (-80).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            RalysndViolet.copy(alpha = 0.28f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size((280 * glowScale).dp)
                .offset(x = (-140).dp, y = 480.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            RalysndEmerald.copy(alpha = 0.22f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )
    }
}
