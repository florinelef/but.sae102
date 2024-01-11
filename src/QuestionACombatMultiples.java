import extensions.CSVFile;

class QuestionACombatMultiples extends Program {

//-----------------------
// -> Variables globales
//-----------------------

    final int VIES_JOUEUR = 100;
    final int ATT_JOUEUR = 5;
    final int STREAK_JOUEUR = 0; //séries de question bien répondues

    final double TAUX_CHANCE_CRIT = 0.0417;
    final double TAUX_CHANCE_BONUS = 0.2; //chance d'avoir un bonus après un combat

    final int CM_CRIT = 2;

    final int NB_BOSS = 15; //Par défaut : 15

    //pour les fichiers CSV
    final char SEPARATEUR = ';';
    final String CHEMIN_SAVE = "ressources/Save/save.csv";
    final String CHEMIN_BOSS = "ressources/Boss/Boss.csv";
    final String MESSAGE_AUCUNE_QUESTION = "Le boss est mort d'inspiration car il n'avait plus de question à vous poser";
    final String MESSAGE_MAUVAISE_REPONSE = ANSI_RED + "Mauvaise réponse !\n" + ANSI_RESET;
    final String MESSAGE_BONNE_REPONSE = ANSI_GREEN + "Bonne réponse !\n" + ANSI_RESET;
    final String MESSAGE_COUP_CRITIQUE = ANSI_YELLOW + "Coup critique !\n" + ANSI_RESET;
    final String MESSAGE_BONUS = "Vous trouvez un blanc co pour effacer vos blessures, vous regagnez 25 PV.";

    final String MOT_SKIP = "dbgskp";

//-------------------
// -> Type Question
//-------------------

    // Créer une question
    Question newQuestion(String question, String[] reponses, int correction){
        Question q = new Question();
        q.question = question;
        q.reponses = reponses;
        q.correction = correction;
        q.dejaPosee = false;
        return q;
    }

    // Affiche une question
    String toString(Question q) {
        String res = "\033[4;37m";
        res = res + q.question + ANSI_RESET + "\n\n";
        for (int i = 0; i < length(q.reponses); i++) {
            res = res + " " + (i+1) + ") " + q.reponses[i] + '\n';
        }
        return res + '\n';
    }

    Question[] csvToQuestionTab(String chemin){
        CSVFile fichier = loadCSV(chemin, SEPARATEUR);
        Question[] tab = new Question[rowCount(fichier)-1];

        for (int i = 1; i < length(tab)+1; i++) {
            String[] reponses = new String[]{getCell(fichier, i, 1), getCell(fichier, i, 2), getCell(fichier, i, 3),getCell(fichier, i, 4)};
            tab[i-1] = newQuestion(getCell(fichier, i, 0), reponses, stringToInt(getCell(fichier, i, 5)));
        }
        return tab;
    }

//-----------------
// -> Type Joueur
//-----------------

    //Créer un joueur
    Joueur newJoueur(String prenom){
        Joueur j = new Joueur();
        j.prenom = prenom;
        j.stats = new int[]{VIES_JOUEUR, ATT_JOUEUR, STREAK_JOUEUR};
        j.score = 0;
        return j;
    }

    Joueur newJoueurContinue(String prenom, int[] stats, int score){
        Joueur j = new Joueur();
        j.prenom = prenom;
        j.stats = stats;
        j.score = score;
        return j;
    }

    String toString(Joueur j){
        return j.prenom + " : " + j.stats[0] + ANSI_RED +" PV" + ANSI_WHITE + " | " + j.stats[1] + ANSI_CYAN +" ATT" + ANSI_WHITE + " | " + j.stats[2] + ANSI_YELLOW + " COMBO" + ANSI_WHITE + " (Score : " + j.score + ")";
    }


//---------------
// -> Type Boss
//---------------

    //Créer un boss
    Boss newBoss(int numBoss, String nom, int pdv, int att, String lore, String cheminFichier){
        Boss b = new Boss();
        b.numBoss = numBoss;
        b.nom = "\033[1;37m" + nom + ANSI_RESET;
        b.pdv = pdv;
        b.att = att;
        b.lore = lore;
        b.cheminFichier = cheminFichier;
        return b;
    }

    String toString(Boss b){
        return b.nom + " : " + b.pdv + ANSI_RED +" PV" + ANSI_WHITE + " | " + b.att + ANSI_CYAN +" ATT" + ANSI_WHITE + " (Boss n°" + b.numBoss + ")";
    }

    Boss[] csvToBossTab(String chemin){
        CSVFile fichier = loadCSV(chemin, SEPARATEUR);
        Boss[] tab = new Boss[NB_BOSS];

        for (int i=0; i<NB_BOSS; i++) {
            tab[i] = newBoss(stringToInt(getCell(fichier,i+2,0)),getCell(fichier,i+2,1),stringToInt(getCell(fichier,i+2,2)),stringToInt(getCell(fichier,i+2,3)),getCell(fichier,i+2,4),getCell(fichier,i+2,5));
        }
        return tab;
    }


//---------------------
// -> Tirage questions
//---------------------

