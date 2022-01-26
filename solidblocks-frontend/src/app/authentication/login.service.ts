import {Injectable} from '@angular/core'
import {AuthService} from "../sevices/auth.service"
import {MessageResponse} from "../sevices/types";

const userTokenKey = "user-token";

@Injectable({
  providedIn: 'root'
})
export class LoginService {

  token: string | null = null

  constructor(private authService: AuthService) {
    this.token = localStorage.getItem(userTokenKey)
  }

  hasValidUser() {
    return this.token != null && this.token?.trim().length !== 0
  }

  logout() {
    this.token = null
    localStorage.removeItem(userTokenKey)
  }

  private updateToken(token: string) {
    this.token = token
    localStorage.setItem(userTokenKey, token)
  }

  login(email: string, password: string) {
    return new Promise((resolve, reject) => {

      this.authService.login(email, password).subscribe({
        next: (response) => {
          this.updateToken(response.token)
          resolve(true)
        },
        error: (error) => {
          reject(error)
        },
        complete: () => console.info('complete')
      })
    })
  }
}
