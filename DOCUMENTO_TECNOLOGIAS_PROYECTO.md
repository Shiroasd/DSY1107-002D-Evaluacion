# Ficha Técnica y Stack Tecnológico del Proyecto
## Sistema de Gestión de Inventario para Locales Comerciales (Cloud Native)

Este documento recopila la totalidad de las tecnologías, herramientas, lenguajes, frameworks, librerías y servicios en la nube utilizados en el desarrollo y despliegue del proyecto. Ha sido estructurado de forma ejecutiva y visual para facilitar su inclusión en diapositivas y presentaciones formales.

---

## 📊 1. Resumen Ejecutivo del Stack

| Capa / Dominio | Tecnología Principal | Versión | Rol en el Proyecto |
| :--- | :--- | :--- | :--- |
| **Frontend** | Angular | 17.3.0 | SPA reactiva basada en Standalone Components |
| **Lenguaje Frontend** | TypeScript | 5.4.2 | Tipado estático y lógica cliente moderna |
| **Autenticación Frontend** | MSAL Angular / Browser | 3.0.x / 3.22.x | Integración OAuth 2.0 / OIDC con Microsoft Entra ID |
| **Backend** | Spring Boot | 3.3.3 | API RESTful y Resource Server OAuth2 |
| **Lenguaje Backend** | Java (OpenJDK LTS) | 17 | Lógica de negocio y servicios transaccionales |
| **Autenticación Backend** | Spring Cloud Azure AD | 5.23.0 | Validación de tokens JWT y autorización RBAC |
| **Persistencia / ORM** | Spring Data JPA / Hibernate | 6.x | Mapeo objeto-relacional y repositorios de datos |
| **Base de Datos (Local)** | H2 Database | Runtime | Base de datos SQL en memoria para pruebas rápidas |
| **Base de Datos (Cloud)** | PostgreSQL / AWS RDS | 15+ | Motor relacional de producción en la nube |
| **Infraestructura Cloud** | Amazon Web Services (AWS) | Cloud | Cómputo (EC2), BD (RDS PostgreSQL) y Networking |
| **Servidor Web / Proxy** | Nginx | 1.2x | Reverse Proxy, terminación SSL/TLS y hosting SPA |
| **Control de Versiones** | Git & GitHub | 2.x | Gestión de código y flujo de ramas (`main`/`develop`) |

---

## 🎨 2. Frontend (Cliente Web SPA)

### Tecnologías Clave:
* **Angular 17 (Framework SPA):**
  * **Arquitectura Standalone:** Eliminación de `NgModule` redundantes a favor de componentes autónomos modernos y ligeros.
  * **Angular Router:** Navegación por rutas protegidas con Guards de autenticación (`MsalGuard`).
  * **Programación Reactiva (RxJS 7.8):** Manejo asíncrono de flujos de datos mediante `Observable`, `BehaviorSubject` y operadores (`filter`, `map`, `switchMap`).
  * **Formularios Reactivos (`ReactiveFormsModule`):** Manejo estructurado de validaciones y captura de datos para inventario y categorías.

* **Seguridad & Autenticación de Cliente (MSAL v3):**
  * `@azure/msal-browser` (v3.22.0) y `@azure/msal-angular` (v3.0.22).
  * **Flujo OAuth 2.0 Authorization Code con PKCE:** Estándar moderno de máxima seguridad para aplicaciones Single Page sin exposición de secretos.
  * **Intercepción Automática (`MsalInterceptor`):** Inyección automática de tokens Bearer en peticiones hacia `/api/v1`.
  * **Gestión de Sesión:** Almacenamiento seguro en `localStorage` y soporte para cookies de estado.

* **Diseño & Experiencia de Usuario (UI/UX):**
  * **Vanilla CSS Moderno:** Sin dependencias externas pesadas tipo Tailwind o Bootstrap; estilos a la medida con CSS Custom Properties (variables).
  * **Estética Glassmorphism & Modo Oscuro:** Sombras sutiles, efectos de desenfoque (`backdrop-filter`) y paleta de colores curada y profesional.
  * **Diseño 100% Responsivo:** Adaptable a dispositivos móviles, tablets y escritorios mediante CSS Flexbox y Grid.

---

## ⚙️ 3. Backend (Resource Server & API REST)

### Tecnologías Clave:
* **Java 17 LTS:**
  * Aprovechamiento de características modernas del lenguaje: Pattern Matching, Records y mejoras en concurrencia y rendimiento de la JVM.

* **Spring Boot 3.3.3:**
  * `spring-boot-starter-web`: Exposición de endpoints RESTful limpios con serialización JSON de alto rendimiento.
  * `spring-boot-starter-validation`: Validación estricta con Jakarta Bean Validation (`@NotNull`, `@Size`, `@Min`, `@DecimalMin`).
  * **Manejo Global de Excepciones:** Controlador unificado con `@RestControllerAdvice` para respuestas de error estándar en formato RFC 7807 (`ProblemDetail`).

