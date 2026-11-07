package br.com.ofisy.ms_ofisy_billing.adapters.gateways.quote;

import br.com.ofisy.ms_ofisy_billing.domain.quote.Quote;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class QuoteRepositoryImpl implements QuoteRepository {

    private final JpaQuoteRepository jpaQuoteRepository;

    @Override
    public Quote save(Quote quote) {
        QuoteEntity saved = jpaQuoteRepository.save(QuoteMapper.toEntity(quote));
        return QuoteMapper.toDomain(saved);
    }

    @Override
    public Optional<Quote> findById(UUID id) {
        return jpaQuoteRepository.findById(id).map(QuoteMapper::toDomain);
    }

    @Override
    public List<Quote> findByServiceOrderId(UUID serviceOrderId) {
        return jpaQuoteRepository.findByServiceOrderId(serviceOrderId).stream()
                .map(QuoteMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByServiceOrderId(UUID serviceOrderId) {
        return jpaQuoteRepository.existsByServiceOrderId(serviceOrderId);
    }
}
