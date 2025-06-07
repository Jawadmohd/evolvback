package com.evolv.app.authenticationandsignup;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

/**
 * We assume only one Quiz document exists per profession.
 *   findByProfession(...) → returns that single Quiz (if it exists).
 */
public interface QuizRepository extends MongoRepository<Quiz, String> {
    Optional<Quiz> findByProfession(String profession);
}
