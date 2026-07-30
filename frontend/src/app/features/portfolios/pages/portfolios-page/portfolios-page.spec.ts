import {
    HttpErrorResponse,
} from '@angular/common/http';
import {
    ComponentFixture,
    TestBed,
} from '@angular/core/testing';
import {
    By,
} from '@angular/platform-browser';
import {
    Observable,
    of,
    throwError,
} from 'rxjs';
import {
    vi,
} from 'vitest';

import {
    PagedResponse,
    PortfolioCreateRequest,
    PortfolioRenameRequest,
    PortfolioResponse,
    PortfolioValueUpdateRequest,
} from '../../../../core/portfolio/models/portfolio.models';
import {
    PortfolioService,
} from '../../../../core/portfolio/services/portfolio.service';
import {
    PortfoliosPage,
} from './portfolios-page';

class PortfolioServiceStub {
    response: PagedResponse<PortfolioResponse> =
        createPagedResponse();

    getPortfolios = vi.fn(
        (): Observable<PagedResponse<PortfolioResponse>> =>
            of(this.response),
    );

    createPortfolio = vi.fn(
        (
            _request: PortfolioCreateRequest,
        ): Observable<PortfolioResponse> =>
            of(createPortfolio()),
    );

    renamePortfolio = vi.fn(
        (
            _portfolioId: string,
            _request: PortfolioRenameRequest,
        ): Observable<PortfolioResponse> =>
            of(createPortfolio()),
    );

    updatePortfolioValue = vi.fn(
        (
            _portfolioId: string,
            _request: PortfolioValueUpdateRequest,
        ): Observable<PortfolioResponse> =>
            of(createPortfolio()),
    );

    deletePortfolio = vi.fn(
        (
            _portfolioId: string,
        ): Observable<void> =>
            of(undefined),
    );
}

