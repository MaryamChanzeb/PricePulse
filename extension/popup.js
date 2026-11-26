// Price Pulse popup. It never calls the server itself: it asks background.js with messages.
// All text goes into the page with textContent, never innerHTML (product titles come from store pages).

const VIEW_IDS = ['view-loading', 'view-offline', 'view-login', 'view-account'];
const PANELS = { page: 'panel-page', watchlist: 'panel-watchlist', alerts: 'panel-alerts' };
const PASSWORD_RULE = /^(?=.*\d).{8,}$/; // FR-01: at least 8 characters and one digit
const EMAIL_SHAPE = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;
const MAX_ALERTS_SHOWN = 10;

// Verdict -> word and colour class (SDS 9). Same mapping as the badge on the page.
const VERDICTS = {
  GENUINE:                { label: 'Genuine Deal',        css: 'pp-verdict-genuine' },
  INFLATED_ORIGINAL:      { label: 'Inflated Discount',   css: 'pp-verdict-inflated' },
  PRICE_HIKE_BEFORE_SALE: { label: 'Inflated Discount',   css: 'pp-verdict-inflated' },
  PERPETUAL_SALE:         { label: 'Permanent Sale',      css: 'pp-verdict-perpetual' },
  NOT_ENOUGH_DATA:        { label: 'Not enough data yet', css: 'pp-verdict-nodata' }
};

let currentProductId = null;

function byId(id) {
  return document.getElementById(id);
}

function showView(viewId) {
  for (const id of VIEW_IDS) {
    byId(id).hidden = (id !== viewId);
  }
  hideError();
}

function showError(text) {
  const box = byId('popup-error');
  box.textContent = text;
  box.hidden = false;
}

function hideError() {
  const box = byId('popup-error');
  box.textContent = '';
  box.hidden = true;
}

function rupees(value) {
  if (value === null || value === undefined) {
    return '-';
  }
  return 'Rs ' + Number(value).toLocaleString('en-US', { maximumFractionDigits: 2 });
}

function percent(value) {
  return Math.round(Number(value)) + '%';
}

function verdictInfo(verdict) {
  return VERDICTS[verdict] || VERDICTS.NOT_ENOUGH_DATA;
}

// Only real web addresses become links (a "javascript:" address would be a security hole).
function safeUrl(url) {
  return typeof url === 'string' && /^https?:\/\//i.test(url) ? url : null;
}

// Sends a message to background.js and always resolves with a reply object.
function sendToBackground(message) {
  return new Promise((resolve) => {
    chrome.runtime.sendMessage(message, (reply) => {
      if (chrome.runtime.lastError || !reply) {
        resolve({ ok: false, error: 'invalid', message: 'Price Pulse could not start. Reload the extension.' });
        return;
      }
      resolve(reply);
    });
  });
}

// Shared handling of a failed reply: offline view, back to login, or a message.
function handleFailure(reply) {
  if (reply.error === 'offline') {
    showView('view-offline');
  } else if (reply.error === 'auth') {
    showView('view-login');
    showLoginTab('login');
    showError(reply.message);
  } else {
    showError(reply.message);
  }
}

// ---------- start-up ----------

async function init() {
  showView('view-loading');

  const ping = await sendToBackground({ type: 'PING' });
  if (!ping.ok) {
    if (ping.error === 'offline') {
      showView('view-offline'); // FR-42
    } else {
      showView('view-login');
      showError(ping.message);
    }
    return;
  }

  const session = await sendToBackground({ type: 'GET_SESSION' });
  if (session.ok && session.data.loggedIn) {
    await showAccount(session.data);
  } else {
    showView('view-login'); // FR-09: ask to log in before anything is sent
    showLoginTab('login');
  }
}

// ---------- log in / sign up ----------

function showLoginTab(name) {
  const loginActive = (name === 'login');
  byId('tab-login').setAttribute('aria-selected', String(loginActive));
  byId('tab-signup').setAttribute('aria-selected', String(!loginActive));
  byId('form-login').hidden = !loginActive;
  byId('form-signup').hidden = loginActive;
  hideError();
}

function setBusy(form, busy) {
  for (const element of form.elements) {
    element.disabled = busy;
  }
}

async function handleAuthReply(reply) {
  if (reply.ok) {
    await showAccount(reply.data);
    return;
  }
  if (reply.error === 'offline') {
    showView('view-offline');
    return;
  }
  showError(reply.message); // e.g. "Invalid email or password" or "This email is already registered"
}

