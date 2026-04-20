/**
 * trajets.js — Logique de la page Trajets
 * Vanilla JS pur, aucun framework.
 */

// ── État de l'application ──────────────────────────────────────────────────
const etat = {
    trajets:       [],
    utilisateur:   null,    // null = non connecté
    chargement:    false
};

// ── Initialisation au chargement de la page ────────────────────────────────
document.addEventListener('DOMContentLoaded', async () => {
    await chargerSession();
    configurerNavbar();
    configurerRecherche();
    configurerModal();
    await chargerTrajets();
});

// ── Chargement de la session utilisateur ─────────────────────────────────
async function chargerSession() {
    // En production, la session est gérée côté serveur (Servlet).
    // Ici, on lit un méta-attribut éventuellement injecté par le servlet dans la page.
    // Simulation : on inspecte le cookie JSESSIONID comme indicateur
    // La session réelle est vérifiée par les servlets.
}

// ── Configuration de la navbar ─────────────────────────────────────────────
function configurerNavbar() {
    // Ajuste la date minimale dans la recherche à aujourd'hui
    const inputDate = document.getElementById('rech-date');
    if (inputDate) {
        inputDate.min = new Date().toISOString().split('T')[0];
    }
}

// ── Chargement des trajets via l'API ──────────────────────────────────────
async function chargerTrajets(params = '') {
    afficherChargement(true);
    try {
        const url = params ? `/trajets?${params}` : '/trajets';
        const reponse = await fetch(url, {
            method: 'GET',
            headers: { 'Accept': 'application/json' }
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

// ── Rendu HTML des trajets ─────────────────────────────────────────────────
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
}

function creerCarteTrajet(t) {
    const el = document.createElement('div');
    el.className = 'carte-trajet';
    el.dataset.id = t.id;

    const dateDepart = new Date(t.dateHeureDepart);
    const dateFormatee = dateDepart.toLocaleDateString('fr-FR', {
        weekday: 'short', day: 'numeric', month: 'short'
    });
    const heureFormatee = dateDepart.toLocaleTimeString('fr-FR', {
        hour: '2-digit', minute: '2-digit'
    });

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
                <span class="trajet-prix">${t.prixParPlace.toFixed(2)}€</span>
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
            ${t.statut === 'OUVERT' ? `
            <button class="btn btn-primaire btn-sm btn-reserver" data-id="${t.id}"
                    data-prix="${t.prixParPlace}" data-ville-depart="${echapper(t.villeDepart)}"
                    data-ville-arrivee="${echapper(t.villeArrivee)}">
                Réserver
            </button>` : ''}
        </div>
    `;

    // Événement clic sur "Réserver"
    const btnReserver = el.querySelector('.btn-reserver');
    if (btnReserver) {
        btnReserver.addEventListener('click', () => ouvrirReservation(t));
    }

    return el;
}

// ── Génération des étoiles ─────────────────────────────────────────────────
function genererEtoiles(note) {
    const plein  = Math.floor(note);
    const vide   = 5 - plein;
    return '★'.repeat(plein) + '☆'.repeat(vide);
}

// ── Badge de statut ────────────────────────────────────────────────────────
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

// ── Ouverture du formulaire de réservation ─────────────────────────────────
function ouvrirReservation(trajet) {
    // Vérification connexion (redirection si pas de session)
    const confirmMsg = `Réserver sur ${trajet.villeDepart} → ${trajet.villeArrivee} pour ${trajet.prixParPlace}€/place ?`;
    if (confirm(confirmMsg)) {
        reserverTrajet(trajet.id);
    }
}

async function reserverTrajet(trajetId) {
    try {
        const formData = new FormData();
        formData.append('trajetId', trajetId);
        formData.append('nombrePlaces', '1');
        formData.append('methodePaiement', 'CARTE_BANCAIRE');

        const reponse = await fetch('/reservation/creer', {
            method: 'POST',
            body: formData
        });

        const data = await reponse.json();

        if (reponse.ok) {
            afficherNotification('✓ Réservation créée ! En attente de confirmation du chauffeur.', 'succes');
            await chargerTrajets(); // Actualiser la liste
        } else if (reponse.status === 401) {
            window.location.href = '/login';
        } else {
            afficherNotification('❌ ' + (data.erreur || 'Erreur lors de la réservation.'), 'erreur');
        }

    } catch (err) {
        console.error('[trajets.js] Erreur réservation :', err);
        afficherNotification('❌ Erreur réseau. Veuillez réessayer.', 'erreur');
    }
}

// ── Formulaire de recherche ────────────────────────────────────────────────
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

// ── Modal nouveau trajet ───────────────────────────────────────────────────
function configurerModal() {
    const modal       = document.getElementById('modal-trajet');
    const btnProposer = document.getElementById('btn-proposer');
    const btnFermer   = document.getElementById('btn-fermer-modal');
    const form        = document.getElementById('form-nouveau-trajet');

    if (btnProposer) {
        btnProposer.addEventListener('click', () => {
            modal.style.display = 'flex';
            // Date minimale = dans 30 minutes
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

    // Fermeture en cliquant en dehors
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

    const formData = new FormData();
    formData.append('villeDepart',         document.getElementById('nv-depart').value.trim());
    formData.append('villeArrivee',        document.getElementById('nv-arrivee').value.trim());
    formData.append('dateHeureDepart',     document.getElementById('nv-date').value);
    formData.append('placesTotal',         document.getElementById('nv-places').value);
    formData.append('prixParPlace',        document.getElementById('nv-prix').value);
    formData.append('descriptionVehicule', document.getElementById('nv-vehicule').value.trim());

    btn.disabled = true;
    btn.innerHTML = '<span class="spinner" style="width:16px;height:16px;border-width:2px;"></span> Publication...';

    try {
        const reponse = await fetch('/trajets/nouveau', {
            method: 'POST',
            body: formData
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

// ── Notification flottante ─────────────────────────────────────────────────
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

// ── Utilitaire : échappement HTML ──────────────────────────────────────────
function echapper(s) {
    if (!s) return '';
    return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}
