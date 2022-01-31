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
import { TenantsCreateComponent } from './components/tenants/tenants-create/tenants-create.component';
import {InputControlComponent} from "./controls/input-control.component";
import { TenantsHomeComponent } from './components/tenants/tenants-home/tenants-home.component';
import { ServicesCreateComponent } from './services-create/services-create.component';

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
        InputControlComponent,
        TenantsHomeComponent,
        ServicesCreateComponent
    ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    HttpClientModule,
    NgbModule,
    ReactiveFormsModule,
    FormsModule
  ],
  providers: [
    {provide: HTTP_INTERCEPTORS, useClass: AuthenticationTokenInterceptor, multi: true},
  ],
  bootstrap: [AppComponent]
})
export class AppModule {
}
