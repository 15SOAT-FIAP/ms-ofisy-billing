CREATE TABLE quote_stock_items (
    id UUID PRIMARY KEY,
    quote_id UUID NOT NULL,
    stock_id UUID NOT NULL,
    unit_price NUMERIC(10, 2) NOT NULL,
    quantity INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_quote_stock_items_quote FOREIGN KEY (quote_id) REFERENCES quotes (id)
);

CREATE INDEX idx_quote_stock_items_quote_id ON quote_stock_items (quote_id);

CREATE TABLE quote_service_items (
    id UUID PRIMARY KEY,
    quote_id UUID NOT NULL,
    service_catalog_id UUID NOT NULL,
    price NUMERIC(10, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_quote_service_items_quote FOREIGN KEY (quote_id) REFERENCES quotes (id)
);

CREATE INDEX idx_quote_service_items_quote_id ON quote_service_items (quote_id);
