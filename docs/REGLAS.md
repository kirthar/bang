# Reglas de BANG! (juego base) — referencia para la implementación

Resumen fiel de las reglas oficiales del juego base (4-7 jugadores). Es la fuente de
verdad para el motor, las IAs y la UI.

## 1. Objetivo y roles

Cada jugador recibe un rol secreto:

| Rol | Cantidad (4/5/6/7 jug.) | Objetivo |
|---|---|---|
| Sheriff | 1/1/1/1 (público) | Eliminar a todos los Forajidos y al Renegado |
| Alguacil (Deputy) | 0/1/1/2 | Proteger al Sheriff; ganan con él |
| Forajido (Outlaw) | 2/2/3/3 | Matar al Sheriff |
| Renegado | 1/1/1/1 | Ser el último en pie (matar al Sheriff el último, en un mano a mano final) |

- El Sheriff revela su rol y empieza la partida. Los demás roles solo se revelan al
  morir su dueño o al acabar la partida.
- **Fin de partida**: (a) muere el Sheriff → si solo queda vivo el Renegado, gana el
  Renegado; si no, ganan los Forajidos; (b) mueren todos los Forajidos y el Renegado →
  ganan el Sheriff y los Alguaciles. Los ganadores lo son aunque estén muertos.

## 2. Personajes y vidas

Cada jugador recibe un personaje con habilidad especial y 3 o 4 vidas (balas).
**El Sheriff juega con una vida adicional**. Las vidas actuales marcan además el
límite de mano al final del turno (§4.3).

Los 16 personajes (vidas base entre paréntesis):

1. **Bart Cassidy (4)** — cada vez que pierde un punto de vida, roba 1 carta del mazo.
2. **Black Jack (4)** — en su fase de robar, enseña la 2ª carta robada; si es Corazones
   o Diamantes, roba 1 carta más (también enseñada... solo la 2ª se enseña; la 3ª no).
3. **Calamity Janet (4)** — puede jugar sus BANG! como ¡Fallaste! y viceversa.
4. **El Gringo (3)** — cada vez que un jugador le hace perder vida, roba 1 carta al
   azar de la mano de ese jugador (la Dinamita no es "un jugador").
5. **Jesse Jones (4)** — puede robar su 1ª carta de la fase de robar de la mano de un
   jugador cualquiera (al azar); la 2ª siempre del mazo.
6. **Jourdonnais (4)** — se considera que tiene un Barril en juego siempre; puede
   acumularlo con un Barril real (dos intentos de «¡desenfunda!»).
7. **Kit Carlson (4)** — en su fase de robar mira las 3 primeras cartas del mazo,
   se queda 2 y devuelve 1 boca abajo encima del mazo.
8. **Lucky Duke (4)** — en cada «¡desenfunda!» voltea 2 cartas, elige la que prefiera
   y descarta ambas.
9. **Paul Regret (3)** — los demás lo ven a distancia +1 (acumulable con Mustang).
10. **Pedro Ramírez (4)** — puede robar su 1ª carta de la cima de los descartes;
    la 2ª siempre del mazo.
11. **Rose Doolan (4)** — ve a los demás a distancia -1 (acumulable con la Mira).
12. **Sid Ketchum (4)** — en cualquier momento (incluso fuera de su turno) puede
    descartar 2 cartas de la mano para recuperar 1 vida. En la práctica del motor:
    en su turno, o como reacción cuando fuese a morir.
13. **Slab the Killer (4)** — para cancelar un BANG! suyo hacen falta 2 ¡Fallaste!
    (el Barril cuenta como uno de ellos).
14. **Suzy Lafayette (4)** — en cuanto se queda sin cartas en la mano, roba 1 del mazo
    (no aplica si está muerta).
15. **Vulture Sam (4)** — cuando un personaje es eliminado, toma en su mano todas las
    cartas (mano + en juego) de ese jugador.
16. **Willy the Kid (4)** — puede jugar tantos BANG! como quiera en su turno.

## 3. Preparación

1. Repartir roles según la tabla (§1). 2. Cada jugador recibe un personaje.
3. Vidas = balas del personaje (+1 el Sheriff). 4. Barajar el mazo de 80 cartas y
repartir a cada jugador tantas cartas como vidas tiene. 5. Empieza el Sheriff y se
juega en el sentido de las agujas del reloj.

## 4. El turno

### 4.1 Inicio de turno: cartas azules pendientes

En este orden, **antes de robar**:

1. **Dinamita**: si la tiene delante, hace «¡desenfunda!»: si sale Picas 2-9, la
   Dinamita explota → pierde 3 vidas y la Dinamita se descarta. Si no, la pasa al
   jugador de su izquierda (que hará lo mismo en su turno).
2. **Cárcel**: si está en la Cárcel, hace «¡desenfunda!»: con Corazones escapa,
   descarta la Cárcel y juega normalmente; si no, descarta la Cárcel y **pierde el
   turno** (la Dinamita ya se resolvió y sigue circulando). El Sheriff no puede ser
   encarcelado.

### 4.2 Fases del turno

1. **Robar**: roba 2 cartas del mazo (o aplica la habilidad del personaje).
2. **Jugar**: juega cualquier número de cartas, con dos límites:
   - solo **1 BANG! por turno** (salvo Volcanic en juego o Willy the Kid);
   - no puede tener delante dos cartas azules con el mismo nombre, y **solo 1 arma**
     en juego (jugar otra descarta la anterior).
3. **Descartar**: si tiene más cartas en mano que vidas actuales, descarta el exceso.

Si el mazo se agota, se rebarajan los descartes (dejando la última carta descartada
como nueva pila de descartes).

## 5. Distancias y armas

