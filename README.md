# MPAZ PRO 2.0 · Backend

Backend de la app Android **MPAZ PRO 2.0** (DSY1105, Desarrollo de Aplicaciones Móviles).

**Integrantes** · Miguel Pasten Medina y Fabián Aguilar

## Microservicios y arquitectura

Los dos módulos son aplicaciones independientes. Usan Java 17, Spring Boot 3.3.5, Spring Web para la API REST, Jakarta Bean Validation para validar los cuerpos JSON y Spring Data JPA con Hibernate para persistir en H2. Maven compila los módulos y ejecuta las pruebas unitarias con JUnit 5 y Mockito.

Cada `controller` recibe solicitudes HTTP y valida los DTO de entrada. La capa `service` aplica las reglas de negocio y usa los `repository` de Spring Data JPA para consultar o guardar las entidades de `model`. Los manejadores de `exception` convierten los errores de validación y de negocio en respuestas HTTP.

| Microservicio | Puerto | Entidades y tablas | URL JDBC |
|---|---:|---|---|
| `ms-resultados` | 9081 | `Resultado` → `resultados_prueba` | `jdbc:h2:file:./data/resultadosdb` |
| `ms-contenidos` | 9082 | `Objetivo` → `objetivos_aprendizaje`, `Actividad` → `actividades` | `jdbc:h2:file:./data/contenidosdb` |

Cada servicio crea su base H2 en el directorio `data/` de su módulo. Los datos se conservan entre reinicios y Hibernate actualiza el esquema al arrancar. La consola H2 local está habilitada en `/h2-console` de cada puerto.

Los servicios no se llaman entre sí. En resultados, `oaId` se guarda como un identificador positivo sin consultar contenidos. En contenidos, `oaId` de una actividad se comprueba contra los objetivos de su propia base.

## API REST

Los listados devuelven arreglos JSON. Para crear o actualizar un recurso envía `Content-Type: application/json` y los campos del cuerpo descritos abajo. El servidor genera el `id` al crear. Al actualizar, el `id` se toma de la ruta.

### Resultados · puerto 9081

| Método | Ruta | Operación | Éxito |
|---|---|---|---:|
| GET | `/api/resultados` | Lista resultados. Admite los filtros opcionales `estudianteId` y `oaId`. | 200 |
| GET | `/api/resultados/{id}` | Obtiene un resultado. | 200 |
| POST | `/api/resultados` | Crea un resultado. | 201 |
| PUT | `/api/resultados/{id}` | Actualiza un resultado. | 200 |
| DELETE | `/api/resultados/{id}` | Elimina un resultado. | 204 |

Si se envían ambos filtros, prevalece `estudianteId`, que se compara con `estudianteFicticioId`. En el cuerpo, los cinco campos son obligatorios. `estudianteFicticioId` y `oaId` deben ser enteros positivos, `puntaje` un entero mayor o igual a cero, `intentosUsados` un entero entre 1 y 5 inclusive y `fechaRegistro` un entero largo positivo.

### Objetivos · puerto 9082

| Método | Ruta | Operación | Éxito |
|---|---|---|---:|
| GET | `/api/objetivos` | Lista objetivos. | 200 |
| GET | `/api/objetivos/{id}` | Obtiene un objetivo. | 200 |
| POST | `/api/objetivos` | Crea un objetivo. | 201 |
| PUT | `/api/objetivos/{id}` | Actualiza un objetivo. | 200 |
| DELETE | `/api/objetivos/{id}` | Elimina un objetivo sin actividades asociadas. | 204 |

`asignaturaId` debe ser un entero positivo. `codigoMineduc` y `descripcion` son obligatorios y no pueden estar en blanco. `autorizado` es opcional. Al crear o actualizar, si se omite o llega como `null`, se guarda como `false`. Intentar eliminar un objetivo con actividades asociadas devuelve HTTP 400.

### Actividades · puerto 9082

| Método | Ruta | Operación | Éxito |
|---|---|---|---:|
| GET | `/api/actividades` | Lista actividades. Admite el filtro opcional `oaId`. | 200 |
| GET | `/api/actividades/{id}` | Obtiene una actividad. | 200 |
| POST | `/api/actividades` | Crea una actividad. | 201 |
| PUT | `/api/actividades/{id}` | Actualiza una actividad. | 200 |
| DELETE | `/api/actividades/{id}` | Elimina una actividad. | 204 |

