


const etat = {
    trajets:       [],
    utilisateur:   null,
    chargement:    false,
    reservationSelection: null
};

function estChauffeurConnecte() {
    return etat.utilisateur &&
    etat.utilisateur.role === 'CHAUFFEUR';
}

function estAdminConnecte() {
    return etat.utilisateur && etat.utilisateur.role === 'ADMIN';
}


document.addEventListener('DOMContentLoaded', async () => {
    await chargerSession();
    configurerNavbar();
    configurerRecherche();
    configurerModal();
    configurerModalReservation();
    configurerModalProfil();
    await chargerTrajets();
});


async function chargerSession() {
    try {
        const reponse = await fetch('/session/me', {
            method: 'GET',
            headers: { 'Accept': 'application/json' }
        });

        if (!reponse.ok) {
            etat.utilisateur = null;
            return;
        }

        const data = await reponse.json();
        etat.utilisateur = data.connecte ? data : null;

    } catch (e) {
        etat.utilisateur = null;
    }
}


function configurerNavbar() {

    const inputDate = document.getElementById('rech-date');
    if (inputDate) {
        inputDate.min = new Date().toISOString().split('T')[0];
    }


    const zoneActions = document.getElementById('navbar-actions');
    if (zoneActions && etat.utilisateur) {
        zoneActions.innerHTML = `
            <span class="badge" style="background:rgba(34,197,94,0.16);color:#86efac;border:1px solid rgba(34,197,94,0.35);">${echapper(etat.utilisateur.role)}</span>
            <span class="texte-sm texte-secondaire">${echapper(etat.utilisateur.prenom)}</span>
            <a href="/logout" class="btn btn-secondaire btn-sm">Deconnexion</a>
        `;
    }

    const lienMesReservations = document.getElementById('nav-mes-reservations');
    if (lienMesReservations) {
        if (estAdminConnecte()) {
            lienMesReservations.href = '/admin/users';
            lienMesReservations.textContent = 'Utilisateurs';
        } else if (estChauffeurConnecte()) {
            lienMesReservations.href = '/trajets/mes';
            lienMesReservations.textContent = 'Mes trajets';
        } else {
            lienMesReservations.href = '/reservation/mes';
            lienMesReservations.textContent = 'Mes reservations';
        }
    }


    const zoneChauffeur = document.getElementById('zone-chauffeur');
    if (zoneChauffeur) {
        zoneChauffeur.style.display = estChauffeurConnecte() ? 'block' : 'none';
    }
}


async function chargerTrajets(params = '') {
    afficherChargement(true);
    try {
        const query = new URLSearchParams(params);

        query.set('_', Date.now().toString());
        const url = `/trajets?${query.toString()}`;
        const reponse = await fetch(url, {
            method: 'GET',
            headers: { 'Accept': 'application/json' },
            cache: 'no-store'
        });

        if (!reponse.ok) {
            throw new Error(`Erreur serveur : ${reponse.status}`);
        }

        etat.trajets = await reponse.json();
        afficherTrajets(etat.trajets);

    } catch (err) {
        console.error('[trajets.js] Erreur chargement :', err);
        afficherErreurGlobale('Impossible de charger les trajets. Veuillez réessayer.');
    } finally {
        afficherChargement(false);
    }
}


function afficherTrajets(trajets) {
    const liste  = document.getElementById('liste-trajets');
    const aucun  = document.getElementById('aucun-trajet');

    liste.innerHTML = '';

    if (!trajets || trajets.length === 0) {
        aucun.style.display = 'block';
        return;
    }
    aucun.style.display = 'none';

    trajets.forEach((trajet, index) => {
        const carte = creerCarteTrajet(trajet);
        carte.style.animationDelay = `${index * 0.05}s`;
        carte.classList.add('animation-entree');
        liste.appendChild(carte);
    });

    if (estAdminConnecte()) {
        liste.querySelectorAll('.btn-reserver').forEach((btn) => btn.remove());
    }
}

