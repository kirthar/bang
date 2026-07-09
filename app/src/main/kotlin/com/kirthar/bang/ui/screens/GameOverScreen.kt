package com.kirthar.bang.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kirthar.bang.R
import com.kirthar.bang.fake.previewGameOverInfo
import com.kirthar.bang.ui.components.GameTexts
import com.kirthar.bang.ui.components.TableBackground
import com.kirthar.bang.ui.components.WesternButton
import com.kirthar.bang.ui.theme.BangTheme
import com.kirthar.bang.viewmodel.GameOverInfo

/** Pantalla de fin de partida: equipo ganador, roles revelados y acciones de revancha/menú. */
@Composable
fun GameOverScreen(info: GameOverInfo, onRematch: () -> Unit, onMenu: () -> Unit) {
    TableBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.7f)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = stringResource(R.string.gameover_title), style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.gameover_winner, winningTeamText(info)),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = stringResource(R.string.gameover_roles), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(info.players, key = { it.seat }) { player ->
                    val isWinner = player.seat in info.result.winnerSeats
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = buildString {
                                append(player.name)
                                append(" (")
                                append(GameTexts.characterName(player.character))
                                append(")")
                                if (!player.isAlive) append(" ☠")
                            },
                            fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
                        )
                        Text(
                            text = buildString {
                                player.role?.let { append(GameTexts.roleName(it)) }
                                if (isWinner) {
                                    append(" — ")
                                    append(stringResource(R.string.gameover_winner_mark))
                                }
                            },
                            fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
                            color = if (isWinner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                WesternButton(
                    text = stringResource(R.string.gameover_rematch),
                    modifier = Modifier.width(180.dp),
                    onClick = onRematch,
                )
                WesternButton(
                    text = stringResource(R.string.gameover_menu),
                    modifier = Modifier.width(180.dp),
                    onClick = onMenu,
                )
            }
        }
    }
}

@Composable
private fun winningTeamText(info: GameOverInfo): String = when (info.result.winningRole) {
    com.kirthar.bang.core.model.Role.SHERIFF -> stringResource(R.string.gameover_team_sheriff)
    com.kirthar.bang.core.model.Role.OUTLAW -> stringResource(R.string.gameover_team_outlaws)
    com.kirthar.bang.core.model.Role.RENEGADE -> stringResource(R.string.gameover_team_renegade)
    com.kirthar.bang.core.model.Role.DEPUTY -> stringResource(R.string.gameover_team_sheriff)
}

@Preview(widthDp = 800, heightDp = 400)
@Composable
private fun GameOverScreenPreview() {
    BangTheme { GameOverScreen(info = previewGameOverInfo(), onRematch = {}, onMenu = {}) }
}
