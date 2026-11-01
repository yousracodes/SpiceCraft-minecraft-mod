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

On Windows, `gradlew.bat` automatically uses `%ProgramFiles%\Java\jdk-17` when
installed, even if `JAVA_HOME` or PATH selects a newer Java. To use a different
installation, set `SPICECRAFT_JAVA_HOME` to its JDK 17 directory. This selection
only affects the wrapper invocation. VS Code Gradle imports are also configured
for the installed JDK 17 in `.vscode/settings.json`; adjust that path on another PC.

`Unsupported class file major version 69` means the build is encountering Java 25
bytecode. Run this project's wrapper with Java 17 rather than adding native-access
flags. The single-use daemon message is normal with `org.gradle.daemon=false`.

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

## Spice Maker

Obtain the Spice Maker from the SpiceCraft creative tab or with
`/give @s spicecraft:spice_maker`. Place it and right-click to open its interface.
Put an edible item in the upper-left slot and an empty SpiceCraft bottle in the
lower-left slot. After 200 server ticks (10 seconds at 20 TPS), collect the filled
bottle from the right-hand slot. No fuel is required.

The output has five servings for the initial glass bottle, source-food identity,
saved effects, and the bottle type's fallback powder color. Each serving uses
one fifth of finite effect durations. Custom names and bottle data are preserved.
Dynamic food-texture colors and additional texture combinations remain future work.

Processing continues with the interface closed while the chunk is ticking, and
inventory/progress persist across saves. An occupied output pauses processing;
removing or replacing inputs resets progress. Breaking the block drops its items.
Shift-click transfers are supported. The block currently uses vanilla stone
textures; crafting recipes are not defined yet.

Run automated machine checks with `.\gradlew.bat runGameTestServer`. For manual
GUI verification, also check shift-clicking, two players opening the same machine,
breaking it during processing, and saving/reopening the world mid-process.

## Applying spice

Hold a filled spice bottle in your main hand and food in your off-hand, then
right-click. One food item receives the seasoning and the bottle loses one
serving. A single food stays in the off-hand; for a stack, the seasoned item goes
to your inventory (or drops at your feet when full), leaving the remaining food
unchanged. This consumes a serving in creative mode too.

Already-seasoned food, non-food items, and empty bottles are rejected with an
action-bar message. Bottle clicks take priority over block activation and
off-hand eating. A short cooldown prevents rapid repeated application. Sound,
particles, and a hand swing confirm success; hover over the food to see its spice.
Effects apply when eating, not during seasoning.


Bottle model and textures: Copyright (c) 2025 Rustic-Potatoes, MIT License.
The original notice is bundled at `META-INF/licenses/rusticsbottles-LICENSE.txt`.

References: [Forge setup](https://docs.minecraftforge.net/en/1.20.1/gettingstarted/),
[Forge downloads](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html).
