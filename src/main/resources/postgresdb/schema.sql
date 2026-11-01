BEGIN;

-- =========================================================
-- Enums
-- =========================================================

CREATE TYPE PAYMENT_STATUS AS ENUM (
    'PAID',
    'CANCELLED',
    'WAITING'
);

CREATE TYPE CATEGORY_CLASSIFICATION AS ENUM (
    'DOWNSTREAM',
    'UPSTREAM'
);

CREATE TYPE EMPLOYEE_STATUS AS ENUM(
	'ACTIVE',
	'INACTIVE',
	'IN_VACATION'
);

CREATE TYPE INVENTORY_STATUS AS ENUM(
    'UNDER_REVIEW',
    'APPROVED',
    'REJECTED',
    'PROCESSING',
    'PROCESSING_FAILED',
    'INSUFFICIENT_DATA',
    'REQUESTED',
    'ISSUED'
);

CREATE TYPE INVENTORY_TYPE AS ENUM(
    'INPUT',
    'OUTPUT'
);

CREATE TYPE PROBLEM_CATEGORY AS ENUM(
    'ERROR',
    'BUG',
    'LOGIN_AND_ACCOUNT',
    'INTERFACE_AND_NAVIGATION',
    'PERFORMANCE',
    'DATA_AND_INFORMATION',
    'PAYMENT',
    'PRIVACY',
    'OTHERS'
);

-- =========================================================
-- Tabelas administrativas / institucionais
-- =========================================================

CREATE TABLE plan (
                      id SERIAL,
                      name VARCHAR(50) NOT NULL UNIQUE,
                      description VARCHAR(150),
                      price NUMERIC NOT NULL CHECK (price >= 0),
                      duration_days INTEGER NOT NULL CHECK (duration_days > 0),
                      is_active BOOLEAN NOT NULL DEFAULT true,
                      created_at TIMESTAMP DEFAULT current_timestamp,
                      updated_at TIMESTAMP,
                      CONSTRAINT pk_plan PRIMARY KEY (id)
);

CREATE TABLE address (
                         id SERIAL,
                         zip_code CHAR(8),
                         state VARCHAR(150) NOT NULL,
                         city VARCHAR(150) NOT NULL,
                         neighborhood VARCHAR(150) NOT NULL,
                         street VARCHAR(150) NOT NULL,
                         number INTEGER NOT NULL,
                         complement VARCHAR(150),
                         created_at TIMESTAMP DEFAULT current_timestamp,
                         updated_at TIMESTAMP,
                         CONSTRAINT pk_address PRIMARY KEY (id)
);

CREATE TABLE enterprise (
                            id SERIAL,
                            name VARCHAR(150) NOT NULL,
                            trade_name VARCHAR(150),
                            cnpj CHAR(14) NOT NULL UNIQUE,
                            created_at TIMESTAMP DEFAULT current_timestamp,
                            updated_at TIMESTAMP,
                            id_address INTEGER,
                            CONSTRAINT pk_enterprise PRIMARY KEY (id)
);

CREATE TABLE plan_subscription (
                                   id SERIAL,
                                   is_active BOOLEAN NOT NULL DEFAULT true,
                                   installments INTEGER NOT NULL CHECK (installments >= 0),
                                   created_at TIMESTAMP DEFAULT current_timestamp,
                                   deactivated_at TIMESTAMP,
                                   id_plan INTEGER,
                                   id_enterprise INTEGER,
                                   CONSTRAINT pk_plan_subscription PRIMARY KEY (id)
);

CREATE TABLE payment (
                         id SERIAL,
                         value NUMERIC NOT NULL CHECK (value > 0),
                         term TIMESTAMP,
                         status PAYMENT_STATUS NOT NULL,
                         created_at TIMESTAMP DEFAULT current_timestamp,
                         updated_at TIMESTAMP,
                         additional_use_value NUMERIC CHECK (additional_use_value >= 0),
                         due_date DATE NOT NULL,
                         id_plan_subscription INTEGER,
                         CONSTRAINT pk_payment PRIMARY KEY (id)
);

CREATE TABLE unit (
                      id SERIAL,
                      cnae CHAR(7),
                      cnpj CHAR(14) NOT NULL UNIQUE,
                      is_active BOOLEAN NOT NULL DEFAULT true,
                      created_at TIMESTAMP DEFAULT current_timestamp,
                      updated_at TIMESTAMP,
                      id_enterprise INTEGER,
                      id_address INTEGER,
                      CONSTRAINT pk_unit PRIMARY KEY (id)
);

CREATE TABLE department (
                            id SERIAL,
                            name VARCHAR(150) NOT NULL,
                            description VARCHAR(150),
                            created_at TIMESTAMP DEFAULT current_timestamp,
                            updated_at TIMESTAMP,
                            id_unit INTEGER,
                            CONSTRAINT pk_department PRIMARY KEY (id)
);

CREATE TABLE permission_group (
                                  id SERIAL,
                                  description VARCHAR(150) NOT NULL,
                                  created_at TIMESTAMP DEFAULT current_timestamp,
                                  id_enterprise INTEGER,
                                  CONSTRAINT pk_permission_group PRIMARY KEY (id)
);

CREATE TABLE permission (
                            id SERIAL,
                            name VARCHAR(150) NOT NULL,
                            description VARCHAR(150),
                            pattern VARCHAR(150) NOT NULL,
                            created_at TIMESTAMP DEFAULT current_timestamp,
                            updated_at TIMESTAMP,
                            CONSTRAINT pk_permission PRIMARY KEY (id),
                            CONSTRAINT uq_permission_pattern UNIQUE (pattern),
                            CONSTRAINT ck_permission_pattern_not_empty CHECK (btrim(pattern) <> '')
);

