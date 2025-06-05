package com.evolv.app.authenticationandsignup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ChallengeCleaner {
    private static final Logger logger = LoggerFactory.getLogger(ChallengeCleaner.class);

    @Autowired
    private ChallengeRepository challengeRepository;

    // Runs daily at 3 AM
    @Scheduled(cron = "0 0 3 * * ?")
    public void deleteExpiredChallenges() {
        logger.info("Starting challenge cleanup task...");

        List<Challenge> all = challengeRepository.findAll();
        int deletedCount = 0;

        for (Challenge ch : all) {
            try {
                if (isExpired(ch)) {
                    challengeRepository.delete(ch);
                    deletedCount++;
                    logger.debug("Deleted expired challenge ID: {}", ch.getId());
                }
            } catch (Exception e) {
                logger.error("Error deleting challenge {}: {}", ch.getId(), e.getMessage());
            }
        }

        logger.info("Cleanup completed. Deleted {} challenges.", deletedCount);
    }

    private boolean isExpired(Challenge challenge) {
        LocalDateTime created = challenge.getCreatedAt();
        String dur = Optional.ofNullable(challenge.getDuration()).orElse("").trim();

        if (created == null || dur.isEmpty()) {
            logger.warn("Skipping challenge {}: missing createdAt or duration", challenge.getId());
            return false;
        }

        LocalDateTime expires = computeExpiration(created, dur);
        return expires != null && expires.isBefore(LocalDateTime.now());
    }

    private LocalDateTime computeExpiration(LocalDateTime created, String duration) {
        // Normalize e.g. "One Month", "3 WEEKS"
        String d = duration.toLowerCase();

        // Word → number map for one…ten
        Map<String, Integer> wordNums = Map.of(
            "one",1,"two",2,"three",3,"four",4,"five",5,
            "six",6,"seven",7,"eight",8,"nine",9,"ten",10
        );

        // Try to extract number + unit
        Pattern p = Pattern.compile("(\\d+|[a-z]+)\\s+(day|days|week|weeks|month|months|year|years)");
        Matcher m = p.matcher(d);
        if (!m.find()) {
            logger.warn("Cannot parse duration '{}' for challenge created at {}", duration, created);
            return null;
        }

        String numToken = m.group(1);
        String unit = m.group(2);

        int n;
        if (numToken.matches("\\d+")) {
            n = Integer.parseInt(numToken);
        } else {
            n = wordNums.getOrDefault(numToken, -1);
            if (n < 0) {
                logger.warn("Unknown number word '{}' in duration '{}'", numToken, duration);
                return null;
            }
        }

        // Add accordingly
        switch (unit) {
            case "day":
            case "days":
                return created.plus(n, ChronoUnit.DAYS);
            case "week":
            case "weeks":
                return created.plus(n * 7L, ChronoUnit.DAYS);
            case "month":
            case "months":
                return created.plus(n, ChronoUnit.MONTHS);
            case "year":
            case "years":
                return created.plus(n, ChronoUnit.YEARS);
            default:
                logger.warn("Unhandled unit '{}' in duration '{}'", unit, duration);
                return null;
        }
    }
}
