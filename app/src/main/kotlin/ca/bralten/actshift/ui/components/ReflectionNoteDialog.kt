package ca.bralten.actshift.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReflectionNoteDialog(
  activityTitle: String,
  initialNote: String,
  onDismiss: () -> Unit,
  onSave: (String) -> Unit,
) {
  var noteText by remember(activityTitle, initialNote) { mutableStateOf(initialNote) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("What did you notice about yourself?") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = activityTitle,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
          value = noteText,
          onValueChange = { noteText = it },
          placeholder = { Text("Notice any shift in mood, energy, or thoughts...") },
          minLines = 3,
          maxLines = 5,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(onClick = { onSave(noteText) }) {
        Text("Save")
      }
    },
    dismissButton = {
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (initialNote.isNotBlank()) {
          TextButton(onClick = { onSave("") }) {
            Text("Clear")
          }
        }
        TextButton(onClick = onDismiss) {
          Text("Cancel")
        }
      }
    },
  )
}
