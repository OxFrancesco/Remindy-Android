package com.francescooddo.remindy.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.francescooddo.remindy.wear.protocol.WearReminder

@Composable
internal fun ReminderListScreen(reminders: List<WearReminder>) {
    val listState = rememberTransformingLazyColumnState()

    ScreenScaffold(scrollState = listState) { scaffoldPadding ->
        TransformingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(
                top = scaffoldPadding.calculateTopPadding() + 8.dp,
                bottom = scaffoldPadding.calculateBottomPadding() + 28.dp,
                start = 14.dp,
                end = 14.dp,
            ),
        ) {
            item {
                ListHeader {
                    Text(
                        text = "Reminders",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            if (reminders.isEmpty()) {
                item {
                    Text(
                        text = "No reminders",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 17.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                reminders.forEach { reminder ->
                    item(key = reminder.id) {
                        ReminderRow(reminder.title)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderRow(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF202024))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 22.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
