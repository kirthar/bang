# ¡BANG! para Android

Implementación no oficial del juego de cartas BANG! como app Android nativa
(Kotlin + Jetpack Compose), para jugar contra IAs y preparada para partidas online
en el futuro. Arte propio estilo western (sin assets con copyright de DV Giochi).

## Módulos

- `core/` — dominio y motor de reglas (Kotlin puro, reutilizable en servidor).
- `ai/` — IAs de los rivales: fácil (aleatoria), media (heurística), difícil (ISMCTS).
- `app/` — aplicación Android (Compose, MVVM, español).

Documentación: [docs/REGLAS.md](docs/REGLAS.md) · [docs/ARQUITECTURA.md](docs/ARQUITECTURA.md)

## Compilar

Requisitos: JDK 17+ y Android SDK (platform 35). `local.properties` debe apuntar al
SDK (`sdk.dir=...`).

```bash
./gradlew :core:test :ai:test    # tests del motor y las IAs (JVM puro)
./gradlew :app:assembleDebug     # APK de debug
```
