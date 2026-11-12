import { Component, inject } from '@angular/core';
import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
@Component({template:`<section class="org-card m-4 max-w-md shadow-xl" role="document"><h2 id="confirm-title" class="text-xl font-bold">{{data.title}}</h2><p class="my-5 text-slate-600">{{data.message}}</p><div class="flex flex-wrap justify-end gap-3"><button class="org-secondary" (click)="ref.close(false)">Cancelar</button><button class="org-danger" (click)="ref.close(true)">Confirmar</button></div></section>`})
export class ConfirmDialogComponent {
 readonly data=inject<{title:string;message:string}>(DIALOG_DATA);readonly ref=inject(DialogRef<boolean>);
}

