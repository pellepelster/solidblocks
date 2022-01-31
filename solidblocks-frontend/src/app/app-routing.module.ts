import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {HomeComponent} from "./components/home/home.component";
import {LoginComponent} from "./components/authentication/login/login.component";
import {ConsoleHomeComponent} from "./components/console-home/console-home.component";
import {LogoutComponent} from "./components/authentication/logout/logout.component";
import {TenantsCreateComponent} from "./components/tenants/tenants-create/tenants-create.component";
import {TenantsHomeComponent} from "./components/tenants/tenants-home/tenants-home.component";
import {ServicesCreateComponent} from "./services-create/services-create.component";

const routes: Routes = [
  {path: '', component: HomeComponent},
  {path: 'home', component: HomeComponent},
  {path: 'login', component: LoginComponent},
  {path: 'logout', component: LogoutComponent},
  {path: 'tenants/create', component: TenantsCreateComponent},
  {path: 'tenants/:id', component: TenantsHomeComponent},
  {path: 'console/home', component: ConsoleHomeComponent},
  {path: 'services/create', component: ServicesCreateComponent},
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {
}
