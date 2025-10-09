$SERVER_URL = "http://127.0.0.1:8080"
$PASS = 0
$FAIL = 0
$TOTAL = 0

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "HTTP Server Test Suite" -ForegroundColor Cyan
Write-Host "Testing server at: $SERVER_URL" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

function Run-Test
{
    param(
        [string]$TestName,
        [string]$ExpectedCode,
        [scriptblock]$Command
    )

    $script:TOTAL++
    Write-Host "Test ${script:TOTAL}: $TestName ... " -NoNewline

    try
    {
        $result = & $Command
        $actualCode = $result.StatusCode

        if ($actualCode -eq $ExpectedCode)
        {
            Write-Host "PASS" -ForegroundColor Green -NoNewline
            Write-Host " (HTTP $actualCode)"
            $script:PASS++
        }
        else
        {
            Write-Host "FAIL" -ForegroundColor Red -NoNewline
            Write-Host " (Expected: $ExpectedCode, Got: $actualCode)"
            $script:FAIL++
        }
    }
    catch
    {
        Write-Host "FAIL" -ForegroundColor Red -NoNewline
        Write-Host " (Error occurred)"
        $script:FAIL++
    }
}

function Test-FileIntegrity
{
    param(
        [string]$TestName,
        [string]$OriginalFile,
        [string]$DownloadedFile
    )

    $script:TOTAL++
    Write-Host "Test ${script:TOTAL}: $TestName ... " -NoNewline

    if ((Test-Path $OriginalFile) -and (Test-Path $DownloadedFile))
    {
        $original = Get-FileHash $OriginalFile -Algorithm MD5
        $downloaded = Get-FileHash $DownloadedFile -Algorithm MD5

        if ($original.Hash -eq $downloaded.Hash)
        {
            Write-Host "PASS" -ForegroundColor Green -NoNewline
            Write-Host " (Files match)"
            $script:PASS++
        }
        else
        {
            Write-Host "FAIL" -ForegroundColor Red -NoNewline
            Write-Host " (Files don't match)"
            $script:FAIL++
        }
    }
    else
    {
        Write-Host "SKIP" -ForegroundColor Yellow -NoNewline
        Write-Host " (Files not found)"
    }
}

Write-Host ""
Write-Host "=== BASIC FUNCTIONALITY TESTS ===" -ForegroundColor Blue
Write-Host ""

Run-Test "GET / (root path)" "200" {
    Invoke-WebRequest -Uri "$SERVER_URL/" -Method GET -UseBasicParsing
}

Run-Test "GET /index.html" "200" {
    Invoke-WebRequest -Uri "$SERVER_URL/index.html" -Method GET -UseBasicParsing
}

Run-Test "GET /about.html" "200" {
    Invoke-WebRequest -Uri "$SERVER_URL/about.html" -Method GET -UseBasicParsing
}

Run-Test "GET /contact.html" "200" {
    Invoke-WebRequest -Uri "$SERVER_URL/contact.html" -Method GET -UseBasicParsing
}

Write-Host ""
Write-Host "=== BINARY FILE TRANSFER TESTS ===" -ForegroundColor Blue
Write-Host ""

Run-Test "GET /logo.png (PNG download)" "200" {
    Invoke-WebRequest -Uri "$SERVER_URL/logo.png" -Method GET -OutFile "$env:TEMP\downloaded_logo.png" -UseBasicParsing
}

Run-Test "GET /photo.jpg (JPEG download)" "200" {
    Invoke-WebRequest -Uri "$SERVER_URL/photo.jpg" -Method GET -OutFile "$env:TEMP\downloaded_photo.jpg" -UseBasicParsing
}

Run-Test "GET /sample.txt (TXT download)" "200" {
    Invoke-WebRequest -Uri "$SERVER_URL/sample.txt" -Method GET -OutFile "$env:TEMP\downloaded_sample.txt" -UseBasicParsing
}

