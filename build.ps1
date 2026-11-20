# Builds ECLIPSE into build\Eclipse.jar. Use -Install to copy it into the game's mods folder.
param([switch]$Install)
$ErrorActionPreference = 'Stop'

$game = 'D:\SteamLibrary\steamapps\common\Mindustry'
$gameJar = "$game\jre\desktop.jar"
$root = $PSScriptRoot
$build = "$root\build"
$stage = "$build\stage"

if (Test-Path $build) { Remove-Item -Recurse -Force $build }
New-Item -ItemType Directory -Force $stage | Out-Null

# 1. Generated sprites, then hand-made assets on top (so real art overrides generated art).
java -cp $gameJar "$root\tools\SpriteGen.java" "$stage\sprites" $gameJar
if ($LASTEXITCODE -ne 0) { throw 'Sprite generation failed' }
Copy-Item -Recurse -Force "$root\assets\*" $stage
Copy-Item "$root\mod.hjson" $stage
# assets\preview.png (copied above) is the Steam Workshop cover; regenerate it with tools\CoverArt.java

# 2. Compile against the installed game so the API always matches.
$sources = (Get-ChildItem -Recurse "$root\src" -Filter *.java).FullName
# Java 17 bytecode so Android's D8 can read it (desktop runs it fine on its Java 25 JRE).
javac --release 17 -nowarn -cp $gameJar -d $stage @sources
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }

# 2b. Android (mobile) only loads classes from classes.dex inside the jar; without it the mod fails
# with ClassNotFoundException: eclipse.EclipseMod. D8 (r8.jar) + android.jar live in D:\Program\android-tools.
$android = 'D:\Program\android-tools'
$classes = "$build\classes.txt"
(Get-ChildItem -Recurse $stage -Filter *.class).FullName | Set-Content -Encoding ascii $classes
java -cp "$android\r8.jar" com.android.tools.r8.D8 --release --min-api 14 --lib "$android\android.jar" --classpath $gameJar --output $stage "@$classes"
if ($LASTEXITCODE -ne 0) { throw 'Dexing failed' }

# 3. Package.
jar --create --file "$build\Eclipse.jar" -C $stage .
if ($LASTEXITCODE -ne 0) { throw 'Packaging failed' }
Write-Host "Built $build\Eclipse.jar"

if ($Install) {
    # Mindustry can only publish mods in folder form to the Steam Workshop, so install as a folder
    # (the jar stays in build\ for sharing). Only one copy may be installed, or the game picks one at random.
    $mods = "$game\saves\mods"
    if (Test-Path "$mods\Eclipse.jar") { Remove-Item -Force "$mods\Eclipse.jar" }
    if (Test-Path "$mods\Eclipse") { Remove-Item -Recurse -Force "$mods\Eclipse" }
    Copy-Item -Recurse $stage "$mods\Eclipse"
    Write-Host "Installed to $mods\Eclipse (folder form, ready to publish)"
}
