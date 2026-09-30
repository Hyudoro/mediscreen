package com.mediscreen.assessment.domain;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The eleven trigger terms of the client's specification, each with the forms of the word it
 * matches.
 *
 * <p>A term matches a whole word in any of its forms, whatever the case and the accents: "Vertige"
 * is {@link #VERTIGO} and "anormale" is {@link #ABNORMAL}. Other words from the same root do not
 * match: "fume" and "fumer" are not {@link #SMOKER}.
 */
public enum TriggerTerm {
    HEMOGLOBIN_A1C("Hémoglobine A1C"),
    MICROALBUMIN("Microalbumine", "Microalbumines"),
    HEIGHT("Taille", "Tailles"),
    WEIGHT("Poids"),
    SMOKER("Fumeur", "Fumeuse", "Fumeurs", "Fumeuses"),
    ABNORMAL("Anormal", "Anormale", "Anormaux", "Anormales"),
    CHOLESTEROL("Cholestérol", "Cholestérols"),
    VERTIGO("Vertiges", "Vertige"),
    RELAPSE("Rechute", "Rechutes"),
    REACTION("Réaction", "Réactions"),
    ANTIBODIES("Anticorps");

    private final Pattern pattern;

    TriggerTerm(String... forms) {
        // The two words of "Hémoglobine A1C" may be split by any whitespace, a line break included.
        var alternatives = Arrays.stream(forms)
                .map(form -> Folding.fold(form).replace(" ", "\\s+"))
                .collect(Collectors.joining("|"));
        this.pattern = Pattern.compile("\\b(?:" + alternatives + ")\\b");
    }

    /** The distinct terms found in the notes: a term counts once, however often it appears. */
    public static Set<TriggerTerm> foundIn(Collection<String> notes) {
        var folded = notes.stream().map(Folding::fold).toList();
        return Arrays.stream(values())
                .filter(term -> folded.stream().anyMatch(term::isIn))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(TriggerTerm.class)));
    }

    private boolean isIn(String foldedNote) {
        return pattern.matcher(foldedNote).find();
    }

    /**
     * Lower case, without accents and with plain spaces, so that "Cholestérol", "cholesterol" and
     * "CHOLESTÉROL" are the same. NFKD, unlike NFD, also turns a no-break space into a space: French
     * typing puts one between words that must stay together, such as "Hémoglobine" and "A1C".
     */
    private static final class Folding {

        private static final Pattern ACCENTS = Pattern.compile("\\p{M}+");

        static String fold(String text) {
            var decomposed = Normalizer.normalize(text, Normalizer.Form.NFKD);
            return ACCENTS.matcher(decomposed).replaceAll("").toLowerCase(Locale.ROOT);
        }
    }
}
