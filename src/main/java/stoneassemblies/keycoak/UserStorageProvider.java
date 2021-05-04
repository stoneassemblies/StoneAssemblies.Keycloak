package stoneassemblies.keycoak;

import stoneassemblies.keycoak.interfaces.UserRepository;
import stoneassemblies.keycoak.models.User;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.*;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public class UserStorageProvider implements org.keycloak.storage.UserStorageProvider,
        UserLookupProvider, UserQueryProvider, CredentialInputUpdater, CredentialInputValidator {

    private Logger logger = Logger.getLogger(UserStorageProvider.class.getName());

    private final KeycloakSession session;

    private final ComponentModel model;

    private final UserRepository repository;

    public UserStorageProvider(KeycloakSession session, ComponentModel model, UserRepository repository) {
        this.session = session;
        this.model = model;
        this.repository = repository;
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return PasswordCredentialModel.TYPE.equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return supportsCredentialType(credentialType);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel)) {
            return false;
        }

        UserCredentialModel cred = (UserCredentialModel) input;
        return repository.validateCredentials(user.getUsername(), cred.getChallengeResponse());
    }

    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel)) {
            return false;
        }

        UserCredentialModel cred = (UserCredentialModel) input;
        return repository.updateCredentials(user.getUsername(), cred.getChallengeResponse());
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
    }

    @Override
    public Set<String> getDisableableCredentialTypes(RealmModel realm, UserModel user) {
        return Collections.emptySet();
    }

    @Override
    public void preRemove(RealmModel realm) {
    }

    @Override
    public void preRemove(RealmModel realm, GroupModel group) {
    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
    }

    @Override
    public void close() {
    }

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        String externalId = StorageId.externalId(id);
        User userById = repository.findUserById(externalId);
        if (userById != null) {
            return new UserAdapterFederatedStorage(session, realm, model, userById);
        }
        return null;
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        User userByUsernameOrEmail = repository.findUserByUsernameOrEmail(username);
        if (userByUsernameOrEmail != null) {
            return new UserAdapterFederatedStorage(session, realm, model, userByUsernameOrEmail);
        }

        return null;
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        return getUserByUsername(email, realm);
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        return repository.getUsersCount();
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm) {
        return repository.getAllUsers().stream()
                .map(user -> new UserAdapterFederatedStorage(session, realm, model, user))
                .collect(Collectors.toList());
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults) {
        return repository.getUsers(firstResult, maxResults).stream()
                .map(user -> new UserAdapterFederatedStorage(session, realm, model, user))
                .collect(Collectors.toList());
    }

//    @Override
//    public int getUsersCount(String search, RealmModel realm) {
//        return repository.getUsersCount(search);
//    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        return repository.findUsers(search).stream()
                .map(user -> new UserAdapterFederatedStorage(session, realm, model, user))
                .collect(Collectors.toList());
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults) {
        // return searchForUser(search, realm);
        return repository.findUsers(search, firstResult, maxResults).stream()
                .map(user -> new UserAdapterFederatedStorage(session, realm, model, user))
                .collect(Collectors.toList());
    }

    @Override
    public int getUsersCount(String search, RealmModel realm) {
        return this.repository.getUsersCount(search);
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm) {
        return getUsers(realm);
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm, int firstResult, int maxResults) {
        return getUsers(realm, firstResult, maxResults);
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults) {
        return Collections.emptyList();
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group) {
        return Collections.emptyList();
    }

    @Override
    public List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm) {
        return Collections.emptyList();
    }


}
