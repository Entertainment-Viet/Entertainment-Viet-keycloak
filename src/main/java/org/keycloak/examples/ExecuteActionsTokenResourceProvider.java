package org.keycloak.examples;

import lombok.extern.slf4j.Slf4j;
import org.jboss.logging.Logger;
import org.keycloak.authentication.actiontoken.execactions.ExecuteActionsActionToken;
import org.keycloak.common.util.Time;
import org.keycloak.models.*;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.services.resource.RealmResourceProvider;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.LinkedList;
import java.util.List;

@Slf4j
public class ExecuteActionsTokenResourceProvider implements RealmResourceProvider {

  private static final Logger logger = Logger.getLogger(ExecuteActionsTokenResourceProvider.class);

  private final KeycloakSession session;

  public ExecuteActionsTokenResourceProvider(KeycloakSession session) {
    this.session = session;
  }

  @POST
  @Path("action-tokens")
  @Produces({MediaType.APPLICATION_JSON})
  public Response getActionToken(
      @QueryParam("userId") String userId,
      @QueryParam("action") String action,
      @QueryParam("redirectUri") String redirectUri,
      @QueryParam("clientId") String clientId,
      @Context UriInfo uriInfo) {

    KeycloakContext context = session.getContext();
    RealmModel realm = context.getRealm();
    int validityInSecs = realm.getActionTokenGeneratedByUserLifespan();
    int absoluteExpirationInSecs = Time.currentTime() + validityInSecs;

    ClientModel client = assertValidClient(clientId, realm);
    logger.info(String.format("Generating token for userId %s from client %s in realm %s, of action %s, with redirectUri %s.",
        userId, client, realm.getName() ,action, redirectUri));
    logger.info("The token will valid until timestamp: " + absoluteExpirationInSecs*1000);

    assertValidRedirectUri(redirectUri, client);

    // Can parameterize this as well
    List requiredActions = new LinkedList();
    requiredActions.add(action);

    String token = new ExecuteActionsActionToken(
        userId,
        absoluteExpirationInSecs,
        requiredActions,
        redirectUri,
        clientId
    ).serialize(
        session,
        context.getRealm(),
        uriInfo
    );

    return Response.status(200).entity(token).build();
  }

  private void assertValidRedirectUri(String redirectUri, ClientModel client) {
    String redirect = RedirectUtils.verifyRedirectUri(session, redirectUri, client);
    if (redirect == null) {
      throw new WebApplicationException("Invalid redirect uri.", Response.Status.BAD_REQUEST);
    }
  }

  private ClientModel assertValidClient(String clientId, RealmModel realm) {
    ClientModel client = realm.getClientByClientId(clientId);
    if (client == null) {
      throw new WebApplicationException("Client doesn't exist", Response.Status.BAD_REQUEST);
    }
    if (!client.isEnabled()) {
      throw new WebApplicationException("Client is not enabled", Response.Status.BAD_REQUEST);
    }
    return client;
  }

  @Override
  public Object getResource() {
    return this;
  }

  @Override
  public void close() {
    // Nothing to close.
  }
}
