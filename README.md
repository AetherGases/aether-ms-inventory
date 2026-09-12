# Aether MS Inventory

A **Aether MS Inventory** é o microsserviço do sistema Aether responsável por realizar .

## Tecnologias utilizadas

- Java 21
- Spring Boot
- Spring Web MVC
- Spring Data JPA
- Hibernate
- PostgreSQL
- Maven
- Lombok
- Bean Validation (Jakarta Validation)
- SpringDoc OpenAPI (Swagger)
- Docker e Docker Compose
- JUnit 5
- Mockito

## Arquitetura

O projeto segue uma arquitetura em camadas, separando responsabilidades em módulos como:

- Módulos com funcionalidades que contém:
  - Controllers
  - Services
  - DTOs
- Repositories
- Entities
- Configurations
- Exceptions
- Validators
- Tests

## Documentação da API

Após iniciar a aplicação, a documentação pode ser acessada em:

```
http://localhost:${API_PORT}/swagger-ui.html
```

ou

```
http://localhost:${API_PORT}/swagger-ui/index.html
```

(dependendo da versão do SpringDoc).

## Executando o projeto

### Pré-requisitos

- Java 21
- Maven 3.9+
- Docker
- Docker Compose

### Executando a aplicação

Clone o repositório pelo comado a seguir:

```bash
git clone https://github.com/AetherGases/aether-core-api.git
cd core-api
```

Copie a .env.example para a .env
```bash
.env.example -> .env
```

Execute o comando abaixo para subir os serviços do Docker necesssários:

```bash
docker compose up -d
```

Entre no VS Code, IntelliJ ou sua IDE de preferência e execute o projeto.

## Banco de dados

O projeto utiliza múltiplos bancos de dados:

- **PostgreSQL** para dados relacionais.
- **MongoDB** para documentos e dados não relacionais.

## Perfis

O projeto possui configurações separadas para cada ambiente:

- `dev`
- `staging`
- `prod`

O perfil é definido pela variável e nesse caso representa a de desenvolvimento:

```properties
SPRING_PROFILES_ACTIVE=dev
```

ou pela configuração da IDE.