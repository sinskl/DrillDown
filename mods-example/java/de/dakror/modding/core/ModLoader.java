package de.dakror.modding.core;

import de.dakror.modding.api.Mod;
import de.dakror.modding.api.registry.Registry;
import de.dakror.modding.runtime.ModClassLoader;
import de.dakror.modding.runtime.ModSecurityManager;
import de.dakror.modding.runtime.EventBus;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Main ModLoader class responsible for loading and managing mods
 */
public class ModLoader {
    
    private static ModLoader instance;
    
    private final Path modsDirectory;
    private final Map<String, Mod> loadedMods = new ConcurrentHashMap<>();
    private final Map<String, Exception> modErrors = new ConcurrentHashMap<>();
    private final List<Registry<?>> registries = new CopyOnWriteArrayList<>();
    private final EventBus eventBus = new EventBus();
    private final ModSecurityManager securityManager;
    
    private ModLoader(Path modsDirectory) {
        this.modsDirectory = modsDirectory;
        this.securityManager = new ModSecurityManager();
        
        // Install security manager
        System.setSecurityManager(securityManager);
    }
    
    public static synchronized ModLoader getInstance() {
        if (instance == null) {
            instance = new ModLoader(Paths.get("mods"));
        }
        return instance;
    }
    
    public static synchronized ModLoader getInstance(Path modsDirectory) {
        if (instance == null) {
            instance = new ModLoader(modsDirectory);
        }
        return instance;
    }
    
    /**
     * Load all mods from the mods directory
     */
    public void loadAllMods() throws IOException {
        if (!Files.exists(modsDirectory)) {
            Files.createDirectories(modsDirectory);
            return;
        }
        
        List<Path> modJars = findModJars();
        Map<Mod, Path> modPaths = new HashMap<>();
        
        // Load mod metadata first
        for (Path jarPath : modJars) {
            try {
                Mod mod = loadModFromJar(jarPath);
                if (mod != null) {
                    modPaths.put(mod, jarPath);
                    System.out.println("Found mod: " + mod.getName() + " v" + mod.getVersion());
                }
            } catch (Exception e) {
                System.err.println("Failed to load mod from " + jarPath);
                e.printStackTrace();
            }
        }
        
        // Sort by load priority
        List<Mod> sortedMods = new ArrayList<>(modPaths.keySet());
        sortedMods.sort(Comparator.comparingInt(Mod::getLoadPriority));
        
        // Initialize mods in order
        for (Mod mod : sortedMods) {
            try {
                mod.onLoad();
                loadedMods.put(mod.getId(), mod);
                eventBus.post(new ModLoadedEvent(mod, modPaths.get(mod)));
                
                System.out.println("Successfully loaded mod: " + mod.getName());
            } catch (Exception e) {
                System.err.println("Failed to initialize mod: " + mod.getId());
                e.printStackTrace();
                modErrors.put(mod.getId(), e);
            }
        }
    }
    
    /**
     * Load a single mod from a JAR file
     */
    private Mod loadModFromJar(Path jarPath) throws Exception {
        ModClassLoader classLoader = new ModClassLoader(jarPath.toUri().toURL(), 
                                                        getClass().getClassLoader());
        
        // Read mod.json to get main class
        String mainClass = readModJson(jarPath);
        if (mainClass == null) {
            throw new IllegalStateException("No mod.json found in " + jarPath);
        }
        
        // Load the mod class
        Class<?> modClass = classLoader.loadClass(mainClass);
        if (!Mod.class.isAssignableFrom(modClass)) {
            throw new IllegalStateException("Main class must implement Mod interface");
        }
        
        // Create instance
        Mod mod = (Mod) modClass.getDeclaredConstructor().newInstance();
        
        // Check version compatibility
        if (!isVersionCompatible(mod)) {
            throw new IllegalStateException("Mod version incompatible with game version");
        }
        
        return mod;
    }
    
    /**
     * Check if mod is compatible with current game version
     */
    private boolean isVersionCompatible(Mod mod) {
        String gameVersion = "1.0"; // Get from Quarry.Q.version
        String minVersion = mod.getMinGameVersion();
        String maxVersion = mod.getMaxGameVersion();
        
        if (minVersion != null && minVersion.compareTo(gameVersion) > 0) {
            return false;
        }
        
        if (maxVersion != null && maxVersion.compareTo(gameVersion) < 0) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Read mod.json and extract main class name
     */
    private String readModJson(Path jarPath) {
        // Simplified - in real implementation, parse JSON properly
        // This should read from the JAR and extract the "main" field
        return "com.example.mod.ExampleMod"; // Placeholder
    }
    
    /**
     * Find all mod JAR files in the mods directory
     */
    private List<Path> findModJars() throws IOException {
        List<Path> jars = new ArrayList<>();
        Files.walk(modsDirectory)
            .filter(path -> path.toString().endsWith(".jar"))
            .forEach(jars::add);
        return jars;
    }
    
    /**
     * Get a mod by ID
     */
    public Mod getMod(String id) {
        return loadedMods.get(id);
    }
    
    /**
     * Get all loaded mods
     */
    public Collection<Mod> getLoadedMods() {
        return new ArrayList<>(loadedMods.values());
    }
    
    /**
     * Register a registry
     */
    public <T extends Identifiable> void register(Registry<T> registry) {
        registries.add(registry);
    }
    
    /**
     * Get the event bus
     */
    public EventBus getEventBus() {
        return eventBus;
    }
    
    /**
     * Unload all mods
     */
    public void unloadAllMods() {
        for (Mod mod : loadedMods.values()) {
            try {
                mod.onUnload();
            } catch (Exception e) {
                System.err.println("Error unloading mod: " + mod.getId());
                e.printStackTrace();
            }
        }
        loadedMods.clear();
    }
    
    /**
     * Reload a specific mod
     */
    public void reloadMod(String modId) throws Exception {
        Mod oldMod = loadedMods.get(modId);
        if (oldMod == null) {
            throw new IllegalArgumentException("Mod not found: " + modId);
        }
        
        // Unload old version
        oldMod.onUnload();
        loadedMods.remove(modId);
        
        // Reload from original JAR
        // In a real implementation, we'd store the jar path
        // For now, just re-initialize
        oldMod.onLoad();
        loadedMods.put(modId, oldMod);
    }
}

/**
 * Event fired when a mod is successfully loaded
 */
class ModLoadedEvent {
    public final Mod mod;
    public final Path modJarPath;
    
    public ModLoadedEvent(Mod mod, Path modJarPath) {
        this.mod = mod;
        this.modJarPath = modJarPath;
    }
}
