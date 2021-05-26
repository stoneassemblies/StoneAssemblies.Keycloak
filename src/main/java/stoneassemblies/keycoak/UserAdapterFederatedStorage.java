package stoneassemblies.keycoak;

import stoneassemblies.keycoak.models.User;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;


public class UserAdapterFederatedStorage extends AbstractUserAdapterFederatedStorage {

    private static Logger log = Logger.getLogger(UserAdapterFederatedStorage.class.getName());

    private final User user;

    static final String FEDERATED_ROLES = "FederatedRoles";

    public UserAdapterFederatedStorage(KeycloakSession session, RealmModel realm, ComponentModel model, User user) {
        super(session, realm, model);
        this.user = user;
        setFirstName(user.getFirstName());
        setLastName(user.getLastName());
        setEmail(user.getEmail());
        setEmailVerified(user.isEmailVerified());
        setEnabled(user.isEnabled());
        setCreatedTimestamp(user.getCreated());
        List<String> roles = this.user.getRoles();
        if (roles != null) {
            this.setAttribute(FEDERATED_ROLES, roles);
        }
    }

    @Override
    public String getId() {
        if (storageId == null) {
            storageId = new StorageId(storageProviderModel.getId(), user.getId());
        }

        return storageId.getId();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public void setUsername(String username) {
        user.setUsername(username);
    }

}