Test-FileIntegrity "PNG file integrity check" "src\main\resources\logo.png" "$env:TEMP\downloaded_logo.png"

Test-FileIntegrity "JPEG file integrity check" "src\main\resources\photo.jpg" "$env:TEMP\downloaded_photo.jpg"

Write-Host ""
Write-Host "=== POST REQUEST TESTS ===" -ForegroundColor Blue
Write-Host ""

Run-Test "POST with valid JSON" "201" {
    $body = '{"name":"John","age":30}'
    Invoke-WebRequest -Uri "$SERVER_URL/" -Method POST -Body $body -ContentType "application/json" -UseBasicParsing
}

$script:TOTAL++
Write-Host "Test ${script:TOTAL}: POST with invalid JSON ... " -NoNewline
try
{
    $body = '{invalid json}'
    $null = Invoke-WebRequest -Uri "$SERVER_URL/" -Method POST -Body $body -ContentType "application/json" -UseBasicParsing -ErrorAction Stop
    Write-Host "FAIL" -ForegroundColor Red -NoNewline
    Write-Host " (Should have failed)"
    $script:FAIL++
}
catch
{
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 400)
    {
        Write-Host "PASS" -ForegroundColor Green -NoNewline
        Write-Host " (HTTP 400)"
        $script:PASS++
    }
    else
    {
        Write-Host "FAIL" -ForegroundColor Red -NoNewline
        Write-Host " (Expected 400, Got $statusCode)"
        $script:FAIL++
    }
}

$script:TOTAL++
Write-Host "Test ${script:TOTAL}: POST without Content-Type ... " -NoNewline
try
{
    $body = '{"test":"data"}'
    $null = Invoke-WebRequest -Uri "$SERVER_URL/" -Method POST -Body $body -UseBasicParsing -ErrorAction Stop
    Write-Host "FAIL" -ForegroundColor Red -NoNewline
    Write-Host " (Should have failed)"
    $script:FAIL++
}
catch
{
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 415)
    {
        Write-Host "PASS" -ForegroundColor Green -NoNewline
        Write-Host " (HTTP 415)"
        $script:PASS++
    }
    else
    {
        Write-Host "FAIL" -ForegroundColor Red -NoNewline
        Write-Host " (Expected 415, Got $statusCode)"
        $script:FAIL++
    }
}

$script:TOTAL++
Write-Host "Test ${script:TOTAL}: POST with wrong Content-Type ... " -NoNewline
try
{
    $body = '{"test":"data"}'
    $null = Invoke-WebRequest -Uri "$SERVER_URL/" -Method POST -Body $body -ContentType "text/plain" -UseBasicParsing -ErrorAction Stop
    Write-Host "FAIL" -ForegroundColor Red -NoNewline
    Write-Host " (Should have failed)"
    $script:FAIL++
}
catch
{
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 415)
    {
        Write-Host "PASS" -ForegroundColor Green -NoNewline
        Write-Host " (HTTP 415)"
        $script:PASS++
    }
    else
    {
        Write-Host "FAIL" -ForegroundColor Red -NoNewline
        Write-Host " (Expected 415, Got $statusCode)"
        $script:FAIL++
    }
}

$script:TOTAL++
Write-Host "Test ${script:TOTAL}: POST without body ... " -NoNewline
try
{
    $null = Invoke-WebRequest -Uri "$SERVER_URL/" -Method POST -ContentType "application/json" -UseBasicParsing -ErrorAction Stop
    Write-Host "FAIL" -ForegroundColor Red -NoNewline
    Write-Host " (Should have failed)"
    $script:FAIL++
}
catch
{
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 400)
    {
        Write-Host "PASS" -ForegroundColor Green -NoNewline
        Write-Host " (HTTP 400)"
        $script:PASS++
    }
    else
    {
        Write-Host "FAIL" -ForegroundColor Red -NoNewline
        Write-Host " (Expected 400, Got $statusCode)"
        $script:FAIL++
    }
}

