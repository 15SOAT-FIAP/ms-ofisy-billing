CREATE TABLE quotes (
    id UUID PRIMARY KEY,
    service_order_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    total_price NUMERIC(10, 2) NOT NULL,
    quote_refusal_reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT uk_quotes_service_order_id UNIQUE (service_order_id)
);