* **Spring Security & OAuth 2.0 Resource Server:**
  * `spring-boot-starter-oauth2-resource-server`.
  * `spring-cloud-azure-starter-active-directory` (5.23.0).
  * **Validación Criptográfica de Tokens:**
    * Verificación asimétrica mediante claves públicas JWKS (`jwk-set-uri`).
    * Validación de emisor (`iss`), expiración de estampas de tiempo (`timestampValidator`) y audiencias autorizadas (`aud` validator).
  * **Control de Acceso Basado en Roles (RBAC):** Restricción de operaciones de creación/edición/eliminación a usuarios con roles administrativos (`ROLE_Admin`), permitiendo lectura al resto de usuarios autenticados.

* **Capa de Persistencia & Acceso a Datos:**
  * **Spring Data JPA & Hibernate 6:** Abstracción completa de consultas con repositorios declarativos.
  * **Estrategia Multi-Entorno:**
    * **Perfil Local:** H2 In-Memory con carga automática de esquemas y datos iniciales (`data.sql`).
    * **Perfil AWS:** Driver de PostgreSQL con pool de conexiones optimizado **HikariCP** hacia AWS RDS.

* **Herramientas de Productividad:**
  * **Project Lombok:** Eliminación de código repetitivo (Getters, Setters, Builders, Constructores).
  * **Apache Maven:** Gestión de dependencias y empaquetado JAR ejecutable portable.

---

## ☁️ 4. Servicios Cloud, Identidad & DevOps

### 1. Microsoft Entra ID (Azure AD):
* **Tenant Institucional:** Directorio federado de identidades (`73d72038-30bf-4ab9-85bc-a402de679470`).
* **App Registrations Separadas (Zero Trust):**
  * **Backend API (`afcd0a4e-f3f3-4934-9861-4d8d13cecc30`):** Expone scopes delegados (`user_impersonation`) y roles de aplicación (`Admin`).
  * **Frontend SPA (`61d2f1d7-99e9-4cf7-83fc-51456a9eb5b6`):** Plataforma Single-page application con URIs de redirección autorizadas en local y en la nube (`https://32.193.45.223/`).

### 2. Amazon Web Services (AWS):
* **AWS EC2 (Elastic Compute Cloud):** Instancia de cómputo en la nube donde se ejecutan los servicios de la aplicación en producción.
* **AWS RDS (Relational Database Service):** Base de datos relacional administrada con PostgreSQL para alta disponibilidad y backups.
* **AWS VPC & Security Groups:** Aislamiento de red, permitiendo acceso público únicamente a los puertos seguros (HTTP/HTTPS) y restringiendo el acceso directo a la base de datos solo desde el backend.

### 3. Servidor Web & Networking:
* **Nginx:**
  * Servidor estático para la distribución del bundle compilado de Angular.
  * Reverse Proxy que enruta llamadas de API (`/api/v1`) al backend Spring Boot.
  * Configuración de CORS y cabeceras de seguridad HTTP.

---

## 🎯 5. Puntos Destacados para la Diapositiva de "Arquitectura y Tecnologías"

Si vas a preparar diapositivas, puedes resumirlo en estos 4 pilares:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       ARQUITECTURA CLOUD NATIVE                             │
└─────────────────────────────────────────────────────────────────────────────┘
       │                                     │                         │
       ▼                                     ▼                         ▼
┌──────────────────┐               ┌──────────────────┐      ┌──────────────────┐
│  FRONTEND (SPA)  │  HTTP/REST    │   BACKEND API    │ SQL  │  BASE DE DATOS   │
│  Angular 17      │ ────────────> │  Spring Boot 3.3 │ ───> │  AWS RDS Postgre │
│  TypeScript 5    │   + Bearer    │  Java 17 LTS     │      │  (H2 para local) │
│  MSAL Angular    │     JWT       │  Spring Security │      └──────────────────┘
└──────────────────┘               └──────────────────┘
       │                                     ▲
       │ Autenticación OAuth2 / OIDC         │ Validación de Token
       ▼                                     │
┌─────────────────────────────────────────────────────────────────────────────┐
│               IDENTIDAD: Microsoft Entra ID (Azure AD)                      │
│     Single Sign-On (SSO) • Roles RBAC (Admin) • Flujo PKCE Seguro           │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Bullet Points sugeridos para tu presentación oral:
1. **Desacoplamiento Total:** Frontend SPA y Backend REST independientes, permitiendo escalar o actualizar cada capa por separado.
2. **Seguridad Empresarial:** Autenticación federada con Azure AD y autorización mediante roles institucionales sin almacenar contraseñas en bases de datos propias.
3. **Estándares de Industria:** OAuth 2.0 + OpenID Connect (OIDC) con validación estricta de tokens JWT en el Resource Server.
4. **Resiliencia y Nube:** Diseñado bajo principios Cloud Native para ejecutarse en AWS (EC2 + RDS PostgreSQL) con soporte para desarrollo local inmediato (H2 + dev servers).
