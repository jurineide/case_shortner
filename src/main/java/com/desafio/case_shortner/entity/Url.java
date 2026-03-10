package com.desafio.case_shortner.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data

@Entity
@Table(name = "urls")
public class Url {
    @Id
    private String id;

    @Column(unique = true, nullable = false)
    private String originalUrl;

    @Column(unique = true, nullable = false)
    private String shortUrl;

    @Column(nullable = false)
    private Instant created_date;

    @Column
    private Instant expiration_date;

    @Column(nullable = false)
    private Integer clicks = 0;

}
