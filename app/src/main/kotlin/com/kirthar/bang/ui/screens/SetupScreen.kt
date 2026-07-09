package com.kirthar.bang.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kirthar.bang.R
import com.kirthar.bang.ai.AiDifficulty
import com.kirthar.bang.core.model.CharacterId
import com.kirthar.bang.ui.components.GameTexts
import com.kirthar.bang.ui.components.TableBackground
import com.kirthar.bang.ui.components.WesternButton
import com.kirthar.bang.ui.theme.BangTheme
import com.kirthar.bang.viewmodel.GameSetupConfig

/** Pantalla de configuración de la partida: jugadores, dificultad, personaje y nombre. */
@Composable
fun SetupScreen(onStart: (GameSetupConfig) -> Unit, onBack: () -> Unit) {
    var playerCount by remember { mutableStateOf(5) }
    var difficulty by remember { mutableStateOf(AiDifficulty.MEDIUM) }
    var characterId by remember { mutableStateOf<CharacterId?>(null) }
    var playerName by remember { mutableStateOf("") }

    TableBackground(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(text = stringResource(R.string.setup_title), style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    SetupSection(title = stringResource(R.string.setup_players_count)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            (MIN_PLAYERS..MAX_PLAYERS).forEach { count ->
                                ChoiceChip(
                                    text = "$count",
                                    selected = playerCount == count,
                                    onClick = { playerCount = count },
                                )
                            }
                        }
                    }

                    SetupSection(title = stringResource(R.string.setup_difficulty)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChoiceChip(
                                text = stringResource(R.string.setup_difficulty_easy),
                                selected = difficulty == AiDifficulty.EASY,
                                onClick = { difficulty = AiDifficulty.EASY },
                            )
                            ChoiceChip(
                                text = stringResource(R.string.setup_difficulty_medium),
                                selected = difficulty == AiDifficulty.MEDIUM,
                                onClick = { difficulty = AiDifficulty.MEDIUM },
                            )
                            ChoiceChip(
                                text = stringResource(R.string.setup_difficulty_hard),
                                selected = difficulty == AiDifficulty.HARD,
                                onClick = { difficulty = AiDifficulty.HARD },
                            )
                        }
                    }

                    SetupSection(title = stringResource(R.string.setup_player_name)) {
                        OutlinedTextField(
                            value = playerName,
                            onValueChange = { playerName = it },
                            singleLine = true,
                            placeholder = { Text(stringResource(R.string.setup_player_name_hint)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    Text(text = stringResource(R.string.setup_character), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        item {
                            CharacterRow(
                                name = stringResource(R.string.setup_character_random),
                                description = stringResource(R.string.setup_character_random_description),
                                selected = characterId == null,
                                onClick = { characterId = null },
                            )
                        }
                        items(CharacterId.entries) { character ->
                            CharacterRow(
                                name = GameTexts.characterName(character),
                                description = GameTexts.characterDescription(character),
                                selected = characterId == character,
                                onClick = { characterId = character },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                WesternButton(text = stringResource(R.string.action_back), onClick = onBack)
                WesternButton(
                    text = stringResource(R.string.setup_start),
                    onClick = {
                        onStart(
                            GameSetupConfig(
                                playerCount = playerCount,
                                difficulty = difficulty,
                                characterId = characterId,
                                playerName = playerName,
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun SetupSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(6.dp))
        content()
    }
}

@Composable
private fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Text(
        text = text,
        color = contentColor,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun CharacterRow(name: String, description: String, selected: Boolean, onClick: () -> Unit) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        Text(text = name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

private const val MIN_PLAYERS = 4
private const val MAX_PLAYERS = 7

@Preview(widthDp = 800, heightDp = 400)
@Composable
private fun SetupScreenPreview() {
    BangTheme { SetupScreen(onStart = {}, onBack = {}) }
}
