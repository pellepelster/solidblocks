import {Injectable} from '@angular/core'
import {HttpClient} from "@angular/common/http"
import {LoginResponse, WhoAmIResponse} from "./types";
import {environment} from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})

export class AuthService {

  constructor(private http: HttpClient) {
  }

  public whoAmI() {
    return this.http.get<WhoAmIResponse>(`${environment.apiAddress}/v1/auth/whoami`);
  }

  login(email: String, password: String) {
    return this.http.post<LoginResponse>(`${environment.apiAddress}/v1/auth/login`, {
      email,
      password
    });
  }
}
