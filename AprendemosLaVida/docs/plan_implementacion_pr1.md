# Plan de Implementación - Primer PR: Conexión con aprenFun_back

## 1. Visión General del Proyecto
Este plan define la estrategia técnica para conectar la aplicación Android **AprendemosLaVida** con el backend **aprenFun_back**, cumpliendo estrictamente con el contrato de la API OpenAPI 3.1.0 (v0.8.0).

---

## 2. Requisitos de Configuración y Build
- **ApplicationId Obligatorio**: Modificar `app/build.gradle` para establecer `applicationId "com.aprenfun.es"`.
- **Permisos de Red**: Añadir `<uses-permission android:name="android.permission.INTERNET" />` y `<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />` a `AndroidManifest.xml`.
- **Configuración de URLs (BuildConfig)**:
  - `DEBUG`: `http://10.0.2.2:8080/api/v1/`
  - `RELEASE`: `https://api.aprenfun.es/api/v1/`
- **Dependencias a Incorporar**:
  - `Retrofit 2` + `Gson Converter`
  - `OkHttp 3` + `LoggingInterceptor`
  - `androidx.security:security-crypto` (Android Keystore / `EncryptedSharedPreferences`)
  - `Coroutines` (`kotlinx-coroutines-android`)
  - `MockWebServer` + `JUnit 4` + `MockK` (para tests de contrato)

---

## 3. Arquitectura de Red y Autenticación

### 3.1. Identificación de la Instalación (`X-Installation-Id`)
- Se generará un UUID v4 en el primer inicio de la aplicación.
- Se almacenará persistentemente en almacenamiento seguro (`TokenStorage`).
- **Cumplimiento de Privacidad**: Prohibido el uso de AAID, IMEI, dirección MAC u otros identificadores de hardware.

### 3.2. Almacenamiento Seguro de Tokens
- Clase `TokenStorage` usando `EncryptedSharedPreferences` respaldada por `MasterKey` de Android Keystore.
- **Datos Persistidos**: `accessToken`, `accessTokenExpiresAt`, `refreshToken`, `installationId`, `activeChildId`.
- **Regla Estricta**: Las contraseñas de adultos y los `GuardianActionToken` NUNCA se persisten en disco; se manejan únicamente en memoria durante la ejecución de la acción protegida.

### 3.3. Interceptores y Renovación de Token (`AuthInterceptor` & `TokenAuthenticator`)
- **`AuthInterceptor`**:
  - Inyecta el encabezado `Authorization: Bearer <accessToken>` cuando el usuario está autenticado.
  - Inyecta `X-Installation-Id: <UUID>` en todas las peticiones que lo requieran.
  - Inyecta `Accept: application/json`.
- **`TokenAuthenticator`**:
  - Intercepta respuestas HTTP `401 Unauthorized`.
  - Ejecuta de manera sincrónica y atómica (mediante `Mutex` de Coroutines) la renovación del token vía `POST /api/v1/auth/refresh`.
  - Reintenta la petición original con el nuevo `accessToken` exactamente una vez.
  - Si el refresco falla con `401`, revoca la sesión localmente y redirige a la pantalla de inicio de sesión.

### 3.4. Manejo de Errores RFC 7807 (`application/problem+json`)
- Mapeo de errores estructurados en la clase DTO `ProblemJson` (`type`, `title`, `status`, `detail`, `instance`).
- Tratamiento explícito:
  - `401 Unauthorized`: Renovación de token o redirección a Login.
  - `403 Forbidden`: Notificación de acción protegida requerida (solicitar contraseña adulta para obtener `GuardianActionToken`).
  - `409 Conflict`: Manejo de conflictos de edición o duplicados.
  - `429 Too Many Requests`: Mensaje de límite de tasa excedido.
  - `5xx Server Error`: Notificación de error en el servidor.

---

## 4. Repositorios y Servicios API

### 4.1. `AuthRepository`
- `register(email, password, locale, countryCode, legalDocumentVersion, parentalConsent)` -> `POST /api/v1/auth/register`
- `verifyEmail(token)` -> `POST /api/v1/auth/verify-email`
- `login(email, password)` -> `POST /api/v1/auth/login`
- `refresh(refreshToken)` -> `POST /api/v1/auth/refresh`
- `logout(refreshToken)` -> `POST /api/v1/auth/logout`
- `logoutAll()` -> `POST /api/v1/auth/logout-all`
- `getGuardianActionToken(password, purpose)` -> `POST /api/v1/auth/guardian-action-token`

### 4.2. `AccountRepository`
- `getCurrentAccount()` -> `GET /api/v1/me`
- `scheduleAccountDeletion(guardianActionToken)` -> `DELETE /api/v1/me`

