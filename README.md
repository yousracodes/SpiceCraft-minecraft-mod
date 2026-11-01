# SpiceCraft

Minecraft **1.20.1**, Forge **47.4.28**, Java **17**. Mod ID: `spicecraft`.

## Setup

Import this folder as a Gradle project. Set the IDE project SDK and Gradle JVM
to JDK 17. In PowerShell, select Java 17 for the current terminal:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat genIntellijRuns
.\gradlew.bat runClient
```

For Eclipse use `genEclipseRuns`. On Linux/macOS use `bash ./gradlew`.
The first run downloads Gradle, Minecraft, and Forge dependencies.

## Build

```powershell
.\gradlew.bat build
```

Output: `build/libs/spicecraft-0.1.0.jar`. Copy it into the `mods` folder
of Minecraft 1.20.1 running Forge 47.4.28 or a later 47.x version.

Use `runServer` for a dedicated development server. Review Minecraft's EULA
and set `eula=true` in `run/eula.txt` if you agree. Use `runData` after adding
data providers to generate resources into `src/generated/resources`.

## Project layout

- `src/main/java/com/spicecraft/SpiceCraft.java`: common mod entry point.
- `src/main/resources/META-INF/mods.toml`: Forge metadata.
- `src/main/resources/pack.mcmeta`: resource pack metadata.
- `gradle.properties`: mod identity and dependency versions.

The starter loads SpiceCraft without adding gameplay content. The default
license is All Rights Reserved; change `mod_license` if you select another.

References: [Forge setup](https://docs.minecraftforge.net/en/1.20.1/gettingstarted/),
[Forge downloads](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html).
