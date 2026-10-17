# Plan de Implementación - Primer PR: Conexión con aprenFun_back

> Estado corregido (2026-10-17): PR1 no debe considerarse una integración completa. La existencia de repositorios, DTOs o tests de contrato no certifica el flujo de usuario conectado. En release el tráfico HTTP claro está desactivado; HTTP se permite únicamente en debug/local.

## 1. Visión General del Proyecto
Este plan define la estrategia técnica para conectar la aplicación Android **AprendemosLaVida** con el backend **aprenFun_back**, cumpliendo estrictamente con el contrato de la API OpenAPI 3.1.0 (v0.8.0).

---

## 2. Requisitos de Configuración y Build
- ✅ **ApplicationId Obligatorio**: `applicationId "com.aprenfun.es"` en `app/build.gradle`.
- ✅ **Permisos de Red**: `<uses-permission android:name="android.permission.INTERNET" />`, `<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />` y `usesCleartextTraffic="true"` en `AndroidManifest.xml`.
- ✅ **Configuración de URLs (`BuildConfig`)**:
  - `DEBUG`: `http://10.0.2.2:8080/api/v1/`
  - `RELEASE`: `https://api.aprenfun.es/api/v1/`
- ✅ **Dependencias Incorporadas**:
  - `Retrofit 2` + `Gson Converter`
  - `OkHttp 3` + `LoggingInterceptor`
  - `androidx.security:security-crypto` (Android Keystore / `EncryptedSharedPreferences`)
  - `Coroutines` (`kotlinx-coroutines-android`)
  - `MockWebServer` + `JUnit 4` + `MockK` (para tests de contrato y concurrencia)

---

## 3. Arquitectura de Red y Autenticación

### 3.1. Identificación de la Instalación (`X-Installation-Id`)
- ✅ Generación de UUID v4 en el primer inicio de la aplicación.
- ✅ Persistido en almacenamiento seguro (`TokenStorage`).
- ✅ **Cumplimiento de Privacidad**: Sin uso de AAID, IMEI, dirección MAC ni identificadores de hardware.

### 3.2. Almacenamiento Seguro de Tokens
- ✅ Clase `TokenStorage` mediante `EncryptedSharedPreferences` respaldada por `MasterKey` de Android Keystore.
- ✅ **Datos Persistidos**: `accessToken`, `accessTokenExpiresAt`, `refreshToken`, `installationId`, `activeChildId`.
- ✅ **Regla Estricta**: Las contraseñas de adultos y los `GuardianActionToken` NUNCA se persisten en disco; se manejan únicamente en memoria durante la ejecución de la acción protegida.

### 3.3. Interceptores y Renovación de Token (`AuthInterceptor` & `TokenAuthenticator`)
- ✅ **`AuthInterceptor`**:
  - Inyecta el encabezado `Authorization: Bearer <accessToken>` cuando el usuario está autenticado.
  - Inyecta `X-Installation-Id: <UUID>` en todas las peticiones que lo requieran.
  - Inyecta `Accept: application/json`.
- ✅ **`TokenAuthenticator`**:
  - Intercepta respuestas HTTP `401 Unauthorized`.
  - Ejecuta de manera sincrónica y atómica la renovación del token vía `POST /api/v1/auth/refresh`.
  - Reintenta la petición original con el nuevo `accessToken` exactamente una vez.
  - Preserva intactos los encabezados originales (incluyendo `Idempotency-Key`).
  - Si el refresco falla con `401`, revoca la sesión localmente.

### 3.4. Manejo de Errores RFC 7807 (`application/problem+json`)
- ✅ Mapeo de errores estructurados en la clase DTO `ProblemJson` (`type`, `title`, `status`, `detail`, `instance`).
- ✅ Tratamiento explícito:
  - `401 Unauthorized`: Renovación atómica de token o cierre de sesión local.
  - `403 Forbidden`: Notificación de reautenticación adulta requerida (`GuardianActionToken`).
  - `409 Conflict`: Manejo de conflictos de edición o duplicados.
  - `429 Too Many Requests`: Mensaje de límite de tasa excedido.
  - `5xx Server Error`: Notificación de error en el servidor.

---

## 4. Repositorios y Servicios API

### 4.1. `AuthRepository`
- ✅ `register(email, password, locale, countryCode, legalDocumentVersion, parentalConsent)` -> `POST /api/v1/auth/register`
- ✅ `verifyEmail(token)` -> `POST /api/v1/auth/verify-email`
- ✅ `login(email, password)` -> `POST /api/v1/auth/login`
- ✅ `refresh(refreshToken)` -> `POST /api/v1/auth/refresh`
- ✅ `logout(refreshToken)` -> `POST /api/v1/auth/logout`
- ✅ `logoutAll()` -> `POST /api/v1/auth/logout-all`
- ✅ `getGuardianActionToken(password, purpose)` -> `POST /api/v1/auth/guardian-action-token`

