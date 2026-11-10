const money = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', minimumFractionDigits: 0, maximumFractionDigits: 2 });
const shop = id => id ? '/shop?category=' + id : '/shop';
const departments = [
  ['Electronics', 'Mobiles'], ['Fashion', 'Men’s Clothing'], ['Home & kitchen', 'Kitchen'],
  ['Beauty', 'Beauty'], ['Sports', 'Sports'], ['Books', 'Books'], ['Toys', 'Toys'], ['Groceries', 'Groceries']
];

function Artwork({ category = '', image, title = '', hero = false }) {
  if (/^https?:\/\//i.test(image || '')) return <img src={image} alt={title} loading="lazy" />;
  const tone = [...(category + title)].reduce((sum, letter) => sum + letter.charCodeAt(0), 0) % 5;
  let shape;
  if (/mobile|tablet|watch/i.test(category)) {
    shape = <><rect x="65" y="22" width="68" height="100" rx="12"/><rect x="72" y="30" width="54" height="78" rx="6" className="screen"/><circle cx="99" cy="115" r="3" className="accent"/></>;
  } else if (/laptop|monitor|computer|television|gaming|office/i.test(category)) {
    shape = <><rect x="44" y="37" width="113" height="69" rx="7"/><rect x="51" y="44" width="99" height="53" rx="4" className="screen"/><path d="M31 109h138l-12 11H43z"/><path d="M67 84l28-27 19 17 13-11" className="stroke"/></>;
  } else if (/clothing|fashion/i.test(category)) {
    shape = <path d="M75 34l13-5c2 11 22 11 24 0l13 5 32 21-17 24-17-10v49H77V69L60 79 43 55z"/>;
  } else if (/book|stationery|arts/i.test(category)) {
    shape = <><path d="M43 43c22-9 40-6 57 8 17-14 35-17 57-8v68c-22-9-40-6-57 7-17-13-35-16-57-7z"/><path d="M100 53v62M56 61l29 5m-29 9l29 5m30-14l29-5m-29 19l29-5" className="stroke"/></>;
  } else if (/sports|fitness|outdoor|cycling/i.test(category)) {
    shape = <><circle cx="100" cy="75" r="43"/><path d="M58 75h84m-42-42v84M71 44c22 17 35 40 56 62m0-62c-22 17-35 40-56 62" className="stroke"/></>;
  } else if (/beauty|fragrance|personal/i.test(category)) {
    shape = <><rect x="78" y="47" width="46" height="72" rx="9"/><rect x="87" y="30" width="28" height="19" rx="4"/><rect x="85" y="70" width="32" height="27" rx="3" className="accent"/></>;
  } else if (/home|kitchen|cookware|dining|appliance|grocer/i.test(category)) {
    shape = <><path d="M58 59h68v34a24 24 0 0 1-24 24H82a24 24 0 0 1-24-24z"/><path d="M126 66h10a14 14 0 0 1 0 28h-10M75 50c-6-8 7-10 1-18m22 18c-6-8 7-10 1-18" className="stroke"/></>;
  } else {
    shape = <><path d="M52 57h96l12 61H40z"/><path d="M77 63V49a23 23 0 0 1 46 0v14" className="stroke"/><path d="M87 84h26v17H87z" className="accent"/></>;
  }
  return <svg className={'art palette-' + tone + (hero ? ' hero-product' : '')} viewBox="0 0 200 150" aria-hidden="true">
    <ellipse cx="100" cy="125" rx="65" ry="8" className="shadow"/>{shape}
  </svg>;
}

function Quantity({ product, count, shopping, busy, changeCart }) {
  const quantity = shopping.cart[product.id] || 0;
  const locked = busy || !!shopping.activeCheckoutId;
  if (!quantity) return <button className="buy" disabled={locked || count >= 10} onClick={() => changeCart(product.id, 1)}>Add to cart</button>;
  return <div className="quantity" aria-label={'Quantity for ' + product.title}>
    <button aria-label={'Decrease ' + product.title} disabled={locked} onClick={() => changeCart(product.id, quantity - 1)}>−</button>
    <output aria-live="polite">{quantity}</output>
    <button aria-label={'Increase ' + product.title} disabled={locked || count >= 10} onClick={() => changeCart(product.id, quantity + 1)}>+</button>
  </div>;
}

function AccountForm({ panel, setPanel, message, busy, profile, accountAction, logout }) {
  const title = { login: 'Sign in', register: 'Create account', forgot: 'Reset your password', reset: 'Choose a new password', profile: 'Your account' }[panel];
  return <section className="account-card">
    <p className="kicker">ECOMMERCE ACCOUNT</p><h1>{title}</h1>
    <p className="muted">{panel === 'forgot' ? 'Enter your account email and we’ll send you a reset link.' :
      panel === 'profile' ? 'Manage your display name and account session.' : 'Shop, save favorites and manage your orders.'}</p>
    {message && <p role="status" className="notice">{message}</p>}
    <form key={panel} onSubmit={accountAction}>
      {(panel === 'register' || panel === 'profile') && <label>Name<input name="name" maxLength={100} defaultValue={panel === 'profile' ? profile?.name : ''} required /></label>}
      {['login', 'register', 'forgot'].includes(panel) && <label>Email address<input name="email" type="email" autoComplete="email" required /></label>}
      {panel === 'profile' && <label>Email address<input value={profile?.email || ''} readOnly /></label>}
      {['login', 'register', 'reset'].includes(panel) && <label>{panel === 'reset' ? 'New password' : 'Password'}<input name="password" type="password" minLength={panel === 'login' ? undefined : 8} maxLength={72} autoComplete={panel === 'login' ? 'current-password' : 'new-password'} required /></label>}
      <button className="buy" disabled={busy}>{({login:'Sign in',register:'Create account',forgot:'Send reset link',reset:'Save password',profile:'Save profile'})[panel]}</button>
      {panel === 'profile' && <button type="button" onClick={logout}>Sign out</button>}
    </form>
    {panel === 'login' ? <div className="account-links"><button onClick={() => setPanel('register')}>Create account</button><button onClick={() => setPanel('forgot')}>Forgot password?</button></div> :
      panel !== 'profile' && <button className="text-link" onClick={() => setPanel('login')}>Back to sign in</button>}
  </section>;
}

function ProductCard({ product, context }) {
  return <article className="product-card">
    <div className="product-picture">
      <a href={'/product/' + product.id} aria-label={'View ' + product.title}><Artwork category={product.category?.name} image={product.image} title={product.title}/></a>
      <button className="heart" aria-label={'Save ' + product.title} aria-pressed={context.shopping.wishlist.includes(product.id)}
        disabled={context.busy} onClick={() => context.saveProduct(product.id)}>{context.shopping.wishlist.includes(product.id) ? '♥' : '♡'}</button>
    </div>
    <div className="product-copy">
      <small>{product.category?.name || 'Marketplace'}</small>
      <h3><a href={'/product/' + product.id}>{product.title}</a></h3>
      <strong className="price">{money.format(product.price)}</strong>
      <Quantity product={product} {...context}/>
    </div>
  </article>;
}

export default function MarketplaceView(context) {
  const { view, token, profile, shopping, count, categories, category, setCategory, search, sort, setSort, page, setPage,
    catalog, loading, message, busy, savedProducts, checkout, returnedOrder, confirm, reviewProduct, reviews, submitReview } = context;
  const accountPath = token ? '/account' : '/login';
  const selected = categories.find(item => String(item.id) === category);
  const cartTotal = Object.entries(shopping.cart).reduce((sum, [id, quantity]) => sum + (savedProducts[id]?.price || 0) * quantity, 0);
  const groups = departments.map(([label, name]) => [label, categories.find(item => item.name === name)]);
  const total = categories.reduce((sum, item) => sum + item.productCount, 0);

  if (view === 'login') return <main className="login-shell">
    <a className="brand login-brand" href="/">ecommerce<span>.</span></a>
    <AccountForm {...context}/>
    <a className="text-link" href="/">Continue browsing</a>
  </main>;

  return <>
    <header className="market-header">
      <div className="wrap masthead">
        <a className="brand" href="/">ecommerce<span>.</span></a>
        <form action="/shop" className="search" role="search">
          <label className="sr-only" htmlFor="search">Search products</label>
          <input id="search" name="q" defaultValue={search} placeholder="Search products and categories" maxLength={100}/>
          <button aria-label="Search">Search</button>
        </form>
        <div className="header-links">
          <a href={accountPath}><small>{profile?.name ? 'Hello, ' + profile.name : 'Hello, sign in'}</small><strong>My account</strong></a>
          <a href="/wishlist" aria-label="Wishlist">♡ <span>Saved</span></a>
          <a href="/cart" className="cart-link">Cart <b>{count}</b></a>
        </div>
      </div>
      <nav className="department-bar" aria-label="Departments"><div className="wrap departments">
        <a className="all" href="/shop">☰ All departments</a>
        {groups.map(([label, item]) => <a key={label} href={shop(item?.id)}>{label}</a>)}
      </div></nav>
    </header>

    <main className="wrap market-main">
      {message && view !== 'account' && view !== 'orders' && <p className="notice" role="status">{message}</p>}
      {view === 'home' && <>
        <section className="market-hero">
          <div><p className="kicker">THE MARKETPLACE</p><h1>Find what you’re<br/>looking for.</h1>
            <p>Explore electronics, fashion, home essentials and more, all in one place.</p>
            <a className="buy" href="/shop">Shop all departments <span aria-hidden="true">→</span></a>
          </div>
          <div className="hero-visual" aria-hidden="true"><Artwork category="Laptops" hero/><Artwork category="Kitchen" hero/><Artwork category="Fashion" hero/></div>
        </section>
        <section className="home-categories">
          <div className="section-head"><div><p className="kicker">BROWSE BY CATEGORY</p><h2>Shop by department</h2></div><a className="text-link" href="/shop">View all {categories.length || ''} categories →</a></div>
          <div className="department-grid">{groups.map(([label, item]) => <a className="department-card" key={label} href={shop(item?.id)}>
            <Artwork category={item?.name || label}/><strong>{label}</strong><small>Explore department →</small>
          </a>)}</div>
        </section>
        <div className="market-summary"><strong>{total ? total.toLocaleString('en-IN') + ' products' : 'Explore the catalog'}</strong><span>Across {categories.length || 'our'} categories</span><a href="/shop">Browse catalog →</a></div>
      </>}

      {view === 'shop' && <div className="catalog-layout">
        <aside className="category-sidebar" aria-label="Catalog categories"><h2>Departments</h2>
          <button className={!category ? 'selected' : ''} onClick={() => setCategory('')}>All products</button>
          {categories.map(item => <button key={item.id} className={category === String(item.id) ? 'selected' : ''} onClick={() => setCategory(String(item.id))}>{item.name}<small>{item.productCount}</small></button>)}
        </aside>
        <section className="results">
          <p className="breadcrumbs"><a href="/">Home</a> / Shop{selected ? ' / ' + selected.name : ''}</p>
          <div className="section-head"><div><h1>{search ? 'Results for “' + search + '”' : selected?.name || 'All products'}</h1><p className="muted">{catalog.totalElements ?? 0} products</p></div>
            <label className="sort">Sort by<select value={sort} onChange={event => setSort(event.target.value)}><option value="price-asc">Price: low to high</option><option value="price-desc">Price: high to low</option><option value="newest">Newest</option></select></label>
          </div>
          <label className="mobile-categories">Category<select value={category} onChange={event => setCategory(event.target.value)}><option value="">All products</option>{categories.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label>
          {loading ? <p className="notice">Loading products…</p> : catalog.content.length ? <div className="product-grid">{catalog.content.map(product => <ProductCard key={product.id} product={product} context={context}/>)}</div> : <p className="notice">No products match these filters.</p>}
          <div className="pagination"><button disabled={!page || loading} onClick={() => { setPage(page - 1); window.scrollTo(0, 0); }}>Previous</button><span>Page {page + 1} of {Math.max(1,catalog.totalPages || 1)}</span><button disabled={!catalog.hasNext || loading} onClick={() => { setPage(page + 1); window.scrollTo(0, 0); }}>Next</button></div>
        </section>
      </div>}

      {view === 'product' && (reviewProduct ? <>
        <p className="breadcrumbs"><a href="/">Home</a> / <a href="/shop">Shop</a> / {reviewProduct.title}</p>
        <section className="product-detail">
          <div className="detail-picture"><Artwork category={reviewProduct.category?.name} image={reviewProduct.image} title={reviewProduct.title}/></div>
          <div><p className="kicker">{reviewProduct.category?.name || 'MARKETPLACE'}</p><h1>{reviewProduct.title}</h1>
            <a className="text-link" href="#reviews">Read customer reviews ↓</a><p className="detail-price">{money.format(reviewProduct.price)}</p>
            <p>{reviewProduct.description || 'Available from the catalog.'}</p>
            <div className="detail-actions"><Quantity product={reviewProduct} {...context}/><button onClick={() => context.saveProduct(reviewProduct.id)} disabled={busy}>{shopping.wishlist.includes(reviewProduct.id) ? '♥ Saved' : '♡ Save to wishlist'}</button></div>
          </div>
        </section>
        <section id="reviews" className="review-section">
          <div className="review-list"><h2>Customer reviews</h2>
            {reviews.length ? reviews.map((review, index) => <article className="review" key={index}><strong className="stars" aria-label={review.rating + ' out of 5 stars'}>{'★'.repeat(review.rating)}{'☆'.repeat(5-review.rating)}</strong><small>{new Date(review.updatedAt).toLocaleDateString('en-IN')}</small><p>{review.comment}</p></article>) : <p className="muted">No reviews yet. Be the first to review this product.</p>}
          </div>
          <form className="review-form" onSubmit={submitReview}><h2>Write a review</h2><label>Rating<select name="rating" defaultValue="5">{[5,4,3,2,1].map(value => <option key={value} value={value}>{value} / 5</option>)}</select></label><label>Your review<textarea name="comment" maxLength={1000} required/></label><button className="buy" disabled={busy}>Save review</button></form>
        </section>
      </> : <p className="notice">{message || 'Loading product details…'}</p>)}

      {view === 'cart' && <section className="utility-page"><h1>Shopping cart</h1>
        {!token ? <p><a className="text-link" href="/login?next=/cart">Sign in to see your cart</a></p> : !count ? <p>Your cart is empty. <a className="text-link" href="/shop">Continue shopping</a></p> :
          <div className="cart-layout"><div className="surface">
            {Object.entries(shopping.cart).map(([id, quantity]) => <div className="cart-row" key={id}>
              <div className="thumbnail"><Artwork category={savedProducts[id]?.category?.name} image={savedProducts[id]?.image}/></div>
              <div><a href={'/product/' + id}><strong>{savedProducts[id]?.title || 'Product ' + id}</strong></a><p>{savedProducts[id] && money.format(savedProducts[id].price * quantity)}</p></div>
              <Quantity product={{id, title:savedProducts[id]?.title || 'product'}} {...context}/>
            </div>)}
          </div><aside className="surface order-summary"><h2>Order summary</h2><p>{count} of 10 units</p><strong>{money.format(cartTotal)}</strong><button className="buy" onClick={checkout} disabled={busy}>{shopping.activeCheckoutId ? 'Resume checkout' : 'Proceed to checkout'}</button>
            {shopping.activeCheckoutId && <button onClick={() => confirm(shopping.activeCheckoutId,true)} disabled={busy}>Cancel checkout</button>}</aside></div>}
      </section>}

      {view === 'wishlist' && <section className="utility-page"><h1>Your wishlist</h1>
        {!token ? <a className="text-link" href="/login?next=/wishlist">Sign in to see your wishlist</a> :
          shopping.wishlist.length ? <div className="product-grid">{shopping.wishlist.map(id => savedProducts[id] && <ProductCard key={id} product={savedProducts[id]} context={context}/>)}</div> : <p>Save products using the heart icon.</p>}
      </section>}

      {view === 'orders' && <section className="utility-page"><div className="surface order-summary order-status">
        <p className="kicker">YOUR CHECKOUT</p><h1>Order status</h1>
        {message && <p className="notice" role="status">{message}</p>}
        {returnedOrder ? <><p className="muted">Order reference</p><code>{returnedOrder}</code>
          <button className="buy" disabled={busy} onClick={() => confirm()}>Check payment status</button></> :
          shopping.activeCheckoutId ? <a className="buy" href={'/orders?order=' + shopping.activeCheckoutId}>View active checkout</a> : <p>Open your cart to start a checkout.</p>}
        <a className="text-link" href="/cart">Back to cart</a>
      </div></section>}

      {view === 'account' && (token ? <div className="account-wrap"><AccountForm {...context} panel="profile"/></div> :
        <p className="notice"><a className="text-link" href="/login?next=/account">Sign in to manage your account</a></p>)}
    </main>
    <footer className="market-footer"><div className="wrap"><a className="brand" href="/">ecommerce<span>.</span></a><a href="/shop">Shop</a><a href="/orders">Order status</a><a href={accountPath}>Account</a></div></footer>
  </>;
}