function creerCarteTrajet(t) {
    const el = document.createElement('div');
    el.className = 'carte-trajet';
    el.dataset.id = t.id;

    const dateDepart = parserDateTrajet(t.dateHeureDepart);
    const dateFormatee = dateDepart
        ? dateDepart.toLocaleDateString('fr-FR', { weekday: 'short', day: 'numeric', month: 'short' })
        : echapper(t.dateHeureDepart || 'Date inconnue');
    const heureFormatee = dateDepart
        ? dateDepart.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })
        : '--:--';

    const initiales = (t.chauffeur.prenom?.[0] || '') + (t.chauffeur.nom?.[0] || '');
    const etoiles   = genererEtoiles(t.chauffeur.note || 0);
    const badgeHtml = getBadgeStatut(t.statut);
    const placesLib = t.placesDisponibles <= 0
        ? '<span style="color:var(--couleur-danger);">Complet</span>'
        : `<span style="color:var(--couleur-succes);">${t.placesDisponibles} dispo</span>`;

    el.innerHTML = `
        <div class="carte-trajet-entete" style="display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:1rem;">
            <div class="trajet-route" style="flex:1;">
                <span class="trajet-ville">${echapper(t.villeDepart)}</span>
                <span class="trajet-fleche">→</span>
                <span class="trajet-ville">${echapper(t.villeArrivee)}</span>
            </div>
            ${badgeHtml}
        </div>

        <div class="trajet-infos">
            <div class="trajet-info-item">
                <span class="trajet-info-label">📅 Date</span>
                <span class="trajet-info-valeur">${dateFormatee}</span>
            </div>
            <div class="trajet-info-item">
                <span class="trajet-info-label">🕐 Heure</span>
                <span class="trajet-info-valeur">${heureFormatee}</span>
            </div>
            <div class="trajet-info-item">
                <span class="trajet-info-label">💺 Places</span>
                <span class="trajet-info-valeur">${placesLib}</span>
            </div>
            <div class="trajet-info-item">
                <span class="trajet-info-label">💰 Prix</span>
                <span class="trajet-prix">${t.prixParPlace.toFixed(2)} DT</span>
            </div>
        </div>

        ${t.descriptionVehicule ? `
        <p class="texte-xs texte-secondaire" style="margin:0.5rem 0;">
            🚗 ${echapper(t.descriptionVehicule)}
        </p>` : ''}

        <div class="trajet-chauffeur">
            <div class="avatar">${initiales.toUpperCase()}</div>
            <div style="flex:1;">
                <p class="semi-gras texte-sm">${echapper(t.chauffeur.prenom)} ${echapper(t.chauffeur.nom)}</p>
                <p class="texte-xs texte-secondaire">${etoiles} ${t.chauffeur.note ? t.chauffeur.note.toFixed(1) : 'Nouveau'}</p>
            </div>
            <button class="btn btn-secondaire btn-sm btn-profil" data-id="${t.id}">Profil</button>
            ${t.statut === 'OUVERT' && !estChauffeurConnecte() && !estAdminConnecte() ? `
            <button class="btn btn-primaire btn-sm btn-reserver" data-id="${t.id}"
                    data-prix="${t.prixParPlace}" data-ville-depart="${echapper(t.villeDepart)}"
                    data-ville-arrivee="${echapper(t.villeArrivee)}">
                Réserver
            </button>` : ''}
        </div>
    `;


    const btnReserver = el.querySelector('.btn-reserver');
    if (btnReserver) {
        btnReserver.addEventListener('click', () => ouvrirReservation(t));
    }

    const btnProfil = el.querySelector('.btn-profil');
    if (btnProfil) {
        btnProfil.addEventListener('click', () => ouvrirProfilChauffeur(t));
    }

    return el;
}


function genererEtoiles(note) {
    const plein  = Math.floor(note);
    const vide   = 5 - plein;
    return '★'.repeat(plein) + '☆'.repeat(vide);
}


