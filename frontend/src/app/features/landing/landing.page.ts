import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  imports: [RouterLink],
  template: `
    <div class="overflow-hidden">
      <section class="relative isolate border-b border-white/10">
        <div class="landing-grid absolute inset-0 -z-10 opacity-35"></div>
        <div class="mx-auto grid max-w-7xl gap-14 px-6 py-20 lg:grid-cols-[1.08fr_.92fr] lg:items-center lg:py-28">
          <div>
            <p class="brand-text text-sm font-bold uppercase tracking-[.28em]">Confianza académica verificable</p>
            <h1 class="mt-6 max-w-4xl text-5xl font-black leading-[1.02] tracking-tight sm:text-6xl lg:text-7xl">
              Credenciales que acompañan al estudiante durante toda su
              <span class="brand-text">trayectoria.</span>
            </h1>
            <p class="mt-7 max-w-2xl text-lg leading-8 text-slate-300 sm:text-xl">
              Conectamos estudiantes e instituciones para emitir, organizar y comprobar diplomas,
              certificados y reconocimientos desde una sola plataforma.
            </p>
            <div class="mt-9 flex flex-wrap gap-3">
              <a routerLink="/verify" class="brand-bg rounded-xl px-6 py-3.5 font-bold text-slate-950 shadow-lg shadow-cyan-950/30">Verificar credencial</a>
              <a routerLink="/register" class="rounded-xl border border-white/15 bg-white/5 px-6 py-3.5 font-bold hover:bg-white/10">Registrar institución</a>
            </div>
            <div class="mt-10 flex flex-wrap gap-x-7 gap-y-3 text-sm text-slate-400">
              <span class="flex items-center gap-2"><i class="status-dot"></i>Identidad única del estudiante</span>
              <span class="flex items-center gap-2"><i class="status-dot"></i>Verificación pública por QR</span>
              <span class="flex items-center gap-2"><i class="status-dot"></i>Datos separados por institución</span>
            </div>
          </div>

          <div class="relative mx-auto w-full max-w-xl">
            <div class="brand-gradient absolute -inset-5 -z-10 rounded-[2.25rem] opacity-20 blur-3xl"></div>
            <div class="rounded-[2rem] border border-white/10 bg-slate-900/90 p-5 shadow-2xl backdrop-blur sm:p-7">
              <div class="flex items-center justify-between border-b border-white/10 pb-5">
                <div><p class="text-xs font-bold uppercase tracking-[.2em] text-slate-500">Trayectoria digital</p><p class="mt-1 font-bold">Expediente del estudiante</p></div>
                <span class="rounded-full border border-emerald-400/20 bg-emerald-400/10 px-3 py-1 text-xs font-bold text-emerald-300">Verificado</span>
              </div>
              <div class="mt-6 flex items-center gap-4">
                <div class="brand-bg grid size-14 shrink-0 place-items-center rounded-2xl text-lg font-black text-slate-950">EP</div>
                <div><p class="text-lg font-bold">Estudiante con identidad única</p><p class="mt-1 text-sm text-slate-400">Código personal para vinculación institucional</p></div>
              </div>
              <div class="relative mt-7 space-y-4 before:absolute before:bottom-5 before:left-[1.05rem] before:top-5 before:w-px before:bg-white/10">
                <div class="relative flex gap-4"><span class="timeline-dot bg-blue-400"></span><div class="min-w-0 flex-1 rounded-2xl border border-white/10 bg-slate-950/70 p-4"><div class="flex justify-between gap-3"><strong>Colegio</strong><span class="text-xs text-slate-500">2023</span></div><p class="mt-1 text-sm text-slate-400">Diploma de bachiller</p></div></div>
                <div class="relative flex gap-4"><span class="timeline-dot bg-violet-400"></span><div class="min-w-0 flex-1 rounded-2xl border border-white/10 bg-slate-950/70 p-4"><div class="flex justify-between gap-3"><strong>Universidad</strong><span class="text-xs text-slate-500">2026</span></div><p class="mt-1 text-sm text-slate-400">Certificado académico</p></div></div>
                <div class="relative flex gap-4"><span class="timeline-dot bg-emerald-400"></span><div class="min-w-0 flex-1 rounded-2xl border border-emerald-400/15 bg-emerald-400/5 p-4"><div class="flex justify-between gap-3"><strong>Consulta pública</strong><span class="text-xs text-emerald-300">QR</span></div><p class="mt-1 text-sm text-slate-400">Autenticidad comprobable en segundos</p></div></div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="mx-auto max-w-7xl px-6 py-20 sm:py-24">
        <div class="grid gap-10 lg:grid-cols-[.72fr_1.28fr] lg:gap-16">
          <div>
            <p class="brand-text text-sm font-bold uppercase tracking-[.22em]">Qué hacemos</p>
            <h2 class="mt-4 text-4xl font-black tracking-tight sm:text-5xl">Convertimos documentos aislados en una trayectoria confiable.</h2>
            <p class="mt-6 leading-7 text-slate-400">Un estudiante puede pasar por un colegio, un instituto y una universidad. Nuestra plataforma mantiene una sola identidad para la persona y permite que cada institución agregue únicamente los reconocimientos que le corresponden.</p>
          </div>
          <div class="grid gap-4 sm:grid-cols-3">
            <article class="feature-card"><span class="feature-number">01</span><h3>Conectamos</h3><p>La institución encuentra al estudiante mediante su código personal, sin crear perfiles duplicados.</p></article>
            <article class="feature-card"><span class="feature-number">02</span><h3>Emitimos</h3><p>Diplomas y certificados personalizados con datos institucionales, documento PDF y código QR.</p></article>
            <article class="feature-card"><span class="feature-number">03</span><h3>Verificamos</h3><p>Cualquier tercero puede consultar el estado y la institución emisora de una credencial.</p></article>
          </div>
        </div>
      </section>

      <section class="border-y border-white/10 bg-slate-900/45">
        <div class="mx-auto max-w-7xl px-6 py-20 sm:py-24">
          <div class="max-w-3xl">
            <p class="brand-text text-sm font-bold uppercase tracking-[.22em]">Lo que ofrecemos</p>
            <h2 class="mt-4 text-4xl font-black tracking-tight sm:text-5xl">Herramientas para todo el ciclo de la credencial.</h2>
            <p class="mt-5 text-lg leading-8 text-slate-400">Desde el registro del estudiante hasta la consulta pública, cada módulo cumple una función concreta.</p>
          </div>
          <div class="mt-12 grid gap-px overflow-hidden rounded-3xl border border-white/10 bg-white/10 md:grid-cols-2 lg:grid-cols-3">
            <article class="service-card"><span>ID</span><h3>Identidad estudiantil</h3><p>Cuenta personal y código de búsqueda para mantener un solo perfil entre diferentes organizaciones.</p></article>
            <article class="service-card"><span>IN</span><h3>Gestión institucional</h3><p>Registro de universidades, colegios e institutos con usuarios, permisos y estudiantes vinculados.</p></article>
            <article class="service-card"><span>CR</span><h3>Emisión de credenciales</h3><p>Creación individual o por lotes de diplomas, certificados y reconocimientos académicos.</p></article>
            <article class="service-card"><span>QR</span><h3>Comprobación pública</h3><p>Consulta mediante código o QR para conocer la validez, titular e institución emisora.</p></article>
            <article class="service-card"><span>BR</span><h3>Identidad visual</h3><p>Logo, colores, estilo, datos del firmante y documentos de respaldo propios de cada institución.</p></article>
            <article class="service-card"><span>PA</span><h3>Portal académico</h3><p>Avisos, fechas importantes, recursos y accesos rápidos administrados por cada organización.</p></article>
          </div>
        </div>
      </section>

      <section class="mx-auto max-w-7xl px-6 py-20 sm:py-24">
        <div class="grid gap-14 lg:grid-cols-2 lg:items-start">
          <div>
            <p class="brand-text text-sm font-bold uppercase tracking-[.22em]">Cómo funciona</p>
            <h2 class="mt-4 text-4xl font-black tracking-tight sm:text-5xl">Cuatro pasos, una misma historia académica.</h2>
          </div>
          <ol class="space-y-3">
            <li class="process-step"><span>1</span><div><h3>El estudiante crea su identidad</h3><p>Registra sus datos una sola vez y obtiene un código personal.</p></div></li>
            <li class="process-step"><span>2</span><div><h3>La institución lo vincula</h3><p>Busca el código, comprueba la información y lo incorpora a su organización.</p></div></li>
            <li class="process-step"><span>3</span><div><h3>Se emite el reconocimiento</h3><p>La institución selecciona al estudiante, define el tipo y genera el documento.</p></div></li>
            <li class="process-step"><span>4</span><div><h3>La credencial puede verificarse</h3><p>El QR y el código conducen a una consulta pública de autenticidad y estado.</p></div></li>
          </ol>
        </div>
      </section>

      <section class="mx-auto max-w-7xl px-6 pb-24">
        <div class="brand-gradient overflow-hidden rounded-[2rem] p-px">
          <div class="rounded-[calc(2rem-1px)] bg-slate-950 px-7 py-12 text-center sm:px-12 sm:py-16">
            <p class="text-sm font-bold uppercase tracking-[.25em] text-slate-400">Una red académica más transparente</p>
            <h2 class="mx-auto mt-4 max-w-3xl text-4xl font-black tracking-tight sm:text-5xl">Empieza verificando una credencial o registra tu institución.</h2>
            <p class="mx-auto mt-5 max-w-2xl text-lg text-slate-400">La plataforma está preparada para colegios, institutos, universidades y otras organizaciones educativas.</p>
            <div class="mt-8 flex flex-wrap justify-center gap-3">
              <a routerLink="/verify" class="brand-bg rounded-xl px-6 py-3.5 font-bold text-slate-950">Verificar ahora</a>
              <a routerLink="/student/register" class="rounded-xl border border-white/15 px-6 py-3.5 font-bold hover:bg-white/5">Crear cuenta de estudiante</a>
            </div>
          </div>
        </div>
      </section>
    </div>
  `,
  styles: `
    .landing-grid { background-image: linear-gradient(to right, #ffffff0b 1px, transparent 1px), linear-gradient(to bottom, #ffffff0b 1px, transparent 1px); background-size: 48px 48px; mask-image: linear-gradient(to bottom, black, transparent 92%); }
    .status-dot { width: .5rem; height: .5rem; border-radius: 999px; background: var(--brand-primary); box-shadow: 0 0 16px var(--brand-primary); }
    .timeline-dot { position: relative; z-index: 1; margin-top: 1.25rem; display: block; width: .55rem; height: .55rem; flex: 0 0 auto; border-radius: 999px; box-shadow: 0 0 0 6px #0f172a; }
    .feature-card { min-height: 18rem; border: 1px solid #ffffff12; border-radius: 1.5rem; background: linear-gradient(150deg, #0f172a, #020617); padding: 1.5rem; }
    .feature-card:nth-child(2) { transform: translateY(1.75rem); }
    .feature-card h3, .service-card h3, .process-step h3 { margin-top: 1rem; font-size: 1.1rem; font-weight: 800; }
    .feature-card p, .service-card p, .process-step p { margin-top: .65rem; color: #94a3b8; line-height: 1.65; font-size: .92rem; }
    .feature-number { color: var(--brand-primary); font-size: .75rem; font-weight: 900; letter-spacing: .2em; }
    .service-card { background: #020617; padding: 1.7rem; min-height: 14.5rem; }
    .service-card > span { display: grid; width: 2.75rem; height: 2.75rem; place-items: center; border: 1px solid color-mix(in srgb, var(--brand-primary) 35%, transparent); border-radius: .8rem; color: var(--brand-primary); background: color-mix(in srgb, var(--brand-primary) 8%, transparent); font-size: .75rem; font-weight: 900; }
    .process-step { display: flex; gap: 1rem; border: 1px solid #ffffff12; border-radius: 1.25rem; padding: 1.25rem; background: #0f172a80; }
    .process-step > span { display: grid; width: 2.5rem; height: 2.5rem; flex: 0 0 auto; place-items: center; border-radius: .8rem; color: #020617; background: var(--brand-primary); font-weight: 900; }
    .process-step h3 { margin-top: 0; }
    @media (max-width: 639px) { .feature-card { min-height: auto; } .feature-card:nth-child(2) { transform: none; } }
  `,
})
export class LandingPage {}