CREATE TABLE permission_group_permission (
                                             id SERIAL,
                                             created_at TIMESTAMP DEFAULT current_timestamp,
                                             updated_at TIMESTAMP,
                                             id_permission INTEGER,
                                             id_permission_group INTEGER,
                                             CONSTRAINT pk_permission_group_permission PRIMARY KEY (id),
                                             CONSTRAINT uq_permission_group_permission UNIQUE (id_permission, id_permission_group)
);

CREATE TABLE parana_seal_forecast (
                                      id SERIAL,
                                      score NUMERIC CHECK (score >= 0),
                                      level INTEGER CHECK (level >= 0),
                                      valid_until DATE,
                                      created_at TIMESTAMP DEFAULT current_timestamp,
                                      updated_at TIMESTAMP,
                                      id_unit INTEGER,
                                      CONSTRAINT pk_parana_seal_forecast PRIMARY KEY (id)
);

CREATE TABLE administrator (
                               id SERIAL,
                               email VARCHAR(255) NOT NULL UNIQUE,
                               password_hash VARCHAR(255) NOT NULL,
                               created_at TIMESTAMP DEFAULT current_timestamp,
                               updated_at TIMESTAMP,
                               CONSTRAINT pk_administrator PRIMARY KEY (id)
);

CREATE TABLE storage_file (
                              id SERIAL,
                              name VARCHAR(150) NOT NULL,
                              path VARCHAR(255) NOT NULL,
                              created_at TIMESTAMP DEFAULT current_timestamp,
                              updated_at TIMESTAMP,
                              CONSTRAINT pk_storage_file PRIMARY KEY (id)
);

CREATE TABLE employee (
                          id SERIAL,
                          cpf CHAR(11) NOT NULL UNIQUE,
                          name VARCHAR(150) NOT NULL,
                          email VARCHAR(255) NOT NULL UNIQUE,
                          phone VARCHAR(20),
                          password_hash VARCHAR(255) NOT NULL,
                          employee_status EMPLOYEE_STATUS NOT NULL,
                          created_at TIMESTAMP DEFAULT current_timestamp,
                          updated_at TIMESTAMP,
                          id_storage_file INTEGER,
                          id_department INTEGER NOT NULL,
                          id_permission_group INTEGER NOT NULL,
                          CONSTRAINT pk_employee PRIMARY KEY (id)
);

-- =========================================================
-- Tabelas do domínio GHG
-- =========================================================

-- Escopo: Scope 1 / Scope 2 / Scope 3 (Tabela 1 e 7 do template)
CREATE TABLE scope (
                       id SERIAL,
                       name VARCHAR(100) NOT NULL UNIQUE,
                       CONSTRAINT pk_scope PRIMARY KEY (id)
);

-- Categoria: as 15 categorias de Scope 3 (upstream/downstream) + "Other"
-- classification só é relevante para linhas de Scope 3; nas demais fica NULL.
CREATE TABLE category (
                          id SERIAL,
                          name VARCHAR(150) NOT NULL UNIQUE,
                          classification CATEGORY_CLASSIFICATION,
                          CONSTRAINT pk_category PRIMARY KEY (id)
);

-- Gas: gases do inventário (CO2, CH4, N2O, HFCs, PFCs, SF6, entre outros)
CREATE TABLE gas (
                     id SERIAL,
                     name VARCHAR(150) NOT NULL,
                     formula VARCHAR(150) NOT NULL UNIQUE,
                     is_biogenic BOOLEAN NOT NULL,
                     gwp NUMERIC NOT NULL CHECK (gwp > 0),
                     CONSTRAINT pk_gas PRIMARY KEY (id)
);

-- inventory: informações descritivas do inventário (Parte 1 do template)
CREATE TABLE inventory (
                           id SERIAL,
                           name VARCHAR(150) NOT NULL,
                           description VARCHAR(150),
                           consolidation_approach VARCHAR(100),
                           inventorying_period_start DATE,
                           inventorying_period_end DATE CHECK (inventorying_period_end >= inventorying_period_start),
                           status INVENTORY_STATUS NOT NULL,
                           status_message VARCHAR(100),
                           type INVENTORY_TYPE NOT NULL,
                           created_at TIMESTAMP DEFAULT current_timestamp,
                           updated_at TIMESTAMP,
                           id_department INTEGER,
                           id_storage_file INTEGER,
                           id_owner_employee INTEGER,
                           id_validator_employee INTEGER,
                           CONSTRAINT pk_inventory PRIMARY KEY (id)
);

-- emission: dados de emissão de GEE (Parte 2 e Parte 3 do template)
-- quantity_co2e é recebido diretamente do inventory de origem (já convertido);
-- a quantidade original do gás não é armazenada por decisão de negócio,
-- pois pode ser obtida dividindo quantity_co2e pelo gwp do gás em gas.gwp.
CREATE TABLE emission (
                          id SERIAL,
                          quantity_co2e NUMERIC NOT NULL CHECK (quantity_co2e >= 0),
                          methodology_description VARCHAR(150),
                          supplier_data_percentage NUMERIC CHECK (supplier_data_percentage BETWEEN 0 AND 100),
                          created_at TIMESTAMP DEFAULT current_timestamp,
                          updated_at TIMESTAMP,
                          id_gas INTEGER,
                          id_scope INTEGER,
                          id_category INTEGER,
                          id_inventory INTEGER,
                          CONSTRAINT pk_emission PRIMARY KEY (id)
);

