require 'spec_helper'

describe 'cloud-init' do
  before(:all) do
    @compose ||= ComposeWrapper.new('test/functional/docker-compose.yml')
    @compose.up('hcloud-api-mock', detached: true)
  end

  after(:all) do
    @compose.shutdown
  end

  def exec(command)
    `docker-compose --file #{File.expand_path(File.dirname(__FILE__))}/docker-compose.yml  exec cloud-init-test-runner-controller-0 #{command}`
  end

  it 'controller' do
    @compose.up('cloud-init-test-runner-controller-0', detached: true)
    Thread.new { @compose.run!('exec', 'cloud-init-test-runner-controller-0', '/cloud-init.sh') }

    @compose.up('cloud-init-test-runner-controller-1', detached: true)
    Thread.new { @compose.run!('exec', 'cloud-init-test-runner-controller-1', '/cloud-init.sh') }

    @compose.up('cloud-init-test-runner-controller-2', detached: true)
    Thread.new { @compose.run!('exec', 'cloud-init-test-runner-controller-2', '/cloud-init.sh') }


    p '=================================================================='
    p @compose.logs('cloud-init-test-runner-controller-2')
    p '=================================================================='

    retry_until timeout: 20 do
      assert_match /Status: install ok installed/, @compose.run!('exec', 'cloud-init-test-runner-controller-0', 'dpkg', '-s', 'jq')
      assert_match /Status: install ok installed/, @compose.run!('exec', 'cloud-init-test-runner-controller-0', 'dpkg', '-s', 'curl')
    end

    assert_match /integration-test/, @compose.run!('exec', 'cloud-init-test-runner-controller-0', 'jq', '-r', '.cloud_name', '/solidblocks/solidblocks.json')

    retry_until timeout: 30 do
      peers = JSON.parse(exec('curl -s http://127.0.0.1:8500/v1/status/peers'))
      assert_includes peers, '12.0.10.1:8300'
      assert_includes peers, '12.0.10.2:8300'
      assert_includes peers, '12.0.10.3:8300'
    end
  end
end
