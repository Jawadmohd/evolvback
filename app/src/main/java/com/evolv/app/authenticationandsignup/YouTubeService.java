package com.evolv.app.authenticationandsignup;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import com.fasterxml.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class YouTubeService {

    @Value("${youtube.api.keys}")
    private String[] apiKeys;
    private int currentKeyIndex = 0;

    private final RestTemplate restTemplate = new RestTemplate();

    // Limit search to 2 results per interest
    private static final int MAX_RESULTS = 5;
    private static final int MAX_RETRIES = 3;

    @Cacheable(value = "videos", key = "#interest")
    public List<Map<String, String>> getVideosForInterest(String interest) {
        int retryCount = 0;
        while (retryCount < MAX_RETRIES) {
            try {
                String currentKey = getNextApiKey();
                String searchUrl = buildSearchUrl(interest, currentKey);

                ResponseEntity<JsonNode> searchResponse =
                    restTemplate.getForEntity(searchUrl, JsonNode.class);

                if (searchResponse.getStatusCode() == HttpStatus.OK 
                        && searchResponse.getBody() != null) {
                    return processResponse(searchResponse.getBody());
                }
            } catch (HttpClientErrorException e) {
                handleApiError(e);
                if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                    retryCount++;
                    continue;
                }
                break;
            } catch (Exception e) {
                break;
            }
        }
        return List.of(); // empty on failure
    }

    private String getNextApiKey() {
        currentKeyIndex = (currentKeyIndex + 1) % apiKeys.length;
        return apiKeys[currentKeyIndex];
    }

    private String buildSearchUrl(String interest, String apiKey) {
        return "https://www.googleapis.com/youtube/v3/search?" +
            "part=snippet&maxResults=" + MAX_RESULTS +
            "&q=" + UriUtils.encodeQueryParam(interest, StandardCharsets.UTF_8) +
            "&type=video&videoDuration=medium" +
            "&fields=items(id/videoId,snippet/title,snippet/thumbnails/high/url)" +
            "&key=" + apiKey;
    }

    private List<Map<String, String>> processResponse(JsonNode body) {
        List<Map<String, String>> videos = new ArrayList<>();
        for (JsonNode item : body.get("items")) {
            String title = item.get("snippet").get("title").asText();

            // only ASCII (basic English letters, numbers, spaces, punctuation)
            if (!title.matches("[\\p{ASCII} ]+")) {
                continue;
            }

            Map<String, String> video = new HashMap<>();
            video.put("title", title);
            video.put("thumbnail",
                      item.get("snippet")
                          .get("thumbnails")
                          .get("high")
                          .get("url")
                          .asText());
            String vidId = item.get("id").get("videoId").asText();
            video.put("videoId", vidId);
            video.put("url", "https://www.youtube.com/watch?v=" + vidId);

            videos.add(video);

            // extra safety: stop if we've already got MAX_RESULTS after filtering
            if (videos.size() >= MAX_RESULTS) {
                break;
            }
        }
        return videos;
    }

    private void handleApiError(HttpClientErrorException e) {
        if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
            System.err.println("YouTube API quota exceeded on key #" + currentKeyIndex);
            // alerting, metrics, whatever
        }
    }
}
