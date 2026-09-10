package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class PromptSuggestion(
    val title: String,
    val icon: ImageVector,
    val prompt: String
)

val DEFAULT_PROMPT_SUGGESTIONS = listOf(
    PromptSuggestion("Jarvis Briefing", Icons.Rounded.SmartToy, "Jarvis, provide me with a full intelligence status briefing for today."),
    PromptSuggestion("Design Photo Prompt", Icons.Rounded.Image, "Generate a detailed, hyper-realistic photo prompt and artistic description for an AI image generator."),
    PromptSuggestion("Cinematic Video Script", Icons.Rounded.Movie, "Create a cinematic video storyboard, scene-by-scene script, and visual shot directions."),
    PromptSuggestion("Plan today", Icons.Rounded.CalendarToday, "Help me plan a focused, high-productivity day."),
    PromptSuggestion("Brainstorm", Icons.Rounded.Lightbulb, "Brainstorm 5 creative ideas for a personal project."),
    PromptSuggestion("Draft note", Icons.Rounded.Mail, "Help me draft a concise, professional message."),
    PromptSuggestion("Learn concept", Icons.Rounded.Psychology, "Explain a fascinating concept in simple, intuitive terms."),
    PromptSuggestion("Tech architecture", Icons.Rounded.Code, "Give me clean architectural advice for an app."),
    PromptSuggestion("Daily reflection", Icons.Rounded.AutoAwesome, "Guide me through a brief mindful daily check-in.")
)

@Composable
fun PromptChipsRow(
    onSelectPrompt: (String) -> Unit,
    modifier: Modifier = Modifier,
    suggestions: List<PromptSuggestion> = DEFAULT_PROMPT_SUGGESTIONS
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.forEachIndexed { index, item ->
            AssistChip(
                onClick = { onSelectPrompt(item.prompt) },
                label = { Text(item.title, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    labelColor = MaterialTheme.colorScheme.onSurface
                ),
                border = AssistChipDefaults.assistChipBorder(
                    enabled = true,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                ),
                modifier = Modifier.testTag("prompt_chip_$index")
            )
        }
    }
}
