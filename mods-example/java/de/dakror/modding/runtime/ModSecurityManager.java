package de.dakror.modding.runtime;

import java.security.Permission;
import java.io.File;
import java.net.SocketPermission;
import java.net.NetPermission;
import java.lang.reflect.ReflectPermission;

/**
 * Security manager to restrict mod capabilities
 * Prevents mods from accessing sensitive system resources
 */
public class ModSecurityManager extends SecurityManager {
    
    private final ModSecurityContext currentContext = new ModSecurityContext();
    
    @Override
    public void checkPermission(Permission perm) {
        // Allow core game functionality
        String permName = perm.getName();
        
        // File system access
        if (perm instanceof FilePermission) {
            if (!isFileAccessAllowed(perm)) {
                throw new SecurityException("File access denied: " + permName);
            }
        }
        
        // Network access
        if (perm instanceof SocketPermission || perm instanceof NetPermission) {
            if (!isNetworkAccessAllowed()) {
                throw new SecurityException("Network access denied for mods");
            }
        }
        
        // Reflection permissions
        if (perm instanceof ReflectPermission) {
            if (!isReflectionAllowed(perm)) {
                throw new SecurityException("Reflection access denied: " + permName);
            }
        }
        
        // Runtime permissions
        if (permName.startsWith("runtime.")) {
            if (!isRuntimeAccessAllowed(permName)) {
                throw new SecurityException("Runtime access denied: " + permName);
            }
        }
    }
    
    @Override
    public void checkPermission(Permission perm, Object context) {
        // Check with context
        checkPermission(perm);
    }
    
    private boolean isFileAccessAllowed(Permission perm) {
        // Allow read access to mod directory and resources
        String path = perm.getName();
        
        // Allow if within mods directory or game resources
        return path.contains("mods/") || 
               path.contains("assets/") ||
               path.endsWith("-mod") ||
               perm.getActions().equals("read");
    }
    
    private boolean isNetworkAccessAllowed() {
        // By default, deny network access for mods
        // Could be configurable per mod
        return false;
    }
    
    private boolean isReflectionAllowed(Permission perm) {
        // Allow reflection within mod's own classes
        // But deny access to core game classes
        String permName = perm.getName();
        return !permName.contains("de.dakror.quarry"); // Block access to game classes
    }
    
    private boolean isRuntimeAccessAllowed(String permName) {
        // Allow setting system properties for mod configuration
        return permName.equals("runtime.setIO") ||
               permName.equals("runtime.setSecurityManager");
    }
    
    @Override
    public void checkExec(String cmd) {
        throw new SecurityException("Process execution denied for mods");
    }
    
    @Override
    public void checkLink(String lib) {
        throw new SecurityException("Native library loading denied for mods");
    }
    
    @Override
    public void checkRead(String file) {
        // Check file read permission
        if (!isFileAccessAllowed(new FilePermission(file, "read"))) {
            super.checkRead(file);
        }
    }
    
    @Override
    public void checkWrite(String file) {
        // Most mods shouldn't write outside their directory
        throw new SecurityException("File write access denied for mods: " + file);
    }
    
    @Override
    public void checkDelete(String file) {
        throw new SecurityException("File deletion denied for mods");
    }
    
    @Override
    public void checkConnect(String host, int port) {
        if (!isNetworkAccessAllowed()) {
            throw new SecurityException("Network connection denied: " + host + ":" + port);
        }
    }
    
    private static class ModSecurityContext {
        private String currentModId;
        
        public String getCurrentModId() {
            return currentModId;
        }
        
        public void setCurrentModId(String modId) {
            this.currentModId = modId;
        }
    }
}
