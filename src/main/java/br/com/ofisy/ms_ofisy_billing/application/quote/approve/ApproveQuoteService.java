package br.com.ofisy.ms_ofisy_billing.application.quote.approve;

import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.QuoteNotFoundException;
import br.com.ofisy.ms_ofisy_billing.domain.quote.Quote;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApproveQuoteService implements ApproveQuoteUseCase {

    private final QuoteRepository quoteRepository;

    @Override
    @Transactional
    public Quote execute(UUID id) {
        Quote quote = quoteRepository.findById(id).orElseThrow(() -> new QuoteNotFoundException(id));
        quote.approve();
        return quoteRepository.save(quote);
    }
}
