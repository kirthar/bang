package com.kirthar.bang.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kirthar.bang.R
import com.kirthar.bang.ui.components.TableBackground
import com.kirthar.bang.ui.components.WesternButton
import com.kirthar.bang.ui.theme.BangTheme

/** Pantalla de menú principal: título y acceso a una partida nueva. */
@Composable
fun MenuScreen(onNewGame: () -> Unit) {
    TableBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.displayLarge)
            Text(text = stringResource(R.string.menu_subtitle), style = MaterialTheme.typography.titleMedium)

            WesternButton(
                text = stringResource(R.string.menu_new_game),
                modifier = Modifier.width(240.dp),
                onClick = onNewGame,
            )
            WesternButton(
                text = stringResource(R.string.menu_online),
                modifier = Modifier.width(240.dp),
                enabled = false,
                onClick = {},
            )
            WesternButton(
                text = stringResource(R.string.menu_options),
                modifier = Modifier.width(240.dp),
                enabled = false,
                onClick = {},
            )
        }
    }
}

@Preview(widthDp = 800, heightDp = 400)
@Composable
private fun MenuScreenPreview() {
    BangTheme { MenuScreen(onNewGame = {}) }
}