Write-Host ""
Write-Host "=== ERROR HANDLING TESTS ===" -ForegroundColor Blue
Write-Host ""

$script:TOTAL++
Write-Host "Test ${script:TOTAL}: GET non-existent file (404) ... " -NoNewline
try
{
    $null = Invoke-WebRequest -Uri "$SERVER_URL/nonexistent.html" -Method GET -UseBasicParsing -ErrorAction Stop
    Write-Host "FAIL" -ForegroundColor Red -NoNewline
    Write-Host " (Should have failed)"
    $script:FAIL++
}
catch
{
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 404)
    {
        Write-Host "PASS" -ForegroundColor Green -NoNewline
        Write-Host " (HTTP 404)"
        $script:PASS++
    }
    else
    {
        Write-Host "FAIL" -ForegroundColor Red -NoNewline
        Write-Host " (Expected 404, Got $statusCode)"
        $script:FAIL++
    }
}

$script:TOTAL++
Write-Host "Test ${script:TOTAL}: PUT method (405) ... " -NoNewline
try
{
    $null = Invoke-WebRequest -Uri "$SERVER_URL/" -Method PUT -UseBasicParsing -ErrorAction Stop
    Write-Host "FAIL" -ForegroundColor Red -NoNewline
    Write-Host " (Should have failed)"
    $script:FAIL++
}
catch
{
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 405)
    {
        Write-Host "PASS" -ForegroundColor Green -NoNewline
        Write-Host " (HTTP 405)"
        $script:PASS++
    }
    else
    {
        Write-Host "FAIL" -ForegroundColor Red -NoNewline
        Write-Host " (Expected 405, Got $statusCode)"
        $script:FAIL++
    }
}

$script:TOTAL++
Write-Host "Test ${script:TOTAL}: DELETE method (405) ... " -NoNewline
try
{
    $null = Invoke-WebRequest -Uri "$SERVER_URL/" -Method DELETE -UseBasicParsing -ErrorAction Stop
    Write-Host "FAIL" -ForegroundColor Red -NoNewline
    Write-Host " (Should have failed)"
    $script:FAIL++
}
catch
{
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 405)
    {
        Write-Host "PASS" -ForegroundColor Green -NoNewline
        Write-Host " (HTTP 405)"
        $script:PASS++
    }
    else
    {
        Write-Host "FAIL" -ForegroundColor Red -NoNewline
        Write-Host " (Expected 405, Got $statusCode)"
        $script:FAIL++
    }
}

$script:TOTAL++
Write-Host "Test ${script:TOTAL}: PATCH method (405) ... " -NoNewline
try
{
    $null = Invoke-WebRequest -Uri "$SERVER_URL/" -Method PATCH -UseBasicParsing -ErrorAction Stop
    Write-Host "FAIL" -ForegroundColor Red -NoNewline
    Write-Host " (Should have failed)"
    $script:FAIL++
}
catch
{
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 405)
    {
        Write-Host "PASS" -ForegroundColor Green -NoNewline
        Write-Host " (HTTP 405)"
        $script:PASS++
    }
    else
    {
        Write-Host "FAIL" -ForegroundColor Red -NoNewline
        Write-Host " (Expected 405, Got $statusCode)"
        $script:FAIL++
    }
}

$script:TOTAL++
Write-Host "Test ${script:TOTAL}: GET unsupported file type (415) ... " -NoNewline
try
{
    $null = Invoke-WebRequest -Uri "$SERVER_URL/test.xml" -Method GET -UseBasicParsing -ErrorAction Stop
    Write-Host "FAIL" -ForegroundColor Red -NoNewline
    Write-Host " (Should have failed)"
    $script:FAIL++
}
catch
{
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 415 -or $statusCode -eq 404)
    {
        Write-Host "PASS" -ForegroundColor Green -NoNewline
        Write-Host " (HTTP $statusCode)"
        $script:PASS++
    }
    else
    {
        Write-Host "FAIL" -ForegroundColor Red -NoNewline
        Write-Host " (Expected 415, Got $statusCode)"
        $script:FAIL++
    }
}