-- reduction: iniciativas de redução de emissões, ligadas ao inventory e à categoria
CREATE TABLE reduction (
                           id SERIAL,
                           quantity_co2e NUMERIC NOT NULL,
                           created_at TIMESTAMP DEFAULT current_timestamp,
                           updated_at TIMESTAMP,
                           id_inventory INTEGER,
                           id_category INTEGER,
                           CONSTRAINT pk_reduction PRIMARY KEY (id)
);

CREATE TABLE reported_problem(
                                 id SERIAL,
                                 category PROBLEM_CATEGORY,
                                 description VARCHAR(100),
                                 id_storage_file INTEGER,
                                 CONSTRAINT pk_reported_problem PRIMARY KEY (id)
);

-- =========================================================
-- Chaves estrangeiras
-- =========================================================

ALTER TABLE enterprise
    ADD CONSTRAINT fk_enterprise_address
        FOREIGN KEY (id_address) REFERENCES address (id);

ALTER TABLE plan_subscription
    ADD CONSTRAINT fk_plan_subscription_plan
        FOREIGN KEY (id_plan) REFERENCES plan (id)
            ON DELETE CASCADE;

ALTER TABLE plan_subscription
    ADD CONSTRAINT fk_plan_subscription_enterprise
        FOREIGN KEY (id_enterprise) REFERENCES enterprise (id);

ALTER TABLE payment
    ADD CONSTRAINT fk_payment_plan_subscription
        FOREIGN KEY (id_plan_subscription) REFERENCES plan_subscription (id)
            ON DELETE RESTRICT;

ALTER TABLE unit
    ADD CONSTRAINT fk_unit_enterprise
        FOREIGN KEY (id_enterprise) REFERENCES enterprise (id);

ALTER TABLE unit
    ADD CONSTRAINT fk_unit_address
        FOREIGN KEY (id_address) REFERENCES address (id);

ALTER TABLE department
    ADD CONSTRAINT fk_department_unit
        FOREIGN KEY (id_unit) REFERENCES unit (id)
            ON DELETE RESTRICT;

ALTER TABLE permission_group
    ADD CONSTRAINT fk_permission_group_enterprise
        FOREIGN KEY (id_enterprise) REFERENCES enterprise (id);

ALTER TABLE permission_group_permission
    ADD CONSTRAINT fk_permission_group_permission_permission
        FOREIGN KEY (id_permission) REFERENCES permission (id);

ALTER TABLE permission_group_permission
    ADD CONSTRAINT fk_permission_group_permission_permission_group
        FOREIGN KEY (id_permission_group) REFERENCES permission_group (id);

ALTER TABLE parana_seal_forecast
    ADD CONSTRAINT fk_parana_seal_forecast_unit
        FOREIGN KEY (id_unit) REFERENCES unit (id)
            ON DELETE CASCADE;

ALTER TABLE employee
    ADD CONSTRAINT fk_employee_storage_file
        FOREIGN KEY (id_storage_file) REFERENCES storage_file (id);

ALTER TABLE employee
    ADD CONSTRAINT fk_employee_department
        FOREIGN KEY (id_department) REFERENCES department (id);

ALTER TABLE employee
    ADD CONSTRAINT fk_employee_permission_group
        FOREIGN KEY (id_permission_group) REFERENCES permission_group (id)
            ON DELETE RESTRICT;

ALTER TABLE inventory
    ADD CONSTRAINT fk_inventory_department
        FOREIGN KEY (id_department) REFERENCES department (id);

ALTER TABLE inventory
    ADD CONSTRAINT fk_inventory_storage_file
        FOREIGN KEY (id_storage_file) REFERENCES storage_file (id);

ALTER TABLE inventory
    ADD CONSTRAINT fk_inventory_owner_employee
        FOREIGN KEY (id_owner_employee) REFERENCES employee (id);

ALTER TABLE inventory
    ADD CONSTRAINT fk_inventory_validator_employee
        FOREIGN KEY (id_validator_employee) REFERENCES employee (id);

ALTER TABLE emission
    ADD CONSTRAINT fk_emission_gas
        FOREIGN KEY (id_gas) REFERENCES gas (id)
            ON DELETE RESTRICT;

ALTER TABLE emission
    ADD CONSTRAINT fk_emission_scope
        FOREIGN KEY (id_scope) REFERENCES scope (id)
            ON DELETE RESTRICT;

ALTER TABLE emission
    ADD CONSTRAINT fk_emission_category
        FOREIGN KEY (id_category) REFERENCES category (id)
            ON DELETE RESTRICT;

ALTER TABLE emission
    ADD CONSTRAINT fk_emission_inventory
        FOREIGN KEY (id_inventory) REFERENCES inventory (id)
            ON DELETE CASCADE;

ALTER TABLE reduction
    ADD CONSTRAINT fk_reduction_inventory
        FOREIGN KEY (id_inventory) REFERENCES inventory (id)
            ON DELETE CASCADE;

ALTER TABLE reduction
    ADD CONSTRAINT fk_reduction_category
        FOREIGN KEY (id_category) REFERENCES category (id)
            ON DELETE RESTRICT;

