package stoneassemblies.keycoak;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProviderFactory;
import stoneassemblies.keycoak.constants.RabbitMqProviderProperties;

import java.util.List;

public class RabbitMqUserStorageProviderFactory implements UserStorageProviderFactory<UserStorageProvider> {

    @Override
    public UserStorageProvider create(KeycloakSession keycloakSession, ComponentModel componentModel) {
        String secret = componentModel.get(RabbitMqProviderProperties.SECRET);
        if (secret != null) {
            secret = secret.trim();
        }

        return new UserStorageProvider(keycloakSession, componentModel, new RabbitMqUserRepository(
                componentModel.get(RabbitMqProviderProperties.HOST),
                Integer.parseInt(componentModel.get(RabbitMqProviderProperties.PORT)),
                componentModel.get(RabbitMqProviderProperties.USERNAME),
                componentModel.get(RabbitMqProviderProperties.PASSWORD),
                secret == null || secret.equals("") ? new DefaultEncryptionService() : new AesEncryptionService(secret)));
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property(RabbitMqProviderProperties.HOST, "Host", "Host", ProviderConfigProperty.STRING_TYPE, "localhost", null)
                .property(RabbitMqProviderProperties.PORT, "Port", "Port", ProviderConfigProperty.STRING_TYPE, "5672", null)
                .property(RabbitMqProviderProperties.USERNAME, "Username", "Username", ProviderConfigProperty.STRING_TYPE, "admin", null)
                .property(RabbitMqProviderProperties.PASSWORD, "Password", "Password", ProviderConfigProperty.PASSWORD, "admin", null)
                .property(RabbitMqProviderProperties.SECRET, "Secret", "Secret", ProviderConfigProperty.PASSWORD, "sOme*ShaREd*SecreT", null)
                .build();
    }

    @Override
    public String getId() {
        return "rabbitmq-user-provider";
    }
}
