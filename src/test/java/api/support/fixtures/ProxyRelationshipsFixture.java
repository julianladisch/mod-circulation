package api.support.fixtures;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.folio.circulation.support.ClockManager;

import api.support.builders.ProxyRelationshipBuilder;
import api.support.http.IndividualResource;
import api.support.http.ResourceClient;

public class ProxyRelationshipsFixture {
  private final ResourceClient proxyRelationshipsClient;

  public ProxyRelationshipsFixture(ResourceClient proxyRelationshipsClient) {
    this.proxyRelationshipsClient = proxyRelationshipsClient;
  }

  public void nonExpiringProxyFor(
    IndividualResource sponsor,
    IndividualResource proxy) {

    proxyRelationshipsClient.create(new ProxyRelationshipBuilder()
      .sponsor(sponsor.getId())
      .proxy(proxy.getId())
      .active()
      .doesNotExpire());
  }

  public void inactiveProxyFor(
    IndividualResource sponsor,
    IndividualResource proxy) {

    proxyRelationshipsClient.create(new ProxyRelationshipBuilder()
      .sponsor(sponsor.getId())
      .proxy(proxy.getId())
      .inactive()
      .expires(ClockManager.getZonedDateTime().plusYears(1)));
  }

  public void currentProxyFor(
    IndividualResource sponsor,
    IndividualResource proxy) {

    proxyFor(sponsor.getId(), proxy.getId(),
      ClockManager.getZonedDateTime().plusYears(1));
  }

  public void expiredProxyFor(
    IndividualResource sponsor,
    IndividualResource proxy) {

    proxyFor(sponsor.getId(), proxy.getId(),
      ClockManager.getZonedDateTime().minusYears(1));
  }

  private void proxyFor(
    UUID sponsorUserId,
    UUID proxyUserId,
    ZonedDateTime expirationDate) {

    proxyRelationshipsClient.create(new ProxyRelationshipBuilder()
      .sponsor(sponsorUserId)
      .proxy(proxyUserId)
      .active()
      .expires(expirationDate));
  }
}
