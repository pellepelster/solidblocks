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
  value = random_string.random.id
}

output "foo" {
  value = "bar"
}