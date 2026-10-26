package com.kunk.singbox.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * RALYSND PROFESSIONAL MOCKUP v3 — the connect control.
 *
 * A large, professional circular button: a THIN emerald-to-violet
 * gradient ring, a dark inner disc, and a white power icon drawn with
 * Canvas (arc + stem). No cartoon, no mascot, no glow halos, no
 * gradient-filled disc. Below the ring, a clearly letter-spaced label
 * (CONNECT when idle, DISCONNECT when running) and a status line
 * (Not Connected / Connected), exactly as in the approved mockup.
 *
 * Signature and onClick contract are unchanged: DashboardScreen calls
 * BigToggle(isRunning, onClick) and the whole control is one tap
 * target that toggles the connection, including the VPN-permission
 * flow handled by the caller.
 */
@Composable
fun BigToggle(
    isRunning: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "BigTogglePressScale"
    )

    val label = if (isRunning) "DISCONNECT" else "CONNECT"
    val status = if (isRunning) "Connected" else "Not Connected"
    val iconColor = if (isRunning) Color(0xFF34D399) else Color.White

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .semantics {
                contentDescription = label
                role = Role.Button
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(216.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(Color(0xFF0B0F16), CircleShape)
                .border(
                    width = 2.dp,
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFF10B981),
                            Color(0xFF8B5CF6),
                            Color(0xFF10B981)
                        )
                    ),
                    shape = CircleShape
                )
        ) {
            // Power icon: a 300-degree arc with a vertical stem, drawn
            // in white (emerald while running) — no icon-font dependency.
            Canvas(modifier = Modifier.size(72.dp)) {
                val stroke = 7.dp.toPx()
                val radius = (size.minDimension - stroke) / 2f
                val center = Offset(size.width / 2f, size.height / 2f)
                // Arc leaves its gap at the top (centred on -90 deg):
                // it runs from -60 deg through 240 deg, i.e. 300 deg.
                val startAngle = 300f
                val sweep = 300f
                drawArc(
                    color = iconColor,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                val top = Offset(center.x, center.y - radius)
                val bottom = Offset(center.x, center.y - radius * 0.15f)
                drawLine(
                    color = iconColor,
                    start = top,
                    end = bottom,
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }
        }
        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 6.sp
            ),
            color = Color.White
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = status,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isRunning) Color(0xFF34D399)
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
