package com.nexus.browser.core.designsystem

import android.content.Context
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.browser.R
import com.nexus.browser.core.common.UrlUtils
import kotlinx.coroutines.delay

/**
 * Logo officiel NEXUS (Ruban N 3D Cyan -> Bleu -> Violet) issu de la charte graphique fournie.
 */
@Composable
fun NexusBrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    isPrivate: Boolean = false
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.nexus_logo_mark),
            contentDescription = "Logo NEXUS",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
        if (isPrivate) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.28f)
                    .clip(CircleShape)
                    .background(NexusColors.PrivateAccent)
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Navigation Privée",
                    tint = Color.White,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Typographie géométrique officielle "N Ξ X U S" avec le E stylisé à 3 barres horizontales.
 */
@Composable
fun NexusWordmark(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 24.sp,
    letterSpacing: TextUnit = 6.sp,
    color: Color = LocalNexusPalette.current.textPrimary
) {
    Text(
        text = "N\u039EXUS",
        color = color,
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
        letterSpacing = letterSpacing,
        fontFamily = FontFamily.SansSerif,
        modifier = modifier
    )
}

/**
 * Fond d'écran cosmique avec ondes lumineuses bleu/cyan/violet conforme à la direction artistique.
 */
@Composable
fun NexusCosmicBackground(
    isPrivate: Boolean = false,
    style: String = "COSMIC",
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalNexusPalette.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        if (palette.isDark && style != "MINIMAL") {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Halo supérieur droit bleu profond
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            when {
                                isPrivate -> Color(0xFF4C1D95).copy(alpha = 0.42f)
                                style == "AURORA" -> Color(0xFF00E5FF).copy(alpha = 0.32f)
                                else -> Color(0xFF1D4ED8).copy(alpha = 0.38f)
                            },
                            Color.Transparent
                        ),
                        center = Offset(w * 0.85f, h * 0.14f),
                        radius = w * 0.78f
                    ),
                    center = Offset(w * 0.85f, h * 0.14f),
                    radius = w * 0.78f
                )

                // Onde fluide inférieure bleue/violette (style maquette NEXUS)
                val bottomWave1 = Path().apply {
                    moveTo(0f, h * 0.76f)
                    cubicTo(
                        w * 0.32f, h * 0.68f,
                        w * 0.65f, h * 0.86f,
                        w, h * 0.72f
                    )
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = bottomWave1,
                    brush = Brush.linearGradient(
                        colors = if (style == "AURORA") {
                            listOf(
                                Color(0xFF064E3B).copy(alpha = 0.65f),
                                Color(0xFF0284C7).copy(alpha = 0.45f),
                                Color(0xFF00E5FF).copy(alpha = 0.55f)
                            )
                        } else {
                            listOf(
                                Color(0xFF0A2472).copy(alpha = 0.65f),
                                Color(0xFF2563EB).copy(alpha = 0.45f),
                                Color(0xFF7C3AED).copy(alpha = 0.55f)
                            )
                        },
                        start = Offset(0f, h * 0.7f),
                        end = Offset(w, h)
                    )
                )

                val bottomWave2 = Path().apply {
                    moveTo(0f, h * 0.86f)
                    cubicTo(
                        w * 0.38f, h * 0.79f,
                        w * 0.72f, h * 0.94f,
                        w, h * 0.82f
                    )
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = bottomWave2,
                    brush = Brush.linearGradient(
                        colors = if (style == "AURORA") {
                            listOf(
                                Color(0xFF10B981).copy(alpha = 0.24f),
                                Color(0xFF00E5FF).copy(alpha = 0.38f),
                                Color(0xFF3B82F6).copy(alpha = 0.52f)
                            )
                        } else {
                            listOf(
                                Color(0xFF00E5FF).copy(alpha = 0.18f),
                                Color(0xFF3B82F6).copy(alpha = 0.38f),
                                Color(0xFFA855F7).copy(alpha = 0.52f)
                            )
                        },
                        start = Offset(w * 0.1f, h * 0.82f),
                        end = Offset(w, h)
                    )
                )
            }
        }
        content()
    }
}

