resource "scaleway_iam_application" "backup" {
  name            = "${var.environment}-${var.name}-backup"
  organization_id = var.scw_organization_id
}

resource "scaleway_object_bucket" "backup" {
  name          = "${var.environment}-${var.name}-backup"
  project_id    = data.scaleway_account_project.project.id
  force_destroy = true
}

resource "scaleway_iam_api_key" "backup" {
  application_id     = scaleway_iam_application.backup.id
  default_project_id = data.scaleway_account_project.project.id
}

data "scaleway_account_project" "project" {
  name            = "solidblocks-test"
  organization_id = var.scw_organization_id
}

resource "scaleway_iam_policy" "backup" {
  name            = "${var.environment}-${var.name}-backup"
  application_id  = scaleway_iam_application.backup.id
  organization_id = var.scw_organization_id

  rule {
    permission_set_names = [
      "ObjectStorageObjectsDelete",
      "ObjectStorageObjectsRead",
      "ObjectStorageObjectsWrite"
    ]
    project_ids = [data.scaleway_account_project.project.id]
  }
}
