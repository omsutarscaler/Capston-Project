'use client';

import { useEffect, useState } from 'react';
import { usePathname } from 'next/navigation';
import MarketplaceView from './marketplace-view';

const SHOP = '/api/shop';
const ACCOUNT = '/api/account';
const EMPTY = { cart: {}, wishlist: [] };

async function api(path, { method = 'GET', body, token } = {}) {
  const headers = {};
  if (token) headers.Authorization = 'Bearer ' + token;
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  const response = await fetch(path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
  const data = await response.json().catch(() => null);
  if (!response.ok) {
    const error = new Error(data?.detail || data?.message || 'Request failed (' + response.status + ').');
    error.status = response.status;
    throw error;
  }
  return data;
}

function fields(event) {
  return Object.fromEntries(new FormData(event.currentTarget));
}

export default function Storefront() {
  const path = usePathname();
  const view = path.startsWith('/product/') ? 'product' : path.slice(1) || 'home';
  const [token, setToken] = useState('');
  const [profile, setProfile] = useState(null);
  const [page, setPage] = useState(0);
  const [catalog, setCatalog] = useState({ content: [], hasNext: false });
  const [loading, setLoading] = useState(true);
  const [shopping, setShopping] = useState(EMPTY);
  const [savedProducts, setSavedProducts] = useState({});
  const [panel, setPanel] = useState('login');
  const [categories, setCategories] = useState([]);
  const [category, setCategory] = useState('');
  const [search, setSearch] = useState('');
  const [sort, setSort] = useState('price-asc');
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);
  const [reset, setReset] = useState('');
  const [returnedOrder, setReturnedOrder] = useState('');
  const [reviewProduct, setReviewProduct] = useState(null);
  const [reviews, setReviews] = useState([]);
  const count = Object.values(shopping.cart).reduce((sum, quantity) => sum + quantity, 0);

  useEffect(() => {
    const query = new URLSearchParams(location.search);
    setCategory(query.get('category') || '');
    setSearch(query.get('q') || '');
    setToken(sessionStorage.getItem('ecommerce-token') || '');
    const resetToken = new URLSearchParams(location.hash.slice(1)).get('reset');
    if (resetToken) {
      if (location.pathname !== '/login') {
        location.replace('/login' + location.hash);
        return;
      }
      setReset(resetToken);
      setPanel('reset');
      history.replaceState(null, '', location.pathname);
    }
    if (query.has('order')) {
      if (location.pathname === '/') {
        location.replace('/orders' + location.search);
        return;
      }
      setReturnedOrder(query.get('order'));
    }
    if (query.get('result') === 'cancel') setMessage('Checkout canceled. Resume from your cart before the session expires.');
  }, []);

  useEffect(() => {
    if (view !== 'home' && view !== 'shop') return;
    api(SHOP + '/products/categories').then(setCategories).catch(error => setMessage(error.message));
  }, [view]);

  useEffect(() => {
    if (view !== 'shop') return;
    let active = true;
    setLoading(true);
    const query = new URLSearchParams({ size: '20', page, q: search, sort });
    if (category) query.set('category', category);
    api(SHOP + '/products?' + query)
      .then(result => { if (active) setCatalog(result); })
      .catch(error => { if (active) setMessage(error.message); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [view, page, category, search, sort]);

  useEffect(() => {
    if (view !== 'product') return;
    const id = path.split('/')[2];
    if (!/^\d+$/.test(id)) return;
    let active = true;
    Promise.all([api(SHOP + '/products/' + id), api(SHOP + '/reviews/' + id)]).then(([product, result]) => {
      if (active) { setReviewProduct(product); setReviews(result.content); }
    }).catch(error => { if (active) setMessage(error.message); });
    return () => { active = false; };
  }, [path, view]);

  useEffect(() => {
    if (!token) return;
    let active = true;
    Promise.all([api(ACCOUNT + '/profile', { token }), api(SHOP + '/shopping', { token })])
      .then(([account, list]) => { if (active) { setProfile(account); setShopping(list); } })
      .catch(error => { if (active) report(error); });
    return () => { active = false; };
  }, [token]);

  useEffect(() => {
    const ids = new Set([...Object.keys(shopping.cart), ...shopping.wishlist.map(String)]);
    let active = true;
    Promise.allSettled([...ids].map(id => api(SHOP + '/products/' + id))).then(results => {
      if (active) setSavedProducts(Object.fromEntries(results.filter(result => result.status === 'fulfilled')
        .map(result => [result.value.id, result.value])));
    });
    return () => { active = false; };
  }, [shopping]);

  function signOut() {
    sessionStorage.removeItem('ecommerce-token');
    setToken('');
    setProfile(null);
    setShopping(EMPTY);
  }

  function report(error) {
    if (error.status === 401 && token) {
      signOut();
      location.assign('/login?next=' + encodeURIComponent(location.pathname + location.search));
    }
    setMessage(error.message);
  }

  async function perform(action) {
    if (busy) return;
    setBusy(true);
    setMessage('');
    try { await action(); } catch (error) { report(error); } finally { setBusy(false); }
  }

  function requireAccount(action) {
    if (!token) {
      location.assign('/login?next=' + encodeURIComponent(location.pathname + location.search));
      return;
    }
    perform(action);
  }

  function changeCart(id, quantity) {
    requireAccount(async () => {
      setShopping(await api(SHOP + '/shopping/cart/' + id, { method: 'PUT', body: { quantity }, token }));
    });
  }

  function saveProduct(id) {
    requireAccount(async () => {
      const saved = !shopping.wishlist.includes(id);
      setShopping(await api(SHOP + '/shopping/wishlist/' + id, { method: 'PUT', body: { saved }, token }));
      setMessage(saved ? 'Saved to wishlist.' : 'Removed from wishlist.');
    });
  }

  function checkout() {
    requireAccount(async () => {
      if (!profile) throw new Error('Your account is still loading.');
      const key = 'ecommerce-cart-order-' + profile.id;
      const orderId = shopping.activeCheckoutId || sessionStorage.getItem(key) || crypto.randomUUID();
      sessionStorage.setItem(key, orderId);
      try {
        const result = await api(SHOP + '/checkout', { method: 'POST', token, body: { orderId } });
        if (result.checkoutUrl) {
          location.assign(result.checkoutUrl);
          return;
        }
        sessionStorage.removeItem(key);
        location.assign('/orders?order=' + encodeURIComponent(orderId));
      } catch (error) {
        if (error.status === 409) sessionStorage.removeItem(key);
        throw error;
      }
    });
  }

  function confirm(orderId = returnedOrder, cancel = false) {
    requireAccount(async () => {
      const result = await api(SHOP + '/checkout/' + encodeURIComponent(orderId) + (cancel ? '/cancel' : '/confirm'), {
        method: 'POST', token
      });
      if (result.paid || result.expired) {
        if (profile) sessionStorage.removeItem('ecommerce-cart-order-' + profile.id);
        setShopping(await api(SHOP + '/shopping', { token }));
      }
      setMessage(result.paid ? (result.receiptSent ? 'Payment confirmed. Your receipt was sent.'
        : 'Payment confirmed. Your receipt is pending.') : result.expired
        ? 'Checkout closed. Stock was released.' : 'Payment is pending.');
    });
  }

  async function accountAction(event) {
    event.preventDefault();
    const body = fields(event);
    await perform(async () => {
      if (view === 'account') {
        setProfile(await api(ACCOUNT + '/profile', { method: 'PUT', token, body }));
        setMessage('Profile updated.');
      } else if (panel === 'login') {
        const result = await api(SHOP + '/auth/login', { method: 'POST', body });
        sessionStorage.setItem('ecommerce-token', result.accessToken);
        setToken(result.accessToken);
        const next = new URLSearchParams(location.search).get('next');
        location.assign(next?.startsWith('/') && !next.startsWith('//') && !next.includes('\\') ? next : '/');
      } else if (panel === 'register') {
        await api(ACCOUNT + '/signup', { method: 'POST', body });
        setPanel('login');
        setMessage('Account created. Sign in to continue.');
      } else if (panel === 'forgot') {
        await api(ACCOUNT + '/forgot-password', { method: 'POST', body });
        setMessage('If the account exists, a reset link will be emailed.');
      } else if (panel === 'reset') {
        await api(ACCOUNT + '/reset-password', { method: 'POST', body: { ...body, token: reset } });
        signOut();
        setReset('');
        setPanel('login');
        setMessage('Password updated. Sign in with your new password.');
      }
    });
  }

  function submitReview(event) {
    event.preventDefault();
    const body = fields(event);
    requireAccount(async () => {
      await api(SHOP + '/reviews/' + reviewProduct.id, {
        method: 'PUT', token, body: { rating: Number(body.rating), comment: body.comment }
      });
      setReviews((await api(SHOP + '/reviews/' + reviewProduct.id)).content);
      setMessage('Review saved.');
    });
  }

  return <MarketplaceView
    view={view} token={token} profile={profile} shopping={shopping} count={count}
    categories={categories} category={category} setCategory={value => { setCategory(value); setPage(0); }}
    search={search} sort={sort} setSort={value => { setSort(value); setPage(0); }}
    page={page} setPage={setPage} catalog={catalog} loading={loading}
    panel={panel} setPanel={setPanel} message={message} busy={busy}
    changeCart={changeCart} saveProduct={saveProduct} savedProducts={savedProducts}
    checkout={checkout} returnedOrder={returnedOrder} confirm={confirm}
    reviewProduct={reviewProduct} reviews={reviews} submitReview={submitReview}
    accountAction={accountAction} logout={() => perform(async () => {
      await api(ACCOUNT + '/logout', { method: 'POST', body: { token } });
      signOut();
      location.assign('/login');
    })}
  />;
}
