package com.august.fitnessvowsync.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.august.fitnessvowsync.ui.theme.FitnessVowSyncTheme

@Composable
fun LoadingGuard(
    isLoading: Boolean,
    modifier: Modifier = Modifier.fillMaxSize(),
    content: @Composable () -> Unit
) {
    if (!isLoading) return content();

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = Color.White,
            strokeWidth = 4.dp
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoadingGuardPreview() {
    FitnessVowSyncTheme {
        LoadingGuard(isLoading = true) {}
    }
}