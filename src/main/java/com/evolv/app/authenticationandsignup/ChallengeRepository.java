package com.evolv.app.authenticationandsignup;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface ChallengeRepository extends MongoRepository<Challenge, String> {
    // Find a challenge by its owner and exact challenge text (used in applause logic)
    Optional<Challenge> findByUsernameAndChallenge(String username, String challenge);

    // Retrieve all challenges for a given owner (used to determine if there's an active one)
    List<Challenge> findByUsername(String username);

    // Find by ID (inherited from MongoRepository), but we can still refer to Optional<Challenge> findById(String id)
}
