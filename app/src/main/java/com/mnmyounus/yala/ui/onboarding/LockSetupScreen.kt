package com.mnmyounus.yala.ui.onboarding

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mnmyounus.yala.domain.model.LockType
import com.mnmyounus.yala.ui.lock.PatternLock

/**
 * Credential creation used during onboarding and from Settings.
 * Each mechanism is captured twice (enter + confirm) before it is hashed and stored.
 */
@Composable
fun LockSetupScreen(
    onSave: (LockType, String) -> Unit,
    onImagePoolPicked: (List<String>) -> Unit
) {
    var type by remember { mutableStateOf(LockType.PIN) }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Choose your lock type", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LockType.entries.forEach { t ->
                FilterChip(
                    selected = type == t,
                    onClick = { type = t },
                    label = { Text(t.name.replace('_', ' ')) }
                )
            }
        }

        when (type) {
            LockType.PIN -> TwoFieldSetup("4-digit PIN", 4, true) { onSave(LockType.PIN, it) }
            LockType.PASSWORD -> TwoFieldSetup("8-character password", 8, false) {
                onSave(LockType.PASSWORD, it)
            }
            LockType.PATTERN -> {
                Text("Draw a pattern of at least 4 dots, then draw it again to confirm.")
                var first by remember { mutableStateOf<List<Int>?>(null) }
                PatternLock { dots ->
                    val encoded = dots.joinToString("-")
                    if (first == null) first = dots
                    else if (first!!.joinToString("-") == encoded) onSave(LockType.PATTERN, encoded)
                    else first = null
                }
                Text(
                    if (first == null) "Draw your pattern" else "Now confirm it",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            LockType.IMAGE_SEQUENCE -> ImageSequenceSetup(onImagePoolPicked) {
                onSave(LockType.IMAGE_SEQUENCE, it)
            }
        }
    }
}

@Composable
private fun TwoFieldSetup(
    label: String,
    length: Int,
    numeric: Boolean,
    onConfirmed: (String) -> Unit
) {
    var a by remember { mutableStateOf("") }
    var b by remember { mutableStateOf("") }
    val valid = a.length == length && a == b

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = a,
            onValueChange = { v -> a = v.filter { !numeric || it.isDigit() }.take(length) },
            label = { Text(label) },
            singleLine = true
        )
        OutlinedTextField(
            value = b,
            onValueChange = { v -> b = v.filter { !numeric || it.isDigit() }.take(length) },
            label = { Text("Confirm") },
            singleLine = true
        )
        if (a.isNotEmpty() && b.isNotEmpty() && a != b) {
            Text("These don't match yet.", color = MaterialTheme.colorScheme.error)
        }
        Button(onClick = { onConfirmed(a) }, enabled = valid) { Text("Save lock") }
    }
}

/** User uploads exactly 5 images, then taps 3-4 of them in their secret order. */
@Composable
private fun ImageSequenceSetup(
    onImagePoolPicked: (List<String>) -> Unit,
    onConfirmed: (String) -> Unit
) {
    var pool by remember { mutableStateOf<List<String>>(emptyList()) }
    var sequence by remember { mutableStateOf<List<String>>(emptyList()) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        pool = uris.take(5).map { it.toString() }
        onImagePoolPicked(pool)
        sequence = emptyList()
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = { picker.launch("image/*") }) { Text("Upload 5 images") }
        Text("${pool.size}/5 images added", style = MaterialTheme.typography.bodySmall)

        if (pool.size == 5) {
            Text("Now tap 3 or 4 images in your secret order.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pool.take(3).forEach { id -> SetupTile(id, sequence) { sequence = toggle(sequence, id) } }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pool.drop(3).forEach { id -> SetupTile(id, sequence) { sequence = toggle(sequence, id) } }
            }
            Button(
                onClick = { onConfirmed(sequence.joinToString("|")) },
                enabled = sequence.size in 3..4
            ) { Text("Save image sequence") }
        }
    }
}

private fun toggle(current: List<String>, id: String): List<String> =
    if (current.size >= 4) listOf(id) else current + id

@Composable
private fun SetupTile(id: String, sequence: List<String>, onClick: () -> Unit) {
    val order = sequence.indexOf(id)
    Box(
        Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                if (order >= 0) 3.dp else 1.dp,
                if (order >= 0) MaterialTheme.colorScheme.primary else Color.Gray,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = id,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (order >= 0) {
            Text(
                "${order + 1}",
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
