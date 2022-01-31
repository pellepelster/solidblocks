export interface User {
  email: string
}

export interface ConfigValueDefinition {
  name: string
  type: string
}

export interface Cloud {
  name: string
}

export interface ResourceReference {
  id: string
  name: string
}

export interface TenantResource {
  cloud: ResourceReference
  environment: ResourceReference
  tenant: ResourceReference
}

export interface Tenant {
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

export interface CloudResponse {
  cloud: Cloud
}

export interface TenantsResponse {
  tenants: Array<Tenant>
}

export interface CreateTenantResponse {
  messages: Array<MessageResponse>
}

export interface LoginResponse {
  token: string
  messages: Array<MessageResponse>
}
