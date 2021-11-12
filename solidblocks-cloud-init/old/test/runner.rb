scope = ARGV.shift

$LOAD_PATH << File.join(__dir__, '../../lib/spec')
$LOAD_PATH << File.join(__dir__, scope)

Dir[File.join(__dir__, scope, '**', '*_spec.rb')].each do |test|
  require_relative test
end
