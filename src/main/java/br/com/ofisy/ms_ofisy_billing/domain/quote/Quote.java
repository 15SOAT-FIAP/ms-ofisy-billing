package br.com.ofisy.ms_ofisy_billing.domain.quote;

import br.com.ofisy.ms_ofisy_billing.domain.quote.exceptions.InvalidQuoteDataException;
import br.com.ofisy.ms_ofisy_billing.domain.quote.exceptions.InvalidQuoteStatusException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Quote {

    public static final String ACTION_APPROVE = "aprovar";
    public static final String ACTION_REPROVE = "reprovar";
    public static final String ACTION_EDIT = "editar";
    public static final String ACTION_SEND_FOR_APPROVAL = "enviar para aprovação";
    public static final String ACTION_CANCEL = "cancelar";

    private UUID id;
    private UUID serviceOrderId;
    private QuoteStatus status;
    private BigDecimal totalPrice;
    private List<QuoteStockItem> stockItems = new ArrayList<>();
    private List<QuoteServiceItem> serviceItems = new ArrayList<>();
    private String quoteRefusalReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Quote create(UUID serviceOrderId,
                                List<QuoteStockItem> stockItems,
                                List<QuoteServiceItem> serviceItems) {
        Quote quote = new Quote();
        quote.serviceOrderId = serviceOrderId;
        quote.stockItems = stockItems != null ? stockItems : new ArrayList<>();
        quote.serviceItems = serviceItems != null ? serviceItems : new ArrayList<>();

        if (quote.stockItems.isEmpty() && quote.serviceItems.isEmpty()) {
            throw new InvalidQuoteDataException(serviceOrderId);
        }

        // Sem itens de estoque não há baixa a aguardar: o orçamento já nasce pronto para aprovação
        quote.status = quote.requiresStockConsumption() ? QuoteStatus.PENDING : QuoteStatus.AWAITING_APPROVAL;
        quote.totalPrice = calculateTotal(quote.stockItems, quote.serviceItems);
        quote.createdAt = LocalDateTime.now();
        quote.updatedAt = LocalDateTime.now();
        quote.stockItems.forEach(item -> item.setQuote(quote));
        quote.serviceItems.forEach(item -> item.setQuote(quote));
        return quote;
    }

    public static Quote reconstruct(UUID id,
                                     UUID serviceOrderId,
                                     QuoteStatus status,
                                     BigDecimal totalPrice,
                                     String quoteRefusalReason,
                                     List<QuoteStockItem> stockItems,
                                     List<QuoteServiceItem> serviceItems,
                                     LocalDateTime createdAt,
                                     LocalDateTime updatedAt) {
        Quote quote = new Quote();
        quote.id = id;
        quote.serviceOrderId = serviceOrderId;
        quote.status = status;
        quote.totalPrice = totalPrice;
        quote.quoteRefusalReason = quoteRefusalReason;
        quote.stockItems = stockItems != null ? stockItems : new ArrayList<>();
        quote.serviceItems = serviceItems != null ? serviceItems : new ArrayList<>();
        quote.createdAt = createdAt;
        quote.updatedAt = updatedAt;
        return quote;
    }

    public boolean requiresStockConsumption() {
        return !stockItems.isEmpty();
    }

    /** Estoque consumido (StockConsumed): o orçamento segue para a decisão do cliente. */
    public void sendForApproval() {
        if (!QuoteStatus.PENDING.equals(this.status)) {
            throw new InvalidQuoteStatusException(ACTION_SEND_FOR_APPROVAL, this.status);
        }
        this.status = QuoteStatus.AWAITING_APPROVAL;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isApproved() {
        return QuoteStatus.APPROVED.equals(this.status);
    }

    /** REPROVED e CANCELLED são finais: não há mais o que compensar nesses orçamentos. */
    public boolean isCancellable() {
        return QuoteStatus.PENDING.equals(this.status)
                || QuoteStatus.AWAITING_APPROVAL.equals(this.status)
                || QuoteStatus.APPROVED.equals(this.status);
    }

    /** Compensação da saga: falta de estoque (PENDING) ou OS cancelada depois do orçamento. */
    public void cancel() {
        if (!isCancellable()) {
            throw new InvalidQuoteStatusException(ACTION_CANCEL, this.status);
        }
        this.status = QuoteStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    public void approve() {
        if (!QuoteStatus.AWAITING_APPROVAL.equals(this.status)) {
            throw new InvalidQuoteStatusException(ACTION_APPROVE, this.status);
        }
        this.status = QuoteStatus.APPROVED;
        this.updatedAt = LocalDateTime.now();
    }

    public void reprove(String reason) {
        if (!QuoteStatus.AWAITING_APPROVAL.equals(this.status)) {
            throw new InvalidQuoteStatusException(ACTION_REPROVE, this.status);
        }
        this.status = QuoteStatus.REPROVED;
        this.quoteRefusalReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    public void update(List<QuoteStockItem> newStockItems, List<QuoteServiceItem> newServiceItems) {
        if (!QuoteStatus.PENDING.equals(this.status)) {
            throw new InvalidQuoteStatusException(ACTION_EDIT, this.status);
        }
        if ((newStockItems == null || newStockItems.isEmpty())
                && (newServiceItems == null || newServiceItems.isEmpty())) {
            throw new InvalidQuoteDataException(this.serviceOrderId);
        }
        this.stockItems = newStockItems != null ? newStockItems : new ArrayList<>();
        this.serviceItems = newServiceItems != null ? newServiceItems : new ArrayList<>();
        this.totalPrice = calculateTotal(this.stockItems, this.serviceItems);
        this.stockItems.forEach(item -> item.setQuote(this));
        this.serviceItems.forEach(item -> item.setQuote(this));
        this.updatedAt = LocalDateTime.now();
    }

    private static BigDecimal calculateTotal(List<QuoteStockItem> stockItems,
                                               List<QuoteServiceItem> serviceItems) {
        BigDecimal stockTotal = stockItems.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal serviceTotal = serviceItems.stream()
                .map(QuoteServiceItem::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return stockTotal.add(serviceTotal);
    }
}
