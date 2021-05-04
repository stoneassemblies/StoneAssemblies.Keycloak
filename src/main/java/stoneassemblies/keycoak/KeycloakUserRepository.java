package stoneassemblies.keycoak;

import stoneassemblies.keycoak.interfaces.UserRepository;
import stoneassemblies.keycoak.models.User;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.QueryParam;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class KeycloakUserRepository implements UserRepository {

    private Logger logger = Logger.getLogger(UserStorageProvider.class.getName());

    private String serverUrl;
    private String realm;
    private String user;
    private String password;
    private String clientId;

    public KeycloakUserRepository(String url,
                                  String realm,
                                  String user,
                                  String password,
                                  String clientId) {
        this.serverUrl = url;
        this.realm = realm;
        this.user = user;
        this.password = password;
        this.clientId = clientId;
    }

    @Override
    public List<User> getAllUsers() {
        try {
            RealmResource realmResource = getRealmResource();
            return realmResource.users().list().stream().map(KeycloakUserRepository::mapUser).collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Collections.emptyList();
    }

    private static User mapUser(UserRepresentation userRepresentation) {
        User user = new User();
        user.setId(userRepresentation.getId());
        user.setUsername(userRepresentation.getUsername());
        user.setFirstName(userRepresentation.getFirstName());
        user.setLastName(userRepresentation.getLastName());
        user.setEmail(userRepresentation.getEmail());
        user.setEnabled(userRepresentation.isEnabled());
        user.setCreated(System.currentTimeMillis());
        return user;
    }

    @Override
    public int getUsersCount() {
        try {
            RealmResource realmResource = getRealmResource();
            return realmResource.users().count();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    @Override
    public User findUserById(String id) {
        try {
            RealmResource realmResource = getRealmResource();
            UserResource userResource = realmResource.users().get(id);
            return mapUser(userResource.toRepresentation());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public User findUserByUsernameOrEmail(String username) {
        try {
            RealmResource realmResource = getRealmResource();
            Stream<UserRepresentation> userRepresentationStream = realmResource.users().search(username, -1, -1)
                    .stream().filter(u -> u.getUsername() != null && u.getUsername().equals(username) || u.getEmail() != null && u.getEmail().equals(username));
            return userRepresentationStream.map(KeycloakUserRepository::mapUser).findFirst().get();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean validateCredentials(String username, String password) {
        boolean succeeded = false;
        Keycloak realmClient = Keycloak.getInstance(serverUrl, realm, username, password, clientId);
        try {
            realmClient.tokenManager().getAccessTokenString();
            succeeded = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return succeeded;
    }

    @Override
    public boolean updateCredentials(String username, String password) {
        try {
            RealmResource realmResource = getRealmResource();
            Stream<UserRepresentation> userRepresentationStream = realmResource.users().search(username, -1, -1)
            .stream().filter(u -> u.getUsername() != null && u.getUsername().equals(username) || u.getEmail() != null && u.getEmail().equals(username));

            UserRepresentation userRepresentation = userRepresentationStream.findFirst().get();
            CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
            credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
            credentialRepresentation.setValue(password);
            userRepresentation.setCredentials(Arrays.asList(credentialRepresentation));

            UserResource userResource = realmResource.users().get(userRepresentation.getId());
            userResource.update(userRepresentation);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public List<User> getUsers(int offset, int take) {
        try {
            RealmResource realmResource = getRealmResource();
            return realmResource.users().list(offset, take).stream().map(KeycloakUserRepository::mapUser).collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Collections.emptyList();
    }

    @Override
    public List<User> findUsers(String search) {
        try {
            RealmResource realmResource = getRealmResource();
            return realmResource.users().search(search, -1, -1)
                    .stream()
                    .map(KeycloakUserRepository::mapUser).collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Collections.emptyList();
    }

    @Override
    public int getUsersCount(String search) {
        return 0;
    }

    @Override
    public List<User> findUsers(String search, int firstResult, int maxResults) {
        return null;
    }

    private RealmResource getRealmResource() {
        Keycloak realmClient = Keycloak.getInstance(this.serverUrl, this.realm, this.user, this.password, this.clientId);
        return realmClient.realm(this.realm);
    }
}
