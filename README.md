# Alien (Fabric 1.21.11)

A Fabric client mod for Minecraft 1.21.1,now update to 1.21.11

## 注意事项
这个客户端并非完整的AlienV4升级版，它存在许多破损部分
由刘文涛的50%AiCode和50%我的SkidShitCode组成
thanks deepseek
Satin现已完全移除,其相关渲染暂时不会回归
Backdoor原位于Severmanager,现已完全移除
以下模块仍未修复或存在问题，尽量别动(你可以试试，大概率不会崩溃=)):
`Shader和satin`
剩下其它模块不清楚，自己研究去吧
灌注b站NRC4喵喵喵
enjoy it=)

## Core Requirements
*   **JDK 21** (Mandatory)
*   **Fabric Loom 1.17.x** (Build System)

## Quick Start

**Build:**
```bash
./gradlew build
```
Artifacts are located in `build/libs/`.

**Run Client:**
```bash
./gradlew runClient
```

## Dependencies
This project uses `flatDir` for local dependencies. The active local libraries must match Minecraft 1.21.11:

- `baritone-api-fabric-1.21.11.jar`
- `sodium-fabric-0.8.14-beta.2+mc1.21.11.jar`
- `apiguardian-api:1.1.2`

`nether-pathfinder` and IAS are currently disabled because the available files target older versions. Do not place the 1.21.1 Baritone or Sodium JARs on the active build classpath.

## Configuration
Configuration files are generated in `alien/cfg/` under the game run directory.

## License
MIT
