(function () {
    // shrink the sticky header once the page is scrolled; the asymmetric
    // thresholds prevent oscillation caused by the header height change
    const header = document.querySelector('.header');
    if (header) {
        let compact = false;
        const onScroll = function () {
            if (!compact && window.scrollY > 140) {
                compact = true;
                header.classList.add('compact');
            } else if (compact && window.scrollY < 30) {
                compact = false;
                header.classList.remove('compact');
            }
        };
        window.addEventListener('scroll', onScroll, {passive: true});
        onScroll();
    }

    document.querySelectorAll('.examples').forEach(function (examples, groupIndex) {
        const tabs = Array.from(examples.querySelectorAll('[role=tab]'));
        const panels = Array.from(examples.querySelectorAll('[role=tabpanel]'));

        // give each tab and panel a stable id and wire the aria relationships,
        // so assistive tech knows which panel a tab controls and vice versa
        tabs.forEach(function (tab, index) {
            const panel = panels[index];
            tab.id = tab.id || ('example-tab-' + groupIndex + '-' + index);
            if (panel) {
                panel.id = panel.id || ('example-panel-' + groupIndex + '-' + index);
                tab.setAttribute('aria-controls', panel.id);
                panel.setAttribute('aria-labelledby', tab.id);
                if (!panel.hasAttribute('tabindex')) panel.tabIndex = 0;
            }
        });

        // select a tab: update aria-selected, the roving tabindex (only the
        // selected tab stays in the page tab sequence) and the visible panel,
        // optionally moving focus to the tab that was just selected
        const select = function (index, focus) {
            tabs.forEach(function (t, i) {
                const on = i === index;
                t.setAttribute('aria-selected', String(on));
                t.tabIndex = on ? 0 : -1;
            });
            panels.forEach(function (p, i) { p.hidden = i !== index; });
            if (focus) tabs[index].focus();
        };

        tabs.forEach(function (tab, index) {
            tab.addEventListener('click', function () { select(index, false); });
            // left/right arrows move between tabs with wrap-around and Home/End
            // jump to the ends; up/down are left alone so the page can scroll.
            // the tab activates as focus lands on it
            tab.addEventListener('keydown', function (event) {
                let next;
                switch (event.key) {
                    case 'ArrowRight': next = (index + 1) % tabs.length; break;
                    case 'ArrowLeft': next = (index - 1 + tabs.length) % tabs.length; break;
                    case 'Home': next = 0; break;
                    case 'End': next = tabs.length - 1; break;
                    default: return;
                }
                event.preventDefault();
                select(next, true);
            });
        });

        // establish the initial roving tabindex from the pre-selected tab
        const current = tabs.findIndex(function (t) { return t.getAttribute('aria-selected') === 'true'; });
        select(current < 0 ? 0 : current, false);

        // allow linking to a specific example tab, like #sse or #type-safe-links
        const slug = function (text) { return text.trim().toLowerCase().replace(/\s+/g, '-'); };
        const activateFromHash = function () {
            const hash = window.location.hash.slice(1).toLowerCase();
            if (!hash) return;
            const index = tabs.findIndex(function (t) { return slug(t.textContent) === hash; });
            if (index >= 0) {
                tabs[index].click();
                examples.scrollIntoView();
            }
        };
        window.addEventListener('hashchange', activateFromHash);
        activateFromHash();
    });

    // htmx swaps in fresh markup that Prism hasn't seen yet, so re-highlight
    // any code it just inserted (used by the live bld build file configurator)
    document.body.addEventListener('htmx:afterSwap', function (event) {
        if (window.Prism) {
            window.Prism.highlightAllUnder(event.target);
        }
    });

    // dynamic example highlighting: while a demo is open on the visible tab,
    // mark the lines of its example that match the demo's current state, and
    // clear them when the demo closes, its state has no mapping, or the tab hides
    (function () {
        var controllers = [];
        document.querySelectorAll('details.demo-inline[data-highlight-field]').forEach(function (details) {
            var panel = details.closest('[role=tabpanel]');
            var pre = panel && panel.querySelector('pre[data-src]');
            if (!pre) return;
            var field = details.getAttribute('data-highlight-field');
            var apply = function () {
                pre.querySelectorAll('.line-highlight').forEach(function (el) { el.remove(); });
                pre.removeAttribute('data-line');
                if (!details.open || panel.hidden) return;
                var select = details.querySelector('[name="' + field + '"]');
                var lines = select && details.getAttribute('data-highlight-' + select.value);
                if (!lines) return;
                pre.setAttribute('data-line', lines);
                if (window.Prism && Prism.plugins && Prism.plugins.lineHighlight) {
                    Prism.plugins.lineHighlight.highlightLines(pre)();
                }
            };
            details.addEventListener('toggle', apply);
            // fires when the demo first loads and on every state change swap
            details.addEventListener('htmx:afterSwap', apply);
            controllers.push(apply);
        });
        if (controllers.length) {
            document.querySelectorAll('.example-tabs [role=tab]').forEach(function (tab) {
                tab.addEventListener('click', function () {
                    controllers.forEach(function (apply) { apply(); });
                });
            });
        }
    })();

    // the JSON demo calls the real API endpoint and shows the exchange: the
    // request that goes out and the application/json body that comes back
    document.querySelectorAll('.demo-json-live').forEach(function (stage) {
        var endpoint = stage.getAttribute('data-endpoint');
        var input = stage.querySelector('input[name="name"]');
        var reqOut = stage.querySelector('.demo-json-req');
        var ctOut = stage.querySelector('.demo-json-ct');
        var bodyOut = stage.querySelector('.demo-json');
        var details = stage.closest('details');
        var timer = null;
        var seq = 0;

        var run = function () {
            var mine = ++seq;
            var url = endpoint + '?name=' + encodeURIComponent(input.value);
            // show just the path, the way an API consumer thinks of the endpoint
            var display = url;
            try { var parsed = new URL(url, window.location.href); display = parsed.pathname + parsed.search; } catch (e) {}
            reqOut.textContent = 'GET ' + display;
            fetch(url, {headers: {'Accept': 'application/json'}}).then(function (response) {
                var ct = response.status + ' ' + (response.headers.get('content-type') || '');
                return response.text().then(function (text) { return {ct: ct, text: text}; });
            }).then(function (result) {
                // ignore a slow response that a newer keystroke has already superseded
                if (mine !== seq) return;
                ctOut.textContent = result.ct;
                bodyOut.textContent = result.text;
            }).catch(function () {
                if (mine !== seq) return;
                ctOut.textContent = '';
                bodyOut.textContent = 'the request could not be reached';
            });
        };
        var schedule = function () {
            if (timer) clearTimeout(timer);
            timer = setTimeout(run, 250);
        };

        input.addEventListener('input', schedule);
        // fetch once when the demo is first opened, and whenever it reopens empty
        if (details) {
            details.addEventListener('toggle', function () {
                if (details.open && !bodyOut.textContent) run();
            });
        }
        if (details && details.open) run();
    });

    // the get-started command differs per platform, so default to the visitor's
    // OS but keep both switchable: detection can't tell WSL or Git Bash apart
    // from a native Windows shell, and people copy commands for other machines
    const osSwitch = document.querySelector('.os-switch');
    if (osSwitch) {
        const osTabs = Array.from(osSwitch.querySelectorAll('[role=tab]'));
        const osPanels = Array.from(document.querySelectorAll('.os-panel'));
        const selectOs = function (os, focus) {
            osTabs.forEach(function (tab) {
                const on = tab.dataset.os === os;
                tab.setAttribute('aria-selected', String(on));
                tab.tabIndex = on ? 0 : -1;
                if (on && focus) tab.focus();
            });
            osPanels.forEach(function (panel) { panel.hidden = panel.dataset.os !== os; });
        };

        osTabs.forEach(function (tab, index) {
            tab.addEventListener('click', function () { selectOs(tab.dataset.os, false); });
            tab.addEventListener('keydown', function (event) {
                let next;
                switch (event.key) {
                    case 'ArrowRight': next = (index + 1) % osTabs.length; break;
                    case 'ArrowLeft': next = (index - 1 + osTabs.length) % osTabs.length; break;
                    case 'Home': next = 0; break;
                    case 'End': next = osTabs.length - 1; break;
                    default: return;
                }
                event.preventDefault();
                selectOs(osTabs[next].dataset.os, true);
            });
        });

        const platform = (navigator.userAgentData && navigator.userAgentData.platform) || navigator.platform || '';
        if (/win/i.test(platform)) selectOs('windows', false);
    }

    document.querySelectorAll('.install .copy').forEach(function (btn) {
        btn.addEventListener('click', function () {
            const command = btn.parentNode.querySelector('code').textContent;
            const showCopied = function () {
                btn.textContent = 'Copied!';
                setTimeout(function () { btn.textContent = 'Copy'; }, 2000);
            };
            // selection fallback for contexts where the async clipboard API is unavailable
            const fallback = function () {
                const area = document.createElement('textarea');
                area.value = command;
                area.style.position = 'fixed';
                area.style.opacity = '0';
                document.body.appendChild(area);
                area.select();
                try {
                    if (document.execCommand('copy')) {
                        showCopied();
                    }
                } finally {
                    area.remove();
                }
            };
            if (navigator.clipboard && navigator.clipboard.writeText) {
                navigator.clipboard.writeText(command).then(showCopied, fallback);
            } else {
                fallback();
            }
        });
    });

    // the Query Manager demo fires an HX-Trigger "bookSaved" event from the
    // server alongside its swap; htmx dispatches it with the saved record as
    // the event detail, and we flash a small toast, a response-side helper at
    // work with no extra request and no polling
    document.body.addEventListener('bookSaved', function (event) {
        var detail = event.detail || {};
        var toast = document.createElement('div');
        toast.className = 'demo-toast';
        toast.textContent = 'Saved “' + (detail.title || 'book') + '” as #' + (detail.id != null ? detail.id : '');
        document.body.appendChild(toast);
        // the toast plays a one-shot flash animation, then removes itself
        setTimeout(function () { toast.remove(); }, 2700);
    });
})();