    //renvoie -1 si toutes les questions ont étées posées sinon renvoie un numero de question non posée aléatoire
    int tirageNumQuestion(Question[] questions){
        boolean resteQuestion = false;
        int[] numQuestionPossible = new int[length(questions)];
        //longueur réelle de la liste de numéro de questions possible (pour éviter de tomber sur une case non allouée de la liste s'il y a des questions déjà posées)
        int lengthNQC = 0;

        for(int i=0; i<length(questions); i++){
            if(!questions[i].dejaPosee){
                resteQuestion = true;
                numQuestionPossible[lengthNQC] = i;
                lengthNQC = lengthNQC + 1;
            }
        }
        if(!resteQuestion){
            return -1;
        }
        return numQuestionPossible[(int)(random()*lengthNQC)];
    }

    // procedure qui pose une question et retourne -1 si aucune question dispo, 0 si réponse fausse, 1 si réponse correcte et 2 si skip
    int poserQuestion(Question[] questions){
        int numQuestion = tirageNumQuestion(questions);
        println();
        if(numQuestion == -1){
            return -1;
        }

        Question questionActuelle = questions[numQuestion];
        String reponse;

        println(toString(questionActuelle));

        do {
            print("Votre réponse (1, 2, 3, ou 4): ");
            reponse = readString();
        } while (!(equals(reponse,"1") || equals(reponse,"2") || equals(reponse,"3") || equals(reponse,"4") || equals(reponse,MOT_SKIP)));

        if(equals(reponse,MOT_SKIP)){
            return 2;
        }
        
        int reponseInt = stringToInt(reponse);
        questionActuelle.dejaPosee = true;

        if(reponseInt == questionActuelle.correction){
            return 1;
        }
        return 0;
    }


//---------------
// -> Aléatoire
//---------------

    boolean coupCritique(){
        return random() <= TAUX_CHANCE_CRIT;
    }

    boolean bonus(){
        return random() <= TAUX_CHANCE_BONUS;
    }

//---------------------
// -> Affichage
//---------------------

    void statsJ(Joueur j){
        println(toString(j));
    }

    void statsB(Boss b){
        println(toString(b));
    }

    void afficherMenu(){
        print("Question à combat multiples !\n\n\n1 - Jouer\n2 - Règles\n3 - Quitter\n\n\nRéponse : ");
    }

    void afficherNouvellePartie(){
        CSVFile save = loadCSV(CHEMIN_SAVE);

        print("Voulez vous continuer la partie ?\n\n\n1 - Continuer ");
        println(
            getCell(save, 1, 0)
            + " - Boss n°"
            + getCell(save, 1, 5)
            + " : Points de vie "
            + getCell(save, 1, 1)
            + " / Attaque "
            + getCell(save, 1, 2)
            + " / Combo "
            + getCell(save, 1, 3)
            + " (score : "
            + getCell(save, 1, 4)
            + ")"
        );
        print("2 - Nouvelle Partie\n\n\nRéponse : ");
    }

    void afficherRegles(){
        println("Règles !!!");
    }

//----------------
// -> Sauvegarde
//----------------

    void sauvegarder(Joueur joueur, int tour){

        String[][] sauvegarde = new String[][]{
            {"prenom","PV","ATT","streak","score","tour"},
            {joueur.prenom, ""+joueur.stats[0], ""+joueur.stats[1], ""+joueur.stats[2], ""+joueur.score, ""+tour}
        };

        saveCSV(sauvegarde, CHEMIN_SAVE);
    }

    String chargerPrenom(CSVFile save){
        return "";
    }

    int[] chargerStats(CSVFile save){
        return new int[0];
    }

    int chargerScore(CSVFile save){
        return 0;
    }

    int chargerTour(CSVFile save){
        return 0;
    }

//-------------------------
// -> Algorithme principal
//-------------------------

