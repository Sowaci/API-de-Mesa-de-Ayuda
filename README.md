# API de Mesa de Ayuda (Helpdesk) con SLA — Spring Boot + JWT

## Ejecución

```bash
mvn spring-boot:run

```

La API queda en `http://localhost:8080`. La consola H2 está disponible en
`http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:helpdesk`, usuario `sa`, sin contraseña).

## Usuarios de prueba (sembrados al arrancar)

| Rol      | Email                 | Password     |
|----------|-----------------------|--------------|
| ADMIN    | admin@helpdesk.com    | admin123     |
| SOPORTE  | soporte@helpdesk.com  | soporte123   |
| USUARIO  | usuario@helpdesk.com  | usuario123   |

El sembrado también crea tickets de ejemplo, **incluido uno vencido** para demostrar
`GET /api/tickets/vencidos`.

## Estrategia de persistencia del refresh token: **Opción A (base de datos)**

Se eligió la **Opción A** porque:

1. **Permite revocación real**: el logout marca `revocado = true`, por lo que un
   refresh token ya no puede reutilizarse (requisito del taller).
2. **Detección de reutilización**: si un token ya rotado se vuelve a enviar, el
   servidor lo detecta y responde 401.
3. **Es la más didáctica**: obliga a razonar sobre el ciclo de vida del token
   (emisión → renovación → rotación → revocación).
4. El token se almacena **hasheado con SHA-256**, nunca en claro: una fuga en la
   base de datos no expone tokens reutilizables.

### Ciclo de vida del refresh token

1. **Login/registro**: se emite un access token (JWT, 15 min) y un refresh token
   aleatorio (7 días) que se persiste en la tabla `refresh_tokens`.
2. **Renovación** (`POST /api/auth/refresh`): si el refresh token es válido, no
   está revocado y no ha expirado, se emite un **nuevo access token** y se aplica
   **rotación**: el refresh token usado se revoca y se emite uno nuevo.
3. **Logout** (`POST /api/auth/logout`): el refresh token del usuario se revoca;
   un `/refresh` posterior con ese token responde **401**.
4. El refresh token **no sirve para acceder a rutas protegidas** (el filtro JWT
   solo acepta tokens con `tipo = access`).


## Endpoints

### Públicos

| Método | Ruta                | Descripción                                     |
|--------|---------------------|-------------------------------------------------|
| POST   | /api/auth/registro  | Registra un usuario (rol USUARIO)               |
| POST   | /api/auth/login     | Login, devuelve accessToken + refreshToken      |
| POST   | /api/auth/refresh   | Renueva el accessToken (con rotación)           |
| GET    | /api/ping           | Health check (`{"message":"pong"}`)             |

### Protegidos (cualquier usuario autenticado)

| Método | Ruta                        | Descripción                                             |
|--------|-----------------------------|---------------------------------------------------------|
| POST   | /api/auth/logout            | Revoca el refreshToken del usuario                      |
| POST   | /api/tickets                | Crea un ticket (el creador es el usuario autenticado)   |
| GET    | /api/tickets/mios           | Tickets creados por el usuario autenticado              |
| GET    | /api/tickets/{id}           | Ticket propio, o cualquiera si es SOPORTE/ADMIN         |
| GET    | /api/tickets/{id}/historial | Historial de cambios de estado (dueño o SOPORTE/ADMIN)  |

### Protegidos por rol

| Método | Ruta                        | Rol requerido  | Descripción                                      |
|--------|-----------------------------|----------------|--------------------------------------------------|
| GET    | /api/tickets                | SOPORTE, ADMIN | Lista todos los tickets (**paginado**, bono)     |
| PATCH  | /api/tickets/{id}/estado    | SOPORTE, ADMIN | Cambia el estado de un ticket                    |
| GET    | /api/tickets/vencidos       | SOPORTE, ADMIN | Tickets que superaron su SLA                     |
| POST   | /api/admin/soporte          | ADMIN          | Asciende un usuario al rol SOPORTE               |
| GET    | /api/admin/estadisticas     | ADMIN          | Tickets por estado y % de cumplimiento de SLA (bono) |

