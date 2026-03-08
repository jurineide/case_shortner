package com.desafio.case_shortner.repository;

import com.desafio.case_shortner.entity.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UrlRepository extends JpaRepository<Url, String> {
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Url url SET url.clicks = url.clicks + 1 WHERE url.id = :id")
    void incrementClicks(@Param("id") String id);
}
