import {Injectable} from '@angular/core'
import {HttpClient} from "@angular/common/http"
import {EnvironmentResponse, EnvironmentsResponse} from "./types";

@Injectable({
  providedIn: 'root'
})

export class EnvironmentsService {

  constructor(private http: HttpClient) {
  }

  public list() {
    return this.http.get<EnvironmentsResponse>("http://localhost:8080/api/v1/environments");
  }

  public get(id: string) {
    return this.http.get<EnvironmentResponse>(`http://localhost:8080/api/v1/environments/${id}`);
  }

}
