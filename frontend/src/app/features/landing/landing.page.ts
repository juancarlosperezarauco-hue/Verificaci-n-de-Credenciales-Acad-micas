import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { UiIconComponent } from '../../shared/ui-icon.component';
import { CredentialPreviewComponent } from './credential-preview.component';
import { LandingFaqComponent } from './landing-faq.component';

@Component({
  imports: [RouterLink, UiIconComponent, CredentialPreviewComponent, LandingFaqComponent],
  templateUrl: './landing.page.html',
  styleUrl: './landing.page.css',
})
export class LandingPage {
  readonly audiences = [
    {
      icon: 'building', label: 'Para instituciones',
      title: 'Tu institución, con su propia identidad.',
      description: 'Organiza a tu comunidad académica y gestiona cada reconocimiento con el logo, los formatos y el equipo de tu institución.',
      benefits: ['Estudiantes vinculados por código', 'Plantillas, revisión y emisión de documentos', 'Usuarios y permisos por institución'],
      path: '/register', action: 'Registrar institución',
      note: 'El registro institucional pasa por revisión de la plataforma.',
    },
    {
      icon: 'academic', label: 'Para estudiantes',
      title: 'Una cuenta para toda tu formación.',
      description: 'Reúne tus reconocimientos en un expediente personal que te acompaña al pasar por un colegio, instituto o universidad.',
      benefits: ['Un perfil para distintas instituciones', 'Certificados disponibles en tu expediente', 'Control de tus vínculos institucionales'],
      path: '/student/register', action: 'Crear cuenta de estudiante',
      note: 'Empieza con tu correo y contraseña. Completa tu perfil después.',
    },
  ];
  readonly steps = [
    { icon: 'building', title: 'Registra tu institución', text: 'Completa su información y envíala a revisión para habilitar su espacio.' },
    { icon: 'users', title: 'Vincula al estudiante', text: 'Usa su código personal para incorporarlo a tu comunidad académica.' },
    { icon: 'document', title: 'Prepara y emite', text: 'Revisa la solicitud y emite el diploma o certificado correspondiente.' },
    { icon: 'shield', title: 'Consulta la credencial', text: 'Comparte su código o QR para consultar el registro y su estado.' },
  ];
  readonly verification = [
    { icon: 'building', title: 'Institución emisora', text: 'La organización responsable del reconocimiento.' },
    { icon: 'user', title: 'Titular y reconocimiento', text: 'La persona y el documento que se está consultando.' },
    { icon: 'refresh', title: 'Estado de la credencial', text: 'Su estado actual, incluida una posible revocación.' },
    { icon: 'shield', title: 'Integridad del documento', text: 'Compara el PDF con el archivo registrado y revisa su firma.' },
  ];
}