/**
 * Badge d'icône de site style application (Google, YouTube, GitHub, Wikipedia ou initiale).
 */
@Composable
fun NexusSiteBadge(
    title: String,
    url: String,
    size: Dp = 52.dp,
    modifier: Modifier = Modifier
) {
    val host = remember(url) { UrlUtils.extractHost(url).lowercase() }
    val lowerTitle = remember(title) { title.lowercase() }

    SurfaceBadgeContainer(size = size, modifier = modifier) {
        when {
            "google" in host || lowerTitle == "google" -> {
                Canvas(modifier = Modifier.size(size * 0.54f)) {
                    val stroke = this.size.width * 0.2f
                    val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
                    val topLeft = Offset(stroke / 2f, stroke / 2f)
                    drawArc(Color(0xFFEA4335), -45f, -95f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
                    drawArc(Color(0xFFFBBC05), -140f, -75f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
                    drawArc(Color(0xFF34A853), 145f, -95f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
                    drawArc(Color(0xFF4285F4), 50f, -55f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
                    drawRect(
                        color = Color(0xFF4285F4),
                        topLeft = Offset(this.size.width * 0.5f, this.size.height * 0.42f),
                        size = Size(this.size.width * 0.42f, stroke * 0.9f)
                    )
                }
            }
            "youtube" in host || lowerTitle == "youtube" -> {
                Canvas(modifier = Modifier.size(size * 0.58f)) {
                    val w = this.size.width
                    val h = this.size.height
                    drawRoundRect(
                        color = Color(0xFFFF0000),
                        topLeft = Offset(0f, h * 0.14f),
                        size = Size(w, h * 0.72f),
                        cornerRadius = CornerRadius(w * 0.22f, w * 0.22f)
                    )
                    val play = Path().apply {
                        moveTo(w * 0.40f, h * 0.33f)
                        lineTo(w * 0.67f, h * 0.50f)
                        lineTo(w * 0.40f, h * 0.67f)
                        close()
                    }
                    drawPath(play, Color.White)
                }
            }
            "github" in host || lowerTitle == "github" -> {
                Box(
                    modifier = Modifier
                        .size(size * 0.62f)
                        .clip(CircleShape)
                        .background(Color(0xFF181717)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "GH",
                        color = Color.White,
                        fontSize = (size.value * 0.25f).sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            "wikipedia" in host || "wikipédia" in lowerTitle || lowerTitle == "wikipedia" -> {
                Text(
                    text = "W",
                    color = Color(0xFF111827),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.46f).sp
                )
            }
            else -> {
                val initial = title.trim().take(1).uppercase().ifBlank {
                    host.take(1).uppercase().ifBlank { "W" }
                }
                Box(
                    modifier = Modifier
                        .size(size * 0.62f)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(NexusColors.BrandCyan, NexusColors.BrandBlue, NexusColors.BrandViolet)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * 0.30f).sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SurfaceBadgeContainer(
    size: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(size * 0.28f)),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun NexusButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isSecondary: Boolean = false
) {
    val palette = LocalNexusPalette.current
    if (isSecondary) {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.height(46.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, palette.border),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.textPrimary)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text = text, fontWeight = FontWeight.SemiBold)
        }
    } else {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.height(46.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = palette.primary,
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text = text, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun NexusIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = LocalNexusPalette.current.textPrimary,
    badgeCount: Int? = null
) {
    val palette = LocalNexusPalette.current
    val effectiveTint = if (enabled) tint else palette.textSecondary.copy(alpha = 0.35f)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        IconButton(onClick = onClick, enabled = enabled) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = effectiveTint,
                modifier = Modifier.size(22.dp)
            )
        }
        if (badgeCount != null && badgeCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp)
                    .background(palette.primary, CircleShape)
                    .padding(horizontal = 5.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun NexusTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {}
) {
    val palette = LocalNexusPalette.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder.isNotEmpty()) ({ Text(placeholder) }) else null,
        singleLine = singleLine,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = palette.primary,
            unfocusedBorderColor = palette.border,
            focusedContainerColor = palette.surface,
            unfocusedContainerColor = palette.surface,
            focusedTextColor = palette.textPrimary,
            unfocusedTextColor = palette.textPrimary
        )
    )
}

@Composable
fun NexusSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: (String) -> Unit,
    onVoiceSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Rechercher ou saisir une URL"
) {
    val palette = LocalNexusPalette.current
    val clipboard = LocalClipboardManager.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val composeView = LocalView.current
    val context = LocalContext.current

    val showKeyboardNow = {
        composeView.requestFocus()
        runCatching { focusRequester.requestFocus() }
        keyboardController?.show()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(composeView, InputMethodManager.SHOW_IMPLICIT)
    }

    val hideKeyboardNow = {
        keyboardController?.hide()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(composeView.windowToken, 0)
        focusManager.clearFocus(force = true)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(27.dp))
            .background(palette.surfaceElevated.copy(alpha = 0.92f))
            .border(1.dp, palette.border.copy(alpha = 0.85f), RoundedCornerShape(27.dp))
            .clickable { showKeyboardNow() }
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Rechercher",
            tint = palette.textSecondary,
            modifier = Modifier.size(21.dp)
        )
        Spacer(Modifier.width(12.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.textPrimary),
            cursorBrush = SolidColor(palette.primary),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Go,
                autoCorrect = false
            ),
            keyboardActions = KeyboardActions(
                onGo = {
                    hideKeyboardNow()
                    onSubmit(query.trim())
                },
                onSearch = {
                    hideKeyboardNow()
                    onSubmit(query.trim())
                },
                onDone = {
                    hideKeyboardNow()
                    onSubmit(query.trim())
                }
            ),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        keyboardController?.show()
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                        imm?.showSoftInput(composeView, InputMethodManager.SHOW_IMPLICIT)
                    }
                }
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
                        (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER ||
                            keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)
                    ) {
                        hideKeyboardNow()
                        onSubmit(query.trim())
                        true
                    } else {
                        false
                    }
                },
            decorationBox = { innerTextField ->
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                innerTextField()
            }
        )
        if (query.isNotEmpty()) {
            IconButton(
                onClick = {
                    onQueryChange("")
                    showKeyboardNow()
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Clear, contentDescription = "Effacer", tint = palette.textSecondary, modifier = Modifier.size(18.dp))
            }
        } else {
            val clipText = clipboard.getText()?.text?.takeIf { it.isNotBlank() }
            if (clipText != null) {
                IconButton(
                    onClick = {
                        onQueryChange(clipText)
                        showKeyboardNow()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = "Coller depuis le presse-papiers",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
        IconButton(onClick = onVoiceSearchClick, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Mic, contentDescription = "Recherche vocale", tint = palette.textSecondary, modifier = Modifier.size(19.dp))
        }
    }
}

@Composable
fun NexusToolbar(
    addressText: String,
    isEditingAddress: Boolean,
    isSecureHttps: Boolean,
    isLoading: Boolean,
    progress: Int,
    isPrivate: Boolean,
    activeExtensionsCount: Int,
    onAddressChange: (String) -> Unit,
    onAddressFocusChange: (Boolean) -> Unit,
    onSubmitAddress: (String) -> Unit,
    onCopyUrl: (String) -> Unit,
    onReloadOrStopClick: () -> Unit,
    onExtensionsClick: () -> Unit,
    onReleaseWebViewFocus: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    val clipboard = LocalClipboardManager.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val composeView = LocalView.current
    val context = LocalContext.current

    var focusRequestTick by remember { mutableIntStateOf(0) }

    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = addressText,
                selection = TextRange(0, addressText.length)
            )
        )
    }

    val hideKeyboardAndClearFocus = {
        keyboardController?.hide()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(composeView.windowToken, 0)
        focusManager.clearFocus(force = true)
    }

    val submitCurrentInput = {
        val submitted = textFieldValue.text.trim()
        hideKeyboardAndClearFocus()
        onAddressFocusChange(false)
        if (submitted.isNotEmpty()) {
            onSubmitAddress(submitted)
        }
    }

    LaunchedEffect(isEditingAddress, focusRequestTick) {
        if (isEditingAddress) {
            onReleaseWebViewFocus()
            withFrameNanos { }
            composeView.requestFocus()
            runCatching { focusRequester.requestFocus() }
            keyboardController?.show()
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(composeView, InputMethodManager.SHOW_IMPLICIT)

            delay(45)
            onReleaseWebViewFocus()
            composeView.requestFocus()
            runCatching { focusRequester.requestFocus() }
            keyboardController?.show()
            imm?.showSoftInput(composeView, InputMethodManager.SHOW_IMPLICIT)
        } else {
            textFieldValue = TextFieldValue(
                text = addressText,
                selection = TextRange(addressText.length)
            )
        }
    }

    LaunchedEffect(addressText) {
        // Ne jamais écraser la saisie ou la sélection pendant que l'utilisateur édite l'URL
        if (!isEditingAddress) {
            textFieldValue = TextFieldValue(
                text = addressText,
                selection = TextRange(addressText.length)
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Barre d'adresse principale
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(21.dp))
                    .background(palette.surfaceElevated)
                    .border(
                        width = 1.dp,
                        color = when {
                            isEditingAddress -> palette.primary
                            isPrivate -> NexusColors.PrivateAccent.copy(alpha = 0.6f)
                            else -> palette.border
                        },
                        shape = RoundedCornerShape(21.dp)
                    )
                    .clickable {
                        onReleaseWebViewFocus()
                        composeView.requestFocus()
                        if (!isEditingAddress) {
                            val initialUrl = if (UrlUtils.isHomeUrl(addressText)) "" else addressText
                            textFieldValue = TextFieldValue(
                                text = initialUrl,
                                selection = TextRange(0, initialUrl.length)
                            )
                            onAddressFocusChange(true)
                        }
                        focusRequestTick++
                    }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when {
                        isPrivate && UrlUtils.isHomeUrl(addressText) -> Icons.Default.Shield
                        isSecureHttps -> Icons.Default.Lock
                        else -> Icons.Default.LockOpen
                    },
                    contentDescription = if (isSecureHttps) "Connexion HTTPS sécurisée" else "Non sécurisé",
                    tint = when {
                        isPrivate && UrlUtils.isHomeUrl(addressText) -> NexusColors.PrivateAccent
                        isSecureHttps -> NexusColors.Success
                        else -> NexusColors.Warning
                    },
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))

                if (isEditingAddress) {
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { newValue ->
                            textFieldValue = newValue
                            onAddressChange(newValue.text)
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.textPrimary),
                        cursorBrush = SolidColor(palette.primary),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go,
                            autoCorrect = false
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = { submitCurrentInput() },
                            onSearch = { submitCurrentInput() },
                            onDone = { submitCurrentInput() }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    onReleaseWebViewFocus()
                                    keyboardController?.show()
                                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                                    imm?.showSoftInput(composeView, InputMethodManager.SHOW_IMPLICIT)
                                }
                            }
                            .onPreviewKeyEvent { keyEvent ->
                                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
                                    (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER ||
                                        keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)
                                ) {
                                    submitCurrentInput()
                                    true
                                } else {
                                    false
                                }
                            },
                        decorationBox = { innerTextField ->
                            if (textFieldValue.text.isEmpty()) {
                                Text(
                                    text = "Rechercher ou saisir une URL",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = palette.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            innerTextField()
                        }
                    )
                    val clipText = clipboard.getText()?.text?.takeIf { it.isNotBlank() }
                    if (clipText != null && textFieldValue.text.isEmpty()) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Coller l'adresse",
                            tint = palette.primary,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable {
                                    textFieldValue = TextFieldValue(
                                        text = clipText,
                                        selection = TextRange(clipText.length)
                                    )
                                    onAddressChange(clipText)
                                    focusRequestTick++
                                }
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    if (textFieldValue.text.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Effacer l'URL",
                            tint = palette.textSecondary,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable {
                                    textFieldValue = TextFieldValue(
                                        text = "",
                                        selection = TextRange.Zero
                                    )
                                    onAddressChange("")
                                    focusRequestTick++
                                }
                        )
                    }
                } else {
                    val isHome = UrlUtils.isHomeUrl(addressText) || addressText.isBlank()
                    val displayDomain = remember(addressText) {
                        if (isHome) {
                            "Rechercher ou saisir une URL"
                        } else {
                            UrlUtils.extractHost(addressText).ifBlank { addressText }
                        }
                    }

                    Text(
                        text = displayDomain,
                        color = if (isHome) palette.textSecondary else palette.textPrimary,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isHome) FontWeight.Normal else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (!isHome) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copier l'URL",
                            tint = palette.textSecondary,
                            modifier = Modifier
                                .size(17.dp)
                                .clickable { onCopyUrl(addressText) }
                        )
                        Spacer(Modifier.width(10.dp))
                        Icon(
                            imageVector = if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                            contentDescription = if (isLoading) "Arrêter le chargement" else "Actualiser la page",
                            tint = palette.textSecondary,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onReloadOrStopClick() }
                        )
                    }
                }
            }

            Spacer(Modifier.width(4.dp))

            // Bouton dédié aux extensions dans la barre supérieure
            NexusIconButton(
                icon = Icons.Default.Extension,
                contentDescription = "Extensions WebExtensions",
                onClick = onExtensionsClick,
                tint = palette.textPrimary,
                badgeCount = activeExtensionsCount.takeIf { it > 0 }
            )
        }

        AnimatedVisibility(visible = isLoading) {
            LinearProgressIndicator(
                progress = { (progress.coerceIn(5, 100)) / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = palette.primary,
                trackColor = Color.Transparent
            )
        }
    }
}

