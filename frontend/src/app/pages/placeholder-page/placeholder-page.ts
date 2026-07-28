import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
    selector: 'app-placeholder-page',
    templateUrl: './placeholder-page.html',
    styleUrl: './placeholder-page.scss',
})
export class PlaceholderPage {
    private readonly route = inject(ActivatedRoute);

    protected readonly title =
        this.route.snapshot.data['title'] ?? 'ShareCutter';

    protected readonly description =
        this.route.snapshot.data['description'] ??
        'This section is currently under development.';
}