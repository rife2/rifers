#!/usr/bin/env pwsh

$ErrorActionPreference = 'Stop'
# Invoke-WebRequest's progress bar makes downloads crawl on Windows PowerShell
$ProgressPreference = 'SilentlyContinue'

# resolve the latest release by following GitHub's redirect, the final URL ends
# in the version tag; 5.1 and 7 expose that URL on different properties
$response = Invoke-WebRequest -Uri 'https://github.com/rife2/bld/releases/latest' -UseBasicParsing
if ($response.BaseResponse.PSObject.Properties['ResponseUri']) {
    $url = $response.BaseResponse.ResponseUri.ToString()
} else {
    $url = $response.BaseResponse.RequestMessage.RequestUri.ToString()
}
$version = $url.Substring($url.LastIndexOf('/') + 1)

$filepath = Join-Path ([System.IO.Path]::GetTempPath()) "bld-$version-$([System.IO.Path]::GetRandomFileName()).jar"

Write-Host "Downloading bld v$version..."
Write-Host

try {
    Invoke-WebRequest -Uri "https://github.com/rife2/bld/releases/download/$version/bld-$version.jar" -OutFile $filepath -UseBasicParsing

    Write-Host "Welcome to bld v$version."
    java -jar $filepath create
} finally {
    Remove-Item -Force -ErrorAction SilentlyContinue $filepath
}
