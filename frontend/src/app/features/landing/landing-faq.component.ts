import { Component } from '@angular/core';

@Component({
  selector: 'app-landing-faq',
  template: `
    @for (question of questions; track question.title) {
      <details class="faq"><summary>{{ question.title }}<span aria-hidden="true">+</span></summary><p>{{ question.answer }}</p></details>
    }
  `,
  styleUrl: './landing-faq.component.css',
})
export class LandingFaqComponent {
  readonly questions = [
    { title: '¿El estudiante necesita una cuenta por cada institución?', answer: 'No. El estudiante tiene una cuenta global. Comparte su código para vincularse a distintas instituciones, y cada una administra sus propios registros y documentos.' },
    { title: '¿Qué necesito para registrarme como estudiante?', answer: 'Solo tu correo electrónico y una contraseña. Dentro de tu cuenta podrás completar tu perfil, consultar tu código de vinculación y gestionar tus autorizaciones.' },
    { title: '¿Qué documentos puede emitir una institución?', answer: 'Diplomas, certificados, títulos, reconocimientos y constancias. La institución configura sus plantillas y define qué personas de su equipo pueden revisar y emitir documentos.' },
    { title: '¿Cómo se verifica una credencial?', answer: 'El código o QR permite consultar el registro, su institución emisora y su estado. Al adjuntar el PDF también puedes comprobar su integridad y, cuando corresponde, su firma.' },
  ];
}
