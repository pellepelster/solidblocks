FROM ruby:2.5
MAINTAINER pelle@pelle.io

ADD Gemfile .
RUN bundle install

ADD hcloud_api_mock_generated.rb /hcloud_api_mock_generated.rb

CMD ["bundle", "exec", "ruby", "/hcloud_api_mock_generated.rb"]
