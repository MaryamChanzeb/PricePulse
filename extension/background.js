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

const HANDLERS = {
  PING: handlePing,
  GET_SESSION: handleGetSession,
  LOGIN: handleLogin,
  SIGNUP: handleSignup,
  LOGOUT: handleLogout,
  OBSERVE: handleObserve
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