ALTER TABLE reported_problem
    ADD CONSTRAINT fk_reported_problem_storage_file
        FOREIGN KEY (id_storage_file) REFERENCES storage_file (id)
            ON DELETE RESTRICT;

-- =========================================================
-- Índices
-- =========================================================

CREATE INDEX idx_enterprise_id_address ON enterprise (id_address);
CREATE INDEX idx_plan_subscription_id_plan ON plan_subscription (id_plan);
CREATE INDEX idx_plan_subscription_id_enterprise ON plan_subscription (id_enterprise);
CREATE INDEX idx_payment_id_plan_subscription ON payment (id_plan_subscription);
CREATE INDEX idx_unit_id_enterprise ON unit (id_enterprise);
CREATE INDEX idx_unit_id_address ON unit (id_address);
CREATE INDEX idx_department_id_unit ON department (id_unit);
CREATE INDEX idx_permission_group_id_enterprise ON permission_group (id_enterprise);
CREATE INDEX idx_permission_group_permission_id_permission ON permission_group_permission (id_permission);
CREATE INDEX idx_permission_group_permission_id_permission_group ON permission_group_permission (id_permission_group);
CREATE INDEX idx_parana_seal_forecast_id_unit ON parana_seal_forecast (id_unit);
CREATE INDEX idx_employee_id_storage_file ON employee (id_storage_file);
CREATE INDEX idx_employee_id_department ON employee (id_department);
CREATE INDEX idx_employee_id_permission_group ON employee (id_permission_group);
CREATE INDEX idx_employee_status ON employee (employee_status);
CREATE INDEX idx_inventory_id_department ON inventory (id_department);
CREATE INDEX idx_inventory_id_storage_file ON inventory (id_storage_file);
CREATE INDEX idx_inventory_id_owner_employee ON inventory (id_owner_employee);
CREATE INDEX idx_inventory_id_validator_employee ON inventory (id_validator_employee);
CREATE INDEX idx_emission_id_gas ON emission (id_gas);
CREATE INDEX idx_emission_id_scope ON emission (id_scope);
CREATE INDEX idx_emission_id_category ON emission (id_category);
CREATE INDEX idx_emission_id_inventory ON emission (id_inventory);
CREATE INDEX idx_reduction_id_inventory ON reduction (id_inventory);
CREATE INDEX idx_reduction_id_category ON reduction (id_category);
CREATE INDEX idx_reported_problem_id_storage_file ON reported_problem (id_storage_file);

-- =========================================================
-- Comentários
-- =========================================================

COMMENT ON COLUMN category.classification IS 'Indicates whether the category is upstream, downstream, or another category, according to the GHG Protocol Scope 3 categories. Applicable only when the emission belongs to Scope 3.';
COMMENT ON COLUMN emission.quantity_co2e IS 'Emission quantity received from the source inventory, already expressed in metric tons of CO2 equivalent (CO2e). The original gas quantity, when needed, can be derived by dividing this value by the corresponding GWP in gas.gwp.';
COMMENT ON COLUMN gas.is_biogenic IS 'Indicates whether the gas emissions are of biogenic origin, according to Part 3 of the GHG template.';
COMMENT ON COLUMN gas.gwp IS 'Global Warming Potential (GWP) factor of the gas, used to convert its physical quantity into CO2e.';

CREATE OR REPLACE FUNCTION fn_validate_employee_permission_group_enterprise()
RETURNS trigger
LANGUAGE plpgsql
SET search_path = public, pg_temp
AS $$
DECLARE
v_employee_enterprise_id integer;
    v_group_enterprise_id integer;
BEGIN
SELECT u.id_enterprise
INTO v_employee_enterprise_id
FROM department d
         JOIN unit u ON u.id = d.id_unit
WHERE d.id = NEW.id_department;

IF v_employee_enterprise_id IS NULL THEN
        RAISE EXCEPTION 'employee department must resolve to an enterprise'
            USING ERRCODE = '23514';
END IF;

SELECT pg.id_enterprise
INTO v_group_enterprise_id
FROM permission_group pg
WHERE pg.id = NEW.id_permission_group;

IF v_group_enterprise_id IS NOT NULL
       AND v_employee_enterprise_id <> v_group_enterprise_id THEN
        RAISE EXCEPTION 'permission group must belong to the same enterprise as the employee department'
            USING ERRCODE = '23514';
END IF;

RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION fn_validate_permission_group_enterprise_change()
RETURNS trigger
LANGUAGE plpgsql
SET search_path = public, pg_temp
AS $$
BEGIN
    IF NEW.id_enterprise IS DISTINCT FROM OLD.id_enterprise
       AND NEW.id_enterprise IS NOT NULL
       AND EXISTS (
           SELECT 1
           FROM employee e
           JOIN department d ON d.id = e.id_department
           JOIN unit u ON u.id = d.id_unit
           WHERE e.id_permission_group = NEW.id
             AND u.id_enterprise IS DISTINCT FROM NEW.id_enterprise
       ) THEN
        RAISE EXCEPTION 'cannot change permission group enterprise while employees reference it from another enterprise'
            USING ERRCODE = '23514';
END IF;

RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validate_employee_permission_group_enterprise
    BEFORE INSERT OR UPDATE OF id_department, id_permission_group ON employee
    FOR EACH ROW
    EXECUTE FUNCTION fn_validate_employee_permission_group_enterprise();

CREATE TRIGGER trg_validate_permission_group_enterprise_change
    BEFORE UPDATE OF id_enterprise ON permission_group
    FOR EACH ROW
    EXECUTE FUNCTION fn_validate_permission_group_enterprise_change();

