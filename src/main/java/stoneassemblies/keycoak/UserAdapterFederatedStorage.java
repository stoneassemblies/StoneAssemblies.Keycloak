package stoneassemblies.keycoak;

import stoneassemblies.keycoak.models.User;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

import java.util.List;


public class UserAdapterFederatedStorage extends AbstractUserAdapterFederatedStorage {

    private final User user;

    static final String FEDERATED_ROLES = "FederatedRoles";

    public UserAdapterFederatedStorage(KeycloakSession session, RealmModel realm, ComponentModel model, User user) {
        super(session, realm, model);
        this.user = user;
        setFirstName(user.getFirstName());
        setLastName(user.getLastName());
        setEmail(user.getEmail());
        setEnabled(user.isEnabled());
        setCreatedTimestamp(user.getCreated());
        List<String> roles = user.getRoles();
        if (roles != null) {
            try {
                this.removeAttribute(FEDERATED_ROLES);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

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