async function onLoginSubmit(event) {
  event.preventDefault();
  const form = byId('form-login');
  const email = byId('login-email').value.trim();
  const password = byId('login-password').value;
  if (!email || !password) {
    showError('Enter your email and password.');
    return;
  }
  hideError();
  setBusy(form, true);
  const reply = await sendToBackground({ type: 'LOGIN', email: email, password: password });
  setBusy(form, false);
  await handleAuthReply(reply);
}

async function onSignupSubmit(event) {
  event.preventDefault();
  const form = byId('form-signup');
  const name = byId('signup-name').value.trim();
  const email = byId('signup-email').value.trim();
  const password = byId('signup-password').value;

  // Same checks as the server, so the shopper gets the message at once. The server checks again.
  if (!name) {
    showError('Name is required');
    return;
  }
  if (!EMAIL_SHAPE.test(email)) {
    showError('Enter a valid email address');
    return;
  }
  if (!PASSWORD_RULE.test(password)) {
    showError('Password must have at least 8 characters and at least one digit');
    return;
  }
  hideError();
  setBusy(form, true);
  const reply = await sendToBackground({ type: 'SIGNUP', name: name, email: email, password: password });
  setBusy(form, false);
  await handleAuthReply(reply);
}

function linkShowPassword(checkboxId, inputId) {
  byId(checkboxId).addEventListener('change', (event) => {
    byId(inputId).type = event.target.checked ? 'text' : 'password';
  });
}

async function onLogout() {
  await sendToBackground({ type: 'LOGOUT' });
  await init();
}

// ---------- logged in ----------

async function showAccount(user) {
  byId('account-name').textContent = user.name || '';
  byId('account-email').textContent = user.email || '';
  showView('view-account');
  showPanel('page');
  await Promise.all([loadPageVerdict(), refreshUnreadCount()]);
}

function showPanel(name) {
  for (const key of Object.keys(PANELS)) {
    byId(PANELS[key]).hidden = (key !== name);
    byId('tab-' + key).setAttribute('aria-selected', String(key === name));
  }
  hideError();
  if (name === 'watchlist') {
    loadWatchlist();
  } else if (name === 'alerts') {
    loadAlerts();
  }
}

function activeTabId() {
  return new Promise((resolve) => {
    chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
      resolve(tabs && tabs[0] ? tabs[0].id : null);
    });
  });
}

// "This page": the verdict content.js got for the current tab (FR-22).
async function loadPageVerdict() {
  const tabId = await activeTabId();
  const reply = await sendToBackground({ type: 'GET_PAGE_VERDICT', tabId: tabId });
  const result = reply.ok ? reply.data : null;
  if (!result || !result.verdict) {
    currentProductId = null;
    byId('page-empty').hidden = false;
    byId('page-verdict').hidden = true;
    return;
  }
  currentProductId = result.productId;
  const verdict = result.verdict;
  const info = verdictInfo(verdict.verdict);

  byId('page-empty').hidden = true;
  byId('page-verdict').hidden = false;
  byId('page-title').textContent = result.productTitle || '';
  const box = byId('verdict-box');
  box.className = 'pp-verdict ' + info.css;
  box.textContent = info.label;

  const parts = [];
  if (verdict.claimedDiscountPct !== null && verdict.claimedDiscountPct !== undefined) {
    parts.push('Claimed ' + percent(verdict.claimedDiscountPct));
  }
  if (verdict.realDiscountPct !== null && verdict.realDiscountPct !== undefined) {
    parts.push('Real ' + percent(verdict.realDiscountPct));
  }
  byId('verdict-numbers').textContent = parts.join(' | ');
  byId('verdict-reason').textContent = verdict.reason || '';
  byId('watch-result').hidden = true;
}

// FR-24: Watch button.
async function onWatchSubmit(event) {
  event.preventDefault();
  const target = Number(byId('watch-target').value);
  if (!currentProductId) {
    showError('Open a product page first.');
    return;
  }
  if (!Number.isFinite(target) || target <= 0) {
    showError('Target price must be greater than zero');
    return;
  }
  hideError();
  const form = byId('form-watch');
  setBusy(form, true);
  const reply = await sendToBackground({ type: 'WATCH', productId: currentProductId, targetPrice: target });
  setBusy(form, false);
  if (reply.ok) {
    const result = byId('watch-result');
    result.textContent = 'Watching. You will get one alert when the price is at or below ' + rupees(target) + '.';
    result.hidden = false;
    byId('watch-target').value = '';
  } else {
    handleFailure(reply); // e.g. "Already in your watchlist" or "Watchlist limit reached (20)"
  }
}

// ---------- watchlist (short version of the dashboard, FR-33) ----------

