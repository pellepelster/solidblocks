import pathlib
import subprocess
import time
import unittest

import boto3


class Test(unittest.TestCase):

    def setUp(self):
        s3 = boto3.resource('s3')
        for bucket in s3.buckets.all():
            if bucket.name.startswith('test-'):
                for obj in bucket.objects.all():
                    obj.delete()

                bucket.delete()
                print(f"deleting S3 bucket: {bucket.name}")
                bucket.wait_until_not_exists()
                print(f"deleted S3 bucket: {bucket.name}")

    def test_nginx_config(self):
        process = subprocess.Popen(
            [f"{pathlib.Path(__file__).parent.resolve()}/../build/solidblocks-terraform", 'backend', 's3',
             '--name', 'test-pelle1', '--region', 'eu-central-1'])
        process.wait()
        assert process.returncode == 0
