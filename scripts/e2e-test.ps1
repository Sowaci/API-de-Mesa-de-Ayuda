$ErrorActionPreference = 'Continue'
$base = 'http://localhost:8080'
$pass = 0
$fail = 0

function Test-Case {
    param([string]$name, [scriptblock]$script)
    try {
        $r = & $script
        $global:pass++
        Write-Output "[PASS] $name $r"
    } catch {
        $global:fail++
        $msg = $_.Exception.Message
        if ($_.Exception.Response) {
            try {
                $stream = $_.Exception.Response.GetResponseStream()
                $reader = New-Object System.IO.StreamReader($stream)
                $msg = $reader.ReadToEnd()
            } catch {}
        }
        Write-Output "[FAIL] $name -> $msg"
    }
}

function Invoke-Api {
    param([string]$method, [string]$path, $body = $null, [string]$token = $null)
    $headers = @{}
    if ($token) { $headers['Authorization'] = "Bearer $token" }
    $params = @{ Method = $method; Uri = "$base$path"; Headers = $headers }
    if ($null -ne $body) {
        $params.ContentType = 'application/json'
        $params.Body = ($body | ConvertTo-Json -Depth 5)
    }
    return Invoke-RestMethod @params
}

function Get-Status {
    param([string]$method, [string]$path, $body = $null, [string]$token = $null)
    try {
        Invoke-Api -method $method -path $path -body $body -token $token | Out-Null
        return 200
    } catch {
        if ($_.Exception.Response) { return [int]$_.Exception.Response.StatusCode }
        return -1
    }
}

# 1. Ping
Test-Case "GET /api/ping = 200" { Invoke-Api GET '/api/ping' | Out-Null; 'OK' }

# 2. Registro exitoso
$reg = Test-Case "POST /api/auth/registro = 201" {
    $r = Invoke-Api POST '/api/auth/registro' @{ nombre = 'Nuevo Usuario'; email = 'nuevo@test.com'; password = 'clave123' }
    if ($r.rol -ne 'USUARIO') { throw "rol incorrecto: $($r.rol)" }
    if (-not $r.accessToken -or -not $r.refreshToken) { throw 'faltan tokens' }
    'OK'
}

# 3. Registro duplicado -> 409
Test-Case "POST /api/auth/registro duplicado = 409" {
    $s = Get-Status POST '/api/auth/registro' @{ nombre = 'Nuevo Usuario'; email = 'nuevo@test.com'; password = 'clave123' }
    if ($s -ne 409) { throw "status=$s" }
    "status=$s"
}

# 4. Registro con datos invalidos -> 400
Test-Case "POST /api/auth/registro invalido = 400" {
    $s = Get-Status POST '/api/auth/registro' @{ nombre = ''; email = 'mal-formato'; password = '123' }
    if ($s -ne 400) { throw "status=$s" }
    "status=$s"
}

# 5. Login admin
Test-Case "POST /api/auth/login admin = 200" {
    $r = Invoke-Api POST '/api/auth/login' @{ email = 'admin@helpdesk.com'; password = 'admin123' }
    $script:adminTokens = $r
    if ($r.rol -ne 'ADMIN') { throw "rol=$($r.rol)" }
    'OK'
}

# 6. Login credenciales incorrectas -> 401
Test-Case "POST /api/auth/login malas credenciales = 401" {
    $s = Get-Status POST '/api/auth/login' @{ email = 'admin@helpdesk.com'; password = 'incorrecta' }
    if ($s -ne 401) { throw "status=$s" }
    "status=$s"
}

# 7. Ruta protegida sin token -> 401
Test-Case "GET /api/tickets sin token = 401" {
    $s = Get-Status GET '/api/tickets'
    if ($s -ne 401) { throw "status=$s" }
    "status=$s"
}

# 8. Login usuario normal
Test-Case "POST /api/auth/login usuario = 200" {
    $r = Invoke-Api POST '/api/auth/login' @{ email = 'usuario@helpdesk.com'; password = 'usuario123' }
    $script:userTokens = $r
    'OK'
}

# 9. Login soporte
Test-Case "POST /api/auth/login soporte = 200" {
    $r = Invoke-Api POST '/api/auth/login' @{ email = 'soporte@helpdesk.com'; password = 'soporte123' }
    $script:soporteTokens = $r
    'OK'
}

# 10. USUARIO intenta listar todos los tickets -> 403
Test-Case "GET /api/tickets con USUARIO = 403" {
    $s = Get-Status GET '/api/tickets' -token $userTokens.accessToken
    if ($s -ne 403) { throw "status=$s" }
    "status=$s"
}

# 11. SOPORTE lista todos con paginacion
Test-Case "GET /api/tickets con SOPORTE = 200 (paginacion)" {
    $r = Invoke-Api GET '/api/tickets?page=0&size=5' -token $soporteTokens.accessToken
    if ($null -eq $r.content) { throw 'sin paginacion' }
    if ($r.content.Count -eq 0) { throw 'sin tickets' }
    "totalElements=$($r.totalElements) size=$($r.size)"
}

