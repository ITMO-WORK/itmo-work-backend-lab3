package org.itmowork.vacancy_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "vacancy_status")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VacancyStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, unique = true)
    private VacancyStatusName vacancyStatusName;
}
