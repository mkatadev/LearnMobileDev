package pl.prodevcode.learnmobiledev.presentation.infographics

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.jetbrains.compose.resources.decodeToImageBitmap
import pl.prodevcode.learnmobiledev.core.ui.AppString
import pl.prodevcode.learnmobiledev.core.ui.UiText
import pl.prodevcode.learnmobiledev.core.ui.localized
import pl.prodevcode.learnmobiledev.core.ui.resolve
import pl.prodevcode.learnmobiledev.domain.model.Infographic
import pl.prodevcode.learnmobiledev.presentation.theme.Spacing

/**
 * The infographics tab: a list of published pictures, each opening full-screen.
 *
 * Stateless like every other screen here — it renders `state` and emits intents. The one
 * thing it owns is decoding: bytes become an `ImageBitmap` at the edge of the UI, because
 * a decoded bitmap is a platform object that has no business travelling through the domain.
 */
@Composable
fun InfographicsScreen(
    viewModel: InfographicsViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.dispatch(InfographicsIntent.Ui.ScreenOpened)
    }

    var pendingMessage by remember { mutableStateOf<UiText?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is InfographicsEffect.ShowMessage -> pendingMessage = effect.text
            }
        }
    }

    pendingMessage?.let { message ->
        val text = message.resolve()
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(text)
            pendingMessage = null
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.screenHorizontal),
        ) {
            Header()

            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.isLoading -> LoadingIndicator()

                    state.error != null -> ErrorPanel(
                        message = state.error!!.resolve(),
                        onRetry = { viewModel.dispatch(InfographicsIntent.Ui.RetryClicked) },
                    )

                    state.showEmptyState -> EmptyPanel()

                    else -> InfographicsList(
                        infographics = state.infographics,
                        onOpen = {
                            viewModel.dispatch(InfographicsIntent.Ui.InfographicOpened(it))
                        },
                    )
                }
            }
        }
    }

    state.opened?.let { infographic ->
        InfographicViewer(
            infographic = infographic,
            onDismiss = { viewModel.dispatch(InfographicsIntent.Ui.ViewerDismissed) },
        )
    }
}

@Composable
private fun Header() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = localized(AppString.InfographicsTitle),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = Spacing.screenTop),
        )
        Text(
            text = localized(AppString.InfographicsSubtitle),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun InfographicsList(
    infographics: List<Infographic>,
    onOpen: (String) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(top = Spacing.small, bottom = Spacing.listBottom),
        verticalArrangement = Arrangement.spacedBy(Spacing.itemSpacing),
    ) {
        items(items = infographics, key = { it.id }) { infographic ->
            InfographicCard(infographic = infographic, onClick = { onOpen(infographic.id) })
        }
    }
}

/**
 * A preview card. The thumbnail is the picture itself, scaled down — at this size the
 * detail is unreadable, which is precisely what the full-screen viewer is for, so the card
 * says so rather than pretending the preview is the content.
 */
@Composable
private fun InfographicCard(infographic: Infographic, onClick: () -> Unit) {
    val bitmap = rememberImageBitmap(infographic)

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(),
    ) {
        Column(modifier = Modifier.padding(Spacing.cardPadding)) {
            Text(
                text = infographic.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = infographic.summary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = Spacing.small),
            )

            bitmap?.let {
                Image(
                    bitmap = it,
                    contentDescription = infographic.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        // The card shows the top of a tall picture rather than squeezing
                        // all of it into a thumbnail nobody could read anyway.
                        .aspectRatio(THUMBNAIL_ASPECT_RATIO)
                        .clip(RoundedCornerShape(6.dp)),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.small),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = localized(AppString.InfographicsZoomHint),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

/**
 * The full-screen viewer.
 *
 * A `Dialog` with the platform width limit turned off, so the picture uses the whole
 * screen — a zoomable image inset by dialog margins wastes exactly the space the reader
 * came for.
 */
@Composable
private fun InfographicViewer(infographic: Infographic, onDismiss: () -> Unit) {
    val bitmap = rememberImageBitmap(infographic) ?: return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            ZoomableImage(
                bitmap = bitmap,
                contentDescription = infographic.title,
                modifier = Modifier.fillMaxSize(),
            )

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(Spacing.cardPadding),
            ) {
                Text(localized(AppString.ActionClose), color = Color.White)
            }
        }
    }
}

/**
 * Decodes the bytes once per infographic.
 *
 * Keyed by id rather than by the array: decoding a megabyte on every recomposition would
 * make the pan gesture stutter, and a `ByteArray` compares by identity anyway.
 */
@Composable
private fun rememberImageBitmap(infographic: Infographic): ImageBitmap? =
    remember(infographic.id) {
        runCatching { infographic.bytes.decodeToImageBitmap() }.getOrNull()
    }

@Composable
private fun LoadingIndicator() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyPanel() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = localized(AppString.InfographicsEmpty),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ErrorPanel(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(
            onClick = onRetry,
            modifier = Modifier.padding(top = Spacing.small),
        ) {
            Text(localized(AppString.ActionRetry))
        }
    }
}

/** Wide enough to recognise the graphic, short enough not to fill the list with one card. */
private const val THUMBNAIL_ASPECT_RATIO = 16f / 10f
