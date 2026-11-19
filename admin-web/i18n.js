// Full 4-Language Localization & Complete App Documentation Encyclopedia (EN, RW, FR, SW)
(function (global) {
  const LANGUAGES = [
    { code: 'en', label: 'English', short: 'EN', flag: '🇬🇧' },
    { code: 'rw', label: 'Ikinyarwanda', short: 'RW', flag: '🇷🇼' },
    { code: 'fr', label: 'Français', short: 'FR', flag: '🇫🇷' },
    { code: 'sw', label: 'Kiswahili', short: 'SW', flag: '🇹🇿' }
  ];

  const UI = {
    en: {
      navExperience: 'Live Demo',
      navDownload: 'Download & QR',
      navPricing: 'Pricing & USSD',
      navDocs: 'Full Docs (4 Lang)',
      btnDownloadApk: 'Download APK',
      btnAdminLogin: 'Admin Login',
      btnAdminConsole: 'Admin Console',
      btnPublicSite: 'Public App Site',
      heroPill: 'OFFLINE-FIRST ENTERPRISE POS • 4 LANGUAGES • AES-256 ENCRYPTED',
      heroTitle: 'Turn Any Android Phone Into a Smart Multi-Branch POS Terminal.',
      heroSub: 'Sell faster offline, scan barcodes, track MTN MoMo & Split Cash payments, recover customer debts (Amadeni), forecast 14-day restocks, and auto-sync all paired shop devices whenever internet connects.',
      heroCtaDownload: 'Download BeBoss APK Now',
      heroCtaScan: 'Scan QR on Phone',
      heroCtaDemo: 'Try Interactive POS Demo',
      heroStat1: '4-Day Free Trial',
      heroStat1Sub: 'Full enterprise access',
      heroStat2: '5,000 FRw/mo',
      heroStat2Sub: '1st owner device included',
      heroStat3: '+2,000 FRw/mo',
      heroStat3Sub: 'Per extra paired terminal',
      heroStat4: '100% Offline',
      heroStat4Sub: 'Auto-syncs on internet',
      expTitle: 'Interactive BeBoss Experience Center',
      expSub: 'Test the checkout counter, multi-device auto-sync, and *182# USSD unlock workflow right in your browser before installing.',
      expTabPos: '1. Live POS & Receipt Seal',
      expTabSync: '2. Multi-Device Auto-Sync',
      expTabPay: '3. USSD & Dual Unlock',
      posAddItem: 'Tap Product to Ring Up',
      posCartTitle: 'Active Counter Basket',
      posPayMethod: 'Payment Method',
      posCashGiven: 'Cash Given (FRw)',
      posCompleteSale: 'Complete Sale & Generate Seal',
      posClear: 'Clear',
      posReceiptTitle: '58mm Sealed Thermal Receipt',
      posSealValid: 'SHA-256 Offline Seal Verified',
      dlTitle: 'Download BeBoss Android App & Scannable QR Hub',
      dlSub: 'Install manually with the verified APK package or scan the live QR code with your Android camera.',
      dlDirectBtn: 'Download APK Package',
      dlChecksum: 'SHA-256 Checksum',
      dlCopyHash: 'Copy SHA-256',
      dlInstallStepsTitle: '4-Step Android Installation Guide',
      dlStep1: 'Tap "Download APK Package" above or scan the QR code on your Android phone.',
      dlStep2: 'Open the downloaded BeBoss-POS APK file from Chrome Downloads or Files.',
      dlStep3: 'When Android prompts for permission, enable "Allow from this source" (Play Protect verified safe).',
      dlStep4: 'Open BeBoss, pick EN/RW/FR/SW, and launch your 4-Day Free Trial or tap "Join Existing Shop".',
      qrTitle: 'Scan QR Code to Install on Any Phone',
      qrModeApk: 'APK Download Link',
      qrModePortal: 'Open Web Portal',
      qrModeWa: 'Admin WhatsApp',
      qrCustomLabel: 'QR Target URL (auto-updates QR instantly)',
      qrDownloadPng: 'Save QR PNG',
      qrCopyLink: 'Copy URL',
      qrPrintPoster: 'Print QR Poster',
      priceTitle: 'Live Subscription, Multi-Device & USSD Calculator',
      priceSub: 'Transparent RWF pricing synced directly with BeBoss Cloud. Pay via *182# MTN MoMo or *500# Airtel Money.',
      priceBranches: 'Active Branches',
      priceWorkers: 'Staff Operators',
      priceDevices: 'Paired Devices (1st Free)',
      priceMonths: 'Billing Cycle',
      priceMonthly: 'Monthly Subtotal',
      priceDiscount: 'Cycle Discount',
      priceTotal: 'Total Payable',
      priceMtnLabel: 'MTN MoMo USSD',
      priceAirtelLabel: 'Airtel Money USSD',
      priceAskWa: 'Send Plan Inquiry on WhatsApp',
      docsTitle: 'Complete BeBoss Documentation & Playbook (4 Languages)',
      docsSub: 'Every screen, workflow, receipt seal, USSD payment rule, multi-device pairing flow, and security layer explained in detail.',
      docsSearchPlaceholder: 'Search documentation topics (e.g. USSD, Join Code, Forecast, PIN, Thermal)...',
      docsAllChapters: 'All 8 Chapters',
      footerNote: 'BeBoss Enterprise POS • Offline-First Android Retail OS • Powered by Room SQLCipher, WorkManager & Supabase Cloud'
    },
    rw: {
      navExperience: 'Igerageza',
      navDownload: 'Kuramo APK & QR',
      navPricing: 'Ibiciro & USSD',
      navDocs: 'Inyoborabikorwa',
      btnDownloadApk: 'Kuramo APK',
      btnAdminLogin: 'Kwinjira kwa Admin',
      btnAdminConsole: 'Ubuyobozi bwa Admin',
      btnPublicSite: 'Urubuga Rusange',
      heroPill: 'POS IKORA OFFLINE • INDIMI 4 • UMUTEKANO WA AES-256',
      heroTitle: 'Hindura Telefoni ya Android Ibe POS Yizewe y’Amaduka n’Amashami.',
      heroSub: 'Curuza utagendeye kuri interineti, soma kode z’ibicuruzwa (Barcode), wakire MTN MoMo na Cash icyarimwe, ukurikirane amadeni y’abakiriya, umenye ibishira mu bubiko mu minsi 14, kandi uhuze telefoni zose zawe interineti ibonetse.',
      heroCtaDownload: 'Kuramo BeBoss APK Ubu',
      heroCtaScan: 'Sikana QR kuri Telefoni',
      heroCtaDemo: 'Gerageza POS Hano',
      heroStat1: 'Iminsi 4 y’Ubuntu',
      heroStat1Sub: 'Uburenganzira bwuzuye',
      heroStat2: '5,000 FRw/ukwezi',
      heroStat2Sub: 'Telefoni ya 1 y’ubuntu',
      heroStat3: '+2,000 FRw/ukwezi',
      heroStat3Sub: 'Kuri buri telefoni yiyongereye',
      heroStat4: '100% Offline',
      heroStat4Sub: 'Ihuza amakuru mu bicu',
      expTitle: 'Igerageza rya BeBoss Imbonankubone',
      expSub: 'Gerageza uburyo bwo gucuruza, guhuza telefoni nyinshi, no kwishyura ukoresheje *182# mbere yo gushyira porogaramu muri telefoni.',
      expTabPos: '1. Gucuruza & Inyemezabuguzi',
      expTabSync: '2. Guhuza Telefoni Nyinshi',
      expTabPay: '3. USSD & Gufungura Konti',
      posAddItem: 'Kanda Igicuruzwa Ugishyire mu Gitebo',
      posCartTitle: 'Ibicuruzwa mu Gitebo',
      posPayMethod: 'Uburyo bwo Kwishyura',
      posCashGiven: 'Amafaranga Umukiriya Atanze (FRw)',
      posCompleteSale: 'Emeza Ugurisha & Kora Seal',
      posClear: 'Siba',
      posReceiptTitle: 'Inyemezabuguzi ya 58mm Irinzwe',
      posSealValid: 'Ikirango cya SHA-256 Cyemejwe Offline',
      dlTitle: 'Kuramo Porogaramu ya BeBoss Android Cyangwa Sikana QR',
      dlSub: 'Kuramo dosiye ya APK ku buryo busanzwe cyangwa usikane QR Code ukoresheje kamera ya telefoni yawe.',
      dlDirectBtn: 'Kuramo Dosiye ya APK',
      dlChecksum: 'Umubare w’Umutekano (SHA-256)',
      dlCopyHash: 'Koporora SHA-256',
      dlInstallStepsTitle: 'Intambwe 4 zo Gushyira BeBoss muri Android',
      dlStep1: 'Kanda "Kuramo Dosiye ya APK" cyangwa usikane QR Code kuri telefoni ya Android.',
      dlStep2: 'Fungura dosiye ya BeBoss-POS APK imaze kumanuka muri Downloads.',
      dlStep3: 'Android nibisaba, emeza "Allow from this source" kugira ngo yinjire neza.',
      dlStep4: 'Fungura BeBoss, uhitemo ururimi (RW/EN/FR/SW), utangire Iminsi 4 y’Ubuntu cyangwa uhuze telefoni n’iduka risanzwe.',
      qrTitle: 'Sikana QR Code Ushire Porogaramu kuri Telefoni',
      qrModeApk: 'Umuyoboro wa APK',
      qrModePortal: 'Fungura Urubuga',
      qrModeWa: 'WhatsApp ya Admin',
      qrCustomLabel: 'Umuyoboro wa QR (Uhindura QR ako kanya)',
      qrDownloadPng: 'Bika QR (PNG)',
      qrCopyLink: 'Koporora Umuyoboro',
      qrPrintPoster: 'Sohora Affiche ya QR',
      priceTitle: 'Kubara Ibiciro, Telefoni Nyinshi & Kode za USSD',
      priceSub: 'Ibiciro bisobanutse mu Mafranga y’u Rwanda (FRw). Ishyura ukoresheje *182# MTN MoMo cyangwa *500# Airtel Money.',
      priceBranches: 'Amashami Akora',
      priceWorkers: 'Abakozi Bakoresha POS',
      priceDevices: 'Telefoni Zihujwe (Iya 1 ni Ubuntu)',
      priceMonths: 'Igihe Wishyurira',
      priceMonthly: 'Igiteranyo cy’Ukwezi',
      priceDiscount: 'Igabanuka',
      priceTotal: 'Ayishyurwa Yose',
      priceMtnLabel: 'MTN MoMo USSD',
      priceAirtelLabel: 'Airtel Money USSD',
      priceAskWa: 'Baza Admin kuri WhatsApp',
      docsTitle: 'Inyoborabikorwa Yuzuye ya BeBoss (Mu Ndini 4)',
      docsSub: 'Ibisobanuro birambuye bya buri gice cya BeBoss: gucuruza, ububiko, amadeni, guhuza telefoni nyinshi, USSD, n’umutekano.',
      docsSearchPlaceholder: 'Shakisha mu nyoborabikorwa (urugero: USSD, Join Code, Amadeni, PIN)...',
      docsAllChapters: 'Ibice 8 Byose',
      footerNote: 'BeBoss Enterprise POS • Porogaramu y’Ubucuruzi Ikora Offline • Room SQLCipher, WorkManager & Supabase Cloud'
    },
    fr: {
      navExperience: 'Démo Live',
      navDownload: 'Télécharger & QR',
      navPricing: 'Tarifs & USSD',
      navDocs: 'Documentation (4 Langues)',
      btnDownloadApk: 'Télécharger APK',
      btnAdminLogin: 'Connexion Admin',
      btnAdminConsole: 'Console Admin',
      btnPublicSite: 'Site Public',
      heroPill: 'POS HORS LIGNE MULTI-AGENCES • 4 LANGUES • CHIFFREMENT AES-256',
      heroTitle: 'Transformez Tout Smartphone Android en Caisse POS Multi-Boutiques.',
      heroSub: 'Vendez sans coupure même hors ligne, scannez les codes-barres, encaissez MTN MoMo + Espèces en paiement mixte, recouvrez les dettes clients (Amadeni), prévoyez vos réapprovisionnements sur 14 jours et synchronisez automatiquement tous vos terminaux.',
      heroCtaDownload: 'Télécharger BeBoss APK',
      heroCtaScan: 'Scanner le QR Code',
      heroCtaDemo: 'Tester la Démo POS',
      heroStat1: 'Essai Gratuit 4 Jours',
      heroStat1Sub: 'Accès complet illimité',
      heroStat2: '5 000 FRw/mois',
      heroStat2Sub: '1er terminal propriétaire inclus',
      heroStat3: '+2 000 FRw/mois',
      heroStat3Sub: 'Par appareil supplémentaire',
      heroStat4: '100% Hors Ligne',
      heroStat4Sub: 'Synchro cloud automatique',
      expTitle: 'Centre d’Expérience Interactive BeBoss',
      expSub: 'Testez l’encaissement POS, la synchronisation multi-appareils et le déblocage USSD *182# directement dans votre navigateur.',
      expTabPos: '1. Caisse POS & Reçu Scellé',
      expTabSync: '2. Synchro Multi-Appareils',
      expTabPay: '3. USSD & Double Déblocage',
      posAddItem: 'Cliquez sur un Produit pour l’Ajouter',
      posCartTitle: 'Panier de Caisse Actif',
      posPayMethod: 'Mode de Paiement',
      posCashGiven: 'Espèces Reçues (FRw)',
      posCompleteSale: 'Valider la Vente & Sceller',
      posClear: 'Vider',
      posReceiptTitle: 'Ticket Thermique 58mm Scellé',
      posSealValid: 'Sceau SHA-256 Vérifié Hors Ligne',
      dlTitle: 'Téléchargement APK Android & QR Code Dynamique',
      dlSub: 'Téléchargez le fichier APK manuellement ou scannez le QR code réel avec la caméra de votre téléphone Android.',
      dlDirectBtn: 'Télécharger le Fichier APK',
      dlChecksum: 'Empreinte SHA-256',
      dlCopyHash: 'Copier SHA-256',
      dlInstallStepsTitle: 'Guide d’Installation Android en 4 Étapes',
      dlStep1: 'Cliquez sur « Télécharger le Fichier APK » ou scannez le QR code sur votre smartphone.',
      dlStep2: 'Ouvrez le fichier BeBoss-POS téléchargé depuis Chrome ou Gestionnaire de fichiers.',
      dlStep3: 'Autorisez l’installation depuis cette source lorsque Android le demande.',
      dlStep4: 'Lancez BeBoss, choisissez FR/EN/RW/SW et démarrez l’essai de 4 jours ou rejoignez une boutique.',
      qrTitle: 'Scannez ce QR Code sur Android',
      qrModeApk: 'Lien APK Direct',
      qrModePortal: 'Ouvrir ce Portail',
      qrModeWa: 'WhatsApp Admin',
      qrCustomLabel: 'URL Cible du QR Code',
      qrDownloadPng: 'Enregistrer QR PNG',
      qrCopyLink: 'Copier le Lien',
      qrPrintPoster: 'Imprimer Affiche QR',
      priceTitle: 'Calculateur Tarifs, Multi-Appareils & Codes USSD',
      priceSub: 'Tarification transparente synchronisée avec BeBoss Cloud. Réglez par MTN MoMo *182# ou Airtel Money *500#.',
      priceBranches: 'Boutiques / Agences',
      priceWorkers: 'Opérateurs / Caissiers',
      priceDevices: 'Appareils Connectés (1er Inclus)',
      priceMonths: 'Durée d’Abonnement',
      priceMonthly: 'Sous-total Mensuel',
      priceDiscount: 'Remise',
      priceTotal: 'Total à Payer',
      priceMtnLabel: 'USSD MTN MoMo',
      priceAirtelLabel: 'USSD Airtel Money',
      priceAskWa: 'Contacter Admin sur WhatsApp',
      docsTitle: 'Documentation Complète BeBoss (4 Langues)',
      docsSub: 'Explications complètes sur chaque module : Caisse POS, Stock, Dettes, Multi-Appareils, USSD, Sécurité et Dépannage.',
      docsSearchPlaceholder: 'Rechercher dans la documentation (ex: USSD, Join Code, Stock, PIN)...',
      docsAllChapters: 'Tous les 8 Chapitres',
      footerNote: 'BeBoss Enterprise POS • Système de Caisse Hors Ligne • Room SQLCipher, WorkManager & Supabase Cloud'
    },
    sw: {
      navExperience: 'Jaribu POS',
      navDownload: 'Pakua APK & QR',
      navPricing: 'Bei & USSD',
      navDocs: 'Mwongozo (Lugha 4)',
      btnDownloadApk: 'Pakua APK',
      btnAdminLogin: 'Ingia Admin',
      btnAdminConsole: 'Dashibodi ya Admin',
      btnPublicSite: 'Tovuti ya Umma',
      heroPill: 'POS BILA MTANDAO • LUGHA 4 • USALAMA WA AES-256',
      heroTitle: 'Geuza Simu Yoyote ya Android Kuwa Kituo Mahiri cha POS ya Matawi Mengi.',
      heroSub: 'Uza bila kukwama hata bila intaneti, skani barcode, pokea malipo mseto ya MTN MoMo + Taslimu, fuatilia madeni ya wateja (Amadeni), tabiri mahitaji ya stoo kwa siku 14, na sawazisha vifaa vyote kiotomatiki.',
      heroCtaDownload: 'Pakua BeBoss APK Sasa',
      heroCtaScan: 'Skani QR Kwenye Simu',
      heroCtaDemo: 'Jaribu Demo ya POS',
      heroStat1: 'Majaribio Siku 4',
      heroStat1Sub: 'Huduma kamili bure',
      heroStat2: '5,000 FRw/mwezi',
      heroStat2Sub: 'Kifaa cha kwanza bure',
      heroStat3: '+2,000 FRw/mwezi',
      heroStat3Sub: 'Kwa kila kifaa cha ziada',
      heroStat4: '100% Bila Mtandao',
      heroStat4Sub: 'Husawazisha kiotomatiki',
      expTitle: 'Kituo cha Majaribio ya Moja kwa Moja ya BeBoss',
      expSub: 'Jaribu mauzo ya kaunta, muunganisho wa vifaa vingi, na malipo ya *182# USSD moja kwa moja kwenye kivinjari chako.',
      expTabPos: '1. Mauzo ya POS & Risiti',
      expTabSync: '2. Vifaa Vingi Kiotomatiki',
      expTabPay: '3. USSD & Kufungua Leseni',
      posAddItem: 'Gusa Bidhaa Kuongeza Kwenye Kikapu',
      posCartTitle: 'Kikapu cha Kaunta',
      posPayMethod: 'Njia ya Malipo',
      posCashGiven: 'Taslimu Iliyopokelewa (FRw)',
      posCompleteSale: 'Kamilisha Mauzo & Tengeneza Muhuri',
      posClear: 'Futa',
      posReceiptTitle: 'Risiti ya 58mm Yenye Muhuri',
      posSealValid: 'Muhuri wa SHA-256 Umethibitishwa',
      dlTitle: 'Pakua App ya BeBoss Android au Skani QR Code',
      dlSub: 'Pakua faili la APK moja kwa moja au skani QR code ukitumia kamera ya simu yako ya Android.',
      dlDirectBtn: 'Pakua Faili la APK',
      dlChecksum: 'Alama ya Usalama (SHA-256)',
      dlCopyHash: 'Nakili SHA-256',
      dlInstallStepsTitle: 'Hatua 4 za Kusakinisha Kwenye Android',
      dlStep1: 'Bofya "Pakua Faili la APK" au skani QR code kwenye simu yako ya Android.',
      dlStep2: 'Fungua faili la BeBoss-POS APK lililopakuliwa kwenye Downloads.',
      dlStep3: 'Android ikiomba ruhusa, washa "Allow from this source" kuendelea.',
      dlStep4: 'Fungua BeBoss, chagua SW/EN/RW/FR, anza Siku 4 za Bure au jiunge na duka lililopo.',
      qrTitle: 'Skani QR Code Kwenye Simu Yako',
      qrModeApk: 'Kiungo cha APK',
      qrModePortal: 'Fungua Tovuti',
      qrModeWa: 'WhatsApp ya Admin',
      qrCustomLabel: 'Kiungo cha QR Code',
      qrDownloadPng: 'Pakua QR (PNG)',
      qrCopyLink: 'Nakili Kiungo',
      qrPrintPoster: 'Chapa Bango la QR',
      priceTitle: 'Kikokotoo cha Bei, Vifaa Vingi & Namba za USSD',
      priceSub: 'Bei wazi za FRw zilizosawazishwa na BeBoss Cloud. Lipa kupitia *182# MTN MoMo au *500# Airtel Money.',
      priceBranches: 'Idadi ya Matawi',
      priceWorkers: 'Wafanyakazi wa POS',
      priceDevices: 'Vifaa Vilivyounganishwa (Cha 1 Bure)',
      priceMonths: 'Muda wa Malipo',
      priceMonthly: 'Jumla ya Mwezi',
      priceDiscount: 'Punguzo',
      priceTotal: 'Jumla ya Kulipa',
      priceMtnLabel: 'MTN MoMo USSD',
      priceAirtelLabel: 'Airtel Money USSD',
      priceAskWa: 'Uliza Admin Kwenye WhatsApp',
      docsTitle: 'Mwongozo Kamili wa BeBoss (Lugha 4)',
      docsSub: 'Maelezo ya kina ya kila kipengele: POS, Stoo, Madeni, Vifaa Vingi, USSD, Usalama na Utatuzi wa Matatizo.',
      docsSearchPlaceholder: 'Tafuta kwenye mwongozo (mfano: USSD, Join Code, Madeni, PIN)...',
      docsAllChapters: 'Sura Zote 8',
      footerNote: 'BeBoss Enterprise POS • Mfumo wa Biashara Bila Mtandao • Room SQLCipher, WorkManager & Supabase Cloud'
    }
  };

  const DOCS = {
    en: [
      {
        id: 'start',
        badge: '01 • ONBOARDING',
        title: 'Store Registration, 4-Day Free Trial & Cloud Auto-Save',
        summary: 'Launch a new shop in under 3 minutes or join an existing shop from another phone.',
        bullets: [
          'Pick your language (English, Ikinyarwanda, Français, Kiswahili) immediately on the welcome screen — all labels, PIN screens, and receipts translate locally offline.',
          'Create Shop Flow (3 Steps): Enter Shop Name, Business Sector, Currency (FRw/USD/KES/UGX), Active Phone & Address, then Owner @username, 4-digit fast PIN, and 6+ char password.',
          'Your shop is saved immediately to SQLCipher AES-256 on the phone AND pushed to Supabase Cloud (shop_accounts, shop_users, shop_devices, branch_sync_snapshots). If you register offline, WorkManager automatically uploads your account as soon as internet connects.',
          'Every new shop gets a strict 4-Day Full-Access Free Trial monitored by anti-rollback ClockGuard protection.'
        ]
      },
      {
        id: 'pos',
        badge: '02 • CHECKOUT & RECEIPTS',
        title: 'POS Checkout, Barcode Scanning, Split Pay & Cryptographic Seals',
        summary: 'Ring up customers rapidly with CameraX ML Kit scanning, multi-cart Hold/Resume, and verifiable receipts.',
        bullets: [
          'Tap items or tap the Barcode Scanner icon to scan EAN/UPC/QR labels using your device camera.',
          'Hold/Resume Carts: Park a busy customer basket with 1 tap while serving the next person in line.',
          '5 Payment Modes: Cash (auto-computes exact change), MTN MoMo, Airtel Money, Bank Card, Split Payment (Cash + MoMo combined), and Customer Credit (Amadeni).',
          'Every completed sale writes the Sale, Line Items, Stock Deduction, Debt Balance, and Sync Outbox atomically inside a Room @Transaction and stamps a SHA-256 BB-XXXXXXXXXX receipt seal verifiable offline.'
        ]
      },
      {
        id: 'inventory',
        badge: '03 • STOCK & FORECAST',
        title: 'Inventory Spreadsheet, Supplier Inflow & 14-Day Demand Forecast',
        summary: 'Eliminate stockouts and see true gross profit margins per SKU.',
        bullets: [
          'Dual-Tab Stock Sheet: Sort by Name, Quantity, Cost Price, Sell Price, or Margin %. View live Cost Value, Retail Value, and Potential Profit cards.',
          'Bulk CSV Import & Export: Import hundreds of products from Excel/CSV or export your full stock ledger anytime.',
          'Purchases & Adjustments: Log supplier deliveries, damaged/expired write-offs, or stock-count corrections with audit reasons.',
          '14-Day Moving Average Forecast (Reports & P/L): Automatically calculates daily sales velocity, days of stock cover remaining, and exact reorder quantities + restock budget.'
        ]
      },
      {
        id: 'debts',
        badge: '04 • CREDIT LEDGER',
        title: 'Customers, Credit Limits & Debt Recovery (Amadeni)',
        summary: 'Never lose money to forgotten notebook credit.',
        bullets: [
          'Import customer names and phone numbers straight from your Android Contacts in 1 tap or register them manually.',
          'Assign Credit Limits and customer categories (VIP, Wholesale, Regular). Cashiers are warned automatically if a credit sale exceeds a buyer limit.',
          'Record partial or full installment repayments and view complete historical customer statements.',
          'Send polite pre-formatted WhatsApp or SMS debt reminders showing exact balance remaining in 1 tap.'
        ]
      },
      {
        id: 'multidevice',
        badge: '05 • MULTI-DEVICE SYNC',
        title: 'Single-Shop Multi-Device Pairing (+2,000 FRw/mo) & Closed-App Sync',
        summary: 'Add staff phones/tablets with Join Codes and keep every device synchronized automatically.',
        bullets: [
          'Owner primary device is included in the 5,000 FRw base branch fee. Each extra active paired device with a staff operator adds +2,000 FRw/month.',
          'In Settings -> Shop Devices, create a slot for a Cashier or Manager and share the generated BB-XXXXXX Join Code.',
          'On the secondary Android phone, open BeBoss -> Join Existing Shop -> enter Owner @username, Owner Phone, Join Code, and the staff operator PIN/password. All products, branches, sales, debts, and users download automatically.',
          'WorkManager Background Sync runs periodically and on data changes whenever NetworkType.CONNECTED is present — syncing sales, stock, registrations, and Admin unlocks even when BeBoss is closed.'
        ]
      },
      {
        id: 'billing',
        badge: '06 • USSD & UNLOCK',
        title: 'Dynamic *182# / *500# USSD Payment, WhatsApp Proof & Dual Unlock',
        summary: 'Live Admin payment numbers and two fast ways to unlock your shop.',
        bullets: [
          'MTN MoMo (*182#) and Airtel Money (*500#) codes automatically pull the live Admin merchant code, phone numbers, and WhatsApp receiver from Supabase app_config.',
          'When submitting payment proof, include your Shop @username, Registered Phone, Sender MoMo Phone, Full Names Registered on the Sender Phone, Device Count, and Telco Reference.',
          'Option 1 (BB2 Unlock Code): Admin verifies your proof and generates a signed BB2-... code bound strictly to your username + phone. Paste it in BeBoss to unlock even 100% offline.',
          'Option 2 (Admin Direct Auto-Unlock): Admin clicks Direct Auto-Unlock in the portal; BeBoss unlocks automatically on the next foreground or background sync and sends a notification.'
        ]
      },
      {
        id: 'security',
        badge: '07 • SECURITY & RBAC',
        title: 'SQLCipher AES-256 Encryption, ClockGuard & Staff Permissions',
        summary: 'Bank-grade local security protecting your cash and records.',
        bullets: [
          'All Room SQLite tables are encrypted at rest with SQLCipher AES-256 using an Android Keystore wrapped passphrase.',
          'Role-Based Access Control separates Owner, Manager, and Cashier permissions (e.g. restricting cost prices, refunds, stock deletion, or shop settings).',
          'PBKDF2-HMAC-SHA256 hashes all PINs and passwords with exponential brute-force lockout protection and optional Hardware Fingerprint unlock.',
          'FLAG_SECURE blocks unauthorized screenshots in recent apps, while ClockGuard monotonic elapsed-time tracking prevents trial/license clock rollbacks.'
        ]
      },
      {
        id: 'troubleshoot',
        badge: '08 • INSTALL & FAQ',
        title: 'APK Installation Help, Thermal Printers & Offline Recovery',
        summary: 'Quick answers for installing, printing, and recovering terminals.',
        bullets: [
          'APK Install Blocked? Open Android Settings -> Apps -> Special App Access -> Install Unknown Apps -> allow Chrome or Files, then tap BeBoss-POS-v2.0.0.apk again.',
          '58mm / 80mm Thermal Printing: Configure paper width, shop TIN/VAT header, and receipt footer under Settings -> Shop & POS, then pair via Bluetooth or share WhatsApp image/text receipts.',
          'Switching Phones? As long as your primary phone synced to Supabase Cloud, register/join on the new phone using your shop credentials or export an encrypted .beboss backup file from Settings -> Security & DB.',
          'Need Master Admin Help? Use the live WhatsApp button in the Payment Calculator or Subscription Screen to reach the verified BeBoss Admin directly.'
        ]
      }
    ],
    rw: [
      {
        id: 'start',
        badge: '01 • GUTANGIRA',
        title: 'Kwandikisha Iduka, Iminsi 4 y’Ubuntu & Kubika muri Cloud',
        summary: 'Tangiza iduka rishya mu minota 3 cyangwa uhuze telefoni nshya n’iduka risanzwe.',
        bullets: [
          'Hitamo ururimi rwose (Ikinyarwanda, English, Français, Kiswahili) ugitangira — paji zose, PIN n’inyemezabuguzi bihita bihinduka offline.',
          'Kwandikisha Iduka (Intambwe 3): Shyiramo izina ry’iduka, icyiciro cy’ubucuruzi, ifaranga (FRw/USD/KES), telefoni yawe ya MoMo/WhatsApp, maze uhitemo @username, PIN y’imibare 4, n’ijambobanga.',
          'Amakuru yawe ahita abikwa muri telefoni afunzwe na AES-256 kandi akoherezwa muri Supabase Cloud ako kanya. Niyo wiyandikisha udafite interineti, WorkManager izayohereza interineti igarutse.',
          'Buri duka rishya rihabwa Iminsi 4 y’Ubuntu rwose rifunguye byose, rigenzurwa na ClockGuard ituma igihe kidashobora gusubizwa inyuma.'
        ]
      },
      {
        id: 'pos',
        badge: '02 • GUCURUZA & INYEMEZABUGUZI',
        title: 'Gucuruza muri POS, Gusoma Barcode, Split Pay & Sceau ya BB-',
        summary: 'Akira abakiriya vuba ukoresheje kamera isoma barcode, kubika igitebo, n’inyemezabuguzi yizewe.',
        bullets: [
          'Kanda ku gicuruzwa cyangwa ukoreshe kamera (CameraX + ML Kit) gusoma barcode ziri ku bicuruzwa.',
          'Hold/Resume Sale: Bika ibyo umukiriya umwe agiye gufata mugihe ugicuruza undi kuri kaunta.',
          'Uburyo 5 bwo kwishyura: Cash (igukubira ayo ugarura), MTN MoMo, Airtel Money, Ikarita ya Banki, Split (Cash + MoMo icyarimwe), na Credit/Ideni.',
          'Buri igurisha rishyirwaho ikirango cy’umutekano SHA-256 (BB-XXXXXXXXXX) gishobora kugenzurwa offline muri Sales Ledger -> Inspect.'
        ]
      },
      {
        id: 'inventory',
        badge: '03 • UBUBIKO & ITEGANYABIKORWA',
        title: 'Urutonde rw’Ububiko, Ibyinjira & Iteganya ry’Iminsi 14',
        summary: 'Menya igicuruzwa kiri hafi gushira n’inyungu nyayo kuri buri kintu.',
        bullets: [
          'Stock Sheet: Tondeka ibicuruzwa ku izina, umubare, igiciro cyo kurangura, icyo kugurisha, cyangwa % y’inyungu.',
          'CSV Import/Export: Injiza ibicuruzwa byinshi icyarimwe uvanye muri Excel cyangwa ubitange muri CSV.',
          'Andika ibyaguzwe ku baranguzi (Purchases), ibyangiritse cyangwa ibyarangiye bifite impamvu yanditse.',
          '14-Day Demand Forecast: Ibara umuvuduko w’igurisha rya buri munsi ikakwereka iminsi ububiko busigaje n’amafaranga ukwiye kurangura.'
        ]
      },
      {
        id: 'debts',
        badge: '04 • ABAKIRIYA & AMADENI',
        title: 'Gucunga Abakiriya, Imipaka y’Amadeni & Kwishyuza',
        summary: 'Ntuzongere kwibagirwa ideni na rimwe ryanditse mu ikayi.',
        bullets: [
          'Kuramo abakiriya muri Contacts za telefoni yawe mu kanya gato cyangwa ubandike.',
          'Shyiriraho buri mukiriya umupaka w’ideni (Credit Limit) n’icyiciro (VIP, Umucuruzi, Usanzwe).',
          'Andika ubwishyu bw’igice cyangwa bwose maze urebe amateka yuzuye y’umukiriya.',
          'Ohereza ubutumwa bwa WhatsApp cyangwa SMS bwibutsa umukiriya ideni asigaje mu kanda kamwe.'
        ]
      },
      {
        id: 'multidevice',
        badge: '05 • TELEFONI NYINSHI',
        title: 'Guhuza Telefoni Nyinshi z’Iduka (+2,000 FRw/ukwezi) & Sync Ihoraho',
        summary: 'Ongera telefoni z’abakozi ukoresheje Join Code maze zose zisangire amakuru.',
        bullets: [
          'Telefoni ya 1 ya nyir’iduka ibarirwa mu mafaranga fatizo (5,000 FRw). Buri telefoni nshya y’umukozi yongeraho +2,000 FRw/ukwezi.',
          'Jya muri Settings -> Shop Devices, wongereho umukozi (Cashier/Manager) maze uhabwe kode ya BB-XXXXXX.',
          'Kuri telefoni nshya, hitamo Join Existing Shop -> shyiramo @username ya nyir’iduka, telefoni ye, Join Code, na PIN nshya y’umukozi. Amakuru yose ahita amanuka!',
          'WorkManager ikora inyuma na porogaramu ifunze (NetworkType.CONNECTED), ikohereza ibyagurishijwe, ububiko n’amakuru yose igihe interineti ihari.'
        ]
      },
      {
        id: 'billing',
        badge: '06 • USSD & GUFUNGURA',
        title: 'Kwishyura na *182# / *500#, Icyemezo cya WhatsApp & Gufungura mu Buryo 2',
        summary: 'Numero za Admin zivugururwa ku murongo n’uburyo 2 bwo gufungura konti.',
        bullets: [
          'Kode za MTN MoMo (*182#) na Airtel (*500#) zikurura numero nshya za Admin muri Supabase app_config.',
          'Iyo wohereza icyemezo cyo kwishyura, shyiramo @username y’iduka, Telefoni yanditse ku iduka, Telefoni yishyuye, Amazina yanditse kuri iyo telefoni yishyuye, umubare wa telefoni zikoreshwa, na Reference ya MoMo.',
          'Uburyo bwa 1 (Kode ya BB2): Admin areba ubwishyu akagukorera kode ya BB2-... ifungura iduka ryawe gusa na offline.',
          'Uburyo bwa 2 (Auto-Unlock): Admin akanda Direct Auto-Unlock, BeBoss igahita yifungura yonyine interineti igeze kuri telefoni.'
        ]
      },
      {
        id: 'security',
        badge: '07 • UMUTEKANO & RBAC',
        title: 'Umutekano wa SQLCipher AES-256, ClockGuard & Uburenganzira bw’Abakozi',
        summary: 'Kurinda amafaranga n’amakuru y’ubucuruzi bwawe ku rwego rwo hejuru.',
        bullets: [
          'Ububiko bwose bwa SQLite burinzwe n’urufunguzo rwa AES-256 (Android Keystore).',
          'Abakozi batandukanywa (Owner, Manager, Cashier): ushobora guhisha ibiciro byo kurangura cyangwa gusiba ibyacurujwe.',
          'PIN n’amajambobanga birindwa na PBKDF2-HMAC-SHA256 kandi byemeza Igikumwe (Fingerprint) cy’igikoresho.',
          'FLAG_SECURE ibuza gufata amafoto ya ekran, naho ClockGuard ikabuza guhindura isaha ngo barenze iminsi y’ubuntu.'
        ]
      },
      {
        id: 'troubleshoot',
        badge: '08 • GUSHYIRAMO & IBIBAZO',
        title: 'Ubufasha bwo Gushyiramo APK, Imashini z’Inyemezabuguzi & Kubika',
        summary: 'Ibisubizo byihuse by’ibibazo bikunze kubaho.',
        bullets: [
          'APK yanze kwinjira? Fungura Settings za telefoni -> Apps -> Install Unknown Apps -> Emerera Chrome cyangwa Files.',
          'Imashini za 58mm / 80mm: Hitamo ubunini bw’urupapuro, TIN n’ijambo ryo hasi muri Settings -> Shop & POS.',
          'Uhinduye telefoni? Koresha Join Existing Shop cyangwa wohereze dosiye ya .beboss backup ibitse amakuru yose.',
          'Ukeneye ubufasha? Kanda buto ya WhatsApp iri hano kuri uru rubuga uvugane na Master Admin.'
        ]
      }
    ],
    fr: [
      {
        id: 'start',
        badge: '01 • DÉMARRAGE',
        title: 'Création de Boutique, Essai Gratuit 4 Jours & Sauvegarde Cloud',
        summary: 'Lancez votre boutique en 3 minutes ou connectez un nouveau téléphone à une boutique existante.',
        bullets: [
          'Choisissez votre langue (Français, English, Ikinyarwanda, Kiswahili) dès l’écran d’accueil — toute l’interface et les tickets basculent instantanément hors ligne.',
          'Assistant en 3 étapes : Nom de boutique, Secteur, Devise (FRw/USD/KES/UGX), Téléphone MoMo/WhatsApp, puis @username, code PIN à 4 chiffres et mot de passe.',
          'Enregistrement immédiat dans SQLCipher AES-256 local et synchronisation vers Supabase Cloud. En cas d’inscription hors ligne, WorkManager synchronise automatiquement dès le retour d’internet.',
          'Essai gratuit complet de 4 jours protégé par l’horloge anti-falsification ClockGuard.'
        ]
      },
      {
        id: 'pos',
        badge: '02 • CAISSE & TICKETS',
        title: 'Caisse POS, Scan Code-Barres, Paiement Mixte & Sceau BB-',
        summary: 'Encaissez rapidement avec scanner caméra CameraX/ML Kit, paniers en attente et reçus scellés.',
        bullets: [
          'Scannez les codes-barres produits avec la caméra ou recherchez instantanément par nom/SKU.',
          'Mise en attente (Hold/Resume) : gardez un panier de côté pour servir le client suivant sans perte.',
          '5 modes de règlement : Espèces (calcul automatique du rendu), MTN MoMo, Airtel Money, Carte, Paiement Mixte (Cash + MoMo), et Crédit client (Amadeni).',
          'Chaque vente génère un sceau cryptographique SHA-256 BB-XXXXXXXXXX vérifiable hors ligne dans le journal.'
        ]
      },
      {
        id: 'inventory',
        badge: '03 • STOCK & PRÉVISIONS',
        title: 'Feuille de Stock, Achats Fournisseurs & Prévision 14 Jours',
        summary: 'Évitez les ruptures de stock et suivez vos marges brutes réelles.',
        bullets: [
          'Stock Sheet triable par nom, quantité, prix d’achat, prix de vente ou marge % avec valorisation globale en temps réel.',
          'Import/Export CSV en masse pour importer votre catalogue Excel ou sauvegarder votre inventaire.',
          'Suivi des achats fournisseurs, pertes (périmés/cassés) et ajustements audités.',
          'Prévision sur 14 jours : calcule la vélocité quotidienne, les jours de couverture restants et le budget exact de réapprovisionnement.'
        ]
      },
      {
        id: 'debts',
        badge: '04 • CLIENTS & DETTES',
        title: 'Gestion des Clients, Plafonds de Crédit & Recouvrement',
        summary: 'Sécurisez chaque franc vendu à crédit grâce à l’historique et aux relances.',
        bullets: [
          'Importez vos clients en 1 clic depuis les Contacts Android ou créez leurs fiches manuellement.',
          'Définissez des plafonds de crédit (Credit Limit) et alertes automatiques en caisse.',
          'Enregistrez les acomptes partiels ou totaux avec relevé complet par client.',
          'Envoyez des rappels WhatsApp ou SMS pré-remplis avec le solde exact dû.'
        ]
      },
      {
        id: 'multidevice',
        badge: '05 • MULTI-APPAREILS',
        title: 'Appairage Multi-Terminaux (+2 000 FRw/mois) & Synchro Arrière-Plan',
        summary: 'Connectez les téléphones/tablettes de vos caissiers avec un Join Code unique.',
        bullets: [
          'Le terminal principal du propriétaire est inclus dans la licence de base (5 000 FRw/mois). Chaque appareil supplémentaire coûte +2 000 FRw/mois.',
          'Dans Paramètres -> Shop Devices, assignez un opérateur et générez son Join Code BB-XXXXXX.',
          'Sur le second appareil, choisissez Rejoindre une Boutique -> entrez @username du propriétaire, téléphone enregistré, Join Code et PIN de l’opérateur.',
          'WorkManager synchronise en arrière-plan (même app fermée) dès que la connexion Internet est active.'
        ]
      },
      {
        id: 'billing',
        badge: '06 • USSD & DÉBLOCAGE',
        title: 'Paiement USSD *182# / *500#, Preuve WhatsApp & Double Déblocage',
        summary: 'Numéros mis à jour en direct par l’Admin et activation immédiate.',
        bullets: [
          'Les codes USSD MTN (*182#) et Airtel (*500#) récupèrent dynamiquement les coordonnées Admin depuis Supabase app_config.',
          'Lors de l’envoi de preuve : précisez @username, téléphone du magasin, téléphone payeur, Noms complets sur le numéro payeur, nombre d’appareils et référence MoMo.',
          'Option 1 (Code BB2) : l’Admin émet un code BB2-... lié à votre compte, activable même 100% hors ligne.',
          'Option 2 (Déblocage Direct) : l’Admin clique Direct Auto-Unlock et votre boutique se débloque automatiquement à la synchronisation.'
        ]
      },
      {
        id: 'security',
        badge: '07 • SÉCURITÉ & RBAC',
        title: 'Chiffrement SQLCipher AES-256, ClockGuard & Contrôle d’Accès',
        summary: 'Protection matérielle et logicielle contre la fraude et le vol de données.',
        bullets: [
          'Base SQLite chiffrée AES-256 via clé protégée par Android Keystore.',
          'Rôles distincts Propriétaire, Manager et Caissier avec permissions granulaires.',
          'Hachage PBKDF2-HMAC-SHA256 des PIN/mots de passe, verrouillage anti-brute-force et biométrie.',
          'Protection FLAG_SECURE contre les captures d’écran et verrouillage d’inactivité configurable.'
        ]
      },
      {
        id: 'troubleshoot',
        badge: '08 • INSTALLATION & FAQ',
        title: 'Guide d’Installation APK, Imprimantes Thermiques & Sauvegarde',
        summary: 'Solutions rapides pour installer et exploiter BeBoss au quotidien.',
        bullets: [
          'Installation bloquée ? Activez « Autoriser cette source » dans les paramètres Android pour Chrome/Fichiers.',
          'Imprimantes 58mm / 80mm : personnalisez l’en-tête TVA/NIF et le pied de ticket dans Paramètres -> Boutique & POS.',
          'Changement de téléphone : utilisez Rejoindre une Boutique ou exportez une sauvegarde chiffrée .beboss.',
          'Besoin d’assistance ? Contactez directement l’Admin via le bouton WhatsApp sur ce portail.'
        ]
      }
    ],
    sw: [
      {
        id: 'start',
        badge: '01 • KUANZA',
        title: 'Usajili wa Duka, Majaribio ya Siku 4 & Kuhifadhi Kwenye Wingu',
        summary: 'Sajili duka jipya chini ya dakika 3 au unganisha simu mpya na duka lililopo.',
        bullets: [
          'Chagua lugha (Kiswahili, English, Ikinyarwanda, Français) mara moja unapoanza — skrini zote na risiti hubadilika bila intaneti.',
          'Hatua 3 za Kusajili Duka: Jina la Duka, Sekta, Sarafu (FRw/USD/KES/UGX), Namba ya MoMo/WhatsApp, kisha @username, PIN ya namba 4 na nenosiri.',
          'Data huhifadhiwa papo hapo kwa usimbaji wa AES-256 na kutumwa Supabase Cloud. Ukijisajili bila mtandao, WorkManager hutuma mara mtandao ukirejea.',
          'Kila duka jipya hupata Siku 4 za Bure za huduma kamili zinazolindwa na mfumo wa ClockGuard.'
        ]
      },
      {
        id: 'pos',
        badge: '02 • MAUZO & RISITI',
        title: 'Kaunta ya POS, Skani Barcode, Malipo Mseto & Muhuri wa BB-',
        summary: 'Hudumia wateja kwa kasi ukitumia kamera ya barcode na risiti zenye uhakiki.',
        bullets: [
          'Skani barcode kwa kamera ya simu au tafuta bidhaa haraka kwa jina au SKU.',
          'Hold/Resume Sale: Hifadhi kikapu cha mteja kwa muda huku ukihudumia mteja mwingine.',
          'Njia 5 za Malipo: Taslimu (huhesabu chenji), MTN MoMo, Airtel Money, Kadi, Malipo Mseto (Taslimu + MoMo), na Mkopo (Amadeni).',
          'Kila mauzo hupewa muhuri wa SHA-256 BB-XXXXXXXXXX unaothibitishwa bila mtandao.'
        ]
      },
      {
        id: 'inventory',
        badge: '03 • STOO & UTABIRI',
        title: 'Jedwali la Stoo, Manunuzi & Utabiri wa Siku 14',
        summary: 'Zuia upungufu wa bidhaa na fuatilia faida halisi ya kila bidhaa.',
        bullets: [
          'Stock Sheet: Panga bidhaa kwa jina, idadi, bei ya kununua, bei ya kuuza au asilimia ya faida.',
          'CSV Import/Export: Ingiza bidhaa nyingi kutoka Excel au toa nakala ya stoo yako.',
          'Rekodi manunuzi kutoka kwa wasambazaji, bidhaa zilizoharibika au marekebisho yenye sababu.',
          'Utabiri wa Siku 14 huhesabu kasi ya mauzo na kupendekeza idadi na bajeti ya kuagiza tena.'
        ]
      },
      {
        id: 'debts',
        badge: '04 • WATEJA & MADENI',
        title: 'Usimamizi wa Wateja, Vikomo vya Mkopo & Kukusanya Madeni',
        summary: 'Usipoteze pesa tena kwenye daftari za mikopo.',
        bullets: [
          'Leta majina na namba za wateja moja kwa moja kutoka kwenye Contacts za simu.',
          'Weka ukomo wa deni (Credit Limit) na makundi ya wateja (VIP, Jumla, Kawaida).',
          'Rekodi malipo ya awamu na tazama historia kamili ya taarifa za mteja.',
          'Tuma ukumbusho wa deni kupitia WhatsApp au SMS kwa kubofya mara moja.'
        ]
      },
      {
        id: 'multidevice',
        badge: '05 • VIFAA VINGI',
        title: 'Kuunganisha Simu Nyingi za Duka (+2,000 FRw/mwezi) & Sync',
        summary: 'Ongeza simu/tableti za wahudumu kwa Join Code na usawazishe data zote.',
        bullets: [
          'Simu kuu ya mmiliki imejumuishwa kwenye ada ya msingi (5,000 FRw/mwezi). Kila kifaa cha ziada kinaongeza +2,000 FRw/mwezi.',
          'Kwenye Mipangilio -> Shop Devices, tengeneza nafasi ya mhudumu upate Join Code ya BB-XXXXXX.',
          'Kwenye simu mpya, chagua Join Existing Shop -> weka @username ya mmiliki, namba ya duka, Join Code na PIN ya mhudumu.',
          'WorkManager husawazisha kiotomatiki nyuma (hata app imefungwa) pindi intaneti inapopatikana.'
        ]
      },
      {
        id: 'billing',
        badge: '06 • USSD & KUFUNGUA',
        title: 'Malipo ya USSD *182# / *500#, Uthibitisho WhatsApp & Kufungua',
        summary: 'Namba mpya za Admin na njia mbili za haraka za kufungua leseni.',
        bullets: [
          'Namba za MTN MoMo (*182#) na Airtel (*500#) hupatikana moja kwa moja kutoka Supabase app_config.',
          'Unapotuma uthibitisho, weka @username, namba iliyosajiliwa, namba iliyolipa, Majina Kamili yaliyosajiliwa kwenye namba iliyolipa, idadi ya vifaa na Reference.',
          'Njia ya 1 (Kode ya BB2): Admin hutoa kode ya BB2-... inayofungua akaunti yako pekee hata bila mtandao.',
          'Njia ya 2 (Auto-Unlock): Admin anabofya Direct Auto-Unlock na app inajifungua yenyewe ikisawazisha mtandaoni.'
        ]
      },
      {
        id: 'security',
        badge: '07 • USALAMA & RBAC',
        title: 'Usimbaji wa SQLCipher AES-256, ClockGuard & Ruhusa za Wafanyakazi',
        summary: 'Ulinzi wa kiwango cha benki kwa fedha na taarifa za duka lako.',
        bullets: [
          'Data zote za SQLite zimesimbwa kwa AES-256 kupitia Android Keystore.',
          'Majukumu tofauti kwa Mmiliki, Meneja na Keshia ili kudhibiti bei za manunuzi na ufutaji.',
          'PIN na nenosiri hulindwa kwa PBKDF2-HMAC-SHA256 pamoja na alama ya kidole (Fingerprint).',
          'FLAG_SECURE huzuia picha za skrini na ClockGuard huzuia udanganyifu wa kubadilisha saa ya simu.'
        ]
      },
      {
        id: 'troubleshoot',
        badge: '08 • USAKINISHAJI & MASWALI',
        title: 'Mwongozo wa APK, Printa za Risiti 58mm/80mm & Hifadhi',
        summary: 'Majibu ya haraka ya usakinishaji na uendeshaji wa kila siku.',
        bullets: [
          'APK imezuiwa? Fungua Android Settings -> Apps -> Install Unknown Apps -> ruhusu Chrome au Files.',
          'Printa za Bluetooth 58mm/80mm: Weka TIN, kichwa na ujumbe wa mwisho chini ya Settings -> Shop & POS.',
          'Kubadilisha simu? Tumia Join Existing Shop au hamisha faili lililosimbwa la .beboss.',
          'Unahitaji msaada? Tumia kitufe cha WhatsApp kuwasiliana na Master Admin moja kwa moja.'
        ]
      }
    ]
  };

  global.BeBossI18n = { LANGUAGES, UI, DOCS };
})(window);