`oaId` debe ser un entero positivo y corresponder a un objetivo existente para crear o actualizar una actividad. Un objetivo inexistente en el cuerpo devuelve HTTP 400. `titulo`, `tipoDesafio`, `pregunta` y `respuestaCorrecta` son obligatorios y no pueden estar en blanco. `alternativaIncorrecta1`, `alternativaIncorrecta2` y `alternativaIncorrecta3` son opcionales. Al crear o actualizar, se guarda una cadena vacía por cada alternativa omitida o enviada como `null`.

### Errores

- **400** si el cuerpo JSON es ilegible, falla una validación del DTO o se infringe una regla de negocio.
- **404** si el recurso identificado en la ruta no existe al consultar, actualizar o eliminar.

Los manejadores devuelven los campos `status` y `mensaje`. Los errores de validación del DTO incluyen además el mapa `errores` por campo. Una eliminación exitosa responde **204** sin cuerpo.

## Clonar y ejecutar

Instala un JDK 17 y Git. Cada módulo incluye su Maven Wrapper (`mvnw`), que descarga Maven en su primera ejecución. Los ejemplos usan Bash o Git Bash en Windows.

Clona el repositorio:

```bash
git clone https://github.com/miguelpastenm-web/mpaz-backend.git
cd mpaz-backend
```

Desde la raíz del clon, abre dos terminales. Ejecuta un servicio en cada una y conserva su directorio de trabajo para que su H2 use el `data/` correspondiente:

```bash
cd ms-resultados
./mvnw spring-boot:run
```

```bash
cd ms-contenidos
./mvnw spring-boot:run
```

En Windows con PowerShell o CMD, usa `mvnw.cmd` en lugar de `./mvnw`.

### Probar los endpoints

Con ambos servicios en marcha, crea un objetivo y sustituye `OA_ID=1` por el `id` devuelto (también si ya había registros en H2). Usa ese valor para crear y filtrar la actividad y el resultado:

```bash
curl -i -X POST http://localhost:9082/api/objetivos \
  -H 'Content-Type: application/json' \
  -d '{"asignaturaId":1,"codigoMineduc":"LE02 OA 01","descripcion":"Leer palabras","autorizado":true}'

OA_ID=1
curl -i -X POST http://localhost:9082/api/actividades \
  -H 'Content-Type: application/json' \
  -d "{\"oaId\":$OA_ID,\"titulo\":\"Lectura\",\"tipoDesafio\":\"opcion\",\"pregunta\":\"¿Qué palabra ves?\",\"respuestaCorrecta\":\"casa\"}"

curl -i -X POST http://localhost:9081/api/resultados \
  -H 'Content-Type: application/json' \
  -d "{\"estudianteFicticioId\":1,\"oaId\":$OA_ID,\"puntaje\":80,\"intentosUsados\":1,\"fechaRegistro\":1700000000000}"

curl -i "http://localhost:9082/api/objetivos/$OA_ID"
curl -i "http://localhost:9082/api/actividades?oaId=$OA_ID"
curl -i "http://localhost:9081/api/resultados?estudianteId=1"
curl -i -X DELETE "http://localhost:9082/api/objetivos/$OA_ID"
```

Las creaciones deben responder 201 y las consultas 200. El último comando comprueba que el objetivo todavía tiene una actividad asociada y debe responder 400.

### Ejecutar los tests

Desde la raíz del clon, ejecuta las pruebas de cada módulo con su propio directorio de trabajo:

```bash
(cd ms-resultados && ./mvnw test)
(cd ms-contenidos && ./mvnw test)
```

### Conectar Android

Desde el emulador de Android Studio, `10.0.2.2` apunta al equipo anfitrión donde se ejecuta el backend. Configura estas URL base en la app:

- Resultados · `http://10.0.2.2:9081/`
- Contenidos · `http://10.0.2.2:9082/`

El manifiesto Android debe declarar el permiso `android.permission.INTERNET`. Para desarrollo local, la configuración de red de la app debe permitir HTTP sin cifrar hacia el backend, por ejemplo, con `android:usesCleartextTraffic="true"` en el manifiesto de depuración.
