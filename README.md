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

Se eligió esta forma porque permite que cuando un usuario cierre sesión el token quede desactivado y no se pueda volver a usar. Además, si alguien intenta reutilizar un token que ya fue cambiado o revocado, el sistema lo detecta y responde con un error (401). También ayuda a entender mejor cómo funciona el ciclo de vida de un token, desde que se crea hasta que se renueva o se invalida. Por último, los tokens se guardan con un hash SHA-256 en lugar de almacenarse directamente, lo que hace que la información sea más segura en caso de que la base de datos llegue a verse comprometida.

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

