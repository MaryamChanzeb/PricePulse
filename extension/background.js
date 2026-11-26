// Price Pulse background service worker (SDS 3.4).
// This is the ONLY part of the extension that talks to the server and holds the login token.
// popup.js and content.js send it messages; every reply has one of two shapes:
//   { ok: true,  data: ... }
//   { ok: false, error: 'offline' | 'auth' | 'invalid', message: 'text for the user' }

const API_ORIGIN = 'http://localhost:8080';
const API_BASE = API_ORIGIN + '/api';
const REQUEST_TIMEOUT_MS = 5000;
const SESSION_KEY = 'session';
const OFFLINE_MESSAGE = 'Price Pulse is offline'; // FR-42

// ---------- reply helpers ----------

function success(data) {
  return { ok: true, data: data === undefined ? null : data };
}

function failure(error, message) {
  return { ok: false, error: error, message: message };
}

// ---------- session (token) storage ----------

async function readSession() {
  const stored = await chrome.storage.local.get(SESSION_KEY);
  return stored[SESSION_KEY] || null;
}

async function saveSession(session) {
  await chrome.storage.local.set({ [SESSION_KEY]: session });
}

async function clearSession() {
  await chrome.storage.local.remove(SESSION_KEY);
}

// ---------- network ----------

// Calls the REST API and turns every outcome into a reply object.
// A network failure or timeout means the server cannot be reached -> 'offline'.
async function apiRequest(path, options) {
  const settings = options || {};
  const headers = { 'Accept': 'application/json' };
  if (settings.body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  if (settings.auth) {
    const session = await readSession();
    if (!session || !session.token) {
      return failure('auth', 'Please log in to Price Pulse.');
    }
    headers['Authorization'] = 'Bearer ' + session.token;
  }

  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
  let response;
  try {
    response = await fetch(API_BASE + path, {
      method: settings.method || 'GET',
      headers: headers,
      body: settings.body === undefined ? undefined : JSON.stringify(settings.body),
      signal: controller.signal
    });
  } catch (networkError) {
    return failure('offline', OFFLINE_MESSAGE);
  } finally {
    clearTimeout(timer);
  }

  const body = await readJson(response);
  if (response.ok) {
    return success(body);
  }
  const message = body && body.message ? body.message : 'Request failed (' + response.status + ')';
  if (response.status === 401 && settings.auth) {
    await clearSession(); // token expired or invalid: the user must log in again
    return failure('auth', 'Your session has ended. Please log in again.');
  }
  if (response.status === 401) {
    return failure('auth', message);
  }
  if (response.status >= 500) {
    return failure('offline', message);
  }
  return failure('invalid', message);
}

async function readJson(response) {
  try {
    const text = await response.text();
    return text ? JSON.parse(text) : null;
  } catch (parseError) {
    return null;
  }
}

// ---------- message handlers ----------

// Is the server reachable? Any HTTP answer counts as "online".
async function handlePing() {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
  try {
    await fetch(API_ORIGIN + '/', { method: 'GET', signal: controller.signal });
    return success({ online: true });
  } catch (networkError) {
    return failure('offline', OFFLINE_MESSAGE);
  } finally {
    clearTimeout(timer);
  }
}

async function handleGetSession() {
  const session = await readSession();
  if (!session) {
    return success({ loggedIn: false });
  }
  return success({ loggedIn: true, email: session.email, name: session.name, role: session.role });
}

async function handleLogout() {
  await clearSession();
  await chrome.action.setBadgeText({ text: '' });
  return success({ loggedIn: false });
}

// FR-04, FR-05: log in with email and password; the server answers with a token.
async function handleLogin(message) {
  const email = String(message.email || '').trim();
  const password = String(message.password || '');
  if (!email || !password) {
    return failure('invalid', 'Enter your email and password.');
  }
  const reply = await apiRequest('/auth/login', { method: 'POST', body: { email: email, password: password } });
  return finishLogin(reply);
}

// FR-01 to FR-03: create a shopper account; the server logs the new user in straight away.
async function handleSignup(message) {
  const name = String(message.name || '').trim();
  const email = String(message.email || '').trim();
  const password = String(message.password || '');
  if (!name || !email || !password) {
    return failure('invalid', 'Enter your name, email and password.');
  }
  const reply = await apiRequest('/auth/signup', {
    method: 'POST',
    body: { name: name, email: email, password: password }
  });
  return finishLogin(reply);
}

// Keeps the token only for shoppers: the extension is a shopper tool (SDS 6 roles).
async function finishLogin(reply) {
  if (!reply.ok) {
    return reply;
  }
  const data = reply.data;
  if (!data || !data.token) {
    return failure('invalid', 'Unexpected answer from the Price Pulse server.');
  }
  if (data.role !== 'SHOPPER') {
    return failure('invalid', 'Admin accounts use the web dashboard, not the extension.');
  }
  await saveSession({ token: data.token, email: data.email, name: data.name, role: data.role });
  ensureUnreadAlarm();
  await refreshUnreadBadge();
  return success({ loggedIn: true, email: data.email, name: data.name, role: data.role });
}

// FR-07, FR-09, FR-22: content.js read a product page; send it to the server and return the verdict.
// Nothing is sent while the shopper is logged out.
async function handleObserve(message, sender) {
  const session = await readSession();
  if (!session || !session.token) {
    return failure('auth', 'Please log in to Price Pulse.');
  }
  const observation = message.observation;
  if (!observation || !observation.url || !observation.sku || !observation.price) {
    return failure('invalid', 'Could not read this page.');
  }
  const reply = await apiRequest('/observations', {
    method: 'POST',
    auth: true,
    body: {
      url: observation.url,
      sku: observation.sku,
      title: observation.title,
      imageUrl: observation.imageUrl,
      price: observation.price,
      claimedOriginalPrice: observation.claimedOriginalPrice,
      discountBadgeShown: observation.discountBadgeShown === true
    }
  });
  if (reply.ok && sender && sender.tab && sender.tab.id !== undefined) {
    // Remembered per tab (until the browser closes) so the popup can show the same verdict (M5).
    await chrome.storage.session.set({ ['tab-' + sender.tab.id]: reply.data });
  }
  return reply;
}

// ---------- popup data (M5) ----------

function tabKey(tabId) {
  return 'tab-' + tabId;
}

// The verdict content.js received for this tab, so the popup can show the same thing (FR-22).
async function handleGetPageVerdict(message) {
  if (message.tabId === undefined || message.tabId === null) {
    return success(null);
  }
  const stored = await chrome.storage.session.get(tabKey(message.tabId));
  return success(stored[tabKey(message.tabId)] || null);
}

// FR-24: watch the product of the current page with a target price.
async function handleWatch(message) {
  const target = Number(message.targetPrice);
  if (!message.productId) {
    return failure('invalid', 'Open a product page first.');
  }
  if (!Number.isFinite(target) || target <= 0) {
    return failure('invalid', 'Target price must be greater than zero');
  }
  return apiRequest('/watchlist', {
    method: 'POST', auth: true, body: { productId: message.productId, targetPrice: target }
  });
}

async function handleGetWatchlist() {
  return apiRequest('/watchlist', { auth: true });
}

async function handleRemoveWatch(message) {
  return apiRequest('/watchlist/' + encodeURIComponent(message.itemId), { method: 'DELETE', auth: true });
}

async function handleGetNotifications() {
  return apiRequest('/notifications', { auth: true });
}

async function handleMarkRead(message) {
  const reply = await apiRequest('/notifications/' + encodeURIComponent(message.notificationId) + '/read',
    { method: 'PUT', auth: true });
  await refreshUnreadBadge();
  return reply;
}

// ---------- unread count on the toolbar icon (FR-31) ----------

const UNREAD_ALARM = 'price-pulse-unread';

async function refreshUnreadBadge() {
  const session = await readSession();
  if (!session) {
    await chrome.action.setBadgeText({ text: '' });
    return success({ count: 0 });
  }
  const reply = await apiRequest('/notifications/unread-count', { auth: true });
  if (reply.ok) {
    const count = Number(reply.data && reply.data.count) || 0;
    await chrome.action.setBadgeBackgroundColor({ color: '#F59E0B' });
    await chrome.action.setBadgeText({ text: count > 0 ? (count > 99 ? '99+' : String(count)) : '' });
    return success({ count: count });
  }
  if (reply.error === 'auth') {
    await chrome.action.setBadgeText({ text: '' });
  }
  return reply; // offline: keep the last number
}

// The service worker is stopped when idle, so a live stream cannot stay open here (SDS 7).
// Instead an alarm wakes it once a minute to ask for the unread count.
function ensureUnreadAlarm() {
  chrome.alarms.create(UNREAD_ALARM, { periodInMinutes: 1 });
}

chrome.runtime.onInstalled.addListener(() => {
  ensureUnreadAlarm();
  refreshUnreadBadge();
});
chrome.runtime.onStartup.addListener(() => {
  ensureUnreadAlarm();
  refreshUnreadBadge();
});
chrome.alarms.onAlarm.addListener((alarm) => {
  if (alarm.name === UNREAD_ALARM) {
    refreshUnreadBadge();
  }
});

// Forget a tab's verdict when it loads another page or closes, so the popup never shows an old verdict.
chrome.tabs.onUpdated.addListener((tabId, changeInfo) => {
  if (changeInfo.status === 'loading') {
    chrome.storage.session.remove(tabKey(tabId));
  }
});
chrome.tabs.onRemoved.addListener((tabId) => {
  chrome.storage.session.remove(tabKey(tabId));
});

async function handleRefreshBadge() {
  return refreshUnreadBadge();
}

const HANDLERS = {
  PING: handlePing,
  GET_SESSION: handleGetSession,
  LOGIN: handleLogin,
  SIGNUP: handleSignup,
  LOGOUT: handleLogout,
  OBSERVE: handleObserve,
  GET_PAGE_VERDICT: handleGetPageVerdict,
  WATCH: handleWatch,
  GET_WATCHLIST: handleGetWatchlist,
  REMOVE_WATCH: handleRemoveWatch,
  GET_NOTIFICATIONS: handleGetNotifications,
  MARK_READ: handleMarkRead,
  REFRESH_BADGE: handleRefreshBadge
};

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  const handler = message && HANDLERS[message.type];
  if (!handler) {
    sendResponse(failure('invalid', 'Unknown request'));
    return false;
  }
  handler(message, sender)
    .then(sendResponse)
    .catch((unexpected) => {
      console.error('Price Pulse background error', unexpected);
      sendResponse(failure('invalid', 'Something went wrong in Price Pulse.'));
    });
  return true; // keep the reply channel open until the async handler finishes
});
