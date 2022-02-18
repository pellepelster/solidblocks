import {Injectable} from '@angular/core'
import {HttpClient} from "@angular/common/http"
import {ServiceCatalogResponse, TenantsResponse} from "./types";
import {environment} from "../../environments/environment";

@Injectable({
  providedIn: 'root'
})

export class ServicesService {

  constructor(private http: HttpClient) {
  }

  public catalog() {
    return this.http.get<ServiceCatalogResponse>(`${environment.apiAddress}/v1/services/catalog`);
  }

}
