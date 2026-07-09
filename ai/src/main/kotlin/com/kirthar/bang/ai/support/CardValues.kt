package com.kirthar.bang.ai.support

import com.kirthar.bang.core.model.Card
import com.kirthar.bang.core.model.CardKind
import com.kirthar.bang.core.model.CardKind.BANG
import com.kirthar.bang.core.model.CardKind.BARREL
import com.kirthar.bang.core.model.CardKind.BEER
import com.kirthar.bang.core.model.CardKind.CAT_BALOU
import com.kirthar.bang.core.model.CardKind.DUEL
import com.kirthar.bang.core.model.CardKind.DYNAMITE
import com.kirthar.bang.core.model.CardKind.GATLING
import com.kirthar.bang.core.model.CardKind.GENERAL_STORE
import com.kirthar.bang.core.model.CardKind.INDIANS
import com.kirthar.bang.core.model.CardKind.JAIL
import com.kirthar.bang.core.model.CardKind.MISSED
import com.kirthar.bang.core.model.CardKind.MUSTANG
import com.kirthar.bang.core.model.CardKind.PANIC
import com.kirthar.bang.core.model.CardKind.REMINGTON
import com.kirthar.bang.core.model.CardKind.REV_CARABINE
import com.kirthar.bang.core.model.CardKind.SALOON
import com.kirthar.bang.core.model.CardKind.SCHOFIELD
import com.kirthar.bang.core.model.CardKind.SCOPE
import com.kirthar.bang.core.model.CardKind.STAGECOACH
import com.kirthar.bang.core.model.CardKind.VOLCANIC
import com.kirthar.bang.core.model.CardKind.WELLS_FARGO
import com.kirthar.bang.core.model.CardKind.WINCHESTER

/**
 * Valoración estática del interés de conservar cada carta en la mano, en una escala
 * de **0 (prescindible) a 10 (muy valiosa)**.
 *
 * Se usa para dos cosas:
 * - elegir qué descartar (se descartan las de menor valor);
 * - romper empates al elegir cartas (Emporio, Panic!, «¡desenfunda!»…).
 *
 * No es una valoración táctica del momento (eso lo hace [com.kirthar.bang.ai.HeuristicStrategy]),
 * sino una utilidad genérica e independiente del estado.
 */
object CardValues {

    /** Valor intrínseco [0..10] de una carta para tenerla en mano. */
    fun handValue(kind: CardKind): Int = when (kind) {
        // Recursos ofensivos y defensivos básicos
        BANG -> 5
        MISSED -> 7          // defensa escasa y crítica
        BEER -> 6            // vida/salvación

        // Control del rival
        PANIC -> 5
        CAT_BALOU -> 5
        DUEL -> 4

        // Robo de cartas (ventaja de material)
        STAGECOACH -> 6
        WELLS_FARGO -> 8

        // Área
        GATLING -> 8
        INDIANS -> 6
        GENERAL_STORE -> 4
        SALOON -> 3

        // Azules de control
        JAIL -> 4
        DYNAMITE -> 2        // arma de doble filo

        // Equipo pasivo
        BARREL -> 7
        MUSTANG -> 6
        SCOPE -> 5

        // Armas: cuanto más alcance, más valor
        VOLCANIC -> 7        // BANG! ilimitados
        SCHOFIELD -> 5
        REMINGTON -> 6
        REV_CARABINE -> 7
        WINCHESTER -> 8
    }

    fun handValue(card: Card): Int = handValue(card.kind)
}
