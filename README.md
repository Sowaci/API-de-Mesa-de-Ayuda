# API de Mesa de Ayuda (Helpdesk) con SLA — Spring Boot + JWT

API REST para gestionar tickets de soporte técnico con **SLA calculado por prioridad**,
**autenticación JWT (access + refresh token)** y **autorización por roles (RBAC)**.

## Stack tecnológico

| Componente       | Tecnología                              |
|------------------|-----------------------------------------|
| Lenguaje         | Java 17+ (probado con JDK 21)           |
| Framework        | Spring Boot 3.4.x                       |
| Seguridad        | Spring Security 6 + JWT (jjwt 0.12.6)   |
| Persistencia     | Spring Data JPA                         |
| Base de datos    | H2 en memoria                           |
| Otros            | Lombok, Bean Validation, Maven          |

## Requisitos

- JDK 17 o superior
- Maven 3.8+

## Ejecución

```bash
mvn spring-boot:run
# o bien
mvn clean package -DskipTests
java -jar target/helpdesk-api-0.0.1-SNAPSHOT.jar
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

## Flujo de autenticación

```
1. POST /api/auth/login  →  { accessToken, refreshToken }
2. Peticiones protegidas →  Authorization: Bearer <accessToken>
3. accessToken expira (401) → POST /api/auth/refresh { refreshToken } → nuevos tokens
4. POST /api/auth/logout → revoca el refresh token
```

## Endpoints

### Públicos

| Método | Ruta                | Descripción                                     |
|--------|---------------------|-------------------------------------------------|
| POST   | /api/auth/registro  | Registra un usuario (rol USUARIO)               |
| POST   | /api/auth/login     | Login, devuelve accessToken + refreshToken      |
| POST   | /api/auth/refresh   | Renueva el accessToken (con rotación)           |
| GET    | /api/ping           | Health check (`{"message":"pong"}`)             |

### Protegidos (cualquier usuario autenticado)

| Método | Ruta                      | Descripción                                             |
|--------|---------------------------|---------------------------------------------------------|
| POST   | /api/auth/logout          | Revoca el refreshToken del usuario                      |
| POST   | /api/tickets              | Crea un ticket (el creador es el usuario autenticado)   |
| GET    | /api/tickets/mios         | Tickets creados por el usuario autenticado              |
| GET    | /api/tickets/{id}         | Ticket propio, o cualquiera si es SOPORTE/ADMIN         |

### Protegidos por rol

| Método | Ruta                        | Rol requerido  | Descripción                                      |
|--------|-----------------------------|----------------|--------------------------------------------------|
| GET    | /api/tickets                | SOPORTE, ADMIN | Lista todos los tickets (**paginado**, bono)     |
| PATCH  | /api/tickets/{id}/estado    | SOPORTE, ADMIN | Cambia el estado de un ticket                    |
| GET    | /api/tickets/vencidos       | SOPORTE, ADMIN | Tickets que superaron su SLA                     |
| POST   | /api/admin/soporte          | ADMIN          | Asciende un usuario al rol SOPORTE               |
| GET    | /api/admin/estadisticas     | ADMIN          | Tickets por estado y % de cumplimiento de SLA (bono) |

## Regla de negocio: SLA

Al crear un ticket el servidor calcula `slaVenceEn = creadoEn + horas según prioridad`.
El cliente **nunca envía** `slaVenceEn` ni `estado` (el estado inicial siempre es `ABIERTO`).

| Prioridad | SLA   |
|-----------|-------|
| ALTA      | 4 h   |
| MEDIA     | 24 h  |
| BAJA      | 72 h  |

Las horas son configurables en `application.yml` (`helpdesk.sla.*`).

Un ticket está **vencido** cuando `ahora > slaVenceEn` y su estado no es `RESUELTO`.
Cada respuesta de ticket incluye el campo `vencido` y el endpoint `GET /api/tickets/vencidos`
lista los vencidos.

## Códigos de respuesta

| Situación                                       | Código |
|-------------------------------------------------|--------|
| Creación exitosa                                | 201    |
| Consulta exitosa                                | 200    |
| Datos inválidos (validaciones, enum incorrecto) | 400    |
| Sin token / token inválido / refresh inválido   | 401    |
| Rol insuficiente                                | 403    |
| Recurso no encontrado                           | 404    |
| Email ya registrado                             | 409    |

## Validaciones

- `email`: formato válido y único (duplicados → 409)
- `password`: mínimo 6 caracteres
- `titulo` y `descripcion`: obligatorios (no vacíos)
- `prioridad` y `estado`: solo aceptan los valores del enum (→ 400)

## Evidencia de funcionamiento

Se ejecutó una suite end-to-end (PowerShell) contra la API con **34/34 casos
aprobados**, cubriendo: registro/login, refresh con rotación, logout que revoca
(un `/refresh` posterior responde 401), 401 sin token, 403 por rol, creación de
tickets con SLA correcto (+4h ALTA, +72h BAJA), listado de vencidos por SOPORTE,
paginación y estadísticas ADMIN.

## Postman

Importar el archivo `Helpdesk.postman_collection.json` (colección 2.1). Incluye
todos los endpoints con ejemplos de cuerpo y variables para los tokens
(`access_token`, `refresh_token`, `ticket_id`).

## Estructura del proyecto

```
src/main/java/com/helpdesk/api
├── config/        SecurityConfig, DataSeeder
├── controller/    AuthController, TicketController, AdminController, PingController
├── dto/           Requests y Responses (records + validación)
├── entity/        Usuario, Ticket, RefreshToken
├── enums/         Rol, Prioridad, Estado
├── exception/     Excepciones de negocio y GlobalExceptionHandler
├── repository/    Spring Data JPA
├── security/      JwtService, JwtAuthenticationFilter, handlers 401/403
└── service/       AuthService, TicketService, RefreshTokenService, SlaService
```

## Retos opcionales implementados (bono)

- **Paginación** en `GET /api/tickets` (`?page=0&size=10&sort=creadoEn,desc`)
- **Estadísticas** en `GET /api/admin/estadisticas`: tickets por estado, % de
  cumplimiento de SLA (resueltos dentro del SLA / resueltos) y tickets vencidos.
