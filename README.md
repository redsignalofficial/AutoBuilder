# AutoBuilder – Advanced Fabric Minecraft Mod

[![Build AutoBuilder Mod](https://github.com/redsignalofficial/AutoBuilder/actions/workflows/build.yml/badge.svg)](https://github.com/redsignalofficial/AutoBuilder/actions/workflows/build.yml)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-brightgreen.svg)](https://fabricmc.net/)
[![Fabric](https://img.shields.io/badge/Fabric-Loader%200.20.1-blue.svg)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**AutoBuilder** is a high-performance, intelligent automated construction mod for **Minecraft Java Edition 1.21.11** built on **Fabric Loader** and **Fabric API**.

Designed for singleplayer and multiplayer, AutoBuilder automates structure reconstruction block-by-block using smooth client camera rotation, physical raycast validation, intelligent obstacle handling, and strict adherence to vanilla Minecraft interaction rules.

---

## 🌟 Key Features

### 📁 Schematic System
* **Multi-Format Support**:
  * Modern Sponge Schematics (`.schem` v1, v2, v3 with VarInt bit-packing and nested tags)
  * Legacy MCEdit Schematics (`.schematic` with legacy block ID/metadata mapping)
  * Vanilla Minecraft Structure Templates (`.nbt` structure blocks format)
* **In-Game Clipboard**: Copy any world selection directly into your active clipboard.
* **Asynchronous I/O**: Load and save schematics in the background without frame drops or game freezes.
* **Schematic Manager Screen**: Browse, preview dimensions, block counts, rename, and delete schematics directly in-game.

### 📐 Selection System
* Select Position 1 and Position 2 using keybinds (`Z`, `X`), commands, or crosshair targeting.
* Dynamic 3D bounding box rendering with distinct color markers for Pos 1 (Blue) and Pos 2 (Green).
* Automatic volume calculation and configurable maximum selection limits to prevent memory exhaustion.

### 🏗️ Auto Build Engine
* **Physical Placement Order**:
  * Bottom-to-top layer-by-layer construction.
  * **Gravity Block Support**: Sand, gravel, concrete powder, and anvils are strictly placed only after a solid supporting block exists underneath.
  * **Dependent Block Support**: Torches, ladders, redstone, doors, levers, buttons, and carpets are placed only after their supporting blocks are secured.
  * Distance sorting to build reachable blocks naturally.
* **Control**: Pause, resume, and cancel construction at any time.

### 🎥 Smooth Camera Rotation
* Smoothly interpolates the player's camera pitch and yaw toward the target block face before placing.
* Configurable rotation speed (°/tick) to prevent sudden jerky movements.
* Strictly aligns client view with placement targets, complying with vanilla reach and raycasting physics.

### ⛏️ Obstacle Detection & Permitted Removal
* Detects existing unwanted blocks obstructing construction.
* **Safe Mining Rules**:
  * Unbreakable blocks (Bedrock, Barrier, End Portal) are strictly never broken.
  * Containers & Block Entities (Chests, Shulker Boxes, Furnaces, Hoppers) are strictly protected by default unless explicitly allowed in config.
  * Respects player game mode and permissions.
  * Automatically switches to the most effective tool in the player's hotbar.

### 🛡️ Placement Validation & Multiplayer Compatibility
* **Legitimate Interactions**: Uses normal client interaction packets (`interactBlock`, `updateBlockBreakingProgress`, `swingHand`).
* **Placement Verification**: Listens for world block changes to confirm server acceptance. Detects server rollbacks/rejections and avoids ghost placements.
* Does not spoof packets or fake player actions.

### 🔮 Hologram Preview
* Semi-transparent ghost preview rendered in real time.
* Color-coded feedback:
  * 🟢 **Green**: Correctly placed blocks.
  * 🔴 **Red**: Obstacles requiring clearance.
  * 🟡 **Yellow**: Missing materials in player inventory.
  * 🔵 **Cyan**: Blocks ready for placement.
* Real-time structure manipulation: nudge X/Y/Z, rotate 90° CW/CCW, mirror along X/Z axis.

### 📊 In-Game Progress HUD
* Compact, stylish on-screen HUD displaying:
  * Current status (`PLACING`, `MINING_OBSTACLE`, `ALIGNING_VIEW`, `PAUSED`, `DONE`).
  * Live progress bar, block count, and percentage.
  * Current layer Y and next block name.

---

## ⌨️ Controls & Keybindings

| Key | Action |
|:---:|:---|
| <kbd>B</kbd> | Open AutoBuilder Control Panel |
| <kbd>K</kbd> | Toggle Construction (Pause / Resume) |
| <kbd>Z</kbd> | Set Selection Position 1 |
| <kbd>X</kbd> | Set Selection Position 2 |

*All keybindings can be customized in Options → Controls → Key Binds.*

---

## 💬 Client Commands

All commands support both `/autobuilder` and `/ab`:

* `/ab gui` – Opens the main AutoBuilder control screen.
* `/ab pos1` – Sets Position 1 to your feet or targeted block.
* `/ab pos2` – Sets Position 2 to your feet or targeted block.
* `/ab clear` – Clears current selection.
* `/ab copy` – Copies current selection to the clipboard.
* `/ab save <name>` – Saves active clipboard schematic locally to `.minecraft/schematics/autobuilder/<name>.schem`.
* `/ab load <name>` – Loads a saved schematic.
* `/ab preview` – Toggles hologram preview on/off.
* `/ab build` – Starts constructing the active schematic.
* `/ab pause` – Pauses ongoing construction.
* `/ab resume` – Resumes paused construction.
* `/ab stop` – Stops and cancels construction.
* `/ab config` – Opens the configuration screen.

---

## ⚙️ Configuration (`autobuilder.json`)

Saved in `.minecraft/config/autobuilder.json`:

```json
{
  "cameraRotationSpeed": 15.0,
  "placementDelayTicks": 3,
  "miningDelayTicks": 4,
  "maxBuildReach": 4.2,
  "breakObstacles": true,
  "breakContainers": false,
  "pauseOnMissing": true,
  "skipUnavailable": false,
  "autoResume": true,
  "maxSelectionBlocks": 500000,
  "smoothCamera": true,
  "layerByLayer": true
}
```

---

## 🛠️ Building & Compilation

AutoBuilder is configured to compile automatically via **GitHub Actions** on every push.

To build manually with Gradle:

```bash
git clone https://github.com/redsignalofficial/AutoBuilder.git
cd AutoBuilder
./gradlew build
```

The resulting mod jar file will be located in `build/libs/`.

---

## 📜 Architecture

Organized cleanly into modular Java packages:

```
dev.autobuilder
├── AutoBuilder.java             # Mod entrypoint
├── client/
│   ├── AutoBuilderClient.java   # Client initialization & event hooks
│   └── render/
│       ├── SelectionRenderer.java
│       ├── HologramRenderer.java
│       └── BuildProgressHud.java
├── command/
│   └── AutoBuilderCommands.java # Brigadier client commands
├── selection/
│   ├── CuboidRegion.java
│   └── SelectionManager.java
├── schematic/
│   ├── Schematic.java
│   ├── SchematicManager.java
│   └── format/
│       ├── ISchematicHandler.java
│       ├── SpongeSchematicHandler.java
│       ├── LegacySchematicHandler.java
│       └── VanillaStructureHandler.java
├── building/
│   ├── BuildState.java
│   ├── BuildTask.java
│   ├── BuildOrderCalculator.java
│   ├── PlacementValidator.java
│   └── BuildManager.java
├── camera/
│   ├── RotationMath.java
│   └── CameraController.java
├── interaction/
│   ├── InventoryHelper.java
│   ├── RaycastHelper.java
│   └── InteractionController.java
├── obstacle/
│   ├── ObstaclePolicy.java
│   ├── ObstacleDetector.java
│   └── ObstacleRemovalManager.java
├── gui/
│   ├── AutoBuilderScreen.java
│   ├── SchematicManagerScreen.java
│   ├── PreviewControlScreen.java
│   └── ConfigScreen.java
├── config/
│   ├── ModConfig.java
│   └── ConfigManager.java
├── network/
│   └── PlacementTracker.java
└── util/
    ├── BlockUtils.java
    ├── MathUtils.java
    └── TextConstants.java
```

---

## ⚖️ License

Distributed under the [MIT License](LICENSE).
