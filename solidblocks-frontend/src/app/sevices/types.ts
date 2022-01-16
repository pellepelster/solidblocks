export interface User {
  email: string
}

export interface MessageResponse {
  code: string
}

export interface WhoAmIResponse {
  user: User
}

export interface LoginResponse {
  token: string
  messages: Array<MessageResponse>
}
