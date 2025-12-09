package org.itmowork.vacancy_service.service;

import lombok.AllArgsConstructor;
import org.itmowork.vacancy_service.exception.exceptions.CurrencyNotFoundException;
import org.itmowork.vacancy_service.model.Currency;
import org.itmowork.vacancy_service.repository.CurrencyRepository;
import org.itmowork.vacancy_service.service.interfaces.CurrencyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class CurrencyServiceImpl implements CurrencyService {
    private final CurrencyRepository currencyRepository;

    @Override
    @Transactional
    public Currency findCurrencyById(Long currencyId) {
        return currencyRepository.findById(currencyId)
                .orElseThrow(() -> new CurrencyNotFoundException("Currency not found"));
    }
}
