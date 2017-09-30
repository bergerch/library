
import { Routes } from '@angular/router';
import {ThroughputLatency} from "./throughputLatency.component";
import {Runner} from "./runner.component";


// Route Configuration
export const throughputLatencyRoutes: Routes = [
  { path: 'microbenchmarks', component: ThroughputLatency },
  { path: 'runner/:id', component: Runner },

];
