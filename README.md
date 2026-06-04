# 🏢 Provisiones Santa Martha — API REST

> API backend para la gestión de contratos funerarios, pagos, personas y cobros programados de **Provisiones Santa Martha**.

---

## 📋 Tabla de Contenidos

- [Descripción](#descripción)
- [Arquitectura](#arquitectura)
- [Patrones de Diseño](#patrones-de-diseño)
- [Tecnologías y Dependencias](#tecnologías-y-dependencias)
- [Base de Datos](#base-de-datos)
- [Endpoints Principales](#endpoints-principales)
- [Variables de Entorno](#variables-de-entorno)
- [Instalación y Ejecución](#instalación-y-ejecución)
- [Estructura del Proyecto](#estructura-del-proyecto)

---

## 📖 Descripción

Sistema de gestión para una empresa de servicios funerarios. Permite administrar:

- 👤 **Personas y clientes**
- 📄 **Contratos funerarios** con planes y beneficiarios
- 💰 **Pagos y cobros programados**
- 📦 **Catálogo de artículos y planes funerarios**
- 🔔 **Notificaciones por correo**
- 🔐 **Autenticación segura con JWT**
- 📝 **Auditoría de registros**

---

## 🏗️ Arquitectura

El proyecto implementa una **arquitectura en capas (Layered Architecture)** sobre una **API REST**, siguiendo el patrón MVC adaptado para servicios web:

```
┌─────────────────────────────────────────┐
│           Cliente HTTP / Frontend        │
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│        Security Layer (JWT Filter)       │
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│         Controller Layer (@RestController)│
│  AuthController, ContratoController,     │
│  PagoController, PersonaController...    │
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│           Service Layer (@Service)        │
│     Lógica de negocio y validaciones     │
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│        Repository Layer (JPA)            │
│     Spring Data JPA Repositories        │
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│           MySQL Database                 │
│            shsm_db                       │
└─────────────────────────────────────────┘
```

---

## 🎨 Patrones de Diseño

| Patrón | Implementación |
|--------|---------------|
| **DTO (Data Transfer Object)** | Clases en `dto/` para desacoplar entidades de la API |
| **Repository Pattern** | Interfaces en `repository/` con Spring Data JPA |
| **Template Method** | `BaseEntity` como superclase abstracta con campos comunes |
| **Chain of Responsibility** | Filtros de seguridad con `JwtAuthenticationFilter` |
| **Singleton** | Beans gestionados por el contenedor de Spring |
| **Global Exception Handler** | `@RestControllerAdvice` centraliza el manejo de errores |

---

## 🛠️ Tecnologías y Dependencias

### Core
| Dependencia | Versión | Descripción |
|------------|---------|-------------|
| **Java** | 21 | Lenguaje principal |
| **Spring Boot** | 3.4.4 | Framework principal |
| **Spring Boot Starter Web** | 3.4.4 | API REST (MVC) |
| **Spring Boot Starter Data JPA** | 3.4.4 | ORM con Hibernate |
| **Spring Boot Starter Security** | 3.4.4 | Autenticación y autorización |
| **Spring Boot Starter Validation** | 3.4.4 | Validación de DTOs con Bean Validation |
| **Spring Boot Starter Mail** | 3.4.4 | Envío de correos SMTP |

### Seguridad
| Dependencia | Versión | Descripción |
|------------|---------|-------------|
| **jjwt-api** | 0.12.6 | JSON Web Tokens |
| **jjwt-impl** | 0.12.6 | Implementación JWT |
| **jjwt-jackson** | 0.12.6 | Serialización JWT con Jackson |

### Base de Datos
| Dependencia | Versión | Descripción |
|------------|---------|-------------|
| **mysql-connector-j** | Runtime | Driver JDBC para MySQL |

### Utilidades
| Dependencia | Versión | Descripción |
|------------|---------|-------------|
| **Lombok** | Managed | Reducción de boilerplate (getters, setters, builders) |

### Testing
| Dependencia | Versión | Descripción |
|------------|---------|-------------|
| **spring-boot-starter-test** | 3.4.4 | JUnit 5, Mockito |
| **spring-security-test** | Managed | Tests de seguridad |

### Build
| Herramienta | Versión |
|------------|---------|
| **Maven** | 3.x |
| **spring-boot-maven-plugin** | 3.4.4 |

---

## 🗄️ Base de Datos

- **Motor:** MySQL
- **Base de datos:** `shsm_db`
- **Host por defecto:** `localhost:3306`
- **DDL Auto:** `update` (Hibernate actualiza el esquema automáticamente)
- **Dialecto:** `org.hibernate.dialect.MySQLDialect`

### Entidades principales

| Entidad | Tabla | Descripción |
|---------|-------|-------------|
| `Persona` | `personas` | Clientes y titulares de contratos |
| `Contrato` | `contratos` | Contratos funerarios |
| `PlanFunerario` | `planes_funerarios` | Planes disponibles |
| `Beneficiario` | `beneficiarios` | Beneficiarios por contrato |
| `Pago` | `pagos` | Registro de pagos |
| `CobroProgramado` | `cobros_programados` | Cobros futuros programados |
| `Empleado` | `empleados` | Personal de la empresa |
| `CatalogoArticulo` | `catalogo_articulos` | Artículos del catálogo |
| `Notificacion` | `notificaciones` | Notificaciones del sistema |
| `Auditoria` | `auditorias` | Log de auditoría |
| `TokenSesion` | `tokens_sesion` | Tokens JWT activos |

---

## 🔌 Endpoints Principales

### Autenticación
```
POST /api/auth/login       → Iniciar sesión (retorna JWT)
POST /api/auth/register    → Registrar usuario
```

### Contratos
```
GET    /api/contratos              → Listar contratos (paginado)
POST   /api/contratos              → Crear contrato
GET    /api/contratos/{id}         → Obtener contrato
PUT    /api/contratos/{id}         → Actualizar contrato
DELETE /api/contratos/{id}         → Eliminar contrato
GET    /api/contratos/persona/{id} → Contratos por persona
```

### Personas
```
GET    /api/personas
POST   /api/personas
GET    /api/personas/{id}
PUT    /api/personas/{id}
DELETE /api/personas/{id}
```

### Pagos
```
GET    /api/pagos
POST   /api/pagos
GET    /api/pagos/{id}
```

### Planes Funerarios
```
GET    /api/planes-funerarios
POST   /api/planes-funerarios
GET    /api/planes-funerarios/{id}
```

> ⚠️ Todos los endpoints excepto `/api/auth/**` requieren autenticación con token JWT en el header:
> `Authorization: Bearer <token>`

---

## ⚙️ Variables de Entorno

Crea un archivo `.env` o configura las siguientes variables en tu entorno:

```env
DB_PASSWORD=tu_password_mysql
MAIL_USERNAME=tu_correo@gmail.com
MAIL_PASSWORD=tu_app_password_gmail
JWT_SECRET=tu_clave_secreta_jwt_muy_larga_y_segura
```

---

## 🚀 Instalación y Ejecución

### Prerrequisitos
- Java 21+
- Maven 3.x
- MySQL 8.x

### Pasos

1. **Clona el repositorio**
   ```bash
   git clone https://github.com/tu-usuario/provisiones-santa-martha-api.git
   cd provisiones-santa-martha-api
   ```

2. **Crea la base de datos**
   ```sql
   CREATE DATABASE shsm_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

3. **Configura las variables de entorno**
   ```bash
   export DB_PASSWORD=tu_password
   export MAIL_USERNAME=tu_correo@gmail.com
   export MAIL_PASSWORD=tu_app_password
   export JWT_SECRET=mi_clave_super_secreta_256bits
   ```

4. **Compila y ejecuta**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

5. **La API estará disponible en:**
   ```
   http://localhost:8080
   ```

---

## 📁 Estructura del Proyecto

```
src/main/java/com/shsm/api/
├── SociedadHumanistaApplication.java  # Punto de entrada
├── config/
│   └── SecurityConfig.java            # Configuración de seguridad
├── controller/                        # Controladores REST
├── dto/                               # Data Transfer Objects
│   ├── auth/
│   ├── contrato/
│   ├── pago/
│   └── persona/
├── entity/                            # Entidades JPA
│   ├── BaseEntity.java                # Superclase con auditoría
│   └── catalog/
├── exception/                         # Manejo global de errores
│   ├── GlobalExceptionHandler.java
│   ├── BusinessException.java
│   └── ResourceNotFoundException.java
├── repository/                        # Repositorios Spring Data JPA
├── security/                          # Filtros y utilidades JWT
└── service/                           # Lógica de negocio
    └── impl/
src/main/resources/
└── application.yml                    # Configuración de la aplicación
```

---

## 🔐 Seguridad

- Autenticación **stateless** con **JWT (JSON Web Tokens)**
- Tokens con expiración de **24 horas** (86400000 ms)
- Contraseñas hasheadas con **BCrypt**
- Sesiones sin estado (`SessionCreationPolicy.STATELESS`)
- Rutas públicas: solo `/api/auth/**`

---

## 👨‍💻 Autor

**Provisiones Santa Martha** — Sistema de gestión de servicios funerarios

---

## 📄 Licencia

Este proyecto es de uso privado para **Provisiones Santa Martha**.