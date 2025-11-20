# CLAUDE.md

此文件为 Claude Code (claude.ai/code) 提供与当前仓库代码协作时的指导。

## 项目概述

Drill Down 是一个基于 LibGDX 框架的 Java 工厂建造游戏。这是一个支持桌面和 Android 平台的跨平台 2D 资源管理和自动化游戏。

## 常用命令

### 开发与运行
```bash
# 运行桌面版进行测试
./gradlew desktop:run

# 构建桌面版 JAR 文件
./gradlew desktop:dist

# 构建 Android APK 文件
./gradlew android:assembleFullRelease

# 清理构建
./gradlew clean

# 完整构建（双平台）
./scripts/build.sh

# 创建发布版本（需要签名配置）
./scripts/release.sh v1.0.0
```

### 测试
```bash
# 运行单元测试
./gradlew test

# 仅测试 core 模块
./gradlew core:test

# 仅测试 desktop 模块
./gradlew desktop:test
```

### IDE 支持
```bashn# 生成 Eclipse 项目文件
./gradlew eclipse

# 生成 IntelliJ IDEA 项目文件
./gradlew idea
```

## 架构概览

### 多模块结构
项目使用 Gradle 多模块架构：
- **core**: 主要游戏逻辑、资源和跨平台代码（核心模块）
- **desktop**: 桌面专用的 LWJGL 后端实现
- **android**: Android 专用实现
- **commons**: 共享工具库（Git 子模块）
- **gdx-sfx**: 音频库（Git 子模块）

### 核心系统架构

1. **场景管理系统**：
   - 基于场景的架构，`GameScene` 作为基类
   - 主要场景：Menu（菜单）、Game（游戏）、Loading（加载）
   - 场景切换通过 `Scene` 管理器处理

2. **建筑（Structure）系统**：
   - 使用继承层次结构组织建筑物和机器
   - 基类：`Structure`（所有建筑的基类）
     - `StorageStructure`：存储物品的建筑
     - `ProducerStructure`：生产资源的建筑
     - `RouterStructure`：路由物品/流体的建筑
   - 专业类型：`OvenStructure`、`FluidTubeStructure` 等
   - 所有建筑在 `StructureType` 枚举中注册，需要唯一 ID

3. **物品/资源系统**：
   - 支持 256 种不同的物品类型
   - 物流系统包括传送带、分选器、管道等
   - 电力系统：电缆、发电机、耗电建筑
   - 流体系统：管道运输

4. **存档系统**：
   - 自定义二进制格式（.qsf 文件）
   - 使用类似 NBT 的结构
   - 包含完整的游戏状态：建筑、物品、研究进度
   - 必须保持向后兼容性

5. **渲染系统**：
   - 基于区块的瓦片渲染（32x32 区块）优化性能
   - 使用 FrameBuffer 进行优化绘制
   - 自定义着色器支持

### 关键代码模式

1. **建筑注册模式**：
   - 在 `StructureType` 枚举中注册所有建筑
   - 每个建筑需要唯一 ID（最近修复了 ID 冲突导致崩溃的问题）
   - 使用注解生成 UI 代码（LML 框架）

2. **组件化设计**：
   - 建筑使用类似组件的模式，通过专业接口和基类实现
   - 例如：`IRotatable`、`IElectricConsumer` 等

3. **资源管理**：
   - 通过 LibGDX 的 `AssetManager` 集中加载
   - 资源文件位于 `core/assets/` 目录
   - 纹理图集、字体、声音统一管理

4. **国际化支持**：
   - 支持多语言（英语、德语、中文等）
   - 语言文件在 `core/assets/i18n/` 目录
   - 使用 LibGDX 的 I18NBundle 系统

5. **事件系统**：
   - 自定义动作系统处理游戏逻辑
   - 配方系统和研究进度

### 重要代码位置

- **主游戏类**：`core/src/de/dakror/quarry/Quarry.java`
- **游戏常量**：`core/src/de/dakror/quarry/Const.java`
- **建筑类型**（重要）：`core/src/de/dakror/quarry/structure/StructureType.java`
- **游戏逻辑**：`core/src/de/dakror/quarry/game/`
- **建筑实现**：`core/src/de/dakror/quarry/structure/`
- **UI 系统**：`core/src/de/dakror/quarry/ui/`
- **场景管理**：`core/src/de/dakror/quarry/scenes/`

### 关键子系统

- **传送带系统**：`core/src/de/dakror/quarry/structure/logistics/ConveyorStructure.java`
- **电力系统**：`core/src/de/dakror/quarry/structure/power/`
- **流体系统**：`core/src/de/dakror/quarry/structure/logistics/FluidTubeStructure.java`
- **研究系统**：`core/src/de/dakror/quarry/game/Research.java`
- **配方系统**：`core/src/de/dakror/quarry/game/Recipe.java`

## 开发注意事项

### 跨平台兼容性
- 代码必须同时支持桌面和 Android 平台
- 避免在 core 模块中使用桌面专用 API
- 平台相关代码放在 desktop 或 android 模块

### 性能优化
- 游戏使用基于区块的渲染和帧缓冲区
- 512x512 的帧缓冲区用于光照和特效
- 谨慎处理渲染代码，避免每帧创建对象

### 存档兼容性
- 修改存档格式时必须保持向后兼容
- 处理版本兼容：`SaveFile.Version` 枚举
- 测试旧存档加载

### 建筑 ID 管理
- `StructureType` 枚举中的 ID 必须唯一且稳定
- 最近已修复 ID 冲突导致的崩溃（ArcWelder vs Stacker）
- 添加新建筑时检查 ID 冲突

### 子模块依赖
- 使用 Git 子模块：commons 和 gdx-sfx
- 拉取代码后更新子模块：`git submodule update --init --recursive`

### 测试策略
- 单元测试覆盖有限
- 主要通过 `desktop:run` 进行手动测试
- Desktop 版本为首选测试平台

### 构建和发布
- GitHub Actions 自动构建双平台
- 发布需要签名配置（keystore）
- APK 和 JAR 自动上传到 GitHub Releases

### 编码规范
- 使用 Apache License 2.0 头注释
- 遵循 Java 命名规范
- LML UI 模板文件使用 `.lml` 扩展名
- 资产文件使用特定命名规则

### 调试建议
- Desktop 版本支持调试日志
- 使用 `Game.G.debug` 标志启用调试模式
- 调试绘图功能在 `GameScene.render()` 中
