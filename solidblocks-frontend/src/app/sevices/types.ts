export interface User {
  email: string
}

export interface ConfigValueDefinition {
  name: string
  type: string
}

export interface ResourceReference {
  id: string
  name: string
}

// cloud
export interface Cloud {
  name: string
  id: string
  environments: Array<Environment>
}

export interface CloudsResponse {
  clouds: Array<Cloud>
}

export interface CloudResponse {
  cloud: Cloud
}

// environment
export interface Environment {
  name: string
  id: string
  tenants: Array<Tenant>
}

export interface EnvironmentsResponse {
  environments: Array<Environment>
}

export interface EnvironmentResponse {
  environment: Environment
}

// tenant
export interface Tenant {
  id: string
  name: string
  services: Array<Service>
}

export interface TenantsResponse {
  tenants: Array<Tenant>
}

export interface TenantResponse {
  tenant: Tenant
}

export interface CreateTenantResponse {
  messages: Array<MessageResponse>
}

// service
export interface Service {
  id: string
  name: string
}


export interface MessageResponse {
  attribute: string
  code: string
}

export interface WhoAmIResponse {
  user: User
}

export interface UserResponse {
  email: string
  scope: string
}

export interface LoginResponse {
  token: string
  user: UserResponse
  messages: Array<MessageResponse>
}
