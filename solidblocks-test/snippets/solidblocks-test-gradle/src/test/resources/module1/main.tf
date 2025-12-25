resource "random_string" "string1" {
  length = 12
}

output "string1" {
  value = random_string.string1.id
}
