# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Drill Down is a 2D factory building game built with LibGDX for Android and PC platforms. The game features resource management, automation, and complex production chains similar to Factorio.

## Build Commands

### Prerequisites
- 64-bit Java >= 11
- Android Studio
- Git submodules must be initialized: `git submodule update --init --recursive`
- Java keystore for code signing (configure in `gradle.properties`)

### Platform Builds
- **Android**: `gradlew android:assembleFullRelease`
- **PC/Desktop**: `gradlew desktop:dist`
- **Clean**: `gradlew clean`

### Development
- **Run Desktop**: `gradlew desktop:run`
- **Generate Eclipse project**: `gradlew eclipse`
- **Generate IntelliJ IDEA project**: `gradlew idea`

## Architecture

### Module Structure
- **core/**: Main game logic, shared between platforms
- **desktop/**: PC-specific implementation using LWJGL backend
- **android/**: Android-specific implementation
- **commons/**: Shared utilities (git submodule)
- **gdx-sfx/**: Sound effects library (git submodule)

### Core Game Architecture

**Scene Management**: LibGDX scene-based architecture
- `Quarry.java`: Main game class, extends GameBase
- `LoadingScreen`: Asset loading and initialization
- `MainMenu`: Main menu interface
- `Game`: Main gameplay scene
- `GameUi`: In-game UI overlay

**Entity-Component System**: Uses Ashley ECS framework
- Components in `structure/base/component/`
- Systems handle game logic updates

**Structure System**: Buildings and machines
- Base classes in `structure/base/`
- Specialized structures:
  - `producer/`: Production buildings (furnaces, assemblers, etc.)
  - `storage/`: Storage buildings (silo, warehouse, tank, etc.)
  - `logistics/`: Transport systems (conveyors, hoppers, etc.)
  - `power/`: Power generation and distribution

**Power System**: Complex power network simulation
- `PowerGrid`: Manages power networks
- `PowerNetwork`: Individual power networks
- Buildings connect via power lines and consume/produce power

**Resource System**: Items and fluids
- `Item`: Enum defining all game items
- `Layer`: Different transport layers (items, fluids, power)
- Storage components handle item/fluid inventory

### Key Design Patterns

**Structure Inheritance Hierarchy**:
- `Structure`: Base class for all buildings
- `Schema`: Defines structure properties and behavior
- `PausableStructure`: Structures that can be paused
- `ProducerStructure`: Buildings that convert inputs to outputs
- `StorageStructure`: Buildings that store items/fluids

**Component-Based Architecture**:
- `CInventory`: Item storage component
- `CTank`: Fluid storage component
- `CRecipeSlotStorage`: Recipe-based production component

**UI System**: LML (LibGDX Markup Language) for declarative UI
- UI definitions in `android/assets/ui/`
- Custom tag registrator in `de.dakror.gen.CustomTagRegistrator`

## Development Guidelines

When modifying the codebase:

1. **Structure Modifications**: New buildings should extend appropriate base classes in `structure/base/` and implement required interfaces
2. **Resource Additions**: Add new items to the `Item` enum and update related systems
3. **UI Changes**: Use LML for UI layouts, modify existing templates in `android/assets/ui/`
4. **Power Networks**: Power connections are managed automatically when placing structures
5. **Save Compatibility**: Maintain backward compatibility in save/load systems

## Important Files

- `core/src/de/dakror/quarry/Const.java`: Game constants and configuration
- `core/src/de/dakror/quarry/game/Item.java`: All game items and resources
- `core/src/de/dakror/quarry/structure/base/StructureType.java`: Structure type definitions
- `android/assets/i18n/`: Localization files for multiple languages