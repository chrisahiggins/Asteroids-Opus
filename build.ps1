# Builds the executable JAR for Asteroids-Opus.
#
# This machine has only a JRE 8 on PATH (no javac), so we compile with the JDK
# bundled inside IntelliJ IDEA (the JetBrains Runtime, OpenJDK 21) and target
# Java 8 bytecode with "--release 8" so the result runs on the installed JRE 8.

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

# Locate a javac: prefer the IntelliJ JBR, fall back to any JDK on PATH.
$javac = $null
$candidates = @(
    'C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2.1\jbr\bin\javac.exe'
)
foreach ($c in $candidates) { if (Test-Path $c) { $javac = $c; break } }
if (-not $javac) {
    $jb = Get-ChildItem 'C:\Program Files\JetBrains' -Directory -ErrorAction SilentlyContinue |
          ForEach-Object { Join-Path $_.FullName 'jbr\bin\javac.exe' } |
          Where-Object { Test-Path $_ } | Select-Object -First 1
    if ($jb) { $javac = $jb }
}
if (-not $javac) {
    $onPath = (Get-Command javac -ErrorAction SilentlyContinue)
    if ($onPath) { $javac = $onPath.Source }
}
if (-not $javac) { throw 'No javac found. Install a JDK or IntelliJ IDEA (which bundles one).' }

Write-Host "Using javac: $javac"

# Clean output dirs.
Remove-Item -Recurse -Force "$root\out", "$root\dist" -ErrorAction SilentlyContinue | Out-Null
New-Item -ItemType Directory -Force "$root\out"  | Out-Null
New-Item -ItemType Directory -Force "$root\dist" | Out-Null

# Compile all sources to Java 8 bytecode.
$sources = Get-ChildItem "$root\src" -Recurse -Filter *.java | ForEach-Object { $_.FullName }
& $javac --release 8 -d "$root\out" $sources
if ($LASTEXITCODE -ne 0) { throw "Compilation failed (exit $LASTEXITCODE)." }

# Package the executable JAR. The JetBrains Runtime ships no jar.exe, so use it
# when available and otherwise build the JAR (a ZIP) directly via .NET.
$jarOut = Join-Path $root 'dist\Asteroids.jar'
$jar = Join-Path (Split-Path -Parent $javac) 'jar.exe'
if (Test-Path $jar) {
    & $jar cfm $jarOut "$root\manifest.mf" -C "$root\out" .
    if ($LASTEXITCODE -ne 0) { throw "JAR packaging failed (exit $LASTEXITCODE)." }
} else {
    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    if (Test-Path $jarOut) { Remove-Item $jarOut -Force }
    $zip = [System.IO.Compression.ZipFile]::Open($jarOut, [System.IO.Compression.ZipArchiveMode]::Create)
    try {
        # Manifest first, written as raw ASCII bytes (no BOM).
        $entry = $zip.CreateEntry('META-INF/MANIFEST.MF')
        $s = $entry.Open()
        $bytes = [System.Text.Encoding]::ASCII.GetBytes((Get-Content "$root\manifest.mf" -Raw))
        $s.Write($bytes, 0, $bytes.Length); $s.Dispose()
        # All compiled classes, with forward-slash entry names.
        $outRoot = (Resolve-Path "$root\out").Path
        Get-ChildItem "$root\out" -Recurse -File | ForEach-Object {
            $rel = $_.FullName.Substring($outRoot.Length + 1).Replace('\', '/')
            [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $_.FullName, $rel) | Out-Null
        }
    } finally { $zip.Dispose() }
}

Write-Host "Build complete: $root\dist\Asteroids.jar"
