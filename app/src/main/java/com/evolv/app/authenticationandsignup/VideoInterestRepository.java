package com.evolv.app.authenticationandsignup;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface VideoInterestRepository extends MongoRepository<VideoInterest, String>
 {
    List<VideoInterest> findAll();
    List<VideoInterest> findByUsername(String username);

}
