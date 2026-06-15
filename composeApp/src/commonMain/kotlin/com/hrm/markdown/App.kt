package com.hrm.markdown

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrm.markdown.preview.MarkdownPreview
import com.hrm.markdown.renderer.Markdown
import com.hrm.markdown.renderer.MarkdownTheme
import kotlinx.serialization.Serializable
import kotlin.collections.get

@Composable
@Preview
fun App() {
    val colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()

    MaterialTheme(colorScheme = colorScheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            MarkdownPreview()


//            QuizScreenPreview()
        }
    }
}


@Composable
fun MathMarkdown(text    : String,
          modifier : Modifier){



    val color = Color.Black
    val fontSize = 18.sp

    Markdown(
        markdown = text,
        modifier = modifier,
        theme = MarkdownTheme.light().copy(
            headingStyles = listOf(
                TextStyle( fontWeight = FontWeight.Bold,     fontSize = fontSize, color = color),
                TextStyle( fontWeight = FontWeight.Bold,     fontSize = fontSize, color = color),
                TextStyle( fontWeight = FontWeight.SemiBold, fontSize = fontSize, color = color),
                TextStyle( fontWeight = FontWeight.SemiBold, fontSize = fontSize, color = color),
                TextStyle( fontWeight = FontWeight.Medium,   fontSize = fontSize, color = color),
                TextStyle( fontWeight = FontWeight.Normal,   fontSize = fontSize, color = color),
            ),
            bodyStyle = TextStyle(
                
                fontWeight =  FontWeight.Bold ,
                fontSize   = fontSize,
                color      = color,
                textAlign  =  TextAlign.Center,
                lineHeight = fontSize * 1.4f
            ),
            mathBlockBackground = Color.Transparent,
            listBulletColor     = color,
            blockQuoteTextColor = color.copy(alpha = 0.7f),
        )
    )
}

