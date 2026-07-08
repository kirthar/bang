# Arquitectura del proyecto

Aplicación Android nativa (Kotlin + Jetpack Compose) para jugar a BANG! contra IAs,
preparada para partidas online futuras. Reglas de referencia: `docs/REGLAS.md`.

## Módulos

```
core/  Kotlin puro (JVM). Dominio + motor de reglas. Sin dependencias de Android.
ai/    Kotlin puro (JVM). Estrategias de IA. Depende de core.
app/   Android (Compose, MVVM). Depende de core y ai.
```

`core` y `ai` no tienen dependencias Android para poder reutilizarse tal cual en un
futuro servidor JVM autoritativo.

## Principios

1. **Motor autoritativo y determinista** (`core/engine`): procesa `GameCommand`,
   valida contra el estado y emite `GameEvent`. Misma semilla + mismos comandos =
   misma partida (requisito del online y de las simulaciones MCTS).
2. **Información oculta**: los jugadores (humanos, IA o remotos) solo ven
   `PlayerGameView`, la proyección legal de su asiento. Nunca el `GameState` completo.
3. **Agentes intercambiables**: `PlayerAgent` es el único contrato entre el motor y
   quien decide. `AiAgent` (ai), la UI (app) y un futuro `RemoteAgent` (online) son
   implementaciones equivalentes.
4. **Toda decisión llega con sus opciones legales**: `DecisionRequest.options`
   enumera los comandos válidos. La UI habilita exactamente esas acciones; la IA
   aleatoria elige una al azar.
5. **Habilidades como estrategia**: `CharacterAbility` (una implementación por
   personaje) para poder añadir expansiones sin tocar el motor.

## Contratos congelados (Fase 0)

Paquete `com.kirthar.bang.core`:

| Archivo | Contenido |
|---|---|
| `model/Cards.kt` | `Suit`, `Rank`, `CardCategory`, `CardKind`, `Card` |
| `model/Role.kt` | `Role`, `rolesFor(n)` |
| `model/Characters.kt` | `CharacterId` (16 personajes, vidas) |
| `model/GameState.kt` | `GamePhase`, `PlayerState`, `PendingInteraction`, `GameState`, `GameConfig`, `GameResult` |
| `deck/DeckFactory.kt` | mazo exacto de 80 cartas |
| `command/GameCommand.kt` | comandos de jugador + `DrawSource` |
| `event/GameEvent.kt` | eventos + `redactFor(seat)` |
| `view/PlayerGameView.kt` | `PublicPlayerInfo`, `PlayerGameView`, `DecisionRequest`, `PickPurpose` |
| `agent/PlayerAgent.kt` | contrato de agente |
| `ability/CharacterAbility.kt` | hooks de habilidades + `AbilityContext` |
| `engine/GameEngine.kt` | `GameEngine`, `GameEngineFactory`, `CommandResult` |
| `engine/GameLoop.kt` | bucle agentes↔motor (implementado) |
| `transport/GameTransport.kt` | contrato de transporte para el online |

Paquete `com.kirthar.bang.ai`: `AiDifficulty`, `AiStrategy`, `AiStrategyFactory`,
`AiAgent` (implementado).

Cambios a estos contratos durante la Fase 1: anotarlos en el informe final para que
la integración (Fase 2) los arbitre — no cambiar la semántica sin documentarlo.

## Reparto de trabajo (Fase 1) y propiedad de archivos

| Área | Dueño | Archivos |
|---|---|---|
| Motor | subagente motor | `core/src/main/kotlin/.../engine/**` (nuevo `BangGameEngine`, habilidades en `ability/impl/`, interacciones), `core/src/test/**` |
| IA | subagente IA | `ai/src/main/kotlin/**` (menos `AiStrategy.kt`), `ai/src/test/**` |
| UI | subagente UI | `app/src/main/kotlin/com/kirthar/bang/ui/screens/**`, `ui/game/**`, `viewmodel/**`, `navigation/**`, `MainActivity.kt` |
| Arte/tema | subagente arte | `app/src/main/kotlin/com/kirthar/bang/ui/theme/**`, `ui/components/**`, `app/src/main/res/**` |

La UI consume los componentes visuales de `ui/components` (cartas, mesa, avatares) y
el tema de `ui/theme`; no los define.

## UI (app)

- Pantallas: Menú → Configurar partida (jugadores 4-7, dificultad, personaje) →
  Partida → Fin de partida.
- Estética western/saloon: mesa ovalada de madera, rivales alrededor, mazo y
  descartes en el centro, mano propia abajo, log de eventos. Solo español.
- `GameViewModel`: crea el motor + `GameLoop` con `AiAgent`s y un agente humano que
  espera la interacción de la UI (un `CompletableDeferred` por decisión).

## Online (futuro, no implementar ahora)

Servidor JVM reutiliza `core` tal cual: motor + `GameLoop` con `RemoteAgent`s que
leen de `GameTransport` (WebSocket). El cliente Android sustituye su `GameEngine`
local por un espejo alimentado por eventos. Nada de esto debe implementarse aún;
solo no romper los contratos que lo permiten.
