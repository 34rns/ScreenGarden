param([string]$Manifest = 'app/src/main/AndroidManifest.xml', [switch]$Release)
$ErrorActionPreference = 'Stop'
$repository = Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $repository
[xml]$xml = Get-Content -LiteralPath $Manifest
$ns = 'http://schemas.android.com/apk/res/android'
$permissions = @($xml.manifest.SelectNodes('uses-permission') | ForEach-Object { $_.GetAttribute('name', $ns) })
if ($permissions -contains 'android.permission.INTERNET' -or $permissions -contains 'android.permission.QUERY_ALL_PACKAGES') {
    throw 'Offline/least-privilege manifest rule failed'
}
if ($xml.manifest.application.GetAttribute('allowBackup', $ns) -ne 'false') { throw 'Backup must remain disabled' }
if ($Release -or $Manifest -eq 'app/src/main/AndroidManifest.xml') {
    if (@($xml.manifest.application.SelectNodes('activity')).Count -ne 1) { throw 'Exactly one production Activity required' }
}
if ($Release -and $xml.manifest.application.GetAttribute('debuggable', $ns) -eq 'true') { throw 'Release is debuggable' }
$hashes = Get-Content -LiteralPath "$PSScriptRoot/schema-hashes.json" -Raw | ConvertFrom-Json
foreach ($schema in $hashes.PSObject.Properties) {
    $path = "app/schemas/com.screengarden.app.data.local.ScreenGardenDatabase/$($schema.Name).json"
    if ((Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash -ne $schema.Value) { throw "Published schema $($schema.Name) changed" }
}
$files = @(git ls-files --cached --others --exclude-standard)
if ($LASTEXITCODE -ne 0) { throw 'Cannot enumerate project files' }
foreach ($file in $files) {
    if ($file -match '\.(jks|keystore)$|(^|/)(local\.properties|keystore\.properties|signing\.properties)$') {
        throw 'Private configuration must not be tracked'
    }
    if ($file -match '\.kt$' -and (Get-Content -LiteralPath $file -TotalCount 1) -ne '// SPDX-License-Identifier: GPL-3.0-or-later') {
        throw "Kotlin license header missing: $file"
    }
}
Write-Output 'PASS: offline manifest, disabled backup, production activity, immutable schemas and file hygiene'
