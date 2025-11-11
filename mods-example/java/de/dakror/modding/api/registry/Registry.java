package de.dakror.modding.api.registry;

import java.util.Collection;

/**
 * Generic registry interface for registering game objects
 */
public interface Registry<T extends Identifiable> {
    
    /**
     * Register an object with the given ID
     */
    void register(String id, T entry);
    
    /**
     * Get an object by ID
     */
    T get(String id);
    
    /**
     * Get all registered objects
     */
    Collection<T> getAll();
    
    /**
     * Check if an ID is registered
     */
    boolean contains(String id);
    
    /**
     * Remove an object by ID
     */
    T unregister(String id);
    
    /**
     * Get the number of registered objects
     */
    int size();
}
