package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.PersonaType

@Composable
fun AiAvatar(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    persona: PersonaType = PersonaType.BALANCED,
    isPulsing: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPulsing) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val gradientBrush = when (persona) {
        PersonaType.BALANCED -> Brush.linearGradient(
            listOf(Color(0xFF38BDF8), Color(0xFF6366F1), Color(0xFFA855F7))
        )
        PersonaType.EXECUTIVE -> Brush.linearGradient(
            listOf(Color(0xFFF59E0B), Color(0xFFEF4444), Color(0xFF8B5CF6))
        )
        PersonaType.CREATIVE -> Brush.linearGradient(
            listOf(Color(0xFFEC4899), Color(0xFF8B5CF6), Color(0xFF3B82F6))
        )
        PersonaType.MINDFUL -> Brush.linearGradient(
            listOf(Color(0xFF10B981), Color(0xFF06B6D4), Color(0xFF6366F1))
        )
        PersonaType.TECH -> Brush.linearGradient(
            listOf(Color(0xFF06B6D4), Color(0xFF3B82F6), Color(0xFF4F46E5))
        )
        PersonaType.JARVIS -> Brush.linearGradient(
            listOf(Color(0xFF00E5FF), Color(0xFF0072FF), Color(0xFF1E3A8A))
        )
    }

    val icon = when (persona) {
        PersonaType.BALANCED -> Icons.Rounded.AutoAwesome
        PersonaType.EXECUTIVE -> Icons.Rounded.Bolt
        PersonaType.CREATIVE -> Icons.Rounded.Palette
        PersonaType.MINDFUL -> Icons.Rounded.Spa
        PersonaType.TECH -> Icons.Rounded.Code
        PersonaType.JARVIS -> Icons.Rounded.SmartToy
    }

    Box(
        modifier = modifier
            .size(size)
            .scale(if (isPulsing) pulseScale else 1f)
            .clip(CircleShape)
            .background(gradientBrush)
            .border(
                width = 1.5.dp,
                color = Color.White.copy(alpha = 0.4f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Personal AI avatar (${persona.title})",
            tint = Color.White,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}
