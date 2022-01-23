import {Injectable} from '@angular/core';
import {HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest} from '@angular/common/http';
import {catchError, Observable, throwError} from 'rxjs';
import {LoginService} from "./login.service";

//import { environment } from '@environments/environment';
//import { AccountService } from '@app/_services';

@Injectable()
export class AuthenticationTokenInterceptor implements HttpInterceptor {
  constructor(private loginService: LoginService) {
  }

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // add auth header with jwt if account is logged in and request is to the api url
    //const isLoggedIn = account?.token;
    //const isApiUrl = request.url.startsWith(environment.apiUrl);
    //if (isLoggedIn && isApiUrl) {
    request = request.clone({
      setHeaders: {Authorization: `Bearer ${this.loginService.token}`}
    });
    // }

    //return next.handle(request);

    return next.handle(request).pipe(catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        //this.loginService.logout()
      }

      return throwError(() => error);
    }));
  }

}
