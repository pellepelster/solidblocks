package de.solidblocks.hetzner.dns;
/*
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.pelle.hetzner.model.*;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

public class HetznerDnsAPI_old {


  private static final String DEFAULT_API_URL = "https://dns.hetzner.com/api/v1";

  private final String token;
  private final String apiUrl;

  private HttpEntity<String> defaultHttpEntity;
  private HttpHeaders httpHeaders;
  private RestTemplate restTemplate;

  public HetznerDnsAPI_old(String token) {
    this(token, DEFAULT_API_URL);
  }

  public HetznerDnsAPI_old(String token, String apiUrl) {
    this.token = token;
    this.apiUrl = apiUrl;


    this.httpHeaders = new HttpHeaders();
    this.httpHeaders.setContentType(MediaType.APPLICATION_JSON);
    this.httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    this.httpHeaders.add("Auth-API-Token", token);
    this.defaultHttpEntity = new HttpEntity<>("parameters", httpHeaders);

    var objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

    var converter = new MappingJackson2HttpMessageConverter();
    converter.setObjectMapper(objectMapper);

    restTemplate = new RestTemplate();
    restTemplate.getMessageConverters().add(0, converter);
  }

  public ZoneResponse getZone(String zoneId) {
    return restTemplate
        .exchange(
            this.apiUrl + "/zones/" + zoneId, HttpMethod.GET, defaultHttpEntity, ZoneResponse.class)
        .getBody();
  }

  public ZoneResponse createZone(ZoneRequest request) {
    var entity = new HttpEntity<>(request, httpHeaders);
    return restTemplate
        .exchange(this.apiUrl + "/zones", HttpMethod.POST, entity, ZoneResponse.class)
        .getBody();
  }

  public ZoneResponse searchZone(String name) {
    return request(
            "/zones?name=" + name, HttpMethod.GET, defaultHttpEntity, ListZonesResponse.class)
        .map(ListZonesResponse::getZones)
        .flatMap(t -> t.stream().findFirst())
        .orElse(null);
  }

  public List<ZoneResponse> getZones() {
    return request("/zones", HttpMethod.GET, defaultHttpEntity, ListZonesResponse.class)
        .map(ListZonesResponse::getZones)
        .orElse(Collections.emptyList());
  }

  public boolean deleteZone(String zoneId) {
    return request("/zones/" + zoneId, HttpMethod.DELETE, defaultHttpEntity, Object.class)
        .isPresent();
  }

  public RecordResponse createRecord(RecordRequest request) {
    var entity = new HttpEntity<>(request, httpHeaders);
    return request("/records", HttpMethod.POST, entity, RecordResponseWrapper.class)
        .map(RecordResponseWrapper::getRecord)
        .orElse(null);
  }


  public List<RecordResponse> getRecords(String zoneId) {
    return request(
            "/records?zone_id=" + zoneId,
            HttpMethod.GET,
            defaultHttpEntity,
            ListRecordsResponse.class)
        .map(ListRecordsResponse::getRecords)
        .orElse(Collections.emptyList());
  }

  public RecordResponse updateRecord(String recordId, RecordRequest request) {
    var entity = new HttpEntity<>(request, httpHeaders);
    return request("/records/" + recordId, HttpMethod.PUT, entity, RecordResponseWrapper.class)
        .map(RecordResponseWrapper::getRecord)
        .orElse(null);
  }

  private <T> Optional<T> request(
      String url, HttpMethod method, HttpEntity<?> httpEntity, Class<T> responseClass) {
    try {
      var response = restTemplate.exchange(this.apiUrl + url, method, httpEntity, responseClass);
      return Optional.of(response.getBody());
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
        return Optional.empty();
      }

      log.error(
          "dns api returned HTTP {}, response was '{}'",
          e.getRawStatusCode(),
          e.getResponseBodyAsString());
      throw new RuntimeException(e);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
*/