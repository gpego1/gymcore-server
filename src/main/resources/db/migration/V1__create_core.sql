CREATE TABLE tenants (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL CHECK (btrim(name) <> ''),
    active BOOLEAN NOT NULL
);

CREATE TABLE students (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    version BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL CHECK (btrim(name) <> ''),
    email VARCHAR(254) NOT NULL CHECK (email = lower(btrim(email))),
    phone VARCHAR(16) NOT NULL CHECK (phone ~ '^\+[1-9][0-9]{9,14}$'),
    birth_date DATE NOT NULL,
    active BOOLEAN NOT NULL,
    UNIQUE (tenant_id, id),
    UNIQUE (tenant_id, email)
);

CREATE TABLE plans (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    version BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL CHECK (btrim(name) <> ''),
    monthly_amount NUMERIC(12,2) NOT NULL CHECK (monthly_amount > 0),
    active BOOLEAN NOT NULL,
    UNIQUE (tenant_id, id)
);

CREATE TABLE contracts (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    version BIGINT NOT NULL,
    student_id UUID NOT NULL,
    plan_id UUID NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    amount_snapshot NUMERIC(12,2) NOT NULL CHECK (amount_snapshot > 0),
    cancellation_reason VARCHAR(500),
    CHECK (end_date IS NULL OR end_date >= start_date),
    UNIQUE (tenant_id, id),
    FOREIGN KEY (tenant_id, student_id) REFERENCES students(tenant_id, id),
    FOREIGN KEY (tenant_id, plan_id) REFERENCES plans(tenant_id, id)
);
CREATE INDEX contracts_student_dates ON contracts (tenant_id, student_id, start_date, end_date);

CREATE TABLE contract_acceptances (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    version BIGINT NOT NULL,
    contract_id UUID NOT NULL UNIQUE,
    terms_version VARCHAR(40) NOT NULL,
    terms_text TEXT NOT NULL,
    accepted_name VARCHAR(120) NOT NULL CHECK (btrim(accepted_name) <> ''),
    recorded_by VARCHAR(120) NOT NULL CHECK (btrim(recorded_by) <> ''),
    occurred_at TIMESTAMPTZ NOT NULL,
    FOREIGN KEY (tenant_id, contract_id) REFERENCES contracts(tenant_id, id)
);
CREATE INDEX acceptances_tenant ON contract_acceptances (tenant_id);

CREATE TABLE invoices (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    version BIGINT NOT NULL,
    contract_id UUID NOT NULL,
    competence DATE NOT NULL CHECK (extract(day FROM competence) = 1),
    due_date DATE NOT NULL,
    amount NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    status VARCHAR(16) NOT NULL CHECK (status IN ('OPEN', 'PAID', 'VOID')),
    void_reason VARCHAR(500),
    CHECK (date_trunc('month', due_date)::date = competence),
    CHECK ((status = 'VOID' AND nullif(btrim(void_reason), '') IS NOT NULL) OR (status <> 'VOID' AND void_reason IS NULL)),
    UNIQUE (tenant_id, id),
    UNIQUE (tenant_id, contract_id, competence),
    FOREIGN KEY (tenant_id, contract_id) REFERENCES contracts(tenant_id, id)
);
CREATE INDEX invoices_open_due ON invoices (tenant_id, due_date) WHERE status = 'OPEN';

CREATE TABLE payments (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    version BIGINT NOT NULL,
    invoice_id UUID NOT NULL,
    amount NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    method VARCHAR(24) NOT NULL CHECK (method IN ('CASH', 'EXTERNAL_PIX', 'EXTERNAL_TRANSFER')),
    reference VARCHAR(200),
    recorded_by VARCHAR(120) NOT NULL CHECK (btrim(recorded_by) <> ''),
    confirmed_at TIMESTAMPTZ NOT NULL,
    reversed_at TIMESTAMPTZ,
    reversed_by VARCHAR(120),
    reversal_reason VARCHAR(500),
    CHECK ((reversed_at IS NULL AND reversed_by IS NULL AND reversal_reason IS NULL)
        OR (reversed_at IS NOT NULL AND nullif(btrim(reversed_by), '') IS NOT NULL
            AND nullif(btrim(reversal_reason), '') IS NOT NULL AND reversed_at >= confirmed_at)),
    FOREIGN KEY (tenant_id, invoice_id) REFERENCES invoices(tenant_id, id)
);
CREATE UNIQUE INDEX payments_one_current ON payments (tenant_id, invoice_id) WHERE reversed_at IS NULL;

CREATE TABLE access_attempts (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    version BIGINT NOT NULL,
    student_id UUID NOT NULL,
    recorded_by VARCHAR(120) NOT NULL CHECK (btrim(recorded_by) <> ''),
    occurred_at TIMESTAMPTZ NOT NULL,
    decision VARCHAR(16) NOT NULL CHECK (decision IN ('ALLOWED', 'DENIED', 'UNAVAILABLE')),
    reason VARCHAR(40) NOT NULL CHECK (reason IN ('ELIGIBLE', 'TENANT_INACTIVE', 'STUDENT_INACTIVE', 'NO_CURRENT_CONTRACT', 'OVERDUE_INVOICE', 'BILLING_SYNC_PENDING')),
    CHECK ((decision = 'ALLOWED' AND reason = 'ELIGIBLE') OR
        (decision = 'UNAVAILABLE' AND reason = 'BILLING_SYNC_PENDING') OR
        (decision = 'DENIED' AND reason IN ('TENANT_INACTIVE', 'STUDENT_INACTIVE', 'NO_CURRENT_CONTRACT', 'OVERDUE_INVOICE'))),
    FOREIGN KEY (tenant_id, student_id) REFERENCES students(tenant_id, id)
);
CREATE INDEX access_student_time ON access_attempts (tenant_id, student_id, occurred_at);
