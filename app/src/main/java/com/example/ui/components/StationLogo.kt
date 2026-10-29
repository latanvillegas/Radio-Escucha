package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlin.math.abs

private val FallbackGradients = listOf(
    listOf(Color(0xFF6366F1), Color(0xFFA855F7)),
    listOf(Color(0xFFEC4899), Color(0xFFF43F5E)),
    listOf(Color(0xFF0EA5E9), Color(0xFF2563EB)),
    listOf(Color(0xFF10B981), Color(0xFF059669)),
    listOf(Color(0xFFF59E0B), Color(0xFFEA580C))
)

@Composable
fun StationLogo(
    stationName: String,
    stationUrl: String,
    faviconUrl: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val cleanFavicon = remember(faviconUrl) { faviconUrl.trim() }
    var loadFailed by remember(cleanFavicon) { mutableStateOf(cleanFavicon.isEmpty()) }

    val gradientColors = remember(stationName) {
        FallbackGradients[abs(stationName.hashCode()) % FallbackGradients.size]
    }
    val initials = remember(stationName) {
        stationName
            .split(" ", "-", ".")
            .filter { it.isNotBlank() && it[0].isLetterOrDigit() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifBlank { stationName.trim().take(2).uppercase() }
    }

    val context = LocalContext.current
    val imageRequest = remember(cleanFavicon, context) {
        if (cleanFavicon.isNotEmpty()) {
            ImageRequest.Builder(context)
                .data(cleanFavicon)
                .size(512)
                .memoryCacheKey(cleanFavicon)
                .diskCacheKey(cleanFavicon)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .crossfade(200)
                .build()
        } else null
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.linearGradient(gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        if (!loadFailed && imageRequest != null) {
            AsyncImage(
                model = imageRequest,
                contentDescription = contentDescription ?: stationName,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
                onError = { loadFailed = true }
            )
        } else {
            if (initials.isNotEmpty()) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Radio,
                    contentDescription = contentDescription,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
