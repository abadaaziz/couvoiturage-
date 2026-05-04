# Test reservations endpoint with a logged-in session
$ProgressPreference = 'SilentlyContinue'
$jar = [System.Net.CookieContainer]::new()

# 1. Login as test passager
$req = [System.Net.HttpWebRequest]::Create([System.Uri]"http://localhost:8080/login")
$req.Method = "POST"
$req.ContentType = "application/x-www-form-urlencoded"
$req.CookieContainer = $jar
$req.AllowAutoRedirect = $false
$body = [System.Text.Encoding]::UTF8.GetBytes("email=test.passager@demo.com&motDePasse=testpass123")
$req.ContentLength = $body.Length
$s = $req.GetRequestStream()
$s.Write($body, 0, $body.Length)
$s.Close()
try {
    $resp = $req.GetResponse()
    Write-Host ("Login: " + [int]$resp.StatusCode + " -> " + $resp.Headers["Location"])
    $resp.Close()
} catch {
    Write-Host "Login failed: $_"
    exit
}

# 2. Get reservations
$req2 = [System.Net.HttpWebRequest]::Create([System.Uri]"http://localhost:8080/reservation/mes")
$req2.Method = "GET"
$req2.Accept = "application/json"
$req2.CookieContainer = $jar

try {
    $resp2 = $req2.GetResponse()
    $reader = [System.IO.StreamReader]::new($resp2.GetResponseStream(), [System.Text.Encoding]::UTF8)
    $content = $reader.ReadToEnd()
    $reader.Close(); $resp2.Close()
    Write-Host ("Status: " + [int]$resp2.StatusCode)
    Write-Host ("CT: " + $resp2.ContentType)
    Write-Host $content
} catch [System.Net.WebException] {
    $errResp = $_.Exception.Response
    if ($errResp) {
        $reader = [System.IO.StreamReader]::new($errResp.GetResponseStream())
        $errBody = $reader.ReadToEnd()
        Write-Host ("Error status: " + [int]$errResp.StatusCode)
        Write-Host ("Error body: " + $errBody)
    } else {
        Write-Host "Network error: $_"
    }
}
