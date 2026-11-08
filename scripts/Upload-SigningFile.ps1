param([Parameter(Mandatory)][string]$Path)
$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath((Split-Path $PSScriptRoot -Parent))
$resolved = (Resolve-Path -LiteralPath $Path).Path
if (!$resolved.StartsWith($root + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase) -or
    $resolved -notmatch '\.(jks|keystore)$' -or !(Test-Path -LiteralPath $resolved -PathType Leaf)) {
    throw 'Choose an existing signing file inside this project; no file contents were read.'
}
$entry = Get-Item -LiteralPath $resolved -Force
while ($entry.FullName -ne $root) {
    if (($entry.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) {
        throw 'Linked signing paths are not accepted; no file contents were read.'
    }
    $entry = Get-Item -LiteralPath (Split-Path $entry.FullName -Parent) -Force
}
$encoded = [Convert]::ToBase64String([IO.File]::ReadAllBytes($resolved))
try {
    $encoded | & gh secret set RELEASE_KEYSTORE_BASE64 --repo 34rns/ScreenGarden
    if ($LASTEXITCODE -ne 0) { throw 'Private upload failed; no values were displayed.' }
    Write-Output 'Signing file uploaded privately. Configure the other three secrets in GitHub.'
} finally {
    $encoded = $null
}
