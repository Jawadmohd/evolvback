package com.evolv.app.authenticationandsignup;


import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface QuizRepository extends MongoRepository<Quiz, String> {
    List<Quiz> findByProfession(String profession);
}