async function loadWatchlist() {
  const list = byId('watchlist-items');
  list.replaceChildren();
  byId('watchlist-summary').textContent = 'Loading...';
  const reply = await sendToBackground({ type: 'GET_WATCHLIST' });
  if (!reply.ok) {
    byId('watchlist-summary').textContent = '';
    handleFailure(reply);
    return;
  }
  const items = reply.data || [];
  byId('watchlist-summary').textContent = items.length === 0
    ? 'Your watchlist is empty. Use the Watch button on a product page.'
    : 'Watching ' + items.length + ' of 20 products.';
  for (const item of items) {
    list.appendChild(watchItemElement(item));
  }
}

function watchItemElement(item) {
  const li = document.createElement('li');
  const head = document.createElement('div');
  head.className = 'pp-item-head';

  const url = safeUrl(item.url);
  const title = document.createElement(url ? 'a' : 'span');
  title.className = 'pp-item-title';
  title.textContent = item.title;
  if (url) {
    title.href = url;
    title.target = '_blank';
    title.rel = 'noopener';
  }

  const remove = document.createElement('button');
  remove.type = 'button';
  remove.className = 'pp-remove';
  remove.textContent = '×';
  remove.title = 'Remove from watchlist';
  remove.setAttribute('aria-label', 'Remove ' + item.title + ' from watchlist');
  remove.addEventListener('click', async () => {
    remove.disabled = true;
    const reply = await sendToBackground({ type: 'REMOVE_WATCH', itemId: item.id });
    if (reply.ok) {
      await loadWatchlist();
    } else {
      remove.disabled = false;
      handleFailure(reply);
    }
  });
  head.append(title, remove);

  const meta = document.createElement('div');
  meta.className = 'pp-item-meta';
  meta.textContent = item.storeName + ' | Now ' + rupees(item.currentPrice) + ' | Target ' + rupees(item.targetPrice);

  const info = verdictInfo(item.verdict);
  const tag = document.createElement('span');
  tag.className = 'pp-tag ' + info.css;
  tag.textContent = info.label;

  li.append(head, meta, tag);
  return li;
}

// ---------- notifications (FR-31, FR-32) ----------

async function refreshUnreadCount() {
  const reply = await sendToBackground({ type: 'REFRESH_BADGE' });
  const badge = byId('alerts-count');
  const count = reply.ok && reply.data ? reply.data.count : 0;
  badge.textContent = String(count);
  badge.hidden = count === 0;
}

async function loadAlerts() {
  const list = byId('alert-items');
  list.replaceChildren();
  const reply = await sendToBackground({ type: 'GET_NOTIFICATIONS' });
  if (!reply.ok) {
    handleFailure(reply);
    return;
  }
  const notifications = (reply.data || []).slice(0, MAX_ALERTS_SHOWN);
  byId('alerts-empty').hidden = notifications.length > 0;
  for (const notification of notifications) {
    list.appendChild(alertElement(notification));
  }
}

function alertElement(notification) {
  const li = document.createElement('li');
  const text = document.createElement('div');
  text.textContent = notification.message;
  if (!notification.read) {
    text.className = 'pp-unread';
  }
  const when = document.createElement('div');
  when.className = 'pp-item-meta';
  when.textContent = new Date(notification.createdAt).toLocaleString();
  li.append(text, when);

  if (!notification.read) {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'pp-small-button';
    button.textContent = 'Mark as read';
    button.addEventListener('click', async () => {
      button.disabled = true;
      const reply = await sendToBackground({ type: 'MARK_READ', notificationId: notification.id });
      if (reply.ok) {
        await Promise.all([loadAlerts(), refreshUnreadCount()]);
      } else {
        button.disabled = false;
        handleFailure(reply);
      }
    });
    li.appendChild(button);
  }
  return li;
}

document.addEventListener('DOMContentLoaded', () => {
  byId('btn-retry').addEventListener('click', init);
  byId('btn-logout').addEventListener('click', onLogout);
  byId('tab-login').addEventListener('click', () => showLoginTab('login'));
  byId('tab-signup').addEventListener('click', () => showLoginTab('signup'));
  byId('form-login').addEventListener('submit', onLoginSubmit);
  byId('form-signup').addEventListener('submit', onSignupSubmit);
  byId('form-watch').addEventListener('submit', onWatchSubmit);
  byId('tab-page').addEventListener('click', () => showPanel('page'));
  byId('tab-watchlist').addEventListener('click', () => showPanel('watchlist'));
  byId('tab-alerts').addEventListener('click', () => showPanel('alerts'));
  linkShowPassword('login-show-password', 'login-password');
  linkShowPassword('signup-show-password', 'signup-password');
  init();
});
