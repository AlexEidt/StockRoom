<#
.DESCRIPTION

.SYNOPSIS

#>

# json
[string]$jsonFile = "C:\Repos\StockRoom\app\Wertpapiere 25.02.2021, 13_06_21.json"
[string]$jsonFileReformated = "C:\Repos\StockRoom\app\Wertpapiere-reformatted.json"
[string]$jsonFileNeu = "C:\Repos\StockRoom\app\Wertpapiere-neu.json"

$json = Get-Content -Encoding UTF8 $jsonFile | ConvertFrom-Json
$json | ConvertTo-Json -Depth 10 | Set-Content -Encoding UTF8 $jsonFileReformated

[int]$zoneOffset = 8*60*60

foreach ($aktien in $json) {
    "$($aktien.symbol) in $($aktien.portfolio)"

    foreach ($asset in $aktien.assets) {
        if($asset.date -ne $null) { $asset.date += $zoneOffset }
        if($asset.expirationDate -ne $null) { $asset.expirationDate += $zoneOffset }
    }

    foreach ($dividend in $aktien.dividends) {
        if($dividend.paydate -ne $null) { $dividend.paydate += $zoneOffset }
        if($dividend.exdate -ne $null) { $dividend.exdate += $zoneOffset }
    }

    foreach ($event in $aktien.events) {
        if($event.datetime -ne $null) { $event.datetime += $zoneOffset }
    }

}

$json | ConvertTo-Json -Depth 10 | Set-Content -Encoding UTF8 $jsonFileNeu
