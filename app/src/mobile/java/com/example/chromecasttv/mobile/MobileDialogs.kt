package com.example.chromecasttv

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================================================
// Telefon / tablet ortak pencereler: PIN, kanal düzenleme, metin girişi, onay, kanal eylem sayfası
// ============================================================================

@Composable
private fun mFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = RedoXColors.TextPrimary,
    unfocusedTextColor = RedoXColors.TextPrimary,
    focusedBorderColor = RedoXColors.Primary,
    unfocusedBorderColor = RedoXColors.Line,
    focusedLabelColor = RedoXColors.Primary,
    unfocusedLabelColor = RedoXColors.TextSecondary,
    cursorColor = RedoXColors.Primary,
    focusedPlaceholderColor = RedoXColors.TextTertiary,
    unfocusedPlaceholderColor = RedoXColors.TextTertiary
)

/** Metin kutusu: tüm ekranlarda aynı renkler. */
@Composable
fun MTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
    trailing: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder.isNotEmpty()) ({ Text(placeholder) }) else null,
        singleLine = singleLine,
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        trailingIcon = trailing,
        colors = mFieldColors(),
        modifier = modifier.fillMaxWidth()
    )
}

/** Ebeveyn kilidi: 4 haneli PIN. Doğru PIN ile [onVerify] true döner ve pencere kapanır (ViewModel kapatır). */
@Composable
fun MPinDialog(
    channel: TvChannel,
    onVerify: (String) -> Boolean,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RedoXColors.SurfaceHigh,
        titleContentColor = RedoXColors.TextPrimary,
        textContentColor = RedoXColors.TextSecondary,
        title = { Text("Ebeveyn kilidi", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("\"${channel.name}\" kilitli. 4 haneli PIN'i girin.")
                Spacer(modifier = Modifier.height(12.dp))
                MTextField(
                    value = pin,
                    onValueChange = { raw ->
                        val digits = raw.filter { it.isDigit() }.take(4)
                        pin = digits
                        wrong = false
                        if (digits.length == 4) {
                            if (!onVerify(digits)) {
                                wrong = true
                                pin = ""
                            }
                        }
                    },
                    label = "PIN",
                    keyboardType = KeyboardType.NumberPassword,
                    password = true
                )
                if (wrong) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("PIN hatalı, tekrar deneyin.", color = RedoXColors.Danger, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal", color = RedoXColors.TextSecondary) }
        }
    )
}

/** Kanal adı ve numarasını bu cihazda değiştirir; "Varsayılana dön" ile geri alınır. */
@Composable
fun MChannelEditDialog(
    channel: TvChannel,
    hasOverride: Boolean,
    onSave: (name: String?, number: Int?) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    var nameInput by remember(channel.id) { mutableStateOf(channel.name) }
    var numberInput by remember(channel.id) { mutableStateOf(channel.number.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RedoXColors.SurfaceHigh,
        titleContentColor = RedoXColors.TextPrimary,
        textContentColor = RedoXColors.TextSecondary,
        title = { Text("Kanalı düzenle", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Ad ve numara yalnızca bu cihazda değişir; M3U listeniz bozulmaz.", fontSize = 12.sp)
                MTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it.take(48) },
                    label = "Kanal adı"
                )
                MTextField(
                    value = numberInput,
                    onValueChange = { numberInput = it.filter { c -> c.isDigit() }.take(4) },
                    label = "Kanal numarası",
                    keyboardType = KeyboardType.Number
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(nameInput.trim().ifEmpty { null }, numberInput.toIntOrNull()) }) {
                Text("Kaydet", color = RedoXColors.Primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                if (hasOverride) {
                    TextButton(onClick = onReset) { Text("Varsayılana dön", color = RedoXColors.Gold) }
                }
                TextButton(onClick = onDismiss) { Text("İptal", color = RedoXColors.TextSecondary) }
            }
        }
    )
}

/** Tek satırlık metin istemi (yeni liste adı, adres vb.). */
@Composable
fun MTextPromptDialog(
    title: String,
    label: String,
    initial: String,
    confirmText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    placeholder: String = ""
) {
    var input by remember(title, initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RedoXColors.SurfaceHigh,
        titleContentColor = RedoXColors.TextPrimary,
        textContentColor = RedoXColors.TextSecondary,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            MTextField(value = input, onValueChange = { input = it }, label = label, placeholder = placeholder)
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(input) }) {
                Text(confirmText, color = RedoXColors.Primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal", color = RedoXColors.TextSecondary) }
        }
    )
}

/** Evet/Hayır onayı. */
@Composable
fun MConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    danger: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RedoXColors.SurfaceHigh,
        titleContentColor = RedoXColors.TextPrimary,
        textContentColor = RedoXColors.TextSecondary,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    confirmText,
                    color = if (danger) RedoXColors.Danger else RedoXColors.Primary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Vazgeç", color = RedoXColors.TextSecondary) }
        }
    )
}

@Composable
private fun MSheetAction(
    icon: ImageVector,
    text: String,
    enabled: Boolean = true,
    tint: Color = RedoXColors.TextPrimary,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) tint else RedoXColors.TextTertiary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            color = if (enabled) RedoXColors.TextPrimary else RedoXColors.TextTertiary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Kanal eylem sayfası (uzun basınca açılır): izle, favori, PIN kilidi, gizle, düzenle, TV'de oynat.
 * [castTvName] doluysa "TV'de oynat" gösterilir; kilitli kanallar TV'ye gönderilemez.
 */
@Composable
fun MChannelActionSheet(
    channel: TvChannel,
    isFavorite: Boolean,
    isLocked: Boolean,
    castTvName: String?,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleLock: () -> Unit,
    onHide: () -> Unit,
    onEdit: () -> Unit,
    onCast: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = RedoXColors.SurfaceHigh,
        contentColor = RedoXColors.TextPrimary
    ) {
        Column(modifier = Modifier.navigationBarsPadding().padding(bottom = 12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                ChannelBrandLogoBox(channel = channel, boxSize = 44.dp, fontSize = 11.sp)
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "${channel.number}. ${channel.name}",
                        color = RedoXColors.TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                    Text(
                        text = channel.nowTitleOrLive(),
                        color = RedoXColors.TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            MSheetAction(RedoXIcons.PlayArrow, "İzle") { onDismiss(); onPlay() }
            MSheetAction(
                icon = if (isFavorite) RedoXIcons.StarFilled else RedoXIcons.StarBorder,
                text = if (isFavorite) "Favorilerden çıkar" else "Favorilere ekle",
                onClick = { onDismiss(); onToggleFavorite() },
                tint = RedoXColors.Gold
            )
            MSheetAction(
                icon = if (isLocked) RedoXIcons.LockOpen else RedoXIcons.Lock,
                text = if (isLocked) "PIN kilidini kaldır" else "PIN ile kilitle",
                onClick = { onDismiss(); onToggleLock() }
            )
            MSheetAction(RedoXIcons.Edit, "Adını / numarasını düzenle") { onDismiss(); onEdit() }
            MSheetAction(RedoXIcons.VisibilityOff, "Listeden gizle") { onDismiss(); onHide() }
            if (castTvName != null) {
                MSheetAction(
                    icon = RedoXIcons.Cast,
                    text = if (isLocked) "Kilitli kanal TV'ye gönderilemez" else "TV'de oynat ($castTvName)",
                    onClick = { onDismiss(); onCast() },
                    enabled = !isLocked,
                    tint = RedoXColors.Primary
                )
            }
        }
    }
}