COMMENT ON FUNCTION fn_validate_employee_permission_group_enterprise() IS
    'Ensures scoped permission groups belong to the same enterprise as the employee department. Global groups (null id_enterprise) are allowed for any employee. AUTH-FR-006, AUTH-FR-007.';

COMMENT ON FUNCTION fn_validate_permission_group_enterprise_change() IS
    'Prevents changing a permission group enterprise when employees from another enterprise still reference it. AUTH-FR-007.';

-- Manual cells of the complete GHG emission declaration (frames 1.1, 1.2, 1.3, 1.5).
CREATE OR REPLACE FUNCTION fn_ghg_complete_declaration(
    p_enterprise_id integer
)
RETURNS TABLE (
    quadro text,
    eixo   text,
    codigo text,
    valor  numeric
)
LANGUAGE plpgsql
STABLE
SET search_path = public, pg_temp
AS $$
DECLARE
v_target_year integer;
    v_period_start date;
    v_period_end date;
BEGIN
    IF p_enterprise_id IS NULL THEN
        RAISE EXCEPTION 'enterprise_id is required'
            USING ERRCODE = '22023';
END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM enterprise e
        WHERE e.id = p_enterprise_id
    ) THEN
        RAISE EXCEPTION 'Enterprise % not found', p_enterprise_id
            USING ERRCODE = '23503';
END IF;

    v_target_year := EXTRACT(YEAR FROM CURRENT_DATE)::integer - 1;
    v_period_start := make_date(v_target_year, 1, 1);
    v_period_end := make_date(v_target_year, 12, 31);