function getBadgeStatut(statut) {
    const classes = {
        OUVERT:  'badge-ouvert',
        COMPLET: 'badge-complet',
        ANNULE:  'badge-annule',
        TERMINE: 'badge-termine'
    };
    const libelles = {
        OUVERT:  '● Ouvert',
        COMPLET: '● Complet',
        ANNULE:  '✕ Annulé',
        TERMINE: '✓ Terminé'
    };
    return `<span class="badge ${classes[statut] || ''}">${libelles[statut] || statut}</span>`;
}


function ouvrirReservation(trajet) {
    const modal = document.getElementById('modal-reservation');
    const selectPlaces = document.getElementById('reservation-places');
    const resume = document.getElementById('reservation-trajet-resume');
    const montant = document.getElementById('reservation-montant');
    const erreur = document.getElementById('reservation-erreur');

    if (!modal || !selectPlaces || !resume || !montant || !erreur) {
        afficherNotification('❌ Interface de réservation indisponible.', 'erreur');
        return;
    }

    if (!trajet || !trajet.id || !trajet.placesDisponibles || trajet.placesDisponibles < 1) {
        afficherNotification('❌ Ce trajet n\'a plus de place disponible.', 'erreur');
        return;
    }

    etat.reservationSelection = trajet;
    erreur.style.display = 'none';

    resume.textContent = `${trajet.villeDepart} → ${trajet.villeArrivee} · ${trajet.placesDisponibles} place(s) disponible(s)`;
    selectPlaces.innerHTML = '';

    const max = Math.min(8, trajet.placesDisponibles);
    for (let i = 1; i <= max; i++) {
        const option = document.createElement('option');
        option.value = String(i);
        option.textContent = `${i} place${i > 1 ? 's' : ''}`;
        selectPlaces.appendChild(option);
    }

    const calculerMontant = () => {
        const nb = Number(selectPlaces.value || '1');
        const total = nb * Number(trajet.prixParPlace || 0);
        montant.textContent = `Montant estimé: ${total.toFixed(2)} DT`;
    };

    selectPlaces.onchange = calculerMontant;
    calculerMontant();

    modal.style.display = 'flex';
}

async function reserverTrajet(trajetId, nombrePlaces, methodePaiement) {
    try {
        if (!trajetId) {
            afficherNotification('❌ Identifiant du trajet introuvable.', 'erreur');
            return false;
        }

        const payload = new URLSearchParams();
        payload.set('trajetId', String(trajetId));
        payload.set('nombrePlaces', String(nombrePlaces || 1));
        payload.set('methodePaiement', methodePaiement || 'CARTE_BANCAIRE');

        const reponse = await fetch('/reservation/creer', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
                'Accept': 'application/json'
            },
            body: payload.toString()
        });

        const data = await reponse.json();

        if (reponse.ok) {
            afficherNotification('✓ Réservation créée. Redirection vers le paiement...', 'succes');
            return data;
        } else if (reponse.status === 401) {
            window.location.href = '/login';
            return null;
        } else {
            afficherNotification('❌ ' + (data.erreur || 'Erreur lors de la réservation.'), 'erreur');
            return null;
        }

    } catch (err) {
        console.error('[trajets.js] Erreur réservation :', err);
        afficherNotification('❌ Erreur réseau. Veuillez réessayer.', 'erreur');
        return null;
    }
}

