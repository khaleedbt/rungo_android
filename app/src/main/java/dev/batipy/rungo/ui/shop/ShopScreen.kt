package dev.batipy.rungo.ui.shop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.batipy.rungo.R
import dev.batipy.rungo.data.network.dto.MerchantDto
import dev.batipy.rungo.ui.common.localizedDescription
import dev.batipy.rungo.ui.theme.RunGoAccentLight
import dev.batipy.rungo.ui.theme.RunGoField
import dev.batipy.rungo.ui.theme.RunGoTextPrimary
import dev.batipy.rungo.ui.theme.RunGoTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    uiState: ShopUiState,
    isRefreshing: Boolean = false,
    isGridView: Boolean = false,
    onToggleGridView: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onMerchantClick: (MerchantDto) -> Unit = {},
    modifier: Modifier = Modifier
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        when (uiState) {
            is ShopUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is ShopUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = uiState.message, color = RunGoTextSecondary)
                }
            }

            is ShopUiState.Success -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.shop_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onToggleGridView) {
                            Icon(
                                imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView,
                                contentDescription = stringResource(
                                    if (isGridView) R.string.shop_view_list else R.string.shop_view_grid
                                ),
                                tint = RunGoTextSecondary
                            )
                        }
                    }

                    if (isGridView) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.merchants) { merchant ->
                                ShopMerchantGridCard(merchant, onClick = { onMerchantClick(merchant) })
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(uiState.merchants) { merchant ->
                                ShopMerchantCard(merchant, onClick = { onMerchantClick(merchant) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShopMerchantCard(merchant: MerchantDto, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = RunGoField,
        border = BorderStroke(1.5.dp, RunGoAccentLight.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column {
            if (merchant.logo != null) {
                AsyncImage(
                    model = merchant.logo,
                    contentDescription = merchant.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                )
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = merchant.name,
                    fontWeight = FontWeight.Bold,
                    color = RunGoTextPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
                if (merchant.description.isNotBlank()) {
                    Text(
                        text = merchant.localizedDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = RunGoTextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                if (merchant.cityName.isNotBlank()) {
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "📍")
                        Text(
                            text = merchant.cityName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = RunGoTextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShopMerchantGridCard(merchant: MerchantDto, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = RunGoField,
        border = BorderStroke(1.5.dp, RunGoAccentLight.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            if (merchant.logo != null) {
                AsyncImage(
                    model = merchant.logo,
                    contentDescription = merchant.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(RunGoAccentLight.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = merchant.name.take(1).uppercase(),
                        color = RunGoTextPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = merchant.name,
                    fontWeight = FontWeight.Bold,
                    color = RunGoTextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (merchant.cityName.isNotBlank()) {
                    Text(
                        text = "📍 " + merchant.cityName,
                        style = MaterialTheme.typography.bodySmall,
                        color = RunGoTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
