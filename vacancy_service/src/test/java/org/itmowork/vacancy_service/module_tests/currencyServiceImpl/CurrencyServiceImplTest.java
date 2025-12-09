package org.itmowork.vacancy_service.module_tests.currencyServiceImpl;

import org.itmowork.vacancy_service.exception.exceptions.CurrencyNotFoundException;
import org.itmowork.vacancy_service.model.Currency;
import org.itmowork.vacancy_service.repository.CurrencyRepository;
import org.itmowork.vacancy_service.service.CurrencyServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class CurrencyServiceImplTest {

    @Mock
    private CurrencyRepository currencyRepository;

    @InjectMocks
    private CurrencyServiceImpl currencyService;

    @Test
    void findCurrencyByIdShouldReturnCurrency() {
        Long id = 1L;
        Currency currency = Currency.builder()
                .id(id)
                .currency("USD")
                .build();

        Mockito.when(currencyRepository.findById(id))
                .thenReturn(Optional.of(currency));

        Currency result = currencyService.findCurrencyById(id);

        Assertions.assertEquals(id, result.getId());
        Assertions.assertEquals("USD", result.getCurrency());
        Mockito.verify(currencyRepository).findById(id);
    }

    @Test
    void findCurrencyByIdShouldThrowWhenNotFound() {
        Long id = 4L;

        Mockito.when(currencyRepository.findById(id))
                .thenReturn(Optional.empty());

        Assertions.assertThrows(
                CurrencyNotFoundException.class,
                () -> currencyService.findCurrencyById(id)
        );

        Mockito.verify(currencyRepository).findById(id);
    }
}
