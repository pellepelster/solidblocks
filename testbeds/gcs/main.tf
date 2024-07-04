resource "google_service_account" "solidblocks_test" {
  project    = "solidblocks-test"
  account_id = "solidblocks-test"
}

resource "google_project_iam_custom_role" "solidblocks_test_bucket" {
  project     = "solidblocks-test"
  permissions = [
    "storage.buckets.create",
    "storage.buckets.delete",
    "storage.buckets.get",
    "storage.buckets.list",
    "storage.objects.delete",
    "storage.objects.create",
    "storage.objects.list",
  ]
  role_id = "solidblocks_test_bucket"
  title   = "solidblocks-test-bucket"
}

resource "google_project_iam_policy" "solidblocks_test_bucket" {
  project     = "solidblocks-test"
  policy_data = data.google_iam_policy.solidblocks_test.policy_data
}

data "google_iam_policy" "solidblocks_test" {
  binding {
    role    = google_project_iam_custom_role.solidblocks_test_bucket.id
    members = ["serviceAccount:${google_service_account.solidblocks_test.email}"]

    /*
    condition {
      title      = "test_bucket_prefix"
      expression = "resource.name.startsWith(\"projects/_/buckets/test-\")"
    }*/
  }

  binding {
    role    = "roles/viewer"
    members = ["serviceAccount:${google_service_account.solidblocks_test.email}"]
  }

}