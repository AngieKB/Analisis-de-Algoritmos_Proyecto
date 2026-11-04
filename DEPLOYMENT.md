# Despliegue de la API

La aplicación expone la API REST bajo `/api/v1` y usa PostgreSQL administrado. No incluye credenciales predeterminadas. El archivo `render.yaml` define un servicio Docker gratuito de Render; la base de datos se crea aparte en Neon Free para evitar el Postgres gratuito de Render, que vence después de 30 días. Neon indica que su plan Free no tiene costo ni requiere tarjeta, pero sus límites (entre ellos 1 GB por proyecto y suspensión del cómputo tras inactividad) hacen que esta opción sea para demo/proyecto académico, no para producción con disponibilidad o respaldos garantizados.

Para desplegar sin pagar:

1. Crea un proyecto gratuito en Neon y copia la cadena JDBC, usuario y contraseña del panel de conexión. Asegúrate de usar SSL (`sslmode=require`).
2. Conecta el repositorio en Render y selecciona **New > Blueprint**. El servicio de aplicación está en plan `free`; no crees una base PostgreSQL en Render.
3. En los valores solicitados por Render, configura `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` con los datos de Neon. En `CORS_ALLOWED_ORIGINS`, escribe el dominio exacto de Netlify, por ejemplo `https://mi-sitio.netlify.app`. `sync: false` evita que estos secretos se guarden en el repositorio.
4. Después del despliegue, comprueba `https://<servicio>.onrender.com/actuator/health`.

Render construye la imagen desde el Dockerfile. Para compilarla localmente:

```sh
docker build -t bibliometria-api .
```

Configura estas variables en el servicio de contenedores antes de iniciarlo:

| Variable | Descripción |
| --- | --- |
| `DB_URL` | URL JDBC completa de PostgreSQL, por ejemplo `jdbc:postgresql://HOST/BASE?sslmode=require` |
| `DB_USERNAME` | Usuario de la base de Neon |
| `DB_PASSWORD` | Contraseña de la base de Neon |
| `CORS_ALLOWED_ORIGINS` | Dominio(s) del frontend separado(s) por coma, por ejemplo `https://mi-sitio.netlify.app` |
| `PORT` | Puerto asignado por la plataforma; por defecto `8080` |

Al primer inicio Flyway crea la tabla `articles` y la aplicación importa los 20 abstracts de `abstracts/` si la tabla está vacía. La carga se serializa con un bloqueo de base de datos para soportar el inicio simultáneo de varias réplicas. No sobrescribe datos existentes: falla explícitamente si encuentra una tabla parcialmente poblada o contenido que no coincide con el corpus incluido. Los cambios futuros al corpus deben desplegarse como una migración de base de datos. Los archivos `embeddings/minilm_abstracts.txt` y `embeddings/glove_corpus.txt` se incluyen en la imagen y se usan para sus respectivos algoritmos. No se debe copiar el modelo ONNX al contenedor.

## Endpoints

- `GET /api/v1/articles` y `GET /api/v1/articles/{id}`: corpus.
- `GET /api/v1/algorithms`: algoritmos disponibles.
- `POST /api/v1/similarities`: compara todos los pares de dos o más artículos.
- `GET /api/v1/clusters?linkage=SINGLE|COMPLETE|AVERAGE`: fusiones y coeficiente cofenético.
- `POST /api/v1/search`: búsqueda por KMP, Boyer–Moore o Rabin–Karp.
- `GET /actuator/health`: health check.

Ejemplo de similitud:

```json
{
  "articleIds": [1, 2, 3],
  "algorithms": ["COSINE_TFIDF", "JACCARD", "MINILM"]
}
```

Ejemplo de búsqueda:

```json
{
  "pattern": "artificial intelligence",
  "algorithm": "KMP",
  "ignoreCase": true
}
```

Los índices de ocurrencia devueltos por la búsqueda son base cero y se refieren al texto del abstract almacenado.
