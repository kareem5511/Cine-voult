package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddLink
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.SyncedAddon
import com.example.data.stremio.StremioUrlHelper
import com.example.ui.theme.AmoledCard
import com.example.ui.theme.machinedCardBorder
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddonsScreen(
    addonsState: UiState<List<SyncedAddon>>,
    addonCatalogNames: Map<String, List<String>> = emptyMap(),
    hiddenCatalogKeys: Set<String> = emptySet(),
    onInstallAddon: (String) -> Unit,
    onToggleAddon: (SyncedAddon, Boolean) -> Unit,
    onRemoveAddon: (SyncedAddon) -> Unit,
    onToggleCatalogHidden: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var urlInput by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    var configuringAddon by remember { mutableStateOf<SyncedAddon?>(null) }

    fun normalizeUrl(raw: String): String {
        val trimmed = raw.trim()
        return if (trimmed.startsWith("stremio://", ignoreCase = true)) {
            "https://" + trimmed.substring(10)
        } else {
            trimmed
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentPadding = PaddingValues(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Stremio Addons",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp
                    ),
                    color = Color.White
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.CloudDone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Synced across all devices via Cloud Vault",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.60f)
                    )
                }
            }
        }

        // Inline Manifest URL Installer Card
        item {
            val installerShape = RoundedCornerShape(18.dp)
            Card(
                colors = CardDefaults.cardColors(containerColor = AmoledCard),
                shape = installerShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .machinedCardBorder(shape = installerShape, cornerRadiusDp = 18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Install Addon via Manifest URL",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )

                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = normalizeUrl(it) },
                        placeholder = {
                            Text(
                                "https://.../manifest.json or stremio://...",
                                color = Color.White.copy(alpha = 0.40f),
                                fontSize = 13.sp
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        trailingIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                TextButton(
                                    onClick = {
                                        val pasted = clipboardManager.getText()?.text?.trim().orEmpty()
                                        if (pasted.isNotEmpty()) {
                                            urlInput = normalizeUrl(pasted)
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("paste_addon_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ContentPaste,
                                        contentDescription = "Paste URL",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        "Paste",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        if (urlInput.isNotBlank()) {
                                            onInstallAddon(urlInput.trim())
                                            urlInput = ""
                                        }
                                    },
                                    enabled = urlInput.isNotBlank(),
                                    modifier = Modifier.testTag("install_addon_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AddLink,
                                        contentDescription = "Install addon",
                                        tint = if (urlInput.isNotBlank()) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            Color.White.copy(alpha = 0.30f)
                                        }
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("install_addon_input")
                    )

                    // Quick chips for popular addons
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AssistChip(
                            onClick = { urlInput = StremioUrlHelper.DEFAULT_CINEMETA_MANIFEST },
                            label = { Text("Cinemeta (Official)", fontSize = 12.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = Color.White,
                                containerColor = Color.White.copy(alpha = 0.08f)
                            ),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                            shape = RoundedCornerShape(8.dp)
                        )
                        AssistChip(
                            onClick = { urlInput = StremioUrlHelper.DEFAULT_OPENSUBTITLES_MANIFEST },
                            label = { Text("OpenSubtitles v3", fontSize = 12.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = Color.White,
                                containerColor = Color.White.copy(alpha = 0.08f)
                            ),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                            shape = RoundedCornerShape(8.dp)
                        )
                        AssistChip(
                            onClick = { urlInput = "https://watchhub.strem.io/manifest.json" },
                            label = { Text("WatchHub", fontSize = 12.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = Color.White,
                                containerColor = Color.White.copy(alpha = 0.08f)
                            ),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                            shape = RoundedCornerShape(8.dp)
                        )
                        AssistChip(
                            onClick = { urlInput = "https://cyberflix.elfhosted.com/manifest.json" },
                            label = { Text("CyberFlix", fontSize = 12.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = Color.White,
                                containerColor = Color.White.copy(alpha = 0.08f)
                            ),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        }

        when (addonsState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = addonsState.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            is UiState.Success -> {
                val addonsList = addonsState.data
                itemsIndexed(addonsList, key = { _, it -> it.id.ifBlank { it.addonKey } }) { index, addon ->
                    val key = addon.addonKey.ifBlank { addon.id }
                    AddonCard(
                        addon = addon,
                        catalogNames = addonCatalogNames[key].orEmpty(),
                        hiddenCatalogKeys = hiddenCatalogKeys,
                        onToggle = { enabled -> onToggleAddon(addon, enabled) },
                        onConfigure = { configuringAddon = addon },
                        onDelete = { onRemoveAddon(addon) },
                        onToggleCatalogHidden = onToggleCatalogHidden
                    )
                }
            }
        }
    }

    // In-App Configurator Modal Dialog
    configuringAddon?.let { addon ->
        var editManifestUrl by remember(addon) { mutableStateOf(addon.manifestUrl) }
        AlertDialog(
            onDismissRequest = { configuringAddon = null },
            title = {
                Text(
                    text = "Configure ${addon.name}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Modify addon configuration or embed API keys directly in the manifest URL.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.70f)
                    )
                    OutlinedTextField(
                        value = editManifestUrl,
                        onValueChange = { editManifestUrl = normalizeUrl(it) },
                        label = { Text("Configured Manifest URL") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.20f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    val cfgUrl = remember(addon.manifestUrl) {
                        if (addon.manifestUrl.contains("/manifest.json")) addon.manifestUrl.replace("/manifest.json", "/configure")
                        else if (addon.manifestUrl.endsWith("/")) "${addon.manifestUrl}configure"
                        else "${addon.manifestUrl}/configure"
                    }
                    val ctx = LocalContext.current
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(cfgUrl)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Rounded.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Configuration Web Page")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editManifestUrl.isNotBlank() && editManifestUrl != addon.manifestUrl) {
                            onRemoveAddon(addon)
                            onInstallAddon(editManifestUrl.trim())
                        }
                        configuringAddon = null
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save & Sync")
                }
            },
            dismissButton = {
                TextButton(onClick = { configuringAddon = null }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.70f))
                }
            },
            containerColor = AmoledCard,
            shape = RoundedCornerShape(18.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddonCard(
    addon: SyncedAddon,
    catalogNames: List<String>,
    hiddenCatalogKeys: Set<String>,
    onToggle: (Boolean) -> Unit,
    onConfigure: () -> Unit,
    onDelete: () -> Unit,
    onToggleCatalogHidden: ((String) -> Unit)?
) {
    var catalogsExpanded by remember { mutableStateOf(false) }
    val cardShape = RoundedCornerShape(16.dp)

    Card(
        colors = CardDefaults.cardColors(containerColor = AmoledCard),
        shape = cardShape,
        modifier = Modifier
            .fillMaxWidth()
            .machinedCardBorder(shape = cardShape, cornerRadiusDp = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Icon, Name, Health Dot, Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val monogram = remember(addon.name) {
                        addon.name.trim()
                            .split(Regex("\\s+"))
                            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                            .take(2)
                            .joinToString("")
                            .ifBlank { "ST" }
                    }
                    var logoFailed by remember(addon.logoUrl) { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (addon.logoUrl.isNotBlank() && !logoFailed) {
                            AsyncImage(
                                model = addon.logoUrl,
                                contentDescription = addon.name,
                                contentScale = ContentScale.Crop,
                                onError = { logoFailed = true },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = monogram,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = addon.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Health status indicator dot
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (addon.enabled) Color(0xFF22C55E) else Color.White.copy(alpha = 0.30f)
                                    )
                            )
                            Text(
                                text = if (addon.enabled) "Active • v${addon.version.ifBlank { "1.0.0" }}" else "Disabled",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (addon.enabled) Color(0xFF22C55E) else Color.White.copy(alpha = 0.50f)
                            )
                        }
                    }
                }

                Switch(
                    checked = addon.enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.60f),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.12f)
                    )
                )
            }

            if (addon.description.isNotBlank()) {
                Text(
                    text = addon.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.65f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Catalogs Drawer (Expandable section)
            if (catalogNames.isNotEmpty()) {
                Surface(
                    color = Color.White.copy(alpha = 0.04f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { catalogsExpanded = !catalogsExpanded }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Catalogs (${catalogNames.size})",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = if (catalogsExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        AnimatedVisibility(visible = catalogsExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                catalogNames.forEach { catalogName ->
                                    val isHidden = hiddenCatalogKeys.contains(catalogName)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { onToggleCatalogHidden?.invoke(catalogName) }
                                            .padding(vertical = 4.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = !isHidden,
                                            onCheckedChange = { onToggleCatalogHidden?.invoke(catalogName) },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = MaterialTheme.colorScheme.primary,
                                                checkmarkColor = Color.Black
                                            ),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = catalogName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (!isHidden) Color.White else Color.White.copy(alpha = 0.40f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Resource tags & Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    addon.resources.forEach { res ->
                        Surface(
                            color = Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = res.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.70f),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                val cardContext = LocalContext.current
                val cardConfigUrl = remember(addon.manifestUrl) {
                    if (addon.manifestUrl.contains("/manifest.json")) addon.manifestUrl.replace("/manifest.json", "/configure")
                    else if (addon.manifestUrl.endsWith("/")) "${addon.manifestUrl}configure"
                    else "${addon.manifestUrl}/configure"
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Open Configuration in Browser
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                cardContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(cardConfigUrl)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            Icons.Rounded.OpenInBrowser,
                            contentDescription = "Configure in browser",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Configure",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Manifest settings button
                    IconButton(
                        onClick = onConfigure,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Settings,
                            contentDescription = "Edit manifest for ${addon.name}",
                            tint = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Delete button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = "Remove ${addon.name}",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
