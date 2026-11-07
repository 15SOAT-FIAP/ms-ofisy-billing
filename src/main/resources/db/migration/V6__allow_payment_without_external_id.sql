-- A cobrança nasce PENDING quando a OS é finalizada (ServiceOrderFinished). O external_id e o
-- checkout_url só existirão quando a integração com o provedor de pagamento criar a cobrança lá;
-- o UNIQUE continua valendo (o Postgres aceita vários NULL em uma coluna UNIQUE).
ALTER TABLE payments ALTER COLUMN external_id DROP NOT NULL;
