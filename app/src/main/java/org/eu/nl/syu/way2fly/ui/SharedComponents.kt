package org.eu.nl.syu.way2fly.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.Image
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import org.eu.nl.syu.way2fly.R
import kotlinx.coroutines.delay

@Composable
fun GlobalHeroHeader(
    title: String,
    subtitle: String? = null,
    isCompact: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = if (isCompact) 16.dp else 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!isCompact) {
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 0.dp
                ) {
                    Way2FlyLogo(
                        modifier = Modifier
                            .size(80.dp)
                            .padding(18.dp),
                        animated = true
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = if (isCompact) Alignment.Start else Alignment.CenterHorizontally, modifier = if (isCompact) Modifier else Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    style = if (isCompact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isCompact) {
                Row(content = actions)
            }
        }
    }
}

@Composable
fun StaticLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.logo_way2fly),
        contentDescription = "Way2Fly Logo",
        modifier = modifier,
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
    )
}

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
fun AnimatedLogo(modifier: Modifier = Modifier) {
    val image = AnimatedImageVector.animatedVectorResource(R.drawable.animated_logo)
    var atEnd by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            atEnd = !atEnd
            delay(1200)
        }
    }
    Image(
        painter = rememberAnimatedVectorPainter(image, atEnd),
        contentDescription = "Way2Fly Logo",
        modifier = modifier,
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
    )
}

@Composable
fun Way2FlyLogo(modifier: Modifier = Modifier, animated: Boolean = true) {
    if (animated && !LocalInspectionMode.current) {
        AnimatedLogo(modifier)
    } else {
        StaticLogo(modifier)
    }
}

