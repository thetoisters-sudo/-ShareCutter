import {
    Injectable,
    computed,
    inject,
} from '@angular/core';

import {
    UiPreferencesService,
} from '../../ui/services/ui-preferences.service';

interface DashboardTranslations {
    portfolioOverview: string;
    title: string;
    description: string;
    managePortfolios: string;
    loadingTitle: string;
    loadingDescription: string;
    unavailableTitle: string;
    retry: string;
    emptyTitle: string;
    emptyDescription: string;
    createPortfolio: string;
    summaryAriaLabel: string;
    portfolios: string;
    activeInvestmentAccounts: string;
    currentValue: string;
    cashPlusHoldings: string;
    availableCash: string;
    ofCurrentValue: string;
    holdingsValue: string;
    invested: string;
    initialValue: string;
    totalStartingCapital: string;
    totalReturn: string;
    calculatedAccounts: string;
    yourPortfolios: string;
    viewAll: string;
    portfolio: string;
    cash: string;
    holdings: string;
    return: string;
    calculated: string;
    createdByAmount: string;
    createdByHoldings: string;
    partialCachedData: string;
    unavailableDataNotice: string;
    bestPerformer: string;
    realizedProfit: string;
    unrealizedProfit: string;
    loadError: string;
}

interface PortfoliosTranslations {
    eyebrow: string;
    title: string;
    description: string;
    createPortfolio: string;
    dismissMessage: string;

    loadingTitle: string;
    loadingDescription: string;
    unavailableTitle: string;
    retry: string;

    emptyTitle: string;
    emptyDescription: string;
    createFirstPortfolio: string;

    currentPageSummary: string;
    allPortfolios: string;
    acrossAllPages: string;
    displayedValue: string;
    calculatedValueOnPage: string;
    displayedProfit: string;
    combinedCalculatedProfit: string;

    analyticsUnavailableSingle: string;
    analyticsUnavailableMultiple: string;
    cachedValuesTemporarily: string;

    createdByAmount: string;
    createdByHoldings: string;
    calculated: string;
    cached: string;

    initialValue: string;
    currentValue: string;
    totalProfit: string;
    realizedProfit: string;
    unrealizedProfit: string;
    activeAssets: string;
    transactions: string;
    dataSource: string;
    liveAnalytics: string;
    cachedPortfolio: string;

    showAllocation: string;
    hideAllocation: string;
    rename: string;
    delete: string;

    currentAllocation: string;
    holdingsAndWeights: string;
    loadingAllocation: string;
    allocationUnavailable: string;
    allocationSummary: string;

    portfolioValue: string;
    availableCash: string;
    holdingsMarketValue: string;
    totalCost: string;
    targetAllocation: string;
    allocatedAssets: string;

    noAllocatedAssets: string;
    noAllocatedAssetsDescription: string;

    asset: string;
    quantity: string;
    averageCost: string;
    currentPrice: string;
    marketValue: string;
    weight: string;
    realized: string;
    unrealized: string;

    portfolioPages: string;
    previous: string;
    next: string;
    page: string;
    of: string;

    portfolioAction: string;
    createDialogTitle: string;
    renameDialogTitle: string;
    deleteDialogTitle: string;
    closeDialog: string;

    portfolioName: string;
    creationMethod: string;
    allocateByAmount: string;
    enterExistingHoldings: string;
    initialValueField: string;
    automaticCurrentValueNote: string;

    cancel: string;
    creating: string;
    saving: string;
    deleting: string;
    saveName: string;

    renamePrompt: string;
    deletePrompt: string;
    deleteDescription: string;

    portfolioNameRequired: string;
    initialValueInvalid: string;
    createdSuccess: string;
    renamedSuccess: string;
    deletedSuccess: string;

    loadError: string;
    serverUnavailable: string;
    sessionExpired: string;
    forbiddenView: string;

    allocationLoadError: string;
    allocationForbidden: string;
    allocationNotFound: string;

    forbiddenAction: string;
    duplicateName: string;
    portfolioConflict: string;
    invalidPortfolio: string;
    createError: string;
    renameError: string;
    deleteError: string;
}

interface AssetsTranslations {
    eyebrow: string;
    title: string;
    description: string;
    addAsset: string;
    dismissMessage: string;

    loadingPortfoliosTitle: string;
    loadingPortfoliosDescription: string;
    unavailableTitle: string;
    retry: string;

    noPortfoliosTitle: string;
    noPortfoliosDescription: string;

    portfolio: string;
    searchAssets: string;
    searchPlaceholder: string;

    loadingAssetsTitle: string;
    loadingAssetsDescription: string;

    noAssetsTitle: string;
    noAssetsDescription: string;
    addFirstAsset: string;

    summaryAriaLabel: string;
    totalAssets: string;
    inSelectedPortfolio: string;
    assetTypes: string;
    distinctClassifications: string;
    currencies: string;
    distinctTradingCurrencies: string;

    noMatchingAssets: string;
    noMatchingAssetsDescription: string;

    exchange: string;
    notSpecified: string;
    isin: string;
    created: string;
    updated: string;

    edit: string;
    delete: string;

    assetAction: string;
    createDialogTitle: string;
    editDialogTitle: string;
    deleteDialogTitle: string;
    closeDialog: string;

    liveMarketSearch: string;
    findListedInstrument: string;
    marketSearchDescription: string;
    clearSelection: string;

    symbolOrCompanyName: string;
    marketSearchPlaceholder: string;
    searchingMarket: string;
    marketSearchResults: string;
    unknownExchange: string;

    selectedInstrument: string;
    country: string;
    instrumentType: string;
    latestPrice: string;
    loading: string;
    unavailable: string;

    manualMarketNote: string;

    symbol: string;
    assetType: string;
    assetTypeStock: string;
    assetTypeEtf: string;
    assetTypeBond: string;
    assetTypeFund: string;
    assetTypeCrypto: string;
    assetTypeCommodity: string;
    assetTypeForex: string;
    assetTypeCash: string;
    assetTypeOther: string;
    displayName: string;
    currency: string;
    notes: string;

    cancel: string;
    adding: string;
    saving: string;
    deleting: string;
    saveAsset: string;

    deletePrompt: string;
    deleteTransactionsNotice: string;

    selectPortfolioBeforeCreate: string;

    createdSuccess: string;
    updatedSuccess: string;
    deletedSuccess: string;

    enterAtLeastTwoCharacters: string;
    noMarketMatches: string;

    symbolRequired: string;
    symbolTooLong: string;
    displayNameRequired: string;
    displayNameTooLong: string;
    currencyInvalid: string;
    isinInvalid: string;
    exchangeTooLong: string;
    notesTooLong: string;

    marketSearchError: string;
    marketServiceUnavailable: string;
    sessionExpired: string;
    marketRateLimit: string;
    marketNotConfigured: string;

    marketPriceError: string;
    marketPriceNotFound: string;
    marketPriceRateLimit: string;

    portfolioLoadError: string;
    serverUnavailable: string;
    forbiddenViewPortfolios: string;

    assetsLoadError: string;
    forbiddenViewAssets: string;
    selectedPortfolioNotFound: string;

    forbiddenAction: string;
    assetOrPortfolioNotFound: string;
    duplicateSymbol: string;
    invalidAsset: string;

    createError: string;
    editError: string;
    deleteError: string;
}


interface TransactionsTranslations {
    eyebrow: string;
    title: string;
    description: string;
    addTransaction: string;
    dismissMessage: string;
    loadingPortfoliosTitle: string;
    loadingPortfoliosDescription: string;
    unavailableTitle: string;
    retry: string;
    noPortfoliosTitle: string;
    noPortfoliosDescription: string;
    portfolio: string;
    asset: string;
    type: string;
    allAssets: string;
    allTypes: string;
    fromDate: string;
    toDate: string;
    clear: string;
    applyFilters: string;
    startDateAfterEndDate: string;
    loadingTransactionsTitle: string;
    loadingTransactionsDescription: string;
    noTransactionsTitle: string;
    noTransactionsDescriptionPrefix: string;
    noTransactionsDescriptionSuffix: string;
    addFirstTransaction: string;
    summaryAriaLabel: string;
    totalTransactions: string;
    matchingActiveFilters: string;
    displayedAmount: string;
    totalOnCurrentPage: string;
    displayedTypes: string;
    distinctTransactionTypes: string;
    executed: string;
    quantity: string;
    unitPrice: string;
    fee: string;
    notApplicable: string;
    notSpecified: string;
    noAsset: string;
    unknownAsset: string;
    edit: string;
    delete: string;
    transactionPages: string;
    previous: string;
    page: string;
    of: string;
    next: string;
    transactionAction: string;
    createDialogTitle: string;
    editDialogTitle: string;
    deleteDialogTitle: string;
    closeDialog: string;
    transactionType: string;
    selectAsset: string;
    loadingLiveMarketPrice: string;
    liveMarketPriceLoaded: string;
    manualExecutionPrice: string;
    manualExecutionPriceDescription: string;
    selectAssetToLoadPrice: string;
    manualPrice: string;
    refreshPrice: string;
    totalAmount: string;
    calculationMode: string;
    calculateByQuantity: string;
    calculateByAmount: string;
    amountModeDescription: string;
    quantityModeDescription: string;
    currency: string;
    executionTime: string;
    notes: string;
    cancel: string;
    adding: string;
    saving: string;
    deleting: string;
    saveTransaction: string;
    deleteTransaction: string;
    deletePromptPrefix: string;
    deletePromptMiddle: string;
    deleteCalculationNotice: string;
    typeBuy: string;
    typeSell: string;
    typeDividend: string;
    typeDeposit: string;
    typeWithdrawal: string;
    typeFee: string;
    createdSuccess: string;
    updatedSuccess: string;
    deletedSuccess: string;
    selectAssetBeforeMarketPrice: string;
    marketPriceError: string;
    marketServiceUnavailable: string;
    sessionExpired: string;
    marketPriceForbidden: string;
    marketPriceNotFound: string;
    selectAssetRequired: string;
    quantityInvalid: string;
    unitPriceInvalid: string;
    feeInvalid: string;
    totalAmountInvalid: string;
    currencyInvalid: string;
    executionTimeRequired: string;
    executionTimeInvalid: string;
    notesTooLong: string;
    portfolioLoadError: string;
    serverUnavailable: string;
    forbiddenViewPortfolios: string;
    transactionsLoadError: string;
    forbiddenViewTransactions: string;
    selectedPortfolioNotFound: string;
    invalidFilters: string;
    forbiddenAction: string;
    transactionResourceNotFound: string;
    transactionConflict: string;
    invalidTransaction: string;
    createError: string;
    editError: string;
    deleteError: string;
}



