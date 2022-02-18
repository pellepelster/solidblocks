import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';

import {AppRoutingModule} from './app-routing.module';
import {AppComponent} from './app.component';
import {LoginComponent} from './components/authentication/login/login.component';
import {HomeComponent} from './components/home/home.component';
import {HTTP_INTERCEPTORS, HttpClientModule} from '@angular/common/http';
import {NgbModule} from '@ng-bootstrap/ng-bootstrap';
import {NavigationComponent} from './components/navigation/navigation.component';
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {ToastsContainer} from "./utils/toasts.container";
import {ConsoleHomeComponent} from './components/console-home/console-home.component';
import {LogoutComponent} from './components/authentication/logout/logout.component';
import {AuthenticationTokenInterceptor} from "./authentication/authentication-token.interceptor";
import {TenantsCreateComponent} from './components/tenants/tenants-create/tenants-create.component';
import {TenantsHomeComponent} from './components/tenants/tenants-home/tenants-home.component';
import {CloudsHomeComponent} from './components/clouds/clouds-home/clouds-home.component';
import {EnvironmentsHomeComponent} from './components/environments/environments-home/environments-home.component';
import {CloudHomeComponent} from './components/clouds/cloud-home/cloud-home.component';
import {EnvironmentHomeComponent} from "./components/environments/environment-home/environment-home.component";
import {TenantsListItemComponent} from "./components/tenants/tenants-list-item.component";
import {TenantHomeComponent} from "./components/tenants/tenant-home/tenant-home.component";
import {EnvironmentsListItemComponent} from "./components/environments/environments-list-item.component";
import {ServicesModule} from "./components/services/services-module";
import {ControlsModule} from "./components/controls/controls-module";
import {CloudsListItemComponent} from "./components/clouds/clouds-list-item.component";
import {TestAbcComponent} from './test-abc/test-abc.component';

@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    HomeComponent,
    NavigationComponent,
    ToastsContainer,
    ConsoleHomeComponent,
    LogoutComponent,
    TenantsCreateComponent,
    TenantsHomeComponent,
    CloudsHomeComponent,
    EnvironmentsHomeComponent,
    CloudHomeComponent,
    EnvironmentHomeComponent,
    TenantHomeComponent,
    TenantsListItemComponent,
    EnvironmentsListItemComponent,
    CloudsListItemComponent,
    TestAbcComponent,
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    HttpClientModule,
    NgbModule,
    ReactiveFormsModule,
    FormsModule,
    ServicesModule,
    ControlsModule
  ],
  providers: [
    {provide: HTTP_INTERCEPTORS, useClass: AuthenticationTokenInterceptor, multi: true},
  ],
  bootstrap: [AppComponent]
})
export class AppModule {
}
