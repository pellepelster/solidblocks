terraform {
  required_providers {
    random = {
      source  = "hashicorp/random"
      version = "3.7.2"
    }
  }
}

resource "random_string" "random1" {
  length = 12
}

output "random1" {
  value = random_string.random1.id
}


output "json_list" {
  value = [
  { name : "foo" }]
}

output "null" {
  value = null
}
