# Aether MS Inventory

Microsserviço dono do **inventário de gases de efeito estufa (GEE)** do [Aether](https://github.com/AetherGases): recebe a planilha enviada pela unidade, aciona a análise de IA, guarda as emissões extraídas e conduz a revisão (aprovar ou rejeitar). Também gera a planilha de inscrição do **Selo Clima Paraná** a partir dos dados.

O [Kong](https://github.com/AetherGases/aether-kong-gateway) encaminha `/api/inventories` para este serviço.

## Como um inventário é processado

```mermaid
sequenceDiagram
  participant App as App / Portal
  participant Inv as ms-inventory
  participant R as Redis
  participant Hub as ms-aeko-hub (Aeko)
  participant DB as PostgreSQL B

  App->>Inv: POST /api/inventories (URL do .xlsx)
  Inv->>DB: grava inventário com status PROCESSING
  Inv->>R: enfileira input_inventories:{id}
  loop InventoryWorker (a cada 5 s)
    Inv->>R: pega lote da fila (lock)
    Inv->>Inv: baixa o .xlsx e converte em markdown
    Inv->>Hub: pede a análise (AekoClient)
    Hub-->>Inv: resposta com bloco inventory em JSON (emissões)
    Inv->>DB: grava emissões; status UNDER_REVIEW
  end
  Inv-->>App: e-mail (Brevo) e push (Firebase) avisando o gestor
```

Pontos importantes do fluxo:

- Status do inventário: `PROCESSING` → `UNDER_REVIEW` → `APPROVED` ou `REJECTED`. Falhas terminam em `PROCESSING_FAILED` ou `INSUFFICIENT_DATA`; `REQUESTED` e `ISSUED` cobrem a geração da planilha do selo.
- O processamento é **assíncrono**. A rota de envio responde logo e o app acompanha o status do inventário.
- O `InventoryWorker` usa `@Scheduled` com polling (`inventory.worker.*`): lote de 10, até 3 tentativas, 30 s entre tentativas, lock de 300 s, arquivo de até 10 MB e 400 mil caracteres de markdown.
- O download só aceita hosts listados em `INVENTORY_ALLOWED_FILE_HOSTS` (proteção contra SSRF). Veja a nota abaixo.
- O `InventoryGenerationWorker` faz o caminho inverso: gera a planilha de inscrição a partir do modelo `templates/planilha-inscricao.xlsx` (`POST /api/inventories/generate`), também por fila no Redis.
- Valores monetários usam cotações externas: preço do carbono EU ETS (OilPriceAPI) e câmbio, ambos com cache.

## Rotas

Todas sob `/api/inventories` e protegidas por JWT com checagem das permissões do funcionário (`permissionManager`).

| Método e rota | Uso |
| --- | --- |
| `POST /` | Registra um inventário a partir do arquivo enviado |
| `POST /generate` | Gera a planilha do Selo Clima Paraná |
| `GET /mine`, `/mine/last`, `/mine/{id}` | Histórico e detalhe dos inventários do usuário |
| `GET /pending`, `PATCH /pending/{id}/approve`, `PATCH /pending/{id}/reject` | Fila de revisão do gestor |
| `GET /reviewed`, `/reviewed/last`, `/reviewed/{id}` | Inventários já revisados |
| `GET /{id}/analysis` | Plano de melhoria gerado pela IA |
| `GET /count`, `/enterprise-info`, `/unit-info` | Indicadores e dados de empresa e unidade |

O Swagger fica em `/swagger-ui/index.html`.

## Stack

- Java 21, Spring Boot 4.1, Maven
- PostgreSQL B (JPA, `ddl-auto: validate`), MongoDB (notificações), Redis (filas e locks)
- Apache POI para ler e gerar `.xlsx` (planilha de inscrição incluída em `src/main/resources/templates`)
- Brevo (e-mail), Firebase (push), Cloudinary (arquivos), OilPriceAPI e câmbio (cotações)
- Cliente HTTP para o hub de IA (`AekoClient`), autenticado com `INTERNAL_AUTHENTICATION_KEY`

## Estrutura

```text
src/main/java/com/aether/ms_inventory
├── inventory/            # controllers, services, mappers e DTOs (input/output/request/query_params/route_params)
└── shared/
    ├── workers/          # InventoryWorker, InventoryGenerationWorker
    ├── clients/          # AekoClient, CarbonPriceClient, ExchangeRateClient
    ├── persistence/      # postgres, mongo e redis (filas)
    ├── config/ · security/ · exceptions/ · handlers/ · helpers/ (templates de e-mail)
    └── docs/             # *ControllerDocs (Swagger)
```

## Executando localmente

Pré-requisitos: Java 21, Maven 3.9+ e os bancos do [aether-toolskit](https://github.com/AetherGases/aether-toolskit).

```bash
git clone https://github.com/AetherGases/aether-ms-inventory.git
cd aether-ms-inventory
cp .env.example .env
mvn spring-boot:run
```

Para testar o fluxo completo, o [ms-aeko-hub](https://github.com/AetherGases/ms-aeko-hub) precisa estar no ar e apontado por `AEKO_URL`.

### Variáveis de ambiente

| Variável | Descrição |
| --- | --- |
| `API_PORT`, `SPRING_PROFILES_ACTIVE` | Porta (padrão `8080`) e perfil |
| `POSTGRES_*`, `MONGO_URL`, `REDIS_HOST/PORT/PASSWORD` | Conexões de dados |
| `AEKO_URL` | Base URL do `ms-aeko-hub` |
| `INTERNAL_AUTHENTICATION_KEY` | Chave enviada ao hub no header `authentication-key` |
| `INVENTORY_ALLOWED_FILE_HOSTS` | Hosts permitidos para baixar o `.xlsx`, separados por vírgula |
| `OILPRICEAPI_API_KEY` | Preço do carbono (EU ETS) |
| `JWT_SECRET` (+ expirações) | Mesmo segredo do `ms-auth` |
| `BREVO_*`, `CLOUDINARY_*` | E-mail e arquivos |
| `FIREBASE_ENABLED`, `FIREBASE_CREDENTIALS` | Push (desligado por padrão) |
| `CORS_ORIGIN_PATTERNS` | Origens permitidas |

## Testes e deploy

`mvn clean compile test -DskipTests=false` roda no CI a cada push e PR (PostgreSQL 16.10, Redis 8.2 e MongoDB 7 como serviços). Publicar uma **release** dispara o deploy no EKS pelo workflow compartilhado da organização ([.github](https://github.com/AetherGases/.github)).

## Pontos de atenção

- `spring.application.name` está como `core-api`, herdado do monólito arquivado.
- `OILPRICEAPI_API_KEY` aparece duas vezes no `.env.example`.
- `INVENTORY_ALLOWED_FILE_HOSTS` vazio ou com `*` relaxa a proteção contra SSRF. Em produção, liste apenas o host do Cloudinary.
- Ao desligar `inventory.worker.enabled`, inventários ficam em `PROCESSING` até um worker voltar a consumir a fila.