RETURN QUERY
    WITH skeleton AS (
        SELECT '1.1'::text AS cell_quadro, 'GAS'::text AS cell_eixo, gas_code AS cell_codigo
        FROM unnest(ARRAY[
            'CO2', 'CH4', 'N2O', 'HFCs',
            'HFC-23', 'HFC-32', 'HFC-41', 'HFC-125', 'HFC-134', 'HFC-134a',
            'HFC-143', 'HFC-143a', 'HFC-152', 'HFC-152a', 'HFC-161',
            'HFC-227ea', 'HFC-236cb', 'HFC-236ea', 'HFC-236fa',
            'HFC-245ca', 'HFC-245fa', 'HFC-365mfc', 'HFC-43-10mee',
            'PFCs',
            'PFC-14', 'PFC-116', 'PFC-218', 'PFC-318',
            'PFC-3-1-10', 'PFC-4-1-12', 'PFC-5-1-14', 'PFC-9-1-18',
            'trifluorometil_pentafluoreto_enxofre', 'perfluorociclopropano',
            'SF6', 'NF3'
        ]::text[]) AS gas_code

        UNION ALL
        SELECT '1.1', 'CATEGORIA', cat_code
        FROM unnest(ARRAY[
            'combustao_movel',
            'combustao_estacionaria',
            'emissoes_fugitivas',
            'processos_industriais',
            'atividades_agricolas_e_mudancas_uso_solo',
            'residuos'
        ]::text[]) AS cat_code

        UNION ALL
        SELECT frame, 'GAS', gas_code
        FROM unnest(ARRAY['1.2', '1.3']::text[]) AS frame
        CROSS JOIN unnest(ARRAY[
            'CO2', 'CH4', 'N2O', 'HFC', 'PFC', 'SF6', 'NF3'
        ]::text[]) AS gas_code

        UNION ALL
        SELECT '1.2', 'CATEGORIA', cat_code
        FROM unnest(ARRAY[
            'energia_eletrica_localizacao',
            'energia_termica',
            'perdas_transmissao_distribuicao'
        ]::text[]) AS cat_code

        UNION ALL
        SELECT '1.3', axis, cat_code
        FROM unnest(ARRAY[
            'CATEGORIA', 'BIOGENICO_EMISSAO', 'BIOGENICO_REMOCAO'
        ]::text[]) AS axis
        CROSS JOIN unnest(ARRAY[
            'transporte_distribuicao_upstream',
            'residuos_solidos_operacao',
            'efluentes_liquidos',
            'viagens_a_negocios',
            'deslocamento_casa_trabalho',
            'transporte_distribuicao_downstream',
            'outras_escopo_iii'
        ]::text[]) AS cat_code

        UNION ALL
        SELECT '1.5', axis, scope_code
        FROM unnest(ARRAY[
            'BIOGENICO_EMISSAO', 'BIOGENICO_REMOCAO'
        ]::text[]) AS axis
        CROSS JOIN unnest(ARRAY['ESCOPO_I', 'ESCOPO_II']::text[]) AS scope_code
    ),
    quadro_scope AS (
        SELECT *
        FROM (VALUES
            ('1.1'::text, 'scope 1'::text),
            ('1.2'::text, 'scope 2'::text),
            ('1.3'::text, 'scope 3'::text)
        ) AS t(cell_quadro, scope_key)
    ),
    category_alias AS (
        SELECT *
        FROM (VALUES
            ('1.1'::text, 'combustao_movel'::text, 'Combustão móvel'::text),
            ('1.1', 'combustao_estacionaria', 'Combustão estacionária'),
            ('1.1', 'emissoes_fugitivas', 'Emissões fugitivas'),
            ('1.1', 'processos_industriais', 'Processos industriais'),
            ('1.1', 'atividades_agricolas_e_mudancas_uso_solo', 'Atividades agrícolas e mudanças no uso do solo'),
            ('1.1', 'residuos', 'Resíduos (resíduos sólidos + efluentes)'),
            ('1.1', 'residuos', 'Resíduos'),
            ('1.2', 'energia_eletrica_localizacao', 'Compra de Energia Elétrica, pelo critério de localização geográfica'),
            ('1.2', 'energia_eletrica_localizacao', 'Energia elétrica adquirida'),
            ('1.2', 'energia_termica', 'Compra de Energia Térmica'),
            ('1.2', 'perdas_transmissao_distribuicao', 'Perdas na Transmissão e Distribuição'),
            ('1.3', 'transporte_distribuicao_upstream', 'Transporte e distribuição (upstream)'),
            ('1.3', 'transporte_distribuicao_upstream', 'Transporte e distribuição upstream'),
            ('1.3', 'residuos_solidos_operacao', 'Resíduos sólidos gerados na operação'),
            ('1.3', 'residuos_solidos_operacao', 'Resíduos gerados nas operações'),
            ('1.3', 'efluentes_liquidos', 'Efluentes líquidos gerados'),
            ('1.3', 'viagens_a_negocios', 'Viagens a negócios'),
            ('1.3', 'deslocamento_casa_trabalho', 'Deslocamento (casa-trabalho)'),
            ('1.3', 'deslocamento_casa_trabalho', 'Deslocamento de funcionários'),
            ('1.3', 'transporte_distribuicao_downstream', 'Transporte e distribuição (downstream)'),
            ('1.3', 'transporte_distribuicao_downstream', 'Transporte e distribuição downstream')
        ) AS t(cell_quadro, cell_codigo, alias_name)
    ),
    eligible AS MATERIALIZED (
        SELECT
            fe.scope_name,
            fe.category_name,
            fe.gas_id,
            fe.gas_name,
            fe.gas_formula,
            fe.gas_is_biogenic,
            fe.quantity_co2e
        FROM vw_fact_emission fe
        WHERE fe.enterprise_id = p_enterprise_id
          AND fe.inventory_status = 'APPROVED'
          AND fe.period_start = v_period_start
          AND fe.period_end = v_period_end
    ),
    gas_matches AS (
        SELECT DISTINCT
            sk.cell_quadro,
            sk.cell_codigo,
            catalog_gas.gas_id
        FROM skeleton sk
        CROSS JOIN (
            SELECT DISTINCT
                el.gas_id,
                el.gas_formula,
                el.gas_name
            FROM eligible el
            WHERE el.gas_id IS NOT NULL
        ) AS catalog_gas
        WHERE sk.cell_eixo = 'GAS'
          AND (
              lower(btrim(catalog_gas.gas_formula)) = lower(sk.cell_codigo)
              OR lower(btrim(catalog_gas.gas_name)) = lower(sk.cell_codigo)
              OR (
                  sk.cell_codigo IN ('HFCs', 'HFC')
                  AND (
                      lower(btrim(catalog_gas.gas_formula)) IN ('hfcs', 'hfc')
                      OR lower(btrim(catalog_gas.gas_name)) IN ('hfcs', 'hfc')
                  )
              )
              OR (
                  sk.cell_codigo IN ('PFCs', 'PFC')
                  AND (
                      lower(btrim(catalog_gas.gas_formula)) IN ('pfcs', 'pfc')
                      OR lower(btrim(catalog_gas.gas_name)) IN ('pfcs', 'pfc')
                  )
              )
              OR (
                  sk.cell_codigo = 'trifluorometil_pentafluoreto_enxofre'
                  AND (
                      lower(btrim(catalog_gas.gas_formula)) = 'sf5cf3'
                      OR lower(btrim(catalog_gas.gas_name))
                          = 'trifluorometil pentafluoreto de enxofre'
                  )
              )
              OR (
                  sk.cell_codigo = 'perfluorociclopropano'
                  AND (
                      lower(btrim(catalog_gas.gas_formula)) = 'c-c3f6'
                      OR lower(btrim(catalog_gas.gas_name)) = 'perfluorociclopropano'
                  )
              )
          )
    ),
    gas_values AS (
        SELECT
            sk.cell_quadro,
            sk.cell_eixo,
            sk.cell_codigo,
            SUM(el.quantity_co2e) AS cell_valor
        FROM skeleton sk
        JOIN quadro_scope qs
          ON qs.cell_quadro = sk.cell_quadro
        JOIN gas_matches gm
          ON gm.cell_quadro = sk.cell_quadro
         AND gm.cell_codigo = sk.cell_codigo
        JOIN eligible el
          ON el.gas_id = gm.gas_id
         AND el.gas_is_biogenic IS FALSE
         AND lower(btrim(el.scope_name)) = qs.scope_key
        WHERE sk.cell_eixo = 'GAS'
        GROUP BY sk.cell_quadro, sk.cell_eixo, sk.cell_codigo
    ),
    category_values AS (
        SELECT
            sk.cell_quadro,
            sk.cell_eixo,
            sk.cell_codigo,
            SUM(el.quantity_co2e) AS cell_valor
        FROM skeleton sk
        JOIN quadro_scope qs
          ON qs.cell_quadro = sk.cell_quadro
        JOIN category_alias ca
          ON ca.cell_quadro = sk.cell_quadro
         AND ca.cell_codigo = sk.cell_codigo
        JOIN eligible el
          ON el.gas_is_biogenic IS FALSE
         AND lower(btrim(el.scope_name)) = qs.scope_key
         AND lower(btrim(el.category_name)) = lower(btrim(ca.alias_name))
        WHERE sk.cell_eixo = 'CATEGORIA'
          AND sk.cell_codigo <> 'outras_escopo_iii'
        GROUP BY sk.cell_quadro, sk.cell_eixo, sk.cell_codigo
    ),
    scope3_other_values AS (
        SELECT
            '1.3'::text AS cell_quadro,
            'CATEGORIA'::text AS cell_eixo,
            'outras_escopo_iii'::text AS cell_codigo,
            SUM(el.quantity_co2e) AS cell_valor
        FROM eligible el
        WHERE el.gas_is_biogenic IS FALSE
          AND lower(btrim(el.scope_name)) = 'scope 3'
          AND (
              el.category_name IS NULL
              OR NOT EXISTS (
                  SELECT 1
                  FROM category_alias ca
                  WHERE ca.cell_quadro = '1.3'
                    AND lower(btrim(el.category_name)) = lower(btrim(ca.alias_name))
              )
          )
        HAVING SUM(el.quantity_co2e) IS NOT NULL
    ),
    biogenic_category_values AS (
        SELECT
            sk.cell_quadro,
            sk.cell_eixo,
            sk.cell_codigo,
            SUM(el.quantity_co2e) AS cell_valor
        FROM skeleton sk
        JOIN category_alias ca
          ON ca.cell_quadro = '1.3'
         AND ca.cell_codigo = sk.cell_codigo
        JOIN eligible el
          ON el.gas_is_biogenic IS TRUE
         AND lower(btrim(el.scope_name)) = 'scope 3'
         AND lower(btrim(el.category_name)) = lower(btrim(ca.alias_name))
        WHERE sk.cell_quadro = '1.3'
          AND sk.cell_eixo = 'BIOGENICO_EMISSAO'
          AND sk.cell_codigo <> 'outras_escopo_iii'
        GROUP BY sk.cell_quadro, sk.cell_eixo, sk.cell_codigo
    ),
    biogenic_scope3_other_values AS (
        SELECT
            '1.3'::text AS cell_quadro,
            'BIOGENICO_EMISSAO'::text AS cell_eixo,
            'outras_escopo_iii'::text AS cell_codigo,
            SUM(el.quantity_co2e) AS cell_valor
        FROM eligible el
        WHERE el.gas_is_biogenic IS TRUE
          AND lower(btrim(el.scope_name)) = 'scope 3'
          AND (
              el.category_name IS NULL
              OR NOT EXISTS (
                  SELECT 1
                  FROM category_alias ca
                  WHERE ca.cell_quadro = '1.3'
                    AND lower(btrim(el.category_name)) = lower(btrim(ca.alias_name))
              )
          )
        HAVING SUM(el.quantity_co2e) IS NOT NULL
    ),
    biogenic_15_values AS (
        SELECT
            '1.5'::text AS cell_quadro,
            'BIOGENICO_EMISSAO'::text AS cell_eixo,
            CASE lower(btrim(el.scope_name))
                WHEN 'scope 1' THEN 'ESCOPO_I'
                WHEN 'scope 2' THEN 'ESCOPO_II'
            END AS cell_codigo,
            SUM(el.quantity_co2e) AS cell_valor
        FROM eligible el
        WHERE el.gas_is_biogenic IS TRUE
          AND lower(btrim(el.scope_name)) IN ('scope 1', 'scope 2')
        GROUP BY 3
    ),
    cell_value AS (
        SELECT * FROM gas_values
        UNION ALL
        SELECT * FROM category_values
        UNION ALL
        SELECT * FROM scope3_other_values
        UNION ALL
        SELECT * FROM biogenic_category_values
        UNION ALL
        SELECT * FROM biogenic_scope3_other_values
        UNION ALL
        SELECT * FROM biogenic_15_values
    )