/**
 * Carte d'aperçu d'onglet conforme au téléphone central de la maquette (Onglets).
 */
@Composable
fun NexusTabCard(
    title: String,
    url: String,
    isActive: Boolean,
    isPrivate: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    onDuplicate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    val isHome = UrlUtils.isHomeUrl(url)
    val borderColor = when {
        isActive && isPrivate -> NexusColors.PrivateAccent
        isActive -> NexusColors.BrandCyan.copy(alpha = 0.85f)
        else -> palette.border
    }

    if (isHome) {
        // Carte d'accueil NEXUS avec fond cosmique et logo NEXUS officiel (comme sur la maquette Onglets)
        Card(
            modifier = modifier
                .fillMaxWidth()
                .height(106.dp)
                .clickable { onSelect() },
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(if (isActive) 2.dp else 1.dp, borderColor),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF060B1E))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF040817),
                                Color(0xFF0D2159),
                                Color(0xFF2E1065)
                            )
                        )
                    )
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    NexusBrandLogo(size = 42.dp, isPrivate = isPrivate)
                    Spacer(Modifier.width(12.dp))
                    NexusWordmark(fontSize = 20.sp, letterSpacing = 4.sp, color = Color.White)
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fermer l'onglet",
                        tint = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    } else {
        // Carte d'onglet web avec badge de site (Google, Wikipedia, etc.)
        val host = remember(url) { UrlUtils.extractHost(url).ifBlank { url } }
        Card(
            modifier = modifier
                .fillMaxWidth()
                .clickable { onSelect() },
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(if (isActive) 2.dp else 1.dp, borderColor),
            colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NexusSiteBadge(
                    title = title.ifBlank { host },
                    url = url,
                    size = 44.dp
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title.ifBlank { host },
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = host,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                TextButton(
                    onClick = onDuplicate,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Dupliquer", modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("Dupliquer", fontSize = 11.sp)
                }
                IconButton(onClick = onClose, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fermer l'onglet",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NexusExtensionCard(
    name: String,
    version: String,
    author: String,
    description: String,
    sourceLabel: String,
    compatibilityLabel: String,
    isCompatible: Boolean,
    enabled: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onDetailsClick: () -> Unit,
    onUninstallClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onDetailsClick() },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, palette.border),
        colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.primary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Extension,
                        contentDescription = null,
                        tint = palette.primary
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "v$version",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textSecondary
                        )
                    }
                    Text(
                        text = "Par $author • $sourceLabel",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSecondary
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggleEnabled,
                    enabled = isCompatible,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = palette.primary)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = compatibilityLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isCompatible) NexusColors.Success else NexusColors.Warning
                )
                Row {
                    TextButton(onClick = onDetailsClick) {
                        Text("Détails & Permissions")
                    }
                    IconButton(onClick = onUninstallClick) {
                        Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = palette.error)
                    }
                }
            }
        }
    }
}

