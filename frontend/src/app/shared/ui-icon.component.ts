import { Component, Input } from '@angular/core';

/** Small, consistent outline icons; labels remain visible beside each action. */
@Component({
  selector: 'app-ui-icon',
  template: `<svg
    [attr.width]="size"
    [attr.height]="size"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="1.7"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
  >
    <path [attr.d]="paths[name] || paths['document']" />
  </svg>`,
  styles:
    ':host{display:inline-flex;align-items:center;justify-content:center;flex-shrink:0;vertical-align:middle;}',
})
export class UiIconComponent {
  @Input() name = 'document';
  @Input() size = 20;
  readonly paths: Record<string, string> = {
    grid: 'M3 3h7v7H3zM14 3h7v7h-7zM3 14h7v7H3zM14 14h7v7h-7z',
    users:
      'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75',
    academic: 'M2 9l10-5 10 5-10 5zM6 11v6c4 3 8 3 12 0v-6M22 9v7',
    document: 'M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8zM14 2v6h6M8 13h8M8 17h5',
    inbox: 'M4 4h16l2 12v4H2v-4zM2 16h6l2 3h4l2-3h6',
    shield: 'M12 22s8-4 8-11V5l-8-3-8 3v6c0 7 8 11 8 11M8 12l3 3 5-6',
    building: 'M3 22h18M6 22V3h12v19M9 7h1M14 7h1M9 11h1M14 11h1M9 15h1M14 15h1M10 22v-3h4v3',
    settings:
      'M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8M12 2v3M12 19v3M2 12h3M19 12h3M5 5l2 2M17 17l2 2M5 19l2-2M17 7l2-2',
    palette:
      'M12 3a9 9 0 1 0 0 18h1a2 2 0 0 0 1-3.7c-1-.6-.6-2.3.8-2.3H18a3 3 0 0 0 3-3 9 9 0 0 0-9-9M7 10h.01M10 7h.01M15 7h.01M17 11h.01',
    key: 'M15 3a6 6 0 1 1-4.8 9.6L3 20H1v-3l2-2h2v-2l3.2-3.2A6 6 0 0 1 15 3M17 7h.01',
    search: 'M10.5 3a7.5 7.5 0 1 0 0 15 7.5 7.5 0 0 0 0-15M16 16l5 5',
    check: 'M5 12l4 4L19 6',
    clock: 'M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18M12 7v5l3 2',
    arrow: 'M5 12h14M13 6l6 6-6 6',
    plus: 'M12 5v14M5 12h14',
    refresh: 'M20 7a9 9 0 0 0-15-1L2 9M2 3v6h6M4 17a9 9 0 0 0 15 1l3-3M22 21v-6h-6',
    mail: 'M3 5h18v14H3zM3 6l9 7 9-7',
    folder: 'M3 5h6l2 3h10v12H3z',
    user: 'M20 21v-2a6 6 0 0 0-6-6h-4a6 6 0 0 0-6 6v2M12 3a4 4 0 1 0 0 8 4 4 0 0 0 0-8',
    chart: 'M4 3v18h17M8 16v-5M13 16V7M18 16v-8',
    logout: 'M9 5H4v14h5M14 5l7 7-7 7M8 12h13',
  };
}
