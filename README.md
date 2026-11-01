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
Food textures are sampled by the client while the Spice Maker interface is open.
Additional bottle texture combinations remain future work.

Processing continues with the interface closed while the chunk is ticking, and
inventory/progress persist across saves. An occupied output pauses processing;
removing or replacing inputs resets progress. Breaking the block drops its items.
Shift-click transfers are supported. The block currently uses vanilla stone
textures; crafting recipes are not defined yet.

Run automated machine checks with `.\gradlew.bat runGameTestServer`. For manual
GUI verification, also check shift-clicking, two players opening the same machine,
breaking it during processing, and saving/reopening the world mid-process.

## Powder colors

The client samples only pixel regions covered by the food's baked-model UVs,
including layered textures and registered item tints. Oklab hue families group
related shades together, with overlapping neighborhoods to avoid splitting gold
across arbitrary color buckets. Midtones and brighter saturated pixels carry more
weight than shadows. A family must cover at least 8% of eligible pixels, preventing
tiny highlights from replacing the main food color. Naturally dark foods retain
their actual colors. Transparent pixels and nearly black/white outline pixels
are ignored. Sampling is bounded and cached for up to
512 item/NBT combinations; resource reloads clear the cache.

Animated sprites, custom-rendered items, missing/unreadable textures, and samples
with no suitable pixels use `fallbackPowderColor` in
`config/spicecraft-common.toml` (an RGB integer in decimal). Machines operating
without a client sample also use that fallback and still complete normally.

While the machine GUI is open, its client reports a sampled RGB color. The server
checks the open menu, its ID and position, player distance, RGB range, and exact
current source item/NBT. The first matching report fixes the input's color, so a
second viewer with a different resource pack cannot overwrite it. This is visual
data only: clients cannot set effects, servings, or conversion results. Machines
retain accepted samples across saves. Outputs save the final RGB color, so they
keep the same appearance for all players and after resource-pack changes.

The supplied model already separates glass/lid textures from the tintable inner
powder. Empty bottles hide that layer; conversion reveals it with the saved tint.
The original `blank.png` is entirely transparent, so filled bottles use the asset
pack's grayscale `potion_liquid.png`, copied as `powder.png`, as a visible tint base.
Existing filled bottles keep their existing colors rather than being recolored.

Client verification: compare apple, carrot, and cookie spices; test a modded
tinted/layered food; reload a resource pack and process a fresh input; verify an
animated/custom model uses the fallback; compare the same bottle on two clients.

## Applying spice

Hold a filled spice bottle in your main hand and food in your off-hand, then
right-click. One food item receives the seasoning and the bottle loses one
serving. A single food stays in the off-hand; for a stack, the seasoned item goes
to your inventory (or drops at your feet when full), leaving the remaining food
unchanged. This consumes a serving in creative mode too.

Already-seasoned food is not seasoned again: right-click passes through to allow
normal off-hand eating, even while the bottle is empty or cooling down. Seasoned
food can be eaten at full hunger in survival from either hand to receive its
effects; unseasoned food keeps its normal hunger rules. Non-food
items and empty bottles cannot be used for seasoning and show an action-bar
message. Bottle clicks take priority during seasoning. A short cooldown prevents
rapid repeated application. Sound,
particles, and a hand swing confirm success; hover over the food to see its spice.
Effects apply when eating, not during seasoning.


Bottle model and textures: Copyright (c) 2025 Rustic-Potatoes, MIT License.
The original notice is bundled at `META-INF/licenses/rusticsbottles-LICENSE.txt`.

References: [Forge setup](https://docs.minecraftforge.net/en/1.20.1/gettingstarted/),
[Forge downloads](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html).
