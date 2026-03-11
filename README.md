# hytale-mod-template-ui

## Overview

UI-oriented command flow with per-player session tracking and runtime diagnostics. This repository is a practical starting point for a Hytale mod.

## Main entrypoint

- Main class from manifest.json: net.hytaledepot.templates.mod.ui.UiModPlugin
- Includes asset pack: true

## Source layout

- Java sources: src/main/java
- Manifest: src/main/resources/manifest.json
- UI layout sample: src/main/resources/Server/UI/Pages/HdUiTemplate.page.json
- Runtime jar output: build/libs/hytale-mod-template-ui-1.0.0.jar

## Key classes

- UiModPlugin
- UiModSession
- UiModTemplate

## Commands

- /hduimod
- /hduimodstatus

## Build

1. Ensure the server jar is available in one of these locations:
   - HYTALE_SERVER_JAR
   - HYTALE_HOME/install/$patchline/package/game/latest/Server/HytaleServer.jar
   - workspace root HytaleServer.jar
   - libs/HytaleServer.jar
2. Run: ./gradlew clean build
3. Copy build/libs/hytale-mod-template-ui-1.0.0.jar into your server mods/ folder.

## License

MIT
