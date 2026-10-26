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
 * RALYSND PROFESSIONAL MOCKUP v3 — shared restrained design system.
 *
 * Approved professional mockup: sophisticated dark charcoal background
 * (#0B0F16), generous whitespace, thin outlines, 16dp card corners,
 * emerald primary and violet secondary, and a THIN gradient ring for
 * the connect control — no glow overload, no cartoon, no mascot.
 * Presentation only: no navigation, ViewModel, protocol, database, or
 * permission logic lives here, and no sample data is produced here —
 * every card renders the real values its caller passes in.
 */

/** Emerald -> violet gradient used for the connect control and accents. */
fun ralysndGradientBrush(): Brush = Brush.linearGradient(
    colors = listOf(RalysndEmerald, RalysndViolet)
)

/** Dark glass card: charcoal surface, thin outline, 16dp corners. */
@Composable
fun RalysndGlassCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
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
 * Barely-there ambient tint for the home screen background (professional
 * mockup v3 is deliberately restrained): a faint violet tint at the
 * top right and a faint emerald tint at the bottom left. [glowScale]
 * lets the caller pulse them gently with the connection animation;
 * values near 1f are static.
 */
@Composable
fun RalysndGlowBackground(
    modifier: Modifier = Modifier,
    glowScale: Float = 1f
) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size((260 * glowScale).dp)
                .offset(x = 180.dp, y = (-80).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            RalysndViolet.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size((240 * glowScale).dp)
                .offset(x = (-140).dp, y = 480.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            RalysndEmerald.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )
    }
}
