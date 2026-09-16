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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun StationLogo(
    stationName: String,
    stationUrl: String,
    faviconUrl: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val candidateUrls = remember(faviconUrl, stationUrl) {
        val list = mutableListOf<String>()
        if (faviconUrl.isNotBlank()) list.add(faviconUrl)
        try {
            val uri = android.net.Uri.parse(stationUrl)
            val host = uri.host
            if (!host.isNullOrBlank()) {
                list.add("https://www.google.com/s2/favicons?domain=$host&sz=128")
                list.add("https://icons.duckduckgo.com/ip3/$host.ico")
            }
        } catch (_: Exception) {}
        list
    }

    var currentAttemptIndex by remember(candidateUrls) { mutableIntStateOf(0) }
    var hasErrorAll by remember(candidateUrls) { mutableStateOf(candidateUrls.isEmpty()) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (!hasErrorAll && currentAttemptIndex < candidateUrls.size) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(candidateUrls[currentAttemptIndex])
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription ?: stationName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onError = {
                    if (currentAttemptIndex + 1 >= candidateUrls.size) {
                        hasErrorAll = true
                    } else {
                        currentAttemptIndex++
                    }
                }
            )
        } else {
            val initial = stationName.trim().take(1).uppercase()
            if (initial.isNotEmpty() && initial[0].isLetterOrDigit()) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Radio,
                    contentDescription = contentDescription,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