interface AllocationPurchaseTranslations {
    eyebrow: string;
    title: string;
    description: string;
    reset: string;
    dismissSuccessMessage: string;

    loadingPortfoliosTitle: string;
    loadingPortfoliosDescription: string;
    unavailableTitle: string;
    retry: string;
    noEligiblePortfoliosTitle: string;
    noEligiblePortfoliosDescription: string;

    purchaseSetup: string;
    setTargetAllocation: string;
    portfolio: string;
    asset: string;
    loadingAssets: string;
    noUsdAssetsAvailable: string;
    targetWeight: string;
    purchaseFee: string;
    portfolioValue: string;
    creationMethod: string;
    creationMethodByAmount: string;
    exchangeNotSpecified: string;

    calculating: string;
    calculatePreview: string;
    calculatingAllocationTitle: string;
    calculatingAllocationDescription: string;

    purchasePreview: string;
    allocation: string;
    actionBuy: string;
    actionSellRequired: string;
    actionNone: string;

    currentMarketPrice: string;
    targetWeightLabel: string;
    targetMarketValue: string;
    existingQuantity: string;
    targetQuantity: string;
    quantityToBuy: string;
    estimatedPurchase: string;
    remainingAssignableWeight: string;
    calculated: string;
    recalculate: string;
    executing: string;
    executePurchase: string;
    sellRequiredMessage: string;
    alreadyAtTargetMessage: string;

    purchaseCompleted: string;
    purchased: string;
    complete: string;
    purchasedQuantity: string;
    unitPrice: string;
    purchaseAmount: string;
    fee: string;
    totalCashUsed: string;
    availableCashAfter: string;
    transactionId: string;
    executed: string;
    startAnotherAllocation: string;

    noPreviewTitle: string;
    noPreviewDescription: string;

    previewError: string;
    previewRequired: string;
    noAdditionalPurchaseRequired: string;
    executeError: string;
    portfoliosLoadError: string;
    assetsLoadError: string;
    selectPortfolio: string;
    selectAsset: string;
    targetWeightRequired: string;
    targetWeightInvalid: string;
    feeInvalid: string;

    serverUnavailable: string;
    sessionExpired: string;
    forbiddenAction: string;
    resourceNotFound: string;

    purchaseSuccessPrefix: string;
    purchaseSuccessSuffix: string;
}

interface LoginTranslations {
    eyebrow: string;
    title: string;
    description: string;
    benefitPerformance: string;
    benefitWeeklyTargets: string;
    benefitAssetsTransactions: string;

    cardTitle: string;
    cardDescription: string;

    emailAddress: string;
    emailPlaceholder: string;
    emailRequired: string;
    emailInvalid: string;

    password: string;
    passwordHint: string;
    passwordPlaceholder: string;
    passwordRequired: string;
    passwordLength: string;

    signingIn: string;
    login: string;

    noAccount: string;
    createOne: string;

    registrationSuccess: string;

    genericError: string;
    serverUnavailable: string;
    invalidCredentials: string;
    forbidden: string;
    loginFailed: string;
}

interface RegisterTranslations {
    eyebrow: string;
    title: string;
    description: string;
    benefitMultiplePortfolios: string;
    benefitAssetsTransactions: string;
    benefitWeeklyTargets: string;

    cardTitle: string;
    cardDescription: string;

    firstName: string;
    firstNamePlaceholder: string;
    firstNameRequired: string;
    firstNameLength: string;

    lastName: string;
    lastNamePlaceholder: string;
    lastNameRequired: string;
    lastNameLength: string;

    emailAddress: string;
    emailPlaceholder: string;
    emailRequired: string;
    emailInvalid: string;
    emailLength: string;

    password: string;
    passwordHint: string;
    passwordPlaceholder: string;
    passwordRequired: string;
    passwordLength: string;

    confirmPassword: string;
    confirmPasswordPlaceholder: string;
    confirmPasswordRequired: string;
    passwordsMismatch: string;

    creatingAccount: string;
    createAccount: string;

    alreadyHaveAccount: string;
    login: string;

    genericError: string;
    serverUnavailable: string;
    duplicateEmail: string;
    createFailed: string;
}

interface AuthTranslations {
    login: LoginTranslations;
    register: RegisterTranslations;
}

interface ApplicationTranslations {
    dashboard: DashboardTranslations;
    portfolios: PortfoliosTranslations;
    assets: AssetsTranslations;
    transactions: TransactionsTranslations;
    allocationPurchase: AllocationPurchaseTranslations;
    auth: AuthTranslations;
}

