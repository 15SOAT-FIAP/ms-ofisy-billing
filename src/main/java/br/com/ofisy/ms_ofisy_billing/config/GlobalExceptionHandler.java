package br.com.ofisy.ms_ofisy_billing.config;

import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.QuoteAlreadyExistsException;
import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.QuoteItemAlreadyExistsException;
import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.QuoteNotFoundException;
import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.ServiceCatalogNotFoundException;
import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.StockNotFoundException;
import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.StockUnavailableException;
import br.com.ofisy.ms_ofisy_billing.domain.payment.exceptions.InvalidPaymentStatusException;
import br.com.ofisy.ms_ofisy_billing.domain.quote.exceptions.InvalidQuoteDataException;
import br.com.ofisy.ms_ofisy_billing.domain.quote.exceptions.InvalidQuoteItemException;
import br.com.ofisy.ms_ofisy_billing.domain.quote.exceptions.InvalidQuoteStatusException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler({QuoteNotFoundException.class, ServiceCatalogNotFoundException.class, StockNotFoundException.class})
    public ProblemDetail handleNotFound(RuntimeException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, ex, request);
    }

    @ExceptionHandler({
            InvalidQuoteDataException.class,
            InvalidQuoteItemException.class,
            InvalidPaymentStatusException.class,
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class
    })
    public ProblemDetail handleBadRequest(Exception ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, ex, request);
    }

    @ExceptionHandler({InvalidQuoteStatusException.class})
    public ProblemDetail handleUnprocessable(RuntimeException ex, HttpServletRequest request) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, ex, request);
    }

    @ExceptionHandler({QuoteAlreadyExistsException.class, QuoteItemAlreadyExistsException.class})
    public ProblemDetail handleConflict(RuntimeException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, ex, request);
    }

    @ExceptionHandler(StockUnavailableException.class)
    public ProblemDetail handleStockUnavailable(StockUnavailableException ex, HttpServletRequest request) {
        log.error("ms-ofisy-stock indisponível ao processar {} {}", request.getMethod(), request.getRequestURI(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Não foi possível consultar o estoque agora. Tente novamente em instantes.");
        problem.setInstance(java.net.URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler({AccessDeniedException.class, AuthenticationException.class})
    public void rethrowSecurityException(RuntimeException ex) {
        throw ex;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Erro inesperado ao processar {} {}", request.getMethod(), request.getRequestURI(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro inesperado.");
        problem.setInstance(java.net.URI.create(request.getRequestURI()));
        return problem;
    }

    private ProblemDetail problem(HttpStatus status, Exception ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problem.setInstance(java.net.URI.create(request.getRequestURI()));
        return problem;
    }
}
