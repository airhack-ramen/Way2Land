package org.eu.nl.syu.way2fly.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun MapScreen() {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offset.x,
                translationY = offset.y
            )
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale *= zoom
                    offset += pan
                }
            }
    ) {
        // Draw a simple airport map
        val w = size.width
        val h = size.height

        // Terminal Building
        drawRect(
            color = Color.LightGray,
            topLeft = Offset(w * 0.1f, h * 0.2f),
            size = Size(w * 0.8f, h * 0.3f)
        )

        // Gates
        for (i in 0..5) {
            drawRect(
                color = Color.Gray,
                topLeft = Offset(w * 0.15f + i * w * 0.12f, h * 0.15f),
                size = Size(w * 0.05f, h * 0.05f)
            )
            // Label for gate
        }

        // Runway
        drawRect(
            color = Color.DarkGray,
            topLeft = Offset(w * 0.05f, h * 0.7f),
            size = Size(w * 0.9f, h * 0.1f)
        )

        // Text or markers would go here, but Canvas text requires native canvas or specialized compose text drawing
        // For simplicity in this demo, just shapes.
        
        // Border
        drawRect(
            color = Color.Black,
            style = Stroke(width = 2f)
        )
    }
}
