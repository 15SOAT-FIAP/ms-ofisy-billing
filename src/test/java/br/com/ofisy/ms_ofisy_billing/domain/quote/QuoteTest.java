package br.com.ofisy.ms_ofisy_billing.domain.quote;

import br.com.ofisy.ms_ofisy_billing.domain.quote.exceptions.InvalidQuoteDataException;
import br.com.ofisy.ms_ofisy_billing.domain.quote.exceptions.InvalidQuoteStatusException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuoteTest {

    private QuoteStockItem stockItem() {
        return QuoteStockItem.create(UUID.randomUUID(), new BigDecimal("50.00"), 2);
    }

    private QuoteServiceItem serviceItem() {
        return QuoteServiceItem.create(UUID.randomUUID(), new BigDecimal("100.00"));
    }

    @Test
    void shouldCreateQuoteWithPendingStatusAndCalculatedTotal() {
        Quote quote = Quote.create(UUID.randomUUID(), List.of(stockItem()), List.of(serviceItem()));

        assertThat(quote.getStatus()).isEqualTo(QuoteStatus.PENDING);
        assertThat(quote.getTotalPrice()).isEqualByComparingTo("200.00");
        assertThat(quote.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldLinkItemsBackToTheQuoteOnCreate() {
        QuoteStockItem stockItem = stockItem();
        Quote quote = Quote.create(UUID.randomUUID(), List.of(stockItem), List.of());

        assertThat(quote.getStockItems().get(0).getQuote()).isSameAs(quote);
    }

    @Test
    void shouldRejectCreationWithoutAnyItem() {
        UUID serviceOrderId = UUID.randomUUID();

        assertThatThrownBy(() -> Quote.create(serviceOrderId, List.of(), List.of()))
                .isInstanceOf(InvalidQuoteDataException.class);
    }

    @Test
    void shouldApprovePendingQuote() {
        Quote quote = Quote.create(UUID.randomUUID(), List.of(stockItem()), List.of());

        quote.approve();

        assertThat(quote.getStatus()).isEqualTo(QuoteStatus.APPROVED);
    }

    @Test
    void shouldNotApproveAnAlreadyApprovedQuote() {
        Quote quote = Quote.create(UUID.randomUUID(), List.of(stockItem()), List.of());
        quote.approve();

        assertThatThrownBy(quote::approve).isInstanceOf(InvalidQuoteStatusException.class);
    }

    @Test
    void shouldReprovePendingQuoteWithReason() {
        Quote quote = Quote.create(UUID.randomUUID(), List.of(stockItem()), List.of());

        quote.reprove("Preço muito alto");

        assertThat(quote.getStatus()).isEqualTo(QuoteStatus.REPROVED);
        assertThat(quote.getQuoteRefusalReason()).isEqualTo("Preço muito alto");
    }

    @Test
    void shouldNotReproveAnAlreadyReprovedQuote() {
        Quote quote = Quote.create(UUID.randomUUID(), List.of(stockItem()), List.of());
        quote.reprove("motivo");

        assertThatThrownBy(() -> quote.reprove("outro motivo"))
                .isInstanceOf(InvalidQuoteStatusException.class);
    }

    @Test
    void shouldUpdateItemsAndRecalculateTotalWhilePending() {
        Quote quote = Quote.create(UUID.randomUUID(), List.of(stockItem()), List.of());

        quote.update(List.of(), List.of(serviceItem()));

        assertThat(quote.getTotalPrice()).isEqualByComparingTo("100.00");
        assertThat(quote.getServiceItems()).hasSize(1);
    }

    @Test
    void shouldNotUpdateAnApprovedQuote() {
        Quote quote = Quote.create(UUID.randomUUID(), List.of(stockItem()), List.of());
        quote.approve();

        assertThatThrownBy(() -> quote.update(List.of(stockItem()), List.of()))
                .isInstanceOf(InvalidQuoteStatusException.class);
    }

    @Test
    void shouldNotUpdateToHaveNoItems() {
        Quote quote = Quote.create(UUID.randomUUID(), List.of(stockItem()), List.of());

        assertThatThrownBy(() -> quote.update(List.of(), List.of()))
                .isInstanceOf(InvalidQuoteDataException.class);
    }

    @Test
    void shouldReconstructWithoutRecalculatingTotal() {
        UUID id = UUID.randomUUID();
        UUID serviceOrderId = UUID.randomUUID();

        Quote quote = Quote.reconstruct(id, serviceOrderId, QuoteStatus.APPROVED, new BigDecimal("999.00"),
                null, List.of(), List.of(), null, null);

        assertThat(quote.getId()).isEqualTo(id);
        assertThat(quote.getTotalPrice()).isEqualByComparingTo("999.00");
        assertThat(quote.getStatus()).isEqualTo(QuoteStatus.APPROVED);
    }

    @Test
    void shouldCreateWithOnlyServiceItemsWhenStockItemsAreNull() {
        Quote quote = Quote.create(UUID.randomUUID(), null, List.of(serviceItem()));

        assertThat(quote.getStockItems()).isEmpty();
        assertThat(quote.getServiceItems()).hasSize(1);
        assertThat(quote.getTotalPrice()).isEqualByComparingTo("100.00");
    }

    @Test
    void shouldCreateWithOnlyStockItemsWhenServiceItemsAreNull() {
        Quote quote = Quote.create(UUID.randomUUID(), List.of(stockItem()), null);

        assertThat(quote.getServiceItems()).isEmpty();
        assertThat(quote.getStockItems()).hasSize(1);
        assertThat(quote.getTotalPrice()).isEqualByComparingTo("100.00");
    }

    @Test
    void shouldRejectCreationWhenBothItemListsAreNull() {
        UUID serviceOrderId = UUID.randomUUID();

        assertThatThrownBy(() -> Quote.create(serviceOrderId, null, null))
                .isInstanceOf(InvalidQuoteDataException.class)
                .hasMessageContaining(serviceOrderId.toString());
    }

    @Test
    void shouldSumStockAndServiceItemsInTheTotal() {
        Quote quote = Quote.create(UUID.randomUUID(),
                List.of(stockItem(), stockItem()),
                List.of(serviceItem(), serviceItem()));

        assertThat(quote.getTotalPrice()).isEqualByComparingTo("400.00");
    }

    @Test
    void shouldNotApproveAReprovedQuote() {
        Quote quote = Quote.create(UUID.randomUUID(), List.of(stockItem()), List.of());
        quote.reprove("motivo");

        assertThatThrownBy(quote::approve)
                .isInstanceOf(InvalidQuoteStatusException.class)
                .hasMessageContaining("aprovar")
                .hasMessageContaining("REPROVED");
    }

    @Test
    void shouldNotReproveAnApprovedQuote() {
        Quote quote = Quote.create(UUID.randomUUID(), List.of(stockItem()), List.of());
        quote.approve();

        assertThatThrownBy(() -> quote.reprove("motivo"))
                .isInstanceOf(InvalidQuoteStatusException.class)
                .hasMessageContaining("reprovar")
                .hasMessageContaining("APPROVED");
    }

    @Test
    void shouldNotUpdateAReprovedQuote() {
        Quote quote = Quote.create(UUID.randomUUID(), List.of(stockItem()), List.of());
        quote.reprove("motivo");

        assertThatThrownBy(() -> quote.update(List.of(stockItem()), List.of()))
                .isInstanceOf(InvalidQuoteStatusException.class)
                .hasMessageContaining("editar");
    }

    @Test
    void shouldUpdateWithOnlyStockItemsWhenServiceItemsAreNull() {
        Quote quote = Quote.create(UUID.randomUUID(), List.of(), List.of(serviceItem()));

        quote.update(List.of(stockItem()), null);

        assertThat(quote.getServiceItems()).isEmpty();
        assertThat(quote.getStockItems()).hasSize(1);
        assertThat(quote.getTotalPrice()).isEqualByComparingTo("100.00");
    }

    @Test
    void shouldUpdateWithOnlyServiceItemsWhenStockItemsAreNull() {
        Quote quote = Quote.create(UUID.randomUUID(), List.of(stockItem()), List.of());

        quote.update(null, List.of(serviceItem()));

        assertThat(quote.getStockItems()).isEmpty();
        assertThat(quote.getServiceItems()).hasSize(1);
        assertThat(quote.getTotalPrice()).isEqualByComparingTo("100.00");
    }

    @Test
    void shouldNotUpdateWhenBothItemListsAreNull() {
        Quote quote = Quote.create(UUID.randomUUID(), List.of(stockItem()), List.of());

        assertThatThrownBy(() -> quote.update(null, null))
                .isInstanceOf(InvalidQuoteDataException.class);
    }

    @Test
    void shouldNotUpdateWhenStockItemsAreEmptyAndServiceItemsAreNull() {
        Quote quote = Quote.create(UUID.randomUUID(), List.of(stockItem()), List.of());

        assertThatThrownBy(() -> quote.update(List.of(), null))
                .isInstanceOf(InvalidQuoteDataException.class);
    }

    @Test
    void shouldNotUpdateWhenStockItemsAreNullAndServiceItemsAreEmpty() {
        Quote quote = Quote.create(UUID.randomUUID(), List.of(stockItem()), List.of());

        assertThatThrownBy(() -> quote.update(null, List.of()))
                .isInstanceOf(InvalidQuoteDataException.class);
    }

    @Test
    void shouldLinkItemsBackToTheQuoteOnUpdate() {
        Quote quote = Quote.create(UUID.randomUUID(), List.of(stockItem()), List.of());
        QuoteServiceItem newItem = serviceItem();

        quote.update(List.of(), List.of(newItem));

        assertThat(newItem.getQuote()).isSameAs(quote);
    }

    @Test
    void shouldReconstructWithEmptyListsWhenItemsAreNull() {
        Quote quote = Quote.reconstruct(UUID.randomUUID(), UUID.randomUUID(), QuoteStatus.PENDING,
                BigDecimal.TEN, null, null, null, null, null);

        assertThat(quote.getStockItems()).isEmpty();
        assertThat(quote.getServiceItems()).isEmpty();
    }

    @Test
    void shouldReconstructKeepingTheGivenItems() {
        QuoteStockItem stock = stockItem();
        QuoteServiceItem service = serviceItem();

        Quote quote = Quote.reconstruct(UUID.randomUUID(), UUID.randomUUID(), QuoteStatus.REPROVED,
                new BigDecimal("200.00"), "Preço alto", List.of(stock), List.of(service), null, null);

        assertThat(quote.getStockItems()).containsExactly(stock);
        assertThat(quote.getServiceItems()).containsExactly(service);
        assertThat(quote.getQuoteRefusalReason()).isEqualTo("Preço alto");
    }
}
