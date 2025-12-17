variable "test_id" {
  type = string
}

variable "bucket1_name" {
  type    = string
  default = "bucket1"
}

variable "bucket2_name" {
  type    = string
  default = "bucket2"
}

variable "docker_public_enable" {
  type = bool
}

variable "docker_enable" {
  type = bool
}
