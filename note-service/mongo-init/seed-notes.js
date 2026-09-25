// The nine doctor notes supplied by the client for the four test patients.
//
// The mongo image runs this script once, when it initialises an empty data
// directory, so it never touches a database that already holds data. patientId
// is the id patient-service gives the same patient: the sprint 2 sheet joins
// its notes to patients 1 to 4 by it. The sheet gives no dates, so each note is
// stamped with the load time, one second apart, which keeps the sheet's order
// when the notes are sorted by date.

const notes = [
  [1, 'TestNone', "Le patient déclare qu'il 'se sent très bien' Poids égal ou inférieur au poids recommandé"],
  [2, 'TestBorderline', "Le patient déclare qu'il ressent beaucoup de stress au travail Il se plaint également que son audition est anormale dernièrement"],
  [2, 'TestBorderline', "Le patient déclare avoir fait une réaction aux médicaments au cours des 3 derniers mois Il remarque également que son audition continue d'être anormale"],
  [3, 'TestInDanger', "Le patient déclare qu'il fume depuis peu"],
  [3, 'TestInDanger', "Le patient déclare qu'il est fumeur et qu'il a cessé de fumer l'année dernière Il se plaint également de crises d’apnée respiratoire anormales Tests de laboratoire indiquant un taux de cholestérol LDL élevé"],
  [4, 'TestEarlyOnset', "Le patient déclare qu'il lui est devenu difficile de monter les escaliers Il se plaint également d’être essoufflé Tests de laboratoire indiquant que les anticorps sont élevés Réaction aux médicaments"],
  [4, 'TestEarlyOnset', "Le patient déclare qu'il a mal au dos lorsqu'il reste assis pendant longtemps"],
  [4, 'TestEarlyOnset', 'Le patient déclare avoir commencé à fumer depuis peu Hémoglobine A1C supérieure au niveau recommandé'],
  [4, 'TestEarlyOnset', 'Taille, Poids, Cholestérol, Vertige et Réaction'],
];

const loadedAt = Date.now();

db.getSiblingDB('mediscreen').notes.insertMany(
  notes.map(([patientId, patient, note], index) => ({
    patientId: NumberLong(patientId),
    patient,
    note,
    createdAt: new Date(loadedAt - (notes.length - index) * 1000),
  })),
);
