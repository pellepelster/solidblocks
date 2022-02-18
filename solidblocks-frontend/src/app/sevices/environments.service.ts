import {Injectable} from '@angular/core'
import {HttpClient} from "@angular/common/http"
import {EnvironmentResponse, EnvironmentsResponse} from "./types";
import {environment} from "../../environments/environment";

@Injectable({
  providedIn: 'root'
})

export class EnvironmentsService {

  constructor(private http: HttpClient) {
  }

  public list() {
    return this.http.get<EnvironmentsResponse>(`${environment.apiAddress}/v1/environments`);
  }

  public get(id: string) {
    return this.http.get<EnvironmentResponse>(`${environment.apiAddress}/v1/environments/${id}`);
  }

}
