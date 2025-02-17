package com.thekeeperofpie.artistalleydatabase.alley

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.thekeeperofpie.artistalleydatabase.compose.conditionally

@Composable
internal fun ImageGrid(
    targetHeight: Int? = null,
    images: List<CatalogImage>,
    onImageClick: (index: Int, image: Uri) -> Unit = { _, _ -> },
) {
    val density = LocalDensity.current
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(160.dp),
        contentPadding = PaddingValues(8.dp),
        verticalItemSpacing = 8.dp,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.let {
            if (targetHeight == null) {
                it
            } else if (targetHeight > 0) {
                it.height(density.run { targetHeight.toDp() })
            } else {
                it.heightIn(max = 320.dp)
            }
        }
    ) {
        itemsIndexed(images) { index, image ->
            AsyncImage(
                model = image.uri,
                contentScale = ContentScale.FillWidth,
                contentDescription = stringResource(R.string.alley_artist_catalog_image),
                modifier = Modifier
                    .fillMaxWidth()
                    .conditionally(image.width != null && image.height != null) {
                        density.run { size(image.width!!.toDp(), image.height!!.toDp()) }
                    }
                    .clickable { onImageClick(index, image.uri) }
            )
        }
    }
}