describe('PortfoliosPage', () => {
    let fixture: ComponentFixture<PortfoliosPage>;
    let component: PortfoliosPage;
    let portfolioService: PortfolioServiceStub;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [
                PortfoliosPage,
            ],
            providers: [
                {
                    provide: PortfolioService,
                    useClass: PortfolioServiceStub,
                },
            ],
        }).compileComponents();

        fixture = TestBed.createComponent(
            PortfoliosPage,
        );

        component = fixture.componentInstance;

        portfolioService = TestBed.inject(
            PortfolioService,
        ) as unknown as PortfolioServiceStub;
    });

    it('should create', () => {
        fixture.detectChanges();

        expect(component).toBeTruthy();
    });

    it('should load and display portfolios', () => {
        fixture.detectChanges();

        const cards = fixture.debugElement.queryAll(
            By.css('.portfolio-card'),
        );

        expect(
            portfolioService.getPortfolios,
        ).toHaveBeenCalledWith({
            page: 0,
            size: 6,
            sortBy: 'createdAt',
            sortDirection: 'desc',
        });

        expect(cards).toHaveLength(2);

        expect(
            fixture.nativeElement.textContent,
        ).toContain('Growth Portfolio');

        expect(
            fixture.nativeElement.textContent,
        ).toContain('Income Portfolio');
    });

    it('should display the empty state', () => {
        portfolioService.response = {
            ...createPagedResponse(),
            content: [],
            totalElements: 0,
            totalPages: 0,
            first: true,
            last: true,
        };

        fixture.detectChanges();

        expect(
            fixture.debugElement.query(
                By.css('.page-state'),
            ),
        ).not.toBeNull();

        expect(
            fixture.nativeElement.textContent,
        ).toContain('No portfolios yet');
    });

    it('should display an error state', () => {
        portfolioService.getPortfolios.mockReturnValueOnce(
            throwError(
                () => new Error('Request failed'),
            ),
        );

        fixture.detectChanges();

        expect(
            fixture.nativeElement.textContent,
        ).toContain('Portfolios unavailable');
    });

    it('should stop loading after a list error', () => {
        portfolioService.getPortfolios.mockReturnValueOnce(
            throwError(
                () => new HttpErrorResponse({
                    status: 500,
                    statusText: 'Server Error',
                }),
            ),
        );

        fixture.detectChanges();

        const state = getComponentState(component);

        expect(state.isLoading).toBe(false);

        expect(state.errorMessage).toBe(
            'Portfolios could not be loaded. Please try again.',
        );
    });

    it('should request the next page', () => {
        portfolioService.response = {
            ...createPagedResponse(),
            first: true,
            last: false,
            totalPages: 2,
        };

        fixture.detectChanges();

        const nextButton =
            fixture.debugElement.queryAll(
                By.css('.pagination button'),
            )[1];

        nextButton.triggerEventHandler(
            'click',
        );

        expect(
            portfolioService.getPortfolios,
        ).toHaveBeenLastCalledWith({
            page: 1,
            size: 6,
            sortBy: 'createdAt',
            sortDirection: 'desc',
        });
    });

    it('should create a portfolio', () => {
        fixture.detectChanges();

        openCreateDialog(component);

        setCreateValues(
            component,
            'New Portfolio',
            5000,
        );

        submitCreate(component);

        expect(
            portfolioService.createPortfolio,
        ).toHaveBeenCalledWith({
            name: 'New Portfolio',
            creationMethod: 'BY_AMOUNT',
            initialValue: 5000,
        });
    });

    it('should stop submitting after a 409 create error', () => {
        portfolioService.createPortfolio.mockReturnValueOnce(
            throwError(
                () => new HttpErrorResponse({
                    status: 409,
                    statusText: 'Conflict',
                    error: {
                        message:
                            'Portfolio name already exists.',
                    },
                }),
            ),
        );

        fixture.detectChanges();

        openCreateDialog(component);

        setCreateValues(
            component,
            'Growth Portfolio',
            5000,
        );

        submitCreate(component);

        const state = getComponentState(component);

        expect(state.isSubmitting).toBe(false);
        expect(state.dialogMode).toBe('create');

        expect(state.actionErrorMessage).toBe(
            'A portfolio with this name already exists.',
        );
    });

    it('should display the duplicate-name message in the create dialog', () => {
        const state = getComponentState(component);

        state.dialogMode = 'create';
        state.actionErrorMessage =
            'A portfolio with this name already exists.';
        state.isSubmitting = false;

        fixture.detectChanges();

        expect(
            fixture.nativeElement.textContent,
        ).toContain(
            'A portfolio with this name already exists.',
        );

        const submitButton =
            fixture.debugElement.query(
                By.css(
                    '.portfolio-form button[type="submit"]',
                ),
            );

        expect(submitButton).not.toBeNull();

        expect(
            submitButton.nativeElement.textContent.trim(),
        ).toBe('Create portfolio');

        expect(
            submitButton.nativeElement.disabled,
        ).toBe(false);
    });

    it('should stop submitting after an unknown create error', () => {
        portfolioService.createPortfolio.mockReturnValueOnce(
            throwError(
                () => new Error('Request failed'),
            ),
        );

        fixture.detectChanges();

        openCreateDialog(component);

        setCreateValues(
            component,
            'New Portfolio',
            5000,
        );

        submitCreate(component);

        const state = getComponentState(component);

        expect(state.isSubmitting).toBe(false);

        expect(state.actionErrorMessage).toBe(
            'The portfolio could not be created.',
        );
    });

    it('should rename a portfolio', () => {
        fixture.detectChanges();

        const portfolio =
            createPagedResponse().content[0];

        openRenameDialog(
            component,
            portfolio,
        );

        setRenameValue(
            component,
            'Renamed Portfolio',
        );

        submitRename(component);

        expect(
            portfolioService.renamePortfolio,
        ).toHaveBeenCalledWith(
            portfolio.id,
            {
                name: 'Renamed Portfolio',
            },
        );
    });

    it('should stop submitting after a rename conflict', () => {
        portfolioService.renamePortfolio.mockReturnValueOnce(
            throwError(
                () => new HttpErrorResponse({
                    status: 409,
                    statusText: 'Conflict',
                }),
            ),
        );

        fixture.detectChanges();

        const portfolio =
            createPagedResponse().content[0];

        openRenameDialog(
            component,
            portfolio,
        );

        setRenameValue(
            component,
            'Income Portfolio',
        );

        submitRename(component);

        const state = getComponentState(component);

        expect(state.isSubmitting).toBe(false);

        expect(state.actionErrorMessage).toBe(
            'A portfolio with this name already exists.',
        );
    });

    it('should update a portfolio value', () => {
        fixture.detectChanges();

        const portfolio =
            createPagedResponse().content[0];

        openValueDialog(
            component,
            portfolio,
        );

        setCurrentValue(
            component,
            12500,
        );

        submitValueUpdate(component);

        expect(
            portfolioService.updatePortfolioValue,
        ).toHaveBeenCalledWith(
            portfolio.id,
            {
                currentValue: 12500,
            },
        );
    });

    it('should delete a portfolio', () => {
        fixture.detectChanges();

        const portfolio =
            createPagedResponse().content[0];

        openDeleteDialog(
            component,
            portfolio,
        );

        submitDelete(component);

        expect(
            portfolioService.deletePortfolio,
        ).toHaveBeenCalledWith(
            portfolio.id,
        );
    });
});

