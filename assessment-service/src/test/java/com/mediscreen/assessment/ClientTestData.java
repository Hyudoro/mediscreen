package com.mediscreen.assessment;

import com.mediscreen.assessment.domain.Gender;
import java.time.LocalDate;
import java.util.List;

/**
 * The client's four test patients, from the sprint 1 sheet, with their notes word for word from the
 * sprint 2 sheet.
 */
public final class ClientTestData {

    public record TestPatient(
            long id,
            String lastName,
            LocalDate birthDate,
            Gender gender,
            String street,
            String phoneNumber,
            List<String> notes) {}

    public static final List<TestPatient> PATIENTS = List.of(
            new TestPatient(
                    1,
                    "TestNone",
                    LocalDate.of(1966, 12, 31),
                    Gender.FEMALE,
                    "1 Brookside St",
                    "100-222-3333",
                    List.of(
                            "Le patient déclare qu'il 'se sent très bien' Poids égal ou inférieur au poids recommandé")),
            new TestPatient(
                    2,
                    "TestBorderline",
                    LocalDate.of(1945, 6, 24),
                    Gender.MALE,
                    "2 High St",
                    "200-333-4444",
                    List.of(
                            "Le patient déclare qu'il ressent beaucoup de stress au travail Il se plaint également"
                                    + " que son audition est anormale dernièrement",
                            "Le patient déclare avoir fait une réaction aux médicaments au cours des 3 derniers mois"
                                    + " Il remarque également que son audition continue d'être anormale")),
            new TestPatient(
                    3,
                    "TestInDanger",
                    LocalDate.of(2004, 6, 18),
                    Gender.MALE,
                    "3 Club Road",
                    "300-444-5555",
                    List.of(
                            "Le patient déclare qu'il fume depuis peu",
                            "Le patient déclare qu'il est fumeur et qu'il a cessé de fumer l'année dernière Il se"
                                    + " plaint également de crises d’apnée respiratoire anormales Tests de laboratoire"
                                    + " indiquant un taux de cholestérol LDL élevé")),
            new TestPatient(
                    4,
                    "TestEarlyOnset",
                    LocalDate.of(2002, 6, 28),
                    Gender.FEMALE,
                    "4 Valley Dr",
                    "400-555-6666",
                    List.of(
                            "Le patient déclare qu'il lui est devenu difficile de monter les escaliers Il se plaint"
                                    + " également d’être essoufflé Tests de laboratoire indiquant que les anticorps"
                                    + " sont élevés Réaction aux médicaments",
                            "Le patient déclare qu'il a mal au dos lorsqu'il reste assis pendant longtemps",
                            "Le patient déclare avoir commencé à fumer depuis peu Hémoglobine A1C supérieure au"
                                    + " niveau recommandé",
                            "Taille, Poids, Cholestérol, Vertige et Réaction")));

    public static TestPatient patient(long id) {
        return PATIENTS.stream()
                .filter(patient -> patient.id() == id)
                .findFirst()
                .orElseThrow();
    }

    private ClientTestData() {}
}
