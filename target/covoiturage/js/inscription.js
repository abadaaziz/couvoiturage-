(function () {
    const params = new URLSearchParams(window.location.search);
    const msgErreur = params.get('erreur');
    if (msgErreur) {
        const alerte = document.getElementById('alerte-erreur');
        alerte.className = 'alerte alerte-erreur';
        alerte.style.display = 'flex';
        alerte.textContent = '⚠ ' + msgErreur;
    }

    document.getElementById('btn-voir-mdp').addEventListener('click', function () {
        const champ = document.getElementById('motDePasse');
        champ.type = champ.type === 'password' ? 'text' : 'password';
        this.textContent = champ.type === 'text' ? '🙈' : '👁';
    });

    document.getElementById('motDePasse').addEventListener('input', function () {
        const val = this.value;
        const indicateur = document.getElementById('indicateur-force');
        const label = document.getElementById('force-mdp');
        let force = 0;
        if (val.length >= 8) force++;
        if (/[A-Z]/.test(val)) force++;
        if (/[0-9]/.test(val)) force++;
        if (/[^A-Za-z0-9]/.test(val)) force++;

        const couleurs = ['#ef4444', '#f59e0b', '#10b981', '#7c3aed'];
        const libelles = ['', 'Faible', 'Moyen', 'Fort', 'Tres fort'];
        indicateur.style.width = force * 25 + '%';
        indicateur.style.background = couleurs[force - 1] || 'transparent';
        label.textContent = val.length > 0 ? libelles[force] : '';
        label.style.color = couleurs[force - 1] || 'transparent';
    });

    document.getElementById('confirmationMotDePasse').addEventListener('input', function () {
        const mdp = document.getElementById('motDePasse').value;
        const msg = document.getElementById('msg-confirmation');
        if (this.value && this.value !== mdp) {
            msg.textContent = 'Les mots de passe ne correspondent pas';
            msg.style.color = '#ef4444';
        } else if (this.value && this.value === mdp) {
            msg.textContent = 'Mots de passe identiques';
            msg.style.color = '#10b981';
        } else {
            msg.textContent = '';
        }
    });

    document.querySelectorAll('input[name="role"]').forEach((radio) => {
        radio.addEventListener('change', function () {
            document.getElementById('label-passager').style.borderColor = 'var(--bordure)';
            document.getElementById('label-chauffeur').style.borderColor = 'var(--bordure)';
            const parentLabel = this.closest('label');
            if (parentLabel) parentLabel.style.borderColor = 'var(--couleur-primaire)';
        });
    });
    document.getElementById('label-passager').style.borderColor = 'var(--couleur-primaire)';

    document.getElementById('form-inscription').addEventListener('submit', function (e) {
        const nom = document.getElementById('nom').value.trim();
        const prenom = document.getElementById('prenom').value.trim();
        const email = document.getElementById('email').value.trim();
        const mdp = document.getElementById('motDePasse').value;
        const conf = document.getElementById('confirmationMotDePasse').value;
        const btn = document.getElementById('btn-inscription');

        if (!nom || !prenom || !email || !mdp) {
            e.preventDefault();
            afficherErreur('Veuillez remplir tous les champs obligatoires (*).');
            return;
        }
        if (mdp.length < 8) {
            e.preventDefault();
            afficherErreur('Le mot de passe doit contenir au moins 8 caracteres.');
            return;
        }
        if (mdp !== conf) {
            e.preventDefault();
            afficherErreur('Les mots de passe ne correspondent pas.');
            return;
        }

        btn.disabled = true;
        btn.innerHTML = '<span class="spinner" style="width:18px;height:18px;border-width:2px;"></span> Creation...';
    });

    function afficherErreur(msg) {
        let alerte = document.getElementById('alerte-erreur');
        if (!alerte) {
            alerte = document.createElement('div');
            alerte.id = 'alerte-erreur';
            alerte.className = 'alerte alerte-erreur';
            document.getElementById('form-inscription').before(alerte);
        }
        alerte.textContent = '⚠ ' + msg;
        alerte.style.display = 'flex';
        alerte.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }
})();
