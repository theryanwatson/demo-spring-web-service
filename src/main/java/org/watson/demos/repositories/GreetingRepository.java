package org.watson.demos.repositories;

import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.cloud.sleuth.annotation.SpanTag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.watson.demos.models.Greeting;

import java.util.Locale;
import java.util.UUID;

@Repository
public interface GreetingRepository extends PagingAndSortingRepository<Greeting, UUID> {
    @NewSpan
    Page<Greeting> findAllByLocale(@SpanTag("locale") Locale locale, Pageable pageable);
}
