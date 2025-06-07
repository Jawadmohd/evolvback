package com.evolv.app.authenticationandsignup;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ToDoRepository extends MongoRepository<Todo, String> {
    List<Todo> findByUsername(String username);
    List<Todo> findByUsernameAndTagsContaining(String username, String tag);
    List<Todo> findByUsernameAndCompleted(String username, boolean completed);
    List<Todo> findByUsernameAndTagsContainingAndCompleted(String username, String tag, boolean completed);
    java.util.Optional<Todo> findByIdAndUsername(String id, String username);
}
