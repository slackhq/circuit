package com.slack.circuit.star.petlist

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest.Builder
import coil3.request.crossfade
import com.slack.circuit.star.petlist.PetListTestConstants.AGE_AND_BREED_TAG
import com.slack.circuit.star.petlist.PetListTestConstants.CARD_TAG
import com.slack.circuit.star.petlist.PetListTestConstants.IMAGE_TAG
import com.slack.circuit.star.ui.Pets
import com.slack.circuit.star.ui.SharedElementTransitionScope
import com.slack.circuit.star.ui.sharedElementAnimatedContentScope

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
actual fun PetListGridItem(animal: PetListAnimal, modifier: Modifier, onClick: () -> Unit) =
  SharedElementTransitionScope {
    val boundsTransform = { _: Rect, _: Rect -> tween<Rect>(1400) }
    ElevatedCard(
      modifier = modifier.fillMaxWidth().testTag(CARD_TAG),
      shape = RoundedCornerShape(16.dp),
      colors =
        CardDefaults.elevatedCardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant,
          contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
      Column(modifier = Modifier.clickable(onClick = onClick)) {
        // Image
        val imageModifier =
          Modifier.sharedBounds(
              sharedContentState = rememberSharedContentState(key = "animal-${animal.id}"),
              animatedVisibilityScope = sharedElementAnimatedContentScope(),
              boundsTransform = boundsTransform,
            )
            .sharedElement(
              state = rememberSharedContentState(key = "animal-image-${animal.id}"),
              animatedVisibilityScope = sharedElementAnimatedContentScope(),
            )
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .fillMaxWidth()
            .testTag(IMAGE_TAG)

        if (animal.imageUrl == null) {
          Image(
            rememberVectorPainter(Pets),
            modifier = imageModifier.padding(8.dp),
            contentDescription = animal.name,
            contentScale = ContentScale.Crop,
            colorFilter = ColorFilter.tint(LocalContentColor.current),
          )
        } else {
          AsyncImage(
            modifier = imageModifier,
            model =
              Builder(LocalPlatformContext.current)
                .data(animal.imageUrl)
                .memoryCacheKey(animal.imageUrl)
                .crossfade(AnimationConstants.DefaultDurationMillis)
                .build(),
            contentDescription = animal.name,
            contentScale = ContentScale.Crop,
            imageLoader = SingletonImageLoader.get(LocalPlatformContext.current),
          )
        }
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.SpaceEvenly) {
          // Name
          Text(text = animal.name, style = MaterialTheme.typography.labelLarge)
          // Type
          animal.breed?.let {
            Text(text = animal.breed, style = MaterialTheme.typography.bodyMedium)
          }
          CompositionLocalProvider(
            LocalContentColor provides LocalContentColor.current.copy(alpha = 0.75f)
          ) {
            // Gender, age
            Text(
              modifier = Modifier.testTag(AGE_AND_BREED_TAG),
              text = "${animal.gender.displayName} – ${animal.age}",
              style = MaterialTheme.typography.bodySmall,
            )
          }
        }
      }
    }
  }
