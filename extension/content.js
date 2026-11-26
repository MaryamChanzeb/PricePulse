// Price Pulse content script (SDS 3.4). Runs only on the pages listed in manifest.json (FR-08).
//
// 1. Pick the adapter for this site (polymorphism: every adapter is used through the BaseAdapter methods).
// 2. Read the product. If a value is missing, show "Could not read this page" instead of sending a guess.
// 3. Ask background.js to send the observation (only background.js talks to the server).
// 4. Draw the verdict badge next to the price (FR-22), inside a Shadow DOM so the store's CSS cannot change it.
// Every step is wrapped so that a failure only hides the badge and never breaks the store page (SDS 8).

(function () {
  'use strict';

  const ADAPTER_CLASSES = [DarazAdapter, DemoStoreAdapter];
  const READ_ATTEMPTS = 6;          // pages that build the price with JavaScript need a moment
  const READ_INTERVAL_MS = 300;     // 6 x 300 ms stays inside the 2 second goal of FR-22
  const URL_CHECK_MS = 1000;        // some stores change product without reloading the page

  // Verdict -> colour and words (SDS 9). The word is always shown with the colour.
  const BADGE_STYLES = {
    GENUINE:                { label: 'Genuine Deal',         background: '#DCFCE7', text: '#166534', border: '#16A34A' },
    INFLATED_ORIGINAL:      { label: 'Inflated Discount',    background: '#FEE2E2', text: '#991B1B', border: '#DC2626' },
    PRICE_HIKE_BEFORE_SALE: { label: 'Inflated Discount',    background: '#FEE2E2', text: '#991B1B', border: '#DC2626' },
    PERPETUAL_SALE:         { label: 'Permanent Sale',       background: '#FEF3C7', text: '#92400E', border: '#F59E0B' },
    NOT_ENOUGH_DATA:        { label: 'Not enough data yet',  background: '#E5E7EB', text: '#374151', border: '#9CA3AF' },
    INFO:                   { label: '',                     background: '#F0FDFA', text: '#115E59', border: '#0F766E' }
  };

  let badgeHost = null;
  let handledUrl = null;

  function pickAdapter() {
    for (const AdapterClass of ADAPTER_CLASSES) {
      if (AdapterClass.matches(window.location)) {
        return new AdapterClass(document, window.location);
      }
    }
    return null;
  }

  function sendToBackground(message) {
    return new Promise((resolve) => {
      try {
        chrome.runtime.sendMessage(message, (reply) => {
          if (chrome.runtime.lastError || !reply) {
            resolve({ ok: false, error: 'offline', message: 'Price Pulse is offline' });
            return;
          }
          resolve(reply);
        });
      } catch (extensionReloaded) {
        resolve({ ok: false, error: 'offline', message: 'Price Pulse is offline' });
      }
    });
  }

  function wait(ms) {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }

  // Tries a few times, because some pages fill in the price after loading.
  async function readWithRetries(adapter) {
    let reading = adapter.readObservation();
    for (let attempt = 1; attempt < READ_ATTEMPTS && !reading.ok; attempt++) {
      await wait(READ_INTERVAL_MS);
      reading = adapter.readObservation();
    }
    return reading;
  }

  // ---------- badge ----------

  function removeBadge() {
    if (badgeHost && badgeHost.parentNode) {
      badgeHost.parentNode.removeChild(badgeHost);
    }
    badgeHost = null;
  }

  // style: a key of BADGE_STYLES; heading and lines: plain text (set with textContent, never innerHTML).
  function showBadge(adapter, styleKey, heading, lines) {
    removeBadge();
    const style = BADGE_STYLES[styleKey] || BADGE_STYLES.INFO;

    badgeHost = document.createElement('div');
    badgeHost.setAttribute('data-price-pulse', 'badge');
    const shadow = badgeHost.attachShadow({ mode: 'closed' });

    const css = document.createElement('style');
    css.textContent =
      '.pp{all:initial;display:block;box-sizing:border-box;max-width:420px;margin:10px 0;padding:10px 12px;' +
      'border-radius:8px;border:2px solid ' + style.border + ';background:' + style.background + ';color:' + style.text + ';' +
      'font:14px/1.4 "Segoe UI",Arial,sans-serif;}' +
      '.pp-head{display:block;font-weight:700;margin-bottom:4px;}' +
      '.pp-line{display:block;margin-top:2px;}' +
      '.pp-floating{position:fixed;top:16px;right:16px;z-index:2147483647;box-shadow:0 4px 16px rgba(0,0,0,.2);}';
    shadow.appendChild(css);

    const box = document.createElement('div');
    box.className = 'pp';
    box.setAttribute('role', 'status');
    const head = document.createElement('span');
    head.className = 'pp-head';
    head.textContent = heading;
    box.appendChild(head);
    for (const line of lines) {
      if (!line) {
        continue;
      }
      const span = document.createElement('span');
      span.className = 'pp-line';
      span.textContent = line;
      box.appendChild(span);
    }
    shadow.appendChild(box);

    const priceElement = adapter.getPriceElement();
    if (priceElement && priceElement.parentNode) {
      priceElement.parentNode.insertBefore(badgeHost, priceElement.nextSibling);
    } else {
      box.classList.add('pp-floating'); // no price element found: float at the top right instead
      document.documentElement.appendChild(badgeHost);
    }
  }

  function percentText(value) {
    if (value === null || value === undefined) {
      return null;
    }
    return Math.round(Number(value)) + '%';
  }

  function showVerdict(adapter, verdict) {
    const style = BADGE_STYLES[verdict.verdict] ? verdict.verdict : 'NOT_ENOUGH_DATA';
    const real = percentText(verdict.realDiscountPct);
    const claimed = percentText(verdict.claimedDiscountPct);
    let numbers = null;
    if (real !== null && claimed !== null) {
      numbers = 'Claimed ' + claimed + ' off | Real discount about ' + real + ' (vs 30-day median)';
    } else if (real !== null) {
      numbers = 'Real discount about ' + real + ' (vs 30-day median)';
    }
    showBadge(adapter, style, 'PRICE PULSE: ' + BADGE_STYLES[style].label, [numbers, verdict.reason]);
  }

  // ---------- main ----------

  async function checkPage() {
    try {
      if (window.location.href === handledUrl) {
        return;
      }
      handledUrl = window.location.href;

      const adapter = pickAdapter();
      if (!adapter || !adapter.isProductPage()) {
        removeBadge();
        return; // not a product page: do nothing (FR-08)
      }

      const reading = await readWithRetries(adapter);
      if (!reading.ok) {
        showBadge(adapter, 'INFO', 'PRICE PULSE', ['Could not read this page.']);
        return;
      }

      const reply = await sendToBackground({ type: 'OBSERVE', observation: reading.data });
      if (reply.ok && reply.data && reply.data.verdict) {
        showVerdict(adapter, reply.data.verdict);
      } else if (reply.error === 'auth') {
        showBadge(adapter, 'INFO', 'PRICE PULSE', ['Log in to Price Pulse (toolbar icon) to see if this discount is real.']); // FR-09
      } else if (reply.error === 'invalid') {
        showBadge(adapter, 'INFO', 'PRICE PULSE', [reply.message]);
      } else {
        removeBadge(); // offline: never disturb the page (FR-42); the popup shows "Price Pulse is offline"
      }
    } catch (unexpected) {
      removeBadge();
    }
  }

  checkPage();
  setInterval(checkPage, URL_CHECK_MS);
})();
