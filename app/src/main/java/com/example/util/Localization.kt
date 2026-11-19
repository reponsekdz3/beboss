package com.example.util

/**
 * Supported offline-first local application languages.
 * All translations are bundled 100% locally with zero internet dependency.
 */
enum class AppLanguage(val code: String, val displayName: String, val flag: String) {
    ENGLISH("en", "English", "EN"),
    KINYARWANDA("rw", "Ikinyarwanda", "RW"),
    FRENCH("fr", "Français", "FR"),
    SWAHILI("sw", "Kiswahili", "SW");

    fun next(): AppLanguage {
        val values = entries
        return values[(ordinal + 1) % values.size]
    }

    companion object {
        fun fromCode(code: String?): AppLanguage {
            return entries.firstOrNull { it.code.equals(code?.trim(), ignoreCase = true) } ?: ENGLISH
        }
    }
}

private fun entry(en: String, rw: String, fr: String, sw: String): Map<AppLanguage, String> = mapOf(
    AppLanguage.ENGLISH to en,
    AppLanguage.KINYARWANDA to rw,
    AppLanguage.FRENCH to fr,
    AppLanguage.SWAHILI to sw
)

object Localization {

    private val translations: Map<String, Map<AppLanguage, String>> = mapOf(
        // =====================================================================
        // APP / NAVIGATION / COMMON
        // =====================================================================
        "app_title" to entry("BeBoss", "BeBoss", "BeBoss", "BeBoss"),
        "dashboard" to entry("Dashboard", "Ahabanza", "Tableau de bord", "Dashibodi"),
        "sales_pos" to entry("POS Sale", "Gucuruza", "Caisse POS", "Mauzo POS"),
        "inventory" to entry("Stock Sheet", "Ububiko", "Stock & Achats", "Stooni"),
        "analytics" to entry("Reports & P/L", "Raporo & Inyungu", "Rapports & P/L", "Ripoti & Faida"),
        "customers" to entry("Customers & Debts", "Abakiriya & Amadeni", "Clients & Dettes", "Wateja & Madeni"),
        "sales_history" to entry("Sales Ledger", "Ibyagurishijwe", "Journal Ventes", "Historia ya Mauzo"),
        "settings" to entry("Settings", "Igenamiterere", "Paramètres", "Mipangilio"),
        "lock_terminal" to entry("Lock Terminal", "Funga Urubuga", "Verrouiller", "Funga Kituo"),
        "unlock" to entry("Unlock", "Fungura", "Déverrouiller", "Fungua"),
        "switch_user" to entry("Switch Operator", "Hindura Umukozi", "Changer d'opérateur", "Badili Mhudumu"),
        "theme" to entry("Theme", "Insanganyamatsiko", "Thème", "Mandhari"),
        "app_theme" to entry("Appearance & Theme", "Imigaragarire & Insanganyamatsiko", "Apparence & Thème", "Mwonekano na Mandhari"),
        "display_and_language" to entry("Language & Display", "Ururimi n'Imigaragarire", "Langue & Affichage", "Lugha na Mwonekano"),
        "language" to entry("Language", "Ururimi", "Langue", "Lugha"),
        "select_language_subtitle" to entry(
            "100% local offline translations across all screens",
            "Ubuhinduzi bwuzuye bukora offline kuri paji zose",
            "Traductions locales 100% hors ligne sur tous les écrans",
            "Tafsiri kamili za ndani bila mtandao kwenye kurasa zote"
        ),
        "dark_mode" to entry("Dark Mode", "Uburyo bw'Umwijima", "Mode Sombre", "Hali ya Giza"),
        "light_mode" to entry("Light Mode", "Uburyo bw'Umucyo", "Mode Clair", "Hali ya Mwanga"),
        "close" to entry("Close", "Funga", "Fermer", "Funga"),
        "back" to entry("Back", "Subira inyuma", "Retour", "Rudi"),
        "continue_btn" to entry("Continue", "Komeza", "Continuer", "Endelea"),
        "retry" to entry("Retry", "Ongera ugerageze", "Réessayer", "Jaribu tena"),
        "inspect" to entry("Inspect", "Suzuma", "Inspecter", "Kagua"),
        "search" to entry("Search", "Shakisha", "Rechercher", "Tafuta"),
        "apply" to entry("Apply", "Shyiraho", "Appliquer", "Tumia"),
        "save_changes" to entry("Save Changes", "Bika Impinduka", "Enregistrer", "Hifadhi Mabadiliko"),
        "save_shop_profile" to entry("Save Shop Profile", "Bika Amakuru y'Ububiko", "Enregistrer le profil boutique", "Hifadhi Wasifu wa Duka"),
        "cancel" to entry("Cancel", "Reka", "Annuler", "Ghairi"),
        "delete" to entry("Delete", "Siba", "Supprimer", "Futa"),
        "edit" to entry("Edit", "Hindura", "Modifier", "Hariri"),
        "logout" to entry("Log Out Account", "Sohoka muri Konti", "Déconnexion", "Ondoka Kwenye Akaunti"),
        "logout_confirm_msg" to entry(
            "Sign out from this terminal? Your encrypted local database remains safely stored.",
            "Ushaka gusohoka kuri iyi telefoni? Amakuru yawe yose arinzwe aguma abitswe neza.",
            "Se déconnecter de ce terminal ? Votre base chiffrée reste en sécurité.",
            "Je, unataka kuondoka kwenye akaunti? Hifadhidata yako iliyosimbwa inabaki salama."
        ),
        "all" to entry("All", "Byose", "Tout", "Zote"),
        "status_live" to entry("LIVE", "IRAKORA", "EN LIGNE", "MTANDAONI"),
        "status_offline" to entry("OFFLINE", "OFFLINE", "HORS LIGNE", "NJE YA MTANDAO"),

        // =====================================================================
        // LOGIN & LOCK SCREEN (100% LOCALIZED)
        // =====================================================================
        "auth_lock_title" to entry("Terminal Locked", "Urubuga Rufunze", "Terminal Verrouillé", "Kituo Kimefungwa"),
        "offline_branch_pos_badge" to entry(
            "OFFLINE-FIRST BRANCH POS",
            "POS Y'AMASHAMI IKORA OFFLINE",
            "POS MULTI-AGENCES HORS LIGNE",
            "POS YA MATAWI BILA MTANDAO"
        ),
        "operator_terminal_header" to entry(
            "SELECT OPERATOR TERMINAL",
            "HITAMO UMUKOZI WO KWINJIRA",
            "SÉLECTIONNER UN OPÉRATEUR",
            "CHAGUA MHUDUMU WA KITUO"
        ),
        "fast_pin_tab" to entry("Fast 4-Digit PIN", "PIN (Imibare 4)", "PIN Rapide (4 chiffres)", "PIN ya Haraka (4)"),
        "credentials_tab" to entry("Username & Password", "Izina & Ijambobanga", "Identifiant & Mot de passe", "Jina & Nenosiri"),
        "fingerprint_unlock" to entry("Touch Fingerprint Sensor", "Koresha Igikumwe", "Déverrouiller par empreinte", "Tumia Alama ya Kidole"),
        "biometric_prompt_title" to entry("Fingerprint Verification", "Kugenzura Igikumwe", "Vérification biométrique", "Uthibitishaji wa Kidole"),
        "biometric_prompt_subtitle" to entry(
            "Verify fingerprint to unlock BeBoss",
            "Koresha igikumwe gufungura BeBoss",
            "Vérifiez votre empreinte pour ouvrir BeBoss",
            "Thibitisha alama ya kidole kufungua BeBoss"
        ),
        "biometric_use_pin" to entry("Use PIN Instead", "Koresha PIN", "Utiliser le PIN", "Tumia PIN Badala Yake"),
        "biometric_unavailable" to entry(
            "Hardware biometric authentication is unavailable on this device. Use PIN or password.",
            "Igikoresho ntigifite uburyo bw'igikumwe bwizewe. Koresha PIN cyangwa ijambobanga.",
            "L'authentification biométrique matérielle n'est pas disponible. Utilisez votre PIN ou mot de passe.",
            "Uthibitishaji wa alama ya kidole haupatikani kwenye kifaa hiki. Tumia PIN au nenosiri."
        ),
        "biometric_required_err" to entry(
            "Hardware biometric verification and an active account are required.",
            "Igikumwe cyemejwe n'igikoresho kirakenewe kuri konti iriho.",
            "Vérification biométrique matérielle et compte actif requis.",
            "Uthibitishaji wa kidole na akaunti hai vinahitajika."
        ),
        "enter_pin" to entry("Enter 4-Digit Security PIN", "Injiza Umubare w'Ibanga (PIN)", "Entrez votre PIN à 4 chiffres", "Weka PIN ya Tarakimu 4"),
        "enter_credentials" to entry("Sign In with Credentials", "Injira Ukoresheje Umwirondoro", "Connexion par identifiants", "Ingia kwa Kitambulisho"),
        "username_or_phone" to entry("Username or Phone", "Izina cyangwa Terefone", "Nom d'utilisateur ou téléphone", "Jina la mtumiaji au Simu"),
        "password" to entry("Password", "Ijambobanga", "Mot de passe", "Nenosiri"),
        "toggle_password" to entry("Show / hide password", "Garagaza / hisha ijambobanga", "Afficher / masquer le mot de passe", "Onyesha / ficha nenosiri"),
        "create_store_account" to entry("Setup Shop & Owner Account", "Gushinga Ububiko & Konti", "Créer une boutique & compte", "Sajili Duka & Akaunti ya Mmiliki"),
        "owner_role" to entry("Shop Owner (Full Admin)", "Nyir'Ububiko (Admin)", "Propriétaire (Admin)", "Mmiliki wa Duka (Admin)"),
        "manager_role" to entry("Store Manager", "Umuyobozi w'Ububiko", "Gérant de boutique", "Meneja wa Duka"),
        "cashier_role" to entry("Cashier / Operator", "Umucuruzi", "Caissier / Opérateur", "Keshia / Mhudumu"),
        "security_warning" to entry(
            "SQLCipher AES-256 Encrypted • Offline Ready",
            "Birinzwe na AES-256 • Bikora nta interineti",
            "Chiffré SQLCipher AES-256 • 100% Hors Ligne",
            "Imesimbwa kwa AES-256 • Inafanya kazi bila mtandao"
        ),
        "incorrect_pin" to entry(
            "Incorrect PIN code. Please try again.",
            "Umubare wa PIN si wo. Ongera ugerageze.",
            "Code PIN incorrect. Veuillez réessayer.",
            "Nambari ya PIN si sahihi. Tafadhali jaribu tena."
        ),
        "invalid_credentials" to entry(
            "Invalid username or password.",
            "Izina cyangwa ijambobanga si byo.",
            "Identifiant ou mot de passe invalide.",
            "Jina la mtumiaji au nenosiri si sahihi."
        ),
        "account_suspended_err" to entry(
            "Account suspended by Master Administrator.",
            "Konti yawe yahagaritswe n'Umuyobozi Mukuru (Admin).",
            "Compte suspendu par l'administrateur principal.",
            "Akaunti imesimamishwa na Msimamizi Mkuu."
        ),
        "welcome_back_prefix" to entry("Welcome back", "Murakaza neza", "Bon retour", "Karibu tena"),
        "logged_in_as_prefix" to entry("Signed in as", "Winjiye nka", "Connecté en tant que", "Umeingia kama"),
        "unlocked_biometric_prefix" to entry("Unlocked with biometric", "Urubuga rufunguwe n'igikumwe", "Déverrouillé par empreinte", "Imefunguliwa kwa kidole"),
        "logged_out_success" to entry("Logged out successfully.", "Wasohotse neza.", "Déconnexion réussie.", "Umeondoka kwa mafanikio."),
        "selected_operator_pin_prompt" to entry(
            "Enter PIN to unlock terminal",
            "Injiza PIN yo gufungura",
            "Entrez le PIN pour déverrouiller",
            "Weka PIN ili kufungua kituo"
        ),

        // =====================================================================
        // 3-STEP SHOP REGISTRATION WIZARD (100% LOCALIZED)
        // =====================================================================
        "reg_subtitle" to entry(
            "AES-256 Offline SQLite • Multi-Branch Sync • 4-Day Free Trial",
            "Ububiko bwa AES-256 • Guhuza Amashami • Iminsi 4 y'Ubuntu",
            "SQLite AES-256 Hors Ligne • Sync Multi-Agences • Essai 4 Jours",
            "SQLite AES-256 Bila Mtandao • Usawazishaji Matawi • Majaribio Siku 4"
        ),
        "reg_step_1" to entry("1. Store", "1. Ububiko", "1. Boutique", "1. Duka"),
        "reg_step_2" to entry("2. Contact & HQ", "2. Aho Bukorera", "2. Contact & Siège", "2. Mawasiliano"),
        "reg_step_3" to entry("3. Credentials", "3. Umutekano", "3. Identifiants", "3. Usalama"),
        "reg_shop_name_label" to entry("Shop / Business Name *", "Izina ry'Ububiko / Ubucuruzi *", "Nom de la boutique *", "Jina la Duka / Biashara *"),
        "reg_shop_name_placeholder" to entry("e.g. Kigali Smart Groceries", "urugero: Kigali Smart Groceries", "ex. Alimentation Kigali Pro", "mf. Duka la Kigali Smart"),
        "reg_sector_label" to entry("Business Sector:", "Icyiciro cy'Ubucuruzi:", "Secteur d'activité :", "Sekta ya Biashara:"),
        "sector_retail" to entry("Retail & Grocery", "Ibicuruzwa & Ibiribwa", "Alimentation & Détail", "Rejareja na Vyakula"),
        "sector_fashion" to entry("Fashion & Boutique", "Imyenda & Butike", "Mode & Boutique", "Mitindo na Mavazi"),
        "sector_electronics" to entry("Electronics & Phones", "Ibikoresho & Telefone", "Électronique & Téléphones", "Vifaa vya Kielektroniki"),
        "sector_pharmacy" to entry("Pharmacy & Health", "Farumasi & Ubuzima", "Pharmacie & Santé", "Famasi na Afya"),
        "sector_hardware" to entry("Hardware & Tools", "Ibyuma & Ibikoresho", "Quincaillerie & Outils", "Vifaa vya Ujenzi"),
        "sector_restaurant" to entry("Restaurant & Bar", "Resitora & Akabari", "Restaurant & Bar", "Mgahawa na Baa"),
        "sector_wholesale" to entry("Wholesale Supply", "Ubucuruzi bwa En Gros", "Commerce de Gros", "Biashara ya Jumla"),
        "sector_salon" to entry("Salon & Spa", "Saloni & Ubwiza", "Salon & Spa", "Saluni na Urembo"),
        "reg_currency_label" to entry("Primary Currency:", "Ifaranga Ukoresha:", "Devise principale :", "Sarafu Kuu:"),
        "reg_phone_label" to entry(
            "Owner Registered Phone (MoMo & WhatsApp) *",
            "Terefone ya Nyir'ububiko (MoMo & WhatsApp) *",
            "Téléphone du propriétaire (MoMo & WhatsApp) *",
            "Simu ya Mmiliki (MoMo na WhatsApp) *"
        ),
        "reg_phone_placeholder" to entry("+250 788 123 456", "+250 788 123 456", "+250 788 123 456", "+250 788 123 456"),
        "reg_address_placeholder" to entry("e.g. Kigali, Nyarugenge Market #12", "urugero: Kigali, Nyarugenge #12", "ex. Kigali, Marché Nyarugenge #12", "mf. Kigali, Soko la Nyarugenge #12"),
        "reg_trial_card_title" to entry(
            "4-Day Full-Access Free Trial • Then 5,000 FRw / mo",
            "Iminsi 4 y'Ubuntu Yuzuye • Hanyuma 5,000 FRw / ukwezi",
            "Essai Gratuit 4 Jours Complets • Puis 5 000 FRw / mois",
            "Majaribio ya Bure Siku 4 • Kisha 5,000 FRw / mwezi"
        ),
        "reg_trial_card_desc" to entry(
            "After 4 free days, the app locks until unlocked via MTN MoMo / Airtel Money or an Admin BB2 Unlock Code bound to your username & phone.",
            "Nyuma y'iminsi 4 y'ubuntu, porogaramu izafungwa kugeza wishyuye na MoMo/Airtel cyangwa ukoresheje Kode y'Admin (BB2) ihujwe n'izina na terefone yawe.",
            "Après 4 jours gratuits, l'application se verrouille jusqu'au paiement MoMo/Airtel ou à la saisie d'un code Admin BB2 lié à votre compte.",
            "Baada ya siku 4 za bure, programu itajifunga hadi ulipe kupitia MoMo/Airtel au uweke msimbo wa Admin BB2 uliounganishwa na akaunti yako."
        ),
        "reg_owner_name_label" to entry("Owner Full Name *", "Amazina Yuzuye ya Nyir'ububiko *", "Nom complet du propriétaire *", "Jina Kamili la Mmiliki *"),
        "reg_owner_name_placeholder" to entry("e.g. Jean Paul Mugisha", "urugero: Jean Paul Mugisha", "ex. Jean Paul Mugisha", "mf. Jean Paul Mugisha"),
        "reg_username_label" to entry(
            "Owner Username (Bound to Unlock Codes) *",
            "Izina ry'Ukoresha / Username (Rya Kode BB2) *",
            "Nom d'utilisateur (Lié aux codes BB2) *",
            "Jina la Mtumiaji (Kwa Misimbo ya BB2) *"
        ),
        "reg_username_placeholder" to entry("e.g. jeanpaul", "urugero: jeanpaul", "ex. jeanpaul", "mf. jeanpaul"),
        "reg_account_handle" to entry("Account handle", "Konti yawe", "Identifiant du compte", "Kitambulisho cha akaunti"),
        "reg_pin_label" to entry("4-Digit Fast Security PIN *", "PIN y'Imibare 4 yo Gufungura *", "Code PIN rapide à 4 chiffres *", "PIN ya Haraka ya Tarakimu 4 *"),
        "reg_pin_placeholder" to entry("e.g. 2580", "urugero: 2580", "ex. 2580", "mf. 2580"),
        "reg_gen_pin" to entry("Generate Strong PIN", "Kora PIN Ikomeye", "Générer un PIN fort", "Tengeneza PIN Imara"),
        "strength_too_short" to entry("Too short", "Migufi cyane", "Trop court", "Fupi sana"),
        "strength_basic" to entry("Basic", "Byoroshye", "Basique", "Wastani"),
        "strength_good" to entry("Good", "Byiza", "Bon", "Nzuri"),
        "strength_strong" to entry("Strong", "Bikomeye", "Fort", "Imara sana"),
        "reg_create_launch" to entry("Create & Launch", "Kora Ububiko & Tangira", "Créer & Lancer", "Unda & Anza"),
        "reg_err_shop_name" to entry("Please enter your shop name.", "Injiza izina ry'ububiko bwawe.", "Veuillez entrer le nom de votre boutique.", "Tafadhali weka jina la duka lako."),
        "reg_err_phone" to entry(
            "Please enter a valid owner phone number (for MoMo & BB2 unlock binding).",
            "Injiza nimero ya terefone y'ukuri (y'ubwishyu na Kode BB2).",
            "Veuillez entrer un numéro de téléphone valide (pour MoMo et code BB2).",
            "Tafadhali weka nambari sahihi ya simu (kwa MoMo na msimbo wa BB2)."
        ),
        "reg_err_owner_name" to entry("Please enter owner full name.", "Injiza amazina ya nyir'ububiko.", "Veuillez entrer le nom complet du propriétaire.", "Tafadhali weka jina kamili la mmiliki."),
        "reg_err_username" to entry("Please enter a username.", "Injiza username yawe.", "Veuillez entrer un nom d'utilisateur.", "Tafadhali weka jina la mtumiaji."),
        "reg_err_pin" to entry("PIN must be exactly 4 digits.", "PIN igomba kuba imibare 4 neza.", "Le PIN doit contenir exactement 4 chiffres.", "PIN lazima iwe na tarakimu 4 kamili."),
        "reg_err_password" to entry("Password must be at least 6 characters.", "Ijambobanga rigomba kuba nibura inyuguti 6.", "Le mot de passe doit contenir au moins 6 caractères.", "Nenosiri lazima liwe na herufi angalau 6."),
        "reg_success_trial" to entry(
            "Store created! Your 4-Day Full-Access Free Trial is now active.",
            "Ububiko bwatangiye! Iminsi 4 y'ubuntu yo kugerageza irakora.",
            "Boutique créée ! Votre essai gratuit de 4 jours est maintenant actif.",
            "Duka limeundwa! Majaribio yako ya bure ya siku 4 yameanza."
        ),
        "default_receipt_footer" to entry(
            "Thank you for your business! Please visit again!",
            "Murakoze cyane kugura! Mwongere kugaruka!",
            "Merci pour votre visite ! À très bientôt !",
            "Asante kwa biashara yako! Karibu tena!"
        ),

        // =====================================================================
        // DASHBOARD
        // =====================================================================
        "today_overview" to entry("TODAY'S PERFORMANCE", "IBYAGURISHIJWE UYU MUNSI", "PERFORMANCE DU JOUR", "UTENDAJI WA LEO"),
        "today_revenue" to entry("Today's Sales", "Amafaranga Yinjiye", "Ventes du jour", "Mauzo ya Leo"),
        "today_profit" to entry("Est. Profit", "Inyungu Y'uyu Munsi", "Bénéfice est.", "Faida ya Leo"),
        "today_orders" to entry("Orders", "Inyemezabuguzi", "Commandes", "Oda"),
        "total_stock_value" to entry("Total Stock Value", "Agaciro k'Ububiko", "Valeur totale du stock", "Thamani ya Stoo"),
        "potential_profit" to entry("Potential Profit", "Inyungu Itegerejwe", "Profit potentiel", "Faida Inayotarajiwa"),
        "outstanding_debts" to entry("Uncollected Debts", "Amadeni Asigaye Hanze", "Créances clients", "Madeni Yasiyolipwa"),
        "low_stock_warning" to entry("Low Stock Items", "Ibicuruzwa Bishize", "Articles en rupture", "Bidhaa Zinazoisha"),
        "quick_actions" to entry("Quick Actions", "Ibikorwa Byihuse", "Actions Rapides", "Vitendo vya Haraka"),
        "new_sale_action" to entry("New Sale (POS)", "Kugurisha (POS)", "Nouvelle vente", "Uuzaji Mpya"),
        "add_product_action" to entry("Add Stock", "Kongera Ibicuruzwa", "Ajouter Stock", "Ongeza Bidhaa"),
        "record_payment_action" to entry("Collect Debt", "Kwakira Umwenda", "Encaisser Dette", "Kusanya Deni"),
        "financial_reports_action" to entry("View Reports", "Kureba Raporo", "Voir Rapports", "Tazama Ripoti"),
        "recent_sales_title" to entry("Recent Transactions", "Ibiheruka Kugurishwa", "Ventes récentes", "Miamala ya Hivi Karibuni"),
        "view_all" to entry("View All", "Reba Byose", "Tout voir", "Tazama Zote"),
        "no_sales_yet" to entry(
            "No sales recorded yet. Tap 'POS Sale' to make your first sale!",
            "Nta bicuruzwa biragurishwa. Kanda 'Gucuruza' utangire!",
            "Aucune vente enregistrée. Appuyez sur « Caisse POS » pour commencer !",
            "Hakuna mauzo bado. Gusa 'Mauzo POS' kuanza kuuza!"
        ),

        // =====================================================================
        // POS & CHECKOUT
        // =====================================================================
        "pos_title" to entry("Point of Sale", "Aho Gucururiza (POS)", "Point de Vente", "Kituo cha Mauzo (POS)"),
        "search_product_hint" to entry("Search product name or barcode...", "Shakisha izina cyangwa barcode...", "Chercher produit ou code-barres...", "Tafuta jina la bidhaa au barcode..."),
        "scan_barcode" to entry("Scan / SKU", "Soma Barcode", "Scanner / SKU", "Skani Barcode"),
        "all_categories" to entry("All", "Byose", "Tout", "Zote"),
        "cart" to entry("Cart", "Igitebo", "Panier", "Kikapu"),
        "empty_cart" to entry("Cart is empty. Tap items to add.", "Igitebo kirimo ubusa. Kanda ku gicuruzwa wongeremo.", "Panier vide. Touchez un article pour l'ajouter.", "Kikapu kipo tupu. Gusa bidhaa kuongeza."),
        "clear_cart" to entry("Clear Cart", "Siba Igitebo", "Vider le panier", "Futa Kikapu"),
        "hold_cart" to entry("Hold Sale", "Bika Igitebo", "Mettre en attente", "Shikilia Mauzo"),
        "held_carts" to entry("Held Carts", "Ibitebo Bibitse", "Paniers en attente", "Vikapu Vilivyoshikiliwa"),
        "resume_cart" to entry("Resume", "Komeza", "Reprendre", "Endelea"),
        "customer" to entry("Customer", "Umukiriya", "Client", "Mteja"),
        "walk_in_customer" to entry("Walk-in Customer", "Umuguzi Usanzwe", "Client de passage", "Mteja wa Kawaida"),
        "payment_method" to entry("Payment Method", "Uburyo bwo Kwishyura", "Mode de paiement", "Njia ya Malipo"),
        "cash" to entry("Cash", "Amafaranga (Cash)", "Espèces", "Taslimu"),
        "momo" to entry("MTN MoMo", "MTN MoMo", "MTN MoMo", "MTN MoMo"),
        "airtel" to entry("Airtel Money", "Airtel Money", "Airtel Money", "Airtel Money"),
        "card" to entry("Bank Card", "Ikarita ya Banki", "Carte Bancaire", "Kadi ya Benki"),
        "split_payment" to entry("Split (Cash + MoMo)", "Kuvanga (Cash + MoMo)", "Mixte (Espèces + MoMo)", "Mchanganyiko (Taslimu + MoMo)"),
        "credit_debt" to entry("Credit / Debt", "Umwenda / Ikirarane", "Crédit / Dette", "Mkopo / Deni"),
        "subtotal" to entry("Subtotal", "Yose Hamwe", "Sous-total", "Jumla Ndogo"),
        "discount" to entry("Discount", "Kugabanyirizwa", "Remise", "Punguzo"),
        "net_total" to entry("Net Total", "Ayishyurwa", "Total Net", "Jumla Kuu"),
        "amount_received" to entry("Amount Received", "Amafaranga Yatanzwe", "Montant reçu", "Kiasi Kilichopokelewa"),
        "change_return" to entry("Change to Return", "Ibiceri Byo Gusubiza", "Monnaie à rendre", "Chenji ya Kurudisha"),
        "remaining_debt" to entry("Remaining Debt", "Umwenda Asigaranye", "Solde restant", "Deni Linalobaki"),
        "complete_sale" to entry("Complete Sale & Receipt", "Emeza & Sohora Inyemezabuguzi", "Valider & Reçu", "Kamilisha Mauzo & Risiti"),
        "insufficient_stock" to entry("Insufficient stock available", "Ibicuruzwa ntibihagije mu bubiko", "Stock insuffisant", "Bidhaa hazitoshi stooni"),
        "select_customer_for_credit" to entry(
            "Please select a registered customer to sell on credit!",
            "Hitamo umukiriya wanditse kugira ngo umuhe ku mwenda!",
            "Sélectionnez un client enregistré pour vendre à crédit !",
            "Tafadhali chagua mteja aliyesajiliwa kuuza kwa mkopo!"
        ),

        // =====================================================================
        // EXCEL TABLE VIEWS & INVENTORY
        // =====================================================================
        "excel_mode" to entry("Excel Spreadsheet View", "Imbonerahamwe ya Excel", "Vue Tableau Excel", "Mwonekano wa Jedwali"),
        "col_num" to entry("#", "#", "#", "#"),
        "col_name" to entry("Product Item", "Igicuruzwa", "Produit", "Bidhaa"),
        "col_category" to entry("Category", "Icyiciro", "Catégorie", "Kategoria"),
        "col_stock_qty" to entry("In Stock", "Ibihari", "En Stock", "Zilizopo"),
        "col_buying_price" to entry("Cost (Buy)", "Waguriyeho", "Coût Achat", "Bei ya Kununua"),
        "col_selling_price" to entry("Sell Price", "Ugurisha", "Prix Vente", "Bei ya Kuuza"),
        "col_profit_margin" to entry("Margin %", "Inyungu %", "Marge %", "Faida %"),
        "col_total_val" to entry("Total Value", "Agaciro", "Valeur Totale", "Thamani Jumla"),
        "col_actions" to entry("Actions", "Ibikorwa", "Actions", "Vitendo"),
        "col_customer_name" to entry("Customer Name", "Amazina", "Nom du client", "Jina la Mteja"),
        "col_phone" to entry("Phone Number", "Terefone", "Téléphone", "Simu"),
        "col_debt_balance" to entry("Debt Balance", "Umwenda", "Solde Dette", "Salio la Deni"),
        "col_credit_limit" to entry("Limit", "Umupaka", "Limite", "Kikomo"),
        "col_invoice_ref" to entry("Receipt #", "Inyemezabuguzi", "Reçu #", "Risiti #"),
        "col_date" to entry("Date & Time", "Itariki & Isaha", "Date & Heure", "Tarehe & Saa"),
        "col_amount" to entry("Total Paid", "Ayishyuwe", "Total Payé", "Jumla Iliyolipwa"),
        "col_method" to entry("Method", "Uburyo", "Méthode", "Njia"),

        "inventory_title" to entry("Inventory & Purchases", "Ububiko n'Ibyaguzwe", "Stock & Achats", "Stoo na Manunuzi"),
        "add_product" to entry("Add New Product", "Ongeramo Igicuruzwa", "Nouveau Produit", "Ongeza Bidhaa Mpya"),
        "product_name" to entry("Product Name", "Izina ry'Igicuruzwa", "Nom du produit", "Jina la Bidhaa"),
        "barcode" to entry("Barcode / SKU", "Kode / Barcode", "Code-barres / SKU", "Barcode / SKU"),
        "category" to entry("Category", "Icyiciro", "Catégorie", "Kategoria"),
        "cost_price" to entry("Cost Price (Buying)", "Igiciro Waguriyeho", "Prix d'achat", "Bei ya Kununua"),
        "selling_price" to entry("Selling Price", "Igiciro Ugurisha", "Prix de vente", "Bei ya Kuuza"),
        "wholesale_price" to entry("Wholesale Price", "Igiciro cya En Gros", "Prix de gros", "Bei ya Jumla"),
        "quantity_in_stock" to entry("Quantity in Stock", "Ingano Iri mu Bubiko", "Quantité en stock", "Idadi Stooni"),
        "min_stock_alert" to entry("Min Alert Threshold", "Ingano y'Iburira", "Seuil d'alerte", "Kiwango cha Tahadhari"),
        "unit" to entry("Unit (pcs, kg, etc.)", "Ibipimo (pcs, kg)", "Unité (pcs, kg)", "Kipimo (pcs, kg)"),
        "adjust_stock" to entry("Adjust Stock", "Hindura Ingano", "Ajuster le stock", "Rekebisha Stoo"),
        "restock" to entry("Restock (New Delivery)", "Kongeramo Ibyageze", "Réapprovisionner", "Ongeza Mzigo Mpya"),
        "damage_loss" to entry("Damage / Expired / Loss", "Ibyangiritse / Ibyarengeje Igihe", "Perte / Avarie", "Uharibifu / Muda Kuisha"),
        "inventory_count" to entry("Stock Count Correction", "Gukosora Ibarura", "Correction d'inventaire", "Marekebisho ya Hesabu"),
        "stock_valuation" to entry("Stock Valuation", "Agaciro k'Ububiko", "Valorisation du stock", "Thamani ya Stoo"),
        "cost_valuation" to entry("Total Cost Value", "Ayishyuwe Yose", "Valeur d'achat", "Gharama Jumla"),
        "retail_valuation" to entry("Retail Value", "Agaciro k'Ugurishije", "Valeur au détail", "Thamani ya Mauzo"),
        "profit_margin" to entry("Est. Gross Profit", "Inyungu Yitezwe", "Marge brute est.", "Faida Ghafi"),
        "low_stock" to entry("Low Stock", "Bishize", "Stock Faible", "Stoo Ndogo"),
        "in_stock" to entry("In Stock", "Bihari", "En Stock", "Ipo Stooni"),
        "export_excel_csv" to entry("Export Excel / CSV Sheet", "Sohora Excel / CSV", "Exporter Excel / CSV", "Hamisha Excel / CSV"),
        "import_excel_csv" to entry("Import from CSV File", "Injiza muri CSV", "Importer depuis CSV", "Ingiza kutoka CSV"),

        // =====================================================================
        // CUSTOMERS & DEBT
        // =====================================================================
        "customers_title" to entry("Customers & Debt Ledger", "Abakiriya & Amadeni", "Clients & Créances", "Wateja & Kitabu cha Madeni"),
        "add_customer" to entry("Add Customer", "Andika Umukiriya", "Ajouter Client", "Ongeza Mteja"),
        "import_contacts" to entry("Import from Contacts", "Vana muri Terefone", "Importer Contacts", "Ingiza kutoka Anwani"),
        "customer_name" to entry("Customer Full Name", "Amazina y'Umukiriya", "Nom complet du client", "Jina Kamili la Mteja"),
        "phone_number" to entry("Phone Number", "Nimero ya Terefone", "Numéro de téléphone", "Nambari ya Simu"),
        "address" to entry("Address / Location", "Aho Aherereye", "Adresse / Localisation", "Anwani / Mahali"),
        "credit_limit" to entry("Credit Limit", "Umupaka w'Umwenda", "Limite de crédit", "Kikomo cha Mkopo"),
        "total_debt" to entry("Current Debt", "Umwenda Afite", "Dette actuelle", "Deni la Sasa"),
        "record_payment" to entry("Record Debt Payment", "Andika Kwishyura", "Enregistrer paiement", "Rekodi Malipo ya Deni"),
        "send_reminder" to entry("Send Debt Reminder (WhatsApp)", "Kwishyuza kuri WhatsApp", "Rappel de dette (WhatsApp)", "Tuma Kikumbusho (WhatsApp)"),
        "debt_statement" to entry("Customer Statement", "Raporo y'Umukiriya", "Relevé client", "Taarifa ya Mteja"),
        "no_debt" to entry("No Debt (Cleared)", "Nta mwenda afite", "Aucune dette", "Hana Deni"),
        "previous_debt" to entry("Previous Debt", "Umwenda wari usanzwe", "Dette précédente", "Deni la Awali"),
        "amount_paid" to entry("Amount Paid", "Ayishyuwe", "Montant payé", "Kiasi Kilicholipwa"),
        "debt_cleared" to entry("DEBT CLEARED", "YARANGIYE NEZA", "DETTE SOLDÉE", "DENI LIMELIPWA"),
        "installment_history" to entry("Payment History", "Uko Yishyuye", "Historique des paiements", "Historia ya Malipo"),
        "auto_debt_saved" to entry("Debt recorded to ledger", "Umwenda wabitswe mu gitabo", "Dette enregistrée", "Deni limehifadhiwa"),

        // =====================================================================
        // ANALYTICS, FORECASTING & SALES HISTORY
        // =====================================================================
        "reports_title" to entry("Business Analytics & P/L", "Raporo z'Ubucuruzi & Inyungu", "Analyses & Pertes/Profits", "Takwimu za Biashara & Faida"),
        "period_today" to entry("Today", "Uyu munsi", "Aujourd'hui", "Leo"),
        "period_yesterday" to entry("Yesterday", "Ejo hashize", "Hier", "Jana"),
        "period_this_week" to entry("This Week", "Iki Cyumweru", "Cette semaine", "Wiki Hii"),
        "period_this_month" to entry("This Month", "Uku Kwezi", "Ce mois", "Mwezi Huu"),
        "period_this_year" to entry("This Year", "Uyu Mwaka", "Cette année", "Mwaka Huu"),
        "period_all_time" to entry("All Time", "Igihe Cyose", "Tout le temps", "Muda Wote"),
        "gross_revenue" to entry("Gross Revenue", "Amafaranga Yinjiye", "Chiffre d'affaires", "Mapato Ghafi"),
        "cost_of_goods" to entry("Cost of Goods (COGS)", "Ikiguzi cy'Ibyagurishijwe", "Coût des ventes", "Gharama za Bidhaa"),
        "gross_profit" to entry("Net Gross Profit", "Inyungu Nyayo", "Bénéfice Net", "Faida Halisi"),
        "profit_margin_pct" to entry("Profit Margin %", "Ijanisha ry'Inyungu", "Marge bénéficiaire %", "Asilimia ya Faida %"),
        "top_selling_products" to entry("Top Selling Products", "Ibicuruzwa Byagurishijwe Cyane", "Meilleures ventes", "Bidhaa Zinazouzwa Zaidi"),
        "category_sales_share" to entry("Sales by Category", "Ibyaguzwe ku Byiciro", "Ventes par catégorie", "Mauzo kwa Kategoria"),
        "export_pdf_report" to entry("Export PDF Financial Report", "Sohora Raporo ya PDF", "Exporter Rapport PDF", "Hamisha Ripoti ya PDF"),
        "demand_forecast_title" to entry("14-Day Demand Forecast & Reorder", "Iteganyamibare ry'Iminsi 14", "Prévision 14 jours & Réappro", "Utabiri wa Siku 14 & Kuagiza"),
        "verify_receipt_seal" to entry("Verify Offline Receipt Seal", "Genzura Kashe y'Inyemezabuguzi", "Vérifier le sceau du reçu", "Thibitisha Muhuri wa Risiti"),
        "refund_void_sale" to entry("Refund / Void Sale", "Gusesa / Gusubiza Amafaranga", "Rembourser / Annuler", "Rejesha / Batilisha Mauzo"),

        // =====================================================================
        // SETTINGS HUB & CATEGORIES (100% LOCALIZED)
        // =====================================================================
        "settings_title" to entry("Shop & System Settings", "Igenamiterere ry'Ububiko", "Paramètres Boutique & Système", "Mipangilio ya Duka na Mfumo"),
        "settings_search_hint" to entry("Search settings, license, sync, printer...", "Shakisha igenamiterere, kode, sync...", "Rechercher paramètres, licence, sync...", "Tafuta mipangilio, leseni, usawazishaji..."),
        "cat_all" to entry("All", "Byose", "Tout", "Zote"),
        "cat_language" to entry("Language & Theme", "Ururimi & Isura", "Langue & Thème", "Lugha & Mandhari"),
        "cat_license" to entry("License & Unlock", "Ifatabuguzi & Kode", "Licence & Code", "Leseni & Kufungua"),
        "cat_sync" to entry("Sync & Network", "Sync & Interineti", "Sync & Réseau", "Usawazishaji"),
        "cat_shop" to entry("Shop & POS", "Ububiko & POS", "Boutique & POS", "Duka & POS"),
        "cat_team" to entry("Staff & Branches", "Abakozi & Amashami", "Équipe & Agences", "Wafanyakazi & Matawi"),
        "cat_security" to entry("Security & DB", "Umutekano & DB", "Sécurité & BD", "Usalama & DB"),
        "shop_profile" to entry("Shop Profile & Info", "Amakuru y'Ububiko", "Profil de la boutique", "Wasifu wa Duka"),
        "shop_name" to entry("Shop Name", "Izina ry'Ububiko", "Nom de la boutique", "Jina la Duka"),
        "owner_name" to entry("Owner Name", "Amazina ya Nyir'ububiko", "Nom du propriétaire", "Jina la Mmiliki"),
        "receipt_footer" to entry("Receipt Footer Note", "Ubutumwa bwo ku Nyemezabuguzi", "Pied de page du reçu", "Ujumbe wa Chini ya Risiti"),
        "receipt_footer_text" to entry("Receipt Footer Note", "Ubutumwa bwo ku Nyemezabuguzi", "Pied de page du reçu", "Ujumbe wa Chini ya Risiti"),
        "staff_management" to entry("Staff & Cashiers", "Abakozi & Abacuruzi", "Équipe & Caissiers", "Wafanyakazi & Keshia"),
        "data_backup" to entry("Backup & Export", "Bika Kopi (Backup)", "Sauvegarde & Export", "Hifadhi Nakala"),
        "restore_data" to entry("Restore Shop Data", "Garura Kopi", "Restaurer les données", "Rejesha Data"),
        "currency" to entry("Currency", "Ifaranga", "Devise", "Sarafu"),
        "security_autolock_title" to entry("Terminal Security & Auto-Lock", "Umutekano & Gufunga Byikora", "Sécurité & Verrouillage Auto", "Usalama & Kujifunga Kiotomatiki"),
        "idle_lock_timeout" to entry("Auto-Lock Idle Timeout", "Igihe cyo Gufunga Byikora", "Délai de verrouillage auto", "Muda wa Kujifunga Kiotomatiki"),
        "offline_db_active" to entry("SQLCipher AES-256 Active • Cloud Ready", "Ububiko bwa AES-256 Burakora", "Base AES-256 Active • Prêt Cloud", "Hifadhidata AES-256 Hai • Tayari Cloud"),
        "cloud_sync_card_title" to entry("Cloud & Multi-Device Sync", "Guhuza n'Ububiko bwo mu Kirere (Cloud)", "Synchronisation Cloud & Multi-Appareils", "Usawazishaji wa Wingu na Vifaa"),
        "cloud_online_desc" to entry("Online • Auto-syncing on changes", "Interineti irahari • Guhuza byikora", "En ligne • Synchronisation automatique", "Mtandaoni • Inasawazisha kiotomatiki"),
        "cloud_offline_desc" to entry("Offline • Safe encrypted local storage active", "Nta interineti • Bibitswe neza muri telefoni", "Hors ligne • Stockage chiffré actif", "Nje ya mtandao • Hifadhi salama inafanya kazi"),
        "push_to_cloud_now" to entry("Push All Data to Cloud Now", "Ohereza Amakuru kuri Cloud Nonaha", "Synchroniser vers le Cloud maintenant", "Tuma Data Zote Kwenye Wingu Sasa"),
        "pushing_to_cloud" to entry("Syncing with Cloud...", "Kugenzura no Guhuza...", "Synchronisation en cours...", "Inasawazisha na Wingu..."),
        "open_sync_hub" to entry("Open Branch & LAN Sync Hub", "Fungura Urubuga rwo Gusangiza Amashami", "Ouvrir le Hub de Sync Agences", "Fungua Kituo cha Usawazishaji Matawi"),
        "admin_portal_title" to entry("Master Admin & Cloud Licensing", "Ubuyobozi Mukuru & Kode BB2", "Console Admin & Licences Cloud", "Usimamizi Mkuu & Leseni za Wingu"),
        "open_admin_console" to entry("Admin Console", "Ubuyobozi (Admin)", "Console Admin", "Dashibodi ya Admin"),
        "web_admin_portal" to entry("Web Admin Portal", "Urubuga rwa Admin", "Portail Web Admin", "Tovuti ya Admin"),

        // =====================================================================
        // SUBSCRIPTION, 4-DAY FREE TRIAL & AUTO-UNLOCK BB2 CODE
        // =====================================================================
        "subscription_title" to entry("Monthly Shop License", "Ifatabuguzi ry'Ukwezi", "Licence Mensuelle Boutique", "Leseni ya Mwezi ya Duka"),
        "monthly_fee_desc" to entry(
            "5,000 FRw / month • Full Offline POS & Multi-Branch",
            "5,000 FRw / ukwezi • POS Ikora Offline n'Amashami",
            "5 000 FRw / mois • POS Hors Ligne & Multi-Agences",
            "5,000 FRw / mwezi • POS Bila Mtandao na Matawi"
        ),
        "active_subscription" to entry("License Active", "Ifatabuguzi Rirakora", "Licence Active", "Leseni Inafanya Kazi"),
        "trial_active" to entry("4-Day Free Trial", "Iminsi 4 y'Ubuntu", "Essai Gratuit 4 Jours", "Majaribio ya Bure Siku 4"),
        "trial_ended_title" to entry("4-Day Free Trial Ended", "Iminsi 4 y'Ubuntu Yarangiye", "Essai Gratuit de 4 Jours Terminé", "Majaribio ya Siku 4 Yameisha"),
        "suspended_title" to entry("Account Suspended by Admin", "Konti Yahagaritswe na Admin", "Compte Suspendu par l'Admin", "Akaunti Imesimamishwa na Admin"),
        "subscription_expired" to entry("License Expired", "Ifatabuguzi Ryararangiye", "Licence Expirée", "Leseni Imeisha Muda"),
        "trial_ended_subtitle" to entry(
            "Your 4-day free trial has ended. Pay via MTN MoMo / Airtel Money or paste your Admin BB2 Unlock Code below to auto-unlock immediately.",
            "Iminsi 4 y'ubuntu yarangiye. Ishyura ukoresheje MTN MoMo / Airtel Money cyangwa winjize Kode ya BB2 yoherejwe na Admin kugira ngo porogaramu ifunguke ako kanya.",
            "Votre essai gratuit de 4 jours est terminé. Payez via MTN MoMo / Airtel Money ou collez votre code Admin BB2 ci-dessous pour déverrouiller automatiquement.",
            "Majaribio yako ya bure ya siku 4 yameisha. Lipa kupitia MTN MoMo / Airtel Money au weka msimbo wako wa Admin BB2 hapa chini kufungua mara moja."
        ),
        "expired_subtitle" to entry(
            "Your monthly license has expired. Complete Mobile Money payment or paste your Admin BB2 Unlock Code to auto-unlock and continue.",
            "Ifatabuguzi ry'ukwezi ryararangiye. Ishyura na Mobile Money cyangwa ushyiremo Kode ya BB2 kugira ngo ukomeze akazi kawe.",
            "Votre licence mensuelle a expiré. Effectuez un paiement Mobile Money ou collez votre code Admin BB2 pour continuer immédiatement.",
            "Leseni yako ya mwezi imeisha. Kamilisha malipo ya Mobile Money au weka msimbo wa Admin BB2 kuendelea mara moja."
        ),
        "suspended_subtitle" to entry(
            "Contact BeBoss Master Admin on WhatsApp or enter a newly issued BB2 unlock code to restore access.",
            "Vugana na BeBoss Master Admin kuri WhatsApp cyangwa winjize kode nshya ya BB2 kugira ngo ufungurirwe.",
            "Contactez l'administrateur BeBoss sur WhatsApp ou entrez un nouveau code BB2 pour rétablir l'accès.",
            "Wasiliana na Msimamizi Mkuu wa BeBoss kwenye WhatsApp au weka msimbo mpya wa BB2 kurejesha huduma."
        ),
        "days_left" to entry("days left", "iminsi isigaye", "jours restants", "siku zilizobaki"),
        "renew_subscription" to entry("Pay / Renew License", "Ishyura / Vugurura Ifatabuguzi", "Payer / Renouveler", "Lipa / Huisha Leseni"),
        "pay_with_momo" to entry("Pay with MTN MoMo", "Ishyura na MTN MoMo", "Payer avec MTN MoMo", "Lipa na MTN MoMo"),
        "pay_with_airtel" to entry("Pay with Airtel Money", "Ishyura na Airtel Money", "Payer avec Airtel Money", "Lipa na Airtel Money"),
        "pay_ussd_whatsapp_cta" to entry(
            "Pay USSD & Send WhatsApp Verification",
            "Ishyura USSD & Ohereza Gihamya kuri WhatsApp",
            "Payer par USSD & Vérifier sur WhatsApp",
            "Lipa kwa USSD & Tuma Uthibitisho WhatsApp"
        ),
        "insert_unlock_code_title" to entry(
            "Enter Admin Unlock Code (Auto-Unlock)",
            "Injiza Kode y'Admin (Ifungura Ako Kanya)",
            "Entrer le Code Admin (Déverrouillage Auto)",
            "Weka Msimbo wa Admin (Hufungua Kiotomatiki)"
        ),
        "enter_voucher_code" to entry("Unlock Code (BB2-...)", "Kode yo Gufungura (BB2-...)", "Code de déverrouillage (BB2-...)", "Msimbo wa Kufungua (BB2-...)"),
        "auto_unlock_detected" to entry(
            "Valid BB2 unlock code detected — verifying & unlocking automatically...",
            "Kode ya BB2 ibonetse — iri kugenzurwa no gufungura ako kanya...",
            "Code BB2 valide détecté — vérification et déverrouillage automatiques...",
            "Msimbo halali wa BB2 umetambuliwa — inathibitisha na kufungua kiotomatiki..."
        ),
        "activate_code" to entry("Verify & Unlock Now", "Emeza & Fungura Nonaha", "Vérifier & Déverrouiller", "Thibitisha & Fungua Sasa"),
        "sync_cloud_approvals" to entry("Sync Cloud Approvals", "Shakisha Kode kuri Cloud", "Synchroniser Cloud", "Sawazisha Wingu"),
        "contact_support" to entry("WhatsApp Admin Support", "Vugana na Admin kuri WhatsApp", "Support Admin WhatsApp", "Msaada wa Admin WhatsApp"),
        "share_app_apk" to entry("Share App APK", "Ohereza Porogaramu (APK)", "Partager l'APK", "Shiriki Programu (APK)"),
        "device_code" to entry("Shop Device Code", "Kode y'Igikoresho", "Code Appareil", "Msimbo wa Kifaa"),

        // =====================================================================
        // QUICK ACTIONS & RUNTIME BANNERS
        // =====================================================================
        "quick_custom_sale" to entry("Quick Custom Sale", "Gucuruza Vuba", "Vente Rapide Libre", "Mauzo ya Haraka"),
        "quick_add_product" to entry("Quick Add Product", "Ongeramo Igicuruzwa", "Ajout Rapide Produit", "Ongeza Bidhaa Haraka"),
        "quick_new_customer" to entry("New Customer / Debt", "Umukiriya Mushya / Ideni", "Nouveau Client / Dette", "Mteja Mpya / Deni"),
        "quick_universal_search" to entry("Universal Search", "Shakisha Byose", "Recherche Globale", "Utafutaji wa Jumla"),
        "send_daily_whatsapp" to entry("Daily Business Summary", "Raporo y'Umunsi", "Résumé Quotidien", "Muhtasari wa Siku"),
        "view_last_receipt" to entry("View Last Receipt", "Inyemezabuguzi Iheruka", "Dernier Reçu", "Risiti ya Mwisho"),
        "items_in_cart" to entry("items in cart", "mu gitebo", "articles au panier", "bidhaa kikapuni"),
        "low_stock_alert_tab" to entry("Low Stock", "Biri gushira", "Stock Bas", "Stoo Ndogo"),
        "unpaid_debtors_tab" to entry("Debts Pending", "Amadeni", "Dettes en cours", "Madeni"),
        "enter_amount_rwf" to entry("Amount", "Amafaranga", "Montant", "Kiasi"),
        "item_description_optional" to entry("Item description (optional)", "Ibisobanuro (si ngombwa)", "Description (optionnel)", "Maelezo (hiari)"),
        "add_and_checkout" to entry("Checkout Now", "Ishyura Ako Kanya", "Encaisser maintenant", "Lipa Sasa Hivi"),
        "add_to_cart_action" to entry("Add to Cart", "Shyira mu Gitebo", "Ajouter au panier", "Weka Kikapuni"),

        "banner_db_warning" to entry(
            "Database integrity warning detected. Inspect in Settings.",
            "Igenzura ry'ububiko ryabonye ikibazo. Reba mu Igenamiterere.",
            "Alerte d'intégrité de la base de données. Vérifiez les paramètres.",
            "Onyo la uadilifu wa hifadhidata limegunduliwa. Kagua Mipangilio."
        ),
        "banner_clock_rollback" to entry(
            "Device clock rollback detected; anti-tamper protection active.",
            "Isaha ya telefoni yasubiye inyuma. Kosora isaha kugira ngo sync ikore.",
            "Recul d'horloge détecté ; protection anti-fraude active.",
            "Saa ya kifaa imerudishwa nyuma; ulinzi wa muda umewashwa."
        ),
        "banner_dead_letter" to entry(
            "Sync items paused after retries. Tap Retry to push.",
            "Hari amakuru yananiwe koherezwa kuri cloud. Kanda Ongera.",
            "Éléments de synchronisation en pause. Appuyez sur Réessayer.",
            "Vipengee vya usawazishaji vimesimama. Gusa Jaribu tena."
        ),
        "banner_offline_queue" to entry(
            "Offline mode — changes queued safely for auto-sync.",
            "Nta internet — amakuru abitswe neza, azoherezwa nimubona umuyoboro.",
            "Mode hors ligne — modifications enregistrées pour synchronisation auto.",
            "Nje ya mtandao — mabadiliko yamehifadhiwa salama kwa usawazishaji."
        ),

        // =====================================================================
        // BRANCHES & MULTI-STORE
        // =====================================================================
        "branches_title" to entry("Branches & Stores", "Amashami n'Ububiko", "Agences & Magasins", "Matawi na Maduka"),
        "manage_branches" to entry("Manage Branches", "Cunga Amashami", "Gérer les agences", "Simamia Matawi"),
        "add_branch" to entry("Add New Branch", "Ongeraho Ishami", "Nouvelle agence", "Ongeza Tawi Jipya"),
        "branch_name" to entry("Branch Name", "Izina ry'Ishami", "Nom de l'agence", "Jina la Tawi"),
        "branch_code" to entry("Branch Code", "Kode y'Ishami", "Code agence", "Msimbo wa Tawi"),
        "select_branch" to entry("Filter by Branch", "Hitamo Ishami", "Filtrer par agence", "Chuja kwa Tawi"),
        "all_branches" to entry("All Branches (Consolidated)", "Amashami Yose Hamwe", "Toutes les agences", "Matawi Yote Pamoja"),
        "main_branch_badge" to entry("Main HQ Branch", "Ishami Rikuru", "Siège Principal", "Tawi Kuu"),
        "assigned_branch" to entry("Assigned Branch", "Ishami Akoreramo", "Agence assignée", "Tawi Alilopangiwa"),
        "branch_earnings" to entry("Branch Earnings", "Inyungu z'Ishami", "Revenus de l'agence", "Mapato ya Tawi"),

        // =====================================================================
        // CLOUD & ADMIN CONSOLE
        // =====================================================================
        "cloud_sync_title" to entry("Cloud Sync & Multi-Branch", "Guhuza na Cloud & Amashami", "Synchronisation Cloud", "Usawazishaji wa Wingu"),
        "cloud_live_sync" to entry("Live Internet Auto-Sync", "Guhuza amakuru ako kanya", "Sync automatique en direct", "Usawazishaji wa moja kwa moja"),
        "cloud_online" to entry("Cloud Online (Live)", "Biri kuri Internet", "Cloud En Ligne", "Wingu Mtandaoni"),
        "cloud_offline" to entry("Offline Safe (Queued)", "Bikora Offline", "Hors Ligne (En file)", "Nje ya Mtandao (Salama)"),
        "push_to_cloud" to entry("Push All Data to Cloud", "Ohereza Byose kuri Cloud", "Tout envoyer au Cloud", "Tuma Data Zote Wingu"),
        "sync_completed" to entry("Cloud Sync Complete", "Byahujwe neza byose", "Synchronisation terminée", "Usawazishaji Umekamilika"),
        "firebase_cloud_connected" to entry("Cloud Storage Active", "Ububiko bwa Cloud Burakora", "Stockage Cloud Actif", "Hifadhi ya Wingu Inafanya Kazi"),

        "admin_console_title" to entry("Master Admin Command Center", "Ikigo cy'Ubuyobozi Mukuru (Admin)", "Centre de Commande Master Admin", "Kituo cha Usimamizi Mkuu"),
        "admin_tab_queue" to entry("Payments", "Ubwishyu", "Paiements", "Malipo"),
        "admin_tab_users" to entry("Users", "Abakoresha", "Utilisateurs", "Watumiaji"),
        "admin_tab_shops" to entry("Shops", "Amaduka", "Boutiques", "Maduka"),
        "admin_tab_issue" to entry("Issue Code", "Tanga Kode", "Générer Code", "Toa Msimbo"),
        "admin_tab_codes" to entry("Codes", "Amakode", "Codes BB2", "Misimbo"),
        "admin_tab_branches" to entry("Branches", "Amashami", "Agences", "Matawi"),
        "admin_tab_audit" to entry("Audit & Sec", "Ubugenzuzi", "Audit & Séc", "Ukaguzi"),
        "admin_pick_registered_shop" to entry(
            "Quick-Fill from Registered Shop (Auto-binds @username & phone):",
            "Hitamo Ububiko Bwanditse (Buhita buzuza @username & terefone):",
            "Remplissage rapide depuis une boutique enregistrée :",
            "Chagua Duka Lililosajiliwa (Hujaza @username na simu):"
        ),
        "admin_generate_bb2_cta" to entry(
            "Generate Real Account-Bound BB2 Unlock Code",
            "Kora Kode Nyayo ya BB2 Ihujwe n'Konti",
            "Générer un Code BB2 Lié au Compte",
            "Tengeneza Msimbo Halisi wa BB2 wa Akaunti"
        ),
        "shop_devices_title" to entry(
            "Shop Devices",
            "Telefoni z'Ububiko",
            "Appareils Boutique",
            "Vifaa vya Duka"
        ),
        "add_shop_device" to entry(
            "Add Device (+2,000 FRw/mo)",
            "Ongeraho Telefoni (+2,000 FRw/ukwezi)",
            "Ajouter Appareil (+2 000 FRw/mois)",
            "Ongeza Kifaa (+2,000 FRw/mwezi)"
        ),
        "join_existing_shop" to entry(
            "Join Existing Shop",
            "Injira mu Duka Ririho",
            "Rejoindre Boutique",
            "Jiunge na Duka"
        ),
        "create_new_shop" to entry(
            "Create New Shop",
            "Fungura Duka Rishya",
            "Créer Nouvelle Boutique",
            "Unda Duka Jipya"
        ),
        "device_join_code" to entry(
            "Device Join Code",
            "Kode yo Kwinjira",
            "Code d'Appairage",
            "Msimbo wa Kujiunga"
        ),
        "device_fee_addon_note" to entry(
            "1st owner device is included free. Each extra paired device with a user adds 2,000 FRw/month and auto-syncs all shop data.",
            "Telefoni ya mbere y'umuyobozi iri mu ifatabuguzi. Buri telefoni nshya n'umukozi yongeraho 2,000 FRw/ukwezi kandi ihuza amakuru yose.",
            "Le 1er appareil propriétaire est inclus. Chaque appareil supplémentaire avec utilisateur ajoute 2 000 FRw/mois avec synchronisation auto.",
            "Kifaa cha kwanza cha mmiliki kimejumuishwa. Kila kifaa cha ziada na mtumiaji huongeza 2,000 FRw/mwezi na kusawazisha data zote."
        ),
        "payer_full_name_label" to entry(
            "Full Names on Sender Phone (MoMo/Airtel) *",
            "Amazina Yuzuye ku Murongo Wohereje Amafaranga *",
            "Nom Complet sur le Numéro Payeur *",
            "Majina Kamili Kwenye Namba Iliyotuma *"
        ),
        "direct_auto_unlock_badge" to entry(
            "Auto-Unlocked Directly by Admin (No Code Needed)",
            "Yafunguwe na Admin Ako Kanya (Nta Kode Ikenewe)",
            "Déverrouillé Directement par Admin (Sans Code)",
            "Imefunguliwa Moja kwa Moja na Msimamizi (Bila Msimbo)"
        ),
        "admin_whatsapp_label" to entry(
            "Admin WhatsApp Number",
            "Nimero ya WhatsApp ya Admin",
            "Numéro WhatsApp Admin",
            "Namba ya WhatsApp ya Msimamizi"
        ),
        "mtn_ussd_config_label" to entry(
            "MTN MoMo Merchant Code & Phone (*182#)",
            "Kode ya MTN MoMo & Nimero (*182#)",
            "Code Marchand MTN MoMo & Tél (*182#)",
            "Msimbo wa MTN MoMo na Simu (*182#)"
        ),
        "airtel_ussd_config_label" to entry(
            "Airtel Money Phone (*500#)",
            "Nimero ya Airtel Money (*500#)",
            "Numéro Airtel Money (*500#)",
            "Namba ya Airtel Money (*500#)"
        ),
        "admin_tab_devices" to entry(
            "Devices",
            "Telefoni",
            "Appareils",
            "Vifaa"
        ),
        "admin_tab_payconfig" to entry(
            "Pay Config",
            "Igenamiterere USSD",
            "Config Paiement",
            "Mipangilio Malipo"
        )
    )

    fun allKeys(): Set<String> = translations.keys

    fun hasTranslation(key: String, language: AppLanguage): Boolean {
        return !translations[key]?.get(language).isNullOrBlank()
    }

    fun get(key: String, language: AppLanguage): String {
        return translations[key]?.get(language) ?: translations[key]?.get(AppLanguage.ENGLISH) ?: key
    }
}
