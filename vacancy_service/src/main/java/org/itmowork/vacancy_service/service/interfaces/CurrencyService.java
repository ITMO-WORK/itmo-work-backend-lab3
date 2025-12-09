package org.itmowork.vacancy_service.service.interfaces;

import org.itmowork.vacancy_service.model.Currency;

public interface CurrencyService {
    Currency findCurrencyById(Long currencyId);
}