@Composable
fun NexusDownloadCard(
    fileName: String,
    sourceUrl: String,
    status: String,
    downloadedBytes: Long,
    totalBytes: Long,
    speedBytesPerSec: Long,
    onPauseResume: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onShowInFolder: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    val progressFraction = if (totalBytes > 0) {
        (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, palette.border),
        colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (status == "FAILED") Icons.Default.ErrorOutline else Icons.Default.DownloadDone,
                    contentDescription = null,
                    tint = when (status) {
                        "COMPLETED" -> NexusColors.Success
                        "FAILED" -> palette.error
                        else -> palette.primary
                    }
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = sourceUrl,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (status == "RUNNING" || status == "PAUSED") {
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = palette.primary,
                    trackColor = palette.border
                )
                Spacer(Modifier.height(6.dp))
            }

            val sizeText = formatBytes(downloadedBytes) +
                if (totalBytes > 0) " / ${formatBytes(totalBytes)}" else ""
            val speedText = if (status == "RUNNING" && speedBytesPerSec > 0) {
                " • ${formatBytes(speedBytesPerSec)}/s"
            } else ""

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$status • $sizeText$speedText",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textSecondary
                )
                Row {
                    if (status == "RUNNING" || status == "PAUSED") {
                        IconButton(onClick = onPauseResume, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = if (status == "RUNNING") Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Pause/Reprise",
                                tint = palette.textPrimary
                            )
                        }
                    }
                    if (status == "COMPLETED") {
                        TextButton(onClick = onOpen) {
                            Text("Ouvrir")
                        }
                        IconButton(onClick = onShowInFolder, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "Afficher dans le dossier", tint = palette.textSecondary)
                        }
                        IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Share, contentDescription = "Partager", tint = palette.textSecondary)
                        }
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = palette.error)
                    }
                }
            }
        }
    }
}

