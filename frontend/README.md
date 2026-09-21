# 📦 CloudStock Pro — Frontend (SPA)

Aplicación Web de Página Única (Single Page Application - SPA) desarrollada con **Angular 17+** y arquitectura de componentes *Standalone*, diseñada para la administración y control de inventario en locales comerciales y terminales de punto de venta (POS).

La aplicación implementa seguridad de nivel empresarial mediante autenticación y autorización basadas en **Microsoft Entra ID (Azure AD)** con flujo OAuth 2.0 / OpenID Connect (PKCE) y Control de Acceso Basado en Roles (RBAC).

---

## 🚀 Tecnologías y Herramientas Utilizadas

### 1. Framework y Núcleo de la Aplicación
* **[Angular 17.3+](https://angular.dev/)**: Framework moderno para desarrollo web frontend con soporte nativo de componentes *Standalone* (`standalone: true`), inyección de dependencias (`inject()`) y nueva API de configuración modular (`provideRouter`, `provideHttpClient`).
* **[TypeScript 5.4+](https://www.typescriptlang.org/)**: Lenguaje base con tipado estático estricto, interfaces fuertemente tipadas y soporte para las últimas características de ECMAScript.
* **[RxJS 7.8+](https://rxjs.dev/)**: Biblioteca de programación reactiva para el manejo asíncrono de eventos, flujos HTTP, estados reactivos (`BehaviorSubject`, `Observable`) y operadores de transformación (`tap`, `catchError`, `concatMap`, `map`).
* **[Zone.js 0.14+](https://github.com/angular/angular/tree/main/packages/zone.js)**: Manejo del contexto de ejecución y detección de cambios automática de Angular.
* **Angular Animations (`@angular/animations`)**: Motor de animaciones para transiciones fluidas de modales, alertas y elementos de la interfaz.

---

### 2. Autenticación, Seguridad y Autorización (IAM)
* **[Microsoft Entra ID](https://learn.microsoft.com/es-es/entra/identity/) (Azure Active Directory)**: Proveedor de identidad en la nube (IDP) que gestiona identidades, tokens JWT (ID Token y Access Token) y roles de usuario.
* **[@azure/msal-browser 3.22+](https://github.com/AzureAD/microsoft-authentication-library-for-js/tree/dev/lib/msal-browser)**: Biblioteca de autenticación de Microsoft para navegadores, implementando el flujo OAuth 2.0 Authorization Code con **PKCE** (Proof Key for Code Exchange) y almacenamiento seguro de sesiones (`localStorage` / cookies).
* **[@azure/msal-angular 3.0+](https://github.com/AzureAD/microsoft-authentication-library-for-js/tree/dev/lib/msal-angular)**: Integración oficial de MSAL con el ciclo de vida de Angular:
  * `MsalGuard`: Protección de rutas para impedir acceso a usuarios no autenticados.
  * `MsalService` & `MsalBroadcastService`: Gestión de eventos de login/logout y sincronización del estado de cuenta activa.
* **Interceptor HTTP Personalizado (`TokenHeaderInterceptor`)**: Intercepta automáticamente todas las llamadas dirigidas a los endpoints de la API (`/api/v1/*`) e inyecta el encabezado `Authorization: Bearer <token>` necesario para el Resource Server.
* **Control de Acceso Basado en Roles (RBAC)**: Decodificación de claims del token JWT para verificar roles asignados (`Admin`, `User`/`Vendedor`) y adaptar dinámicamente la interfaz (habilitar o bloquear botones de creación, edición y eliminación de stock).

---

### 3. Diseño, Estilos y Experiencia de Usuario (UI / UX)
* **CSS3 Nativo / Vanilla CSS**:
  * Sistema de diseño basado en **Variables CSS (Custom Properties)** para coherencia visual, paleta de colores corporativa y tipografía estandarizada.
  * Diseño **Totalmente Responsivo** (Mobile-First / Desktop) mediante CSS Flexbox y CSS Grid.
  * Efectos modernos de **Glassmorphism**, desenfoques de fondo (`backdrop-filter`), sombras suaves y micro-interacciones hover.
* **Google Fonts**:
  * **[Inter](https://fonts.google.com/specimen/Inter)**: Tipografía principal para una lectura limpia y profesional.
  * **[JetBrains Mono](https://fonts.google.com/specimen/JetBrains+Mono)**: Tipografía monoespaciada para códigos SKU, precios y números de serie.
* **Iconografía SVG Optimizada**: Iconos SVG vectoriales inline de alto rendimiento, escalables y sin dependencias pesadas de terceros.
* **Internacionalización y Localización (`es-CL`)**: Configuración regional chilena (`@angular/common/locales/es-CL`) para formateo automático de moneda en Pesos Chilenos (`$ CLP`) y fechas locales.

---

### 4. Herramientas de Construcción y Entorno de Desarrollo
* **[Angular CLI 17.3+](https://angular.dev/tools/cli)**: Interfaz de línea de comandos para creación, desarrollo, pruebas y despliegue del proyecto.
* **[@angular-devkit/build-angular (Vite / esbuild)](https://esbuild.github.io/)**: Compilador y empaquetador ultrarrápido para optimización de bundles en producción y Hot Module Replacement (HMR) en desarrollo.
* **[Node.js](https://nodejs.org/) & [npm](https://www.npmjs.com/)**: Entorno de ejecución y gestor de paquetes de dependencias.

---

## 📁 Estructura del Código Fuente

```text
frontend/
├── angular.json                     # Configuración del workspace de Angular CLI
├── package.json                     # Dependencias y scripts del proyecto
├── tsconfig.json                    # Configuración global de TypeScript
├── tsconfig.app.json                # Configuración de compilación para la aplicación
└── src/
    ├── index.html                   # HTML base con fuentes de Google y metadatos
    ├── main.ts                      # Punto de entrada de la aplicación (bootstrapApplication)
    ├── styles.css                   # Estilos globales y variables de diseño CSS
    ├── assets/                      # Iconos, imágenes y recursos estáticos
    │   └── favicon.svg
    ├── environments/                # Variables de entorno y configuración de Azure AD / API
    │   ├── environment.ts           # Configuración de producción
    │   └── environment.development.ts # Configuración de desarrollo local
    └── app/
        ├── app.component.ts|html|css # Componente raíz y contenedor principal (Shell)
        ├── app.config.ts            # Proveedores globales, inicializador MSAL y configuración HTTP
        ├── app.routes.ts            # Definición de rutas y aplicación de Guards
        ├── components/              # Componentes de la interfaz de usuario (Standalone)
        │   ├── alert/               # Notificaciones y alertas contextuales (Éxito, Error 403, etc.)
        │   ├── categories/          # Gestión y consulta de categorías de productos
        │   ├── inventory/           # Panel principal de inventario (Tabla, filtros, estadísticas)
        │   ├── landing/             # Página de bienvenida e inicio de sesión
        │   ├── navbar/              # Barra de navegación con perfil de usuario y control de sesión
        │   └── product-modal/       # Modal para registro y edición de productos
        ├── guards/                  # Guardianes de seguridad para rutas
        │   └── auth.guard.ts        # Guard personalizado para validar autenticación
        ├── models/                  # Interfaces y modelos de datos tipados
        │   ├── category.model.ts    # Modelo de Categoría
        │   ├── product.model.ts     # Modelo de Producto
        │   └── user-profile.model.ts # Modelo del perfil de usuario autenticado
        └── services/                # Servicios para lógica de negocio y comunicación HTTP
            ├── auth.service.ts      # Manejo de tokens, roles, sesión y estado del usuario
            └── inventory.service.ts # Consumo de endpoints REST (/api/v1/products, /api/v1/categories)
```

---

## ⚙️ Configuración y Variables de Entorno

La configuración de conexión con **Microsoft Entra ID** y el **Backend API** se encuentra en `src/environments/environment.ts`:

```typescript
export const environment = {
  production: false,
  msalConfig: {
    auth: {
      clientId: '61d2f1d7-99e9-4cf7-83fc-51456a9eb5b6',       // ID de la aplicación en Azure AD
      authority: 'https://login.microsoftonline.com/<TENANT_ID>', // Tenant de Microsoft Entra
      redirectUri: 'http://localhost:4200'                      // URL de redirección post-login
    }
  },
  apiConfig: {
    baseUrl: 'http://localhost:8080/api/v1',                    // Endpoint base del Resource Server
    scope: 'api://<BACKEND_APP_ID>/user_impersonation'          // Scope de permisos delegado
  }
};
```

---

## 🛠️ Scripts Disponibles

En la raíz de la carpeta `frontend/` se pueden ejecutar los siguientes comandos:

| Comando | Descripción |
|---|---|
| `npm install` | Descarga e instala todas las dependencias listadas en `package.json`. |
| `npm start` | Inicia el servidor de desarrollo en `http://localhost:4200/` con recarga automática. |
| `npm run build` | Compila la aplicación optimizada para producción en el directorio `dist/`. |
| `npm run watch` | Compila la aplicación en modo desarrollo y observa cambios en tiempo real. |
| `npm test` | Ejecuta las pruebas unitarias mediante Karma / Jasmine. |

---

## 🔒 Flujo de Autenticación en el Frontend

1. **Ingreso al Sistema**: El usuario no autenticado visualiza el componente `landing` y solicita inicio de sesión mediante el botón *Iniciar Sesión con Microsoft*.
2. **Redirección MSAL**: Se inicia el flujo interactivo de Microsoft Entra ID con protocolo PKCE.
3. **Recepción de Tokens**: Al completar la autenticación, MSAL intercepta la redirección y almacena el `id_token` y `access_token`.
4. **Navegación Protegida**: `auth.guard.ts` autoriza el acceso a `/inventory` y `/categories`.
5. **Consumo de la API**: `TokenHeaderInterceptor` añade el encabezado `Authorization: Bearer <token>` a cada solicitud hacia el backend en Spring Boot.
6. **Validación de Roles (RBAC)**: Si el usuario tiene el rol `Admin`, se habilitan las acciones para crear, editar o eliminar productos; en caso contrario, se opera en modo de solo lectura para el personal de ventas/operadores.
