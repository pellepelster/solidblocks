import {Injectable} from '@angular/core'
import {HttpClient} from "@angular/common/http"
import {CloudResponse, LoginResponse, WhoAmIResponse} from "./types";
import {LoginService} from "../authentication/login.service";

@Injectable({
  providedIn: 'root'
})

export class CloudsService {

  constructor(private http: HttpClient, private loginService: LoginService) {
  }

  public list() {
    return this.http.get<CloudResponse>("http://localhost:8080/api/v1/clouds");
  }

}
