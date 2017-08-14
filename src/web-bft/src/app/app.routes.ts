import { ModuleWithProviders }  from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import {counterRoutes} from "./CounterDemo/counter.routes";
import {editorRoutes} from "./CollaborativeEditor/editor.routes";



// Route Configuration
export const routes: Routes = [
  {
    path: '',
    redirectTo: '',
    pathMatch: 'full'
  },
  ...counterRoutes,
  ...editorRoutes
];


export const routing: ModuleWithProviders = RouterModule.forRoot(routes);
