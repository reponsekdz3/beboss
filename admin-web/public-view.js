// BeBoss Public Interactive Showcase, APK/QR Download Hub, POS Experience Sandbox & 4-Language Docs
(function (global) {
  const { useState, useMemo } = React;

  const SAMPLE_PRODUCTS = [
    { id: 'p1', sku: '6161100', name: 'Inyange Milk 500ml', category: 'Dairy', price: 800, stock: 42 },
    { id: 'p2', sku: '5449000', name: 'Fanta Citron 300ml', category: 'Drinks', price: 700, stock: 68 },
    { id: 'p3', sku: '6001087', name: 'Akabanga Chili Oil', category: 'Pantry', price: 2500, stock: 19 },
    { id: 'p4', sku: '8901425', name: 'Azam Wheat Flour 1kg', category: 'Groceries', price: 1400, stock: 31 },
    { id: 'p5', sku: '6164001', name: 'Sina Rice Basmati 2kg', category: 'Grains', price: 4200, stock: 15 },
    { id: 'p6', sku: '4005808', name: 'Movit Body Cream 200g', category: 'Care', price: 2200, stock: 11 },
    { id: 'p7', sku: '7622210', name: 'MTN Airtime Voucher', category: 'Services', price: 1000, stock: 99 }
  ];

  function QrSvg({ value, size = 216 }) {
    const qr = useMemo(() => {
      try {
        return global.BeBossQR.encodeQR(value || 'https://beboss.app');
      } catch (_) {
        return null;
      }
    }, [value]);

    if (!qr) return null;
    const quiet = 3;
    const vb = qr.size + quiet * 2;
    const rects = [];
    for (let r = 0; r < qr.size; r++) {
      for (let c = 0; c < qr.size; c++) {
        if (qr.grid[r][c] === 1) {
          rects.push(<rect key={`${r}-${c}`} x={c + quiet} y={r + quiet} width={1.03} height={1.03} fill="#070b14" />);
        }
      }
    }

    return (
      <div className="qr-frame" style={{ width: size, height: size }}>
        <svg viewBox={`0 0 ${vb} ${vb}`} width={size - 20} height={size - 20} shapeRendering="crispEdges" role="img" aria-label={`QR Code for ${value}`}>
          <rect x="0" y="0" width={vb} height={vb} fill="#ffffff" />
          {rects}
        </svg>
      </div>
    );
  }

  function BeBossPublicApp({
    lang,
    setLang,
    payConfig,
    releaseInfo,
    isAdminAuthenticated,
    onOpenAdminLogin,
    onOpenAdminConsole,
    notify
  }) {
    const { LANGUAGES, UI, DOCS } = global.BeBossI18n;
    const {
      calculateSubscriptionFee,
      buildMtnUssd,
      buildAirtelUssd,
      buildWhatsAppUrl
    } = global.BeBossCore;

    const t = UI[lang] || UI.en;
    const docsList = DOCS[lang] || DOCS.en;

    const copyText = (txt, msg = 'Copied!') => {
      navigator.clipboard.writeText(txt);
      notify(msg);
    };

    // Experience Center state
    const [expTab, setExpTab] = useState('pos');
    const [cart, setCart] = useState([
      { ...SAMPLE_PRODUCTS[0], qty: 2 },
      { ...SAMPLE_PRODUCTS[2], qty: 1 }
    ]);
    const [payMethod, setPayMethod] = useState('CASH');
    const [cashGiven, setCashGiven] = useState(5000);
    const [lastReceipt, setLastReceipt] = useState(null);

    // Multi-device Sync simulator state
    const [simDevices, setSimDevices] = useState([
      { id: 'd1', name: 'Owner Samsung A54', user: '@kigali_boss', role: 'OWNER', fee: 0, online: true },
      { id: 'd2', name: 'Counter 2 Techno Spark', user: '@marie_cashier', role: 'CASHIER', fee: 2000, online: true },
      { id: 'd3', name: 'Nyabugogo Branch Tablet', user: '@eric_mgr', role: 'MANAGER', fee: 2000, online: false }
    ]);
    const [syncStage, setSyncStage] = useState(0); // 0=Offline Sale Stored, 1=Network Connected, 2=WorkManager Push, 3=All Paired Terminals Updated

    // APK & QR Download Hub state
    const rawHref = useMemo(() => {
      const configured = String(payConfig.apk_download_url || '').trim();
      return configured || releaseInfo.apkRelativeUrl || './BeBoss-POS-v2.0.0.apk';
    }, [payConfig.apk_download_url, releaseInfo.apkRelativeUrl]);

    const resolvedApkUrl = useMemo(() => {
      try {
        return new URL(rawHref, window.location.href).href;
      } catch (_) {
        return rawHref;
      }
    }, [rawHref]);

    const portalUrl = useMemo(() => {
      try {
        return window.location.origin + window.location.pathname;
      } catch (_) {
        return 'https://beboss.app';
      }
    }, []);

    const waSupportUrl = useMemo(() => {
      const waNum = payConfig.admin_whatsapp_number || '250788765432';
      return buildWhatsAppUrl(waNum, 'Hello BeBoss Admin, I would like assistance installing & activating BeBoss POS.');
    }, [payConfig.admin_whatsapp_number, buildWhatsAppUrl]);

    const [qrMode, setQrMode] = useState('apk');
    const [qrCustomUrl, setQrCustomUrl] = useState('');
    const qrTargetUrl = useMemo(() => {
      if (qrCustomUrl.trim()) return qrCustomUrl.trim();
      if (qrMode === 'apk') return resolvedApkUrl;
      if (qrMode === 'portal') return portalUrl;
      return waSupportUrl;
    }, [qrMode, qrCustomUrl, resolvedApkUrl, portalUrl, waSupportUrl]);

    // Pricing calculator state
    const [calcBranches, setCalcBranches] = useState(1);
    const [calcWorkers, setCalcWorkers] = useState(2);
    const [calcDevices, setCalcDevices] = useState(2);
    const [calcMonths, setCalcMonths] = useState(1);
    const perDevRate = Number(payConfig.per_device_fee_rwf || 2000);
    const calcResult = useMemo(
      () => calculateSubscriptionFee(calcBranches, calcWorkers, calcMonths, calcDevices, perDevRate),
      [calcBranches, calcWorkers, calcMonths, calcDevices, perDevRate, calculateSubscriptionFee]
    );

    // Docs state
    const [docChapter, setDocChapter] = useState('ALL');
    const [docQuery, setDocQuery] = useState('');
    const filteredDocs = useMemo(() => {
      const q = docQuery.trim().toLowerCase();
      return docsList.filter(ch => {
        const chMatch = docChapter === 'ALL' || ch.id === docChapter;
        if (!chMatch) return false;
        if (!q) return true;
        return (
          ch.title.toLowerCase().includes(q) ||
          ch.summary.toLowerCase().includes(q) ||
          ch.badge.toLowerCase().includes(q) ||
          ch.bullets.some(b => b.toLowerCase().includes(q))
        );
      });
    }, [docsList, docChapter, docQuery]);

    // POS Demo helpers
    const cartSubtotal = useMemo(() => cart.reduce((s, it) => s + it.price * it.qty, 0), [cart]);
    const changeDue = payMethod === 'CASH' ? Math.max(0, Number(cashGiven || 0) - cartSubtotal) : 0;

    const addCartItem = (prod) => {
      setCart(prev => {
        const idx = prev.findIndex(x => x.id === prod.id);
        if (idx >= 0) {
          return prev.map((x, i) => (i === idx ? { ...x, qty: x.qty + 1 } : x));
        }
        return [...prev, { ...prod, qty: 1 }];
      });
    };

    const updateCartQty = (id, delta) => {
      setCart(prev =>
        prev
          .map(x => (x.id === id ? { ...x, qty: x.qty + delta } : x))
          .filter(x => x.qty > 0)
      );
    };

    const handleCompleteDemoSale = async () => {
      if (cart.length === 0) {
        notify('Add at least one item to test POS checkout.');
        return;
      }
      const receiptId = `REC-${Date.now().toString().slice(-6)}`;
      const payloadStr = `${receiptId}|${cartSubtotal}|${payMethod}|${cart.map(c => `${c.sku}:${c.qty}`).join(',')}`;
      const buf = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(payloadStr));
      const hash10 = Array.from(new Uint8Array(buf)).map(b => b.toString(16).padStart(2, '0')).join('').toUpperCase().slice(0, 10);
      setLastReceipt({
        id: receiptId,
        seal: `BB-${hash10}`,
        items: cart,
        total: cartSubtotal,
        method: payMethod,
        cashGiven: Number(cashGiven || cartSubtotal),
        change: changeDue,
        timestamp: new Date().toLocaleTimeString()
      });
      notify(`Sale completed & sealed offline: BB-${hash10}`);
    };

    const runSyncSimulation = () => {
      setSyncStage(1);
      setTimeout(() => setSyncStage(2), 450);
      setTimeout(() => {
        setSyncStage(3);
        setSimDevices(prev => prev.map(d => ({ ...d, online: true })));
        notify('WorkManager pushed Room SQLCipher changes to all shop terminals!');
      }, 1050);
    };

    const handlePrintQrPoster = () => {
      const qr = global.BeBossQR.encodeQR(qrTargetUrl);
      const quiet = 3;
      const vb = qr.size + quiet * 2;
      let cells = '';
      for (let r = 0; r < qr.size; r++) {
        for (let c = 0; c < qr.size; c++) {
          if (qr.grid[r][c] === 1) {
            cells += `<rect x="${c + quiet}" y="${r + quiet}" width="1" height="1" fill="#070b14"/>`;
          }
        }
      }
      const win = window.open('', '_blank', 'width=560,height=720');
      if (!win) {
        notify('Allow popups or use Save QR PNG.');
        return;
      }
      win.document.write(`<!DOCTYPE html><html><head><title>BeBoss POS QR Poster</title>
        <style>body{font-family:Inter,Arial,sans-serif;text-align:center;padding:36px;color:#0f172a;background:#fff}
        .box{border:3px solid #f97316;border-radius:24px;padding:28px;max-width:440px;margin:0 auto}
        .tag{display:inline-block;background:#fff7ed;color:#c2410c;font-weight:800;font-size:12px;padding:6px 12px;border-radius:99px;margin-bottom:10px}
        .url{font-family:monospace;font-size:11px;word-break:break-all;background:#f1f5f9;padding:8px;border-radius:8px;margin-top:12px}
        </style></head><body>
        <div class="box">
          <div class="tag">BEBOSS ENTERPRISE POS • v${payConfig.apk_version || releaseInfo.versionName}</div>
          <h1 style="margin:6px 0 6px;font-size:24px">Scan to Install BeBoss</h1>
          <p style="margin:0 0 18px;color:#475569;font-size:14px">EN • Ikinyarwanda • Français • Kiswahili</p>
          <svg viewBox="0 0 ${vb} ${vb}" width="260" height="260" shape-rendering="crispEdges">
            <rect width="${vb}" height="${vb}" fill="#fff"/>${cells}
          </svg>
          <div class="url">${qrTargetUrl}</div>
          <p style="margin:14px 0 0;font-size:12px;color:#64748b">4-Day Free Trial • Offline SQLCipher AES-256 • Multi-Device Cloud Sync</p>
        </div>
        <script>window.onload=function(){setTimeout(function(){window.print()},250)}</script>
        </body></html>`);
      win.document.close();
    };

    const extraSimDevices = Math.max(0, simDevices.length - 1);
    const simMonthlyFee = 5000 + extraSimDevices * perDevRate;

    return (
      <div className="pub-shell">
        {/* Top Sticky Glass Header */}
        <header className="pub-nav">
          <div className="pub-nav-inner">
            <a href="#top" className="pub-brand">
              <div className="brand-mark">BB</div>
              <div>
                <div style={{ fontWeight: 900, fontSize: 15, letterSpacing: '-0.02em' }}>BeBoss POS</div>
                <div style={{ fontSize: 10.5, color: 'var(--muted)', fontWeight: 600 }}>v{payConfig.apk_version || releaseInfo.versionName} • Enterprise Retail OS</div>
              </div>
            </a>

            <nav className="pub-links">
              <a href="#experience">{t.navExperience}</a>
              <a href="#download">{t.navDownload}</a>
              <a href="#pricing">{t.navPricing}</a>
              <a href="#docs">{t.navDocs}</a>
            </nav>

            <div className="pub-actions">
              <div className="lang-switcher" role="group" aria-label="Language Switcher">
                {LANGUAGES.map(l => (
                  <button
                    key={l.code}
                    type="button"
                    className={`lang-btn ${lang === l.code ? 'active' : ''}`}
                    onClick={() => setLang(l.code)}
                    title={l.label}
                  >
                    <span>{l.flag}</span>
                    <span>{l.short}</span>
                  </button>
                ))}
              </div>

              <a href="#download" className="btn btn-primary btn-sm pub-hide-xs">
                {t.btnDownloadApk}
              </a>

              {isAdminAuthenticated ? (
                <button type="button" className="btn btn-green btn-sm" onClick={onOpenAdminConsole}>
                  {t.btnAdminConsole}
                </button>
              ) : (
                <button type="button" className="btn btn-dark btn-sm" onClick={onOpenAdminLogin}>
                  {t.btnAdminLogin}
                </button>
              )}
            </div>
          </div>
        </header>

        {/* Hero Section */}
        <section id="top" className="pub-hero">
          <div className="pub-container hero-grid">
            <div>
              <div className="hero-pill">{t.heroPill}</div>
              <h1 className="hero-h1">{t.heroTitle}</h1>
              <p className="hero-p">{t.heroSub}</p>

              <div className="hero-ctas">
                <a href={resolvedApkUrl} download={releaseInfo.apkFileName} className="btn btn-primary btn-lg">
                  ⬇ {t.heroCtaDownload} ({releaseInfo.sizeDisplay})
                </a>
                <a href="#download" className="btn btn-green btn-lg" onClick={() => { setQrMode('apk'); setQrCustomUrl(''); }}>
                  ⊞ {t.heroCtaScan}
                </a>
                <a href="#experience" className="btn btn-dark btn-lg">
                  ▶ {t.heroCtaDemo}
                </a>
              </div>

              <div className="hero-kpis">
                <div className="hero-kpi">
                  <div className="hero-kpi-v" style={{ color: '#38bdf8' }}>{t.heroStat1}</div>
                  <div className="hero-kpi-s">{t.heroStat1Sub}</div>
                </div>
                <div className="hero-kpi">
                  <div className="hero-kpi-v" style={{ color: '#fdba74' }}>{t.heroStat2}</div>
                  <div className="hero-kpi-s">{t.heroStat2Sub}</div>
                </div>
                <div className="hero-kpi">
                  <div className="hero-kpi-v" style={{ color: '#34d399' }}>{t.heroStat3}</div>
                  <div className="hero-kpi-s">{t.heroStat3Sub}</div>
                </div>
                <div className="hero-kpi">
                  <div className="hero-kpi-v" style={{ color: '#c4b5fd' }}>{t.heroStat4}</div>
                  <div className="hero-kpi-s">{t.heroStat4Sub}</div>
                </div>
              </div>
            </div>

            {/* Live Device Mockup Preview */}
            <div className="phone-mockup-wrap">
              <div className="phone-frame">
                <div className="phone-notch" />
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
                  <div>
                    <div style={{ fontSize: 10, color: '#34d399', fontWeight: 800 }}>● SUPABASE AUTO-SYNC ON</div>
                    <div style={{ fontWeight: 900, fontSize: 16 }}>BeBoss Terminal • {lang.toUpperCase()}</div>
                  </div>
                  <span className="badge b-orange">TRIAL 4D</span>
                </div>

                <div className="phone-stat-card">
                  <div style={{ fontSize: 11, color: '#94a3b8' }}>TODAY NET SALES (OFFLINE-FIRST)</div>
                  <div style={{ fontSize: 24, fontWeight: 900, color: '#f8fafc', marginTop: 2 }}>248,500 FRw</div>
                  <div style={{ display: 'flex', gap: 10, marginTop: 8, fontSize: 11.5 }}>
                    <span style={{ color: '#34d399' }}>Cash: 142,000</span>
                    <span style={{ color: '#fbbf24' }}>MoMo: 86,500</span>
                    <span style={{ color: '#f87171' }}>Credit: 20,000</span>
                  </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, marginTop: 10 }}>
                  <div className="phone-mini-tile">
                    <div style={{ fontSize: 10.5, color: 'var(--muted)' }}>PAIRED DEVICES</div>
                    <div style={{ fontWeight: 800, fontSize: 14, marginTop: 2 }}>3 / 3 Online</div>
                  </div>
                  <div className="phone-mini-tile">
                    <div style={{ fontSize: 10.5, color: 'var(--muted)' }}>14D FORECAST</div>
                    <div style={{ fontWeight: 800, fontSize: 14, color: '#fbbf24', marginTop: 2 }}>4 SKUs Low</div>
                  </div>
                </div>

                <div className="phone-mini-tile" style={{ marginTop: 10 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 11, marginBottom: 4 }}>
                    <span style={{ color: '#fdba74', fontWeight: 800 }}>BB-7F9A31C204 SEALED</span>
                    <span style={{ color: '#34d399' }}>AES-256</span>
                  </div>
                  <div style={{ fontSize: 11.5, color: 'var(--muted)' }}>
                    2x Inyange Milk • 1x Akabanga = <strong style={{ color: '#fff' }}>4,100 FRw</strong>
                  </div>
                </div>

                <div style={{ marginTop: 12, display: 'flex', gap: 6 }}>
                  <a href="#experience" className="btn btn-primary btn-sm" style={{ flex: 1 }}>Test POS Counter</a>
                  <a href="#download" className="btn btn-dark btn-sm" style={{ flex: 1 }}>Install APK</a>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* Interactive Experience Sandbox */}
        <section id="experience" className="pub-section">
          <div className="pub-container">
            <div className="section-head">
              <h2>{t.expTitle}</h2>
              <p>{t.expSub}</p>
            </div>

            <div className="exp-tabs">
              <button type="button" className={`exp-tab ${expTab === 'pos' ? 'active' : ''}`} onClick={() => setExpTab('pos')}>{t.expTabPos}</button>
              <button type="button" className={`exp-tab ${expTab === 'sync' ? 'active' : ''}`} onClick={() => setExpTab('sync')}>{t.expTabSync}</button>
              <button type="button" className={`exp-tab ${expTab === 'pay' ? 'active' : ''}`} onClick={() => setExpTab('pay')}>{t.expTabPay}</button>
            </div>

            {expTab === 'pos' && (
              <div className="exp-grid">
                <div className="card">
                  <div style={{ fontWeight: 800, fontSize: 14, marginBottom: 10, color: '#fdba74' }}>{t.posAddItem}</div>
                  <div className="catalog-grid">
                    {SAMPLE_PRODUCTS.map(prod => (
                      <button key={prod.id} type="button" className="catalog-item" onClick={() => addCartItem(prod)}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 10.5, color: 'var(--muted)' }}>
                          <span className="mono">{prod.sku}</span>
                          <span>{prod.stock} left</span>
                        </div>
                        <div style={{ fontWeight: 800, fontSize: 13.5, margin: '4px 0' }}>{prod.name}</div>
                        <div style={{ fontWeight: 900, color: '#34d399', fontSize: 13 }}>{prod.price.toLocaleString()} FRw</div>
                      </button>
                    ))}
                  </div>
                </div>

                <div className="card" style={{ display: 'grid', gap: 12 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div style={{ fontWeight: 800, fontSize: 15 }}>{t.posCartTitle}</div>
                    <button type="button" className="btn btn-dark btn-sm" onClick={() => { setCart([]); setLastReceipt(null); }}>{t.posClear}</button>
                  </div>

                  {cart.length === 0 ? (
                    <div style={{ padding: 18, borderRadius: 12, border: '1px dashed var(--border)', color: 'var(--muted)', textAlign: 'center', fontSize: 13 }}>
                      Tap products on the left to build a live counter basket.
                    </div>
                  ) : (
                    <div style={{ display: 'grid', gap: 8 }}>
                      {cart.map(item => (
                        <div key={item.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 8, padding: '8px 10px', borderRadius: 10, background: '#0b1222' }}>
                          <div>
                            <div style={{ fontWeight: 700, fontSize: 13 }}>{item.name}</div>
                            <div style={{ fontSize: 11.5, color: 'var(--muted)' }}>{item.price.toLocaleString()} FRw × {item.qty} = <strong style={{ color: '#fff' }}>{(item.price * item.qty).toLocaleString()} FRw</strong></div>
                          </div>
                          <div style={{ display: 'flex', gap: 5 }}>
                            <button type="button" className="btn btn-dark btn-sm" onClick={() => updateCartQty(item.id, -1)}>−</button>
                            <button type="button" className="btn btn-dark btn-sm" onClick={() => updateCartQty(item.id, 1)}>+</button>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}

                  <div>
                    <label className="field-label">{t.posPayMethod}</label>
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 6 }}>
                      {['CASH', 'MTN MoMo', 'SPLIT', 'CREDIT'].map(pm => (
                        <button key={pm} type="button" className={`btn btn-sm ${payMethod === pm ? 'btn-primary' : 'btn-dark'}`} onClick={() => setPayMethod(pm)}>
                          {pm}
                        </button>
                      ))}
                    </div>
                  </div>

                  {payMethod === 'CASH' && (
                    <div className="grid-2">
                      <div>
                        <label className="field-label">{t.posCashGiven}</label>
                        <input type="number" className="input" value={cashGiven} onChange={e => setCashGiven(e.target.value)} />
                      </div>
                      <div style={{ background: '#0b1222', borderRadius: 10, padding: '8px 12px', display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
                        <span style={{ fontSize: 11, color: 'var(--muted)' }}>Change Due</span>
                        <strong style={{ fontSize: 16, color: '#34d399' }}>{changeDue.toLocaleString()} FRw</strong>
                      </div>
                    </div>
                  )}

                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingTop: 6, borderTop: '1px solid var(--border)' }}>
                    <div style={{ fontSize: 18, fontWeight: 900 }}>{cartSubtotal.toLocaleString()} FRw</div>
                    <button type="button" className="btn btn-green" onClick={handleCompleteDemoSale}>
                      ✓ {t.posCompleteSale}
                    </button>
                  </div>

                  {lastReceipt && (
                    <div className="receipt-paper">
                      <div style={{ fontWeight: 900, fontSize: 13 }}>BEBOSS DEMO SUPERMARKET</div>
                      <div style={{ fontSize: 11, color: '#475569' }}>58mm Thermal • {lastReceipt.id} • {lastReceipt.timestamp}</div>
                      <hr style={{ border: 'none', borderTop: '1px dashed #94a3b8', margin: '8px 0' }} />
                      {lastReceipt.items.map(it => (
                        <div key={it.id} style={{ display: 'flex', justifyContent: 'space-between', fontSize: 11.5 }}>
                          <span>{it.qty}x {it.name}</span>
                          <span>{(it.qty * it.price).toLocaleString()}</span>
                        </div>
                      ))}
                      <hr style={{ border: 'none', borderTop: '1px dashed #94a3b8', margin: '8px 0' }} />
                      <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 900, fontSize: 13 }}>
                        <span>TOTAL ({lastReceipt.method})</span>
                        <span>{lastReceipt.total.toLocaleString()} FRw</span>
                      </div>
                      <div style={{ marginTop: 8, padding: 6, background: '#ecfdf5', borderRadius: 6, color: '#065f46', fontSize: 11, fontWeight: 800 }}>
                        ✓ {t.posSealValid}: <span className="mono">{lastReceipt.seal}</span>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            )}

            {expTab === 'sync' && (
              <div className="card">
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12, marginBottom: 14 }}>
                  <div>
                    <h3 style={{ margin: 0, fontSize: 17 }}>Single-Shop Multi-Device Cloud Sync (+{perDevRate.toLocaleString()} FRw / Extra Device)</h3>
                    <div style={{ fontSize: 12.5, color: 'var(--muted)', marginTop: 3 }}>
                      Every added terminal shares one unified stock, sales, debts, and user directory via WorkManager background sync (`NetworkType.CONNECTED`).
                    </div>
                  </div>
                  <div style={{ display: 'flex', gap: 8 }}>
                    <button
                      type="button"
                      className="btn btn-dark btn-sm"
                      onClick={() => {
                        const n = simDevices.length + 1;
                        setSimDevices(prev => [
                          ...prev,
                          {
                            id: `d${Date.now()}`,
                            name: `POS Counter #${n}`,
                            user: `@cashier_${n}`,
                            role: 'CASHIER',
                            fee: perDevRate,
                            online: false
                          }
                        ]);
                      }}
                    >
                      + Add Device Slot (+{perDevRate.toLocaleString()} FRw)
                    </button>
                    <button type="button" className="btn btn-primary btn-sm" onClick={runSyncSimulation}>
                      ⚡ Simulate Closed-App Auto-Sync
                    </button>
                  </div>
                </div>

                <div className="sync-pipeline">
                  {[
                    '1. Cashier rings sale offline (Room SQLCipher AES-256 + SyncQueue Outbox)',
                    '2. Internet returns (NetworkType.CONNECTED)',
                    '3. Background WorkManager wakes even if app is closed',
                    '4. Supabase pushes update to Owner & all paired shop terminals'
                  ].map((label, idx) => (
                    <div key={idx} className={`sync-step ${syncStage >= idx ? 'active' : ''}`}>
                      <div style={{ fontSize: 11, fontWeight: 800, color: syncStage >= idx ? '#34d399' : 'var(--muted)' }}>
                        {syncStage >= idx ? 'COMPLETED' : 'QUEUED'}
                      </div>
                      <div style={{ fontSize: 12.5, fontWeight: 700, marginTop: 4 }}>{label}</div>
                    </div>
                  ))}
                </div>

                <div className="grid-3" style={{ marginTop: 14 }}>
                  {simDevices.map((d, idx) => (
                    <div key={d.id} className="phone-mini-tile" style={{ padding: 14 }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <strong>{d.name}</strong>
                        <span className={`badge ${idx === 0 ? 'b-green' : 'b-orange'}`}>
                          {idx === 0 ? 'INCLUDED' : `+${perDevRate.toLocaleString()} FRw`}
                        </span>
                      </div>
                      <div className="mono" style={{ fontSize: 12, color: '#fdba74', marginTop: 4 }}>{d.user} • {d.role}</div>
                      <div style={{ fontSize: 11.5, color: d.online ? '#34d399' : '#fbbf24', marginTop: 6 }}>
                        {d.online ? '● Synced with Owner Cloud' : '○ Waiting for internet auto-sync...'}
                      </div>
                    </div>
                  ))}
                </div>

                <div style={{ marginTop: 14, display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 10, paddingTop: 12, borderTop: '1px solid var(--border)' }}>
                  <div style={{ fontSize: 13 }}>
                    Paired Terminals: <strong>{simDevices.length}</strong> (1st free + {extraSimDevices} extra) • Monthly Fee: <strong style={{ color: '#fdba74' }}>{simMonthlyFee.toLocaleString()} FRw/mo</strong>
                  </div>
                  <div style={{ fontSize: 12, color: '#34d399' }}>Managed dynamically by Master Admin in Supabase</div>
                </div>
              </div>
            )}

            {expTab === 'pay' && (
              <div className="grid-3">
                <div className="card">
                  <span className="badge b-orange">STEP 1 • DYNAMIC USSD</span>
                  <h3 style={{ margin: '10px 0 6px', fontSize: 16 }}>Dial *182# MTN MoMo or *500# Airtel Money</h3>
                  <p style={{ color: 'var(--muted)', fontSize: 13 }}>
                    BeBoss automatically fills the live Admin Merchant Code, phone numbers, and exact plan + device fee into the USSD string from Supabase <span className="mono">app_config</span>.
                  </p>
                  <div className="mono" style={{ padding: 10, borderRadius: 10, background: '#090e1a', fontSize: 12, color: '#fdba74' }}>
                    {buildMtnUssd(calcResult.total, payConfig)}
                  </div>
                </div>
                <div className="card">
                  <span className="badge b-blue">STEP 2 • WHATSAPP PROOF</span>
                  <h3 style={{ margin: '10px 0 6px', fontSize: 16 }}>Send Full Payer Credentials</h3>
                  <p style={{ color: 'var(--muted)', fontSize: 13 }}>
                    With 1 tap, BeBoss prepares a verified WhatsApp proof message to Admin (<span className="mono">{payConfig.admin_whatsapp_number || 'Configured by Admin'}</span>) with your <strong>@username</strong>, <strong>Registered Shop Phone</strong>, <strong>Sender MoMo Phone</strong>, and <strong>Full Names on Sender MoMo</strong>.
                  </p>
                </div>
                <div className="card">
                  <span className="badge b-green">STEP 3 • DUAL ADMIN UNLOCK</span>
                  <h3 style={{ margin: '10px 0 6px', fontSize: 16 }}>Unlock Code OR Direct Auto-Unlock</h3>
                  <p style={{ color: 'var(--muted)', fontSize: 13 }}>
                    Admin verifies your MoMo receipt and chooses <strong>Option 1</strong> (issue offline cryptographic <span className="mono">BB2-...</span> code) OR <strong>Option 2</strong> (Direct Auto-Unlock so your phone unlocks automatically when internet syncs — zero code typing needed).
                  </p>
                </div>
              </div>
            )}
          </div>
        </section>

        {/* Download APK + Real Scannable QR Hub */}
        <section id="download" className="pub-section alt-bg">
          <div className="pub-container">
            <div className="section-head">
              <h2>{t.dlTitle}</h2>
              <p>{t.dlSub}</p>
            </div>

            <div className="dl-grid">
              {/* Left: Manual APK Package Download */}
              <div className="card dl-card">
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 10 }}>
                  <div>
                    <span className="badge b-green">ANDROID RELEASE • SIGNED APK</span>
                    <h3 style={{ margin: '8px 0 2px', fontSize: 21 }}>
                      BeBoss Enterprise POS v{payConfig.apk_version || releaseInfo.versionName}
                    </h3>
                    <div style={{ fontSize: 12.5, color: 'var(--muted)' }}>
                      {releaseInfo.apkFileName} • {releaseInfo.sizeDisplay} • {releaseInfo.minAndroid} • 4 Languages
                    </div>
                  </div>
                  <div className="brand-mark" style={{ width: 52, height: 52, fontSize: 20 }}>BB</div>
                </div>

                <a
                  href={resolvedApkUrl}
                  download={releaseInfo.apkFileName}
                  className="btn btn-primary btn-lg"
                  style={{ width: '100%', marginTop: 18, padding: '15px 18px', fontSize: 15 }}
                >
                  ⬇ {t.dlDirectBtn} ({releaseInfo.sizeDisplay})
                </a>

                <div style={{ marginTop: 14, padding: 12, borderRadius: 12, background: '#090e1a', border: '1px solid var(--border)' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 4 }}>
                    <span style={{ fontSize: 11, fontWeight: 800, color: 'var(--muted)', textTransform: 'uppercase' }}>{t.dlChecksum}</span>
                    <button type="button" className="btn btn-dark btn-sm" onClick={() => copyText(releaseInfo.sha256, 'SHA-256 copied!')}>
                      {t.dlCopyHash}
                    </button>
                  </div>
                  <div className="mono" style={{ fontSize: 11.5, color: '#34d399', wordBreak: 'break-all' }}>{releaseInfo.sha256}</div>
                </div>

                <div style={{ marginTop: 18 }}>
                  <div style={{ fontWeight: 800, fontSize: 14, marginBottom: 10 }}>{t.dlInstallStepsTitle}</div>
                  <div style={{ display: 'grid', gap: 8 }}>
                    {[t.dlStep1, t.dlStep2, t.dlStep3, t.dlStep4].map((step, i) => (
                      <div key={i} className="install-step">
                        <span className="step-num">{i + 1}</span>
                        <span>{step}</span>
                      </div>
                    ))}
                  </div>
                </div>
              </div>

              {/* Right: Live Scannable QR Studio */}
              <div className="card qr-card">
                <div style={{ fontWeight: 800, fontSize: 17, marginBottom: 4 }}>{t.qrTitle}</div>
                <div style={{ fontSize: 12, color: 'var(--muted)', marginBottom: 14 }}>
                  ISO/IEC 18004 Reed-Solomon QR • Works natively with Android & iPhone cameras
                </div>

                <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', justifyContent: 'center', marginBottom: 16 }}>
                  {[
                    { id: 'apk', label: t.qrModeApk },
                    { id: 'portal', label: t.qrModePortal },
                    { id: 'wa', label: t.qrModeWa }
                  ].map(m => (
                    <button
                      key={m.id}
                      type="button"
                      className={`btn btn-sm ${qrMode === m.id && !qrCustomUrl ? 'btn-primary' : 'btn-dark'}`}
                      onClick={() => { setQrMode(m.id); setQrCustomUrl(''); }}
                    >
                      {m.label}
                    </button>
                  ))}
                </div>

                <QrSvg value={qrTargetUrl} size={230} />

                <div style={{ width: '100%', marginTop: 14, textAlign: 'left' }}>
                  <label className="field-label">{t.qrCustomLabel}</label>
                  <input
                    className="input mono"
                    style={{ fontSize: 11.5 }}
                    value={qrTargetUrl}
                    onChange={e => setQrCustomUrl(e.target.value)}
                  />
                </div>

                <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', justifyContent: 'center', marginTop: 14, width: '100%' }}>
                  <button
                    type="button"
                    className="btn btn-green btn-sm"
                    onClick={() => {
                      global.BeBossQR.downloadQrPng(qrTargetUrl, `beboss-${qrMode}-qr.png`);
                      notify('QR Code PNG downloaded!');
                    }}
                  >
                    {t.qrDownloadPng}
                  </button>
                  <button type="button" className="btn btn-dark btn-sm" onClick={() => copyText(qrTargetUrl, 'Target URL copied!')}>
                    {t.qrCopyLink}
                  </button>
                  <button type="button" className="btn btn-dark btn-sm" onClick={handlePrintQrPoster}>
                    {t.qrPrintPoster}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* Live Pricing, Multi-Device & USSD Calculator */}
        <section id="pricing" className="pub-section">
          <div className="pub-container">
            <div className="section-head">
              <h2>{t.priceTitle}</h2>
              <p>{t.priceSub}</p>
            </div>

            <div className="dl-grid">
              <div className="card" style={{ display: 'grid', gap: 16 }}>
                <div className="grid-2">
                  <div>
                    <label className="field-label">{t.priceBranches}: <strong style={{ color: '#fff' }}>{calcBranches}</strong></label>
                    <input type="range" min={1} max={5} value={calcBranches} onChange={e => setCalcBranches(Number(e.target.value))} style={{ width: '100%' }} />
                    <div style={{ fontSize: 11.5, color: 'var(--muted)' }}>1 Branch = 5,000 FRw • 2 = 10,000 FRw • 3+ = 20,000 FRw</div>
                  </div>
                  <div>
                    <label className="field-label">{t.priceWorkers}: <strong style={{ color: '#fff' }}>{calcWorkers}</strong></label>
                    <input type="range" min={1} max={12} value={calcWorkers} onChange={e => setCalcWorkers(Number(e.target.value))} style={{ width: '100%' }} />
                    <div style={{ fontSize: 11.5, color: 'var(--muted)' }}>1st Staff Free • +2,000 (2-3) • +4,000 (4-5)</div>
                  </div>
                </div>

                <div>
                  <label className="field-label">{t.priceDevices}: <strong style={{ color: '#fdba74' }}>{calcDevices}</strong> (+{perDevRate.toLocaleString()} FRw/mo per extra)</label>
                  <input type="range" min={1} max={10} value={calcDevices} onChange={e => setCalcDevices(Number(e.target.value))} style={{ width: '100%' }} />
                  <div style={{ fontSize: 11.5, color: '#34d399' }}>
                    {calcDevices === 1 ? 'Owner primary device included at 0 extra fee.' : `1 Owner primary device free + ${calcResult.extraDevices} extra devices × ${perDevRate.toLocaleString()} = +${calcResult.deviceFee.toLocaleString()} FRw/mo`}
                  </div>
                </div>

                <div>
                  <label className="field-label">{t.priceMonths}</label>
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 8 }}>
                    {[
                      { m: 1, label: '1 Mo' },
                      { m: 3, label: '3 Mo (-10%)' },
                      { m: 6, label: '6 Mo' },
                      { m: 12, label: '12 Mo (-20%)' }
                    ].map(opt => (
                      <button
                        key={opt.m}
                        type="button"
                        className={`btn btn-sm ${calcMonths === opt.m ? 'btn-primary' : 'btn-dark'}`}
                        onClick={() => setCalcMonths(opt.m)}
                      >
                        {opt.label}
                      </button>
                    ))}
                  </div>
                </div>
              </div>

              <div className="card" style={{ display: 'grid', gap: 12, background: 'linear-gradient(160deg, var(--card), #0c1428)' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span className="badge b-orange">{calcResult.tier} TIER • +{calcResult.days} DAYS</span>
                  {calcResult.discount > 0 && <span className="badge b-green">{calcResult.discount}% OFF</span>}
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
                  <div className="phone-mini-tile">
                    <div style={{ fontSize: 11, color: 'var(--muted)' }}>{t.priceMonthly}</div>
                    <div style={{ fontSize: 18, fontWeight: 900 }}>{calcResult.monthly.toLocaleString()} FRw</div>
                  </div>
                  <div className="phone-mini-tile" style={{ borderColor: 'rgba(16,185,129,0.4)' }}>
                    <div style={{ fontSize: 11, color: '#34d399' }}>{t.priceTotal}</div>
                    <div style={{ fontSize: 22, fontWeight: 900, color: '#34d399' }}>{calcResult.total.toLocaleString()} FRw</div>
                  </div>
                </div>

                <div style={{ background: '#090e1a', border: '1px solid var(--border)', borderRadius: 12, padding: 12, display: 'grid', gap: 8 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 8 }}>
                    <div>
                      <div style={{ fontSize: 10.5, color: '#fbbf24', fontWeight: 800 }}>{t.priceMtnLabel} ({payConfig.mtn_recipient_name})</div>
                      <div className="mono" style={{ fontSize: 13 }}>{buildMtnUssd(calcResult.total, payConfig)}</div>
                    </div>
                    <button type="button" className="btn btn-dark btn-sm" onClick={() => copyText(buildMtnUssd(calcResult.total, payConfig), 'MTN USSD copied!')}>Copy</button>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 8, paddingTop: 8, borderTop: '1px solid rgba(37,52,86,0.6)' }}>
                    <div>
                      <div style={{ fontSize: 10.5, color: '#fca5a5', fontWeight: 800 }}>{t.priceAirtelLabel} ({payConfig.airtel_recipient_name})</div>
                      <div className="mono" style={{ fontSize: 13 }}>{buildAirtelUssd(calcResult.total, payConfig)}</div>
                    </div>
                    <button type="button" className="btn btn-dark btn-sm" onClick={() => copyText(buildAirtelUssd(calcResult.total, payConfig), 'Airtel USSD copied!')}>Copy</button>
                  </div>
                </div>

                <a
                  className="btn btn-green"
                  target="_blank"
                  rel="noopener noreferrer"
                  href={buildWhatsAppUrl(
                    payConfig.admin_whatsapp_number || '250788765432',
                    `Hello BeBoss Admin! I am interested in BeBoss POS (${calcBranches} branches, ${calcWorkers} staff, ${calcDevices} paired devices, ${calcMonths} mo = ${calcResult.total.toLocaleString()} FRw).`
                  )}
                >
                  💬 {t.priceAskWa}
                </a>
              </div>
            </div>
          </div>
        </section>

        {/* Full 4-Language Documentation Encyclopedia */}
        <section id="docs" className="pub-section alt-bg">
          <div className="pub-container">
            <div className="section-head">
              <h2>{t.docsTitle}</h2>
              <p>{t.docsSub}</p>
            </div>

            <div className="card" style={{ marginBottom: 16, display: 'flex', gap: 12, flexWrap: 'wrap', alignItems: 'center' }}>
              <input
                className="input"
                style={{ flex: '1 1 280px' }}
                placeholder={t.docsSearchPlaceholder}
                value={docQuery}
                onChange={e => setDocQuery(e.target.value)}
              />
              <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                <button
                  type="button"
                  className={`btn btn-sm ${docChapter === 'ALL' ? 'btn-primary' : 'btn-dark'}`}
                  onClick={() => setDocChapter('ALL')}
                >
                  {t.docsAllChapters}
                </button>
                {docsList.map((ch, idx) => (
                  <button
                    key={ch.id}
                    type="button"
                    className={`btn btn-sm ${docChapter === ch.id ? 'btn-primary' : 'btn-dark'}`}
                    onClick={() => setDocChapter(ch.id)}
                  >
                    0{idx + 1}
                  </button>
                ))}
              </div>
            </div>

            <div className="docs-grid">
              {filteredDocs.length === 0 && (
                <div className="card" style={{ textAlign: 'center', color: 'var(--muted)' }}>
                  No documentation chapters match "{docQuery}".
                </div>
              )}
              {filteredDocs.map(ch => (
                <article key={ch.id} className="card doc-card">
                  <span className="badge b-purple">{ch.badge}</span>
                  <h3 style={{ margin: '10px 0 4px', fontSize: 17 }}>{ch.title}</h3>
                  <p style={{ margin: '0 0 12px', color: 'var(--muted)', fontSize: 13 }}>{ch.summary}</p>
                  <ul className="doc-bullets">
                    {ch.bullets.map((b, i) => (
                      <li key={i}>{b}</li>
                    ))}
                  </ul>
                </article>
              ))}
            </div>
          </div>
        </section>

        {/* Footer */}
        <footer className="pub-footer">
          <div className="pub-container" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
            <div>
              <strong style={{ color: '#fff' }}>BeBoss POS</strong> — {t.footerNote}
            </div>
            <div style={{ display: 'flex', gap: 10, alignItems: 'center', flexWrap: 'wrap' }}>
              <a href={resolvedApkUrl} download={releaseInfo.apkFileName} style={{ color: '#fdba74', fontWeight: 700 }}>
                {releaseInfo.apkFileName}
              </a>
              <span>•</span>
              <button
                type="button"
                className="btn btn-dark btn-sm"
                onClick={isAdminAuthenticated ? onOpenAdminConsole : onOpenAdminLogin}
              >
                {isAdminAuthenticated ? t.btnAdminConsole : t.btnAdminLogin}
              </button>
            </div>
          </div>
        </footer>
      </div>
    );
  }

  global.BeBossPublicApp = BeBossPublicApp;
})(window);
