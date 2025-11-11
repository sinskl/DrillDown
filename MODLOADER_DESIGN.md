# DrillDown ModLoader 设计方案

## 现有架构分析

### 已发现的modding基础设施

**1. Java Agent系统** (`desktop/src/de/dakror/modding/stub/StubAgent.java`)
- 使用 `java.lang.instrument` API
- 支持类重新定义和转换
- 通过 JAR Manifest 配置：
  ```gradle
  manifest {
      attributes(
          'Premain-Class': 'de.dakror.modding.stub.StubAgent',
          'Agent-Class': 'de.dakror.modding.stub.StubAgent',
          'Can-Redefine-Classes': 'true',
          'Can-Retransform-Classes': 'true',
      )
  }
  ```

**2. Agent加载机制**
- 动态加载 ModAgent 类
- 支持外部ModLoader.jar文件
- 使用自定义URLClassLoader隔离mod代码

## 核心设计原则

### 1. 模块化架构
```
ModLoader/
├── api/                 # 核心API
│   ├── Mod.java         # Mod基础接口
│   ├── ModContainer.java
│   └── registry/        # 注册系统
├── core/                # ModLoader核心
│   ├── Loader.java      # 主加载器
│   ├── Transformer.java # 类转换器
│   └── EventBus.java    # 事件系统
├── runtime/             # 运行时支持
│   ├── ClassLoader.java # 隔离类加载
│   ├── Reloader.java    # 热重载
│   └── Security.java    # 安全检查
└── mods/                # 示例mod
    ├── example-mod/
    └── template/
```

### 2. 关键组件设计

#### A. Mod接口
```java
public interface Mod {
    // Mod信息
    String getId();
    String getName();
    String getVersion();
    String getDescription();
    
    // 生命周期
    void onLoad();        // 加载时调用
    void onUnload();      // 卸载时调用
    void onConfigReload(); // 配置重载
    
    // 依赖
    List<String> getDependencies();
    boolean isClientOnly();
    boolean isServerOnly();
}
```

#### B. 注册系统
```java
public interface Registry<T> {
    void register(String id, T entry);
    T get(String id);
    Collection<T> getAll();
    boolean contains(String id);
}

// 预定义注册表
public class Registries {
    Registry<Building> buildings;
    Registry<Item> items;
    Registry<Recipe> recipes;
    Registry<Tile> tiles;
    Registry<Structure> structures;
}
```

#### C. 事件系统
```java
public interface EventBus {
    <T extends Event> void register(Class<T> eventType, EventHandler<T> handler);
    <T extends Event> void post(T event);
    void subscribe(Object subscriber);
    void unsubscribe(Object subscriber);
}

// 预定义事件
public interface Event {
    Object getSource();
}

public class BuildingPlacedEvent implements Event {
    public final Building building;
    public final BlockPos position;
}

public class RecipeCompletedEvent implements Event {
    public final Recipe recipe;
    public final Inventory output;
}
```

### 3. 类转换与注入

#### ASM字节码操作
```java
public class ClassTransformer {
    public byte[] transform(String className, byte[] classBytes) {
        ClassReader reader = new ClassReader(classBytes);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
        
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM7, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, 
                                           String signature, String[] exceptions) {
                // 注入事件调用
                return super.visitMethod(access, name, desc, signature, exceptions);
            }
        };
        
        reader.accept(visitor, ClassReader.SKIP_FRAMES);
        return writer.toByteArray();
    }
}
```

#### 字段/方法注入
```java
// 注入示例：在Tile类中添加自定义字段
public class TileFieldInjector {
    public void injectFields(ClassVisitor cv) {
        cv.visitField(Opcodes.ACC_PUBLIC, "modData", "Lde/dakror/modding/api/data/ModData;", null, null);
    }
}
```

### 4. 类加载隔离

#### URLClassLoader层次
```
Bootstrap ClassLoader
    ↓
Platform ClassLoader (Java 9+)
    ↓
Application ClassLoader (游戏主类加载器)
    ↓
Mod ClassLoader (隔离mods)
    ↓
Mod-specific ClassLoader (每个mod单独加载)
```

#### 安全沙箱
```java
public class ModSecurityManager extends SecurityManager {
    @Override
    public void checkPermission(Permission perm) {
        // 检查mod权限：文件系统访问、网络、反射等
        if (modContext != null) {
            modContext.checkPermission(perm);
        }
    }
}
```

### 5. 热重载机制

#### 文件监控
```java
public class ModReloader {
    private final WatchService watchService;
    private final Map<WatchKey, Path> watchKeys;
    
    public void startWatching(Path modDirectory) {
        // 监控mods目录变化
        // 检测mod更新事件
        // 触发重新加载
    }
}
```

#### 状态保持
```java
public class ReloadContext {
    private final Map<String, Object> savedState = new HashMap<>();
    
    public void saveState(String key, Object value) {
        savedState.put(key, value);
    }
    
    public <T> T restoreState(String key, Class<T> type) {
        return type.cast(savedState.get(key));
    }
}
```

### 6. 配置系统

#### mod配置
```java
public interface ModConfig {
    <T> ConfigValue<T> get(String key, T defaultValue, String comment);
    void save();
    void load();
}

public class ModConfigSpec {
    public static class Builder {
        private final Map<String, ConfigEntry<?>> entries = new HashMap<>();
        
        public <T> ConfigEntry<T> define(String key, T defaultValue) {
            return new ConfigEntry<>(key, defaultValue);
        }
        
        public ModConfig build() {
            return new ModConfigImpl(entries);
        }
    }
}

// 使用示例：
public class MyMod implements Mod {
    private ModConfig config;
    
    public void onLoad() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        ConfigEntry<String> name = builder.define("greeting", "Hello");
        ConfigEntry<Integer> count = builder.define("maxItems", 10);
        
        config = builder.build();
    }
    
    public void onConfigReload() {
        String greeting = config.get("greeting").get();
        System.out.println(greeting + " World!");
    }
}
```