SELECT
    sk.cell_quadro,
    sk.cell_eixo,
    sk.cell_codigo,
    cv.cell_valor
FROM skeleton sk
         LEFT JOIN cell_value cv
                   ON cv.cell_quadro = sk.cell_quadro
                       AND cv.cell_eixo = sk.cell_eixo
                       AND cv.cell_codigo = sk.cell_codigo;
END;
$$;

COMMENT ON FUNCTION fn_ghg_complete_declaration(integer) IS
    'Returns the 84 manual cells of the complete GHG emission declaration (frames 1.1, 1.2, 1.3, 1.5) for one enterprise. Eligible facts are APPROVED inventories whose period is 1 Jan–31 Dec of the calendar year before CURRENT_DATE. valor is SUM(quantity_co2e) in tCO2e, or NULL when no fact matches. Does not return frame 1.4, spreadsheet automatic totals, reductions, or biogenic removals. Raises 22023 when enterprise_id is null and 23503 when the enterprise does not exist.';

REVOKE ALL ON FUNCTION fn_ghg_complete_declaration(integer) FROM PUBLIC;

GRANT EXECUTE ON FUNCTION fn_ghg_complete_declaration(integer) TO aether_app, aether_auditor, aether_admin;
-- Registers a validated emission and returns its identifier.
CREATE OR REPLACE PROCEDURE sp_register_emission(
    OUT p_emission_id integer,
    IN p_quantity_co2e numeric,
    IN p_id_inventory integer,
    IN p_id_gas integer,
    IN p_id_scope integer,
    IN p_id_category integer DEFAULT NULL,
    IN p_methodology_description varchar DEFAULT NULL,
    IN p_supplier_data_percentage numeric DEFAULT NULL,
    IN p_app_actor_id text DEFAULT NULL,
    IN p_logical_origin text DEFAULT 'PROCEDURE'
)
LANGUAGE plpgsql
SET search_path = public, pg_temp
AS $$
BEGIN
    IF p_app_actor_id IS NOT NULL THEN
        PERFORM set_config('aether.app_actor_id', p_app_actor_id, true);
    END IF;
    PERFORM set_config('aether.logical_origin', COALESCE(p_logical_origin, 'PROCEDURE'), true);

    IF p_quantity_co2e IS NULL OR p_quantity_co2e < 0 THEN
        RAISE EXCEPTION 'quantity_co2e is required and must be non-negative'
            USING ERRCODE = '22023';
    END IF;
    IF p_id_inventory IS NULL THEN
        RAISE EXCEPTION 'id_inventory is required'
            USING ERRCODE = '22023';
    END IF;
    IF p_id_gas IS NULL THEN
        RAISE EXCEPTION 'id_gas is required'
            USING ERRCODE = '22023';
    END IF;
    IF p_id_scope IS NULL THEN
        RAISE EXCEPTION 'id_scope is required'
            USING ERRCODE = '22023';
    END IF;
    IF p_supplier_data_percentage IS NOT NULL
       AND (p_supplier_data_percentage < 0 OR p_supplier_data_percentage > 100) THEN
        RAISE EXCEPTION 'supplier_data_percentage must be between 0 and 100'
            USING ERRCODE = '22023';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM inventory i WHERE i.id = p_id_inventory) THEN
        RAISE EXCEPTION 'Inventory % not found', p_id_inventory
            USING ERRCODE = '23503';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM gas g WHERE g.id = p_id_gas) THEN
        RAISE EXCEPTION 'Gas % not found', p_id_gas
            USING ERRCODE = '23503';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM scope s WHERE s.id = p_id_scope) THEN
        RAISE EXCEPTION 'Scope % not found', p_id_scope
            USING ERRCODE = '23503';
    END IF;
    IF p_id_category IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM category c WHERE c.id = p_id_category) THEN
        RAISE EXCEPTION 'Category % not found', p_id_category
            USING ERRCODE = '23503';
    END IF;

    INSERT INTO emission (
        quantity_co2e,
        methodology_description,
        supplier_data_percentage,
        id_gas,
        id_scope,
        id_category,
        id_inventory
    ) VALUES (
        p_quantity_co2e,
        p_methodology_description,
        p_supplier_data_percentage,
        p_id_gas,
        p_id_scope,
        p_id_category,
        p_id_inventory
    )
    RETURNING id INTO p_emission_id;