interface PortfoliosPageTestState {
    isLoading: boolean;
    isSubmitting: boolean;
    errorMessage: string;
    actionErrorMessage: string;
    dialogMode:
    | 'create'
    | 'rename'
    | 'value'
    | 'delete'
    | null;
    createName: string;
    createInitialValue: number | null;
    renameValue: string;
    currentValue: number | null;
    openCreateDialog(): void;
    submitCreate(): void;
    openRenameDialog(
        portfolio: PortfolioResponse,
    ): void;
    submitRename(): void;
    openValueDialog(
        portfolio: PortfolioResponse,
    ): void;
    submitValueUpdate(): void;
    openDeleteDialog(
        portfolio: PortfolioResponse,
    ): void;
    submitDelete(): void;
}

function getComponentState(
    component: PortfoliosPage,
): PortfoliosPageTestState {
    return component as unknown as PortfoliosPageTestState;
}

function openCreateDialog(
    component: PortfoliosPage,
): void {
    getComponentState(component).openCreateDialog();
}

function setCreateValues(
    component: PortfoliosPage,
    name: string,
    initialValue: number,
): void {
    const state = getComponentState(component);

    state.createName = name;
    state.createInitialValue = initialValue;
}

function submitCreate(
    component: PortfoliosPage,
): void {
    getComponentState(component).submitCreate();
}

function openRenameDialog(
    component: PortfoliosPage,
    portfolio: PortfolioResponse,
): void {
    getComponentState(component)
        .openRenameDialog(portfolio);
}

function setRenameValue(
    component: PortfoliosPage,
    name: string,
): void {
    getComponentState(component).renameValue = name;
}

function submitRename(
    component: PortfoliosPage,
): void {
    getComponentState(component).submitRename();
}

function openValueDialog(
    component: PortfoliosPage,
    portfolio: PortfolioResponse,
): void {
    getComponentState(component)
        .openValueDialog(portfolio);
}

function setCurrentValue(
    component: PortfoliosPage,
    value: number,
): void {
    getComponentState(component).currentValue = value;
}

function submitValueUpdate(
    component: PortfoliosPage,
): void {
    getComponentState(component)
        .submitValueUpdate();
}

function openDeleteDialog(
    component: PortfoliosPage,
    portfolio: PortfolioResponse,
): void {
    getComponentState(component)
        .openDeleteDialog(portfolio);
}

function submitDelete(
    component: PortfoliosPage,
): void {
    getComponentState(component).submitDelete();
}

function createPagedResponse():
    PagedResponse<PortfolioResponse> {
    return {
        content: [
            createPortfolio(),
            {
                ...createPortfolio(),
                id: 'portfolio-2',
                name: 'Income Portfolio',
                initialValue: 8000,
                currentValue: 7600,
                totalReturnPercent: -5,
            },
        ],
        page: 0,
        size: 6,
        totalElements: 2,
        totalPages: 1,
        first: true,
        last: true,
        hasNext: false,
        hasPrevious: false,
    };
}

function createPortfolio(): PortfolioResponse {
    return {
        id: 'portfolio-1',
        userId: 'user-1',
        name: 'Growth Portfolio',
        creationMethod: 'BY_AMOUNT',
        initialValue: 10000,
        currentValue: 11500,
        totalRealizedProfit: 500,
        totalUnrealizedProfit: 1000,
        totalReturnPercent: 15,
        createdAt: '2026-07-20T10:00:00Z',
        updatedAt: '2026-07-29T10:00:00Z',
    };
}