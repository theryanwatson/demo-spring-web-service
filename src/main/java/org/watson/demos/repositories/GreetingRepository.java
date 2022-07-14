package org.watson.demos.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.watson.demos.models.Greeting;

import java.util.Locale;
import java.util.UUID;

@Repository
//@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = Exception.class)
public interface GreetingRepository extends PagingAndSortingRepository<Greeting, UUID> {
    Page<Greeting> findAllByLocale(Locale locale, Pageable pageable);
}
