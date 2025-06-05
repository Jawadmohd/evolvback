package com.evolv.app.authenticationandsignup;

// QuoteRepository.java (Repository)

import com.evolv.app.authenticationandsignup.Quote;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface QuoteRepository extends MongoRepository<Quote, String> {
    List<Quote> findAll();
}