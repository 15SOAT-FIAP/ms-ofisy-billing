package br.com.ofisy.ms_ofisy_billing.application.quote.reprove;

import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.QuoteNotFoundException;
import br.com.ofisy.ms_ofisy_billing.domain.quote.Quote;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReproveQuoteService implements ReproveQuoteUseCase {

    private final QuoteRepository quoteRepository;

    @Override
    @Transactional
    public Quote execute(ReproveQuoteCommand cmd) {
        Quote quote = quoteRepository.findById(cmd.id()).orElseThrow(() -> new QuoteNotFoundException(cmd.id()));
        quote.reprove(cmd.reason());
        Quote saved = quoteRepository.save(quote);

        return saved;
    }
}
