export interface User {
  email: string
}

export interface Cloud {
  name: string
}

export interface MessageResponse {
  code: string
}

export interface WhoAmIResponse {
  user: User
}

export interface CloudResponse {
  cloud: Cloud
}

export interface LoginResponse {
  token: string
  messages: Array<MessageResponse>
}
