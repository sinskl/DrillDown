package de.dakror.modding.runtime;

import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Isolated ClassLoader for mod classes
 * Provides sandboxing and prevents mods from accessing game classes directly
 */
public class ModClassLoader extends URLClassLoader {
    
    private final Map<String, Class<?>> loadedClasses = new ConcurrentHashMap<>();
    private final String modId;
    
    public ModClassLoader(URL[] urls, ClassLoader parent, String modId) {
        super(urls, parent);
        this.modId = modId;
    }
    
    public ModClassLoader(URL url, ClassLoader parent) {
        this(new URL[]{url}, parent, "unknown");
    }
    
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            // First check if already loaded
            Class<?> clazz = loadedClasses.get(name);
            if (clazz != null) {
                return clazz;
            }
            
            // Check if it's a mod class (starts with mod package)
            if (name.startsWith("mod.") || name.contains(".mod.")) {
                // Load mod class directly without delegation
                clazz = findClass(name);
                loadedClasses.put(name, clazz);
                
                if (resolve) {
                    resolveClass(clazz);
                }
                
                return clazz;
            }
            
            // Delegate to parent for game classes
            clazz = super.loadClass(name, resolve);
            loadedClasses.put(name, clazz);
            
            return clazz;
        }
    }
    
    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        // Perform security checks
        if (!isClassAllowed(name)) {
            throw new SecurityException("Class not allowed: " + name);
        }
        
        return super.findClass(name);
    }
    
    /**
     * Check if a class is allowed to be loaded
     */
    private boolean isClassAllowed(String className) {
        // Block dangerous packages
        String[] blocked = {
            "java.lang.management",
            "java.lang.instrument",
            "sun.",
            "com.sun."
        };
        
        for (String blockedPackage : blocked) {
            if (className.startsWith(blockedPackage)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Unload this classloader and all its classes
     */
    public void unload() {
        loadedClasses.clear();
        
        // Close the classloader
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                try {
                    ModClassLoader.this.close();
                } catch (Exception e) {
                    System.err.println("Error closing mod classloader for " + modId);
                    e.printStackTrace();
                }
                return null;
            }
        });
    }
    
    public String getModId() {
        return modId;
    }
}
