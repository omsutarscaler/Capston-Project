const login = document.querySelector('#login');
const message = document.querySelector('#message');
const products = document.querySelector('#products');
const check = document.querySelector('#check');
const previous = document.querySelector('#previous');
const next = document.querySelector('#next');
const returnInfo = new URLSearchParams(location.search);
const returnedOrder = returnInfo.get('order');
let page = 0;

function token() {
  return sessionStorage.getItem('capston-token');
}

async function request(path, body, authenticated = false) {
  const headers = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (authenticated && token()) headers.Authorization = 'Bearer ' + token();
  const response = await fetch(path, {
    method: body === undefined ? 'GET' : 'POST',
    headers,
    body: body === undefined ? undefined : JSON.stringify(body)
  });
  if (!response.ok) {
    if (authenticated && response.status === 401) {
      sessionStorage.removeItem('capston-token');
      throw new Error('Sign in again to continue.');
    }
    const problem = await response.json().catch(() => ({}));
    throw new Error(problem.detail || 'Request failed (' + response.status + ').');
  }
  return response.json();
}

async function startCheckout(product, button) {
  if (!token()) {
    message.textContent = 'Sign in to buy this product.';
    login.elements.email.focus();
    return;
  }
  const key = 'capston-order-' + sessionStorage.getItem('capston-account') + ':' + product.id;
  const orderId = sessionStorage.getItem(key) || crypto.randomUUID();
  sessionStorage.setItem(key, orderId);
  button.disabled = true;
  try {
    const order = await request('/checkout', { orderId, productId: product.id }, true);
    if (order.checkoutUrl) location.assign(order.checkoutUrl);
    else {
      sessionStorage.removeItem(key);
      message.textContent = order.paid ? 'This order was already paid.' : 'Checkout expired. Select Buy to start again.';
    }
  } catch (error) {
    message.textContent = error.message;
  } finally {
    button.disabled = false;
  }
}

async function loadProducts(targetPage) {
  try {
    const catalog = await request('/products?size=12&page=' + targetPage);
    page = targetPage;
    products.replaceChildren();
    if (!catalog.content.length) products.textContent = 'The catalog is empty.';
    for (const product of catalog.content) {
      const card = document.createElement('article');
      const title = document.createElement('h3');
      const price = document.createElement('p');
      const buy = document.createElement('button');
      title.textContent = product.title;
      price.textContent = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(product.price);
      buy.textContent = 'Buy';
      buy.addEventListener('click', () => startCheckout(product, buy));
      card.append(title, price, buy);
      products.append(card);
    }
    previous.disabled = page === 0;
    next.disabled = !catalog.hasNext;
  } catch (error) {
    message.textContent = error.message;
  }
}

async function confirmPayment() {
  if (!token()) {
    message.textContent = 'Sign in to check payment status.';
    return;
  }
  check.disabled = true;
  try {
    const order = await request('/checkout/' + encodeURIComponent(returnedOrder) + '/confirm', {}, true);
    if (order.paid || order.expired) {
      for (const key of Object.keys(sessionStorage)) {
        if (key.startsWith('capston-order-') && sessionStorage.getItem(key) === returnedOrder) {
          sessionStorage.removeItem(key);
        }
      }
    }
    if (order.paid) {
      message.textContent = order.receiptSent ? 'Payment confirmed. Receipt sent to your account email.'
        : 'Payment confirmed. Your receipt is pending.';
    } else {
      message.textContent = order.expired ? 'Checkout expired.' : 'Payment has not been confirmed yet.';
    }
  } catch (error) {
    message.textContent = error.message;
  } finally {
    check.disabled = false;
  }
}

login.addEventListener('submit', async event => {
  event.preventDefault();
  const submit = login.querySelector('button');
  submit.disabled = true;
  try {
    const credentials = Object.fromEntries(new FormData(login));
    const result = await request('/auth/login', credentials);
    sessionStorage.setItem('capston-account', credentials.email.trim().toLowerCase());
    sessionStorage.setItem('capston-token', result.accessToken);
    login.reset();
    message.textContent = 'Signed in.';
    if (returnedOrder && returnInfo.get('result') === 'success') await confirmPayment();
  } catch (error) {
    message.textContent = error.message;
  } finally {
    submit.disabled = false;
  }
});

check.hidden = !returnedOrder;
check.addEventListener('click', confirmPayment);
previous.addEventListener('click', () => loadProducts(page - 1));
next.addEventListener('click', () => loadProducts(page + 1));
if (returnInfo.get('result') === 'cancel') message.textContent = 'Checkout canceled. You can try again.';
if (returnedOrder && returnInfo.get('result') === 'success') confirmPayment();
loadProducts(0);