function configurerModalReservation() {
    const modal = document.getElementById('modal-reservation');
    const form = document.getElementById('form-reservation');
    const btnFermer = document.getElementById('btn-fermer-modal-reservation');
    const btnConfirmer = document.getElementById('btn-confirmer-reservation');
    const errDiv = document.getElementById('reservation-erreur');

    if (!modal || !form || !btnFermer || !btnConfirmer || !errDiv) {
        return;
    }

    const fermer = () => {
        modal.style.display = 'none';
        errDiv.style.display = 'none';
        etat.reservationSelection = null;
    };

    btnFermer.addEventListener('click', fermer);

    modal.addEventListener('click', (e) => {
        if (e.target === modal) fermer();
    });

    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        const trajet = etat.reservationSelection;
        const nb = Number(document.getElementById('reservation-places').value || '0');
        const methode = document.getElementById('reservation-methode').value;

        if (!trajet || !trajet.id) {
            errDiv.textContent = '⚠ Trajet introuvable.';
            errDiv.style.display = 'flex';
            return;
        }

        if (!nb || nb < 1) {
            errDiv.textContent = '⚠ Nombre de places invalide.';
            errDiv.style.display = 'flex';
            return;
        }

        btnConfirmer.disabled = true;
        btnConfirmer.innerHTML = '<span class="spinner" style="width:16px;height:16px;border-width:2px;"></span> Réservation...';
        errDiv.style.display = 'none';

        const reservation = await reserverTrajet(trajet.id, nb, methode);

        btnConfirmer.disabled = false;
        btnConfirmer.innerHTML = 'Confirmer la réservation';

        if (reservation && reservation.id) {
            window.location.href = `/paiement?reservationId=${reservation.id}`;
            return;
        }

        if (reservation) {
            fermer();
        }
    });
}

function configurerModalProfil() {
    const modal = document.getElementById('modal-profil');
    const btnFermer = document.getElementById('btn-fermer-modal-profil');

    if (!modal || !btnFermer) return;

    const fermer = () => {
        modal.style.display = 'none';
        const err = document.getElementById('profil-erreur');
        if (err) err.style.display = 'none';
    };

    btnFermer.addEventListener('click', fermer);
    modal.addEventListener('click', (e) => {
        if (e.target === modal) fermer();
    });
}

function afficherProfilErreur(message) {
    const err = document.getElementById('profil-erreur');
    if (!err) return;
    err.textContent = message;
    err.style.display = 'flex';
}

function ouvrirProfilChauffeur(trajet) {
    const modal = document.getElementById('modal-profil');
    const nom = document.getElementById('profil-nom');
    const note = document.getElementById('profil-note');
    const tel = document.getElementById('profil-telephone');
    const email = document.getElementById('profil-email');
    const avatar = document.getElementById('profil-avatar');

    if (!modal || !trajet || !trajet.chauffeur) return;

    const chauffeur = trajet.chauffeur;
    const initiales = (chauffeur.prenom?.[0] || '') + (chauffeur.nom?.[0] || '');

    nom.textContent = `${chauffeur.prenom || ''} ${chauffeur.nom || ''}`.trim();
    note.textContent = chauffeur.note ? `Note: ${Number(chauffeur.note).toFixed(1)} / 5` : 'Nouveau chauffeur';
    tel.textContent = chauffeur.telephone || '-';
    email.textContent = chauffeur.email || '-';
    avatar.textContent = initiales.toUpperCase();
    modal.style.display = 'flex';
}
function configurerRecherche() {
    const form = document.getElementById('form-recherche');
    const btnReset = document.getElementById('btn-reset');

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const depart  = document.getElementById('rech-depart').value.trim();
        const arrivee = document.getElementById('rech-arrivee').value.trim();
        const date    = document.getElementById('rech-date').value;
        const places  = document.getElementById('rech-places').value;

        const params = new URLSearchParams();
        if (depart)  params.set('depart', depart);
        if (arrivee) params.set('arrivee', arrivee);
        if (date)    params.set('date', date);
        if (places)  params.set('places', places);

        await chargerTrajets(params.toString());
    });

    btnReset.addEventListener('click', async () => {
        form.reset();
        await chargerTrajets();
    });
}


function configurerModal() {
    const modal       = document.getElementById('modal-trajet');
    const btnProposer = document.getElementById('btn-proposer');
    const btnFermer   = document.getElementById('btn-fermer-modal');
    const form        = document.getElementById('form-nouveau-trajet');

    if (btnProposer) {
        btnProposer.addEventListener('click', () => {
            modal.style.display = 'flex';

            const min = new Date(Date.now() + 30 * 60 * 1000);
            document.getElementById('nv-date').min =
                min.toISOString().slice(0, 16);
        });
    }

    if (btnFermer) {
        btnFermer.addEventListener('click', () => {
            modal.style.display = 'none';
        });
    }


    modal.addEventListener('click', (e) => {
        if (e.target === modal) modal.style.display = 'none';
    });

    if (form) {
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            await soumettreNouveauTrajet();
        });
    }
}

