import {Injectable} from '@angular/core'
import {HttpClient} from "@angular/common/http"
import {CreateTenantResponse, TenantResponse, TenantsResponse} from "./types";

@Injectable({
  providedIn: 'root'
})

export class TenantsService {

  constructor(private http: HttpClient) {
  }

  public list() {
    return this.http.get<TenantsResponse>("http://localhost:8080/api/v1/tenants");
  }

  public get(id: string) {
    return this.http.get<TenantResponse>(`http://localhost:8080/api/v1/tenants/${id}`);
  }

  public create(email: String, tenant: String) {
    return this.http.post<CreateTenantResponse>("http://localhost:8080/api/v1/tenants", {
      email,
      tenant
    });
  }


}
