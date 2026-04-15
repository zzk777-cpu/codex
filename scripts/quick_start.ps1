param(
    [ValidateSet('frontend','api')]
    [string]$Mode = 'frontend'
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

function Start-MockServices {
    return Start-Process -FilePath python -ArgumentList "scripts/mock_services.py" -PassThru -WindowStyle Hidden
}

function Start-Backend {
    if (!(Test-Path "backend/out")) { New-Item -ItemType Directory -Path "backend/out" | Out-Null }

    $javaFiles = Get-ChildItem -Path "backend/src/main/java" -Filter "*.java" -Recurse | ForEach-Object { $_.FullName }
    & javac -encoding UTF-8 -d backend/out $javaFiles

    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = "java"
    $psi.Arguments = "-cp backend/out com.example.docsys.StandaloneServer"
    $psi.UseShellExecute = $false
    $psi.EnvironmentVariables["SERVICES_NLP_BASE_URL"] = "http://127.0.0.1:18000"
    $psi.EnvironmentVariables["SERVICES_ES_BASE_URL"] = "http://127.0.0.1:19200"
    $psi.EnvironmentVariables["SERVICES_ES_INDEX"] = "course_documents"

    $proc = New-Object System.Diagnostics.Process
    $proc.StartInfo = $psi
    $proc.Start() | Out-Null
    return $proc
}

function Start-Frontend {
    Push-Location frontend
    try {
        return Start-Process -FilePath python -ArgumentList "-m http.server 5173" -PassThru -WindowStyle Hidden
    } finally {
        Pop-Location
    }
}

function Cleanup($procs) {
    foreach ($p in $procs) {
        if ($null -ne $p -and -not $p.HasExited) {
            try { Stop-Process -Id $p.Id -Force -ErrorAction SilentlyContinue } catch {}
        }
    }
}

$mock = $null
$backend = $null
$frontend = $null

try {
    $mock = Start-MockServices
    $backend = Start-Backend
    Start-Sleep -Seconds 1

    if ($Mode -eq 'api') {
        Write-Host "[quick-start] 运行 API 端到端演示（PowerShell）..."
        Write-Host "== health =="
        (Invoke-RestMethod -Uri "http://127.0.0.1:8080/health" -Method GET | ConvertTo-Json -Depth 10)

        Write-Host "`n== analyze =="
        $analyzeBody = @{ title='机器学习课程教案'; content='本文档介绍文本分类与检索'; docType='教案'; courseName='机器学习' } | ConvertTo-Json
        (Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/documents/analyze" -Method POST -Body $analyzeBody -ContentType 'application/json' | ConvertTo-Json -Depth 10)

        Write-Host "`n== upload-and-index =="
        $uploadBody = @{ title='课程论文模板'; content='这是一篇论文写作规范'; docType='论文'; courseName='人工智能' } | ConvertTo-Json
        (Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/documents/upload-and-index" -Method POST -Body $uploadBody -ContentType 'application/json' | ConvertTo-Json -Depth 10)

        Write-Host "`n== search =="
        (Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/documents/search?q=论文&size=10" -Method GET | ConvertTo-Json -Depth 10)
        Write-Host "`nDemo completed."
    } else {
        $frontend = Start-Frontend
        Write-Host "[quick-start] 前端演示已启动（PowerShell）"
        Write-Host "- Frontend: http://127.0.0.1:5173"
        Write-Host "- Backend : http://127.0.0.1:8080"
        Write-Host "- Mock NLP: http://127.0.0.1:18000"
        Write-Host "- Mock ES : http://127.0.0.1:19200"
        Write-Host "`n按 Ctrl+C 停止。"
        while ($true) { Start-Sleep -Seconds 3 }
    }
} finally {
    if ($Mode -eq 'api') { Cleanup @($frontend,$backend,$mock) }
}