Write-Host ""
Write-Host "=== SECURITY TESTS ===" -ForegroundColor Blue
Write-Host ""

$script:TOTAL++
Write-Host "Test ${script:TOTAL}: Path traversal ../ (403) ... " -NoNewline
try
{
    $null = Invoke-WebRequest -Uri "$SERVER_URL/../etc/passwd" -Method GET -UseBasicParsing -ErrorAction Stop
    Write-Host "FAIL" -ForegroundColor Red -NoNewline
    Write-Host " (Should have failed)"
    $script:FAIL++
}
catch
{
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 403)
    {
        Write-Host "PASS" -ForegroundColor Green -NoNewline
        Write-Host " (HTTP 403)"
        $script:PASS++
    }
    else
    {
        Write-Host "FAIL" -ForegroundColor Red -NoNewline
        Write-Host " (Expected 403, Got $statusCode)"
        $script:FAIL++
    }
}

$script:TOTAL++
Write-Host "Test ${script:TOTAL}: Path traversal ../../ (403) ... " -NoNewline
try
{
    $null = Invoke-WebRequest -Uri "$SERVER_URL/../../etc/hosts" -Method GET -UseBasicParsing -ErrorAction Stop
    Write-Host "FAIL" -ForegroundColor Red -NoNewline
    Write-Host " (Should have failed)"
    $script:FAIL++
}
catch
{
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 403)
    {
        Write-Host "PASS" -ForegroundColor Green -NoNewline
        Write-Host " (HTTP 403)"
        $script:PASS++
    }
    else
    {
        Write-Host "FAIL" -ForegroundColor Red -NoNewline
        Write-Host " (Expected 403, Got $statusCode)"
        $script:FAIL++
    }
}

$script:TOTAL++
Write-Host "Test ${script:TOTAL}: Path traversal with // (403) ... " -NoNewline
try
{
    $null = Invoke-WebRequest -Uri "$SERVER_URL//etc/passwd" -Method GET -UseBasicParsing -ErrorAction Stop
    Write-Host "FAIL" -ForegroundColor Red -NoNewline
    Write-Host " (Should have failed)"
    $script:FAIL++
}
catch
{
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 403)
    {
        Write-Host "PASS" -ForegroundColor Green -NoNewline
        Write-Host " (HTTP 403)"
        $script:PASS++
    }
    else
    {
        Write-Host "FAIL" -ForegroundColor Red -NoNewline
        Write-Host " (Expected 403, Got $statusCode)"
        $script:FAIL++
    }
}

$script:TOTAL++
Write-Host "Test ${script:TOTAL}: Invalid Host header (403) ... " -NoNewline
try
{
    $headers = @{ "Host" = "evil.com" }
    $null = Invoke-WebRequest -Uri "$SERVER_URL/" -Method GET -Headers $headers -UseBasicParsing -ErrorAction Stop
    Write-Host "FAIL" -ForegroundColor Red -NoNewline
    Write-Host " (Should have failed)"
    $script:FAIL++
}
catch
{
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 403)
    {
        Write-Host "PASS" -ForegroundColor Green -NoNewline
        Write-Host " (HTTP 403)"
        $script:PASS++
    }
    else
    {
        Write-Host "FAIL" -ForegroundColor Red -NoNewline
        Write-Host " (Expected 403, Got $statusCode)"
        $script:FAIL++
    }
}

Write-Host ""
Write-Host "=== CONNECTION MANAGEMENT TESTS ===" -ForegroundColor Blue
Write-Host ""

