# Script para verificar que el HTML esté bien formado
$htmlFile = "src\main\resources\templates\admin\gestionOfertas.html"
$content = Get-Content $htmlFile -Raw

Write-Host "=== VERIFICACIÓN DE ARCHIVO HTML ===" -ForegroundColor Green

# Verificar que termine con </html>
if ($content -match "</html>\s*$") {
    Write-Host "✅ Archivo termina correctamente con </html>" -ForegroundColor Green
} else {
    Write-Host "❌ Archivo NO termina correctamente" -ForegroundColor Red
    Write-Host "Últimas 5 líneas:" -ForegroundColor Yellow
    Get-Content $htmlFile | Select-Object -Last 5
}

# Contar modales
$modales = ($content | Select-String -Pattern '<div id=".*modal.*"' -AllMatches).Matches.Count
Write-Host "📊 Modales encontrados: $modales" -ForegroundColor Cyan

# Verificar que no haya JavaScript fuera de <script>
$afterHtml = if ($content -match "</html>(.*)$") { $Matches[1].Trim() } else { "" }
if ($afterHtml -eq "") {
    Write-Host "✅ No hay contenido después de </html>" -ForegroundColor Green
} else {
    Write-Host "❌ Hay contenido después de </html>:" -ForegroundColor Red
    Write-Host $afterHtml.Substring(0, [Math]::Min(200, $afterHtml.Length)) -ForegroundColor Yellow
}

Write-Host "=== FIN VERIFICACIÓN ===" -ForegroundColor Green