# Case Shortner API

API REST para encurtamento de URLs — Desafio Itaú.

## Stack

- **Java 21**
- **Spring Boot 4.0.3**
- **Spring Data JPA** com **H2** (banco em memória)
- **Maven**

---

## Como rodar

### Pré-requisitos

- Java 21+
- Maven 4.0.0+

### Com Maven

```bash
./mvnw spring-boot:run
```

Ou compilando antes:

```bash
./mvnw package -DskipTests
java -jar target/case_shortner-0.0.1.jar
```

### Com Docker

```bash
docker compose up --build
```

A aplicação sobe em `http://localhost:8080`.

---

## Como rodar os testes

```bash
./mvnw test
```

---

## Decisões de arquitetura

### Geração de ID
IDs são gerados com **Base62** (a-z, A-Z, 0-9) de 6 caracteres via `SecureRandom`, resultando em ~56 bilhões de combinações possíveis. Colisões são verificadas consultando o banco antes de persistir — em caso de colisão (improvável), até 10 tentativas são feitas.

### Persistência
Banco H2 em memória com Spring Data JPA. Os dados são perdidos ao reiniciar, o que é adequado para o desafio. A camada de repositório (`UrlRepository`) é totalmente reutilizável e substituível por outro banco (PostgreSQL, MySQL) apenas alterando as configurações no `application.yml`.

### Autenticação
Um header `X-API-Key` é obrigatório apenas para `POST /v1/urls`. O filtro `ApiKeyFilter` intercepta apenas esse endpoint. A chave padrão é `my-secret-api-key`, configurável via variável de ambiente `API_KEY`.

### Expiração
O campo `expiration_date` é opcional. No redirecionamento (`GET /{id}`), se a URL estiver expirada, retorna `410 Gone`. Na consulta de detalhes (`GET /v1/urls/{id}`), a URL expirada continua sendo retornada normalmente (sem redirecionamento).

### Custom Format   
O campo `customFormat` é opcional. Deve ter 3–50 caracteres alfanuméricos, hífens ou underscores. Conflitos retornam `409 Conflict`.

### Código de status HTTP para redirect de URL expirada
Optei por `410 Gone` (em vez de `404 Not Found`) para diferenciar semanticamente uma URL que existiu mas expirou de uma URL que nunca existiu.

---

## Exemplos de requisição

### Criar URL encurtada

```bash
curl -X POST http://localhost:8080/v1/urls \
  -H "Content-Type: application/json" \
  -H "X-API-Key: my-secret-api-key" \
  -d '{
    "originalUrl": "https://www.itau.com.br/minha-conta/saldo",
    "expiration_date": "2026-12-31T23:59:59Z"
  }'
```

**Response `201 Created`:**
```json
{
  "id": "abc123",
  "shortUrl": "http://localhost:8080/abc123",
  "originalUrl": "https://www.itau.com.br/minha-conta/saldo",
  "created_date": "2026-03-08T10:00:00Z",
  "expiration_date": "2026-12-31T23:59:59Z",
  "clicks": 0
}
```

### Criar com formato customizado

```bash
curl -X POST http://localhost:8080/v1/urls \
  -H "Content-Type: application/json" \
  -H "X-API-Key: my-secret-api-key" \
  -d '{
    "originalUrl": "https://www.itau.com.br",
    "customFormat": "itauhome"
  }'
```

### Redirecionar

```bash
curl -L http://localhost:8080/abc123
```

Retorna `302 Found` com header `Location: https://www.itau.com.br/minha-conta/saldo`.

### Consultar detalhes

```bash
curl http://localhost:8080/v1/urls/abc123
```

**Response `200 OK`:**
```json
{
  "id": "abc123",
  "shortUrl": "http://localhost:8080/abc123",
  "originalUrl": "https://www.itau.com.br/minha-conta/saldo",
  "created_date": "2026-03-08T10:00:00Z",
  "expiration_date": "2026-12-31T23:59:59Z",
  "clicks": 42
}
```

### Listar URLs (paginado)

```bash
curl "http://localhost:8080/v1/urls?page=0&size=10"
```

---

## Endpoints

| Método | Endpoint          | Auth       | Descrição                         |
|--------|-------------------|------------|-----------------------------------|
| POST   | `/v1/urls`        | X-API-Key  | Cria uma URL encurtada            |
| GET    | `/{id}`           | —          | Redireciona para a URL original   |
| GET    | `/v1/urls/{id}`   | —          | Retorna detalhes da URL           |
| GET    | `/v1/urls`        | —          | Lista URLs com paginação          |

---

## Códigos de erro

| Status | Código interno    | Cenário                                     |
|--------|-------------------|---------------------------------------------|
| 400    | VALIDATION_ERROR  | `originalUrl` ausente ou vazio              |
| 401    | UNAUTHORIZED      | `X-API-Key` ausente ou inválido             |
| 404    | NOT_FOUND         | ID não encontrado                           |
| 409    | DUPLICATE_ALIAS   | `customAlias` já em uso                     |
| 410    | URL_EXPIRED       | URL expirada                                |
| 422    | INVALID_URL       | URL mal formada ou protocolo inválido       |
| 500    | INTERNAL_ERROR    | Erro inesperado                             |

