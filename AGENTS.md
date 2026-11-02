# AGENTS.md

Guía para agentes que trabajan en este repositorio. Describe el stack, los comandos reales, la
arquitectura y las convenciones que ya usa el proyecto. Cuando algo aquí no coincida con el código,
manda el código: corrige esta guía en el mismo cambio.

## Qué es este repositorio

AuthCore es el servicio de autenticación y autorización de Ferretería Ambiente Murano 3D. Registra
usuarios, valida credenciales con BCrypt, asigna roles (`ADMIN`, `USER`) y emite el JWT que valida
el servicio de dominio (`../domain`) con la misma clave compartida.

- Spring Boot 4.1.1 · Java 21 · Gradle 9.7.1 (wrapper)
- MySQL 8.4 · Spring Data JPA · Hibernate (`ddl-auto: update`)
- Spring Security stateless · BCrypt · JJWT 0.12.6
- Puerto 8081 · arquitectura hexagonal

## Demo completa (con `domain`)

La demo integrada (AuthCore + API de dominio + frontend) se levanta desde el repo hermano `domain`,
no desde el compose de este repo. Los dos deben quedar en la misma carpeta: `pi/domain` y
`pi/authcore`. El overlay de desarrollo de `domain` monta `../authcore`.

```bash
cd domain
./scripts/generar-env.sh
docker compose -f docker-compose.yml -f docker-compose.dev.yml build
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d
./scripts/seed.sh      # datos del dominio con las fechas de hoy
# http://localhost:8080  → admin / admin123
```

- `DataSeedRunner` siembra aquí los 10 usuarios solo si `users` está vacía; el seed de `domain` no
  toca usuarios.
- Guía paso a paso: `../domain/docs-tecnicos/puesta-en-marcha-demo.md`
  (<https://github.com/heyleyp14/domain/blob/main/docs-tecnicos/puesta-en-marcha-demo.md>).

## Comandos

```bash
cd authcore
bash gradlew compileJava    # compila
bash gradlew test           # pruebas (levantan el contexto y necesitan MySQL)
bash gradlew build          # JAR + pruebas
bash gradlew bootRun        # arranca en :8081
```

- El wrapper está sin bit de ejecución en el repo: usa `bash gradlew` o `chmod +x gradlew` una vez.
- No hay Gradle instalado en el sistema; usa siempre el wrapper.
- `test` usa `@SpringBootTest`, así que necesita una base MySQL accesible en la URL configurada.
- Docker: `docker compose up -d --build` levanta MySQL 8.4 y este servicio aislados. La demo
  integrada usa el compose de `domain` (ver "Demo completa").

## Variables de entorno

`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET`,
`AUTHCORE_SEED_ENABLED` y `CLOUDINARY_URL`. Los valores por defecto están en
`src/main/resources/application.yml`. Nunca commitees secretos.

`CLOUDINARY_URL` usa el formato `cloudinary://<api_key>:<api_secret>@<cloud_name>`. Alternativa
unsigned: `CLOUDINARY_CLOUD_NAME` + `CLOUDINARY_UPLOAD_PRESET`. Sin ninguna de las dos el servicio
arranca igual y `POST /api/auth/me/image` responde 503.

`JWT_SECRET` debe tener al menos 32 bytes UTF-8 y coincidir exactamente con el del servicio de
dominio; si cambia en un lado, los tokens dejan de validarse en el otro.

## Arquitectura

Paquete base `com.pamplona.authcore`.

- `domain/` — modelo puro (`User`, `Role`). Sin Spring, JPA ni HTTP.
- `application/` — `AuthService` y puertos:
  - `ports/in/` — casos de uso (`RegisterUserUseCase`, `LoginUseCase`, `AssignRoleUseCase`,
    `ListUsersUseCase`).
  - `ports/out/` — contratos (`UserRepositoryPort`, `PasswordHasherPort`, `TokenProviderPort`).
  - `exception/` — excepciones de aplicación.
- `infrastructure/` — adaptadores y configuración:
  - `adapters/in/rest/` — `AuthController`, DTOs y `GlobalExceptionHandler`.
  - `adapters/out/persistence/` — JPA (`UserJpaEntity`, repositorio y adaptador).
  - `adapters/out/security/` — BCrypt y JWT.
  - `adapters/out/storage/` — Cloudinary para las imágenes de usuario.
  - `config/` — `SecurityConfig`, `JwtAuthenticationFilter`, `AdminBootstrapRunner`.

Reglas:

- La dependencia apunta hacia `domain`. `application` no importa nada de `infrastructure`.
- `UserJpaEntity` solo vive en `infrastructure`; la traducción a/desde `User` ocurre en el adaptador.
- `AuthService` implementa los puertos de entrada usando solo los puertos de salida.
- Para agregar un rol basta con añadir una constante a `Role`; no requiere cambios en otras capas.

## Seguridad

- `POST /api/auth/register` y `/api/auth/login` son públicos; el resto exige JWT, y las rutas de
  usuarios exigen rol `ADMIN`.
- Sesiones stateless, CSRF deshabilitado por ser una API con Bearer token.
- Contraseñas con BCrypt; nunca en texto plano ni en logs.
- Las imágenes de usuario se suben desde el backend (`POST /api/auth/me/image`): el navegador
  nunca recibe el API secret de Cloudinary. El campo `imageUrl` es opcional y el front muestra la
  inicial cuando falta.
- El token incluye `sub`, `uid`, `roles` y `exp`. El filtro mapea `roles` a authorities `ROLE_*`.
- `AdminBootstrapRunner` crea `admin` / `admin123` solo si no existe. Cámbialo en entornos
  compartidos.
- `ddl-auto: update` es para desarrollo; en producción conviene controlar el esquema con migraciones.

## Convenciones

- Código e identificadores en inglés (`User`, `AuthService`, `register`, `findByUsername`).
- Comentarios, documentación, mensajes de error y textos de prueba en español.
- DTOs como `record` con validación Jakarta (`@NotBlank`, `@Size`, `@NotNull`).
- Errores de la API como `{"error": "..."}` con su estado HTTP (409, 401, 404, 400, 403).
- Commits: conventional commits (`feat:`, `fix:`, `docs:`...). Sin atribución de IA ni
  `Co-Authored-By`.

## Verificación antes de terminar

1. `bash gradlew compileJava` (o `build`) sin errores.
2. `bash gradlew test` con MySQL disponible, si tocaste el contexto, la persistencia o la seguridad.
3. No declares una verificación que no ejecutaste.

## Documentación

- `README.md` — arranque, configuración, API, token, pruebas y estructura.
- `../domain/docs-tecnicos/puesta-en-marcha-demo.md` — guía única para levantar los dos repos, sembrar
  la demo y ver lo mismo que en la presentación.
- `../domain/docs-tecnicos/api/authcore-openapi.yaml` — contrato OpenAPI de este servicio.
