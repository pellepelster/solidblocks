import {Injectable} from '@angular/core'
import {HttpClient} from "@angular/common/http"
import {CloudResponse, CloudsResponse} from "./types";

@Injectable({
  providedIn: 'root'
})

export class CloudsService {

  constructor(private http: HttpClient) {
  }

  public list() {
    return this.http.get<CloudsResponse>("http://localhost:8080/api/v1/clouds");
  }

  public get(id: string) {
    return this.http.get<CloudResponse>(`http://localhost:8080/api/v1/clouds/${id}`);
  }

}
