package com.itmowork.company_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table(name = "user_companies")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserCompany {

    @Id
    private Long id;

    @Column("user_id")
    private UUID userId;

    @Column("company_id")
    private UUID companyId;

}
