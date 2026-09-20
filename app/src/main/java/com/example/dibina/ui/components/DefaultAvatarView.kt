package com.example.dibina.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R

/**
 * Avatar Composable that defaults to a neutral front-facing unisex silhouette avatar
 * when no photoUrl is provided, or when the photo is loading/failed.
 */
@Composable
fun DefaultAvatarView(
    photoUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    borderColor: Color = MaterialTheme.colorScheme.primaryContainer
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(2.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Foto Profil Siswa",
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                error = painterResource(id = R.drawable.ic_default_avatar),
                placeholder = painterResource(id = R.drawable.ic_default_avatar)
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.ic_default_avatar),
                contentDescription = "Avatar Siswa Netral",
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
            )
        }
    }
}
