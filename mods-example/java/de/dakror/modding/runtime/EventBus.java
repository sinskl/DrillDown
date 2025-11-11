package de.dakror.modding.runtime;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple event bus implementation for mod communication
 */
public class EventBus {
    
    private final Map<Class<?>, List<EventHandler>> handlers = new ConcurrentHashMap<>();
    
    public interface Event {
        Object getSource();
    }
    
    @FunctionalInterface
    public interface EventHandler<T extends Event> {
        void handle(T event);
    }
    
    /**
     * Subscribe an object to receive events
     * Methods annotated with @Subscribe will be automatically called
     */
    public void subscribe(Object subscriber) {
        Map<Class<?>, List<Method>> methods = findSubscribeMethods(subscriber);
        methods.forEach((eventType, methodList) -> {
            for (Method method : methodList) {
                registerHandler(eventType, (event) -> {
                    try {
                        method.invoke(subscriber, event);
                    } catch (Exception e) {
                        throw new RuntimeException("Error handling event", e);
                    }
                });
            }
        });
    }
    
    /**
     * Unsubscribe an object from receiving events
     */
    public void unsubscribe(Object subscriber) {
        // Remove all handlers for methods belonging to this subscriber
        handlers.values().forEach(handlerList -> {
            handlerList.removeIf(handler -> 
                handler.subscriber == subscriber
            );
        });
    }
    
    /**
     * Register a handler for a specific event type
     */
    @SuppressWarnings("unchecked")
    public <T extends Event> void registerHandler(Class<T> eventType, EventHandler<T> handler) {
        handlers.computeIfAbsent(eventType, k -> new ArrayList<>())
                .add((EventHandler<Event>) handler);
    }
    
    /**
     * Post an event to all registered handlers
     */
    @SuppressWarnings("unchecked")
    public void post(Event event) {
        List<EventHandler> handlers = this.handlers.get(event.getClass());
        if (handlers != null) {
            for (EventHandler handler : handlers) {
                try {
                    handler.handle(event);
                } catch (Exception e) {
                    System.err.println("Error in event handler: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Find all methods annotated with @Subscribe
     */
    private Map<Class<?>, List<Method>> findSubscribeMethods(Object subscriber) {
        Map<Class<?>, List<Method>> methods = new HashMap<>();
        Class<?> clazz = subscriber.getClass();
        
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Subscribe.class)) {
                Class<?>[] params = method.getParameterTypes();
                if (params.length != 1) {
                    throw new IllegalArgumentException("@Subscribe method must have exactly one parameter");
                }
                
                Class<?> eventType = params[0];
                methods.computeIfAbsent(eventType, k -> new ArrayList<>())
                        .add(method);
            }
        }
        
        return methods;
    }
}

/**
 * Annotation to mark methods that should receive events
 */
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(java.lang.annotation.ElementType.METHOD)
@interface Subscribe {
    EventPriority priority() default EventPriority.NORMAL;
}

enum EventPriority {
    LOWEST,
    LOW,
    NORMAL,
    HIGH,
    HIGHEST
}

/**
 * Base class for events
 */
abstract class ModEvent implements EventBus.Event {
    private final Object source;
    
    protected ModEvent(Object source) {
        this.source = source;
    }
    
    @Override
    public Object getSource() {
        return source;
    }
}
