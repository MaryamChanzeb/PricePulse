// Price Pulse popup. It never calls the server itself: it asks background.js with messages.
// All text goes into the page with textContent, never innerHTML.

const VIEW_IDS = ['view-loading', 'view-offline', 'view-login', 'view-account'];
const PASSWORD_RULE = /^(?=.*\d).{8,}$/; // FR-01: at least 8 characters and one digit
const EMAIL_SHAPE = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;

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
    showAccount(session.data);
  } else {
    showView('view-login'); // FR-09: ask to log in before anything is sent
    showTab('login');
  }
}

function showAccount(user) {
  byId('account-name').textContent = user.name || '';
  byId('account-email').textContent = user.email || '';
  showView('view-account');
}

// ---------- log in / sign up ----------

function showTab(name) {
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

// Shows the answer of a LOGIN or SIGNUP request.
async function handleAuthReply(reply) {
  if (reply.ok) {
    showAccount(reply.data);
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

document.addEventListener('DOMContentLoaded', () => {
  byId('btn-retry').addEventListener('click', init);
  byId('btn-logout').addEventListener('click', onLogout);
  byId('tab-login').addEventListener('click', () => showTab('login'));
  byId('tab-signup').addEventListener('click', () => showTab('signup'));
  byId('form-login').addEventListener('submit', onLoginSubmit);
  byId('form-signup').addEventListener('submit', onSignupSubmit);
  linkShowPassword('login-show-password', 'login-password');
  linkShowPassword('signup-show-password', 'signup-password');
  init();
});
