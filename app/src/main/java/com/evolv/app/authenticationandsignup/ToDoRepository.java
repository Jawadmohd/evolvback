package com.evolv.app.authenticationandsignup;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface ToDoRepository extends MongoRepository<Todo, String> {
    List<Todo> findByUsername(String username);
    Optional<Todo> findByIdAndUsername(String id, String username);
}
