# AuthCore Service

Servicio de autenticación y autorización de Ferretería Ambiente Murano 3D. Registra usuarios,
valida credenciales con BCrypt y emite el JWT que consume el servicio de dominio. El dominio valida
ese token con la misma clave compartida, sin llamar a AuthCore en cada petición.

- Spring Boot 4.1.1 · Java 21 · Gradle 9.7.1 (wrapper)
- MySQL 8.4 · Spring Data JPA · Hibernate (`ddl-auto: update`)
- Spring Security stateless · BCrypt · JJWT 0.12.6
- Puerto 8081 · arquitectura hexagonal

## Requisitos

- Java 21
- MySQL 8.4 (o Docker para levantar todo)
- Gradle no es necesario: el wrapper viene en el repositorio

## Arranque

Con Docker (MySQL y el servicio):

```bash
cd authcore
docker compose up -d --build
```

En local, con MySQL escuchando en `localhost:3306`:

```bash
cd authcore
bash gradlew bootRun
```

El wrapper está sin bit de ejecución en el repositorio. Usa `bash gradlew`, o ejecuta una sola vez
`chmod +x gradlew` para poder usar `./gradlew`.

La base `authcore_db` debe existir antes de arrancar. Con el `docker compose` de este repo se crea
automáticamente; la URL por defecto apunta a `jdbc:mysql://localhost:3306/authcore_db`.

## Demo completa (con `domain`)

El sistema completo (AuthCore + API de dominio + frontend) se levanta desde el repo hermano `domain`.
Los dos repositorios deben quedar en la misma carpeta: `pi/domain` y `pi/authcore`.

```bash
cd ../domain   # desde la raíz de authcore/
./scripts/generar-env.sh
docker compose -f docker-compose.yml -f docker-compose.dev.yml build
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d
./scripts/seed.sh
# http://localhost:8080 → admin / admin123
```

El `docker compose up -d --build` de **este** repo sirve solo para trabajar en AuthCore aislado
(MySQL y servicio en 8081). La guía paso a paso, con los datos de demo y los problemas comunes, está
en `../domain/docs-tecnicos/puesta-en-marcha-demo.md`.

## Configuración

Variables de entorno (los valores por defecto están en `src/main/resources/application.yml`):

| Variable | Para qué | Valor por defecto |
|---|---|---|
| `SPRING_DATASOURCE_URL` | URL JDBC de MySQL | `jdbc:mysql://localhost:3306/authcore_db?...` |
| `SPRING_DATASOURCE_USERNAME` | Usuario de la base | `root` |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de la base | `1312` |
| `JWT_SECRET` | Clave compartida para firmar y validar el JWT | valor de ejemplo |
| `AUTHCORE_SEED_ENABLED` | Crea el seed de desarrollo solo si la base no tiene usuarios (`true`/`false`) | `true` |
| `CLOUDINARY_URL` | Credencial firmada de Cloudinary (`cloudinary://key:secret@cloud`) para las imágenes de usuario | vacío |
| `CLOUDINARY_CLOUD_NAME` | Alternativa sin secret: nombre de la nube | vacío |
| `CLOUDINARY_UPLOAD_PRESET` | Alternativa sin secret: preset unsigned con permiso de subida | vacío |

`JWT_SECRET` debe ser largo y aleatorio (mínimo 32 bytes UTF-8) y coincidir exactamente con el que
usa el servicio de dominio. No lo commitees ni lo escribas en `application.yml`.

`docker compose` usa su propia base (`authcoredb`) y credenciales (`authcore` / `authcore`); no
comparte los valores por defecto del arranque local.

## API

Base: `/api/auth`. Contrato completo en `domain/docs-tecnicos/api/authcore-openapi.yaml`.

| Método | Ruta | Acceso | Cuerpo | Respuesta |
|---|---|---|---|---|
| POST | `/register` | Público | `{"username","password","imageUrl"?}` | `201 {"id","username","imageUrl"}` |
| POST | `/login` | Público | `{"username","password"}` | `200 {"token"}` |
| POST | `/users/{id}/roles` | ADMIN | `{"role":"ADMIN"\|"USER"}` | `204` |
| GET | `/users` | ADMIN | — | `200 {"total","pagina","tamano","totalPaginas","usuarios":[{"id","username","roles","imageUrl"}]}` |
| GET | `/me` | Autenticado | — | `200 {"id","username","roles","imageUrl"}` |
| POST | `/me/image` | Autenticado | `multipart/form-data: archivo` | `200 {"imageUrl"}` |

