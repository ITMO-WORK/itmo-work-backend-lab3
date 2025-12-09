package org.ilestegor.applicationservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Table(name = "applications")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application {

    @Id
    private UUID id;

    private String coverLetter;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Column("status_id")
    private Long status;

    @Column("vacancy_id")
    private UUID vacancyId;

    @Column("user_id")
    private UUID userId;

}
