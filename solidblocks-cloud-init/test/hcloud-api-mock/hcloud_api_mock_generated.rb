require 'sinatra'
require 'json'

SERVERS = []

class HCLoudApiMock < Sinatra::Base
  configure do
    set :bind, '0.0.0.0'
    set :port, 8080
  end

  # before application/request starts
  before do
    content_type 'application/json'
    cache_control :public, max_age: 86400
  end

  def servers
    if SERVERS.empty?
      SERVERS.push(control_server(0))
      SERVERS.push(control_server(1))
      SERVERS.push(control_server(2))
      SERVERS.push(storage_server(0))
      SERVERS.push(storage_server(1))
      SERVERS.push(worker_server(0))
      SERVERS.push(worker_server(1))
    end
    SERVERS
  end

  def control_server(index)
    floating_ips = []

    if index == 1
      floating_ips = [ 1 ]
    elsif index == 2
      floating_ips = [ 1, 2 ]
    end
    {
        'id' => 100 + index,
        'name' => "controller-#{index}",
        'private_net' => [
            { 'ip' => "4.4.4.#{index + 1}" }
        ],
        'public_net' => {
            'ipv4' => {
                'ip' => "12.0.10.#{index + 1}",
            },
            'floating_ips' => floating_ips
        },
        'labels' => { 'role' => 'controller' }
      }
  end

  def worker_server(index)
    {
        'id' => 200 + index,
        'name' => "worker-#{index}",
        'public_net' => {
            'ipv4' => {
                'ip' => "12.0.20.#{index + 1}",
            },
            'floating_ips' => []
        },
        'labels' => {'role' => 'worker'}
    }
  end

  def storage_server(index)
    {
        'id' => 300 + index,
        'name' => "storage-#{index}",
        'public_net' => {
            'ipv4' => {
                'ip' => "12.0.30.#{index + 1}",
            },
            'floating_ips' => []
        },
        'labels' => {'role' => 'storage'}
    }
  end

  def servers_response(servers)
    {
        'servers' => servers,
        'meta' => {
            'pagination' => {
                'page' => 1,
                'per_page' => 25,
                'previous_page' => nil,
                'next_page' => nil,
                'last_page' => 1,
                'total_entries' => servers.length
            }
        }
    }
  end

  def server_response(server)
    {
        'server' => server
    }
  end

  def floating_ip_controller(index)
    {
        'id' => index,
        'description' => "controller-#{index}",
        'ip' => "4.4.4.#{index + 1}"
    }
  end

  def floating_ips_response(floating_ips)
    {
        'floating_ips' => floating_ips
    }
  end

  def floating_ip_response(floating_ip)
    {
        'floating_ip' => floating_ip
    }
  end

  get '/v1/servers' do
    if params['name']
      JSON.generate(servers_response(servers.select{ |server| server['name'] == params['name'] }))
    elsif params['label_selector'] == 'role=controller'
      JSON.generate(servers_response(servers.select{ |server| server['labels']['role'] == 'controller' }))
    else
      JSON.generate(servers_response(servers))
    end
  end

  put '/v1/servers/:id' do
    request_body = JSON.parse(request.body.read)
    result = servers.select{ |server| server['id'] == params['id'].to_i }

    if result.length == 1
      if request_body['labels']
        result[0]['labels'] = request_body['labels']
      end
      return JSON.generate(server_response(result[0]))
    end

  end

  get '/v1/floating_ips/?:id?' do
    if params['label_selector'] == 'role=controller'
      JSON.generate(floating_ips_response([ floating_ip_controller(0), floating_ip_controller(1), floating_ip_controller(2) ]))
    elsif params['id']
      JSON.generate(floating_ip_response(floating_ip_controller(params['id'].to_i)))
    else
      JSON.generate(floating_ips_response([ floating_ip_controller(0), floating_ip_controller(1), floating_ip_controller(2) ]))
    end
  end

  get '/*' do
    puts '================================================='
    puts "no mock for path '#{request.path_info}'"
    puts '================================================='
    JSON.generate(error: 'not mocked')
  end

  run! if app_file == $0
end
