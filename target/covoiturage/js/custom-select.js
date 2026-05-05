/**
 * custom-select.js
 * Remplace chaque <select class="form-controle"> par un menu déroulant
 * entièrement stylisable. Le <select> natif reste caché et synchronisé
 * (select.value continue de fonctionner pour toute la logique existante).
 */
(function () {
    'use strict';

    function ensureCriticalStyles() {
        if (document.getElementById('cs-critical-styles')) return;

        const style = document.createElement('style');
        style.id = 'cs-critical-styles';
        style.textContent = `
            .cs-native{display:none!important}
            .cs-wrapper{position:relative;display:block;width:100%}
            .cs-trigger{display:block;width:100%;padding:.75rem 2.5rem .75rem 1rem;background-color:#1a1b26;border:1px solid rgba(255,255,255,.08);border-radius:.75rem;color:#f8fafc;-webkit-text-fill-color:#f8fafc;font:inherit;font-size:.95rem;line-height:1.6;text-align:left;cursor:pointer;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;background-image:url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%2394a3b8' d='M6 8L1 3h10z'/%3E%3C/svg%3E");background-repeat:no-repeat;background-position:right 1rem center}
            .cs-wrapper.cs-open .cs-trigger,.cs-trigger:hover{border-color:#7c3aed;color:#fff;-webkit-text-fill-color:#fff;box-shadow:0 0 0 2px rgba(124,58,237,.3)}
            .cs-panel{display:none;position:absolute;left:0;right:0;padding:.35rem;background:#202234;color:#f8fafc;border:1px solid rgba(124,58,237,.45);border-radius:.75rem;box-shadow:0 24px 64px rgba(0,0,0,.7);z-index:9999;max-height:360px;overflow-y:auto;pointer-events:auto}
            .cs-wrapper.cs-open .cs-panel{display:block}
            .cs-option{padding:.65rem 1rem;border-radius:.5rem;background:#111218;color:#f8fafc;-webkit-text-fill-color:#f8fafc;font:inherit;font-size:.9rem;cursor:pointer;user-select:none}
            .cs-option:hover:not(.cs-option--disabled){background:#2d1b69;color:#fff;-webkit-text-fill-color:#fff}
            .cs-option--selected{background:#5b21b6;color:#fff;-webkit-text-fill-color:#fff;font-weight:600}
            .cs-option--disabled{background:#1a1b26;color:#94a3b8;-webkit-text-fill-color:#94a3b8;cursor:not-allowed;opacity:.6}
        `;
        document.head.appendChild(style);
    }

    function initCustomSelect(select) {
        ensureCriticalStyles();
        if (select.dataset.csInit === '1') return;
        select.dataset.csInit = '1';

        /* ── Wrapper ────────────────────────────────────────────── */
        const wrapper = document.createElement('div');
        wrapper.className = 'cs-wrapper';

        // Préserver les styles inline (ex. max-width:120px)
        if (select.style.maxWidth) wrapper.style.maxWidth = select.style.maxWidth;
        if (select.style.width)    wrapper.style.width    = select.style.width;

        select.parentNode.insertBefore(wrapper, select);
        select.classList.add('cs-native');
        select.style.display = 'none';
        select.tabIndex = -1;
        select.setAttribute('aria-hidden', 'true');
        wrapper.appendChild(select);

        /* ── Bouton déclencheur ─────────────────────────────────── */
        const trigger = document.createElement('button');
        trigger.className = 'cs-trigger';
        trigger.type = 'button';
        trigger.setAttribute('role', 'combobox');
        trigger.setAttribute('aria-haspopup', 'listbox');
        trigger.setAttribute('aria-expanded', 'false');
        trigger.setAttribute('tabindex', '0');
        wrapper.appendChild(trigger);

        /* ── Panneau des options ─────────────────────────────────── */
        const panel = document.createElement('div');
        panel.className = 'cs-panel';
        panel.setAttribute('role', 'listbox');
        wrapper.appendChild(panel);

        let isOpen = false;

        /* ── Rendu des options ───────────────────────────────────── */
        function choisirIndex(index) {
            const opt = select.options[index];
            if (!opt || opt.disabled) return;

            opt.selected = true;
            select.selectedIndex = index;
            select.value = opt.value;
            select.dispatchEvent(new Event('input', { bubbles: true }));
            select.dispatchEvent(new Event('change', { bubbles: true }));
            syncTrigger();
            close();
        }

        function optionDepuisEvenement(e) {
            const option = e.target.closest('.cs-option');
            if (!option || !panel.contains(option) || option.classList.contains('cs-option--disabled')) {
                return null;
            }
            return Number(option.dataset.idx);
        }

        function renderOptions() {
            panel.innerHTML = '';
            Array.from(select.options).forEach((opt, i) => {
                const item = document.createElement('div');
                item.className = 'cs-option';
                if (opt.disabled)            item.classList.add('cs-option--disabled');
                if (i === select.selectedIndex) item.classList.add('cs-option--selected');
                item.setAttribute('role', 'option');
                item.setAttribute('aria-selected', String(i === select.selectedIndex));
                item.textContent = opt.text;
                item.dataset.idx = i;

                panel.appendChild(item);
            });
        }
        select._csSync = syncTrigger;

        function syncTrigger() {
            const opt = select.options[select.selectedIndex];
            trigger.textContent = opt ? opt.text : '';
            wrapper.classList.toggle('cs-disabled', select.disabled);
            trigger.disabled = select.disabled;
            trigger.setAttribute('aria-disabled', String(select.disabled));
            trigger.setAttribute('tabindex', select.disabled ? '-1' : '0');
            // Mettre à jour la classe selected dans le panel
            panel.querySelectorAll('.cs-option').forEach((item) => {
                const selected = Number(item.dataset.idx) === select.selectedIndex;
                item.classList.toggle('cs-option--selected', selected);
                item.setAttribute('aria-selected', String(selected));
            });
        }

        /* ── Ouverture / fermeture ────────────────────────────────── */
        function open() {
            if (select.disabled) return;
            if (isOpen) return;
            document.dispatchEvent(new CustomEvent('cs:close-all', { detail: wrapper }));
            isOpen = true;
            wrapper.classList.add('cs-open');
            trigger.setAttribute('aria-expanded', 'true');
            renderOptions();
            // Ouvrir vers le haut si pas de place en bas
            const rect = wrapper.getBoundingClientRect();
            const spaceBelow = window.innerHeight - rect.bottom;
            if (spaceBelow < 210) {
                panel.style.top    = 'auto';
                panel.style.bottom = 'calc(100% + 4px)';
            } else {
                panel.style.top    = 'calc(100% + 4px)';
                panel.style.bottom = 'auto';
            }
        }

        function close() {
            if (!isOpen) return;
            isOpen = false;
            wrapper.classList.remove('cs-open');
            trigger.setAttribute('aria-expanded', 'false');
        }

        /* ── Événements ──────────────────────────────────────────── */
        trigger.addEventListener('click', (e) => {
            e.stopPropagation();
            if (select.disabled) return;
            isOpen ? close() : open();
        });

        trigger.addEventListener('keydown', (e) => {
            if (select.disabled) return;
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                isOpen ? close() : open();
            } else if (e.key === 'Escape') {
                close();
            } else if (e.key === 'ArrowDown') {
                e.preventDefault();
                const next = Math.min(select.selectedIndex + 1, select.options.length - 1);
                select.selectedIndex = next;
                select.dispatchEvent(new Event('change', { bubbles: true }));
                syncTrigger();
                if (!isOpen) open();
            } else if (e.key === 'ArrowUp') {
                e.preventDefault();
                const prev = Math.max(select.selectedIndex - 1, 0);
                select.selectedIndex = prev;
                select.dispatchEvent(new Event('change', { bubbles: true }));
                syncTrigger();
                if (!isOpen) open();
            }
        });

        document.addEventListener('click', (e) => {
            if (!wrapper.contains(e.target)) close();
        });

        panel.addEventListener('pointerdown', (e) => {
            const index = optionDepuisEvenement(e);
            if (index == null || Number.isNaN(index)) return;
            e.preventDefault();
            e.stopPropagation();
            choisirIndex(index);
        }, true);

        panel.addEventListener('click', (e) => {
            const index = optionDepuisEvenement(e);
            if (index == null || Number.isNaN(index)) return;
            e.preventDefault();
            e.stopPropagation();
            choisirIndex(index);
        }, true);

        document.addEventListener('cs:close-all', (e) => {
            if (e.detail !== wrapper) close();
        });

        select.addEventListener('change', syncTrigger);

        // Observer: re-sync si les <option> changent dynamiquement
        new MutationObserver(() => {
            renderOptions();
            syncTrigger();
        }).observe(select, { childList: true, subtree: true, attributes: true });

        // Init initiale
        renderOptions();
        syncTrigger();
    }

    /** Initialise tous les select.form-controle dans un conteneur donné */
    function initAll(root) {
        (root || document).querySelectorAll('select.form-controle:not([data-cs-init])').forEach(initCustomSelect);
    }

    function start() {
        initAll();

        if (document.body && !document.body.dataset.csObserver) {
            document.body.dataset.csObserver = '1';
            new MutationObserver((mutations) => {
                mutations.forEach((mutation) => {
                    mutation.addedNodes.forEach((node) => {
                        if (node.nodeType === Node.ELEMENT_NODE) initAll(node);
                    });
                });
            }).observe(document.body, { childList: true, subtree: true });
        }
    }

    // Auto-init au chargement de la page, puis une passe courte pour les scripts diffÃ©rÃ©s
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', start);
    } else {
        start();
    }
    window.addEventListener('load', start);
    setTimeout(start, 0);

    // Exposition globale pour les selects créés dynamiquement
    window.initCustomSelects  = initAll;
    window.initCustomSelect   = initCustomSelect;
    window.syncCustomSelect   = function (select) {
        if (select && typeof select._csSync === 'function') select._csSync();
    };
})();