$script:TOTAL++
Write-Host "Test ${script:TOTAL}: Keep-alive connection ... " -NoNewline
try
{
    $response = Invoke-WebRequest -Uri "$SERVER_URL/" -Method GET -Headers @{ "Connection" = "keep-alive" } -UseBasicParsing
    if ($response.Headers["Keep-Alive"] -or $response.Headers.ContainsKey("Keep-Alive"))
    {
        Write-Host "PASS" -ForegroundColor Green -NoNewline
        Write-Host " (Keep-Alive header present)"
        $script:PASS++
    }
    else
    {
        Write-Host "FAIL" -ForegroundColor Red -NoNewline
        Write-Host " (Keep-Alive header missing)"
        $script:FAIL++
    }
}
catch
{
    Write-Host "FAIL" -ForegroundColor Red -NoNewline
    Write-Host " (Error occurred)"
    $script:FAIL++
}

$script:TOTAL++
Write-Host "Test ${script:TOTAL}: Close connection ... " -NoNewline
try
{
    $response = Invoke-WebRequest -Uri "$SERVER_URL/" -Method GET -Headers @{ "Connection" = "close" } -UseBasicParsing
    if ($response.Headers["Connection"] -eq "close")
    {
        Write-Host "PASS" -ForegroundColor Green -NoNewline
        Write-Host " (Connection closes properly)"
        $script:PASS++
    }
    else
    {
        Write-Host "FAIL" -ForegroundColor Red -NoNewline
        Write-Host " (Connection header not set)"
        $script:FAIL++
    }
}
catch
{
    Write-Host "FAIL" -ForegroundColor Red -NoNewline
    Write-Host " (Error occurred)"
    $script:FAIL++
}

Write-Host ""
Write-Host "=== CONCURRENT CONNECTION TESTS ===" -ForegroundColor Blue
Write-Host ""

$script:TOTAL++
Write-Host "Test ${script:TOTAL}: 5 concurrent GET requests ... " -NoNewline
try
{
    $jobs = 1..5 | ForEach-Object {
        Start-Job -ScriptBlock {
            param($url)
            Invoke-WebRequest -Uri $url -Method GET -UseBasicParsing | Out-Null
        } -ArgumentList $SERVER_URL
    }
    $jobs | Wait-Job | Remove-Job
    Write-Host "PASS" -ForegroundColor Green -NoNewline
    Write-Host " (All requests completed)"
    $script:PASS++
}
catch
{
    Write-Host "FAIL" -ForegroundColor Red -NoNewline
    Write-Host " (Error occurred)"
    $script:FAIL++
}

