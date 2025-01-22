```shell
terraform apply -var 'name=test1' -auto-approve
ansible-playbook site.yml -i ../terraform/ansible/test.inventory.yml --extra-vars "@../terraform/ansible/variables.yml"
```