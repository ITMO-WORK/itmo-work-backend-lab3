package com.itmowork.company_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "company_status")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompanyStatus {

    @Id
    private Long id;

    private CompanyStatusName status;

}
