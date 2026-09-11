package dev.bennyb.daysay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

val ContentWidth = 640.dp
val Gutter = 24.dp

/**
 * Page frame: paper background, centred column, generous margins.
 * With [centered] the content sits in the middle of the free space under the header, and only
 * scrolls when it does not fit. Without it the content starts under the header and scrolls.
 */
@Composable
fun Page(
    title: String,
    onBack: (() -> Unit)? = null,
    centered: Boolean = false,
    actions: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .widthIn(max = ContentWidth)
                    .fillMaxWidth()
                    .padding(horizontal = Gutter)
                    .padding(top = 8.dp, bottom = 8.dp),
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack, modifier = Modifier.padding(end = 4.dp)) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
                Text(
                    title,
                    style = if (onBack == null) EyebrowStyle else MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                actions()
            }
            BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val minHeight = if (centered) maxHeight else 0.dp
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = minHeight),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = if (centered) Arrangement.Center else Arrangement.Top,
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = ContentWidth)
                            .fillMaxWidth()
                            .padding(horizontal = Gutter),
                        content = content,
                    )
                    Spacer(Modifier.height(if (centered) 24.dp else 48.dp))
                }
            }
        }
    }
}

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = EyebrowStyle, color = Paper.graphite, modifier = modifier)
}

/** Section label. Whitespace separates sections: no rule under the eyebrow. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Eyebrow(text, modifier = modifier.fillMaxWidth().padding(top = 44.dp, bottom = 16.dp))
}

/** A bordered paper card. No shadow: the display has no depth to fake. */
@Composable
fun InkCard(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Paper.ink, RoundedCornerShape(16.dp))
            .padding(20.dp),
        content = content,
    )
}

/** A filled ink slab, the inverse of the page. Used for the one thing that is alive on screen. */
@Composable
fun InkSlab(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Paper.ink, RoundedCornerShape(22.dp)),
        content = content,
    )
}

@Composable
fun MetaLine(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = Paper.ash, modifier = modifier, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/** Selection mark for a list row: a filled ink dot, or an empty ring. */
@Composable
fun Dot(selected: Boolean, modifier: Modifier = Modifier) {
    val base = modifier.size(14.dp)
    if (selected) Box(base.background(Paper.ink, CircleShape))
    else Box(base.border(1.5.dp, Paper.ink, CircleShape))
}
