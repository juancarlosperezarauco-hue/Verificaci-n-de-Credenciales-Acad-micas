import { Component } from '@angular/core';
import { UiIconComponent } from '../../shared/ui-icon.component';

@Component({
  selector: 'app-credential-preview',
  imports: [UiIconComponent],
  template: `
    <figure aria-label="Ejemplo ilustrativo de un certificado académico">
      <figcaption><span>Tu trayectoria, organizada</span><span class="example">Vista de ejemplo</span></figcaption>
      <div class="preview">
        <article class="certificate">
          <div class="issuer"><span class="seal"><app-ui-icon name="academic" [size]="25" /></span><span>Institución educativa<small>Documento académico</small></span></div>
          <div class="certificate-body">
            <p class="document-kind">PROGRAMA DE FORMACIÓN</p>
            <h2>Certificado de<br />participación</h2>
            <span class="fine-rule"></span>
            <p>Otorgado a</p><strong>Nombre del estudiante</strong>
            <p>Por su participación en un<br />programa de formación académica.</p>
          </div>
          <div class="certificate-footer">
            <div><span class="signature" aria-hidden="true"></span><span>Emisión institucional</span></div>
            <svg class="qr" viewBox="0 0 44 44" fill="currentColor" aria-hidden="true">
              <path fill-rule="evenodd" d="M1 1h14v14H1zm3 3v8h8V4zM29 1h14v14H29zm3 3v8h8V4zM1 29h14v14H1zm3 3v8h8v-8z"/>
              <path d="M6 6h4v4H6zM34 6h4v4h-4zM6 34h4v4H6zM19 1h4v7h-4zM19 12h7v4h-7zM1 19h8v4H1zM13 19h4v7h-4zM22 20h5v5h-5zM32 19h11v4H32zM19 29h4v14h-4zM27 28h8v4h-8zM28 36h4v7h-4zM38 27h5v8h-5zM36 39h7v4h-7z"/>
            </svg>
          </div>
        </article>
        <div class="verification"><span class="verification-icon"><app-ui-icon name="shield" [size]="21" /></span><div><strong>Verificación por código o QR</strong><p>Consulta el registro y la institución emisora.</p></div></div>
      </div>
    </figure>
  `,
  styleUrl: './credential-preview.component.css',
})
export class CredentialPreviewComponent {}
