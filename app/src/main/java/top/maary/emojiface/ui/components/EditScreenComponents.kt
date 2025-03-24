package top.maary.emojiface.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import top.maary.emojiface.Constants.DEFAULT_FONT_MARKER
import top.maary.emojiface.R

@Composable
fun ShareButton(backgroundColor: Color, onClick: () -> Unit) {
    ExtendedFloatingActionButton(onClick = onClick,
        containerColor = backgroundColor,
        modifier = Modifier.padding(8.dp),
        icon = { Icon(Icons.Default.Share, stringResource(R.string.share)) },
        text = { Text(text = stringResource(R.string.share)) })
}

@Composable
fun SaveButton(backgroundColor: Color, onClick: () -> Unit) {
    ExtendedFloatingActionButton(onClick = onClick,
        containerColor = backgroundColor,
        modifier = Modifier.padding(8.dp),
        icon = { Icon(Icons.Rounded.SaveAlt, stringResource(R.string.save)) },
        text = { Text(text = stringResource(R.string.save)) })
}

@Composable
fun SettingsButton(backgroundColor: Color, onClick: () -> Unit) {
    FloatingActionButton(onClick = onClick,
        containerColor = backgroundColor,
        modifier = Modifier.padding(8.dp)) {
        Icon(Icons.Outlined.Settings, stringResource(R.string.settings))
    }
}

@Composable
fun EmojiCard(emoji: String, onClick: () -> Unit, clickable: Boolean = true, fontFamily: FontFamily? = null, containerColor: Color) {
    Card(
        modifier = Modifier
            .wrapContentHeight()
            .padding(horizontal = 8.dp, vertical = 16.dp)
            .clickable(enabled = clickable) { onClick() },  // 添加点击事件
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Text(text = emoji, fontSize = 60.sp, fontFamily = fontFamily)
    }
}

@Composable
fun EmojiCardSmall(emoji: String, onClick: () -> Unit, fontFamily: FontFamily? = null) {
    Card(
        modifier = Modifier
            .wrapContentHeight()
            .padding(end = 8.dp)
            .clickable { onClick() }
    ) {
        Text(
            text = emoji,
            fontSize = 20.sp,
            modifier = Modifier.padding(8.dp),
            fontFamily = fontFamily
        )
    }
}

@Composable
fun ResultImg(modifier: Modifier, bitmap: ImageBitmap, description: String, ratio: Float, animate: Boolean) {
    GlowingCard (
        modifier = modifier,
        ratio = ratio,
        animate = animate,
        cornersRadius = 16.dp,
        content = {
            Image(bitmap = bitmap,
                contentDescription = description,
                modifier = Modifier.fillMaxSize().padding(horizontal = ratio*8.dp, vertical = 8.dp).clip(
                    RoundedCornerShape(16.dp)
                ))
        }
    )
}

@Composable
fun PredefinedEmojiSettings(
    emojiOptions: List<String>,
    onClick: () -> Unit,
    fontFamily: FontFamily? = null
) {
    LazyRow(modifier = Modifier.padding(vertical = 8.dp)) {
        item { Spacer(modifier = Modifier.height(8.dp).width(16.dp)) }
        itemsIndexed(emojiOptions) { _, emoji ->
            EmojiCardSmall(emoji = emoji, onClick = onClick, fontFamily = fontFamily)
        }
        item { Spacer(modifier = Modifier.size(8.dp)) }
    }
}

@Composable
fun EditEmojiList(emojiOptions: List<String>, onClick: (String) -> Unit, fontFamily: FontFamily? = null) {
    var text by remember { mutableStateOf(emojiOptions.joinToString(separator = "")) }
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        value = text,
        trailingIcon = {
            IconButton(onClick = { onClick(text) }) {
                Icon(Icons.Outlined.Done, stringResource(R.string.done))
            }
        },
        onValueChange = { text = it },
        textStyle = TextStyle(fontFamily = fontFamily, fontSize = 20.sp)
    )
}

@Composable
fun HomeSwitchRow(
    state: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures {
                    onCheckedChange(!state) // 当点击 SwitchRow 时触发点击事件
                }
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(modifier = Modifier.weight(1f), text = stringResource(R.string.hide_home))
        Switch(checked = state, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownItem(
    modifier: Modifier,
    options: MutableList<String>,
    position: Int,
    onItemClicked: (Int) -> Unit,
    onItemActionClicked: (Int) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    Box(modifier = modifier) {
        ExposedDropdownMenuBox(
            modifier = Modifier.padding(8.dp),
            expanded = expanded,
            onExpandedChange = {
                if (options.size > 1) {
                    expanded = it
                } },
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                value = if (options[position] == DEFAULT_FONT_MARKER) stringResource(R.string.default_font) else options[position],
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                trailingIcon = {
                    if (options.size > 1) {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    }
                },
            )
            ExposedDropdownMenu(
                modifier = Modifier.wrapContentWidth(),
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        modifier = Modifier.fillMaxWidth(),
                        text = {
                            if (option == DEFAULT_FONT_MARKER) { Text(stringResource(R.string.default_font)) }
                            else { Text(option) } },
                        onClick = {
                            expanded = false
                            onItemClicked(options.indexOf(option))
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                        trailingIcon = {
                            if (options.indexOf(option) != 0) {
                                IconButton(onClick = { onItemActionClicked(options.indexOf(option)) }) {
                                    Icon(Icons.Outlined.RemoveCircleOutline, stringResource(R.string.remove_font))
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DropdownRow(
    options: MutableList<String>,
    position: Int,
    onItemClicked: (Int) -> Unit,
    onAddClick: () -> Unit,
    onRemoveClick: (Int) -> Unit
) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DropdownItem(
            modifier = Modifier.weight(1f), options = options,
            position = position, onItemClicked = onItemClicked, onItemActionClicked = onRemoveClick
        )
        OutlinedIconButton(onClick = { onAddClick() }, modifier = Modifier.padding(8.dp)) {
            Icon(Icons.Outlined.AttachFile, stringResource(R.string.choose_font))
        }
    }
}

@Composable
fun SliderWithCaption(
    leadingIcon: @Composable (() -> Unit),
    description: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    minRange: Float,
    maxRange: Float) {
    Row (modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically){
        leadingIcon()
        Column {
            Text(text = description, modifier = Modifier.padding(start = 8.dp))
            Slider(
                modifier = Modifier.padding(horizontal = 8.dp),
                value = value,
                onValueChange = onValueChange,
                valueRange = minRange..maxRange
            )
        }
    }

}
