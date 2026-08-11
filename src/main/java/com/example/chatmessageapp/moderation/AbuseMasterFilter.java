package com.example.chatmessageapp.moderation;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Real-time Abuse Master filter using a Trie data structure.
 * Performs fast O(L) profanity detection and asterisk masking per word.
 */
@Component
public class AbuseMasterFilter {

    private static class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        boolean isEndOfWord = false;
    }

    private final TrieNode root = new TrieNode();

    public AbuseMasterFilter() {
        // Load default blacklist dictionary
        List<String> blacklist = Arrays.asList(
                "abuse", "badword", "stupid", "idiot", "hate",
                "scam", "spam", "fool", "crap", "garbage"
        );
        for (String word : blacklist) {
            insert(word.toLowerCase());
        }
    }

    /**
     * Inserts a word into the Trie.
     */
    public void insert(String word) {
        TrieNode current = root;
        for (char ch : word.toCharArray()) {
            current = current.children.computeIfAbsent(ch, c -> new TrieNode());
        }
        current.isEndOfWord = true;
    }

    /**
     * Checks if a word exists in the profanity Trie.
     */
    public boolean containsProfanity(String word) {
        String clean = word.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        if (clean.isEmpty()) return false;

        TrieNode current = root;
        for (char ch : clean.toCharArray()) {
            current = current.children.get(ch);
            if (current == null) {
                return false;
            }
        }
        return current.isEndOfWord;
    }

    /**
     * Filters a complete text message, replacing any profanity with asterisks.
     * Returns a ModerationResult indicating if any terms were masked.
     */
    public ModerationResult sanitize(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ModerationResult(text, false);
        }

        String[] words = text.split(" ");
        boolean wasMasked = false;
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (containsProfanity(word)) {
                wasMasked = true;
                String cleanWord = word.replaceAll("[^a-zA-Z0-9]", "");
                
                String maskedWord;
                if (cleanWord.length() <= 2) {
                    // If length is 2 or 1, mask completely
                    maskedWord = "*".repeat(cleanWord.length());
                } else {
                    // Keep first 2 characters, mask the rest with asterisks
                    String prefix = cleanWord.substring(0, 2);
                    String asterisks = "*".repeat(cleanWord.length() - 2);
                    maskedWord = prefix + asterisks;
                }
                
                result.append(maskedWord);
            } else {
                result.append(word);
            }

            if (i < words.length - 1) {
                result.append(" ");
            }
        }

        return new ModerationResult(result.toString(), wasMasked);
    }

    public record ModerationResult(String sanitizedContent, boolean wasMasked) {}
}
