$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$site = Join-Path $root 'site'
$html = Get-Content -LiteralPath (Join-Path $site 'index.html') -Raw
if ($html -notmatch '<html lang="en"' -or $html -notmatch 'name="viewport"' -or
    $html -notmatch 'fake debug-only' -or $html -notmatch 'unverified') {
    throw 'Site language, mobile layout or honest verification disclosures are missing.'
}
foreach ($match in [regex]::Matches($html, '(?:src|href)="([^"#]+)"')) {
    $target = $match.Groups[1].Value
    if ($target -match '^https://') { continue }
    $path = [IO.Path]::GetFullPath((Join-Path $site $target))
    if (!$path.StartsWith($site + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase) -or
        !(Test-Path -LiteralPath $path -PathType Leaf)) { throw "Broken local site reference: $target" }
}
foreach ($image in Get-ChildItem -LiteralPath (Join-Path $site 'screenshots') -File) {
    $original = Join-Path $root "docs/screenshots/$($image.Name)"
    if (!(Test-Path -LiteralPath $original) -or
        (Get-FileHash -LiteralPath $image.FullName).Hash -ne (Get-FileHash -LiteralPath $original).Hash) {
        throw 'Site screenshots must exactly match committed emulator demo images.'
    }
}
if ($html -match '<(?:script|link)[^>]+(?:src|href)="https?://') { throw 'External script/font dependency forbidden.' }
Write-Output 'PASS: local site links, emulator image hashes, offline assets and honest disclosures'
