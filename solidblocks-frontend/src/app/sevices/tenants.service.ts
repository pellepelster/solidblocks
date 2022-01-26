import {Injectable} from '@angular/core'
import {HttpClient} from "@angular/common/http"
import {CreateTenantResponse, TenantsResponse} from "./types";
import {LoginService} from "../authentication/login.service";

@Injectable({
  providedIn: 'root'
})

export class TenantsService {

  constructor(private http: HttpClient, private loginService: LoginService) {
  }

  public list() {
    return this.http.get<TenantsResponse>("http://localhost:8080/api/v1/tenants");
  }

  public create(email: String, tenant: String) {
    return this.http.post<CreateTenantResponse>("http://localhost:8080/api/v1/tenants", {
      email,
      tenant
    });
  }


}
