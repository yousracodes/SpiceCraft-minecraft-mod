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
Shift-click transfers are supported. The block uses a custom machine model and
texture. Its bottom tray displays the inserted empty bottle, then the finished
bottle with its saved spice color until collected. The display updates for nearby
players and after loading the chunk. If both bottle slots are occupied, the
finished output is displayed. Crafting recipes are not defined yet.
The input food is displayed in the top hopper, including when no bottle is
inserted. It disappears when the last input food is consumed or removed.
Shift-right-click with an empty spice bottle to insert it directly. Shift-right-click
with an empty hand to take the finished output first, or the input bottle if no
output is ready. Drop edible items onto the top to load the food slot; matching
food stacks merge up to their stack limit, leaving excess or incompatible items
in the world. Hover over the Spice Maker item to see these controls.

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

## Placing bottles

Hold an empty or filled spice bottle and **Shift + right-click the top of a solid
surface** to place it. Up to four bottles share one block space in a 2×2 layout.
Aim at the desired quarter of the counter; occupied quarters reject placement.
You can also aim at the group when adding bottles. Each bottle keeps its own data.
The world renderer uses the same bottle item model and saved powder tint. Contents,
servings, source effects, color, custom name, and other item data survive placement,
saving/reloading, and recovery. Placement takes priority over seasoning when sneaking.

**Left-click a bottle** to recover only that bottle, without a tool. It drops
the original bottle once, including in creative mode. Removing its supporting
surface drops all remaining bottles. Older single-bottle displays load into the
first quarter. Placement uses standard block-item placement checks;
protected areas and adventure-mode rules still apply.

## Spice rack

Find **Spice Rack** in the SpiceCraft tab or use `/give @s spicecraft:spice_rack`.
The existing rack now has four shelves with four bottle positions each: **16 total**.
It retains the supplied wood texture and fits within one block (10 units wide,
16 tall, 4 deep; 16 units = one Minecraft block).

Right-click the desired row and column with an empty or filled spice bottle to
insert it. Right-click that slot with an empty hand to recover it. Sneaking with
a bottle also inserts into the rack. Occupied slots reject insertion. Aim at the
backboard behind a slot; the shelf rails mark each row. Breaking the rack drops
all stored bottles and the rack item. Contents persist across saves and synchronize
with nearby players, including each bottle's name, spice color, servings, and effects.

Bottles appear at 25% size only inside the rack. Their displayed bounds are about
1.503 wide x 3 tall x 1.5 deep. Each shelf has a 3.5-unit vertical opening and
2.25-unit column spacing, leaving clear space around all sixteen bottles.
Bottle items retain their normal inventory and hand size; counter displays use 60% scale. Existing placed racks
use the new model; replace an older decorative rack if it has no storage entity.

## Applying spice

Hold a filled spice bottle in your main hand and food in your off-hand, then
right-click. One food item receives the seasoning and the bottle loses one
serving. A single food stays in the off-hand; for a stack, the seasoned item goes
to your inventory (or drops at your feet when full), leaving the remaining food
unchanged. This consumes a serving in creative mode too.

Food supports up to three different source spices. To add another spice to an
already-seasoned meal, hold the new bottle in your main hand, the meal in your
off-hand, and **right-click**. Shift is not required. Each addition costs one serving. Duplicate
source spices and a fourth seasoning are rejected without consuming anything.
For a stack, only one food item receives the additional seasoning.

With a filled bottle in the main hand, right-click always attempts seasoning,
including on already-seasoned food. To eat, switch away from the bottle or put
the food in your main hand. An empty bottle also allows off-hand eating. Seasoned
food can be eaten at full hunger in survival from either hand to receive its
effects; unseasoned food keeps its normal hunger rules. Non-food
items and empty bottles cannot be used for seasoning and show an action-bar
message. Bottle clicks take priority during seasoning. A short cooldown prevents
rapid repeated application. Sound,
particles, and a hand swing confirm success; hover over the food to see its spice.
Effects apply when eating, not during seasoning.

Each seasoning has its own tooltip line in its saved powder color and retains
its own effects and probabilities. On consumption, probabilities are rolled
independently, then overlapping successful effects are merged by type: highest
strength wins, with the longest duration at that strength (infinite wins a duration
tie). Strengths and durations are never added together; each effect type is applied
once, including instant effects. Source snapshots are capped at 16 effects per
spice, for at most 48 stored effect entries per meal. Effects are evaluated only
on consumption, not every server tick.

Old single-seasoning meals remain readable, edible, and visible in tooltips.
Adding another spice migrates them to the versioned list format, preserving source,
color, and effects; unrelated food NBT is retained. Unknown seasoning versions are
not overwritten. Different seasoning combinations have different item data and
will not stack together. Changes to seasoning and bottle servings run on the server.


## Seasoning in crafted meals

Edible results from the crafting table and the inventory's 2x2 grid inherit
seasonings from consumed edible ingredients. For example, a seasoned baked potato
used in a modpack's salad crafting recipe passes its seasoning to the salad.
The preview, normal pickup, and Shift-click crafting use the same result.

The first three unique spice sources in ingredient-slot order are kept. Duplicate
sources do not add strength or duration; the first occurrence wins. Each slot
contributes one consumed ingredient, regardless of its stack size. Recipe outputs
retain their own names and other mods' data. Recipes that already provide seasoning
data (including unknown versions) keep their own result unchanged. Seasonings that
remain on returned ingredients are not also transferred to the output.

For recipes producing multiple portions, finite effect durations are divided by
the output count and rounded down; effects reduced below one tick are removed.
Instant and infinite effects instead divide their probability by the output count.
Hidden effects also have their durations divided; a chain containing an instant or
infinite effect has its probability divided as well. This limits the total expected
effect budget; it does not guarantee that exactly one portion triggers an effect.
Repeated crafting never rounds duration up. Existing foods and saved worlds keep
their existing seasoning format and behavior.

Per-world settings are in `serverconfig/spicecraft-crafting-server.toml` inside
the world folder (the server controls these settings):

```toml
inheritSeasoning = true
excludedRecipes = ["othermod:salad_recipe_to_skip"]
```

Set `inheritSeasoning = false` to disable inheritance, or list exact recipe IDs
under `excludedRecipes`. Modpacks can supply defaults using the same filename in
`defaultconfigs`. Cooking pots, ovens, and machine crafting need separate
integrations unless they use the standard crafting-grid result calculation.
This does not add a salad recipe or change which ingredients a recipe accepts.
Crafting compatibility with a particular modpack should be checked in that pack.

Bottle model and textures: Copyright (c) 2025 Rustic-Potatoes, MIT License.
The original notice is bundled at `META-INF/licenses/rusticsbottles-LICENSE.txt`.

References: [Forge setup](https://docs.minecraftforge.net/en/1.20.1/gettingstarted/),
[Forge downloads](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html).
