import { ModuleWithProviders }  from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import {counterRoutes} from "./CounterDemo/counter.routes";
import {editorRoutes} from "./CollaborativeEditor/editor.routes";
import {throughputLatencyRoutes} from "./Microbenchmarks/throughputLatency.routes"



// Route Configuration
export const routes: Routes = [
  {
    path: '',
    redirectTo: '',
    pathMatch: 'full'
  },
  ...counterRoutes,
  ...editorRoutes,
  ...throughputLatencyRoutes
];


export const routing: ModuleWithProviders = RouterModule.forRoot(routes);
