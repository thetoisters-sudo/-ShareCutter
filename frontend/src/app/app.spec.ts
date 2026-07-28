import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { App } from './app';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('should create the application', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;

    expect(app).toBeTruthy();
  });

  it('should render the ShareCutter brand', () => {
    const fixture = TestBed.createComponent(App);

    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const brandName = compiled.querySelector('.brand__text strong');

    expect(brandName?.textContent?.trim()).toBe('ShareCutter');
  });

  it('should render the primary navigation links', () => {
    const fixture = TestBed.createComponent(App);

    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const navigationLinks = Array.from(
      compiled.querySelectorAll<HTMLAnchorElement>(
        '.main-navigation .main-navigation__link',
      ),
    ).map((link) => link.textContent?.trim());

    expect(navigationLinks).toEqual(['Dashboard', 'Portfolios']);
  });
});