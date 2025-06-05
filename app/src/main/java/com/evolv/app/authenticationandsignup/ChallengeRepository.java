package com.evolv.app.authenticationandsignup;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface ChallengeRepository extends MongoRepository<Challenge, String> {
    // Existing method (you can keep it if you need to query by both username and challenge name)
    Optional<Challenge> findByUsernameAndChallenge(String username, String challenge);

    // New method to retrieve all Challenge documents for a given username
    List<Challenge> findByUsername(String username);
}