END;
$$;

COMMENT ON PROCEDURE sp_register_emission(integer, numeric, integer, integer, integer, integer, varchar, numeric, text, text) IS
    'Registers an emission atomically and with audit. Validates required fields, non-negative values, and existence of inventory, gas, scope, and category when provided. A null category remains valid. OUT p_emission_id identifies the created row. Validation errors are not suppressed. ROT-FR-005, ROT-FR-007, ROT-FR-008.';

-- Registers a validated reduction and returns its identifier.
CREATE OR REPLACE PROCEDURE sp_register_reduction(
    OUT p_reduction_id integer,
    IN p_quantity_co2e numeric,
    IN p_id_inventory integer,
    IN p_id_category integer,
    IN p_app_actor_id text DEFAULT NULL,
    IN p_logical_origin text DEFAULT 'PROCEDURE'
)
LANGUAGE plpgsql
SET search_path = public, pg_temp
AS $$
BEGIN
    IF p_app_actor_id IS NOT NULL THEN
        PERFORM set_config('aether.app_actor_id', p_app_actor_id, true);
    END IF;
    PERFORM set_config('aether.logical_origin', COALESCE(p_logical_origin, 'PROCEDURE'), true);

    IF p_quantity_co2e IS NULL OR p_quantity_co2e < 0 THEN
        RAISE EXCEPTION 'quantity_co2e is required and must be non-negative'
            USING ERRCODE = '22023';
    END IF;
    IF p_id_inventory IS NULL THEN
        RAISE EXCEPTION 'id_inventory is required'
            USING ERRCODE = '22023';
    END IF;
    IF p_id_category IS NULL THEN
        RAISE EXCEPTION 'id_category is required to register a reduction'
            USING ERRCODE = '22023';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM inventory i WHERE i.id = p_id_inventory) THEN
        RAISE EXCEPTION 'Inventory % not found', p_id_inventory
            USING ERRCODE = '23503';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM category c WHERE c.id = p_id_category) THEN
        RAISE EXCEPTION 'Category % not found', p_id_category
            USING ERRCODE = '23503';
    END IF;

    INSERT INTO reduction (
        quantity_co2e,
        id_inventory,
        id_category
    ) VALUES (
        p_quantity_co2e,
        p_id_inventory,
        p_id_category
    )
    RETURNING id INTO p_reduction_id;
END;
$$;

COMMENT ON PROCEDURE sp_register_reduction(integer, numeric, integer, integer, text, text) IS
    'Registers a reduction atomically and with audit. Validates non-negative quantity and existence of inventory and category. OUT p_reduction_id identifies the created row. ROT-FR-006, ROT-FR-007, ROT-FR-008.';

COMMIT;