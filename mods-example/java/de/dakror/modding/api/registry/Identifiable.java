package de.dakror.modding.api.registry;

/**
 * Interface for objects that can be identified by ID
 */
public interface Identifiable {
    /**
     * Get the unique ID for this object
     */
    String getId();
    
    /**
     * Get the display name for this object
     */
    String getName();
}
