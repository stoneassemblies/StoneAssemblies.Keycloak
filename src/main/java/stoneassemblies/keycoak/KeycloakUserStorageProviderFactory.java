package stoneassemblies.keycoak;

import stoneassemblies.keycoak.constants.KeycloakProviderProperties;
import stoneassemblies.keycoak.interfaces.UserRepository;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProviderFactory;

import java.util.List;


public class KeycloakUserStorageProviderFactory implements UserStorageProviderFactory<UserStorageProvider> {

    @Override
    public UserStorageProvider create(KeycloakSession session, ComponentModel model) {
        String url = model.get(KeycloakProviderProperties.SERVER_URL).trim();
        if(!url.endsWith("/auth")){
            url += "/auth";
        }
        String username = model.get(KeycloakProviderProperties.USERNAME);
        String realm = model.get(KeycloakProviderProperties.REALM);
        String password = model.get(KeycloakProviderProperties.PASSWORD);
        String clientId = model.get(KeycloakProviderProperties.CLIENT_ID);
        UserRepository userRepository = new KeycloakUserRepository(url, realm, username, password, clientId);
        UserStorageProvider userStorageProvider = new UserStorageProvider(session, model, userRepository);
        return userStorageProvider;
    }

    @Override
    public String getId() {
        return "keycloak-user-provider";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property(KeycloakProviderProperties.SERVER_URL, "Server Url", "Server Url", ProviderConfigProperty.STRING_TYPE, "http://192.168.1.6:18080/auth", null)
                .property(KeycloakProviderProperties.REALM, "Realm", "Realm", ProviderConfigProperty.STRING_TYPE, "master", null)
                .property(KeycloakProviderProperties.USERNAME, "Username", "Username", ProviderConfigProperty.STRING_TYPE, "admin", null)
                .property(KeycloakProviderProperties.PASSWORD, "Password", "Password", ProviderConfigProperty.PASSWORD, "admin", null)
                .property(KeycloakProviderProperties.CLIENT_ID, "Client Id", "Client Id", ProviderConfigProperty.STRING_TYPE, "admin-cli", null)
                .build();
    }
}