const ENGLISH_TRANSLATIONS:
    ApplicationTranslations = {
    dashboard: {
        portfolioOverview:
            'Portfolio overview',
        title:
            'Dashboard',
        description:
            (
                'Review cash, holdings, calculated values ' +
                'and overall portfolio performance.'
            ),
        managePortfolios:
            'Manage portfolios',
        loadingTitle:
            'Loading portfolio analytics',
        loadingDescription:
            (
                'Your latest transaction-based values ' +
                'are being calculated.'
            ),
        unavailableTitle:
            'Dashboard unavailable',
        retry:
            'Try again',
        emptyTitle:
            'No portfolios yet',
        emptyDescription:
            (
                'Create your first portfolio to begin tracking ' +
                'allocations, values and returns.'
            ),
        createPortfolio:
            'Create a portfolio',
        summaryAriaLabel:
            'Portfolio summary',
        portfolios:
            'Portfolios',
        activeInvestmentAccounts:
            'Active investment accounts',
        currentValue:
            'Current value',
        cashPlusHoldings:
            'Cash plus holdings',
        availableCash:
            'Available cash',
        ofCurrentValue:
            'of current value',
        holdingsValue:
            'Holdings value',
        invested:
            'invested',
        initialValue:
            'Initial value',
        totalStartingCapital:
            'Total starting capital',
        totalReturn:
            'Total return',
        calculatedAccounts:
            'Calculated accounts',
        yourPortfolios:
            'Your portfolios',
        viewAll:
            'View all',
        portfolio:
            'Portfolio',
        cash:
            'Cash',
        holdings:
            'Holdings',
        return:
            'Return',
        calculated:
            'Calculated',
        createdByAmount:
            'Created by amount',
        createdByHoldings:
            'Created by holdings',
        partialCachedData:
            'Partial cached data',
        unavailableDataNotice:
            (
                'Some live analytics or allocation requests ' +
                'were unavailable. Cached values are shown ' +
                'where necessary.'
            ),
        bestPerformer:
            'Best performer',
        realizedProfit:
            'Realized profit',
        unrealizedProfit:
            'Unrealized profit',
        loadError:
            (
                'Portfolio data could not be loaded. ' +
                'Please try again.'
            ),
    },

    portfolios: {
        eyebrow:
            'Portfolio management',
        title:
            'Portfolios',
        description:
            (
                'Create and manage your investment portfolios, ' +
                'review transaction-based values and monitor ' +
                'calculated performance.'
            ),
        createPortfolio:
            'Create portfolio',
        dismissMessage:
            'Dismiss message',

        loadingTitle:
            'Loading portfolio analytics',
        loadingDescription:
            (
                'Portfolio values and performance are being ' +
                'calculated from your transaction history.'
            ),
        unavailableTitle:
            'Portfolios unavailable',
        retry:
            'Try again',

        emptyTitle:
            'No portfolios yet',
        emptyDescription:
            (
                'Create your first portfolio to begin tracking ' +
                'holdings, transactions, values and returns.'
            ),
        createFirstPortfolio:
            'Create your first portfolio',

        currentPageSummary:
            'Current page summary',
        allPortfolios:
            'All portfolios',
        acrossAllPages:
            'Across all pages',
        displayedValue:
            'Displayed value',
        calculatedValueOnPage:
            'Calculated value on this page',
        displayedProfit:
            'Displayed profit',
        combinedCalculatedProfit:
            'Combined calculated profit',

        analyticsUnavailableSingle:
            'portfolio analytics request was unavailable.',
        analyticsUnavailableMultiple:
            'portfolio analytics requests were unavailable.',
        cachedValuesTemporarily:
            'Cached portfolio values are shown temporarily.',

        createdByAmount:
            'Created by amount',
        createdByHoldings:
            'Created by holdings',
        calculated:
            'Calculated',
        cached:
            'Cached',

        initialValue:
            'Initial value',
        currentValue:
            'Current value',
        totalProfit:
            'Total profit',
        realizedProfit:
            'Realized profit',
        unrealizedProfit:
            'Unrealized profit',
        activeAssets:
            'Active assets',
        transactions:
            'Transactions',
        dataSource:
            'Data source',
        liveAnalytics:
            'Live analytics',
        cachedPortfolio:
            'Cached portfolio',

        showAllocation:
            'View allocation',
        hideAllocation:
            'Hide allocation',
        rename:
            'Rename',
        delete:
            'Delete',

        currentAllocation:
            'Current allocation',
        holdingsAndWeights:
            'Holdings and weights',
        loadingAllocation:
            'Loading portfolio allocation...',
        allocationUnavailable:
            'Allocation unavailable',
        allocationSummary:
            'Portfolio allocation summary',

        portfolioValue:
            'Portfolio value',
        availableCash:
            'Available cash',
        holdingsMarketValue:
            'Holdings market value',
        totalCost:
            'Total cost',
        targetAllocation:
            'Target allocation',
        allocatedAssets:
            'Allocated assets',

        noAllocatedAssets:
            'No allocated assets',
        noAllocatedAssetsDescription:
            (
                'Add assets and transactions to this portfolio ' +
                'to calculate quantities, values and weights.'
            ),

        asset:
            'Asset',
        quantity:
            'Quantity',
        averageCost:
            'Average cost',
        currentPrice:
            'Current price',
        marketValue:
            'Market value',
        weight:
            'Weight',
        realized:
            'Realized',
        unrealized:
            'Unrealized',

        portfolioPages:
            'Portfolio pages',
        previous:
            'Previous',
        next:
            'Next',
        page:
            'Page',
        of:
            'of',

        portfolioAction:
            'Portfolio action',
        createDialogTitle:
            'Create portfolio',
        renameDialogTitle:
            'Rename portfolio',
        deleteDialogTitle:
            'Delete portfolio',
        closeDialog:
            'Close dialog',

        portfolioName:
            'Portfolio name',
        creationMethod:
            'Creation method',
        allocateByAmount:
            'Allocate by amount',
        enterExistingHoldings:
            'Enter existing holdings',
        initialValueField:
            'Initial value',
        automaticCurrentValueNote:
            (
                'Current value will be calculated automatically ' +
                'from transactions and portfolio analytics.'
            ),

        cancel:
            'Cancel',
        creating:
            'Creating...',
        saving:
            'Saving...',
        deleting:
            'Deleting...',
        saveName:
            'Save name',

        renamePrompt:
            'Enter a new name for',
        deletePrompt:
            'You are about to delete',
        deleteDescription:
            (
                'This action removes the portfolio ' +
                'from your active account.'
            ),

        portfolioNameRequired:
            'Portfolio name is required.',
        initialValueInvalid:
            'Initial value must be zero or greater.',
        createdSuccess:
            'Portfolio created successfully.',
        renamedSuccess:
            'Portfolio renamed successfully.',
        deletedSuccess:
            'Portfolio deleted successfully.',

        loadError:
            (
                'Portfolios could not be loaded. ' +
                'Please try again.'
            ),
        serverUnavailable:
            (
                'The server could not be reached. ' +
                'Check that the backend is running.'
            ),
        sessionExpired:
            (
                'Your session has expired. ' +
                'Please log in again.'
            ),
        forbiddenView:
            'You do not have permission to view portfolios.',

        allocationLoadError:
            (
                'Portfolio allocation could not be loaded. ' +
                'Please try again.'
            ),
        allocationForbidden:
            (
                'You do not have permission to view ' +
                'this portfolio allocation.'
            ),
        allocationNotFound:
            'Portfolio allocation was not found.',

        forbiddenAction:
            (
                'You do not have permission ' +
                'to perform this action.'
            ),
        duplicateName:
            'A portfolio with this name already exists.',
        portfolioConflict:
            'The portfolio conflicts with existing data.',
        invalidPortfolio:
            'The submitted portfolio information is invalid.',
        createError:
            'The portfolio could not be created.',
        renameError:
            'The portfolio could not be renamed.',
        deleteError:
            'The portfolio could not be deleted.',
    },

    assets: {
        eyebrow:
            'Asset management',
        title:
            'Assets',
        description:
            (
                'Create and maintain the financial instruments ' +
                'held in each investment portfolio.'
            ),
        addAsset:
            'Add asset',
        dismissMessage:
            'Dismiss message',

        loadingPortfoliosTitle:
            'Loading portfolios',
        loadingPortfoliosDescription:
            'Available portfolios are being retrieved.',
        unavailableTitle:
            'Assets unavailable',
        retry:
            'Try again',

        noPortfoliosTitle:
            'No portfolios available',
        noPortfoliosDescription:
            'Create a portfolio before adding assets.',

        portfolio:
            'Portfolio',
        searchAssets:
            'Search assets',
        searchPlaceholder:
            'Symbol, name, currency or exchange',

        loadingAssetsTitle:
            'Loading assets',
        loadingAssetsDescription:
            (
                'Assets for the selected portfolio ' +
                'are being retrieved.'
            ),

        noAssetsTitle:
            'No assets yet',
        noAssetsDescription:
            'Add the first asset to',
        addFirstAsset:
            'Add first asset',

        summaryAriaLabel:
            'Asset summary',
        totalAssets:
            'Total assets',
        inSelectedPortfolio:
            'In the selected portfolio',
        assetTypes:
            'Asset types',
        distinctClassifications:
            'Distinct classifications',
        currencies:
            'Currencies',
        distinctTradingCurrencies:
            'Distinct trading currencies',

        noMatchingAssets:
            'No matching assets',
        noMatchingAssetsDescription:
            (
                'Change the search value ' +
                'to display other assets.'
            ),

        exchange:
            'Exchange',
        notSpecified:
            'Not specified',
        isin:
            'ISIN',
        created:
            'Created',
        updated:
            'Updated',

        edit:
            'Edit',
        delete:
            'Delete',

        assetAction:
            'Asset action',
        createDialogTitle:
            'Add asset',
        editDialogTitle:
            'Edit asset',
        deleteDialogTitle:
            'Delete asset',
        closeDialog:
            'Close dialog',

        liveMarketSearch:
            'Live market search',
        findListedInstrument:
            'Find a listed instrument',
        marketSearchDescription:
            (
                'Search by ticker symbol or company name. ' +
                'Selecting a result fills the asset details ' +
                'automatically.'
            ),
        clearSelection:
            'Clear selection',

        symbolOrCompanyName:
            'Symbol or company name',
        marketSearchPlaceholder:
            'TTWO, AAPL, NVIDIA...',
        searchingMarket:
            'Searching market',
        marketSearchResults:
            'Market search results',
        unknownExchange:
            'Unknown exchange',

        selectedInstrument:
            'Selected instrument',
        country:
            'Country',
        instrumentType:
            'Instrument type',
        latestPrice:
            'Latest price',
        loading:
            'Loading...',
        unavailable:
            'Unavailable',

        manualMarketNote:
            (
                'Market search is optional. All fields below ' +
                'remain editable and can be completed manually.'
            ),

        symbol:
            'Symbol',
        assetType:
            'Asset type',
        assetTypeStock:
            'Stock',
        assetTypeEtf:
            'ETF',
        assetTypeBond:
            'Bond',
        assetTypeFund:
            'Fund',
        assetTypeCrypto:
            'Cryptocurrency',
        assetTypeCommodity:
            'Commodity',
        assetTypeForex:
            'Foreign exchange',
        assetTypeCash:
            'Cash',
        assetTypeOther:
            'Other',
        displayName:
            'Display name',
        currency:
            'Currency',
        notes:
            'Notes',

        cancel:
            'Cancel',
        adding:
            'Adding...',
        saving:
            'Saving...',
        deleting:
            'Deleting...',
        saveAsset:
            'Save asset',

        deletePrompt:
            'You are about to delete',
        deleteTransactionsNotice:
            (
                'Existing transactions may still ' +
                'refer to this asset.'
            ),

        selectPortfolioBeforeCreate:
            'Select a portfolio before creating an asset.',

        createdSuccess:
            'Asset created successfully.',
        updatedSuccess:
            'Asset updated successfully.',
        deletedSuccess:
            'Asset deleted successfully.',

        enterAtLeastTwoCharacters:
            'Enter at least 2 characters.',
        noMarketMatches:
            'No matching market instruments were found.',

        symbolRequired:
            'Asset symbol is required.',
        symbolTooLong:
            'Asset symbol must not exceed 30 characters.',
        displayNameRequired:
            'Asset display name is required.',
        displayNameTooLong:
            'Asset display name must not exceed 160 characters.',
        currencyInvalid:
            'Currency must contain exactly 3 letters.',
        isinInvalid:
            'ISIN must contain exactly 12 letters or digits.',
        exchangeTooLong:
            'Exchange must not exceed 40 characters.',
        notesTooLong:
            'Notes must not exceed 2000 characters.',

        marketSearchError:
            (
                'Market instruments could not be searched. ' +
                'You can continue with manual entry.'
            ),
        marketServiceUnavailable:
            (
                'The market-data service could not be reached. ' +
                'You can continue with manual entry.'
            ),
        sessionExpired:
            (
                'Your session has expired. ' +
                'Please log in again.'
            ),
        marketRateLimit:
            (
                'The market-data request limit has been reached. ' +
                'Wait briefly or continue with manual entry.'
            ),
        marketNotConfigured:
            (
                'Market data is not configured. ' +
                'You can continue with manual entry.'
            ),

        marketPriceError:
            (
                'The latest price could not be loaded. ' +
                'The asset details were still filled in.'
            ),
        marketPriceNotFound:
            (
                'No current price was returned for this instrument. ' +
                'The asset details were still filled in.'
            ),
        marketPriceRateLimit:
            (
                'The market-data request limit has been reached. ' +
                'The asset details were still filled in.'
            ),

        portfolioLoadError:
            (
                'Portfolios could not be loaded. ' +
                'Please try again.'
            ),
        serverUnavailable:
            (
                'The server could not be reached. ' +
                'Check that the backend is running.'
            ),
        forbiddenViewPortfolios:
            'You do not have permission to view portfolios.',

        assetsLoadError:
            (
                'Assets could not be loaded. ' +
                'Please try again.'
            ),
        forbiddenViewAssets:
            'You do not have permission to view assets.',
        selectedPortfolioNotFound:
            'The selected portfolio could not be found.',

        forbiddenAction:
            (
                'You do not have permission ' +
                'to perform this action.'
            ),
        assetOrPortfolioNotFound:
            'The asset or portfolio could not be found.',
        duplicateSymbol:
            (
                'An asset with this symbol ' +
                'already exists in the portfolio.'
            ),
        invalidAsset:
            'The submitted asset information is invalid.',

        createError:
            'The asset could not be created.',
        editError:
            'The asset could not be updated.',
        deleteError:
            'The asset could not be deleted.',
    },

    transactions: {
        eyebrow: 'Transaction management',
        title: 'Transactions',
        description: (
            'Record purchases, sales, dividends, deposits, ' +
            'withdrawals and portfolio fees.'
        ),
        addTransaction: 'Add transaction',
        dismissMessage: 'Dismiss message',
        loadingPortfoliosTitle: 'Loading portfolios',
        loadingPortfoliosDescription: 'Available portfolios are being retrieved.',
        unavailableTitle: 'Transactions unavailable',
        retry: 'Try again',
        noPortfoliosTitle: 'No portfolios available',
        noPortfoliosDescription: 'Create a portfolio before recording transactions.',
        portfolio: 'Portfolio',
        asset: 'Asset',
        type: 'Type',
        allAssets: 'All assets',
        allTypes: 'All types',
        fromDate: 'From date',
        toDate: 'To date',
        clear: 'Clear',
        applyFilters: 'Apply filters',
        startDateAfterEndDate: 'Start date must not be after end date.',
        loadingTransactionsTitle: 'Loading transactions',
        loadingTransactionsDescription: 'Transactions for the selected portfolio are being retrieved.',
        noTransactionsTitle: 'No transactions found',
        noTransactionsDescriptionPrefix: 'Record the first transaction for',
        noTransactionsDescriptionSuffix: 'or change the active filters.',
        addFirstTransaction: 'Add first transaction',
        summaryAriaLabel: 'Transaction summary',
        totalTransactions: 'Total transactions',
        matchingActiveFilters: 'Matching the active filters',
        displayedAmount: 'Displayed amount',
        totalOnCurrentPage: 'Total on the current page',
        displayedTypes: 'Displayed types',
        distinctTransactionTypes: 'Distinct transaction types',
        executed: 'Executed',
        quantity: 'Quantity',
        unitPrice: 'Unit price',
        fee: 'Fee',
        notApplicable: 'Not applicable',
        notSpecified: 'Not specified',
        noAsset: 'No asset',
        unknownAsset: 'Unknown asset',
        edit: 'Edit',
        delete: 'Delete',
        transactionPages: 'Transaction pages',
        previous: 'Previous',
        page: 'Page',
        of: 'of',
        next: 'Next',
        transactionAction: 'Transaction action',
        createDialogTitle: 'Add transaction',
        editDialogTitle: 'Edit transaction',
        deleteDialogTitle: 'Delete transaction',
        closeDialog: 'Close dialog',
        transactionType: 'Transaction type',
        selectAsset: 'Select asset',
        loadingLiveMarketPrice: 'Loading live market price...',
        liveMarketPriceLoaded: 'Live market price loaded',
        manualExecutionPrice: 'Manual execution price',
        manualExecutionPriceDescription: 'Use this for historical transactions or a known execution price.',
        selectAssetToLoadPrice: 'Select an asset to load its price',
        manualPrice: 'Manual price',
        refreshPrice: 'Refresh price',
        totalAmount: 'Total amount',
        calculationMode: 'Calculation mode',
        calculateByQuantity: 'By quantity',
        calculateByAmount: 'By amount',
        amountModeDescription:
            'Enter the total amount and ShareCutter will calculate fractional quantity from the current or manual unit price.',
        quantityModeDescription:
            'Enter the quantity and ShareCutter will calculate the total amount from quantity, unit price and fee.',
        currency: 'Currency',
        executionTime: 'Execution time',
        notes: 'Notes',
        cancel: 'Cancel',
        adding: 'Adding...',
        saving: 'Saving...',
        deleting: 'Deleting...',
        saveTransaction: 'Save transaction',
        deleteTransaction: 'Delete transaction',
        deletePromptPrefix: 'You are about to delete this',
        deletePromptMiddle: 'transaction from',
        deleteCalculationNotice: 'The transaction will no longer appear in portfolio calculations.',
        typeBuy: 'Buy',
        typeSell: 'Sell',
        typeDividend: 'Dividend',
        typeDeposit: 'Deposit',
        typeWithdrawal: 'Withdrawal',
        typeFee: 'Fee',
        createdSuccess: 'Transaction created successfully.',
        updatedSuccess: 'Transaction updated successfully.',
        deletedSuccess: 'Transaction deleted successfully.',
        selectAssetBeforeMarketPrice: 'Select an asset before loading its market price.',
        marketPriceError: 'The current market price could not be loaded. Enter the execution price manually.',
        marketServiceUnavailable: 'The market data service could not be reached. Enter the execution price manually.',
        sessionExpired: 'Your session has expired. Please log in again.',
        marketPriceForbidden: 'You do not have permission to retrieve market prices.',
        marketPriceNotFound: 'No current market price was found for this asset. Enter the execution price manually.',
        selectAssetRequired: 'Select an asset for this transaction type.',
        quantityInvalid: 'Quantity must be greater than zero.',
        unitPriceInvalid: 'Unit price must be zero or greater.',
        feeInvalid: 'Fee must be zero or greater.',
        totalAmountInvalid: 'Total amount must be zero or greater.',
        currencyInvalid: 'Currency must contain exactly 3 letters.',
        executionTimeRequired: 'Execution date and time are required.',
        executionTimeInvalid: 'Execution date and time are invalid.',
        notesTooLong: 'Notes must not exceed 2000 characters.',
        portfolioLoadError: 'Portfolios could not be loaded. Please try again.',
        serverUnavailable: 'The server could not be reached. Check that the backend is running.',
        forbiddenViewPortfolios: 'You do not have permission to view portfolios.',
        transactionsLoadError: 'Transactions could not be loaded. Please try again.',
        forbiddenViewTransactions: 'You do not have permission to view transactions.',
        selectedPortfolioNotFound: 'The selected portfolio could not be found.',
        invalidFilters: 'The transaction filters are invalid.',
        forbiddenAction: 'You do not have permission to perform this action.',
        transactionResourceNotFound: 'The portfolio, transaction or asset could not be found.',
        transactionConflict: 'The transaction conflicts with existing data.',
        invalidTransaction: 'The submitted transaction information is invalid.',
        createError: 'The transaction could not be created.',
        editError: 'The transaction could not be updated.',
        deleteError: 'The transaction could not be deleted.',
    },


    allocationPurchase: {
        eyebrow:
            'Portfolio allocation',
        title:
            'Allocation purchase',
        description:
            (
                'Choose a portfolio, select an asset and calculate ' +
                'the fractional purchase required to reach a target portfolio weight.'
            ),
        reset:
            'Reset',
        dismissSuccessMessage:
            'Dismiss success message',

        loadingPortfoliosTitle:
            'Loading portfolios',
        loadingPortfoliosDescription:
            (
                'Portfolios eligible for percentage-based allocation ' +
                'are being retrieved.'
            ),
        unavailableTitle:
            'Allocation unavailable',
        retry:
            'Try again',
        noEligiblePortfoliosTitle:
            'No eligible portfolios',
        noEligiblePortfoliosDescription:
            (
                'Allocation purchases are available only for portfolios ' +
                'created by investment amount.'
            ),

        purchaseSetup:
            'Purchase setup',
        setTargetAllocation:
            'Set the target allocation',
        portfolio:
            'Portfolio',
        asset:
            'Asset',
        loadingAssets:
            'Loading assets...',
        noUsdAssetsAvailable:
            'No USD assets available',
        targetWeight:
            'Target weight (%)',
        purchaseFee:
            'Purchase fee',
        portfolioValue:
            'Portfolio value',
        creationMethod:
            'Creation method',
        creationMethodByAmount:
            'By investment amount',
        exchangeNotSpecified:
            'Exchange not specified',

        calculating:
            'Calculating...',
        calculatePreview:
            'Calculate preview',
        calculatingAllocationTitle:
            'Calculating allocation',
        calculatingAllocationDescription:
            (
                'The latest market price is being used to calculate ' +
                'the required fractional quantity.'
            ),

        purchasePreview:
            'Purchase preview',
        allocation:
            'allocation',
        actionBuy:
            'Buy',
        actionSellRequired:
            'Sell required',
        actionNone:
            'At target',

        currentMarketPrice:
            'Current market price',
        targetWeightLabel:
            'Target weight',
        targetMarketValue:
            'Target market value',
        existingQuantity:
            'Existing quantity',
        targetQuantity:
            'Target quantity',
        quantityToBuy:
            'Quantity to buy',
        estimatedPurchase:
            'Estimated purchase',
        remainingAssignableWeight:
            'Remaining assignable weight',
        calculated:
            'Calculated',
        recalculate:
            'Recalculate',
        executing:
            'Executing...',
        executePurchase:
            'Execute purchase',
        sellRequiredMessage:
            (
                'The selected target is below the current holding. ' +
                'A sell operation is required instead of a purchase.'
            ),
        alreadyAtTargetMessage:
            'The asset already matches the selected target allocation.',

        purchaseCompleted:
            'Purchase completed',
        purchased:
            'purchased',
        complete:
            'Complete',
        purchasedQuantity:
            'Purchased quantity',
        unitPrice:
            'Unit price',
        purchaseAmount:
            'Purchase amount',
        fee:
            'Fee',
        totalCashUsed:
            'Total cash used',
        availableCashAfter:
            'Available cash after',
        transactionId:
            'Transaction ID',
        executed:
            'Executed',
        startAnotherAllocation:
            'Start another allocation',

        noPreviewTitle:
            'No preview calculated',
        noPreviewDescription:
            (
                'Select a portfolio and asset, enter a target weight ' +
                'and calculate the expected purchase before execution.'
            ),

        previewError:
            'The allocation preview could not be calculated.',
        previewRequired:
            'Calculate a preview before executing the purchase.',
        noAdditionalPurchaseRequired:
            'This allocation does not require an additional purchase.',
        executeError:
            'The allocation purchase could not be executed.',
        portfoliosLoadError:
            'Portfolios could not be loaded.',
        assetsLoadError:
            'Assets for the selected portfolio could not be loaded.',
        selectPortfolio:
            'Select a portfolio.',
        selectAsset:
            'Select an asset.',
        targetWeightRequired:
            'Enter a target weight percentage.',
        targetWeightInvalid:
            'Target weight must be greater than zero and no more than 100.',
        feeInvalid:
            'Fee must not be negative.',

        serverUnavailable:
            'The server could not be reached. Check that the backend is running.',
        sessionExpired:
            'Your session has expired. Please log in again.',
        forbiddenAction:
            'You do not have permission to perform this action.',
        resourceNotFound:
            'The selected portfolio or asset could not be found.',

        purchaseSuccessPrefix:
            'Purchased',
        purchaseSuccessSuffix:
            'successfully.',
    },

    auth: {
        login: {
            eyebrow:
                'ShareCutter account',
            title:
                'Welcome back',
            description:
                (
                    'Log in to review your portfolios, weekly targets, ' +
                    'asset allocations and transaction history.'
                ),
            benefitPerformance:
                'Track portfolio performance',
            benefitWeeklyTargets:
                'Review weekly allocation targets',
            benefitAssetsTransactions:
                'Manage assets and transactions',

            cardTitle:
                'Log in',
            cardDescription:
                (
                    'Enter the email and password associated with ' +
                    'your account.'
                ),

            emailAddress:
                'Email address',
            emailPlaceholder:
                'name@example.com',
            emailRequired:
                'Email address is required.',
            emailInvalid:
                'Enter a valid email address.',

            password:
                'Password',
            passwordHint:
                'Minimum 8 characters',
            passwordPlaceholder:
                'Enter your password',
            passwordRequired:
                'Password is required.',
            passwordLength:
                'Password must contain between 8 and 100 characters.',

            signingIn:
                'Signing in...',
            login:
                'Log in',

            noAccount:
                'Do not have an account?',
            createOne:
                'Create one',

            registrationSuccess:
                (
                    'Your account was created successfully. ' +
                    'You can now log in.'
                ),

            genericError:
                'Something went wrong. Please try again.',
            serverUnavailable:
                (
                    'The ShareCutter server is unavailable. ' +
                    'Make sure the backend is running.'
                ),
            invalidCredentials:
                'The email or password is incorrect.',
            forbidden:
                (
                    'This account is not currently allowed ' +
                    'to access ShareCutter.'
                ),
            loginFailed:
                'Unable to log in. Please try again.',
        },

        register: {
            eyebrow:
                'Start with ShareCutter',
            title:
                'Create your account',
            description:
                (
                    'Build portfolios, record transactions and manage ' +
                    'weekly allocation targets from one personal workspace.'
                ),
            benefitMultiplePortfolios:
                'Create and manage multiple portfolios',
            benefitAssetsTransactions:
                'Track assets and transaction history',
            benefitWeeklyTargets:
                'Define weekly portfolio targets',

            cardTitle:
                'Create account',
            cardDescription:
                'Enter your details to create a ShareCutter account.',

            firstName:
                'First name',
            firstNamePlaceholder:
                'First name',
            firstNameRequired:
                'First name is required.',
            firstNameLength:
                'First name must not exceed 100 characters.',

            lastName:
                'Last name',
            lastNamePlaceholder:
                'Last name',
            lastNameRequired:
                'Last name is required.',
            lastNameLength:
                'Last name must not exceed 100 characters.',

            emailAddress:
                'Email address',
            emailPlaceholder:
                'name@example.com',
            emailRequired:
                'Email address is required.',
            emailInvalid:
                'Enter a valid email address.',
            emailLength:
                'Email address must not exceed 320 characters.',

            password:
                'Password',
            passwordHint:
                '8 to 72 characters',
            passwordPlaceholder:
                'Create a password',
            passwordRequired:
                'Password is required.',
            passwordLength:
                'Password must contain between 8 and 72 characters.',

            confirmPassword:
                'Confirm password',
            confirmPasswordPlaceholder:
                'Enter the password again',
            confirmPasswordRequired:
                'Password confirmation is required.',
            passwordsMismatch:
                'The passwords do not match.',

            creatingAccount:
                'Creating account...',
            createAccount:
                'Create account',

            alreadyHaveAccount:
                'Already have an account?',
            login:
                'Log in',

            genericError:
                'Something went wrong. Please try again.',
            serverUnavailable:
                (
                    'The ShareCutter server is unavailable. ' +
                    'Make sure the backend is running.'
                ),
            duplicateEmail:
                'An account with this email address already exists.',
            createFailed:
                'Unable to create the account. Please try again.',
        },
    },
};

