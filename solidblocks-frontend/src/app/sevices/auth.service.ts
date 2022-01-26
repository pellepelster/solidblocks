import {Injectable} from '@angular/core'
import {HttpClient} from "@angular/common/http"
import {LoginResponse, WhoAmIResponse} from "./types";

@Injectable({
  providedIn: 'root'
})

export class AuthService {

  constructor(private http: HttpClient) {
  }

  public whoAmI() {
    return this.http.get<WhoAmIResponse>("http://localhost:8080/api/v1/auth/whoami");
  }

  login(email: String, password: String) {
    return this.http.post<LoginResponse>("http://localhost:8080/api/v1/auth/login", {
      email,
      password
    });
  }
}
