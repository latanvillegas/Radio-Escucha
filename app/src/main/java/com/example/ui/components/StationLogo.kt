package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.util.LogoImageLoader
import kotlin.math.abs

private val Fallback3DGradients = listOf(
    listOf(Color(0xFF93C5FD), Color(0xFF6366F1), Color(0xFF4C1D95)),
    listOf(Color(0xFFF9A8D4), Color(0xFFEC4899), Color(0xFF881337)),
    listOf(Color(0xFF7DD3FC), Color(0xFF0EA5E9), Color(0xFF1E3A8A)),
    listOf(Color(0xFF6EE7B7), Color(0xFF10B981), Color(0xFF064E3B)),
    listOf(Color(0xFFFDE047), Color(0xFFF59E0B), Color(0xFF7C2D12)),
    listOf(Color(0xFFD8B4FE), Color(0xFFA855F7), Color(0xFF3B0764))
)

private val LogoRim3DBrush = Brush.linearGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.55f),
        Color.White.copy(alpha = 0.14f),
        Color.Black.copy(alpha = 0.55f)
    ),
    start = Offset(0f, 0f),
    end = Offset(160f, 160f)
)

@Composable
fun StationLogo(
    stationName: String,
    stationUrl: String,
    faviconUrl: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val candidateUrls = remember(faviconUrl, stationUrl) {
        LogoImageLoader.buildCandidateUrls(faviconUrl, stationUrl)
    }
    var candidateIndex by remember(candidateUrls) { mutableIntStateOf(0) }
    val activeUrl = candidateUrls.getOrNull(candidateIndex)

    val gradientColors = remember(stationName) {
        Fallback3DGradients[abs(stationName.hashCode()) % Fallback3DGradients.size]
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
    val customImageLoader = remember(context) { LogoImageLoader.get(context) }

    val imageRequest = remember(activeUrl, context) {
        if (!activeUrl.isNullOrEmpty()) {
            val inlineBytes = LogoImageLoader.decodeDataUriIfPresent(activeUrl)
            ImageRequest.Builder(context)
                .data(inlineBytes ?: activeUrl)
                .size(512)
                .memoryCacheKey(activeUrl)
                .diskCacheKey(activeUrl)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .crossfade(180)
                .build()
        } else null
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.linearGradient(
                    colors = gradientColors,
                    start = Offset(0f, 0f),
                    end = Offset(160f, 160f)
                )
            )
            .border(BorderStroke(1.dp, LogoRim3DBrush), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (imageRequest != null) {
            AsyncImage(
                model = imageRequest,
                imageLoader = customImageLoader,
                contentDescription = contentDescription ?: stationName,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
                onError = {
                    candidateIndex += 1
                }
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