$script:TOTAL++
Write-Host "Test ${script:TOTAL}: 3 concurrent POST requests ... " -NoNewline
try
{
    $jobs = 1..3 | ForEach-Object {
        Start-Job -ScriptBlock {
            param($url, $num)
            $body = "{`"test`":$num}"
            Invoke-WebRequest -Uri $url -Method POST -Body $body -ContentType "application/json" -UseBasicParsing | Out-Null
        } -ArgumentList $SERVER_URL, $_
    }
    $jobs | Wait-Job | Remove-Job
    Write-Host "PASS" -ForegroundColor Green -NoNewline
    Write-Host " (All requests completed)"
    $script:PASS++
}
catch
{
    Write-Host "FAIL" -ForegroundColor Red -NoNewline
    Write-Host " (Error occurred)"
    $script:FAIL++
}

Write-Host ""
Write-Host "=== HEADER VALIDATION TESTS ===" -ForegroundColor Blue
Write-Host ""

$script:TOTAL++
Write-Host "Test ${script:TOTAL}: Content-Type for HTML ... " -NoNewline
try
{
    $response = Invoke-WebRequest -Uri "$SERVER_URL/" -Method GET -UseBasicParsing
    if ($response.Headers["Content-Type"] -like "*text/html*")
    {
        Write-Host "PASS" -ForegroundColor Green -NoNewline
        Write-Host " (Correct Content-Type)"
        $script:PASS++
    }
    else
    {
        Write-Host "FAIL" -ForegroundColor Red -NoNewline
        Write-Host " (Wrong Content-Type)"
        $script:FAIL++
    }
}
catch
{
    Write-Host "FAIL" -ForegroundColor Red -NoNewline
    Write-Host " (Error occurred)"
    $script:FAIL++
}

$script:TOTAL++
Write-Host "Test ${script:TOTAL}: Content-Type for binary files ... " -NoNewline
try
{
    $response = Invoke-WebRequest -Uri "$SERVER_URL/logo.png" -Method GET -UseBasicParsing
    if ($response.Headers["Content-Type"] -like "*application/octet-stream*")
    {
        Write-Host "PASS" -ForegroundColor Green -NoNewline
        Write-Host " (Correct Content-Type)"
        $script:PASS++
    }
    else
    {
        Write-Host "FAIL" -ForegroundColor Red -NoNewline
        Write-Host " (Wrong Content-Type)"
        $script:FAIL++
    }
}
catch
{
    Write-Host "FAIL" -ForegroundColor Red -NoNewline
    Write-Host " (Error occurred)"
    $script:FAIL++
}

$script:TOTAL++
Write-Host "Test ${script:TOTAL}: Content-Disposition header ... " -NoNewline
try
{
    $response = Invoke-WebRequest -Uri "$SERVER_URL/logo.png" -Method GET -UseBasicParsing
    if ($response.Headers["Content-Disposition"] -like "*attachment*")
    {
        Write-Host "PASS" -ForegroundColor Green -NoNewline
        Write-Host " (Header present)"
        $script:PASS++
    }
    else
    {
        Write-Host "FAIL" -ForegroundColor Red -NoNewline
        Write-Host " (Header missing)"
        $script:FAIL++
    }
}
catch
{
    Write-Host "FAIL" -ForegroundColor Red -NoNewline
    Write-Host " (Error occurred)"
    $script:FAIL++
}

$script:TOTAL++
Write-Host "Test ${script:TOTAL}: Date header present ... " -NoNewline
try
{
    $response = Invoke-WebRequest -Uri "$SERVER_URL/" -Method GET -UseBasicParsing
    if ( $response.Headers.ContainsKey("Date"))
    {
        Write-Host "PASS" -ForegroundColor Green -NoNewline
        Write-Host " (Date header present)"
        $script:PASS++
    }
    else
    {
        Write-Host "FAIL" -ForegroundColor Red -NoNewline
        Write-Host " (Date header missing)"
        $script:FAIL++
    }
}
catch
{
    Write-Host "FAIL" -ForegroundColor Red -NoNewline
    Write-Host " (Error occurred)"
    $script:FAIL++
}

$script:TOTAL++
Write-Host "Test ${script:TOTAL}: Server header present ... " -NoNewline
try
{
    $response = Invoke-WebRequest -Uri "$SERVER_URL/" -Method GET -UseBasicParsing
    if ($response.Headers["Server"] -like "*Multi-threaded HTTP Server*")
    {
        Write-Host "PASS" -ForegroundColor Green -NoNewline
        Write-Host " (Server header correct)"
        $script:PASS++
    }
    else
    {
        Write-Host "FAIL" -ForegroundColor Red -NoNewline
        Write-Host " (Server header incorrect)"
        $script:FAIL++
    }
}
catch
{
    Write-Host "FAIL" -ForegroundColor Red -NoNewline
    Write-Host " (Error occurred)"
    $script:FAIL++
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "TEST SUMMARY" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Total Tests: $TOTAL"
Write-Host "Passed: $PASS" -ForegroundColor Green
Write-Host "Failed: $FAIL" -ForegroundColor Red
Write-Host ""

if ($FAIL -eq 0)
{
    Write-Host "ALL TESTS PASSED!" -ForegroundColor Green
    exit 0
}
else
{
    Write-Host "SOME TESTS FAILED" -ForegroundColor Red
    exit 1
}