const HEBREW_TRANSLATIONS:
    ApplicationTranslations = {
    dashboard: {
        portfolioOverview:
            'סקירת תיקי השקעות',
        title:
            'לוח בקרה',
        description:
            (
                'סקירת מזומן, אחזקות, שווי מחושב ' +
                'וביצועי תיקי ההשקעות.'
            ),
        managePortfolios:
            'ניהול תיקים',
        loadingTitle:
            'טוען נתוני תיקים',
        loadingDescription:
            (
                'המערכת מחשבת את הנתונים העדכניים ' +
                'על בסיס העסקאות שלך.'
            ),
        unavailableTitle:
            'לוח הבקרה אינו זמין',
        retry:
            'נסה שוב',
        emptyTitle:
            'עדיין אין תיקי השקעות',
        emptyDescription:
            (
                'צור את תיק ההשקעות הראשון שלך כדי להתחיל ' +
                'לעקוב אחר הקצאות, שווי ותשואות.'
            ),
        createPortfolio:
            'יצירת תיק',
        summaryAriaLabel:
            'סיכום תיקי השקעות',
        portfolios:
            'תיקים',
        activeInvestmentAccounts:
            'תיקי השקעות פעילים',
        currentValue:
            'שווי נוכחי',
        cashPlusHoldings:
            'מזומן בתוספת אחזקות',
        availableCash:
            'מזומן זמין',
        ofCurrentValue:
            'מהשווי הנוכחי',
        holdingsValue:
            'שווי אחזקות',
        invested:
            'מושקע',
        initialValue:
            'שווי התחלתי',
        totalStartingCapital:
            'סך ההון ההתחלתי',
        totalReturn:
            'תשואה כוללת',
        calculatedAccounts:
            'תיקים מחושבים',
        yourPortfolios:
            'תיקי ההשקעות שלך',
        viewAll:
            'הצג הכל',
        portfolio:
            'תיק',
        cash:
            'מזומן',
        holdings:
            'אחזקות',
        return:
            'תשואה',
        calculated:
            'עודכן',
        createdByAmount:
            'נוצר לפי סכום',
        createdByHoldings:
            'נוצר לפי אחזקות',
        partialCachedData:
            'חלק מהנתונים מוצגים מהמטמון',
        unavailableDataNotice:
            (
                'חלק מנתוני האנליטיקה או ההקצאה בזמן אמת ' +
                'אינם זמינים כרגע. במידת הצורך מוצגים נתונים שמורים.'
            ),
        bestPerformer:
            'התיק המוביל',
        realizedProfit:
            'רווח ממומש',
        unrealizedProfit:
            'רווח לא ממומש',
        loadError:
            (
                'לא ניתן לטעון את נתוני תיקי ההשקעות. ' +
                'נסה שוב.'
            ),
    },

    portfolios: {
        eyebrow:
            'ניהול תיקי השקעות',
        title:
            'תיקי השקעות',
        description:
            (
                'יצירה וניהול של תיקי ההשקעות, ' +
                'סקירת שווי המבוסס על עסקאות ומעקב אחר ביצועים.'
            ),
        createPortfolio:
            'יצירת תיק',
        dismissMessage:
            'סגור הודעה',

        loadingTitle:
            'טוען נתוני תיקים',
        loadingDescription:
            (
                'שווי התיקים והביצועים מחושבים ' +
                'על בסיס היסטוריית העסקאות שלך.'
            ),
        unavailableTitle:
            'לא ניתן לטעון את התיקים',
        retry:
            'נסה שוב',

        emptyTitle:
            'עדיין אין תיקי השקעות',
        emptyDescription:
            (
                'צור את תיק ההשקעות הראשון שלך כדי להתחיל ' +
                'לעקוב אחר אחזקות, עסקאות, שווי ותשואות.'
            ),
        createFirstPortfolio:
            'צור את התיק הראשון',

        currentPageSummary:
            'סיכום העמוד הנוכחי',
        allPortfolios:
            'כל התיקים',
        acrossAllPages:
            'בכל העמודים',
        displayedValue:
            'שווי מוצג',
        calculatedValueOnPage:
            'השווי המחושב בעמוד זה',
        displayedProfit:
            'רווח מוצג',
        combinedCalculatedProfit:
            'סך הרווח המחושב',

        analyticsUnavailableSingle:
            'בקשת אנליטיקה אחת אינה זמינה.',
        analyticsUnavailableMultiple:
            'בקשות אנליטיקה אינן זמינות.',
        cachedValuesTemporarily:
            'נתונים שמורים מוצגים באופן זמני.',

        createdByAmount:
            'נוצר לפי סכום',
        createdByHoldings:
            'נוצר לפי אחזקות',
        calculated:
            'מחושב',
        cached:
            'נתון שמור',

        initialValue:
            'שווי התחלתי',
        currentValue:
            'שווי נוכחי',
        totalProfit:
            'רווח כולל',
        realizedProfit:
            'רווח ממומש',
        unrealizedProfit:
            'רווח לא ממומש',
        activeAssets:
            'נכסים פעילים',
        transactions:
            'עסקאות',
        dataSource:
            'מקור נתונים',
        liveAnalytics:
            'אנליטיקה בזמן אמת',
        cachedPortfolio:
            'נתוני תיק שמורים',

        showAllocation:
            'הצג הקצאה',
        hideAllocation:
            'הסתר הקצאה',
        rename:
            'שינוי שם',
        delete:
            'מחיקה',

        currentAllocation:
            'הקצאה נוכחית',
        holdingsAndWeights:
            'אחזקות ומשקלים',
        loadingAllocation:
            'טוען הקצאת תיק...',
        allocationUnavailable:
            'נתוני ההקצאה אינם זמינים',
        allocationSummary:
            'סיכום הקצאת התיק',

        portfolioValue:
            'שווי תיק',
        availableCash:
            'מזומן זמין',
        holdingsMarketValue:
            'שווי שוק של האחזקות',
        totalCost:
            'עלות כוללת',
        targetAllocation:
            'הקצאת יעד',
        allocatedAssets:
            'נכסים מוקצים',

        noAllocatedAssets:
            'אין נכסים מוקצים',
        noAllocatedAssetsDescription:
            (
                'הוסף נכסים ועסקאות לתיק כדי לחשב ' +
                'כמויות, שווי ומשקלים.'
            ),

        asset:
            'נכס',
        quantity:
            'כמות',
        averageCost:
            'עלות ממוצעת',
        currentPrice:
            'מחיר נוכחי',
        marketValue:
            'שווי שוק',
        weight:
            'משקל',
        realized:
            'ממומש',
        unrealized:
            'לא ממומש',

        portfolioPages:
            'עמודי תיקי השקעות',
        previous:
            'הקודם',
        next:
            'הבא',
        page:
            'עמוד',
        of:
            'מתוך',

        portfolioAction:
            'פעולה בתיק',
        createDialogTitle:
            'יצירת תיק',
        renameDialogTitle:
            'שינוי שם תיק',
        deleteDialogTitle:
            'מחיקת תיק',
        closeDialog:
            'סגור חלון',

        portfolioName:
            'שם התיק',
        creationMethod:
            'שיטת יצירה',
        allocateByAmount:
            'הקצאה לפי סכום',
        enterExistingHoldings:
            'הזנת אחזקות קיימות',
        initialValueField:
            'שווי התחלתי',
        automaticCurrentValueNote:
            (
                'השווי הנוכחי יחושב אוטומטית ' +
                'לפי העסקאות והאנליטיקה של התיק.'
            ),

        cancel:
            'ביטול',
        creating:
            'יוצר...',
        saving:
            'שומר...',
        deleting:
            'מוחק...',
        saveName:
            'שמירת שם',

        renamePrompt:
            'הזן שם חדש עבור',
        deletePrompt:
            'אתה עומד למחוק את',
        deleteDescription:
            (
                'פעולה זו תסיר את התיק ' +
                'מרשימת התיקים הפעילים שלך.'
            ),

        portfolioNameRequired:
            'יש להזין שם לתיק.',
        initialValueInvalid:
            'השווי ההתחלתי חייב להיות אפס או יותר.',
        createdSuccess:
            'התיק נוצר בהצלחה.',
        renamedSuccess:
            'שם התיק עודכן בהצלחה.',
        deletedSuccess:
            'התיק נמחק בהצלחה.',

        loadError:
            'לא ניתן לטעון את התיקים. נסה שוב.',
        serverUnavailable:
            (
                'לא ניתן להתחבר לשרת. ' +
                'ודא שה־Backend פועל.'
            ),
        sessionExpired:
            (
                'פג תוקף ההתחברות שלך. ' +
                'יש להתחבר מחדש.'
            ),
        forbiddenView:
            'אין לך הרשאה לצפות בתיקי ההשקעות.',

        allocationLoadError:
            'לא ניתן לטעון את הקצאת התיק. נסה שוב.',
        allocationForbidden:
            'אין לך הרשאה לצפות בהקצאת תיק זה.',
        allocationNotFound:
            'לא נמצאו נתוני הקצאה לתיק.',

        forbiddenAction:
            'אין לך הרשאה לבצע פעולה זו.',
        duplicateName:
            'כבר קיים תיק בשם זה.',
        portfolioConflict:
            'קיימת התנגשות בין התיק לנתונים קיימים.',
        invalidPortfolio:
            'פרטי התיק שהוזנו אינם תקינים.',
        createError:
            'לא ניתן ליצור את התיק.',
        renameError:
            'לא ניתן לשנות את שם התיק.',
        deleteError:
            'לא ניתן למחוק את התיק.',
    },

    assets: {
        eyebrow:
            'ניהול נכסים',
        title:
            'נכסים',
        description:
            (
                'יצירה וניהול של הנכסים הפיננסיים ' +
                'המוחזקים בכל תיק השקעות.'
            ),
        addAsset:
            'הוספת נכס',
        dismissMessage:
            'סגור הודעה',

        loadingPortfoliosTitle:
            'טוען תיקי השקעות',
        loadingPortfoliosDescription:
            'רשימת תיקי ההשקעות נטענת.',
        unavailableTitle:
            'לא ניתן לטעון את הנכסים',
        retry:
            'נסה שוב',

        noPortfoliosTitle:
            'אין תיקי השקעות זמינים',
        noPortfoliosDescription:
            'יש ליצור תיק השקעות לפני הוספת נכסים.',

        portfolio:
            'תיק השקעות',
        searchAssets:
            'חיפוש נכסים',
        searchPlaceholder:
            'סימול, שם, מטבע או בורסה',

        loadingAssetsTitle:
            'טוען נכסים',
        loadingAssetsDescription:
            'הנכסים בתיק שנבחר נטענים.',

        noAssetsTitle:
            'עדיין אין נכסים',
        noAssetsDescription:
            'הוסף את הנכס הראשון אל',
        addFirstAsset:
            'הוסף נכס ראשון',

        summaryAriaLabel:
            'סיכום נכסים',
        totalAssets:
            'סך הנכסים',
        inSelectedPortfolio:
            'בתיק ההשקעות שנבחר',
        assetTypes:
            'סוגי נכסים',
        distinctClassifications:
            'סיווגים שונים',
        currencies:
            'מטבעות',
        distinctTradingCurrencies:
            'מטבעות מסחר שונים',

        noMatchingAssets:
            'לא נמצאו נכסים מתאימים',
        noMatchingAssetsDescription:
            'שנה את החיפוש כדי להציג נכסים אחרים.',

        exchange:
            'בורסה',
        notSpecified:
            'לא צוין',
        isin:
            'ISIN',
        created:
            'נוצר',
        updated:
            'עודכן',

        edit:
            'עריכה',
        delete:
            'מחיקה',

        assetAction:
            'פעולה בנכס',
        createDialogTitle:
            'הוספת נכס',
        editDialogTitle:
            'עריכת נכס',
        deleteDialogTitle:
            'מחיקת נכס',
        closeDialog:
            'סגור חלון',

        liveMarketSearch:
            'חיפוש שוק בזמן אמת',
        findListedInstrument:
            'חיפוש נייר ערך נסחר',
        marketSearchDescription:
            (
                'חפש לפי סימול או שם חברה. ' +
                'בחירת תוצאה תמלא אוטומטית את פרטי הנכס.'
            ),
        clearSelection:
            'נקה בחירה',

        symbolOrCompanyName:
            'סימול או שם חברה',
        marketSearchPlaceholder:
            'TTWO, AAPL, NVIDIA...',
        searchingMarket:
            'מחפש בשוק',
        marketSearchResults:
            'תוצאות חיפוש שוק',
        unknownExchange:
            'בורסה לא ידועה',

        selectedInstrument:
            'נייר הערך שנבחר',
        country:
            'מדינה',
        instrumentType:
            'סוג מכשיר',
        latestPrice:
            'מחיר אחרון',
        loading:
            'טוען...',
        unavailable:
            'לא זמין',

        manualMarketNote:
            (
                'החיפוש בשוק אינו חובה. כל השדות למטה ' +
                'ניתנים לעריכה ולהזנה ידנית.'
            ),

        symbol:
            'סימול',
        assetType:
            'סוג נכס',
        assetTypeStock:
            'מניה',
        assetTypeEtf:
            'קרן סל',
        assetTypeBond:
            'אג"ח',
        assetTypeFund:
            'קרן',
        assetTypeCrypto:
            'מטבע קריפטוגרפי',
        assetTypeCommodity:
            'סחורה',
        assetTypeForex:
            'מט"ח',
        assetTypeCash:
            'מזומן',
        assetTypeOther:
            'אחר',
        displayName:
            'שם לתצוגה',
        currency:
            'מטבע',
        notes:
            'הערות',

        cancel:
            'ביטול',
        adding:
            'מוסיף...',
        saving:
            'שומר...',
        deleting:
            'מוחק...',
        saveAsset:
            'שמירת נכס',

        deletePrompt:
            'אתה עומד למחוק את',
        deleteTransactionsNotice:
            'עסקאות קיימות עדיין עשויות להפנות לנכס זה.',

        selectPortfolioBeforeCreate:
            'יש לבחור תיק לפני יצירת נכס.',

        createdSuccess:
            'הנכס נוצר בהצלחה.',
        updatedSuccess:
            'הנכס עודכן בהצלחה.',
        deletedSuccess:
            'הנכס נמחק בהצלחה.',

        enterAtLeastTwoCharacters:
            'יש להזין לפחות 2 תווים.',
        noMarketMatches:
            'לא נמצאו מכשירים פיננסיים מתאימים.',

        symbolRequired:
            'יש להזין סימול לנכס.',
        symbolTooLong:
            'הסימול אינו יכול להכיל יותר מ־30 תווים.',
        displayNameRequired:
            'יש להזין שם תצוגה לנכס.',
        displayNameTooLong:
            'שם התצוגה אינו יכול להכיל יותר מ־160 תווים.',
        currencyInvalid:
            'המטבע חייב להכיל בדיוק 3 אותיות.',
        isinInvalid:
            'ISIN חייב להכיל בדיוק 12 אותיות או ספרות.',
        exchangeTooLong:
            'שם הבורסה אינו יכול להכיל יותר מ־40 תווים.',
        notesTooLong:
            'ההערות אינן יכולות להכיל יותר מ־2000 תווים.',

        marketSearchError:
            (
                'לא ניתן לבצע חיפוש שוק כרגע. ' +
                'ניתן להמשיך בהזנה ידנית.'
            ),
        marketServiceUnavailable:
            (
                'לא ניתן להתחבר לשירות נתוני השוק. ' +
                'ניתן להמשיך בהזנה ידנית.'
            ),
        sessionExpired:
            (
                'פג תוקף ההתחברות שלך. ' +
                'יש להתחבר מחדש.'
            ),
        marketRateLimit:
            (
                'הגעת למגבלת הבקשות לשירות נתוני השוק. ' +
                'המתן מעט או המשך בהזנה ידנית.'
            ),
        marketNotConfigured:
            (
                'שירות נתוני השוק אינו מוגדר. ' +
                'ניתן להמשיך בהזנה ידנית.'
            ),

        marketPriceError:
            (
                'לא ניתן לטעון את המחיר האחרון. ' +
                'פרטי הנכס עדיין מולאו.'
            ),
        marketPriceNotFound:
            (
                'לא התקבל מחיר נוכחי עבור נייר ערך זה. ' +
                'פרטי הנכס עדיין מולאו.'
            ),
        marketPriceRateLimit:
            (
                'הגעת למגבלת הבקשות לשירות נתוני השוק. ' +
                'פרטי הנכס עדיין מולאו.'
            ),

        portfolioLoadError:
            'לא ניתן לטעון את תיקי ההשקעות. נסה שוב.',
        serverUnavailable:
            (
                'לא ניתן להתחבר לשרת. ' +
                'ודא שה־Backend פועל.'
            ),
        forbiddenViewPortfolios:
            'אין לך הרשאה לצפות בתיקי ההשקעות.',

        assetsLoadError:
            'לא ניתן לטעון את הנכסים. נסה שוב.',
        forbiddenViewAssets:
            'אין לך הרשאה לצפות בנכסים.',
        selectedPortfolioNotFound:
            'תיק ההשקעות שנבחר לא נמצא.',

        forbiddenAction:
            'אין לך הרשאה לבצע פעולה זו.',
        assetOrPortfolioNotFound:
            'הנכס או תיק ההשקעות לא נמצאו.',
        duplicateSymbol:
            'כבר קיים בתיק נכס עם סימול זה.',
        invalidAsset:
            'פרטי הנכס שהוזנו אינם תקינים.',

        createError:
            'לא ניתן ליצור את הנכס.',
        editError:
            'לא ניתן לעדכן את הנכס.',
        deleteError:
            'לא ניתן למחוק את הנכס.',
    },

    transactions: {
        eyebrow: 'ניהול עסקאות',
        title: 'עסקאות',
        description: 'רישום קניות, מכירות, דיבידנדים, הפקדות, משיכות ועמלות בתיק ההשקעות.',
        addTransaction: 'הוספת עסקה',
        dismissMessage: 'סגור הודעה',
        loadingPortfoliosTitle: 'טוען תיקי השקעות',
        loadingPortfoliosDescription: 'רשימת תיקי ההשקעות נטענת.',
        unavailableTitle: 'לא ניתן לטעון את העסקאות',
        retry: 'נסה שוב',
        noPortfoliosTitle: 'אין תיקי השקעות זמינים',
        noPortfoliosDescription: 'יש ליצור תיק השקעות לפני רישום עסקאות.',
        portfolio: 'תיק השקעות',
        asset: 'נכס',
        type: 'סוג',
        allAssets: 'כל הנכסים',
        allTypes: 'כל הסוגים',
        fromDate: 'מתאריך',
        toDate: 'עד תאריך',
        clear: 'נקה',
        applyFilters: 'החל סינון',
        startDateAfterEndDate: 'תאריך ההתחלה אינו יכול להיות אחרי תאריך הסיום.',
        loadingTransactionsTitle: 'טוען עסקאות',
        loadingTransactionsDescription: 'העסקאות בתיק שנבחר נטענות.',
        noTransactionsTitle: 'לא נמצאו עסקאות',
        noTransactionsDescriptionPrefix: 'רשום את העסקה הראשונה עבור',
        noTransactionsDescriptionSuffix: 'או שנה את מסנני החיפוש הפעילים.',
        addFirstTransaction: 'הוסף עסקה ראשונה',
        summaryAriaLabel: 'סיכום עסקאות',
        totalTransactions: 'סך העסקאות',
        matchingActiveFilters: 'בהתאם למסננים הפעילים',
        displayedAmount: 'סכום מוצג',
        totalOnCurrentPage: 'סך הסכומים בעמוד הנוכחי',
        displayedTypes: 'סוגים מוצגים',
        distinctTransactionTypes: 'סוגי עסקאות שונים',
        executed: 'בוצע',
        quantity: 'כמות',
        unitPrice: 'מחיר ליחידה',
        fee: 'עמלה',
        notApplicable: 'לא רלוונטי',
        notSpecified: 'לא צוין',
        noAsset: 'ללא נכס',
        unknownAsset: 'נכס לא ידוע',
        edit: 'עריכה',
        delete: 'מחיקה',
        transactionPages: 'עמודי עסקאות',
        previous: 'הקודם',
        page: 'עמוד',
        of: 'מתוך',
        next: 'הבא',
        transactionAction: 'פעולה בעסקה',
        createDialogTitle: 'הוספת עסקה',
        editDialogTitle: 'עריכת עסקה',
        deleteDialogTitle: 'מחיקת עסקה',
        closeDialog: 'סגור חלון',
        transactionType: 'סוג עסקה',
        selectAsset: 'בחר נכס',
        loadingLiveMarketPrice: 'טוען מחיר שוק בזמן אמת...',
        liveMarketPriceLoaded: 'מחיר השוק בזמן אמת נטען',
        manualExecutionPrice: 'מחיר ביצוע ידני',
        manualExecutionPriceDescription: 'השתמש באפשרות זו לעסקאות היסטוריות או כאשר מחיר הביצוע ידוע.',
        selectAssetToLoadPrice: 'בחר נכס כדי לטעון את מחירו',
        manualPrice: 'מחיר ידני',
        refreshPrice: 'רענן מחיר',
        totalAmount: 'סכום כולל',
        calculationMode: 'שיטת חישוב',
        calculateByQuantity: 'לפי כמות',
        calculateByAmount: 'לפי סכום',
        amountModeDescription:
            'הזן את הסכום הכולל ו־ShareCutter יחשב את כמות המניות, כולל שברים, לפי המחיר הנוכחי או המחיר הידני.',
        quantityModeDescription:
            'הזן את הכמות ו־ShareCutter יחשב את הסכום הכולל לפי הכמות, המחיר ליחידה והעמלה.',
        currency: 'מטבע',
        executionTime: 'מועד ביצוע',
        notes: 'הערות',
        cancel: 'ביטול',
        adding: 'מוסיף...',
        saving: 'שומר...',
        deleting: 'מוחק...',
        saveTransaction: 'שמירת עסקה',
        deleteTransaction: 'מחיקת עסקה',
        deletePromptPrefix: 'אתה עומד למחוק עסקת',
        deletePromptMiddle: 'מתיק ההשקעות',
        deleteCalculationNotice: 'העסקה לא תיכלל עוד בחישובי תיק ההשקעות.',
        typeBuy: 'קנייה',
        typeSell: 'מכירה',
        typeDividend: 'דיבידנד',
        typeDeposit: 'הפקדה',
        typeWithdrawal: 'משיכה',
        typeFee: 'עמלה',
        createdSuccess: 'העסקה נוצרה בהצלחה.',
        updatedSuccess: 'העסקה עודכנה בהצלחה.',
        deletedSuccess: 'העסקה נמחקה בהצלחה.',
        selectAssetBeforeMarketPrice: 'יש לבחור נכס לפני טעינת מחיר השוק שלו.',
        marketPriceError: 'לא ניתן לטעון את מחיר השוק הנוכחי. יש להזין את מחיר הביצוע ידנית.',
        marketServiceUnavailable: 'לא ניתן להתחבר לשירות נתוני השוק. יש להזין את מחיר הביצוע ידנית.',
        sessionExpired: 'פג תוקף ההתחברות שלך. יש להתחבר מחדש.',
        marketPriceForbidden: 'אין לך הרשאה לקבל מחירי שוק.',
        marketPriceNotFound: 'לא נמצא מחיר שוק נוכחי עבור נכס זה. יש להזין את מחיר הביצוע ידנית.',
        selectAssetRequired: 'יש לבחור נכס עבור סוג עסקה זה.',
        quantityInvalid: 'הכמות חייבת להיות גדולה מאפס.',
        unitPriceInvalid: 'המחיר ליחידה חייב להיות אפס או יותר.',
        feeInvalid: 'העמלה חייבת להיות אפס או יותר.',
        totalAmountInvalid: 'הסכום הכולל חייב להיות אפס או יותר.',
        currencyInvalid: 'המטבע חייב להכיל בדיוק 3 אותיות.',
        executionTimeRequired: 'יש להזין תאריך ושעת ביצוע.',
        executionTimeInvalid: 'תאריך או שעת הביצוע אינם תקינים.',
        notesTooLong: 'ההערות אינן יכולות להכיל יותר מ־2000 תווים.',
        portfolioLoadError: 'לא ניתן לטעון את תיקי ההשקעות. נסה שוב.',
        serverUnavailable: 'לא ניתן להתחבר לשרת. ודא שה־Backend פועל.',
        forbiddenViewPortfolios: 'אין לך הרשאה לצפות בתיקי ההשקעות.',
        transactionsLoadError: 'לא ניתן לטעון את העסקאות. נסה שוב.',
        forbiddenViewTransactions: 'אין לך הרשאה לצפות בעסקאות.',
        selectedPortfolioNotFound: 'תיק ההשקעות שנבחר לא נמצא.',
        invalidFilters: 'מסנני העסקאות אינם תקינים.',
        forbiddenAction: 'אין לך הרשאה לבצע פעולה זו.',
        transactionResourceNotFound: 'תיק ההשקעות, העסקה או הנכס לא נמצאו.',
        transactionConflict: 'העסקה מתנגשת עם נתונים קיימים.',
        invalidTransaction: 'פרטי העסקה שהוזנו אינם תקינים.',
        createError: 'לא ניתן ליצור את העסקה.',
        editError: 'לא ניתן לעדכן את העסקה.',
        deleteError: 'לא ניתן למחוק את העסקה.',
    },


    allocationPurchase: {
        eyebrow:
            'הקצאת תיק השקעות',
        title:
            'רכישה לפי הקצאה',
        description:
            (
                'בחר תיק השקעות ונכס, וחשב את הרכישה בשברי מניה ' +
                'הנדרשת כדי להגיע למשקל היעד בתיק.'
            ),
        reset:
            'איפוס',
        dismissSuccessMessage:
            'סגור הודעת הצלחה',

        loadingPortfoliosTitle:
            'טוען תיקי השקעות',
        loadingPortfoliosDescription:
            'נטענים תיקים המתאימים להקצאה לפי אחוזים.',
        unavailableTitle:
            'ההקצאה אינה זמינה',
        retry:
            'נסה שוב',
        noEligiblePortfoliosTitle:
            'אין תיקים מתאימים',
        noEligiblePortfoliosDescription:
            (
                'רכישה לפי הקצאה זמינה רק לתיקים ' +
                'שנוצרו לפי סכום השקעה.'
            ),

        purchaseSetup:
            'הגדרת רכישה',
        setTargetAllocation:
            'הגדרת הקצאת היעד',
        portfolio:
            'תיק השקעות',
        asset:
            'נכס',
        loadingAssets:
            'טוען נכסים...',
        noUsdAssetsAvailable:
            'אין נכסים זמינים בדולר',
        targetWeight:
            'משקל יעד (%)',
        purchaseFee:
            'עמלת רכישה',
        portfolioValue:
            'שווי התיק',
        creationMethod:
            'שיטת יצירה',
        creationMethodByAmount:
            'לפי סכום השקעה',
        exchangeNotSpecified:
            'הבורסה לא צוינה',

        calculating:
            'מחשב...',
        calculatePreview:
            'חשב תצוגה מקדימה',
        calculatingAllocationTitle:
            'מחשב הקצאה',
        calculatingAllocationDescription:
            (
                'מחיר השוק העדכני משמש לחישוב ' +
                'כמות שברי המניה הנדרשת.'
            ),

        purchasePreview:
            'תצוגה מקדימה לרכישה',
        allocation:
            'הקצאה',
        actionBuy:
            'קנייה',
        actionSellRequired:
            'נדרשת מכירה',
        actionNone:
            'ביעד',

        currentMarketPrice:
            'מחיר שוק נוכחי',
        targetWeightLabel:
            'משקל יעד',
        targetMarketValue:
            'שווי שוק יעד',
        existingQuantity:
            'כמות קיימת',
        targetQuantity:
            'כמות יעד',
        quantityToBuy:
            'כמות לרכישה',
        estimatedPurchase:
            'רכישה משוערת',
        remainingAssignableWeight:
            'משקל זמין להקצאה',
        calculated:
            'חושב',
        recalculate:
            'חשב מחדש',
        executing:
            'מבצע...',
        executePurchase:
            'בצע רכישה',
        sellRequiredMessage:
            (
                'משקל היעד שנבחר נמוך מהאחזקה הנוכחית. ' +
                'נדרשת פעולת מכירה במקום רכישה.'
            ),
        alreadyAtTargetMessage:
            'הנכס כבר תואם להקצאת היעד שנבחרה.',

        purchaseCompleted:
            'הרכישה הושלמה',
        purchased:
            'נרכש',
        complete:
            'הושלם',
        purchasedQuantity:
            'כמות שנרכשה',
        unitPrice:
            'מחיר ליחידה',
        purchaseAmount:
            'סכום רכישה',
        fee:
            'עמלה',
        totalCashUsed:
            'סך המזומן שנוצל',
        availableCashAfter:
            'מזומן זמין לאחר הרכישה',
        transactionId:
            'מזהה עסקה',
        executed:
            'בוצע',
        startAnotherAllocation:
            'התחל הקצאה נוספת',

        noPreviewTitle:
            'טרם חושבה תצוגה מקדימה',
        noPreviewDescription:
            (
                'בחר תיק ונכס, הזן משקל יעד וחשב את הרכישה ' +
                'הצפויה לפני הביצוע.'
            ),

        previewError:
            'לא ניתן לחשב את תצוגת ההקצאה המקדימה.',
        previewRequired:
            'יש לחשב תצוגה מקדימה לפני ביצוע הרכישה.',
        noAdditionalPurchaseRequired:
            'הקצאה זו אינה דורשת רכישה נוספת.',
        executeError:
            'לא ניתן לבצע את הרכישה לפי ההקצאה.',
        portfoliosLoadError:
            'לא ניתן לטעון את תיקי ההשקעות.',
        assetsLoadError:
            'לא ניתן לטעון את הנכסים בתיק שנבחר.',
        selectPortfolio:
            'יש לבחור תיק השקעות.',
        selectAsset:
            'יש לבחור נכס.',
        targetWeightRequired:
            'יש להזין אחוז משקל יעד.',
        targetWeightInvalid:
            'משקל היעד חייב להיות גדול מאפס ולא יותר מ־100.',
        feeInvalid:
            'העמלה אינה יכולה להיות שלילית.',

        serverUnavailable:
            'לא ניתן להתחבר לשרת. ודא שה־Backend פועל.',
        sessionExpired:
            'פג תוקף ההתחברות שלך. יש להתחבר מחדש.',
        forbiddenAction:
            'אין לך הרשאה לבצע פעולה זו.',
        resourceNotFound:
            'תיק ההשקעות או הנכס שנבחר לא נמצאו.',

        purchaseSuccessPrefix:
            'נרכשו',
        purchaseSuccessSuffix:
            'בהצלחה.',
    },

    auth: {
        login: {
            eyebrow:
                'חשבון ShareCutter',
            title:
                'ברוך שובך',
            description:
                (
                    'התחבר כדי לצפות בתיקי ההשקעות, יעדים שבועיים, ' +
                    'הקצאות נכסים והיסטוריית עסקאות.'
                ),
            benefitPerformance:
                'מעקב אחר ביצועי תיקי ההשקעות',
            benefitWeeklyTargets:
                'סקירת יעדי הקצאה שבועיים',
            benefitAssetsTransactions:
                'ניהול נכסים ועסקאות',

            cardTitle:
                'התחברות',
            cardDescription:
                'הזן את כתובת האימייל והסיסמה של החשבון שלך.',

            emailAddress:
                'כתובת אימייל',
            emailPlaceholder:
                'name@example.com',
            emailRequired:
                'יש להזין כתובת אימייל.',
            emailInvalid:
                'יש להזין כתובת אימייל תקינה.',

            password:
                'סיסמה',
            passwordHint:
                'לפחות 8 תווים',
            passwordPlaceholder:
                'הזן את הסיסמה שלך',
            passwordRequired:
                'יש להזין סיסמה.',
            passwordLength:
                'הסיסמה חייבת להכיל בין 8 ל־100 תווים.',

            signingIn:
                'מתחבר...',
            login:
                'התחברות',

            noAccount:
                'אין לך חשבון?',
            createOne:
                'יצירת חשבון',

            registrationSuccess:
                'החשבון נוצר בהצלחה. כעת ניתן להתחבר.',

            genericError:
                'אירעה שגיאה. נסה שוב.',
            serverUnavailable:
                (
                    'שרת ShareCutter אינו זמין. ' +
                    'ודא שה־Backend פועל.'
                ),
            invalidCredentials:
                'כתובת האימייל או הסיסמה שגויות.',
            forbidden:
                'חשבון זה אינו מורשה כרגע לגשת ל־ShareCutter.',
            loginFailed:
                'לא ניתן להתחבר. נסה שוב.',
        },

        register: {
            eyebrow:
                'מתחילים עם ShareCutter',
            title:
                'יצירת החשבון שלך',
            description:
                (
                    'צור תיקי השקעות, רשום עסקאות ונהל יעדי הקצאה ' +
                    'שבועיים מתוך סביבת עבודה אישית אחת.'
                ),
            benefitMultiplePortfolios:
                'יצירה וניהול של מספר תיקי השקעות',
            benefitAssetsTransactions:
                'מעקב אחר נכסים והיסטוריית עסקאות',
            benefitWeeklyTargets:
                'הגדרת יעדי תיק שבועיים',

            cardTitle:
                'יצירת חשבון',
            cardDescription:
                'הזן את פרטיך כדי ליצור חשבון ShareCutter.',

            firstName:
                'שם פרטי',
            firstNamePlaceholder:
                'שם פרטי',
            firstNameRequired:
                'יש להזין שם פרטי.',
            firstNameLength:
                'השם הפרטי אינו יכול להכיל יותר מ־100 תווים.',

            lastName:
                'שם משפחה',
            lastNamePlaceholder:
                'שם משפחה',
            lastNameRequired:
                'יש להזין שם משפחה.',
            lastNameLength:
                'שם המשפחה אינו יכול להכיל יותר מ־100 תווים.',

            emailAddress:
                'כתובת אימייל',
            emailPlaceholder:
                'name@example.com',
            emailRequired:
                'יש להזין כתובת אימייל.',
            emailInvalid:
                'יש להזין כתובת אימייל תקינה.',
            emailLength:
                'כתובת האימייל אינה יכולה להכיל יותר מ־320 תווים.',

            password:
                'סיסמה',
            passwordHint:
                '8 עד 72 תווים',
            passwordPlaceholder:
                'צור סיסמה',
            passwordRequired:
                'יש להזין סיסמה.',
            passwordLength:
                'הסיסמה חייבת להכיל בין 8 ל־72 תווים.',

            confirmPassword:
                'אימות סיסמה',
            confirmPasswordPlaceholder:
                'הזן שוב את הסיסמה',
            confirmPasswordRequired:
                'יש לאשר את הסיסמה.',
            passwordsMismatch:
                'הסיסמאות אינן תואמות.',

            creatingAccount:
                'יוצר חשבון...',
            createAccount:
                'יצירת חשבון',

            alreadyHaveAccount:
                'כבר יש לך חשבון?',
            login:
                'התחברות',

            genericError:
                'אירעה שגיאה. נסה שוב.',
            serverUnavailable:
                (
                    'שרת ShareCutter אינו זמין. ' +
                    'ודא שה־Backend פועל.'
                ),
            duplicateEmail:
                'כבר קיים חשבון עם כתובת אימייל זו.',
            createFailed:
                'לא ניתן ליצור את החשבון. נסה שוב.',
        },
    },
};

@Injectable({
    providedIn: 'root',
})
export class TranslationService {
    private readonly uiPreferences =
        inject(UiPreferencesService);

    readonly text =
        computed<ApplicationTranslations>(
            () =>
                this.uiPreferences.isHebrew()
                    ? HEBREW_TRANSLATIONS
                    : ENGLISH_TRANSLATIONS,
        );
}