# 12. Crear ticket ALTA por USUARIO, verificar SLA = +4h
Test-Case "POST /api/tickets ALTA con SLA=+4h = 201" {
    $r = Invoke-Api POST '/api/tickets' @{ titulo = 'Incidente critico'; descripcion = 'Sistema de facturacion caido'; prioridad = 'ALTA' } -token $userTokens.accessToken
    $diff = ([datetime]$r.slaVenceEn - [datetime]$r.creadoEn).TotalHours
    if ($r.estado -ne 'ABIERTO') { throw "estado=$($r.estado)" }
    if ($diff -ne 4) { throw "SLA horas=$diff" }
    if ($r.vencido -ne $false) { throw 'no deberia estar vencido' }
    $script:ticketAlta = $r
    "slaHoras=$diff id=$($r.id)"
}

# 13. Crear ticket BAJA, SLA = +72h
Test-Case "POST /api/tickets BAJA con SLA=+72h = 201" {
    $r = Invoke-Api POST '/api/tickets' @{ titulo = 'Solicitud menor'; descripcion = 'Actualizar datos de contacto'; prioridad = 'BAJA' } -token $userTokens.accessToken
    $diff = ([datetime]$r.slaVenceEn - [datetime]$r.creadoEn).TotalHours
    if ($diff -ne 72) { throw "SLA horas=$diff" }
    "slaHoras=$diff"
}

# 14. Crear ticket con prioridad invalida -> 400
Test-Case "POST /api/tickets prioridad invalida = 400" {
    $s = Get-Status POST '/api/tickets' @{ titulo = 'x'; descripcion = 'y'; prioridad = 'URGENTISIMA' } -token $userTokens.accessToken
    if ($s -ne 400) { throw "status=$s" }
    "status=$s"
}

# 15. Crear ticket sin titulo -> 400
Test-Case "POST /api/tickets sin titulo = 400" {
    $s = Get-Status POST '/api/tickets' @{ titulo = ''; descripcion = 'y'; prioridad = 'BAJA' } -token $userTokens.accessToken
    if ($s -ne 400) { throw "status=$s" }
    "status=$s"
}

# 16. POST /api/tickets sin token -> 401
Test-Case "POST /api/tickets sin token = 401" {
    $s = Get-Status POST '/api/tickets' @{ titulo = 'x'; descripcion = 'y'; prioridad = 'BAJA' }
    if ($s -ne 401) { throw "status=$s" }
    "status=$s"
}

# 17. GET /api/tickets/mios
Test-Case "GET /api/tickets/mios = 200" {
    $r = Invoke-Api GET '/api/tickets/mios' -token $userTokens.accessToken
    "count=$($r.Count)"
}

# 18. USUARIO consulta ticket ajeno -> 403
Test-Case "GET /api/tickets/{id} ticket ajeno con USUARIO = 403" {
    $ajeno = Invoke-Api POST '/api/tickets' @{ titulo = 'Ticket de soporte'; descripcion = 'Creado por soporte'; prioridad = 'MEDIA' } -token $soporteTokens.accessToken
    $s = Get-Status GET "/api/tickets/$($ajeno.id)" -token $userTokens.accessToken
    if ($s -ne 403) { throw "status=$s" }
    "status=$s"
}

# 19. USUARIO consulta su propio ticket -> 200
Test-Case "GET /api/tickets/{id} propio con USUARIO = 200" {
    $r = Invoke-Api GET "/api/tickets/$($ticketAlta.id)" -token $userTokens.accessToken
    "id=$($r.id) vencido=$($r.vencido)"
}

# 20. SOPORTE consulta cualquier ticket -> 200
Test-Case "GET /api/tickets/{id} con SOPORTE = 200" {
    $r = Invoke-Api GET "/api/tickets/$($ticketAlta.id)" -token $soporteTokens.accessToken
    "id=$($r.id)"
}

# 21. GET /api/tickets/vencidos con SOPORTE (debe incluir el sembrado vencido)
Test-Case "GET /api/tickets/vencidos con SOPORTE = 200" {
    $r = @(Invoke-Api GET '/api/tickets/vencidos' -token $soporteTokens.accessToken)
    if ($r.Count -eq 0) { throw 'no hay vencidos' }
    $vencido = @($r | Where-Object { $_.vencido -eq $true })
    if ($vencido.Count -eq 0) { throw "ninguno marcado vencido: $($r | ConvertTo-Json -Depth 5)" }
    "vencidos=$($r.Count) verificadosVencido=$($vencido.Count)"
}

# 22. GET /api/tickets/vencidos con USUARIO -> 403
Test-Case "GET /api/tickets/vencidos con USUARIO = 403" {
    $s = Get-Status GET '/api/tickets/vencidos' -token $userTokens.accessToken
    if ($s -ne 403) { throw "status=$s" }
    "status=$s"
}

