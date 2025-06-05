package com.evolv.app.authenticationandsignup;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/videos")
public class VideoController {

    @Autowired
    private VideoInterestRepository interestRepo;

    @Autowired
    private YouTubeService youTubeService;

    @GetMapping("/all")
    public ResponseEntity<List<Map<String, String>>> getVideosByUsername(
            @RequestParam String username) {

        // 1) fetch all interests
        List<VideoInterest> interests = interestRepo.findByUsername(username);

        // 2) for each interest, call the service and flat-map into one big stream
        List<Map<String, String>> combined = interests.stream()
            .flatMap(vi ->
                youTubeService
                  .getVideosForInterest(vi.getInterest())
                  .stream()
            )
            // 3) English-only: ASCII titles
            .filter(m -> m.getOrDefault("title", "")
                          .matches("[\\p{ASCII} ]+"))
            // 4) drop duplicates (by videoId)
            .filter(new java.util.HashSet<>()::add) 
            // 5) limit to 2 results total
            .limit(5)
            .collect(Collectors.toList());

        return ResponseEntity.ok(combined);
    }
}
