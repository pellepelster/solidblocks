terraform {
  required_providers {
    random = {
      source  = "hashicorp/random"
      version = "3.6.3"
    }
  }
}

resource "random_string" "random" {
  length  = 16
  special = false
}

output "random" {
  description = "some random string"
  value       = random_string.random.id
}

output "foo" {
  description = "important foo content"
  value       = "bar"
}