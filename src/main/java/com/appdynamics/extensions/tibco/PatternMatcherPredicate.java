package com.appdynamics.extensions.tibco;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import java.util.List;

/**
 * @author Satish Muddam
 */
public class PatternMatcherPredicate implements Predicate<String> {

    private List<String> patternsToMatch;
    private Predicate<CharSequence> patternPredicate;

    public PatternMatcherPredicate(List<String> patternsToMatch) {
        this.patternsToMatch = patternsToMatch;
        build();
    }

    private void build() {
        if (patternsToMatch != null && !patternsToMatch.isEmpty()) {
            for (String pattern : patternsToMatch) {
                Predicate<CharSequence> charSequencePredicate = Predicates.containsPattern(pattern);
                if (patternPredicate == null) {
                    patternPredicate = charSequencePredicate;
                } else {
                    patternPredicate = Predicates.or(patternPredicate, charSequencePredicate);
                }
            }
        }
    }

    public boolean apply(String input) {
        return patternPredicate.apply(input);
    }
}
