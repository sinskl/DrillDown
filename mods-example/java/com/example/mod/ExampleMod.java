package com.example.mod;

import de.dakror.modding.api.Mod;
import de.dakror.modding.runtime.EventBus;
import de.dakror.modding.runtime.ModEvent;

import java.util.Arrays;
import java.util.List;

/**
 * Example mod demonstrating ModLoader API usage
 */
public class ExampleMod implements Mod {
    
    @Override
    public String getId() {
        return "example";
    }
    
    @Override
    public String getName() {
        return "Example Mod";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public String getDescription() {
        return "A simple example mod that demonstrates the ModLoader API";
    }
    
    @Override
    public String getAuthor() {
        return "ModLoader Example";
    }
    
    @Override
    public List<String> getDependencies() {
        return Arrays.asList("core");
    }
    
    @Override
    public String getMinGameVersion() {
        return "1.0.0";
    }
    
    @Override
    public String getMaxGameVersion() {
        return "2.0.0";
    }
    
    @Override
    public boolean isClientOnly() {
        return false;
    }
    
    @Override
    public boolean isServerOnly() {
        return false;
    }
    
    @Override
    public void onLoad() {
        System.out.println("[ExampleMod] Loading mod...");
        
        // Subscribe to events
        EventBus eventBus = de.dakror.modding.core.ModLoader.getInstance().getEventBus();
        eventBus.subscribe(this);
        
        // In a real implementation, you would:
        // - Register new buildings, items, recipes
        // - Add UI elements
        // - Hook into game events
        
        System.out.println("[ExampleMod] Mod loaded successfully!");
    }
    
    @Override
    public void onUnload() {
        System.out.println("[ExampleMod] Unloading mod...");
        // Clean up resources
        System.out.println("[ExampleMod] Mod unloaded!");
    }
    
    @Override
    public void onConfigReload() {
        System.out.println("[ExampleMod] Configuration reloaded");
    }
    
    /**
     * Example event handler
     * This method will be automatically called when a GameInitEvent is posted
     */
    @Subscribe
    public void onGameInit(GameInitEvent event) {
        System.out.println("[ExampleMod] Game initialized!");
    }
}

/**
 * Example event class
 */
class GameInitEvent extends ModEvent {
    public GameInitEvent(Object source) {
        super(source);
    }
}