- La distancia entre dos jugadores es el mínimo número de asientos entre ellos
  contando solo **jugadores vivos**, en cualquiera de los dos sentidos.
- Solo se puede disparar (BANG!) a jugadores a distancia ≤ alcance del arma.
- Sin arma en juego se usa el **Colt .45** (alcance 1).
- Armas: Volcanic (1, BANG! ilimitados), Schofield (2), Remington (3),
  Rev. Carabine (4), Winchester (5).
- Modificadores: **Mustang** (+1 a cómo te ven los demás), **Mira/Scope** (-1 a cómo
  ves tú a los demás), Paul Regret (+1), Rose Doolan (-1). La distancia mínima es 1.
- Panic! exige distancia 1 (¡ojo!: distancia de visión, con modificadores);
  Cat Balou no tiene límite de distancia.

## 6. «¡Desenfunda!» (draw!)

Cuando una carta o habilidad lo pide (Barril, Dinamita, Cárcel), se voltea la primera
carta del mazo, se aplica su palo/valor y se descarta. Lucky Duke voltea 2 y elige.

- **Barril**: con Corazones, cuenta como un ¡Fallaste! contra el BANG! recibido.
  Solo un intento por BANG! (Jourdonnais + Barril real = 2 intentos).
- **Dinamita**: Picas 2-9 → explota (3 vidas). — **Cárcel**: Corazones → escapa.

## 7. Cartas marrones (efecto inmediato)

| Carta | Cant. | Efecto |
|---|---|---|
| BANG! | 25 | 1 golpe a un jugador al alcance del arma; se cancela con ¡Fallaste! |
| ¡Fallaste! | 12 | Cancela un BANG! (solo se juega como reacción) |
| Birra | 6 | Recupera 1 vida (máx. las vidas iniciales). **Sin efecto con solo 2 jugadores vivos**. También se puede jugar fuera de turno en el instante de recibir un golpe mortal para no morir |
| Panic! | 4 | Roba 1 carta (mano al azar o en juego a elección) a un jugador a distancia 1 |
| Cat Balou | 4 | Descarta 1 carta (mano al azar o en juego a elección) de cualquier jugador |
| Diligencia | 2 | Roba 2 cartas del mazo |
| Wells Fargo | 1 | Roba 3 cartas del mazo |
| Gatling | 1 | Un BANG! contra todos los demás jugadores (no cuenta para el límite de BANG!) |
| ¡Indios! | 2 | Todos los demás descartan un BANG! o pierden 1 vida (el Barril y ¡Fallaste! no sirven; Calamity Janet puede descartar ¡Fallaste! como BANG!) |
| Duelo | 3 | Reta a un jugador: empezando por el retado, alternan descartando BANG!; el primero que no lo haga pierde 1 vida. (Los ¡Fallaste!/Barril no sirven; no cuenta para el límite de BANG!) |
| Emporio | 2 | Se revelan tantas cartas como jugadores vivos; empezando por quien la jugó, cada uno toma una |
| Salón | 1 | Todos los jugadores vivos recuperan 1 vida (quien la juega incluido). No es una Birra: no salva de un golpe mortal |

### Resolución de un BANG! (o Gatling, que es un BANG! a cada rival)

1. El objetivo puede usar Barril (si tiene, «¡desenfunda!»).
2. Si sigue necesitando cancelar, puede jugar ¡Fallaste! de la mano
   (2 en total contra Slab the Killer; el Barril cuenta como 1).
3. Si no cancela, pierde 1 vida (y se aplican habilidades: Bart Cassidy, El Gringo).

## 8. Cartas azules (permanecen en juego)

- **Barril, Mustang, Mira**: efecto pasivo mientras estén delante.
- **Armas**: sustituyen al Colt .45; solo una en juego.
- **Cárcel**: se juega delante de **otro** jugador (no el Sheriff). Ver §4.1.
- **Dinamita**: se juega delante de uno mismo y circula. Ver §4.1.
- Las pérdidas de vida por Dinamita no las causa ningún jugador (sin habilidades de
  represalia ni recompensas).

## 9. Muerte, recompensas y penalizaciones

- Un jugador con 0 vidas puede salvarse **en ese instante** jugando Birras (1 por
  cada vida por debajo de 1). Si no, queda eliminado: revela su rol y descarta todo
  (o lo toma Vulture Sam).
- **Recompensa**: quien elimine a un **Forajido** (aunque sea otro Forajido) roba
  3 cartas del mazo.
- **Penalización**: si el **Sheriff** elimina a un **Alguacil**, el Sheriff descarta
  toda su mano y sus cartas en juego.
- Al morir un jugador, la Birra ya no puede salvarlo si ya quedó eliminado; las
  distancias se recalculan sin él.

## 10. Detalles finos que el motor debe respetar

1. El límite de mano al final del turno = vidas **actuales**.
2. La vida máxima recuperable = vidas iniciales (maxHealth).
3. Los efectos «todos los jugadores» (Gatling, Indios, Salón, Emporio) se resuelven
   en orden de turno empezando por el jugador activo (o su izquierda para reacciones).
4. Solo puede reaccionarse con cartas de reacción a lo que las reglas permiten:
   ¡Fallaste! solo contra BANG!/Gatling; BANG! solo como respuesta en Duelo/Indios.
5. Un jugador en la Cárcel pierde solo la fase de su turno; puede seguir reaccionando
   (¡Fallaste!, Birra…) fuera de turno.
6. La partida puede terminar en mitad de una resolución (p. ej. Duelo que mata al
   Sheriff): se comprueba la condición de victoria tras **cada** pérdida de vida o
   eliminación.
7. Con 2 jugadores vivos, la Birra deja de tener efecto (incluida la que salva de
   golpe mortal).
