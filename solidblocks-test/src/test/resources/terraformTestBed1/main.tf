terraform {
  required_providers {
    random = {
      source  = "hashicorp/random"
      version = "3.7.2"
    }
  }
}

output "string1" {
  value = "foo-bar"
}

output "number1" {
  value = 123
}

output "boolean1" {
  value = true
}

output "json1" {
  value = { name : "foo" }
}

output "json_list1" {
  value = [
  { name : "foo" }]
}

output "null" {
  value = null
}