### 7. 事件系统

#### 发布-订阅模式
```java
public class EventBusImpl implements EventBus {
    private final Map<Class<?>, List<EventHandler<?>>> handlers = new HashMap<>();
    
    public <T extends Event> void post(T event) {
        List<EventHandler<?>> handlers = this.handlers.get(event.getClass());
        if (handlers != null) {
            for (EventHandler<?> handler : handlers) {
                try {
                    ((EventHandler<T>) handler).handle(event);
                } catch (Exception e) {
                    // 记录错误但不中断
                    logger.error("Event handler error", e);
                }
            }
        }
    }
}

// 注解支持
public @interface Subscribe {
    EventPriority priority() default EventPriority.NORMAL;
    boolean receiveCanceled() default false;
}

public class EventHandlerMethod {
    public void handleEvent(BuildingPlacedEvent event) {
        // 自动调用
    }
}
```

### 8. 资源管理

#### 资产注入
```java
public class AssetInjector {
    public void injectAssets() {
        // 注入纹理
        Texture newTexture = new Texture("mod:textures/custom.png");
        TextureAtlas.atlas.addRegion("custom", newTexture);
        
        // 注入音频
        Sound newSound = Gdx.audio.newSound(Gdx.files.internal("mod:sounds/click.ogg"));
        SoundManager.register("custom.click", newSound);
    }
}
```

#### 蓝图系统
```java
public class BlueprintModSupport {
    public void registerBlueprint(Blueprint blueprint) {
        // 支持mod添加自定义蓝图
        BlueprintRegistry.register(blueprint);
    }
}
```

## 实施步骤

### Phase 1: 核心架构 (Week 1-2)
1. 创建Mod接口和基础类
2. 实现ModClassLoader
3. 创建注册系统架构
4. 实现事件总线

### Phase 2: 类加载与转换 (Week 3-4)
1. 实现ASM转换器
2. 添加字段/方法注入支持
3. 实现安全检查机制

### Phase 3: API开发 (Week 5-6)
1. 实现建筑/物品注册API
2. 创建配置文件系统
3. 实现资源注入机制

### Phase 4: 热重载 (Week 7)
1. 实现文件监控
2. 添加状态保存/恢复
3. 实现优雅卸载

### Phase 5: 示例与文档 (Week 8)
1. 创建示例mod
2. 编写API文档
3. 创建mod开发指南

## 安全考虑

### 1. 代码签名
```java
public class ModValidator {
    public boolean validateSignature(Path modJar) {
        // 验证mod JAR签名
        // 检查证书链
        return true;
    }
}
```

### 2. 权限管理
```java
public class ModPermissions {
    public static final Permission FILE_ACCESS = new FilePermission("mods/*", "read,write");
    public static final Permission NETWORK_ACCESS = new NetPermission("*");
    public static final Permission REFLECTION_ACCESS = new RuntimePermission("*");
}
```

### 3. 资源限制
```java
public class ResourceMonitor {
    private final AtomicLong memoryUsed = new AtomicLong();
    private final Semaphore fileHandles;
    
    public boolean checkLimits() {
        return memoryUsed.get() < MAX_MEMORY && 
               activeFileHandles < MAX_FILES;
    }
}
```

## 性能优化

### 1. 懒加载
- 延迟加载mod资源
- 缓存常用数据
- 异步事件处理

### 2. 内存管理
- 及时卸载未使用的mods
- 清理ASM转换器缓存
- 监控内存使用

### 3. 并发优化
- 使用线程池处理异步任务
- 线程安全的事件分发
- 无锁数据结构

## 示例Mod结构

```
mods/example-mod/
├── build.gradle
├── src/main/java/
│   └── com/example/mod/
│       └── ExampleMod.java
├── src/main/resources/
│   ├── mod.json
│   ├── textures/
│   ├── sounds/
│   └── lang/
└── README.md
```

```json
{
  "id": "example",
  "name": "Example Mod",
  "version": "1.0.0",
  "author": "Your Name",
  "description": "A simple example mod",
  "main": "com.example.mod.ExampleMod",
  "dependencies": {
    "core": ">=1.0.0"
  },
  "entrypoints": {
    "client": "com.example.mod.ClientMod",
    "server": "com.example.mod.ServerMod"
  }
}
```

```java
public class ExampleMod implements Mod {
    @Override
    public void onLoad() {
        // 注册建筑
        Building exampleBuilding = Building.builder()
            .id("example")
            .name("Example Building")
            .size(2, 2)
            .recipe(exampleRecipe)
            .build();
            
        Registries.buildings.register("example", exampleBuilding);
        
        // 监听事件
        EventBus.subscribe(this);
    }
    
    @Subscribe
    public void onBuildingPlaced(BuildingPlacedEvent event) {
        // 处理建筑放置事件
        if (event.building.getId().equals("example")) {
            // 自定义逻辑
        }
    }
}
```

## 总结

DrillDown已经有良好的modding基础（Java Agent系统），我们可以在此基础上构建：

1. **完整的mod管理系统**
2. **安全的类加载隔离**
3. **强大的事件系统**
4. **热重载支持**
5. **丰富的API生态**

这个设计平衡了功能性和安全性，为mod开发者提供了强大的工具，同时保护游戏核心系统。