`imageUrl` es opcional: si el cliente no sube nada se guarda `null` y el front puede mostrar la
inicial del usuario. Cuando se envía, debe ser una URL `http(s)` de máximo 512 caracteres
(normalmente una URL de Cloudinary).

La foto de perfil se sube con `POST /me/image` (multipart, campo `archivo`): JPG, PNG o WebP de
máximo 2 MB. El backend la publica en Cloudinary y guarda la URL; el navegador nunca recibe el API
secret.

Cloudinary admite dos modos: **firmado** con `CLOUDINARY_URL` (recomendado) o **unsigned** con
`CLOUDINARY_CLOUD_NAME` + `CLOUDINARY_UPLOAD_PRESET`, útil cuando la API key no tiene permiso de
subida. Sin ninguno de los dos el servicio arranca igual y esa ruta responde 503.

Las rutas protegidas esperan `Authorization: Bearer <token>`.

Errores: JSON `{"error": "..."}`.

| Situación | Estado |
|---|---|
| Usuario ya registrado | 409 |
| Credenciales inválidas | 401 |
| Usuario no encontrado | 404 |
| Validación de campos | 400 |
| Sin token, token inválido o rol insuficiente | 403 |

`POST /users/{id}/roles` solo agrega el rol; no existe operación para quitarlo.

## Token JWT

El token se firma con HMAC usando los bytes UTF-8 de `JWT_SECRET`. Con un secreto de 48 a 63 bytes el
algoritmo es HS384. Claims:

- `sub`: nombre de usuario
- `uid`: id del usuario
- `roles`: lista de roles (`ADMIN`, `USER`)
- `iat` y `exp`: emisión y expiración (1 hora por defecto, `authcore.jwt.expiration-ms`)

El servicio de dominio debe validar la firma con la misma clave y mapear cada rol a una authority
`ROLE_<rol>`.

## Usuario admin inicial y seed de desarrollo

`DataSeedRunner` corre antes de `AdminBootstrapRunner` y, **solo si no existe ningún usuario**,
crea 10 cuentas de ejemplo (incluido `admin`) con contraseña hasheada con BCrypt y una URL de
Cloudinary como imagen. Si la tabla ya tiene usuarios, no hace nada.

| Usuario | Contraseña | Roles |
|---|---|---|
| `admin` | `admin123` | `ADMIN`, `USER` |
| `maria.gomez`, `juan.perez`, `ana.rodriguez`, `carlos.mendoza`, `lucia.fernandez`, `diego.ramirez`, `sofia.torres`, `miguel.vargas`, `valentina.rojas` | `Demo1234` | `USER` |

Como el seed ya incluye `admin`, `AdminBootstrapRunner` lo detecta y no lo duplica. Para desactivar
el seed en cualquier entorno usa `AUTHCORE_SEED_ENABLED=false`; `AdminBootstrapRunner` seguirá
creando el admin si falta. Cambia las contraseñas por defecto en cuanto el servicio quede expuesto
o compartido.

## Pruebas

```bash
cd authcore
bash gradlew test
```

La prueba actual (`AuthcoreServiceApplicationTests`) es un `@SpringBootTest`: levanta el contexto
completo y necesita MySQL accesible en la URL configurada.

## Estructura

```
src/main/java/com/pamplona/authcore/
├── domain/                 # User, Role: sin Spring ni JPA
├── application/
│   ├── ports/in|out/       # casos de uso (entrada) y contratos (salida)
│   ├── service/            # AuthService: implementa los casos de uso
│   └── exception/          # excepciones de aplicación
└── infrastructure/
    ├── adapters/in/rest/   # AuthController, DTOs y manejo global de errores
    ├── adapters/out/       # JPA (usuarios) y seguridad (BCrypt, JWT)
    └── config/             # SecurityConfig, filtro JWT, seed y bootstrap del admin
```

## Docker

`docker compose up -d --build` levanta:

- `authcore-db`: MySQL 8.4, base `authcoredb`, volumen persistente
- `authcore-service`: este servicio, publicado en el puerto 8081

## Diferencias conocidas

Documentadas en la especificación OpenAPI (decisión D2):

- La contraseña exige mínimo 6 caracteres (el backlog pide 8, con letras y números).
- El nombre de usuario no se valida como email ni con otro formato.
- Las rutas protegidas responden 403, no 401, cuando falta el token o es inválido.
- El usuario `admin` se crea con una contraseña fija definida en `AdminBootstrapRunner`.
