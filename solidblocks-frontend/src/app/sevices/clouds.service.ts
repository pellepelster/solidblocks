import {Injectable} from '@angular/core'
import {HttpClient} from "@angular/common/http"
import {CloudResponse, CloudsResponse} from "./types";
import {environment} from "../../environments/environment";

@Injectable({
  providedIn: 'root'
})

export class CloudsService {

  constructor(private http: HttpClient) {
  }

  public list() {
    return this.http.get<CloudsResponse>(`${environment.apiAddress}/v1/clouds`);
  }

  public get(id: string) {
    return this.http.get<CloudResponse>(`${environment.apiAddress}/v1/clouds/${id}`);
  }

}
