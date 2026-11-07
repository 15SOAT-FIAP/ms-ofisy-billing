CREATE TABLE payments (
    id UUID PRIMARY KEY,
    service_order_id UUID NOT NULL,
    quote_id UUID NOT NULL,
    amount NUMERIC(10, 2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    external_id VARCHAR(255) NOT NULL,
    checkout_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT uk_payments_external_id UNIQUE (external_id)
);

CREATE INDEX idx_payments_service_order_id ON payments (service_order_id);