# 23. PATCH estado con SOPORTE -> 200
Test-Case "PATCH /api/tickets/{id}/estado con SOPORTE = 200" {
    $r = Invoke-Api PATCH "/api/tickets/$($ticketAlta.id)/estado" @{ estado = 'EN_PROCESO' } -token $soporteTokens.accessToken
    if ($r.estado -ne 'EN_PROCESO') { throw "estado=$($r.estado)" }
    "estado=$($r.estado)"
}

# 24. PATCH estado con USUARIO -> 403
Test-Case "PATCH /api/tickets/{id}/estado con USUARIO = 403" {
    $s = Get-Status PATCH "/api/tickets/$($ticketAlta.id)/estado" @{ estado = 'RESUELTO' } -token $userTokens.accessToken
    if ($s -ne 403) { throw "status=$s" }
    "status=$s"
}

# 25. PATCH con estado invalido -> 400
Test-Case "PATCH estado invalido = 400" {
    $s = Get-Status PATCH "/api/tickets/$($ticketAlta.id)/estado" @{ estado = 'FINALIZADO' } -token $soporteTokens.accessToken
    if ($s -ne 400) { throw "status=$s" }
    "status=$s"
}

# 26. Ticket inexistente -> 404
Test-Case "GET /api/tickets/99999 = 404" {
    $s = Get-Status GET '/api/tickets/99999' -token $soporteTokens.accessToken
    if ($s -ne 404) { throw "status=$s" }
    "status=$s"
}

# 27. POST /api/admin/soporte con ADMIN -> 200
Test-Case "POST /api/admin/soporte con ADMIN = 200" {
    $r = Invoke-Api POST '/api/admin/soporte' @{ email = 'nuevo@test.com' } -token $adminTokens.accessToken
    if ($r.rol -ne 'SOPORTE') { throw "rol=$($r.rol)" }
    "rol=$($r.rol)"
}

# 28. POST /api/admin/soporte con SOPORTE -> 403
Test-Case "POST /api/admin/soporte con SOPORTE = 403" {
    $s = Get-Status POST '/api/admin/soporte' @{ email = 'nuevo@test.com' } -token $soporteTokens.accessToken
    if ($s -ne 403) { throw "status=$s" }
    "status=$s"
}

# 29. GET /api/admin/estadisticas con ADMIN -> 200 (bono)
Test-Case "GET /api/admin/estadisticas con ADMIN = 200" {
    $r = Invoke-Api GET '/api/admin/estadisticas' -token $adminTokens.accessToken
    "porEstado=$($r.ticketsPorEstado.PSObject.Properties.Count) pctSla=$($r.porcentajeCumplimientoSla)"
}

# 30. Refresh token valido -> 200 con rotacion
Test-Case "POST /api/auth/refresh valido = 200" {
    $r = Invoke-Api POST '/api/auth/refresh' @{ refreshToken = $userTokens.refreshToken }
    if (-not $r.accessToken -or -not $r.refreshToken) { throw 'faltan tokens' }
    if ($r.refreshToken -eq $userTokens.refreshToken) { throw 'no hubo rotacion' }
    $script:nuevoRefresh = $r.refreshToken
    'OK (con rotacion)'
}

# 31. Refresh token viejo (rotado) reutilizado -> 401
Test-Case "POST /api/auth/refresh token ya rotado = 401" {
    $s = Get-Status POST '/api/auth/refresh' @{ refreshToken = $userTokens.refreshToken }
    if ($s -ne 401) { throw "status=$s" }
    "status=$s"
}

# 32. Refresh token inexistente -> 401
Test-Case "POST /api/auth/refresh token inexistente = 401" {
    $s = Get-Status POST '/api/auth/refresh' @{ refreshToken = 'token-que-no-existe' }
    if ($s -ne 401) { throw "status=$s" }
    "status=$s"
}

# 33. Logout con el refresh nuevo, luego refresh -> 401
Test-Case "POST /api/auth/logout + refresh posterior = 401" {
    $s1 = Get-Status POST '/api/auth/logout' @{ refreshToken = $nuevoRefresh } -token $userTokens.accessToken
    if ($s1 -ne 200) { throw "logout status=$s1" }
    $s2 = Get-Status POST '/api/auth/refresh' @{ refreshToken = $nuevoRefresh }
    if ($s2 -ne 401) { throw "refresh post-logout status=$s2" }
    "logout=$s1 refreshDespues=$s2"
}

# 34. Refresh token no sirve como access token -> 401
Test-Case "GET /api/tickets/mios con refresh token = 401" {
    $s = Get-Status GET '/api/tickets/mios' -token $soporteTokens.refreshToken
    if ($s -ne 401) { throw "status=$s" }
    "status=$s"
}

Write-Output "==============================="
Write-Output "RESULTADO: $pass pasaron, $fail fallaron"
if ($fail -gt 0) { exit 1 }