    void algorithm() {
        boolean quitter = false;
        
        while(!quitter){
            clearScreen();

            afficherMenu();
            String choix = readString();

            clearScreen();
            if(equals(choix, "1") || equals(choix, "Jouer")){

                afficherNouvellePartie();

                //cas : nouvelle partie
                if(equals(choix2, "2") || equals(choix2, "Nouvelle Partie")){
                    print("Quel est votre prénom ?\n> ");
                    String prenom = readString();

                    //éviter d'avoir un prénom vide
                    if(equals(prenom, "")){
                        prenom = "Florine";
                    }

                    //Mettre le prénom en gras et en blanc
                    prenom = "\033[1;37m" + prenom + ANSI_RESET;

                    //Création du/de la joueur.euse, première sauvegarde et affichage du texte introductif
                    Joueur joueur = newJoueur(prenom);
                    sauvegarder(joueur, 0);

                    println();
                    println(prenom + getCell(loadCSV(CHEMIN_BOSS, SEPARATEUR), 1,4));
                    int tour = 0;
                } 
                
                //cas : continuer la partie
                else {
                    Joueur joueur = newJoueurContinue(chargerPrenom, chargerStats, chargerScore);
                    int tour = chargerTour;
                }

                //importation des boss
                Boss[] listeBoss = csvToBossTab(CHEMIN_BOSS);


                //boucle principale                
                while(tour < NB_BOSS && joueur.stats[0] > 0){

                    //enchainement de conditions pour les evenements
                    if(tour == 2){
                        joueur.stats[0] += 25;
                    }
                    if(tour == 4){
                        joueur.stats[1] += 5;
                    }
                    if(tour == 5){
                        joueur.stats[0] += 40;
                    }
                    if(tour == 10){
                        joueur.stats[1] += 50;
                    }
                    if(tour == 13){
                        joueur.stats[1] += 70;
                        joueur.stats[0] += 70;
                    }
                    if(tour == 14){
                        joueur.stats[0] = joueur.stats[0]/2;
                    }


                    Boss boss = listeBoss[tour];
                    Question[] questions = csvToQuestionTab(boss.cheminFichier);

                    println(boss.lore);

                    //tant que le boss ou lae joueur.euse
                    do{
                        boolean resetStreak = false;
                        //afficher stats
                        println();
                        statsJ(joueur);
                        statsB(boss);
                        println('\n' + boss.nom + " vous pose une question :");

                        //a faire condition perdre vie, gerer streak, actualiser vie et att etc
                        //cas : plus aucune question dispo
                        int resultat = poserQuestion(questions);
                        println();
                        if(resultat == -1){
                            println(MESSAGE_AUCUNE_QUESTION);
                            boss.pdv = 0;
                        } 
                        //cas : réponse fausse
                        else if(resultat == 0){
                            println(MESSAGE_MAUVAISE_REPONSE);
                            joueur.stats[2] = 0; //reset streak
                            joueur.score -= 50; //actualisation score

                            joueur.stats[0] -= boss.att;
                            println("Vous avez perdu " + boss.att + ANSI_RED + " PV" + ANSI_WHITE);
                        }
                        //cas : réponse bonne
                        else if(resultat == 1){
                            println(MESSAGE_BONNE_REPONSE);

                            joueur.stats[2] += 1; //ajout 1 streak
                            joueur.score += 20; //actualisation score

                            if(coupCritique()){
                                println(MESSAGE_COUP_CRITIQUE);
                                boss.pdv -= (joueur.stats[1] * CM_CRIT);
                                println("Le boss a perdu " + (joueur.stats[1] * CM_CRIT) + ANSI_RED + " PV" + ANSI_WHITE);
                            } else {
                                boss.pdv -= joueur.stats[1];
                                println("Le boss a perdu " + joueur.stats[1] + ANSI_RED + " PV" + ANSI_WHITE);
                            }
                        } 
                        
                        //cas : skip
                        else {
                            println("SKIP effectué");
                            boss.pdv = 0;
                        }

                    } while(boss.pdv > 0 && joueur.stats[0] > 0);

                    //fin de tour
                    if(bonus()){
                        println(MESSAGE_BONUS);
                        joueur.stats[0] += 25;
                    }

                    joueur.score += 100;
                    tour = tour + 1;
                    joueur.stats[1] += joueur.stats[2];

                    println("\nAppuyez sur entrée pour continuer, sinon écrivez \"quitter\" pour sauvegarder et arrêter !\n> ");
                    if(equals(readString(), "quitter")){
                        sauvegarder(joueur, tour);
                        quitter = true;
                        tour = NB_BOSS + 1; //faire quitter la boucle en invalidant la condition (le tour ayant déjà été sauvegardé, on peut le modifier)
                    }
                    clearScreen();
                }

                if(!quitter){
                    clearScreen();

                    if(joueur.stats[0] <= 0){
                        println("Vous êtes mort...");
                    } else {
                        println("Fin du jeu, vous avez battu les boss bien joué !");
                        statsJ(joueur);
                    }
                }


            //Regles
            } else if(equals(choix, "2")){
                afficherRegles();
                readString();

            //Quitter
            } else if(equals(choix, "3")){
                quitter = true;
            }
        }
    }

//----------------
// -> Assertions
//----------------

    void testPoserQuestion(){

        Question[] questionsTest = csvToQuestionTab("ressources/Questions/Template.csv");
        //le tableau questionsTest contient deux questions (voir le fichier ressource/Questions/Template.csv)

        poserQuestion(questionsTest);
        poserQuestion(questionsTest);
        //on a posé deux questions pour "vider" le tableau (mettre tous les .dejaPosee des questions à true)
        //cela permet de vérifier que les questions ne sont pas posées deux fois puisque sinon le tableau ne serait pas "vide" à la fin

        assertEquals(-1,poserQuestion(questionsTest));
        //on vérifie que la fonction retourne bien -1 (ce qui signifie qu'il ne reste plus aucune question)
    }
}