package com.evolv.app.authenticationandsignup;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CountRepository extends MongoRepository<Count, String> {
    Optional<Count> findByUsername(String username);
}
