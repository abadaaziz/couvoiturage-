Application de covoiturage avec interface graphique 
Vous concevez une plateforme de covoiturage en ligne où : 
• Les passagers peuvent créer un compte, rechercher et réserver des trajets, payer et 
consulter leurs réservations. 
• Les chauffeurs peuvent proposer des trajets, gérer leurs véhicules et consulter les 
réservations. 
• Les admins peuvent gérer les utilisateurs et superviser les trajets. 
L’objectif est de construire un système complet avec : 
• Diagramme UML (classes, services, relations, interfaces) 
• Back-end Java (classes, interfaces, services) 
• Front-end avec deux options soit :  
▪ Option 1 : Java Swing / JavaFX → interface graphique desktop. 
▪ Option 2 : HTML/CSS + back-end Java → interface web. 
I. 
Règles métier principales 
1. Compte utilisateur 
• Tout utilisateur peut créer un compte en ligne (chauffeur ou passager). 
• Suspension possible (ADMIN) ou blocage après tentatives de connexion 
échouées. 
• Notification par email et SMS intégrée dans les objets utilisateurs. 
2. Trajets 
• Un trajet passe automatiquement à COMPLET dès que la dernière place est 
réservée. 
• Le chauffeur peut annuler uniquement si aucune réservation, sinon pénalité 
pour les passagers (<24h avant départ). 
3. Réservation et paiement 
• Paiement : autorisation immédiate, capture lors de l’acceptation par le 
chauffeur. 
• Annulation passager : remboursement total si >24h avant départ, sinon 
partiel. 
• Annulation chauffeur : pénalisation de 20% par passager si <24h avant départ. 
4. Encapsulation et sécurité 
• Certaines méthodes doivent être private ou protected. 
• Les collections doivent être retournées en copie défensive. 
• Les objets ne modifient que leur état, pas l’état global du système. 
II. 
III. 
Méthodes possibles 
• Gestion utilisateur : creerCompte(), modifierCompte(), suspendreCompte(), 
authentifier(), deconnecter(), bloquerUtilisateur() 
• Trajet : proposerTrajet(), cloreTrajet(), ajouterPassager(), retirerPassager() 
• Réservation : creerReservation(), confirmerReservation(), annulerReservation(), 
rembourserReservation() 
• Paiement : payer(), rembourser() 
• Notifications : notifierSMS(), notifierEmail() 
• Évaluation : evaluerChauffeur(), consulterNotesChauffeur() 
• Consultation : getTrajets(), getReservations(), getVehicules(), getMoyenPaiement() 
Étapes du projet 
1. Modélisation UML 
Faire un diagramme de classes complet avec : 
o Classes : User, Passager, Chauffeur, Admin, Trajet, Reservation, Vehicule, 
MoyenPaiement, etc. 
o Services : AuthService, TrajetService, ReservationService, PaiementService, 
NotificationService 
2. Back-end Java 
• Implémenter les classes et services avec : 
o Encapsulation (private, protected) 
o Copie défensive pour les collections 
o Placement correct des méthodes selon SRP (les étudiants doivent réfléchir 
eux-mêmes) 
o Respect des règles métier : paiement, annulation, notification, état 
automatique COMPLET du trajet 
o Gestion des exceptions  
o Utilisations des collections et des maps  
o Tostring(), equals(), hashCode() …… 
3. Interface graphique 
• Créer des pages pour : 
o Connexion / inscription 
o Consulter trajets disponibles 
o Réserver un trajet et payer 
o Gestion des trajets pour les chauffeurs 
o Consultation des notifications pour tous les utilisateurs