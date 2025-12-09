package org.ilestegor.applicationservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table(name = "application_status")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationStatus {
    @Id
    private Long id;

    @Column("status")
    private ApplicationStatusName applicationStatusName;
}