async function soumettreNouveauTrajet() {
    const errDiv = document.getElementById('modal-erreur');
    const sucDiv = document.getElementById('modal-succes');
    const btn    = document.getElementById('btn-soumettre-trajet');

    errDiv.style.display = 'none';
    sucDiv.style.display = 'none';

    const villeDepart = document.getElementById('nv-depart').value.trim();
    const villeArrivee = document.getElementById('nv-arrivee').value.trim();
    const dateRaw = document.getElementById('nv-date').value.trim();
    const placesTotal = document.getElementById('nv-places').value;
    const prixParPlace = document.getElementById('nv-prix').value;
    const descriptionVehicule = document.getElementById('nv-vehicule').value.trim();


    const dateHeureDepart = dateRaw.replace(' ', 'T');

    if (!villeDepart || !villeArrivee || !dateHeureDepart || !placesTotal || !prixParPlace) {
        errDiv.textContent = '⚠ Tous les champs obligatoires doivent être renseignés.';
        errDiv.style.display = 'flex';
        return;
    }

    const payload = new URLSearchParams();
    payload.set('villeDepart', villeDepart);
    payload.set('villeArrivee', villeArrivee);
    payload.set('dateHeureDepart', dateHeureDepart);
    payload.set('placesTotal', placesTotal);
    payload.set('prixParPlace', prixParPlace);
    payload.set('descriptionVehicule', descriptionVehicule);

    btn.disabled = true;
    btn.innerHTML = '<span class="spinner" style="width:16px;height:16px;border-width:2px;"></span> Publication...';

    try {
        const reponse = await fetch('/trajets/nouveau', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
            },
            body: payload.toString()
        });

        const data = await reponse.json();

        if (reponse.ok || reponse.status === 201) {
            sucDiv.textContent = '✓ Trajet publié avec succès ! (Réf. #' + data.id + ')';
            sucDiv.style.display = 'flex';
            document.getElementById('form-nouveau-trajet').reset();
            await chargerTrajets();
            setTimeout(() => { document.getElementById('modal-trajet').style.display = 'none'; }, 2000);
        } else if (reponse.status === 401) {
            window.location.href = '/login';
        } else {
            errDiv.textContent = '⚠ ' + (data.erreur || 'Erreur lors de la publication.');
            errDiv.style.display = 'flex';
        }

    } catch (err) {
        errDiv.textContent = '⚠ Erreur réseau. Veuillez réessayer.';
        errDiv.style.display = 'flex';
    } finally {
        btn.disabled = false;
        btn.innerHTML = 'Publier le trajet';
    }
}


function afficherNotification(message, type = 'info') {
    const notif = document.createElement('div');
    notif.className = `alerte alerte-${type}`;
    notif.style.cssText = `
        position: fixed; bottom: 2rem; right: 2rem; z-index: 3000;
        max-width: 400px; box-shadow: 0 8px 32px rgba(0,0,0,0.4);
        animation: fadeInUp 0.3s ease;
    `;
    notif.textContent = message;
    document.body.appendChild(notif);
    setTimeout(() => notif.remove(), 4000);
}

function afficherErreurGlobale(msg) {
    const zone = document.getElementById('zone-resultats');
    zone.innerHTML = `<div class="alerte alerte-erreur">${msg}</div>`;
}

function afficherChargement(visible) {
    const el = document.getElementById('chargement');
    if (el) el.style.display = visible ? 'flex' : 'none';
}

function parserDateTrajet(valeur) {
    if (!valeur) return null;


    let iso = String(valeur).trim().replace(' ', 'T');
    iso = iso.replace(/\.(\d{3})\d+/, '.$1');

    const d = new Date(iso);
    return Number.isNaN(d.getTime()) ? null : d;
}


function echapper(s) {
    if (!s) return '';
    return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}
