package org.itmowork.vacancy_service.mappers;

import org.itmowork.vacancy_service.dto.request.VacancyUpdateRequestDto;
import org.itmowork.vacancy_service.model.Currency;
import org.itmowork.vacancy_service.model.Vacancy;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface VacancyMapper {

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "currencyId", target = "currency", qualifiedByName = "mapCurrencyIdToCurrency")
    void update(@MappingTarget Vacancy vacancy, VacancyUpdateRequestDto vacancyUpdateRequestDto);

    @Named("mapCurrencyIdToCurrency")
    default Currency mapCurrencyIdToCurrency(Long currencyId) {
        if (currencyId == null) return null;
        Currency currency = new Currency();
        currency.setId(currencyId);
        return currency;
    }
}
