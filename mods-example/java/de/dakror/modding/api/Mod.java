package de.dakror.modding.api;

import java.util.List;

/**
 * Base interface for all mods in DrillDown
 */
public interface Mod {
    // Core information
    String getId();
    String getName();
    String getVersion();
    String getDescription();
    String getAuthor();
    
    // Lifecycle methods
    void onLoad();
    void onUnload();
    void onConfigReload();
    
    // Dependencies and compatibility
    List<String> getDependencies();
    String getMinGameVersion();
    String getMaxGameVersion();
    
    // Client/Server specific
    boolean isClientOnly();
    boolean isServerOnly();
    
    // Mod initialization order (lower loads first)
    default int getLoadPriority() {
        return 100;
    }
}