@Composable
fun NexusSettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val palette = LocalNexusPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.surfaceElevated)
            .border(1.dp, palette.border, RoundedCornerShape(14.dp))
            .let { if (onClick != null) it.clickable { onClick() } else it }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(palette.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = palette.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
            if (subtitle.isNotBlank()) {
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
            }
        }
        if (trailingContent != null) {
            Spacer(Modifier.width(8.dp))
            trailingContent()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexusBottomSheet(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalNexusPalette.current
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = palette.surface,
        contentColor = palette.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            content = content
        )
    }
}

@Composable
fun NexusDialog(
    title: String,
    onDismissRequest: () -> Unit,
    confirmText: String = "Confirmer",
    dismissText: String = "Annuler",
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    val palette = LocalNexusPalette.current
    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = palette.surfaceElevated,
        titleContentColor = palette.textPrimary,
        textContentColor = palette.textSecondary,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = content,
        confirmButton = {
            NexusButton(text = confirmText, onClick = onConfirm)
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(dismissText, color = palette.textSecondary)
            }
        }
    )
}

@Composable
fun NexusLoadingIndicator(
    label: String = "Chargement en cours...",
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    val infinite = rememberInfiniteTransition(label = "nexus_pulse")
    val alpha by infinite.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse_alpha"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(palette.surfaceElevated)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = palette.primary,
            strokeWidth = 2.5.dp
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            color = palette.textPrimary.copy(alpha = alpha),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f Ko", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.2f Mo", mb)
    val gb = mb / 1024.0
    return String.format("%.2f Go", gb)
}