### 4.3. `ProfileRepository`
- `listChildren()` -> `GET /api/v1/children`
- `createChild(guardianToken, childInput)` -> `POST /api/v1/children`
- `getChild(childId)` -> `GET /api/v1/children/{childId}`
- `updateChild(childId, guardianToken, childInput)` -> `PATCH /api/v1/children/{childId}`
- `deleteChild(childId, guardianToken)` -> `DELETE /api/v1/children/{childId}`
- `activateChild(childId, guardianTokenOptional, activationInput)` -> `POST /api/v1/children/{childId}/activate`

---

## 5. Módulos de Interfaz de Usuario (UI)
Se mantendrá la línea gráfica actual (`Theme.AprendemosLaVida`, `@drawable/bg_gradient`, `@color/primary`, `@style/Widget.Aprendemos.Button`, `ThemeOverlay.Aprendemos.AlertDialog`):

1. **Pantalla de Inicio de Sesión (`AuthLoginActivity`)**:
   - Formulario de entrada con Email y Contraseña.
   - Opción para ir a Registro o Recuperación de Contraseña.
2. **Pantalla de Registro (`AuthRegisterActivity`)**:
   - Campos: Email, Contraseña (mínimo 12 caracteres), Idioma (`es`/`gl`), Código de País (`ES`), aceptación de términos y consentimiento parental.
3. **Pantalla de Verificación de Email (`AuthVerifyEmailActivity`)**:
   - Introducción de token recibido por correo electrónico para activar la cuenta.
4. **Selector Protegido de Perfiles Infantiles (`ChildSelectorActivity`)**:
   - Listado de perfiles infantiles de la cuenta con sus avatares.
   - Activación de perfil mediante `POST /api/v1/children/{childId}/activate`.
   - Botón de administración de perfiles / zona de adultos protegido por contraseña.
5. **Gestión de Perfiles Infantiles (`ProfileManagementActivity`)**:
   - Pantalla para crear, editar (pseudónimo, edad) o eliminar perfiles infantiles.
   - Diálogo modal para solicitar la contraseña de adulto y generar un `GuardianActionToken` puntual antes de invocar la API.
6. **Reautenticación Adulta / Diálogo Guardian (`GuardianPasswordDialog`)**:
   - Componente reutilizable para solicitar la contraseña adulta al realizar operaciones sensibles (crear/editar/borrar perfiles o borrar cuenta).
7. **Pantalla de Cuenta y Privacidad (`AccountPrivacyActivity`)**:
   - Información de la cuenta actual (`GET /api/v1/me`).
   - Opciones para "Cerrar sesión", "Cerrar sesión en todos los dispositivos" y "Programar borrado de cuenta".
8. **Manejo Universal de Conexión y Errores (`NetworkErrorBanner` / `NetworkUtils`)**:
   - Detección de falta de conectividad y representación clara de errores devueltos en formato `application/problem+json`.

---

## 6. Flujo de Inicio de la Aplicación (`SplashActivity` / Session Restore)
- Al iniciar la app:
  1. Verificar si existen tokens en `TokenStorage`.
  2. Si NO existen -> Redirigir a `AuthLoginActivity`.
  3. Si existen -> Llamar a `GET /api/v1/me` para validar la sesión y obtener `activeChildId`.
  4. Restaurar el `activeChildId` activo.
  5. Redirigir a `MainMenuActivity` (si hay perfil activo) o a `ChildSelectorActivity` (si se requiere seleccionar perfil).
  6. En caso de error 401 invalorable -> Limpiar sesión y solicitar inicio de sesión.

---

## 7. Estrategia de Tests
- **Tests de Contrato API (`ContractTests`)**:
  - Utilizar `MockWebServer` para verificar la correcta serialización y deserialización de JSON según las especificaciones OpenAPI 3.1.0 (`AuthTokens`, `Account`, `Child`, `ProblemJson`).
  - Probar que los encabezados `Authorization: Bearer <token>` y `X-Installation-Id` son enviados correctamente en cada petición.
- **Tests de Renovación Concurrente (`ConcurrentRefreshTest`)**:
  - Simular múltiples peticiones simultáneas que devuelven HTTP `401 Unauthorized`.
  - Verificar que solo se realiza UNA única llamada al endpoint `/api/v1/auth/refresh` y que las peticiones pausadas se reintentan exitosamente con el nuevo token.
  - Verificar el comportamiento cuando el refresh token expira y la sesión debe ser anulada.

---

## 8. Reglas y Restricciones Estrictas
- **No juego offline**: La aplicación requerirá conexión activa para operar.
- **Sin persistencia de secretos adultos**: No se guardarán contraseñas de adultos ni tokens de guardián en disco.
- **Sin cómputo local**: No se calcularán localmente respuestas, puntos, rachas, temporizadores autoritativos ni rankings.
- **Idempotencia rigurosa**: No se reintentarán mutaciones modificando la clave de idempotencia (`Idempotency-Key`).
- **Alcance del Primer PR**: NO se incluirá la importación legacy (`legacy-import`) ni las partidas (`game-sessions`, `story-sessions`).
