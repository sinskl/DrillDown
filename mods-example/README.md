# DrillDown ModLoader - Example Implementation

This directory contains example code demonstrating how to implement a ModLoader for the DrillDown game.

## Architecture Overview

The ModLoader is built on top of DrillDown's existing Java Agent infrastructure (`StubAgent.java`), which provides:
- Bytecode transformation capabilities
- Class redefinition support
- Dynamic class loading

## Core Components

### 1. Mod Interface (`api/Mod.java`)
Base interface that all mods must implement:
- Lifecycle methods: `onLoad()`, `onUnload()`, `onConfigReload()`
- Metadata: `getId()`, `getName()`, `getVersion()`
- Compatibility: `getMinGameVersion()`, `getMaxGameVersion()`

### 2. Registry System (`api/registry/`)
Generic registry system for registering game objects:
- `Identifiable`: Base interface for objects that can be registered
- `Registry<T>`: Generic registry interface

### 3. Event Bus (`runtime/EventBus.java`)
Simple publish-subscribe event system:
- Automatic handler discovery via `@Subscribe` annotation
- Thread-safe event distribution
- Mod-to-mod communication

### 4. Class Loading (`runtime/ModClassLoader.java`)
Isolated class loader for mod classes:
- Prevents mod code from accessing sensitive game classes
- Supports hot reloading
- Tracks loaded mod classes

### 5. Security (`runtime/ModSecurityManager.java`)
Security manager to restrict mod capabilities:
- File system access control
- Network access denial
- Reflection restrictions
- Runtime permission checking

### 6. Main Loader (`core/ModLoader.java`)
Central mod management system:
- Discovers and loads mod JARs
- Manages mod lifecycle
- Handles dependencies and versioning
- Manages registries

## Mod Structure

A typical mod JAR contains:

```
mod.jar
├── mod.json                 # Mod metadata
├── com/example/mod/         # Mod classes
│   ├── ExampleMod.class
│   ├── ExampleClientMod.class
│   └── ExampleServerMod.class
└── resources/              # Mod assets
    ├── textures/
    ├── sounds/
    └── lang/
```

### mod.json

```json
{
  "id": "example",
  "name": "Example Mod",
  "version": "1.0.0",
  "main": "com.example.mod.ExampleMod",
  "dependencies": ["core"],
  "clientOnly": false,
  "serverOnly": false
}
```

## Example Usage

### Creating a Mod

```java
public class MyMod implements Mod {
    @Override
    public void onLoad() {
        // Register new buildings, items, recipes
        // Subscribe to events
        // Add UI elements
    }
    
    @Subscribe
    public void onBuildingPlaced(BuildingPlacedEvent event) {
        // Handle game events
    }
}
```

### Event Handling

```java
@Subscribe
public void onGameInit(GameInitEvent event) {
    System.out.println("Game starting!");
}
```

### Loading the ModLoader

```java
// In Quarry.java, after assets are loaded
ModLoader loader = ModLoader.getInstance();
loader.loadAllMods();
```

## Implementation Status

### Completed ✅
- [x] Mod interface and lifecycle management
- [x] Basic class loading and isolation
- [x] Event bus with annotations
- [x] Security manager foundation
- [x] Registry system design

### To Implement 🔄
- [ ] ASM bytecode transformation
- [ ] Actual game integration
- [ ] Hot reloading mechanism
- [ ] Building/Item registration APIs
- [ ] Resource injection system
- [ ] Configuration file support
- [ ] Dependency resolution
- [ ] UI integration

## Next Steps

1. **Integrate with Quarry.java**
   - Add ModLoader initialization to game startup
   - Connect with existing LML UI system
   - Hook into asset loading

2. **Implement ASM Transformations**
   - Transform game classes to support mod hooks
   - Add event injection points
   - Support field/method injection

3. **Build Registration APIs**
   - Create BuildingRegistry
   - Create ItemRegistry
   - Create RecipeRegistry

4. **Add Resource Support**
   - Texture injection
   - Audio registration
   - UI template extension

## Security Considerations

The ModSecurityManager currently:
- ✅ Blocks file system write access
- ✅ Blocks network access
- ✅ Restricts reflection
- ✅ Prevents native library loading
- ✅ Blocks process execution

Future improvements:
- Add code signing verification
- Implement resource usage monitoring
- Add mod-specific permission granting
- Support trusted mod certificates

## Performance

Optimizations to consider:
- Lazy loading of mod assets
- Async event processing
- Class loading cache
- Memory usage monitoring

## Testing

The example mod demonstrates:
- Basic mod lifecycle
- Event subscription
- Security boundaries
- Error handling

## References

- [Java Instrument API Documentation](https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/package-summary.html)
- [ASM Bytecode Manipulation Framework](https://asm.ow2.io/)
- [LibGDX Documentation](https://libgdx.com/)
- [ServiceLoader Documentation](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html)