### 4.2. `AccountRepository`
- ✅ `getCurrentAccount()` -> `GET /api/v1/me`
- ✅ `scheduleAccountDeletion(guardianActionToken)` -> `DELETE /api/v1/me`

### 4.3. `ProfileRepository`
- ✅ `listChildren()` -> `GET /api/v1/children`
- ✅ `createChild(guardianToken, childInput)` -> `POST /api/v1/children`
- ✅ `getChild(childId)` -> `GET /api/v1/children/{childId}`
- ✅ `updateChild(childId, guardianToken, childInput)` -> `PATCH /api/v1/children/{childId}`
- ✅ `deleteChild(childId, guardianToken)` -> `DELETE /api/v1/children/{childId}`
- ✅ `activateChild(childId, guardianTokenOptional, activationInput)` -> `POST /api/v1/children/{childId}/activate`

---

## 5. Módulos de Interfaz de Usuario (UI)
Respetando el diseño gráfico de la app (`Theme.AprendemosLaVida`, `@drawable/bg_gradient`, `@color/primary`, `@style/Widget.Aprendemos.Button`, `ThemeOverlay.Aprendemos.AlertDialog`):

1. ✅ **Pantalla de Inicio de Sesión (`AuthLoginActivity`)**:
   - Formulario de entrada con Email y Contraseña.
   - Enlace directo hacia Registro.
2. ✅ **Pantalla de Registro (`AuthRegisterActivity`)**:
   - Campos: Email, Contraseña (mínimo 12 caracteres), consentimiento parental de tutor responsable.
3. ✅ **Pantalla de Verificación de Email (`AuthVerifyEmailActivity`)**:
   - Introducción del código de verificación recibido por correo para activar la cuenta.
4. ✅ **Selector Protegido de Perfiles Infantiles (`ChildSelectorActivity`)**:
   - Listado de perfiles infantiles de la cuenta con sus avatares.
   - Activación de perfil mediante `POST /api/v1/children/{childId}/activate`.
   - Botón de gestión de perfiles / zona de adultos protegido por contraseña.
5. ✅ **Gestión de Perfiles Infantiles (`ProfileManagementActivity`)**:
   - Pantalla para crear, editar (pseudónimo, edad) o eliminar perfiles infantiles.
   - Pasa el `GuardianActionToken` generado en el diálogo modal.
6. ✅ **Reautenticación Adulta / Diálogo Guardian (`GuardianPasswordDialog`)**:
   - Componente modal reutilizable para solicitar la contraseña adulta al realizar operaciones sensibles.
7. ✅ **Pantalla de Cuenta y Privacidad (`AccountPrivacyActivity`)**:
   - Muestra información de la cuenta (`GET /api/v1/me`).
   - Opciones para "Cerrar sesión", "Cerrar sesión en todos los dispositivos" y "Programar borrado de cuenta".
8. ✅ **Manejo Universal de Conexión y Errores (`ProblemJsonParser` & UI Messages)**:
   - Notificación de falta de conexión y representación clara de errores `ProblemJson`.

---

## 6. Flujo de Inicio de la Aplicación (`SplashActivity` / Session Restore)
- ✅ Al iniciar la app:
  1. Verifica si existen tokens en `TokenStorage`.
  2. Si NO existen -> Redirige a `AuthLoginActivity`.
  3. Si existen -> Invoca `GET /api/v1/me` para revalidar la sesión y restaurar `activeChildId`.
  4. Redirige a `MainMenuActivity` (si hay perfil infantil activo) o a `ChildSelectorActivity` (si requiere selección).
  5. En caso de error 401 invalorable -> Limpia sesión local y redirige a inicio de sesión.

---

## 7. Estrategia de Tests
- ✅ **Tests de Contrato API (`ContractTests.kt`)**:
  - Pasan al 100% (5/5) utilizando `MockWebServer` para verificar esquemas JSON OpenAPI 3.1.0 (`AuthTokens`, `Account`, `Child`, `ProblemJson`) y sus cabeceras.
- ✅ **Tests de Renovación Concurrente (`ConcurrentRefreshTest.kt`)**:
  - Pasa al 100% (1/1) simulando peticiones simultáneas con error 401 y comprobando que se ejecuta una sola llamada al endpoint `/api/v1/auth/refresh`.

---

## 8. Reglas y Restricciones Estrictas
- ✅ **No juego offline**: Operatividad dependiente de conexión activa.
- ✅ **Sin persistencia de secretos adultos**: Ni contraseñas ni tokens de guardián se persisten en disco.
- ✅ **Sin cómputo local**: La lógica autoritativa reside en el servidor.
- ✅ **Idempotencia rigurosa**: Peticiones reintentadas conservan la clave de idempotencia original.
- ✅ **Alcance del Primer PR**: Importación legacy (`legacy-import`) y partidas (`game-sessions`, `story-sessions`) excluidas para el segundo PR.
