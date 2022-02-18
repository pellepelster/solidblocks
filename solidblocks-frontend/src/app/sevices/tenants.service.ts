import {Injectable} from '@angular/core'
import {HttpClient} from "@angular/common/http"
import {CreateTenantResponse, TenantResponse, TenantsResponse} from "./types";
import {environment} from "../../environments/environment";

@Injectable({
  providedIn: 'root'
})

export class TenantsService {

  constructor(private http: HttpClient) {
  }

  public list() {
    return this.http.get<TenantsResponse>(`${environment.apiAddress}/v1/tenants`);
  }

  public get(id: string) {
    return this.http.get<TenantResponse>(`${environment.apiAddress}/v1/tenants/${id}`);
  }

  public create(email: String, tenant: String) {
    return this.http.post<CreateTenantResponse>(`${environment.apiAddress}/v1/tenants`, {
      email,
      tenant
    });
  }


}
