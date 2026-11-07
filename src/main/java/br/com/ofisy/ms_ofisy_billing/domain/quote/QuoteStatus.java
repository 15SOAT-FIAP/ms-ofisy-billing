package br.com.ofisy.ms_ofisy_billing.domain.quote;

/**
 * Ciclo de vida do orçamento, dirigido pelos eventos da saga (ADR-0012):
 *
 * <pre>
 * PENDING ──StockConsumed──▶ AWAITING_APPROVAL ──▶ APPROVED
 *    │                              │   └────────▶ REPROVED
 *    └─StockConsumptionFailed─┐     │
 *                             ▼     ▼
 *                        CANCELLED (também a partir de APPROVED, via StockRefunded)
 * </pre>
 *
 * <ul>
 *   <li>{@code PENDING}: criado, aguardando o ms-ofisy-stock dar baixa nos itens
 *   ({@code QuoteCreated} publicado);</li>
 *   <li>{@code AWAITING_APPROVAL}: estoque consumido, aguardando a decisão do cliente
 *   ({@code QuoteSentForApproval} publicado);</li>
 *   <li>{@code APPROVED} / {@code REPROVED}: decisão do cliente;</li>
 *   <li>{@code CANCELLED}: compensação da saga (falta de estoque ou OS cancelada).</li>
 * </ul>
 */
public enum QuoteStatus {
    PENDING,
    AWAITING_APPROVAL,
    APPROVED,
    REPROVED,
    CANCELLED
}
