import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {HomeComponent} from "./components/home/home.component";
import {LoginComponent} from "./components/authentication/login/login.component";
import {ConsoleHomeComponent} from "./components/console-home/console-home.component";
import {LogoutComponent} from "./components/authentication/logout/logout.component";
import {TenantsCreateComponent} from "./components/tenants/tenants-create/tenants-create.component";
import {TenantsHomeComponent} from "./components/tenants/tenants-home/tenants-home.component";
import {ServicesCreateComponent} from "./components/services/services-create/services-create.component";
import {CloudsHomeComponent} from "./components/clouds/clouds-home/clouds-home.component";
import {EnvironmentsHomeComponent} from "./components/environments/environments-home/environments-home.component";
import {ServicesHomeComponent} from "./components/services/services-home/services-home.component";
import {CloudHomeComponent} from "./components/clouds/cloud-home/cloud-home.component";
import {EnvironmentHomeComponent} from "./components/environments/environment-home/environment-home.component";
import {TenantHomeComponent} from "./components/tenants/tenant-home/tenant-home.component";

const routes: Routes = [
  {path: '', component: HomeComponent},
  {path: 'home', component: HomeComponent},
  {path: 'login', component: LoginComponent},
  {path: 'logout', component: LogoutComponent},

  {path: 'clouds/home', component: CloudsHomeComponent},
  {path: 'clouds/:id', component: CloudHomeComponent},

  {path: 'environments/home', component: EnvironmentsHomeComponent},
  {path: 'environments/:id', component: EnvironmentHomeComponent},

  {path: 'tenants/home', component: TenantsHomeComponent},
  {path: 'tenants/:id', component: TenantHomeComponent},

  {path: 'services/home', component: ServicesHomeComponent},

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
